package com.example.iot.core.ir

data class StbIrModel(
    val index: Int,
    val type: String = "STB",
    val label: String,
)

object StbIrCatalog {
    private val genericModels: List<StbIrModel> = listOf(
        StbIrModel(3001, label = "Try: Samsung STB (NEC 32-bit)"),
        StbIrModel(3002, label = "Try: Cable Box (GICABLE 16-bit)"),
    )

    private val catalog: Map<String, List<StbIrModel>> = mapOf(
        "Samsung" to listOf(
            StbIrModel(1, label = "Samsung STB (NEC 32-bit - BN59-00603A-STB)"),
        ),
        "Comcast" to listOf(
            StbIrModel(1, label = "Cable Box (GICABLE 16-bit - Comcast)"),
        ),
        "Motorola" to listOf(
            StbIrModel(1, label = "Cable Box (GICABLE 16-bit - Motorola)"),
        ),
        "General Instrument" to listOf(
            StbIrModel(1, label = "Cable Box (GICABLE 16-bit - General Instrument)"),
        ),
        "Jerrold" to listOf(
            StbIrModel(1, label = "Cable Box (GICABLE 16-bit - Jerrold)"),
        ),
        "Zinwell" to listOf(
            StbIrModel(1, label = "Cable Box (GICABLE 16-bit - Zinwell)"),
        ),
        "Novaplex" to listOf(
            StbIrModel(1, label = "Cable Box (GICABLE 16-bit - Novaplex)"),
        ),
    )

    fun modelsFor(brand: String): List<StbIrModel> = (catalog[brand] ?: emptyList()) + genericModels
}

