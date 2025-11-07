package com.example.iot.core.mqtt

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.iot.MainActivity
import com.example.iot.R
import com.example.iot.core.Defaults
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.*

@AndroidEntryPoint
class MqttForegroundService : Service() {

    @Inject lateinit var conn: MqttConnectionManager
    @Inject lateinit var broker: EmbeddedBroker
    @Inject lateinit var settingsRepo: com.example.iot.data.prefs.SettingsRepository
    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main.immediate + serviceJob)
    private var lastHostPort: String = "${Defaults.BROKER_HOST}:${Defaults.BROKER_PORT}"


    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTI_ID, buildNotification("Đang kết nối..."))
        // kết nối broker (service sống = kết nối sống)
        scope.launch {
            settingsRepo.settings.collect { s ->
                lastHostPort = "${s.brokerHost}:${s.brokerPort}"
                broker.ensure(s.brokerHost, s.brokerPort)
                updateNotification("Broker ${s.brokerHost}:${s.brokerPort}")
                conn.connect(applicationContext, s.url, s.defaultNode)
            }
        }

        scope.launch {
            conn.anyNodeOnline.collect { online ->
                val status = if (online) "Thiết bị đã sẵn sàng" else "Đang chờ thiết bị..."
                val suffix = if (lastHostPort.isNotBlank()) " ($lastHostPort)" else ""
                updateNotification(status + suffix)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        broker.stop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID, "MQTT", NotificationManager.IMPORTANCE_MIN
                ).apply { setShowBadge(false); description = "Kết nối MQTT" }
            )
        }
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // dùng ic_launcher nếu chưa có
            .setContentTitle("MQTT Gateway")
            .setContentText(text)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTI_ID, buildNotification(text))
    }

    companion object {
        private const val CHANNEL_ID = "mqtt_channel"
        private const val NOTI_ID = 101
        fun start(ctx: Context) {
            ctx.startForegroundService(Intent(ctx, MqttForegroundService::class.java))
        }
        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, MqttForegroundService::class.java))
        }
    }
}

