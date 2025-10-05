package com.example.iot.ui.home

import com.example.iot.domain.model.DeviceType

data class RemoteCardUi(
    val id: String,
    val name: String,
    val room: String,
    val nodeId: String,
    val brand: String,
    val type: String,
    val codeSetIndex: Int,
    val deviceType: DeviceType,
    val online: Boolean
)
