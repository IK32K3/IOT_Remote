package com.example.iot.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_profiles")
data class RemoteProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val room: String,
    val brand: String,
    val type: String,
    val nodeId: String,
    val codeSetIndex: Int,
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(defaultValue = "AC")
    val deviceType: String = "AC"   // lưu bằng enum.name
)