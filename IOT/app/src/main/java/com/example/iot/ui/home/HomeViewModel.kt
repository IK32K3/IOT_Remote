package com.example.iot.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iot.core.mqtt.MqttTopics
import com.example.iot.data.local.RemoteProfile
import com.example.iot.domain.model.DeviceType
import com.example.iot.domain.usecase.GetRemotesUseCase
import com.example.iot.domain.usecase.ObserveEspStatusUseCase
import com.example.iot.domain.usecase.ObserveNodesUseCase
import com.example.iot.domain.usecase.PublishUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeEspStatus: ObserveEspStatusUseCase,
    getRemotes: GetRemotesUseCase,
    observeNodes: ObserveNodesUseCase,
    private val publish: PublishUseCase
) : ViewModel() {

    // ESP gateway online/offline
    val isEspOnline = observeEspStatus()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val remotesFlow: Flow<List<RemoteProfile>> = getRemotes()

    private val nodesFlow: StateFlow<Map<String, Boolean>> =
        observeNodes().stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // UI list hiển thị trên Home
    val remoteCards: StateFlow<List<RemoteCardUi>> =
        combine(remotesFlow, nodesFlow) { remotes, nodes ->
            remotes.map { entity ->
                RemoteCardUi(
                    id = entity.id.toString(),
                    name = entity.name,
                    room = entity.room,
                    nodeId = entity.nodeId,
                    online = nodes[entity.nodeId] == true,
                    brand = entity.brand,
                    type = entity.type,
                    codeSetIndex = entity.codeSetIndex,
                    deviceType = DeviceType.from(entity.deviceType)   // ✅ thêm
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Gửi lệnh Power cho card tương ứng.
     * Ứng với mỗi deviceType có thể thay topic khác nhau.
     */
    fun sendPower(card: RemoteCardUi) {
        val topic = when (card.deviceType) {
            DeviceType.AC  -> MqttTopics.testIrTopic(card.nodeId)
            DeviceType.TV  -> MqttTopics.cmdTopic(card.nodeId, "tv")
            DeviceType.FAN -> MqttTopics.cmdTopic(card.nodeId, "fan")
            DeviceType.STB -> MqttTopics.cmdTopic(card.nodeId, "stb")
            else -> MqttTopics.testIrTopic(card.nodeId)
        }

        val payload = when (card.deviceType) {
            DeviceType.AC -> """{"cmd":"power","brand":"${card.brand}","type":"AC","index":${card.codeSetIndex}}"""
            DeviceType.TV -> """{"cmd":"key","key":"POWER","brand":"${card.brand}","type":"TV","index":${card.codeSetIndex}}"""
            DeviceType.FAN-> """{"cmd":"power","brand":"${card.brand}","type":"FAN","index":${card.codeSetIndex}}"""
            DeviceType.STB-> """{"cmd":"key","key":"POWER","brand":"${card.brand}","type":"STB","index":${card.codeSetIndex}}"""
            else -> """{"cmd":"power","brand":"${card.brand}","type":"AC","index":${card.codeSetIndex}}"""
        }

        publish(topic, payload)
    }
}
