package com.example.iot.core.mqtt

import com.example.iot.core.Defaults

object MqttTopics {
    fun nodeStatus(nodeId: String): String =
        "iot/nodes/${nodeId.ifBlank { Defaults.NODE_ID }}/status"

    fun testIrTopic(nodeId: String): String =
        "iot/nodes/${nodeId.ifBlank { Defaults.NODE_ID }}/ir/test"

    fun stateTopic(nodeId: String, device: String): String =
        "iot/nodes/${nodeId.ifBlank { Defaults.NODE_ID }}/${device.lowercase()}/state"

    fun cmdTopic(nodeId: String, device: String): String =
        "iot/nodes/${nodeId.ifBlank { Defaults.NODE_ID }}/${device.lowercase()}/cmd"
}