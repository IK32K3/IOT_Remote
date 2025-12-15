package com.example.iot.data

import com.example.iot.core.mqtt.MqttConnectionManager
import com.example.iot.core.mqtt.MqttTopics
import com.example.iot.domain.model.AcState
import com.example.iot.domain.model.DeviceType
import com.example.iot.domain.model.IrLearningEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MqttRepository @Inject constructor(
    private val conn: MqttConnectionManager
) {

    fun espOnline(): StateFlow<Boolean> = conn.anyNodeOnline
    fun publish(topic: String, payload: String) = conn.publish(topic, payload)

    fun nodesOnline(): StateFlow<Map<String, Boolean>> = conn.nodesOnline

    private val incoming = conn.incoming

    // Chỉ điều hòa cần state; các thiết bị còn lại chỉ gửi lệnh.
    fun observeAcState(nodeId: String): Flow<AcState> =
        incoming
            .filter { (t, _) -> t == MqttTopics.stateTopic(nodeId, "ac") }
            .map { (_, payload) ->
                try {
                    val j = JSONObject(payload)
                    AcState(
                        power = j.optBoolean("power", false),
                        mode = j.optString("mode", "cool"),
                        temp = j.optInt("temp", 24),
                        fan = j.optString("fan", "auto")
                    )
                } catch (_: Exception) {
                    AcState()
                }
            }

    fun observeIrLearning(nodeId: String): Flow<IrLearningEvent> =
        incoming
            .filter { (t, _) -> t == MqttTopics.learnResultTopic(nodeId) }
            .mapNotNull { (_, payload) ->
                try {
                    val j = JSONObject(payload)
                    val device = DeviceType.from(j.optString("device"))
                    val key = j.optString("key").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val status = j.optString("status", "ok")
                    val success = status.equals("ok", true) || status.equals("success", true)
                    IrLearningEvent(
                        device = device,
                        key = key,
                        success = success,
                        protocol = j.optString("protocol").takeIf { it.isNotBlank() },
                        code = j.optString("code").takeIf { it.isNotBlank() },
                        bits = j.optInt("bits").takeIf { j.has("bits") },
                        error = j.optString("error").takeIf { it.isNotBlank() }
                    )
                } catch (_: Exception) {
                    null
                }
            }
}

