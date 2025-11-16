package com.example.iot.domain.model

data class IrLearningEvent(
    val device: DeviceType,
    val key: String,
    val success: Boolean,
    val protocol: String? = null,
    val code: String? = null,
    val bits: Int? = null,
    val error: String? = null
)