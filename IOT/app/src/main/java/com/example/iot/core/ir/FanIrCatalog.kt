package com.example.iot.core.ir

data class FanIrModel(
    val index: Int,
    val type: String = "FAN",
    val label: String,
)

object FanIrCatalog {
    private val genericModels: List<FanIrModel> = listOf(
        FanIrModel(1, label = "Try: Generic Fan (NEC 32-bit)"),
        FanIrModel(2, label = "Try: Fan (LG/NEC 32-bit)"),
        FanIrModel(3, label = "Try: Fan (Panasonic 48-bit)"),
        FanIrModel(4, label = "Try: Fan (Mitsubishi 24-bit)"),
        FanIrModel(5, label = "Try: Fan (Samsung 12-bit)"),
        FanIrModel(6, label = "Try: Fan (Sharp 15-bit)"),
        FanIrModel(7, label = "Try: Fan (Toshiba/NEC 32-bit)"),
    )

    private val genericModelsWithoutIndex1: List<FanIrModel> = genericModels.filter { it.index != 1 }

    private val catalog: Map<String, List<FanIrModel>> = mapOf(
        "LG" to listOf(FanIrModel(1, label = "LG Fan (NEC 32-bit)")),
        "Panasonic" to listOf(FanIrModel(1, label = "Panasonic Fan (Panasonic 48-bit)")),
        "Mitsubishi" to listOf(FanIrModel(1, label = "Mitsubishi Fan (Mitsubishi 24-bit)")),
        "Samsung" to listOf(FanIrModel(1, label = "Samsung Fan (Samsung 12-bit)")),
        "Sharp" to listOf(FanIrModel(1, label = "Sharp Fan (Sharp 15-bit)")),
        "Toshiba" to listOf(FanIrModel(1, label = "Toshiba Fan (NEC 32-bit)")),
    )

    fun modelsFor(brand: String): List<FanIrModel> {
        val curated = catalog[brand] ?: emptyList()
        return if (curated.isEmpty()) {
            genericModels
        } else {
            curated + genericModelsWithoutIndex1
        }
    }
}

