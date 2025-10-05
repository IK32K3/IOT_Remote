package com.example.iot.domain.model

data class AcState(
    val power: Boolean = false,
    val mode: String = "cool",
    val temp: Int = 24,
    val fan: String = "auto"
)
