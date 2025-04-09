package com.example.mc_a2.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FlightRecord::class, RouteStatisticEntity::class], version = 3, exportSchema = false)
abstract class FlightDatabase : RoomDatabase() {
    abstract fun flightRecordDao(): FlightRecordDao
    abstract fun routeStatisticDao(): RouteStatisticDao
    
    companion object {
        @Volatile
        private var INSTANCE: FlightDatabase? = null
        
        fun getDatabase(context: Context): FlightDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FlightDatabase::class.java,
                    "flight_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}