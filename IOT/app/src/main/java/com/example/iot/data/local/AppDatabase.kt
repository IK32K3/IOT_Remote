package com.example.iot.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RemoteProfile::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun remoteDao(): RemoteDao
}
