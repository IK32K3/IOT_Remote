package com.example.iot.domain.model

enum class DeviceType { AC, TV, FAN, STB, DVD;
    companion object {
        /** Chuyển từ chuỗi (vd "ac", "TV") về enum, mặc định là AC */
        fun from(value: String?): DeviceType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: AC
    }
}   // có thể bổ sung thêm STB, DVD...
