package com.example.simpledocumentscanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ScanDocumentEntity::class, ScanPageEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ScanDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao

    companion object {
        @Volatile
        private var instance: ScanDatabase? = null

        fun getInstance(context: Context): ScanDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScanDatabase::class.java,
                    "simple-document-scanner.db",
                ).build().also { instance = it }
            }
    }
}
