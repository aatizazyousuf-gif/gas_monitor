package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GasDao {
    // --- User Profile ---
    @Query("SELECT * FROM user_profiles WHERE id = 'current_user' LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE id = 'current_user' LIMIT 1")
    suspend fun getUserProfileDirect(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    @Update
    suspend fun updateUserProfile(profile: UserProfile)

    // --- Gas Readings ---
    @Query("SELECT * FROM gas_readings ORDER BY timestamp DESC")
    fun getAllReadingsFlow(): Flow<List<GasReading>>

    @Query("SELECT * FROM gas_readings ORDER BY timestamp DESC LIMIT 100")
    suspend fun getRecentReadingsDirect(): List<GasReading>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: GasReading)

    @Query("DELETE FROM gas_readings")
    suspend fun clearAllReadings()

    // --- Alerts & Safety ---
    @Query("SELECT * FROM alert_records ORDER BY timestamp DESC")
    fun getAllAlertsFlow(): Flow<List<AlertRecord>>

    @Query("SELECT * FROM alert_records WHERE isResolved = 0 ORDER BY timestamp DESC")
    fun getActiveAlertsFlow(): Flow<List<AlertRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertRecord)

    @Update
    suspend fun updateAlert(alert: AlertRecord)

    @Query("UPDATE alert_records SET isResolved = 1, resolvedTimestamp = :timestamp WHERE id = :id")
    suspend fun resolveAlert(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE alert_records SET isResolved = 1, resolvedTimestamp = :timestamp WHERE isResolved = 0")
    suspend fun resolveAllActiveAlerts(timestamp: Long = System.currentTimeMillis())

    // --- Promotions ---
    @Query("SELECT * FROM gas_promos ORDER BY timestamp DESC")
    fun getAllPromosFlow(): Flow<List<GasPromo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromo(promo: GasPromo)

    // --- User Accounts ---
    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getUserAccount(email: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAccount(account: UserAccount)

    // --- Email Alerts ---
    @Query("SELECT * FROM email_alerts ORDER BY timestamp DESC")
    fun getAllEmailAlertsFlow(): Flow<List<EmailAlertRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmailAlert(emailAlert: EmailAlertRecord)

    @Query("DELETE FROM email_alerts")
    suspend fun clearAllEmailAlerts()

    // --- Cylinder Orders ---
    @Query("SELECT * FROM cylinder_orders ORDER BY timestamp DESC")
    fun getAllOrdersFlow(): Flow<List<CylinderOrder>>

    @Query("SELECT * FROM cylinder_orders WHERE userEmail = :email ORDER BY timestamp DESC")
    fun getOrdersForUserFlow(email: String): Flow<List<CylinderOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: CylinderOrder)

    @Query("UPDATE cylinder_orders SET status = :status, assignedDriver = :driver WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Long, status: String, driver: String)

    // --- Gas Bills ---
    @Query("SELECT * FROM gas_bills ORDER BY timestamp DESC")
    fun getAllBillsFlow(): Flow<List<GasBill>>

    @Query("SELECT * FROM gas_bills WHERE userEmail = :email ORDER BY timestamp DESC")
    fun getBillsForUserFlow(email: String): Flow<List<GasBill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: GasBill)

    @Query("UPDATE gas_bills SET status = :status, paymentMethod = :method WHERE id = :billId")
    suspend fun updateBillStatus(billId: Long, status: String, method: String)

    // --- Service Tickets ---
    @Query("SELECT * FROM service_tickets ORDER BY timestamp DESC")
    fun getAllServiceTicketsFlow(): Flow<List<ServiceTicket>>

    @Query("SELECT * FROM service_tickets WHERE userEmail = :email ORDER BY timestamp DESC")
    fun getServiceTicketsForUserFlow(email: String): Flow<List<ServiceTicket>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceTicket(ticket: ServiceTicket)

    @Query("UPDATE service_tickets SET status = :status, assignedTechnician = :tech, resolutionNotes = :notes WHERE id = :ticketId")
    suspend fun updateServiceTicketStatus(ticketId: Long, status: String, tech: String, notes: String)

    // --- Gas Connections / Secondary Tanks ---
    @Query("SELECT * FROM gas_connections ORDER BY connectionId ASC")
    fun getAllConnectionsFlow(): Flow<List<GasConnection>>

    @Query("SELECT * FROM gas_connections WHERE userEmail = :email ORDER BY connectionId ASC")
    fun getConnectionsForUserFlow(email: String): Flow<List<GasConnection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnection(connection: GasConnection)

    @Query("DELETE FROM gas_connections WHERE connectionId = :id")
    suspend fun deleteConnection(id: String)

    // --- Supplier Chat ---
    @Query("SELECT * FROM supplier_chat ORDER BY timestamp ASC")
    fun getAllChatMessagesFlow(): Flow<List<SupplierChatMessage>>

    @Query("SELECT * FROM supplier_chat WHERE userEmail = :userEmail OR companyEmail = :companyEmail ORDER BY timestamp ASC")
    fun getChatMessagesFlow(userEmail: String, companyEmail: String): Flow<List<SupplierChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(msg: SupplierChatMessage)

    // --- Audit Logs ---
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogsFlow(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog)

    // --- Account Moderation (Admin) ---
    @Query("SELECT * FROM user_accounts ORDER BY email ASC")
    fun getAllAccountsFlow(): Flow<List<UserAccount>>

    @Query("UPDATE user_accounts SET isSuspended = :isSuspended WHERE email = :email")
    suspend fun updateAccountSuspendStatus(email: String, isSuspended: Boolean)
}

