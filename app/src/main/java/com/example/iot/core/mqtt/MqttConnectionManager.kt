package com.example.iot.core.mqtt

import android.content.Context
import android.util.Log
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

    fun connect(context: Context, broker: String = "tcp://10.0.2.2:1883") {
        if (client != null && client!!.isConnected && currentBroker == broker) return

        try { client?.disconnectForcibly(0, 0, true) } catch (_: Exception) { }
        client = null
        currentBroker = broker

        val clientId = "android-" + UUID.randomUUID().toString().take(8)
        val c = MqttAsyncClient(broker, clientId, MemoryPersistence())
        client = c

        /** 🔹 Callback nhận và phát lại các topic/state cần thiết */
        c.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                Log.d("MQTT", "connectComplete(reconnect=$reconnect, uri=$serverURI)")
                _connected.value = true
                // đăng ký các topic cần thiết
                subscribe(MqttTopics.NODE_STATUS, 1)
                subscribe("iot/nodes/+/ac/state", 1)
                subscribe("iot/nodes/+/tv/state", 1)
                subscribe("iot/nodes/+/fan/state", 1)
                subscribe("iot/nodes/+/stb/state", 1)
            }

            override fun connectionLost(cause: Throwable?) {
                Log.w("MQTT", "connectionLost: ${cause?.message}")
                _connected.value = false
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val t = topic ?: return
                val payload = message?.toString().orEmpty()
                val retained = message?.isRetained ?: false
                Log.d("MQTT", "msg: t=$t, retained=$retained, p=$payload")
                _incoming.tryEmit(t to payload)   // 🔸 Đẩy toàn bộ message cho Repository

                // cập nhật trạng thái node online/offline
                Regex("iot/nodes/([^/]+)/status").matchEntire(t)?.let { m ->
                    val nodeId = m.groupValues[1]
                    nodeOnline[nodeId] = payload.equals("online", true)
                    _nodesOnline.value = nodeOnline.toMap()
                    _anyNodeOnline.value = nodeOnline.values.any { it }
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
            }
        })
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
