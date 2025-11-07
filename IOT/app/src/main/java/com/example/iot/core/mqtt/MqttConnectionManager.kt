package com.example.iot.core.mqtt

import android.content.Context
import android.util.Log
import com.example.iot.core.Defaults
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MqttConnectionManager @Inject constructor() {

    private var client: MqttAsyncClient? = null

    private val nodeOnline: MutableMap<String, Boolean> = mutableMapOf()

    private val _nodesOnline = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val nodesOnline: StateFlow<Map<String, Boolean>> = _nodesOnline

    private val _connected = MutableStateFlow(false)

    private val _anyNodeOnline = MutableStateFlow(false)
    val anyNodeOnline: StateFlow<Boolean> = _anyNodeOnline

    /** 🔹 Dòng dữ liệu MQTT nhận được (topic → payload) */
    private val _incoming = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 64)
    val incoming: SharedFlow<Pair<String, String>> = _incoming

    private var currentBroker: String? = null
    private var currentNodeId: String? = null

    fun connect(@Suppress("UNUSED_PARAMETER") context: Context, broker: String = Defaults.BROKER_URL, nodeId: String = Defaults.NODE_ID) {
        val normalizedNode = nodeId.ifBlank { Defaults.NODE_ID }
        if (client != null && client!!.isConnected && currentBroker == broker && currentNodeId == normalizedNode) return

        try { client?.disconnectForcibly(0, 0, true) } catch (_: Exception) { }
        client = null
        currentBroker = broker
        currentNodeId = normalizedNode

        nodeOnline.clear()
        nodeOnline[normalizedNode] = false
        _nodesOnline.value = nodeOnline.toMap()
        _anyNodeOnline.value = false

        val clientId = "android-" + UUID.randomUUID().toString().take(8)
        val c = MqttAsyncClient(broker, clientId, MemoryPersistence())
        client = c

        /** 🔹 Callback nhận và phát lại các topic/state cần thiết */
        c.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                Log.d("MQTT", "connectComplete(reconnect=$reconnect, uri=$serverURI)")
                _connected.value = true
                val nodeId = currentNodeId ?: Defaults.NODE_ID
                subscribe(MqttTopics.nodeStatus(nodeId), 1)
                subscribe(MqttTopics.stateTopic(nodeId, "ac"), 1)
                subscribe(MqttTopics.stateTopic(nodeId, "tv"), 1)
                subscribe(MqttTopics.stateTopic(nodeId, "fan"), 1)
                subscribe(MqttTopics.stateTopic(nodeId, "stb"), 1)
            }

            override fun connectionLost(cause: Throwable?) {
                Log.w("MQTT", "connectionLost: ${cause?.message}")
                _connected.value = false
                currentNodeId?.let { updateNodeOnline(it, false) }
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val t = topic ?: return
                val payload = message?.toString().orEmpty()
                val retained = message?.isRetained ?: false
                Log.d("MQTT", "msg: t=$t, retained=$retained, p=$payload")
                _incoming.tryEmit(t to payload)   // 🔸 Đẩy toàn bộ message cho Repository

                val nodeId = currentNodeId ?: Defaults.NODE_ID
                if (t == MqttTopics.nodeStatus(nodeId)) {
                    updateNodeOnline(nodeId, payload.equals("online", true))
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })

        val opts = MqttConnectOptions().apply {
            isCleanSession = true
            isAutomaticReconnect = true
            connectionTimeout = 10
            keepAliveInterval = 20
            setWill("iot/app/$clientId/lwt", "offline".toByteArray(), 1, true)
        }

        c.connect(opts, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("MQTT", "connect success")
                _connected.value = true
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("MQTT", "connect fail: ${exception?.message}", exception)
                _connected.value = false
                currentNodeId?.let { updateNodeOnline(it, false) }
            }
        })
    }

    private fun updateNodeOnline(nodeId: String, online: Boolean) {
        nodeOnline[nodeId] = online
        _nodesOnline.value = nodeOnline.toMap()
        _anyNodeOnline.value = nodeOnline.values.any { it }
    }

    private fun subscribe(topic: String, qos: Int) {
        client?.subscribe(topic, qos, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("MQTT", "SUB ok: $topic")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("MQTT", "SUB fail: ${exception?.message}", exception)
            }
        })
    }

    fun publish(topic: String, payload: String, qos: Int = 1, retain: Boolean = false) {
        val c = client
        if (c == null) {
            Log.w("MQTT", "publish skipped: client=null, topic=$topic")
            return
        }
        if (!c.isConnected) {
            Log.w("MQTT", "publish skipped: not connected, topic=$topic")
            return
        }
        try {
            val msg = MqttMessage(payload.toByteArray()).apply {
                this.qos = qos
                isRetained = retain
            }
            c.publish(topic, msg)
            Log.d("MQTT", "publish ok: t=$topic payload=$payload")
        } catch (e: Exception) {
            Log.e("MQTT", "publish failed: t=$topic, cause=${e.message}", e)
        }
    }
}
