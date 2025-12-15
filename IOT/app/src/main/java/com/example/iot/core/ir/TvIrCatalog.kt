package com.example.iot.core.ir

data class TvIrModel(
    val index: Int,
    val type: String = "TV",
    val label: String,
)

object TvIrCatalog {
    private val genericModels: List<TvIrModel> = listOf(
        TvIrModel(1001, label = "Try: Samsung TV (SAMSUNG 32-bit)"),
        TvIrModel(1002, label = "Try: Samsung TV (alt)"),
        TvIrModel(1003, label = "Try: LG TV (NEC 32-bit)"),
        TvIrModel(1004, label = "Try: Sony TV (SIRC 12-bit)"),
        TvIrModel(1005, label = "Try: Sony TV (SIRC 15-bit)"),
        TvIrModel(1006, label = "Try: Sony TV (SIRC 20-bit)"),
        TvIrModel(1007, label = "Try: Panasonic TV (Panasonic 48-bit)"),
        TvIrModel(1008, label = "Try: Sharp TV (Sharp 15-bit)"),
        TvIrModel(1009, label = "Try: Mitsubishi TV (Sharp 15-bit)"),
        TvIrModel(1012, label = "Try: Toshiba TV (NEC 32-bit)"),
        TvIrModel(1013, label = "Try: Philips TV (RC5 12-bit)"),
        TvIrModel(1014, label = "Try: JVC TV (JVC 16-bit)"),
        TvIrModel(1015, label = "Try: Sanyo TV (NEC 32-bit)"),
    )

    private val catalog: Map<String, List<TvIrModel>> = mapOf(
        "Samsung" to listOf(
            TvIrModel(1, label = "Samsung TV (SAMSUNG 32-bit)"),
            TvIrModel(2, label = "Samsung TV (alt)"),
        ),
        "LG" to listOf(
            TvIrModel(1, label = "LG TV (NEC 32-bit)"),
            TvIrModel(2, label = "LG TV (LG 28-bit)"),
            TvIrModel(3, label = "LG TV (LG 32-bit)"),
        ),
        "Sony" to listOf(
            TvIrModel(1, label = "Sony TV (SIRC 12-bit)"),
            TvIrModel(2, label = "Sony TV (SIRC 15-bit)"),
            TvIrModel(3, label = "Sony TV (SIRC 20-bit)"),
        ),
        "Panasonic" to listOf(
            TvIrModel(1, label = "Panasonic TV (Panasonic 48-bit)"),
        ),
        "Sharp" to listOf(
            TvIrModel(1, label = "Sharp TV (Sharp 15-bit)"),
        ),
        "Mitsubishi" to listOf(
            TvIrModel(1, label = "Mitsubishi TV (Sharp 15-bit)"),
        ),
        "Toshiba" to listOf(
            TvIrModel(1, label = "Toshiba TV (NEC 32-bit)"),
        ),
        "Philips" to listOf(
            TvIrModel(1, label = "Philips TV (RC5 12-bit)"),
        ),
        "JVC" to listOf(
            TvIrModel(1, label = "JVC TV (JVC 16-bit)"),
        ),
        "Sanyo" to listOf(
            TvIrModel(1, label = "Sanyo TV (NEC 32-bit)"),
        ),
    )

    fun modelsFor(brand: String): List<TvIrModel> = (catalog[brand] ?: emptyList()) + genericModels
}

