package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: String = "current_user",
    val role: String = "HOMEOWNER", // "HOMEOWNER", "GAS_COMPANY", "ADMIN"
    val name: String = "John Doe",
    val email: String = "john.doe@example.com",
    val address: String = "123 Smart Grid Lane, Sector 7",
    val meterId: String = "MTR-MPX-7002-99B",
    val tankCapacityLiters: Double = 120.0, // Total liters when full
    val currentGasLiters: Double = 84.5, // Current remaining liters
    val mq2ThresholdPpm: Double = 700.0, // Alert threshold for MQ-2 leak detection
    val mpxCalibrationK: Double = 0.45, // Orifice flow constant K: Q = K * sqrt(delta_p)
    val affiliatedCompanyEmail: String = "company@example.com", // Email of affiliated utility company
    val isAutoReorderEnabled: Boolean = false,
    val autoReorderThresholdPercent: Double = 20.0,
    val isSuspended: Boolean = false,
    val firebaseProjectId: String = "smartgasmonitor-vbtqkx",
    val firebaseApiKey: String = "AIzaSyFakeKeyForLocalInitialization_SmartGasMonitor"
)

@Entity(tableName = "gas_readings")
data class GasReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val pressureDiffPa: Double, // Delta P from MPXV7002DP (-2000 to +2000 Pa)
    val calculatedFlowRate: Double, // Flow rate in L/min, calculated: K * sqrt(|delta_P|)
    val mq2ValuePpm: Double, // Smoke/LPG concentration from MQ-2 (PPM)
    val isLeakDetected: Boolean, // True if MQ-2 PPM > threshold
    val wifiConnected: Boolean, // ESP32 status
    val batteryVoltage: Double // ESP32 telemetry
)

@Entity(tableName = "alert_records")
data class AlertRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val alertType: String, // "LEAK_DANGER", "OVERPRESSURE", "BATTERY_LOW", "DISCONNECTED"
    val severity: String, // "CRITICAL", "WARNING"
    val message: String,
    val isResolved: Boolean = false,
    val resolvedTimestamp: Long? = null
)

@Entity(tableName = "gas_promos")
data class GasPromo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val content: String,
    val companyName: String,
    val promoCode: String = ""
)

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey val email: String,
    val passwordPlain: String,
    val role: String, // "HOMEOWNER", "GAS_COMPANY", "ADMIN"
    val name: String,
    val address: String = "",
    val isSuspended: Boolean = false
)

data class FirebaseNotification(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val userEmail: String = "",
    val mq2ValuePpm: Double = 0.0,
    val thresholdPpm: Double = 700.0,
    val message: String = "",
    val isResolved: Boolean = false
)

@Entity(tableName = "email_alerts")
data class EmailAlertRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val recipientEmail: String,
    val recipientType: String, // "HOMEOWNER" or "GAS_COMPANY"
    val subject: String,
    val body: String,
    val status: String = "SENT", // "PENDING", "SENT", "DELIVERED"
    val smtpLog: String = ""
)

@Entity(tableName = "cylinder_orders")
data class CylinderOrder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val userEmail: String,
    val tankId: String,
    val deliverySlot: String, // e.g., "Morning (8 AM - 12 PM)", "Afternoon (1 PM - 5 PM)"
    val status: String, // "Placed", "Dispatched", "Delivered"
    val assignedDriver: String = "",
    val price: Double = 45.0
)

@Entity(tableName = "gas_bills")
data class GasBill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val userEmail: String,
    val amountDue: Double,
    val status: String, // "PAID", "UNPAID"
    val dueDate: String,
    val billingPeriod: String,
    val paymentMethod: String = ""
)

@Entity(tableName = "service_tickets")
data class ServiceTicket(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val userEmail: String,
    val ticketType: String, // "Gas Leak Check", "Technician Visit", "Meter Calibration", "General Complaint"
    val description: String,
    val status: String, // "Open", "Assigned", "Resolved"
    val assignedTechnician: String = "",
    val resolutionNotes: String = ""
)

@Entity(tableName = "gas_connections")
data class GasConnection(
    @PrimaryKey val connectionId: String, // e.g. "CON-9831"
    val userEmail: String,
    val name: String, // "Main Kitchen", "Rental Unit A", "Outdoor Grill"
    val tankCapacityLiters: Double,
    val currentGasLiters: Double,
    val status: String = "ACTIVE"
)

@Entity(tableName = "supplier_chat")
data class SupplierChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val userEmail: String,
    val companyEmail: String,
    val senderType: String, // "HOMEOWNER" or "GAS_COMPANY"
    val message: String
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val userEmail: String,
    val actionType: String, // "CONFIG_CHANGE", "USER_BANNED", "TICKET_RESOLUTION", "EMERGENCY_SOS"
    val details: String
)
