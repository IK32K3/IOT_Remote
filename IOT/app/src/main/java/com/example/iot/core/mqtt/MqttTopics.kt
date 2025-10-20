package com.example.iot.core.mqtt
object MqttTopics {
    const val NODE_STATUS = "iot/nodes/+/status"
    fun testIrTopic(nodeId: String) = "iot/nodes/$nodeId/ir/test"

    fun stateTopic(nodeId: String, device: String) =
        "iot/nodes/$nodeId/${device.lowercase()}/state"
    fun cmdTopic(nodeId: String, device: String) =
        "iot/nodes/$nodeId/${device.lowercase()}/cmd"

}
