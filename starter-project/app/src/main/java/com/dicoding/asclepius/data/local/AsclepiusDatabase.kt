package com.dicoding.asclepius.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PredictionHistory::class], version = 2, exportSchema = false)
abstract class AsclepiusDatabase : RoomDatabase() {

    abstract fun predictionHistoryDao(): AsclepiusDao

    companion object {
        @Volatile
        private var INSTANCE: AsclepiusDatabase? = null

        fun getDatabase(context: Context): AsclepiusDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AsclepiusDatabase::class.java,
                    "app_database"
                )
//                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}