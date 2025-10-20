package com.example.iot.domain.model

data class StbState(
    val power: Boolean = false,
    val muted: Boolean = false,
    val lastKey: String? = null,
    val hint: String? = null
)
