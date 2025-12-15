package com.example.iot.core.ir

data class DvdIrModel(
    val index: Int,
    val type: String = "DVD",
    val label: String,
)

object DvdIrCatalog {
    private val genericModels: List<DvdIrModel> = listOf(
        DvdIrModel(2001, label = "Try: LG BD/DVD (NEC 32-bit)"),
        DvdIrModel(2002, label = "Try: Samsung DVD (NEC 32-bit)"),
        DvdIrModel(2003, label = "Try: Sony DVD (SIRC 20-bit)"),
        DvdIrModel(2004, label = "Try: Sony DVD (SIRC 12-bit)"),
        DvdIrModel(2005, label = "Try: Panasonic DVD (Panasonic 48-bit)"),
        DvdIrModel(2006, label = "Try: Philips DVD (RC6 20-bit)"),
        DvdIrModel(2007, label = "Try: Toshiba DVD (NEC 32-bit)"),
        DvdIrModel(2008, label = "Try: JVC DVD (JVC 16-bit)"),
        DvdIrModel(2009, label = "Try: Yamaha DVD (NEC 32-bit)"),
        DvdIrModel(2010, label = "Try: Magnavox DVD (NEC 32-bit)"),
        DvdIrModel(2011, label = "Try: Memorex DVD (NEC 32-bit)"),
    )

    private val catalog: Map<String, List<DvdIrModel>> = mapOf(
        "LG" to listOf(
            DvdIrModel(1, label = "LG BD/DVD (NEC 32-bit - BD300)"),
        ),
        "Samsung" to listOf(
            DvdIrModel(1, label = "Samsung DVD (NEC 32-bit - SV-DVD3E)"),
        ),
        "Sony" to listOf(
            DvdIrModel(1, label = "Sony DVD (SIRC 20-bit - RMT-V501A)"),
            DvdIrModel(2, label = "Sony DVD (SIRC 12-bit - RMT-V181N)"),
        ),
        "Panasonic" to listOf(
            DvdIrModel(1, label = "Panasonic DVD (Panasonic 48-bit)"),
        ),
        "Philips" to listOf(
            DvdIrModel(1, label = "Philips DVD (RC6 20-bit)"),
        ),
        "Toshiba" to listOf(
            DvdIrModel(1, label = "Toshiba DVD (NEC 32-bit)"),
        ),
        "JVC" to listOf(
            DvdIrModel(1, label = "JVC DVD (JVC 16-bit)"),
        ),
        "Yamaha" to listOf(
            DvdIrModel(1, label = "Yamaha DVD (NEC 32-bit)"),
        ),
        "Magnavox" to listOf(
            DvdIrModel(1, label = "Magnavox DVD (NEC 32-bit)"),
        ),
        "Memorex" to listOf(
            DvdIrModel(1, label = "Memorex DVD (NEC 32-bit)"),
        ),
    )

    fun modelsFor(brand: String): List<DvdIrModel> = (catalog[brand] ?: emptyList()) + genericModels
}

