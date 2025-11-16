package com.example.iot.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "learned_commands",
    indices = [Index(value = ["remoteId", "key"], unique = true)]
)
data class LearnedCommandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: Long,
    val deviceType: String,
    val key: String,
    val protocol: String,
    val code: String,
    val bits: Int,
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis()
)