package com.example.iot.domain.model

data class FanState(
    val power: Boolean = false,
    val speed: Int = 0,           // 0..N
    val swing: Boolean = false,   // oscillation
    val type: String = "",        // normal/nature/sleep...
    val timerMin: Int = 0         // còn lại (phút)
)
