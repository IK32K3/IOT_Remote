package com.example.iot.core.mqtt

import android.util.Log
import com.example.iot.core.Defaults
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.util.Enumeration

@Singleton
class LocalDiscoveryServer @Inject constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var advertisedHost: String = Defaults.BROKER_HOST
    private var advertisedPort: Int = Defaults.BROKER_PORT

    fun ensure(host: String, port: Int) {
        advertisedHost = host
        advertisedPort = port
        if (!shouldRespond(host)) {
            stop()
            return
        }
        if (job?.isActive == true) return
        job = scope.launch { runServer() }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun runServer() = coroutineScope {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket(DISCOVERY_PORT, InetAddress.getByName("0.0.0.0")).apply {
                broadcast = true
                soTimeout = 1000
            }
            Log.i(TAG, "Discovery server started on udp/$DISCOVERY_PORT")
            val buffer = ByteArray(256)
            while (isActive) {
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(packet)
                } catch (_: SocketTimeoutException) {
                    continue
                }
                val payload = String(packet.data, 0, packet.length).trim()
                if (payload != DISCOVERY_REQUEST) continue
                val response = responsePayload() ?: continue
                val replyBytes = response.encodeToByteArray()
                socket.send(DatagramPacket(replyBytes, replyBytes.size, packet.address, packet.port))
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Discovery server stopped: ${t.message}", t)
        } finally {
            socket?.close()
            Log.i(TAG, "Discovery server terminated")
        }
    }

    private fun responsePayload(): String? {
        val port = advertisedPort.takeIf { it > 0 } ?: Defaults.BROKER_PORT
        val host = resolveAdvertisedHost() ?: return null
        return "$DISCOVERY_RESPONSE_PREFIX$host:$port"
    }

    private fun resolveAdvertisedHost(): String? {
        if (!shouldRespond(advertisedHost)) return null
        return try {
            val address = InetAddress.getByName(advertisedHost)
            when {
                address.isLoopbackAddress || address.isAnyLocalAddress -> currentSiteLocalIp()
                else -> address.hostAddress
            }
        } catch (_: Exception) {
            currentSiteLocalIp()
        }
    }

    private fun currentSiteLocalIp(): String? {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
        for (network in interfaces.asSequence()) {
            for (addr in network.inetAddresses.asSequence()) {
                if (addr is Inet4Address && !addr.isLoopbackAddress && addr.isSiteLocalAddress) {
                    return addr.hostAddress
                }
            }
        }
        return null
    }

    private fun shouldRespond(host: String): Boolean {
        if (host.isBlank()) return true
        return try {
            val address = InetAddress.getByName(host)
            address.isAnyLocalAddress || address.isSiteLocalAddress || address.isLoopbackAddress
        } catch (_: Exception) {
            host.equals("localhost", ignoreCase = true)
        }
    }

    companion object {
        private const val TAG = "DiscoveryServer"
        private const val DISCOVERY_PORT = 4210
        private const val DISCOVERY_REQUEST = "DISCOVER_IOT_MQTT"
        private const val DISCOVERY_RESPONSE_PREFIX = "MQTT://"
    }
}

private fun <T> Enumeration<T>.asSequence(): Sequence<T> = sequence {
    while (hasMoreElements()) {
        yield(nextElement())
    }
}