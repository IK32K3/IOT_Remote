package com.example.iot.feature.control.test

import com.example.iot.core.ir.AcIrCatalog
import com.example.iot.core.ir.AcIrModel
import com.example.iot.core.ir.DvdIrCatalog
import com.example.iot.core.ir.FanIrCatalog
import com.example.iot.core.ir.ProjectorIrCatalog
import com.example.iot.core.ir.StbIrCatalog
import com.example.iot.core.ir.TvIrCatalog
import com.example.iot.core.mqtt.MqttTopics
import com.example.iot.domain.model.DeviceType
import org.json.JSONObject

/** Model thông tin một bộ mã IR dùng để kiểm thử */
data class CodeSetTestModel(
    val index: Int,
    val type: String,
    val label: String = type,
)

/**
 * Chiến lược xây dựng topic và payload MQTT cho nút "Power" trên màn hình CodeSetTest.
 */
sealed interface CodeSetTestStrategy {
    val deviceType: DeviceType

    fun topic(nodeId: String): String

    fun payload(brand: String, model: CodeSetTestModel): String
}

private object AcCodeSetTestStrategy : CodeSetTestStrategy {
    override val deviceType: DeviceType = DeviceType.AC

    override fun topic(nodeId: String): String = MqttTopics.cmdTopic(nodeId, "ac")

    override fun payload(brand: String, model: CodeSetTestModel): String =
        JSONObject().apply {
            put("cmd", "toggle")
            put("brand", brand)
            put("type", model.type)
            put("index", model.index)
        }.toString()
}

private class KeyCodeSetTestStrategy(
    override val deviceType: DeviceType,
) : CodeSetTestStrategy {
    override fun topic(nodeId: String): String =
        MqttTopics.cmdTopic(nodeId, deviceType.name.lowercase())

    override fun payload(brand: String, model: CodeSetTestModel): String =
        JSONObject().apply {
            put("cmd", "key")
            put("brand", brand)
            put("type", model.type)
            put("index", model.index)
            put("key", "POWER")
        }.toString()
}

object CodeSetTestStrategies {
    private val keyStrategies = mutableMapOf<DeviceType, KeyCodeSetTestStrategy>()

    fun strategyFor(type: DeviceType): CodeSetTestStrategy = when (type) {
        DeviceType.AC -> AcCodeSetTestStrategy
        else -> keyStrategies.getOrPut(type) { KeyCodeSetTestStrategy(type) }
    }
}

object CodeSetTestCatalog {
    fun modelsFor(deviceType: DeviceType, brand: String, fallbackLabel: String): List<CodeSetTestModel> {
        val label = fallbackLabel.ifBlank { deviceType.name }
        return when (deviceType) {
            DeviceType.AC -> AcIrCatalog.modelsFor(brand).map { it.toTestModel() }
            DeviceType.FAN -> FanIrCatalog.modelsFor(brand).map { CodeSetTestModel(it.index, it.type, it.label) }
            DeviceType.TV -> TvIrCatalog.modelsFor(brand).map { CodeSetTestModel(it.index, it.type, it.label) }
            DeviceType.DVD -> DvdIrCatalog.modelsFor(brand).map { CodeSetTestModel(it.index, it.type, it.label) }
            DeviceType.STB -> StbIrCatalog.modelsFor(brand).map { CodeSetTestModel(it.index, it.type, it.label) }
            DeviceType.PROJECTOR -> ProjectorIrCatalog.modelsFor(brand).map { CodeSetTestModel(it.index, it.type, it.label) }
            else -> defaultCodeSets(deviceType, label)
        }
    }

    private fun defaultCodeSets(deviceType: DeviceType, label: String): List<CodeSetTestModel> =
        (1..10).map { idx ->
            CodeSetTestModel(index = idx, type = deviceType.name, label = "$label $idx")
        }
}

private fun AcIrModel.toTestModel(): CodeSetTestModel = CodeSetTestModel(index, type, label)
