package com.example.iot.core.mqtt

import com.example.iot.core.Defaults

object MqttTopics {
    fun nodeStatus(nodeId: String): String =
        "iot/nodes/${nodeId.ifBlank { Defaults.NODE_ID }}/status"

    fun nodeCommandTopic(nodeId: String): String =
        "iot/nodes/${nodeId.ifBlank { Defaults.NODE_ID }}/commands"

    fun testIrTopic(nodeId: String): String =
        "iot/nodes/${nodeId.ifBlank { Defaults.NODE_ID }}/ir/test"

    fun learnRequestTopic(nodeId: String): String =
        "iot/nodes/${nodeId.ifBlank { Defaults.NODE_ID }}/ir/learn/cmd"

    fun learnResultTopic(nodeId: String): String =
        "iot/nodes/${nodeId.ifBlank { Defaults.NODE_ID }}/ir/learn"

    fun emitIrTopic(nodeId: String): String =
        "iot/nodes/${nodeId.ifBlank { Defaults.NODE_ID }}/ir/emit"

    fun stateTopic(nodeId: String, device: String): String =
        "iot/nodes/${nodeId.ifBlank { Defaults.NODE_ID }}/${device.lowercase()}/state"

    fun cmdTopic(nodeId: String, device: String): String =
        "iot/nodes/${nodeId.ifBlank { Defaults.NODE_ID }}/${device.lowercase()}/cmd"
}