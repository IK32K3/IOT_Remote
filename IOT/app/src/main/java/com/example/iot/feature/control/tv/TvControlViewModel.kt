package com.example.iot.feature.control.tv

import androidx.lifecycle.viewModelScope
import com.example.iot.core.mqtt.MqttTopics
import com.example.iot.data.RemoteRepository
import com.example.iot.data.local.RemoteProfile
import com.example.iot.domain.usecase.ObserveNodesUseCase
import com.example.iot.domain.usecase.ObserveTvStateUseCase
import com.example.iot.domain.usecase.PublishUseCase
import com.example.iot.feature.control.common.BaseControlViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class TvControlViewModel @Inject constructor(
    private val repo: RemoteRepository,
    private val publish: PublishUseCase,
    observeNodes: ObserveNodesUseCase,
    private val observeTvState: ObserveTvStateUseCase,
) : BaseControlViewModel() {

    private var remote: RemoteProfile? = null

    private val nodes: StateFlow<Map<String, Boolean>> =
        observeNodes().stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    override val isNodeOnline: StateFlow<Boolean> =
        combine(nodes, _nodeId) { map, id -> map[id] == true }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _power = MutableStateFlow(false)
    val power: StateFlow<Boolean> = _power

    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted

    private val _volume = MutableStateFlow(0)
    val volume: StateFlow<Int> = _volume

    private val _channel = MutableStateFlow(1)
    val channel: StateFlow<Int> = _channel

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input

    override fun load(remoteId: String) {
        val id = remoteId.toLongOrNull() ?: return
        viewModelScope.launch {
            remote = repo.getById(id)
            remote?.let { r ->
                _title.value = "${r.brand} TV ${r.name}".trim()
                _nodeId.value = r.nodeId
                observeState(r.nodeId)
            }
        }
    }

    private fun observeState(nodeId: String) {
        viewModelScope.launch {
            observeTvState(nodeId).collect { state ->
                _power.value = state.power
                _muted.value = state.muted
                _volume.value = state.volume
                _channel.value = state.channel
                _input.value = state.input
            }
        }
    }

    private fun sendKey(key: String) {
        val r = remote ?: return
        val payload = JSONObject().apply {
            put("cmd", "key")
            put("brand", r.brand)
            put("type", r.deviceType.ifBlank { "TV" })
            put("index", r.codeSetIndex)
            put("key", key)
        }.toString()
        publish(MqttTopics.cmdTopic(r.nodeId, "tv"), payload)
    }

    fun power() = sendKey("POWER")
    fun mute() = sendKey("MUTE")
    fun tvAv() = sendKey("TV_AV")

    fun volUp() = sendKey("VOL_UP")
    fun volDown() = sendKey("VOL_DOWN")
    fun chUp() = sendKey("CH_UP")
    fun chDown() = sendKey("CH_DOWN")

    fun menu() = sendKey("MENU")
    fun exit() = sendKey("EXIT")

    fun digit(n: Int) = sendKey("DIGIT_$n")
    fun dash() = sendKey("DASH")

    fun navUp() = sendKey("UP")
    fun navDown() = sendKey("DOWN")
    fun navLeft() = sendKey("LEFT")
    fun navRight() = sendKey("RIGHT")
    fun ok() = sendKey("OK")

    fun home() = sendKey("HOME")
    fun back() = sendKey("BACK")
    fun more() = sendKey("MORE")

    fun deleteRemote() {
        val r = remote ?: return
        viewModelScope.launch { repo.delete(r) }
    }

}
