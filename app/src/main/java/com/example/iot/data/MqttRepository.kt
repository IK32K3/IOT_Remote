package com.example.iot.data


import com.example.iot.core.mqtt.MqttConnectionManager
import com.example.iot.domain.model.AcState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
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

    fun observeTopic(prefix: String): Flow<Pair<String, String>> =
        conn.incoming.filter { (t, _) -> t.startsWith(prefix.removeSuffix("/")) }

    private val incoming = conn.incoming

    fun observeAcState(nodeId: String): Flow<AcState> =
        incoming
            .filter { (t, _) -> t == "iot/nodes/$nodeId/ac/state" }
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
}