package com.example.iot.core.ir

import com.example.iot.domain.model.DeviceType

object BrandCatalog {
    fun brandsFor(deviceType: DeviceType): List<String> = when (deviceType) {
        DeviceType.AC -> listOf(
            "Daikin",
            "LG",
            "Mitsubishi",
            "Panasonic",
            "Samsung",
            "Sharp",
            "Sony",
            "TCL",
            "Aqua",
            "Gree",
            "Haier",
            "Midea",
        )
        DeviceType.FAN -> listOf(
            "LG",
            "Panasonic",
            "Samsung",
            "Midea",
            "Sharp",
            "Toshiba",
            "Mitsubishi",
            "Aqua",
            "Hatari",
            "Senko",
        )
        DeviceType.TV -> listOf(
            "LG",
            "Samsung",
            "Sony",
            "Panasonic",
            "Sharp",
            "Mitsubishi",
            "Philips",
            "Toshiba",
            "JVC",
            "Sanyo",
        )
        DeviceType.STB -> listOf(
            "Samsung",
            "Comcast",
            "Motorola",
            "General Instrument",
            "Jerrold",
            "Zinwell",
            "Novaplex",
        )
        DeviceType.DVD -> listOf(
            "LG",
            "Samsung",
            "Sony",
            "Panasonic",
            "Philips",
            "Yamaha",
            "Toshiba",
            "JVC",
            "Magnavox",
            "Memorex",
        )
        DeviceType.PROJECTOR -> listOf(
            "Epson",
            "InFocus",
            "BenQ",
            "Optoma",
            "Sony",
            "Hitachi",
            "Sanyo",
            "Sharp",
            "JVC",
            "Boxlight",
        )
    }
}
