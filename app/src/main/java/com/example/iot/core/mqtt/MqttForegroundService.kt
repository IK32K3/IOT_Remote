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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.*

@AndroidEntryPoint
class MqttForegroundService : Service() {

    @Inject lateinit var conn: MqttConnectionManager
    @Inject lateinit var settingsRepo: com.example.iot.data.prefs.SettingsRepository
    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main.immediate + serviceJob)


    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTI_ID, buildNotification("Đang kết nối..."))
        // kết nối broker (service sống = kết nối sống)
        scope.launch {
            settingsRepo.settings.collect { s ->
                conn.connect(applicationContext, s.url)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
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
