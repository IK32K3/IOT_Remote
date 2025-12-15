package com.example.iot.core.ir

data class ProjectorIrModel(
    val index: Int,
    val type: String = "PROJECTOR",
    val label: String,
)

object ProjectorIrCatalog {
    private val genericModels: List<ProjectorIrModel> = listOf(
        ProjectorIrModel(4001, label = "Try: InFocus Projector (NEC 32-bit)"),
        ProjectorIrModel(4002, label = "Try: Epson Projector (NEC 32-bit)"),
        ProjectorIrModel(4003, label = "Try: BenQ Projector (NEC 32-bit)"),
        ProjectorIrModel(4004, label = "Try: Optoma Projector (NEC 32-bit)"),
        ProjectorIrModel(4005, label = "Try: Sony Projector (SIRC 15-bit)"),
        ProjectorIrModel(4006, label = "Try: Hitachi Projector (NEC 32-bit)"),
        ProjectorIrModel(4007, label = "Try: Sanyo Projector (NEC 32-bit)"),
        ProjectorIrModel(4008, label = "Try: Sharp Projector (Sharp 15-bit)"),
        ProjectorIrModel(4009, label = "Try: JVC Projector (JVC 16-bit)"),
        ProjectorIrModel(4010, label = "Try: Boxlight Projector (NEC 32-bit)"),
    )

    private val catalog: Map<String, List<ProjectorIrModel>> = mapOf(
        "InFocus" to listOf(ProjectorIrModel(1, label = "InFocus Projector (NEC 32-bit)")),
        "Epson" to listOf(ProjectorIrModel(2, label = "Epson Projector (NEC 32-bit)")),
        "BenQ" to listOf(ProjectorIrModel(3, label = "BenQ Projector (NEC 32-bit)")),
        "Optoma" to listOf(ProjectorIrModel(4, label = "Optoma Projector (NEC 32-bit)")),
        "Sony" to listOf(ProjectorIrModel(5, label = "Sony Projector (SIRC 15-bit)")),
        "Hitachi" to listOf(ProjectorIrModel(6, label = "Hitachi Projector (NEC 32-bit)")),
        "Sanyo" to listOf(ProjectorIrModel(7, label = "Sanyo Projector (NEC 32-bit)")),
        "Sharp" to listOf(ProjectorIrModel(8, label = "Sharp Projector (Sharp 15-bit)")),
        "JVC" to listOf(ProjectorIrModel(9, label = "JVC Projector (JVC 16-bit)")),
        "Boxlight" to listOf(ProjectorIrModel(10, label = "Boxlight Projector (NEC 32-bit)")),
    )

    fun modelsFor(brand: String): List<ProjectorIrModel> =
        (catalog[brand] ?: emptyList()) + genericModels
}

