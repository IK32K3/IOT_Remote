package com.example.iot.domain.model

data class LearnedCommandDraft(
    val deviceType: DeviceType,
    val key: String,
    val protocol: String,
    val code: String,
    val bits: Int
)