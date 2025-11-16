package com.example.iot.data

import com.example.iot.core.mqtt.MqttConnectionManager
import com.example.iot.core.mqtt.MqttTopics
import com.example.iot.domain.model.AcState
import com.example.iot.domain.model.FanState
import com.example.iot.domain.model.DeviceType
import com.example.iot.domain.model.IrLearningEvent
import com.example.iot.domain.model.StbState
import com.example.iot.domain.model.TvState
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

    fun observeAcState(nodeId: String): Flow<AcState> =
        incoming
            .filter { (t, _) -> t == MqttTopics.stateTopic(nodeId, "ac") }
            .map { (_, payload) ->
                try {
                    val j = JSONObject(payload)
                    AcState(
                        power = j.optBoolean("power", false),
                        mode  = j.optString("mode", "cool"),
                        temp  = j.optInt("temp", 24),
                        fan   = j.optString("fan", "auto")
                    )
                } catch (_: Exception) {
                    AcState()
                }
            }

    fun observeTvState(nodeId: String): Flow<TvState> =
        incoming
            .filter { (t, _) -> t == MqttTopics.stateTopic(nodeId, "tv") }
            .map { (_, payload) ->
                try{
                    val j = JSONObject(payload)
                    TvState(
                        power   = j.optBoolean("power", false),
                        muted   = j.optBoolean("mute", false),
                        volume  = j.optInt("volume", 0),
                        channel = j.optInt("channel", 1),
                        input   = j.optString("input", "")
                    )
                } catch (_: Exception) {
                    TvState()
                }
            }

    fun observeFanState(nodeId: String): Flow<FanState> =
        incoming
            .filter { (t, _) -> t == MqttTopics.stateTopic(nodeId, "fan") }
            .map { (_, p) ->
                try {
                    val j = JSONObject(p)
                    FanState(
                        power = j.optBoolean("power", false),
                        speed = j.optInt("speed", 0),
                        swing = j.optBoolean("swing", false),
                        type = j.optString("type", ""),
                        timerMin = j.optInt("timer", 0)
                    )
                } catch (_: Exception) { FanState() }
            }

    fun observeStbState(nodeId: String): Flow<StbState> =
        incoming
            .filter { (t, _) -> t == MqttTopics.stateTopic(nodeId, "stb") }
            .map { (_, payload) ->
                try {
                    val j = JSONObject(payload)
                    StbState(
                        power  = j.optBoolean("power", false),
                        // chấp nhận cả "muted" và "mute" tuỳ firmware
                        muted  = if (j.has("muted")) j.optBoolean("muted", false)
                        else j.optBoolean("mute", false),
                        lastKey = j.optString("lastKey").takeIf { it.isNotEmpty() },
                        hint    = j.optString("hint").takeIf { it.isNotEmpty() }
                    )
                } catch (_: Exception) {
                    StbState()
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