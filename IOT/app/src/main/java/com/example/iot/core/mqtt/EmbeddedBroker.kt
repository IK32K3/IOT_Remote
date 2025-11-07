package com.example.iot.core.mqtt

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.moquette.broker.Server
import io.moquette.broker.config.MemoryConfig
import java.io.File
import java.net.InetAddress
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmbeddedBroker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var server: Server? = null
    private var currentPort: Int = -1

    @Synchronized
    fun ensure(host: String, port: Int) {
        if (!shouldRun(host)) {
            stop()
            return
        }
        if (server != null && currentPort == port) return

        stop()
        val storePath = File(context.filesDir, "moquette_store.mapdb").absolutePath
        val props = Properties().apply {
            put(Props.PORT, port.toString())
            put(Props.HOST, "0.0.0.0")
            put(Props.WEB_SOCKET_PORT, "0")
            put(Props.PERSISTENT_STORE, storePath)
            put(Props.ALLOW_ANONYMOUS, "true")
        }
        try {
            val cfg = MemoryConfig(props)
            val srv = Server()
            srv.startServer(cfg)
            server = srv
            currentPort = port
            Log.i(TAG, "Embedded broker started on 0.0.0.0:$port")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to start embedded broker: ${t.message}", t)
            stop()
        }
    }

    @Synchronized
    fun stop() {
        try {
            server?.stopServer()
            if (server != null) {
                Log.i(TAG, "Embedded broker stopped")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Error while stopping broker: ${t.message}", t)
        } finally {
            server = null
            currentPort = -1
        }
    }

    private fun shouldRun(host: String): Boolean {
        return try {
            val address = InetAddress.getByName(host)
            address.isLoopbackAddress || address.isAnyLocalAddress || address.isSiteLocalAddress
        } catch (_: Exception) {
            host.equals("localhost", ignoreCase = true)
        }
    }

    fun isActive(): Boolean = server != null

    companion object {
        private const val TAG = "EmbeddedBroker"

        private object Props {
            const val PORT = "port"
            const val HOST = "host"
            const val WEB_SOCKET_PORT = "websocket_port"
            const val PERSISTENT_STORE = "persistent_store"
            const val ALLOW_ANONYMOUS = "allow_anonymous"
        }
    }
}