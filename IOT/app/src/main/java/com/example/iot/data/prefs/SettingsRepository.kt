package com.example.iot.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("iot_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    object Keys {
        val brokerHost = stringPreferencesKey("broker_host")
        val brokerPort = intPreferencesKey("broker_port")
        val defaultNode = stringPreferencesKey("default_node")
    }

    data class Settings(
        val brokerHost: String = "10.0.2.2",
        val brokerPort: Int = 1883,
        val defaultNode: String = "esp-bedroom"
    ) {
        val url: String get() = "tcp://$brokerHost:$brokerPort"
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            brokerHost = p[Keys.brokerHost] ?: "10.0.2.2",
            brokerPort = p[Keys.brokerPort] ?: 1883,
            defaultNode = p[Keys.defaultNode] ?: "esp-bedroom"
        )
    }

    suspend fun save(host: String, port: Int, node: String) {
        context.dataStore.edit { p ->
            p[Keys.brokerHost] = host
            p[Keys.brokerPort] = port
            p[Keys.defaultNode] = node
        }
    }
}
