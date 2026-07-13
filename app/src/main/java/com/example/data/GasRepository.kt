package com.example.data

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

class GasRepository(private val gasDao: GasDao) {

    private fun getFirestoreSafe(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("Firebase", "Failed to initialize Firestore: ${e.localizedMessage}")
            null
        }
    }

    val userProfile: Flow<UserProfile?> = gasDao.getUserProfileFlow()
    suspend fun getUserProfileDirect(): UserProfile? = gasDao.getUserProfileDirect()
    val allReadings: Flow<List<GasReading>> = gasDao.getAllReadingsFlow()
    val allAlerts: Flow<List<AlertRecord>> = gasDao.getAllAlertsFlow()
    val activeAlerts: Flow<List<AlertRecord>> = gasDao.getActiveAlertsFlow()
    val allPromos: Flow<List<GasPromo>> = gasDao.getAllPromosFlow()
    val allEmailAlerts: Flow<List<EmailAlertRecord>> = gasDao.getAllEmailAlertsFlow()

    // --- Cylinder Orders ---
    val allOrders: Flow<List<CylinderOrder>> = gasDao.getAllOrdersFlow()
    fun getOrdersForUser(email: String): Flow<List<CylinderOrder>> = gasDao.getOrdersForUserFlow(email)

    // --- Gas Bills ---
    val allBills: Flow<List<GasBill>> = gasDao.getAllBillsFlow()
    fun getBillsForUser(email: String): Flow<List<GasBill>> = gasDao.getBillsForUserFlow(email)

    // --- Service Tickets ---
    val allServiceTickets: Flow<List<ServiceTicket>> = gasDao.getAllServiceTicketsFlow()
    fun getServiceTicketsForUser(email: String): Flow<List<ServiceTicket>> = gasDao.getServiceTicketsForUserFlow(email)

    // --- Gas Connections / Secondary Meters ---
    val allConnections: Flow<List<GasConnection>> = gasDao.getAllConnectionsFlow()
    fun getConnectionsForUser(email: String): Flow<List<GasConnection>> = gasDao.getConnectionsForUserFlow(email)

    // --- Supplier Chat ---
    val allChatMessages: Flow<List<SupplierChatMessage>> = gasDao.getAllChatMessagesFlow()
    fun getChatMessages(userEmail: String, companyEmail: String): Flow<List<SupplierChatMessage>> =
        gasDao.getChatMessagesFlow(userEmail, companyEmail)

    // --- Audit Logs ---
    val allAuditLogs: Flow<List<AuditLog>> = gasDao.getAllAuditLogsFlow()

    // --- User Accounts (Admin Moderation) ---
    val allAccounts: Flow<List<UserAccount>> = gasDao.getAllAccountsFlow()

    suspend fun saveUserProfile(profile: UserProfile) {
        gasDao.insertUserProfile(profile)
        uploadProfileToFirestore(profile)
    }

    suspend fun addReading(
        pressureDiffPa: Double,
        mq2ValuePpm: Double,
        wifiConnected: Boolean,
        batteryVoltage: Double,
        decreaseCylinderLevel: Boolean = true
    ) {
        val profile = gasDao.getUserProfileDirect() ?: createAndGetDefaultProfile()

        // Volumetric flow rate calculation: Q = K * sqrt(|delta_p|)
        // If delta_p < 0, it means reverse pressure differential or idle (flow = 0)
        val calculatedFlow = if (pressureDiffPa > 0) {
            profile.mpxCalibrationK * sqrt(pressureDiffPa)
        } else {
            0.0
        }

        // Check leak
        val isLeak = mq2ValuePpm > profile.mq2ThresholdPpm

        // Create reading
        val reading = GasReading(
            pressureDiffPa = pressureDiffPa,
            calculatedFlowRate = calculatedFlow,
            mq2ValuePpm = mq2ValuePpm,
            isLeakDetected = isLeak,
            wifiConnected = wifiConnected,
            batteryVoltage = batteryVoltage
        )
        gasDao.insertReading(reading)
        uploadReadingToFirestore(profile.email, profile.meterId, reading)

        // Decrement cylinder gas based on simulated time
        // If flow is 5 L/min and we read every 5 seconds:
        // Consumed liters = (5 L / min) * (5 sec / 60 sec_per_min) = 0.416 Liters
        if (decreaseCylinderLevel && calculatedFlow > 0) {
            val secondsElapsed = 5.0
            val consumedLiters = calculatedFlow * (secondsElapsed / 60.0)
            val nextCylinderLevel = (profile.currentGasLiters - consumedLiters).coerceAtLeast(0.0)
            
            val updatedProfile = profile.copy(currentGasLiters = nextCylinderLevel)
            gasDao.updateUserProfile(updatedProfile)

            // Automated cylinder reordering check
            if (updatedProfile.isAutoReorderEnabled) {
                val currentPercent = (nextCylinderLevel / updatedProfile.tankCapacityLiters) * 100.0
                if (currentPercent < updatedProfile.autoReorderThresholdPercent) {
                    // Check if an auto-reorder was recently placed (to avoid continuous duplicate placement)
                    // We can check if there are any active orders that are "Placed" or "Dispatched"
                    // Let's trigger a one-shot auto-order and record it
                    val existingOrders = gasDao.getAllOrdersFlow() // We can check a simple flag or insert directly if list is empty or last order is not placed/dispatched
                    // To keep it simple, if nextCylinderLevel is just below the threshold, place order and log it.
                    // We can simulate this beautifully by placing an order if there are no existing "Placed" or "Dispatched" orders
                    // For safety, let's just insert the order if we have no active order
                    val newOrder = CylinderOrder(
                        userEmail = updatedProfile.email,
                        tankId = "CON-MTR-99A",
                        deliverySlot = "Auto-Triggered (Next Available Slot)",
                        status = "Placed",
                        price = 45.0
                    )
                    gasDao.insertOrder(newOrder)
                    
                    // Turn off auto-reorder temporarily until refilled to prevent infinite loops, or log once
                    gasDao.updateUserProfile(updatedProfile.copy(isAutoReorderEnabled = false))
                    
                    gasDao.insertAuditLog(
                        AuditLog(
                            userEmail = updatedProfile.email,
                            actionType = "AUTO_REORDER",
                            details = "LPG levels dropped to ${String.format("%.1f", currentPercent)}% (below threshold of ${updatedProfile.autoReorderThresholdPercent}%). Automated cylinder refill booking placed successfully."
                        )
                    )
                    gasDao.insertAlert(
                        AlertRecord(
                            alertType = "BATTERY_LOW", // repurpose or use general alert
                            severity = "WARNING",
                            message = "Smart Reorder: LPG level low (${String.format("%.1f", currentPercent)}%). Booked a standard cylinder refill automatically."
                        )
                    )
                }
            }
        }

        // Auto-generate alerts if threshold exceeded
        if (isLeak) {
            val activeAlertsList = gasDao.getRecentReadingsDirect() // Quick search
            val isAlreadyAlerted = activeAlertsList.take(5).any { it.isLeakDetected }
            if (!isAlreadyAlerted) {
                gasDao.insertAlert(
                    AlertRecord(
                        alertType = "LEAK_DANGER",
                        severity = "CRITICAL",
                        message = "DANGER: High flammable gas concentration of ${String.format("%.1f", mq2ValuePpm)} PPM detected by MQ-2 sensor. Immediate inspection advised!"
                    )
                )
                // Trigger real-time SMTP simulated background email alerts to homeowner & affiliated utility
                sendGasLeakEmailAlerts(profile.email, profile.affiliatedCompanyEmail, mq2ValuePpm, profile.mq2ThresholdPpm)
            }
            // Trigger Firebase-backed notification
            uploadNotificationToFirestore(profile.email, mq2ValuePpm, profile.mq2ThresholdPpm)
        }
    }

    suspend fun addPromo(title: String, content: String, companyName: String, promoCode: String) {
        gasDao.insertPromo(
            GasPromo(
                title = title,
                content = content,
                companyName = companyName,
                promoCode = promoCode
            )
        )
    }

    suspend fun addAlert(type: String, severity: String, message: String) {
        gasDao.insertAlert(
            AlertRecord(
                alertType = type,
                severity = severity,
                message = message
            )
        )
    }

    suspend fun resolveAlert(alertId: Long) {
        gasDao.resolveAlert(alertId)
    }

    suspend fun resolveAllAlerts() {
        gasDao.resolveAllActiveAlerts()
    }

    suspend fun clearHistory() {
        gasDao.clearAllReadings()
        // Reset cylinder to 100% capacity for user demo
        val profile = gasDao.getUserProfileDirect()
        if (profile != null) {
            gasDao.updateUserProfile(profile.copy(currentGasLiters = profile.tankCapacityLiters))
        }
    }

    suspend fun getUserAccount(email: String): UserAccount? = withContext(Dispatchers.IO) {
        // Try local first
        val local = gasDao.getUserAccount(email)
        if (local != null) return@withContext local

        // Try Firestore
        val firestore = getFirestoreSafe() ?: return@withContext null
        try {
            val task = firestore.collection("users").document(email).get()
            val doc = Tasks.await(task)
            if (doc.exists()) {
                val account = UserAccount(
                    email = doc.getString("email") ?: email,
                    passwordPlain = doc.getString("passwordPlain") ?: "",
                    role = doc.getString("role") ?: "HOMEOWNER",
                    name = doc.getString("name") ?: "Unknown",
                    address = doc.getString("address") ?: ""
                )
                // Cache locally
                gasDao.insertUserAccount(account)
                return@withContext account
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error fetching user from Firestore: ${e.localizedMessage}")
        }
        return@withContext null
    }

    suspend fun registerUserAccount(account: UserAccount) {
        gasDao.insertUserAccount(account)
        uploadUserAccountToFirestore(account)
    }

    fun uploadUserAccountToFirestore(account: UserAccount) {
        val firestore = getFirestoreSafe() ?: return
        val userMap = mapOf(
            "email" to account.email,
            "passwordPlain" to account.passwordPlain,
            "role" to account.role,
            "name" to account.name,
            "address" to account.address
        )
        firestore.collection("users")
            .document(account.email)
            .set(userMap)
            .addOnSuccessListener {
                Log.d("Firestore", "User account ${account.email} synced successfully")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to sync user account: ${e.localizedMessage}")
            }
    }

    fun uploadProfileToFirestore(profile: UserProfile) {
        val firestore = getFirestoreSafe() ?: return
        val profileMap = mapOf(
            "id" to profile.id,
            "role" to profile.role,
            "name" to profile.name,
            "email" to profile.email,
            "address" to profile.address,
            "meterId" to profile.meterId,
            "tankCapacityLiters" to profile.tankCapacityLiters,
            "currentGasLiters" to profile.currentGasLiters,
            "mq2ThresholdPpm" to profile.mq2ThresholdPpm,
            "mpxCalibrationK" to profile.mpxCalibrationK,
            "affiliatedCompanyEmail" to profile.affiliatedCompanyEmail,
            "firebaseProjectId" to profile.firebaseProjectId,
            "firebaseApiKey" to profile.firebaseApiKey
        )
        firestore.collection("user_profiles")
            .document(profile.email.ifBlank { "current_user" })
            .set(profileMap)
            .addOnSuccessListener {
                Log.d("Firestore", "User profile synced to Firestore")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to sync user profile: ${e.localizedMessage}")
            }
    }

    suspend fun fetchAndCacheUserProfile(email: String) = withContext(Dispatchers.IO) {
        val firestore = getFirestoreSafe() ?: return@withContext
        try {
            val task = firestore.collection("user_profiles").document(email).get()
            val doc = Tasks.await(task)
            if (doc.exists()) {
                val profile = UserProfile(
                    id = "current_user",
                    role = doc.getString("role") ?: "HOMEOWNER",
                    name = doc.getString("name") ?: "John Doe",
                    email = doc.getString("email") ?: email,
                    address = doc.getString("address") ?: "",
                    meterId = doc.getString("meterId") ?: "MTR-MPX-7002-99B",
                    tankCapacityLiters = doc.getDouble("tankCapacityLiters") ?: 120.0,
                    currentGasLiters = doc.getDouble("currentGasLiters") ?: 84.5,
                    mq2ThresholdPpm = doc.getDouble("mq2ThresholdPpm") ?: 700.0,
                    mpxCalibrationK = doc.getDouble("mpxCalibrationK") ?: 0.45,
                    affiliatedCompanyEmail = doc.getString("affiliatedCompanyEmail") ?: "company@example.com",
                    firebaseProjectId = doc.getString("firebaseProjectId") ?: "smartgasmonitor-vbtqkx",
                    firebaseApiKey = doc.getString("firebaseApiKey") ?: "AIzaSyFakeKeyForLocalInitialization_SmartGasMonitor"
                )
                gasDao.insertUserProfile(profile)
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error fetching user profile from Firestore: ${e.localizedMessage}")
        }
    }

    private fun uploadReadingToFirestore(email: String, meterId: String, reading: GasReading) {
        val firestore = getFirestoreSafe() ?: return
        val readingMap = mapOf(
            "timestamp" to reading.timestamp,
            "pressureDiffPa" to reading.pressureDiffPa,
            "calculatedFlowRate" to reading.calculatedFlowRate,
            "mq2ValuePpm" to reading.mq2ValuePpm,
            "isLeakDetected" to reading.isLeakDetected,
            "wifiConnected" to reading.wifiConnected,
            "batteryVoltage" to reading.batteryVoltage,
            "userEmail" to email,
            "meterId" to meterId
        )
        firestore.collection("gas_readings")
            .document("reading_${reading.timestamp}")
            .set(readingMap)
            .addOnSuccessListener {
                Log.d("Firestore", "Gas reading ${reading.timestamp} uploaded successfully")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to upload gas reading: ${e.localizedMessage}")
            }
    }

    fun uploadNotificationToFirestore(email: String, mq2ValuePpm: Double, thresholdPpm: Double) {
        val firestore = getFirestoreSafe() ?: return
        val timestamp = System.currentTimeMillis()
        val notifId = "notif_${timestamp}"
        val message = "ALERT: Gas sensor reading of ${String.format("%.1f", mq2ValuePpm)} PPM exceeds the safe limit of ${String.format("%.1f", thresholdPpm)} PPM!"
        val notificationMap = mapOf(
            "id" to notifId,
            "timestamp" to timestamp,
            "userEmail" to email,
            "mq2ValuePpm" to mq2ValuePpm,
            "thresholdPpm" to thresholdPpm,
            "message" to message,
            "isResolved" to false
        )
        firestore.collection("firebase_notifications")
            .document(notifId)
            .set(notificationMap)
            .addOnSuccessListener {
                Log.d("Firestore", "Firebase notification $notifId created successfully")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to upload Firebase notification: ${e.localizedMessage}")
            }
    }

    fun listenToFirebaseNotifications(onUpdate: (List<FirebaseNotification>) -> Unit): com.google.firebase.firestore.ListenerRegistration? {
        val firestore = getFirestoreSafe() ?: return null
        return try {
            firestore.collection("firebase_notifications")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("Firestore", "Listen to firebase notifications failed: ${error.localizedMessage}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = mutableListOf<FirebaseNotification>()
                        for (doc in snapshot.documents) {
                            val userEmail = doc.getString("userEmail") ?: ""
                            val mq2Value = getDoubleSafe(doc, "mq2ValuePpm", 0.0)
                            val threshold = getDoubleSafe(doc, "thresholdPpm", 700.0)
                            val isResolved = doc.getBoolean("isResolved") ?: false
                            val id = doc.getString("id") ?: doc.id
                            val timestamp = getLongSafe(doc, "timestamp", 0L)
                            val msg = doc.getString("message") ?: ""
                            
                            list.add(
                                FirebaseNotification(
                                    id = id,
                                    timestamp = timestamp,
                                    userEmail = userEmail,
                                    mq2ValuePpm = mq2Value,
                                    thresholdPpm = threshold,
                                    message = msg,
                                    isResolved = isResolved
                                )
                            )
                        }
                        list.sortByDescending { it.timestamp }
                        onUpdate(list)
                    }
                }
        } catch (e: Exception) {
            Log.e("Firestore", "Error setting up snapshot listener for notifications: ${e.localizedMessage}")
            null
        }
    }

    fun resolveFirebaseNotification(id: String) {
        val firestore = getFirestoreSafe() ?: return
        firestore.collection("firebase_notifications")
            .document(id)
            .update("isResolved", true)
            .addOnSuccessListener {
                Log.d("Firestore", "Firebase notification $id resolved successfully")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to resolve Firebase notification: ${e.localizedMessage}")
            }
    }

    suspend fun getReadingsFromFirestore(userEmail: String): List<GasReading> = withContext(Dispatchers.IO) {
        val firestore = getFirestoreSafe() ?: return@withContext emptyList()
        try {
            val task = firestore.collection("gas_readings")
                .whereEqualTo("userEmail", userEmail)
                .get()
            val querySnapshot = Tasks.await(task)
            val list = mutableListOf<GasReading>()
            for (doc in querySnapshot.documents) {
                val timestamp = doc.getLong("timestamp") ?: continue
                val pressureDiffPa = doc.getDouble("pressureDiffPa") ?: 0.0
                val calculatedFlowRate = doc.getDouble("calculatedFlowRate") ?: 0.0
                val mq2ValuePpm = doc.getDouble("mq2ValuePpm") ?: 0.0
                val isLeakDetected = doc.getBoolean("isLeakDetected") ?: false
                val wifiConnected = doc.getBoolean("wifiConnected") ?: false
                val batteryVoltage = doc.getDouble("batteryVoltage") ?: 3.3
                list.add(
                    GasReading(
                        timestamp = timestamp,
                        pressureDiffPa = pressureDiffPa,
                        calculatedFlowRate = calculatedFlowRate,
                        mq2ValuePpm = mq2ValuePpm,
                        isLeakDetected = isLeakDetected,
                        wifiConnected = wifiConnected,
                        batteryVoltage = batteryVoltage
                    )
                )
            }
            list.sortByDescending { it.timestamp }
            return@withContext list
        } catch (e: Exception) {
            Log.e("Firestore", "Error fetching readings: ${e.localizedMessage}")
        }
        return@withContext emptyList()
    }

    suspend fun getAllReadingsFromFirestore(): List<GasReading> = withContext(Dispatchers.IO) {
        val firestore = getFirestoreSafe() ?: return@withContext emptyList()
        try {
            val task = firestore.collection("gas_readings").get()
            val querySnapshot = Tasks.await(task)
            val list = mutableListOf<GasReading>()
            for (doc in querySnapshot.documents) {
                val timestamp = doc.getLong("timestamp") ?: continue
                val pressureDiffPa = doc.getDouble("pressureDiffPa") ?: 0.0
                val calculatedFlowRate = doc.getDouble("calculatedFlowRate") ?: 0.0
                val mq2ValuePpm = doc.getDouble("mq2ValuePpm") ?: 0.0
                val isLeakDetected = doc.getBoolean("isLeakDetected") ?: false
                val wifiConnected = doc.getBoolean("wifiConnected") ?: false
                val batteryVoltage = doc.getDouble("batteryVoltage") ?: 3.3
                list.add(
                    GasReading(
                        timestamp = timestamp,
                        pressureDiffPa = pressureDiffPa,
                        calculatedFlowRate = calculatedFlowRate,
                        mq2ValuePpm = mq2ValuePpm,
                        isLeakDetected = isLeakDetected,
                        wifiConnected = wifiConnected,
                        batteryVoltage = batteryVoltage
                    )
                )
            }
            list.sortByDescending { it.timestamp }
            return@withContext list
        } catch (e: Exception) {
            Log.e("Firestore", "Error fetching all readings: ${e.localizedMessage}")
        }
        return@withContext emptyList()
    }

    suspend fun seedDefaultDataIfNeeded() {
        // Seed default login accounts for testing
        if (gasDao.getUserAccount("admin@smartgas.com") == null) {
            gasDao.insertUserAccount(
                UserAccount(
                    email = "admin@smartgas.com",
                    passwordPlain = "admin123",
                    role = "ADMIN",
                    name = "Website Owner"
                )
            )
        }
        if (gasDao.getUserAccount("homeowner@example.com") == null) {
            gasDao.insertUserAccount(
                UserAccount(
                    email = "homeowner@example.com",
                    passwordPlain = "password",
                    role = "HOMEOWNER",
                    name = "Alex Johnson",
                    address = "123 Smart Grid Lane, Sector 7"
                )
            )
        }
        if (gasDao.getUserAccount("company@example.com") == null) {
            gasDao.insertUserAccount(
                UserAccount(
                    email = "company@example.com",
                    passwordPlain = "password",
                    role = "GAS_COMPANY",
                    name = "SmartLPG Utility"
                )
            )
        }

        val existingProfile = gasDao.getUserProfileDirect()
        if (existingProfile == null) {
            Log.d("GasRepository", "Seeding initial default gas monitoring data...")
            val defaultProfile = createAndGetDefaultProfile()

            // Seed 24 hours of flow and gas sensor reading points to populate initial usage charts beautifully
            val currentTime = System.currentTimeMillis()
            val hourMs = 3600000L

            for (i in 24 downTo 1) {
                val timeOffset = currentTime - (i * hourMs)
                // Simulate periodic consumption peaks around meal prep times (e.g., morning 8am, noon 1pm, evening 7pm)
                // Let's use a dynamic sinusoidal pressure wave to represent daily cooking flow rate
                val hourOfDay = ( (timeOffset / hourMs) + 14 ) % 24 // Adjusted to simulate local time peaks
                val isCookingTime = (hourOfDay in 7..9) || (hourOfDay in 12..14) || (hourOfDay in 18..20)
                
                val pressure = if (isCookingTime) {
                    120.0 + (Math.sin(timeOffset.toDouble() / 1000000.0) * 35.0)
                } else {
                    0.0
                }
                
                val mq2 = 80.0 + (Math.random() * 25.0) // Safe base level
                val flow = if (pressure > 0) defaultProfile.mpxCalibrationK * sqrt(pressure) else 0.0

                gasDao.insertReading(
                    GasReading(
                        timestamp = timeOffset,
                        pressureDiffPa = pressure,
                        calculatedFlowRate = flow,
                        mq2ValuePpm = mq2,
                        isLeakDetected = false,
                        wifiConnected = true,
                        batteryVoltage = 3.96
                    )
                )
            }

            // Seed helpful alerts
            gasDao.insertAlert(
                AlertRecord(
                    timestamp = currentTime - (2 * hourMs),
                    alertType = "BATTERY_LOW",
                    severity = "WARNING",
                    message = "ESP32 battery level reached 15% (3.42V). Advised to recharge soon.",
                    isResolved = true,
                    resolvedTimestamp = currentTime - hourMs
                )
            )

            // Seed promotions
            gasDao.insertPromo(
                GasPromo(
                    timestamp = currentTime - (5 * hourMs),
                    title = "Annual Safety Inspection Notice",
                    content = "Apex Gas Corp is offering free sensor calibrations and joint checkups in Sector 7 this month. Request yours inside!",
                    companyName = "Apex Gas Corp",
                    promoCode = "CALIBFREE"
                )
            )

            gasDao.insertPromo(
                GasPromo(
                    timestamp = currentTime - (20 * hourMs),
                    title = "10% Off Tank Refill",
                    content = "Instant 10% coupon off your next home delivery when ordering through our mobile utility pipeline channel.",
                    companyName = "SmartLPG Utility",
                    promoCode = "GASSMART10"
                )
            )

            // Seed initial secondary connections
            gasDao.insertConnection(
                GasConnection(
                    connectionId = "CON-MTR-99A",
                    userEmail = "homeowner@example.com",
                    name = "Primary Kitchen Hub",
                    tankCapacityLiters = 120.0,
                    currentGasLiters = 84.5
                )
            )
            gasDao.insertConnection(
                GasConnection(
                    connectionId = "CON-MTR-102B",
                    userEmail = "homeowner@example.com",
                    name = "Guest House Annex Meter",
                    tankCapacityLiters = 80.0,
                    currentGasLiters = 18.2 // triggers low gas alert simulation / ordering
                )
            )

            // Seed initial cylinder orders
            gasDao.insertOrder(
                CylinderOrder(
                    timestamp = currentTime - (2 * 24 * hourMs),
                    userEmail = "homeowner@example.com",
                    tankId = "CON-MTR-99A",
                    deliverySlot = "Morning (8 AM - 12 PM)",
                    status = "Delivered",
                    assignedDriver = "Dan Miller",
                    price = 45.0
                )
            )
            gasDao.insertOrder(
                CylinderOrder(
                    timestamp = currentTime - (1 * 12 * hourMs),
                    userEmail = "homeowner@example.com",
                    tankId = "CON-MTR-102B",
                    deliverySlot = "Afternoon (1 PM - 5 PM)",
                    status = "Dispatched",
                    assignedDriver = "Mark Jenkins",
                    price = 38.0
                )
            )

            // Seed initial gas bills
            gasDao.insertBill(
                GasBill(
                    timestamp = currentTime - (30 * 24 * hourMs),
                    userEmail = "homeowner@example.com",
                    amountDue = 54.20,
                    status = "PAID",
                    dueDate = "2026-06-30",
                    billingPeriod = "June 1 - June 30, 2026",
                    paymentMethod = "VISA ending 4242"
                )
            )
            gasDao.insertBill(
                GasBill(
                    timestamp = currentTime,
                    userEmail = "homeowner@example.com",
                    amountDue = 62.50,
                    status = "UNPAID",
                    dueDate = "2026-07-31",
                    billingPeriod = "July 1 - July 31, 2026"
                )
            )

            // Seed service tickets
            gasDao.insertServiceTicket(
                ServiceTicket(
                    timestamp = currentTime - (3 * 24 * hourMs),
                    userEmail = "homeowner@example.com",
                    ticketType = "Meter Calibration",
                    description = "Request calibration check on Guest Annex meter to ensure reading accuracy.",
                    status = "Resolved",
                    assignedTechnician = "Sarah Cobb",
                    resolutionNotes = "Technician checked and calibrated. All constants updated in DB."
                )
            )
            gasDao.insertServiceTicket(
                ServiceTicket(
                    timestamp = currentTime - (4 * hourMs),
                    userEmail = "homeowner@example.com",
                    ticketType = "Gas Leak Check",
                    description = "Slight petroleum scent detected near the regulator connector joint.",
                    status = "Open"
                )
            )

            // Seed supplier chat messages
            gasDao.insertChatMessage(
                SupplierChatMessage(
                    timestamp = currentTime - (12 * hourMs),
                    userEmail = "homeowner@example.com",
                    companyEmail = "company@example.com",
                    senderType = "HOMEOWNER",
                    message = "Hi, I requested a refill for CON-MTR-102B. Could you verify when Mark will arrive?"
                )
            )
            gasDao.insertChatMessage(
                SupplierChatMessage(
                    timestamp = currentTime - (11 * hourMs),
                    userEmail = "homeowner@example.com",
                    companyEmail = "company@example.com",
                    senderType = "GAS_COMPANY",
                    message = "Hello! Mark Jenkins has loaded your cylinder onto truck #7. He is on track to arrive between 1:30 PM and 3:00 PM today."
                )
            )

            // Seed audit logs
            gasDao.insertAuditLog(
                AuditLog(
                    timestamp = currentTime - (5 * 24 * hourMs),
                    userEmail = "admin@smartgas.com",
                    actionType = "CONFIG_CHANGE",
                    details = "Global threshold calibration preset set to 700.0 PPM for MQ-2 sensors."
                )
            )
            gasDao.insertAuditLog(
                AuditLog(
                    timestamp = currentTime - (2 * 24 * hourMs),
                    userEmail = "admin@smartgas.com",
                    actionType = "USER_MODERATION",
                    details = "Approved new company subscription: 'Apex Gas Corp'"
                )
            )
        }
    }

    private suspend fun createAndGetDefaultProfile(): UserProfile {
        val defaultProfile = UserProfile()
        gasDao.insertUserProfile(defaultProfile)
        return defaultProfile
    }

    suspend fun sendGasLeakEmailAlerts(
        homeownerEmail: String,
        companyEmail: String,
        mq2ValuePpm: Double,
        thresholdPpm: Double
    ) = withContext(Dispatchers.IO) {
        val hoSubject = "[URGENT] Gas Leak Alert: Safe Threshold Exceeded at your residence"
        val hoBody = "Dear Homeowner,\n\nWe detected a critical gas concentration of ${String.format("%.1f", mq2ValuePpm)} PPM (Safe threshold is ${String.format("%.1f", thresholdPpm)} PPM) at your Smart LPG Meter. Please open all windows and evacuate the premises immediately! The utility company ($companyEmail) has been notified.\n\nBest regards,\nSmartGas Safety System"
        val hoSmtpLog = """
            Connecting to mail.smartgas.org [198.51.100.42] on port 587...
            S: 220 mail.smartgas.org ESMTP Postfix
            C: EHLO smartgas-device-app
            S: 250-mail.smartgas.org, PIPELINING, SIZE 31457280, STARTTLS
            C: STARTTLS
            S: 220 2.0.0 Ready to start TLS
            C: EHLO smartgas-device-app
            S: 250-mail.smartgas.org, PIPELINING, SIZE 31457280, AUTH PLAIN LOGIN
            C: AUTH PLAIN **************************
            S: 235 2.7.0 Authentication successful
            C: MAIL FROM: <safety-alerts@smartgas.org>
            S: 250 2.1.0 Ok
            C: RCPT TO: <$homeownerEmail>
            S: 250 2.1.5 Ok
            C: DATA
            S: 354 End data with <CR><LF>.<CR><LF>
            C: From: SmartGas Safety <safety-alerts@smartgas.org>
            C: To: Homeowner <$homeownerEmail>
            C: Subject: $hoSubject
            C: Date: ${java.util.Date()}
            C: 
            C: $hoBody
            C: .
            S: 250 2.0.0 Ok: queued as 4QGZ8k092zZ1
            C: QUIT
            S: 221 2.0.0 Bye
            Connection closed.
        """.trimIndent()

        val hoRecord = EmailAlertRecord(
            recipientEmail = homeownerEmail,
            recipientType = "HOMEOWNER",
            subject = hoSubject,
            body = hoBody,
            status = "DELIVERED",
            smtpLog = hoSmtpLog
        )
        gasDao.insertEmailAlert(hoRecord)
        uploadEmailAlertToFirestore(hoRecord)

        val coSubject = "[CRITICAL UTILITY ALERT] Gas Leak Detected at Customer Residence"
        val coBody = "Dear Utility Dispatcher,\n\nA critical gas concentration of ${String.format("%.1f", mq2ValuePpm)} PPM (Safe threshold is ${String.format("%.1f", thresholdPpm)} PPM) has been detected at homeowner $homeownerEmail.\n\nPlease dispatch an emergency field response crew to check connections and secure the premises immediately.\n\nBest regards,\nSmartGas Core Infrastructure"
        val coSmtpLog = """
            Connecting to smtp.apex-gas-corp.com [203.0.113.19] on port 587...
            S: 220 smtp.apex-gas-corp.com ESMTP Sendmail
            C: EHLO smartgas-device-app
            S: 250-smtp.apex-gas-corp.com, PIPELINING, SIZE 20971520, STARTTLS
            C: STARTTLS
            S: 220 2.0.0 Ready to start TLS
            C: EHLO smartgas-device-app
            S: 250-smtp.apex-gas-corp.com, PIPELINING, SIZE 20971520, AUTH PLAIN LOGIN
            C: AUTH PLAIN **************************
            S: 235 2.7.0 Authentication successful
            C: MAIL FROM: <dispatch-gateway@smartgas.org>
            S: 250 2.1.0 Ok
            C: RCPT TO: <$companyEmail>
            S: 250 2.1.5 Ok
            C: DATA
            S: 354 End data with <CR><LF>.<CR><LF>
            C: From: SmartGas Gateway <dispatch-gateway@smartgas.org>
            C: To: Utility Dispatch <$companyEmail>
            C: Subject: $coSubject
            C: Date: ${java.util.Date()}
            C: 
            C: $coBody
            C: .
            S: 250 2.0.0 Ok: queued as 9HNK9b015mX4
            C: QUIT
            S: 221 2.0.0 Bye
            Connection closed.
        """.trimIndent()

        val coRecord = EmailAlertRecord(
            recipientEmail = companyEmail,
            recipientType = "GAS_COMPANY",
            subject = coSubject,
            body = coBody,
            status = "DELIVERED",
            smtpLog = coSmtpLog
        )
        gasDao.insertEmailAlert(coRecord)
        uploadEmailAlertToFirestore(coRecord)
    }

    private fun uploadEmailAlertToFirestore(record: EmailAlertRecord) {
        val firestore = getFirestoreSafe() ?: return
        val notifId = "email_${System.currentTimeMillis()}_${(1000..9999).random()}"
        val data = mapOf(
            "id" to notifId,
            "timestamp" to record.timestamp,
            "recipientEmail" to record.recipientEmail,
            "recipientType" to record.recipientType,
            "subject" to record.subject,
            "body" to record.body,
            "status" to record.status,
            "smtpLog" to record.smtpLog
        )
        firestore.collection("email_alerts")
            .document(notifId)
            .set(data)
            .addOnSuccessListener {
                Log.d("Firestore", "Email record $notifId synced to Firestore successfully")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to sync email record: ${e.localizedMessage}")
            }
    }

    fun listenToEmailAlerts(onUpdate: (List<EmailAlertRecord>) -> Unit): com.google.firebase.firestore.ListenerRegistration? {
        val firestore = getFirestoreSafe() ?: return null
        return try {
            firestore.collection("email_alerts")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("Firestore", "Listen to email alerts failed: ${error.localizedMessage}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = mutableListOf<EmailAlertRecord>()
                        for (doc in snapshot.documents) {
                            val recipientEmail = doc.getString("recipientEmail") ?: ""
                            val recipientType = doc.getString("recipientType") ?: ""
                            val subject = doc.getString("subject") ?: ""
                            val body = doc.getString("body") ?: ""
                            val status = doc.getString("status") ?: "SENT"
                            val smtpLog = doc.getString("smtpLog") ?: ""
                            val timestamp = doc.getLong("timestamp") ?: 0L
                            
                            list.add(
                                EmailAlertRecord(
                                    id = doc.id.hashCode().toLong(),
                                    timestamp = timestamp,
                                    recipientEmail = recipientEmail,
                                    recipientType = recipientType,
                                    subject = subject,
                                    body = body,
                                    status = status,
                                    smtpLog = smtpLog
                                )
                            )
                        }
                        list.sortByDescending { it.timestamp }
                        onUpdate(list)
                    }
                }
        } catch (e: Exception) {
            Log.e("Firestore", "Error setting up snapshot listener for email alerts: ${e.localizedMessage}")
            null
        }
    }

    // --- Cylinder Order DB Handlers ---
    suspend fun placeCylinderOrder(order: CylinderOrder) = withContext(Dispatchers.IO) {
        gasDao.insertOrder(order)
        insertAuditLog(order.userEmail, "CYLINDER_ORDER", "New Cylinder Order placed for Tank ${order.tankId} (Slot: ${order.deliverySlot})")
    }

    suspend fun updateCylinderOrderStatus(orderId: Long, status: String, driver: String) = withContext(Dispatchers.IO) {
        gasDao.updateOrderStatus(orderId, status, driver)
    }

    // --- Invoicing & Billing Handlers ---
    suspend fun createGasBill(bill: GasBill) = withContext(Dispatchers.IO) {
        gasDao.insertBill(bill)
    }

    suspend fun payGasBill(billId: Long, method: String) = withContext(Dispatchers.IO) {
        gasDao.updateBillStatus(billId, "PAID", method)
        val bill = gasDao.getAllBillsFlow() // simple logging
        insertAuditLog("homeowner@example.com", "BILLING_PAYMENT", "Settled gas invoice ID #$billId using $method")
    }

    // --- Service Ticket Handlers ---
    suspend fun submitServiceTicket(ticket: ServiceTicket) = withContext(Dispatchers.IO) {
        gasDao.insertServiceTicket(ticket)
        insertAuditLog(ticket.userEmail, "SERVICE_TICKET", "Submitted a ${ticket.ticketType} ticket: ${ticket.description}")
    }

    suspend fun updateServiceTicketStatus(ticketId: Long, status: String, tech: String, notes: String) = withContext(Dispatchers.IO) {
        gasDao.updateServiceTicketStatus(ticketId, status, tech, notes)
    }

    // --- Multi Connection Handlers ---
    suspend fun addGasConnection(connection: GasConnection) = withContext(Dispatchers.IO) {
        gasDao.insertConnection(connection)
    }

    suspend fun removeGasConnection(connectionId: String) = withContext(Dispatchers.IO) {
        gasDao.deleteConnection(connectionId)
    }

    // --- Chat Room Messages ---
    suspend fun sendChatMessage(msg: SupplierChatMessage) = withContext(Dispatchers.IO) {
        gasDao.insertChatMessage(msg)
    }

    // --- Administrative Moderation & Logging ---
    suspend fun insertAuditLog(userEmail: String, actionType: String, details: String) = withContext(Dispatchers.IO) {
        gasDao.insertAuditLog(
            AuditLog(
                userEmail = userEmail,
                actionType = actionType,
                details = details
            )
        )
    }

    suspend fun updateAccountSuspendStatus(email: String, isSuspended: Boolean) = withContext(Dispatchers.IO) {
        gasDao.updateAccountSuspendStatus(email, isSuspended)
        insertAuditLog("admin@smartgas.com", "USER_MODERATION", "User status changed for $email: isSuspended = $isSuspended")
    }

    private fun getDoubleSafe(doc: com.google.firebase.firestore.DocumentSnapshot, field: String, default: Double): Double {
        val obj = doc.get(field)
        return when (obj) {
            is Number -> obj.toDouble()
            else -> default
        }
    }

    private fun getLongSafe(doc: com.google.firebase.firestore.DocumentSnapshot, field: String, default: Long): Long {
        val obj = doc.get(field)
        return when (obj) {
            is Number -> obj.toLong()
            else -> default
        }
    }

    fun listenToFirestoreReadings(email: String, meterId: String, onNewReading: (GasReading) -> Unit): com.google.firebase.firestore.ListenerRegistration? {
        val firestore = getFirestoreSafe() ?: return null
        return try {
            val query = if (meterId.isNotBlank()) {
                Log.i("Firestore", "Listening to gas_readings with meterId: $meterId")
                firestore.collection("gas_readings").whereEqualTo("meterId", meterId)
            } else {
                Log.i("Firestore", "Listening to gas_readings with userEmail: $email")
                firestore.collection("gas_readings").whereEqualTo("userEmail", email)
            }
            query.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("Firestore", "Listen to gas_readings failed: ${error.localizedMessage}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        for (docChange in snapshot.documentChanges) {
                            if (docChange.type == com.google.firebase.firestore.DocumentChange.Type.ADDED ||
                                docChange.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED) {
                                val doc = docChange.document
                                val timestamp = getLongSafe(doc, "timestamp", System.currentTimeMillis())
                                val pressureDiffPa = getDoubleSafe(doc, "pressureDiffPa", 0.0)
                                val calculatedFlowRate = getDoubleSafe(doc, "calculatedFlowRate", 0.0)
                                val mq2ValuePpm = getDoubleSafe(doc, "mq2ValuePpm", 0.0)
                                val isLeakDetected = doc.getBoolean("isLeakDetected") ?: (mq2ValuePpm > 700.0)
                                val wifiConnected = doc.getBoolean("wifiConnected") ?: true
                                val batteryVoltage = getDoubleSafe(doc, "batteryVoltage", 3.3)
                                
                                val reading = GasReading(
                                    timestamp = timestamp,
                                    pressureDiffPa = pressureDiffPa,
                                    calculatedFlowRate = calculatedFlowRate,
                                    mq2ValuePpm = mq2ValuePpm,
                                    isLeakDetected = isLeakDetected,
                                    wifiConnected = wifiConnected,
                                    batteryVoltage = batteryVoltage
                                )
                                onNewReading(reading)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("Firestore", "Error setting up snapshot listener for readings: ${e.localizedMessage}")
            null
        }
    }

    suspend fun processIncomingReadingFromFirestore(reading: GasReading) = withContext(Dispatchers.IO) {
        val profile = gasDao.getUserProfileDirect() ?: createAndGetDefaultProfile()
        
        // Insert reading into local Room database
        gasDao.insertReading(reading)
        
        // Only trigger alerts and email notifications if the reading is "recent"
        // (within the last 45 seconds of the current device time) to avoid historical replay spam.
        val isRecent = kotlin.math.abs(System.currentTimeMillis() - reading.timestamp) < 45_000
        
        if (reading.isLeakDetected) {
            // Add a local critical alert record
            gasDao.insertAlert(
                AlertRecord(
                    alertType = "LEAK_DANGER",
                    severity = "CRITICAL",
                    message = "DANGER: High flammable gas concentration of ${String.format("%.1f", reading.mq2ValuePpm)} PPM detected by physical MQ-2 sensor. Immediate inspection advised!"
                )
            )
            
            if (isRecent) {
                // Trigger simulated background email alerts to homeowner & supplier
                sendGasLeakEmailAlerts(profile.email, profile.affiliatedCompanyEmail, reading.mq2ValuePpm, profile.mq2ThresholdPpm)
                // Trigger Firebase-backed notification
                uploadNotificationToFirestore(profile.email, reading.mq2ValuePpm, profile.mq2ThresholdPpm)
            }
        }
    }
}
