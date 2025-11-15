package com.example.iot.feature.control.test

import com.example.iot.core.ir.AcIrCatalog
import com.example.iot.core.ir.AcIrModel
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
        val models = when (deviceType) {
            DeviceType.AC -> AcIrCatalog.modelsFor(brand).map { it.toTestModel() }
            else -> emptyList()
        }
        return models.ifEmpty { listOf(defaultModel(deviceType, label)) }
    }

    private fun defaultModel(deviceType: DeviceType, label: String): CodeSetTestModel =
        CodeSetTestModel(index = 1, type = deviceType.name, label = label)
}

private fun AcIrModel.toTestModel(): CodeSetTestModel = CodeSetTestModel(index, type, label)