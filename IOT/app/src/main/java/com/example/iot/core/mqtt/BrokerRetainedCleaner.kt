package com.example.iot.core.mqtt

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID

@Singleton
class BrokerRetainedCleaner @Inject constructor() {

    suspend fun resetNodeStatus(brokerUrl: String, nodeId: String) {
        withContext(Dispatchers.IO) {
            val statusTopic = MqttTopics.nodeStatus(nodeId)
            val clientId = "cleaner-" + UUID.randomUUID().toString().take(8)
            val client = MqttAsyncClient(brokerUrl, clientId, MemoryPersistence())
            try {
                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    isAutomaticReconnect = false
                    connectionTimeout = 5
                    keepAliveInterval = 10
                }
                client.connect(options).waitForCompletion(5_000)
                val message = MqttMessage("offline".toByteArray()).apply {
                    qos = 1
                    isRetained = true
                }
                client.publish(statusTopic, message).waitForCompletion(5_000)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to reset retained status: ${t.message}", t)
            } finally {
                try {
                    client.pendingDeliveryTokens.firstOrNull()?.waitForCompletion(1_000)
                } catch (_: Exception) {
                }
                try {
                    client.disconnectForcibly(0, 0, true)
                } catch (_: Exception) {
                }
                try {
                    client.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    companion object {
        private const val TAG = "BrokerRetainedCleaner"
    }
}