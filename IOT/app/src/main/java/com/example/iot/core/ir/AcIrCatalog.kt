package com.example.iot.core.ir

data class AcIrModel(val index: Int, val type: String, val label: String = type)

object AcIrCatalog {
    private val catalog: Map<String, List<AcIrModel>> = mapOf(
        "Daikin" to listOf(
            AcIrModel(1, "ARC480A1"),
            AcIrModel(2, "ARC423A5"),
            AcIrModel(3, "ARC433A46"),
            AcIrModel(4, "ARC433A70"),
            AcIrModel(5, "ARC452A1"),
            AcIrModel(6, "ARC452A2"),
            AcIrModel(7, "ARC466A21"),
            AcIrModel(8, "ARC480A5"),
            AcIrModel(9, "ARC433A1"),
            AcIrModel(10, "ARC433A18"),
            AcIrModel(11, "ARC477A1"),
        ),
        "LG" to listOf(
            AcIrModel(1, "AKB74955603"),
            AcIrModel(2, "AKB74955604"),
        ),
        "Mitsubishi" to listOf(
            AcIrModel(1, "MSZ-GL"),
            AcIrModel(2, "MSZ-GE"),
            AcIrModel(3, "MSZ-EF"),
            AcIrModel(4, "Heavy-88"),
            AcIrModel(5, "Heavy-152"),
        ),
        "Panasonic" to listOf(
            AcIrModel(1, "A75C3747"),
            AcIrModel(2, "A75C3748"),
            AcIrModel(3, "CZ-RD514C"),
            AcIrModel(4, "CZ-T056"),
        ),
        "Samsung" to listOf(
            AcIrModel(1, "DB93-14871C"),
            AcIrModel(2, "DB93-14974C"),
            AcIrModel(3, "DB93-16223A"),
            AcIrModel(4, "DB93-11489L"),
        ),
        "Sharp" to listOf(
            AcIrModel(1, "A903JB"),
            AcIrModel(2, "CRMC-A843JBEZ"),
        ),
        "Sony" to listOf(
            AcIrModel(1, "RM-AC001"),
        ),
        "TCL" to listOf(
            AcIrModel(1, "GZ-1002B"),
            AcIrModel(2, "GZ-1002A"),
        ),
        "Aqua" to listOf(
            AcIrModel(1, "AQV-RD"),
        ),
    )

    fun countFor(brand: String): Int = (catalog[brand]?.size ?: 0).takeIf { it > 0 } ?: 1

    fun modelsFor(brand: String): List<AcIrModel> = catalog[brand] ?: emptyList()
}