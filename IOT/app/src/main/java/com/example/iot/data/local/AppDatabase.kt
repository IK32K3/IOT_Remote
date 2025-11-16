package com.example.iot.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RemoteProfile::class, LearnedCommandEntity::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun remoteDao(): RemoteDao
    abstract fun learnedCommandDao(): LearnedCommandDao
}