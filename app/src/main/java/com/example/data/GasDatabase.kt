package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserProfile::class,
        GasReading::class,
        AlertRecord::class,
        GasPromo::class,
        UserAccount::class,
        EmailAlertRecord::class,
        CylinderOrder::class,
        GasBill::class,
        ServiceTicket::class,
        GasConnection::class,
        SupplierChatMessage::class,
        AuditLog::class
    ],
    version = 5,
    exportSchema = false
)
abstract class GasDatabase : RoomDatabase() {
    abstract fun gasDao(): GasDao

    companion object {
        @Volatile
        private var INSTANCE: GasDatabase? = null

        fun getDatabase(context: Context): GasDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GasDatabase::class.java,
                    "smart_gas_monitor_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
