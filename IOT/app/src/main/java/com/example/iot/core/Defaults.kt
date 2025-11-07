package com.example.iot.core

object Defaults {
    const val BROKER_HOST: String = "127.0.0.1"
    const val BROKER_PORT: Int = 1883
    const val NODE_ID: String = "esp-remote"

    val BROKER_URL: String get() = "tcp://$BROKER_HOST:$BROKER_PORT"
}