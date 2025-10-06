package com.example.iot.domain.model

data class TvState(
    val power: Boolean = false,
    val muted: Boolean = false,
    val volume: Int = 0,
    val channel: Int = 1,
    val input: String = ""
)
