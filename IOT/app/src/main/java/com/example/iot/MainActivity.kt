package com.example.iot

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.iot.core.mqtt.MqttConnectionManager
import com.example.iot.core.mqtt.MqttForegroundService
import com.example.iot.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject lateinit var mqtt: MqttConnectionManager   // giữ lại nếu nơi khác cần

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khởi động service sau khi (hoặc đồng thời) có quyền thông báo
        startMqttServiceWithPermission()
    }

    private fun startMqttServiceWithPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQ_POST_NOTIF
                )
                return
            }
        }
        // Đã có quyền (hoặc dưới Android 13) -> start ForegroundService
        ContextCompat.startForegroundService(
            applicationContext,
            Intent(applicationContext, MqttForegroundService::class.java)
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_POST_NOTIF) {
            // Thử start lại (nếu user cho phép thì chạy, không thì bỏ qua)
            startMqttServiceWithPermission()
        }
    }

    companion object {
        private const val REQ_POST_NOTIF = 1001
    }
}
