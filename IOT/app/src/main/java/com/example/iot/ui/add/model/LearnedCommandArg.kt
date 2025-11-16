package com.example.iot.ui.add.model

import android.os.Parcelable
import com.example.iot.domain.model.DeviceType
import kotlinx.parcelize.Parcelize

@Parcelize
data class LearnedCommandArg(
    val deviceType: DeviceType,
    val key: String,
    val protocol: String,
    val code: String,
    val bits: Int
) : Parcelable