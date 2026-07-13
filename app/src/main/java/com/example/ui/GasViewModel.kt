package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GasViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GasRepository

    // Database Streams
    val userProfile: StateFlow<UserProfile?>
    val allReadings: StateFlow<List<GasReading>>
    val allAlerts: StateFlow<List<AlertRecord>>
    val activeAlerts: StateFlow<List<AlertRecord>>
    val allPromos: StateFlow<List<GasPromo>>

    // Portal Expanded Streams
    val allOrders: StateFlow<List<CylinderOrder>>
    val allBills: StateFlow<List<GasBill>>
    val allServiceTickets: StateFlow<List<ServiceTicket>>
    val allConnections: StateFlow<List<GasConnection>>
    val allChatMessages: StateFlow<List<SupplierChatMessage>>
    val allAuditLogs: StateFlow<List<AuditLog>>
    val allAccounts: StateFlow<List<UserAccount>>

    // Firestore Firebase Notifications stream
    private val _firebaseNotifications = MutableStateFlow<List<FirebaseNotification>>(emptyList())
    val firebaseNotifications: StateFlow<List<FirebaseNotification>> = _firebaseNotifications.asStateFlow()
    private var firebaseNotifListener: ListenerRegistration? = null

    // Real-time Email Alerts stream
    private val _emailAlerts = MutableStateFlow<List<EmailAlertRecord>>(emptyList())
    val emailAlerts: StateFlow<List<EmailAlertRecord>> = _emailAlerts.asStateFlow()
    private var emailAlertsListener: ListenerRegistration? = null
    private var firestoreReadingsListener: ListenerRegistration? = null

    // UI state
    private val _currentRole = MutableStateFlow("HOMEOWNER") // "HOMEOWNER", "GAS_COMPANY", "ADMIN"
    val currentRole: StateFlow<String> = _currentRole.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _loggedInUser = MutableStateFlow<UserAccount?>(null)
    val loggedInUser: StateFlow<UserAccount?> = _loggedInUser.asStateFlow()

    // Firestore-retrieved consumption readings for Recharts-styled visualizer
    private val _firestoreReadings = MutableStateFlow<List<GasReading>>(emptyList())
    val firestoreReadings: StateFlow<List<GasReading>> = _firestoreReadings.asStateFlow()

    private val _isFirestoreLoading = MutableStateFlow(false)
    val isFirestoreLoading: StateFlow<Boolean> = _isFirestoreLoading.asStateFlow()

    fun fetchFirestoreReadings() {
        val user = _loggedInUser.value ?: return
        viewModelScope.launch {
            _isFirestoreLoading.value = true
            try {
                val fetched = if (user.role == "GAS_COMPANY") {
                    repository.getAllReadingsFromFirestore()
                } else {
                    repository.getReadingsFromFirestore(user.email)
                }
                _firestoreReadings.value = fetched
            } catch (e: Exception) {
                android.util.Log.e("GasViewModel", "Error fetching from Firestore: ${e.localizedMessage}")
            } finally {
                _isFirestoreLoading.value = false
            }
        }
    }

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _currentTab = MutableStateFlow("DASHBOARD") // "DASHBOARD", "ALERTS", "PROMOS"
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    private val _currentCompanyTab = MutableStateFlow("CUSTOMERS")
    val currentCompanyTab: StateFlow<String> = _currentCompanyTab.asStateFlow()

    private val _currentAdminTab = MutableStateFlow("ACCOUNTS")
    val currentAdminTab: StateFlow<String> = _currentAdminTab.asStateFlow()

    // Interactive Telemetry Simulation Parameters (Simulates physical ESP32 outputs)
    private val _simPressurePa = MutableStateFlow(125.0) // Delta P in Pascals
    val simPressurePa = _simPressurePa.asStateFlow()

    private val _simMq2Ppm = MutableStateFlow(110.0) // MQ-2 leak concentration (PPM)
    val simMq2Ppm = _simMq2Ppm.asStateFlow()

    private val _simWifiConnected = MutableStateFlow(true)
    val simWifiConnected = _simWifiConnected.asStateFlow()

    private val _simBatteryVoltage = MutableStateFlow(3.92) // 3.3V to 4.2V
    val simBatteryVoltage = _simBatteryVoltage.asStateFlow()

    private val _isSimActive = MutableStateFlow(true) // Whether live sensor streaming is active
    val isSimActive = _isSimActive.asStateFlow()

    // Admin Inputs
    private val _calibrationKStr = MutableStateFlow("0.45")
    val calibrationKStr = _calibrationKStr.asStateFlow()

    private val _thresholdPpmStr = MutableStateFlow("700.0")
    val thresholdPpmStr = _thresholdPpmStr.asStateFlow()

    private var simJob: Job? = null

    init {
        val database = GasDatabase.getDatabase(application)
        repository = GasRepository(database.gasDao())

        userProfile = repository.userProfile.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        // Run the dynamic sequential startup logic asynchronously to avoid blocking main thread,
        // while preventing redundant/rapid re-initializations that crash the DataStore.
        viewModelScope.launch {
            // 1. Seed default data if needed (runs on IO)
            withContext(Dispatchers.IO) {
                repository.seedDefaultDataIfNeeded()
            }

            // 2. Load the saved user profile
            val profile = withContext(Dispatchers.IO) {
                repository.getUserProfileDirect()
            }

            // 3. Determine the desired Firebase Options
            val desiredProjId = if (profile != null && profile.firebaseProjectId.isNotBlank()) profile.firebaseProjectId else "smartgasmonitor-vbtqkx"
            val desiredApiKey = if (profile != null && profile.firebaseApiKey.isNotBlank()) profile.firebaseApiKey else "AIzaSyFakeKeyForLocalInitialization_SmartGasMonitor"

            // 4. Check the current FirebaseApp state
            val currentApp = try { FirebaseApp.getInstance() } catch (e: Exception) { null }
            val currentProjId = currentApp?.options?.projectId
            val currentApiKey = currentApp?.options?.apiKey

            val needsInit = currentApp == null || (currentProjId != desiredProjId || currentApiKey != desiredApiKey)

            if (needsInit) {
                Log.i("FirebaseInit", "Initializing Firebase with: Project=$desiredProjId")
                try {
                    // Unregister listeners if they exist
                    try { firebaseNotifListener?.remove() } catch (e: Exception) {}
                    try { emailAlertsListener?.remove() } catch (e: Exception) {}
                    try { firestoreReadingsListener?.remove() } catch (e: Exception) {}
                    
                    firebaseNotifListener = null
                    emailAlertsListener = null
                    firestoreReadingsListener = null

                    FirebaseApp.getApps(application).forEach { app ->
                        if (app.name == FirebaseApp.DEFAULT_APP_NAME) {
                            app.delete()
                        }
                    }
                    
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:535304740076:android:6fbc17345b5993de")
                        .setApiKey(desiredApiKey)
                        .setProjectId(desiredProjId)
                        .build()
                    FirebaseApp.initializeApp(application, options)
                    Log.i("FirebaseInit", "Firebase App initialized successfully.")
                } catch (e: Exception) {
                    Log.e("FirebaseInit", "Failed to initialize Firebase App: ${e.message}", e)
                }
            } else {
                Log.i("FirebaseInit", "Firebase already initialized with matching options. Skipping re-initialization.")
            }

            // 5. Register Firebase notifications & email alerts listeners safely
            try {
                firebaseNotifListener = repository.listenToFirebaseNotifications { list ->
                    _firebaseNotifications.value = list
                }
            } catch (e: Exception) {
                Log.e("FirebaseInit", "Failed to register notifications listener: ${e.message}")
            }

            try {
                emailAlertsListener = repository.listenToEmailAlerts { list ->
                    _emailAlerts.value = list
                }
            } catch (e: Exception) {
                Log.e("FirebaseInit", "Failed to register email alerts listener: ${e.message}")
            }

            // 6. Observe logged-in user changes to register real-time snapshot listener for physical ESP32 readings
            viewModelScope.launch {
                loggedInUser.collect { user ->
                    try {
                        firestoreReadingsListener?.remove()
                    } catch (e: Exception) {}
                    firestoreReadingsListener = null
                    if (user != null && user.role == "HOMEOWNER") {
                        try {
                            val profile = withContext(Dispatchers.IO) { repository.getUserProfileDirect() }
                            val meterId = profile?.meterId ?: ""
                            firestoreReadingsListener = repository.listenToFirestoreReadings(user.email, meterId) { reading ->
                                viewModelScope.launch {
                                    repository.processIncomingReadingFromFirestore(reading)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("FirebaseInit", "Failed to register readings listener: ${e.message}")
                        }
                    }
                }
            }

            // 7. Auto-login previous user session if exists
            if (profile != null) {
                val account = withContext(Dispatchers.IO) {
                    repository.getUserAccount(profile.email)
                }
                if (account != null) {
                    Log.i("AutoLogin", "Auto-logging in saved session: ${account.email}")
                    _loggedInUser.value = account
                    _currentRole.value = account.role
                    _isLoggedIn.value = true
                    fetchFirestoreReadings()
                }
            }

            // 8. Sync Admin inputs with database values on load
            viewModelScope.launch {
                userProfile.collect { updatedProfile ->
                    if (updatedProfile != null) {
                        _calibrationKStr.value = updatedProfile.mpxCalibrationK.toString()
                        _thresholdPpmStr.value = updatedProfile.mq2ThresholdPpm.toString()
                    }
                }
            }

            // 9. Start the simulation loop safely
            startSimulationLoop()

            // 10. Dynamically monitor userProfile for subsequent credential changes (e.g., from settings screen)
            var isFirstEmission = true
            viewModelScope.launch {
                repository.userProfile.collect { updatedProfile ->
                    if (isFirstEmission) {
                        isFirstEmission = false
                        return@collect
                    }
                    
                    if (updatedProfile != null) {
                        val appInstance = try { FirebaseApp.getInstance() } catch (e: Exception) { null }
                        val appProjId = appInstance?.options?.projectId
                        val appApiKey = appInstance?.options?.apiKey
                        
                        if (updatedProfile.firebaseProjectId.isNotBlank() && updatedProfile.firebaseApiKey.isNotBlank() &&
                            (appProjId != updatedProfile.firebaseProjectId || appApiKey != updatedProfile.firebaseApiKey)) {
                            
                            Log.i("FirebaseInit", "Detected Firebase config change. Reinitializing FirebaseApp with: ${updatedProfile.firebaseProjectId}")
                            try {
                                // Safely remove listeners before deleting app
                                try { firebaseNotifListener?.remove() } catch (e: Exception) {}
                                try { emailAlertsListener?.remove() } catch (e: Exception) {}
                                try { firestoreReadingsListener?.remove() } catch (e: Exception) {}
                                
                                firebaseNotifListener = null
                                emailAlertsListener = null
                                firestoreReadingsListener = null

                                FirebaseApp.getApps(application).forEach { app ->
                                    if (app.name == FirebaseApp.DEFAULT_APP_NAME) {
                                        app.delete()
                                    }
                                }
                                
                                val newOptions = FirebaseOptions.Builder()
                                    .setApplicationId("1:535304740076:android:6fbc17345b5993de")
                                    .setApiKey(updatedProfile.firebaseApiKey)
                                    .setProjectId(updatedProfile.firebaseProjectId)
                                    .build()
                                FirebaseApp.initializeApp(application, newOptions)
                                
                                // Restart listeners on the newly configured Firebase project
                                restartFirebaseListeners(updatedProfile.email, updatedProfile.role)
                            } catch (e: Exception) {
                                Log.e("FirebaseInit", "Error during dynamic reinitialization: ${e.message}")
                            }
                        }
                    }
                }
            }
        }

        allReadings = repository.allReadings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allAlerts = repository.allAlerts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        activeAlerts = repository.activeAlerts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allPromos = repository.allPromos.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allOrders = repository.allOrders.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allBills = repository.allBills.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allServiceTickets = repository.allServiceTickets.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allConnections = repository.allConnections.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allChatMessages = repository.allChatMessages.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allAuditLogs = repository.allAuditLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allAccounts = repository.allAccounts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Sequential startup handles seeding, auto-login, and simulation start safely.
    }

    // Controls the simulation telemetry streaming tick
    fun startSimulationLoop() {
        simJob?.cancel()
        _isSimActive.value = true
        simJob = viewModelScope.launch {
            while (true) {
                delay(4000) // Insert simulated telemetry tick every 4 seconds
                if (_isSimActive.value) {
                    repository.addReading(
                        pressureDiffPa = _simPressurePa.value,
                        mq2ValuePpm = _simMq2Ppm.value,
                        wifiConnected = _simWifiConnected.value,
                        batteryVoltage = _simBatteryVoltage.value,
                        decreaseCylinderLevel = true
                    )
                    // Sync Firestore cache list
                    fetchFirestoreReadings()
                }
            }
        }
    }

    fun stopSimulationLoop() {
        _isSimActive.value = false
        simJob?.cancel()
    }

    // Setters for interactive sliders
    fun setSimPressurePa(value: Double) {
        _simPressurePa.value = value
    }

    fun setSimMq2Ppm(value: Double) {
        _simMq2Ppm.value = value
    }

    fun setSimWifiConnected(value: Boolean) {
        _simWifiConnected.value = value
        if (!value) {
            viewModelScope.launch {
                repository.addAlert(
                    type = "DISCONNECTED",
                    severity = "WARNING",
                    message = "WARNING: ESP32 telemetry pipeline has gone offline. Check local router credentials."
                )
            }
        }
    }

    fun setSimBatteryVoltage(value: Double) {
        _simBatteryVoltage.value = value
        if (value < 3.45) {
            viewModelScope.launch {
                repository.addAlert(
                    type = "BATTERY_LOW",
                    severity = "WARNING",
                    message = "WARNING: ESP32 battery level critically low (${String.format("%.2f", value)}V). Sensor readings may suffer drift."
                )
            }
        }
    }

    // UI actions
    fun setRole(role: String) {
        _currentRole.value = role
    }

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    fun setCompanyTab(tab: String) {
        _currentCompanyTab.value = tab
    }

    fun setAdminTab(tab: String) {
        _currentAdminTab.value = tab
    }

    fun login(email: String, passwordPlain: String, expectedRole: String, onSuccess: () -> Unit = {}) {
        _loginError.value = null
        viewModelScope.launch {
            val account = repository.getUserAccount(email.trim())
            if (account == null) {
                _loginError.value = "Account not found with this email."
                return@launch
            }
            if (account.passwordPlain != passwordPlain) {
                _loginError.value = "Incorrect password."
                return@launch
            }
            if (account.role != expectedRole) {
                _loginError.value = "Account is registered as ${account.role}, not $expectedRole."
                return@launch
            }
            
            // Successful Login
            _loggedInUser.value = account
            _currentRole.value = account.role
            _isLoggedIn.value = true
            
            // If they are a homeowner, update the UserProfile to display correct metadata
            if (account.role == "HOMEOWNER") {
                repository.fetchAndCacheUserProfile(account.email)
                val currentP = repository.getUserProfileDirect()
                if (currentP != null) {
                    repository.saveUserProfile(
                        currentP.copy(
                            name = account.name,
                            address = account.address,
                            email = account.email
                        )
                    )
                }
            }
            fetchFirestoreReadings()
            onSuccess()
        }
    }

    fun signup(name: String, email: String, passwordPlain: String, role: String, address: String = "", onSuccess: () -> Unit = {}) {
        _loginError.value = null
        if (name.isBlank() || email.isBlank() || passwordPlain.isBlank()) {
            _loginError.value = "Please fill in all fields."
            return
        }
        viewModelScope.launch {
            val existing = repository.getUserAccount(email.trim())
            if (existing != null) {
                _loginError.value = "An account with this email already exists."
                return@launch
            }
            val newAccount = UserAccount(
                email = email.trim(),
                passwordPlain = passwordPlain,
                role = role,
                name = name,
                address = address
            )
            repository.registerUserAccount(newAccount)
            
            // Auto-login after successful signup
            _loggedInUser.value = newAccount
            _currentRole.value = role
            _isLoggedIn.value = true
            
            if (role == "HOMEOWNER") {
                val currentP = repository.getUserProfileDirect() ?: com.example.data.UserProfile(id = "current_user")
                repository.saveUserProfile(
                    currentP.copy(
                        name = name,
                        address = address,
                        email = email.trim()
                    )
                )
            }
            fetchFirestoreReadings()
            onSuccess()
        }
    }

    fun logout() {
        _loggedInUser.value = null
        _isLoggedIn.value = false
        _loginError.value = null
        _currentTab.value = "DASHBOARD"
    }

    fun clearLoginError() {
        _loginError.value = null
    }

    fun resolveAlert(id: Long) {
        viewModelScope.launch {
            repository.resolveAlert(id)
        }
    }

    fun resolveAllAlerts() {
        viewModelScope.launch {
            repository.resolveAllAlerts()
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun resetCylinder() {
        viewModelScope.launch {
            val profile = repository.getUserProfileDirect()
            if (profile != null) {
                repository.saveUserProfile(profile.copy(currentGasLiters = profile.tankCapacityLiters))
            }
        }
    }

    // Admin Configurations
    fun setCalibrationK(kStr: String) {
        _calibrationKStr.value = kStr
        val kValue = kStr.toDoubleOrNull()
        if (kValue != null && kValue > 0) {
            viewModelScope.launch {
                val profile = repository.getUserProfileDirect()
                if (profile != null) {
                    repository.saveUserProfile(profile.copy(mpxCalibrationK = kValue))
                }
            }
        }
    }

    fun setThresholdPpm(ppmStr: String) {
        _thresholdPpmStr.value = ppmStr
        val ppmValue = ppmStr.toDoubleOrNull()
        if (ppmValue != null && ppmValue > 0) {
            viewModelScope.launch {
                val profile = repository.getUserProfileDirect()
                if (profile != null) {
                    repository.saveUserProfile(profile.copy(mq2ThresholdPpm = ppmValue))
                }
            }
        }
    }

    fun updateProfileSettings(name: String, address: String, capacity: Double, affiliatedCompanyEmail: String) {
        viewModelScope.launch {
            val profile = repository.getUserProfileDirect()
            if (profile != null) {
                repository.saveUserProfile(
                    profile.copy(
                        name = name,
                        address = address,
                        tankCapacityLiters = capacity,
                        affiliatedCompanyEmail = affiliatedCompanyEmail
                    )
                )
            }
        }
    }

    fun updateMeterId(meterId: String) {
        viewModelScope.launch {
            val profile = repository.getUserProfileDirect()
            if (profile != null) {
                repository.saveUserProfile(profile.copy(meterId = meterId))
            }
        }
    }

    fun updateIotConfig(meterId: String, projectId: String, apiKey: String) {
        viewModelScope.launch {
            val profile = repository.getUserProfileDirect()
            if (profile != null) {
                repository.saveUserProfile(
                    profile.copy(
                        meterId = meterId,
                        firebaseProjectId = projectId,
                        firebaseApiKey = apiKey
                    )
                )
            }
        }
    }

    private fun restartFirebaseListeners(email: String, role: String) {
        // Remove existing listeners
        try { firebaseNotifListener?.remove() } catch (e: Exception) { Log.e("Firebase", "Failed removing firebaseNotifListener: ${e.localizedMessage}") }
        try { emailAlertsListener?.remove() } catch (e: Exception) { Log.e("Firebase", "Failed removing emailAlertsListener: ${e.localizedMessage}") }
        try { firestoreReadingsListener?.remove() } catch (e: Exception) { Log.e("Firebase", "Failed removing firestoreReadingsListener: ${e.localizedMessage}") }
        
        firebaseNotifListener = null
        emailAlertsListener = null
        firestoreReadingsListener = null
        
        // Re-establish listeners with the newly configured Firebase/Firestore client context
        try {
            firebaseNotifListener = repository.listenToFirebaseNotifications { list ->
                _firebaseNotifications.value = list
            }
        } catch (e: Exception) {
            Log.e("Firebase", "Failed starting firebaseNotifListener: ${e.localizedMessage}")
        }

        try {
            emailAlertsListener = repository.listenToEmailAlerts { list ->
                _emailAlerts.value = list
            }
        } catch (e: Exception) {
            Log.e("Firebase", "Failed starting emailAlertsListener: ${e.localizedMessage}")
        }
        
        if (role == "HOMEOWNER") {
            try {
                viewModelScope.launch {
                    val profile = withContext(Dispatchers.IO) { repository.getUserProfileDirect() }
                    val meterId = profile?.meterId ?: ""
                    firestoreReadingsListener = repository.listenToFirestoreReadings(email, meterId) { reading ->
                        viewModelScope.launch {
                            repository.processIncomingReadingFromFirestore(reading)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Firebase", "Failed starting firestoreReadingsListener: ${e.localizedMessage}")
            }
        }
    }

    // Utility actions
    fun publishPromo(title: String, content: String, companyName: String, promoCode: String) {
        viewModelScope.launch {
            repository.addPromo(title, content, companyName, promoCode)
        }
    }

    fun manualInsertTelemetry() {
        viewModelScope.launch {
            repository.addReading(
                pressureDiffPa = _simPressurePa.value,
                mq2ValuePpm = _simMq2Ppm.value,
                wifiConnected = _simWifiConnected.value,
                batteryVoltage = _simBatteryVoltage.value,
                decreaseCylinderLevel = true
            )
            fetchFirestoreReadings()
        }
    }

    fun resolveFirebaseNotification(id: String) {
        viewModelScope.launch {
            repository.resolveFirebaseNotification(id)
        }
    }

    // --- Cylinder order operations ---
    fun placeCylinderOrder(tankId: String, slot: String, price: Double = 45.0) {
        val userEmail = _loggedInUser.value?.email ?: "homeowner@example.com"
        viewModelScope.launch {
            repository.placeCylinderOrder(
                CylinderOrder(
                    userEmail = userEmail,
                    tankId = tankId,
                    deliverySlot = slot,
                    status = "Placed",
                    price = price
                )
            )
        }
    }

    fun updateCylinderOrderStatus(orderId: Long, status: String, driver: String) {
        viewModelScope.launch {
            repository.updateCylinderOrderStatus(orderId, status, driver)
        }
    }

    // --- Billing & invoices ---
    fun createGasBill(userEmail: String, amount: Double, billingPeriod: String, dueDate: String) {
        viewModelScope.launch {
            repository.createGasBill(
                GasBill(
                    userEmail = userEmail,
                    amountDue = amount,
                    status = "UNPAID",
                    dueDate = dueDate,
                    billingPeriod = billingPeriod
                )
            )
        }
    }

    fun payGasBill(billId: Long, paymentMethod: String) {
        viewModelScope.launch {
            repository.payGasBill(billId, paymentMethod)
        }
    }

    // --- Service & support tickets ---
    fun submitServiceTicket(ticketType: String, description: String) {
        val userEmail = _loggedInUser.value?.email ?: "homeowner@example.com"
        viewModelScope.launch {
            repository.submitServiceTicket(
                ServiceTicket(
                    userEmail = userEmail,
                    ticketType = ticketType,
                    description = description,
                    status = "Open"
                )
            )
        }
    }

    fun submitServiceTicketForUser(userEmail: String, ticketType: String, description: String) {
        viewModelScope.launch {
            repository.submitServiceTicket(
                ServiceTicket(
                    userEmail = userEmail,
                    ticketType = ticketType,
                    description = description,
                    status = "Open"
                )
            )
        }
    }

    fun updateServiceTicketStatus(ticketId: Long, status: String, tech: String, notes: String) {
        viewModelScope.launch {
            repository.updateServiceTicketStatus(ticketId, status, tech, notes)
        }
    }

    // --- Multiple Connections / Meters ---
    fun addGasConnection(connectionId: String, name: String, capacity: Double, currentLiters: Double) {
        val userEmail = _loggedInUser.value?.email ?: "homeowner@example.com"
        viewModelScope.launch {
            repository.addGasConnection(
                GasConnection(
                    connectionId = connectionId,
                    userEmail = userEmail,
                    name = name,
                    tankCapacityLiters = capacity,
                    currentGasLiters = currentLiters
                )
            )
        }
    }

    fun removeGasConnection(connectionId: String) {
        viewModelScope.launch {
            repository.removeGasConnection(connectionId)
        }
    }

    // --- Supplier Direct Chat ---
    fun sendChatMessage(messageText: String, senderType: String) {
        val userEmail = "homeowner@example.com"
        val companyEmail = "company@example.com"
        val currentUser = _loggedInUser.value
        val actualUserEmail = if (currentUser?.role == "HOMEOWNER") currentUser.email else userEmail
        val actualCompanyEmail = if (currentUser?.role == "GAS_COMPANY") currentUser.email else companyEmail
        
        viewModelScope.launch {
            repository.sendChatMessage(
                SupplierChatMessage(
                    userEmail = actualUserEmail,
                    companyEmail = actualCompanyEmail,
                    senderType = senderType,
                    message = messageText
                )
            )
        }
    }

    // --- Auto-reorder configuration ---
    fun updateAutoReorderSettings(isEnabled: Boolean, thresholdPercent: Double) {
        viewModelScope.launch {
            val profile = userProfile.value
            if (profile != null) {
                repository.saveUserProfile(
                    profile.copy(
                        isAutoReorderEnabled = isEnabled,
                        autoReorderThresholdPercent = thresholdPercent
                    )
                )
            }
        }
    }

    fun switchActiveConnection(conn: GasConnection) {
        viewModelScope.launch {
            val profile = userProfile.value
            if (profile != null) {
                repository.saveUserProfile(
                    profile.copy(
                        currentGasLiters = conn.currentGasLiters,
                        tankCapacityLiters = conn.tankCapacityLiters
                    )
                )
            }
        }
    }

    // --- Administrative moderation ---
    fun updateAccountSuspendStatus(email: String, isSuspended: Boolean) {
        viewModelScope.launch {
            repository.updateAccountSuspendStatus(email, isSuspended)
        }
    }

    fun triggerEmergencySOS() {
        val userEmail = _loggedInUser.value?.email ?: "homeowner@example.com"
        viewModelScope.launch {
            repository.insertAuditLog(
                userEmail = userEmail,
                actionType = "EMERGENCY_SOS",
                details = "CRITICAL SOS triggered by homeowner. Automated notifications dispatched to 911 & Gas Company."
            )
            // Safety alarm trigger
            repository.addReading(
                pressureDiffPa = 1800.0,
                mq2ValuePpm = 1200.0, // leak threshold hazard
                wifiConnected = true,
                batteryVoltage = 3.88
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        simJob?.cancel()
        firebaseNotifListener?.remove()
        emailAlertsListener?.remove()
        firestoreReadingsListener?.remove()
    }
}
