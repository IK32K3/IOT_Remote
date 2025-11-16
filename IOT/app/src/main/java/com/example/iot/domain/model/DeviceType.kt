package com.example.iot.domain.model

enum class DeviceType { AC, TV, FAN, STB, DVD;
    companion object {
        /** Chuyển từ chuỗi (vd "ac", "TV") về enum, mặc định là AC */
        fun from(value: String?): DeviceType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: AC

        fun fromLabel(value: String?): DeviceType {
            val normalized = value?.trim()?.lowercase() ?: return AC
            return when {
                normalized.contains("tv") || normalized.contains("tivi") -> TV
                normalized.contains("fan") || normalized.contains("quạt") -> FAN
                normalized.contains("stb") || normalized.contains("sat") -> STB
                normalized.contains("dvd") -> DVD
                else -> AC
            }
        }
    }
}   // có thể bổ sung thêm STB, DVD...