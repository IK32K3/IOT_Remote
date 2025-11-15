package com.example.iot.domain.model

enum class DeviceType { AC, TV, FAN, STB, DVD;
    companion object {
        /** Chuyển từ chuỗi (vd "ac", "TV") hoặc nhãn hiển thị về enum, mặc định là AC */
        fun from(value: String?): DeviceType {
            val raw = value?.trim().orEmpty()
            if (raw.isEmpty()) return AC

            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }?.let { return it }

            return when (raw.lowercase()) {
                "tivi" -> TV
                "máy lạnh", "dieu hoa", "máy điều hòa" -> AC
                "quạt điện", "quat dien" -> FAN
                "stb/sat", "sat", "set-top-box" -> STB
                else -> AC
            }
        }
    }
}   // có thể bổ sung thêm STB, DVD...