
package com.example.iot.domain.model

data class LearnedCommand(
    val remoteId: Long,
    val deviceType: DeviceType,
    val key: String,
    val protocol: String,
    val code: String,
    val bits: Int
)