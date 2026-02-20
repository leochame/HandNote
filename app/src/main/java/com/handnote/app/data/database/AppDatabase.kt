package com.handnote.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.handnote.app.data.dao.*
import com.handnote.app.data.entity.*

@Database(
    entities = [
        ShiftRule::class,
        Anniversary::class,
        TaskRecord::class,
        Post::class,
        Holiday::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shiftRuleDao(): ShiftRuleDao
    abstract fun anniversaryDao(): AnniversaryDao
    abstract fun taskRecordDao(): TaskRecordDao
    abstract fun postDao(): PostDao
    abstract fun holidayDao(): HolidayDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = INSTANCE ?: try {
                    android.util.Log.d("AppDatabase", "Creating database instance...")
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "handnote_database"
                    )
                        .fallbackToDestructiveMigration() // 如果 schema 不匹配，重建数据库
                        .build()
                        .also {
                            android.util.Log.d("AppDatabase", "Database instance created successfully")
                            INSTANCE = it
                        }
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Failed to create database", e)
                    throw e
                }
                instance
            }
        }
    }
}

