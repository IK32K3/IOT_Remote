package com.example.iot.feature.control.tv

import androidx.lifecycle.viewModelScope
import com.example.iot.core.mqtt.MqttTopics
import com.example.iot.data.RemoteRepository
import com.example.iot.data.local.RemoteProfile
import com.example.iot.domain.usecase.ObserveNodesUseCase
import com.example.iot.domain.usecase.PublishUseCase
import com.example.iot.feature.control.common.BaseControlViewModel
import com.example.iot.domain.usecase.ObserveLearnedCommandsUseCase
import com.example.iot.domain.model.LearnedCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class TvControlViewModel @Inject constructor(
    private val repo: RemoteRepository,
    private val publish: PublishUseCase,
    observeNodes: ObserveNodesUseCase,
    private val observeLearned: ObserveLearnedCommandsUseCase,
) : BaseControlViewModel() {

    private var remote: RemoteProfile? = null
    private var remoteIdLong: Long? = null
    private val _power = MutableStateFlow(false)

    private val nodes: StateFlow<Map<String, Boolean>> =
        observeNodes().stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    override val isNodeOnline: StateFlow<Boolean> =
        combine(nodes, _nodeId) { map, id -> map[id] == true }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _learnedCommands = MutableStateFlow<Map<String, LearnedCommand>>(emptyMap())
    private val learnedCommands: Map<String, LearnedCommand> get() = _learnedCommands.value

    private val channelBuffer = StringBuilder()
    private var channelJob: Job? = null
    private val channelDelayMs = 900L

    override fun load(remoteId: String) {
        val id = remoteId.toLongOrNull() ?: return
        remoteIdLong = id
        viewModelScope.launch {
            remote = repo.getById(id)
            remote?.let { r ->
                _title.value = when {
                    r.name.isNotBlank() -> r.name
                    else -> "${r.brand} TV".trim()
                }
                _nodeId.value = r.nodeId
                observeLearned(r.id).collect { list ->
                    _learnedCommands.value = list.associateBy { it.key.uppercase() }
                }
            }
        }
    }

    private fun sendKey(key: String) {
        val r = remote ?: return
        val learned = learnedCommands[key.uppercase()]
        val payload = JSONObject().apply {
            put("cmd", "key")
            put("brand", r.brand)
            put("type", r.deviceType.ifBlank { "TV" })
            put("index", r.codeSetIndex)
            put("key", key)
            learned?.let {
                put("ir", JSONObject().apply {
                    put("protocol", it.protocol)
                    put("code", it.code)
                    put("bits", it.bits)
                })
            }
        }.toString()
        publish(MqttTopics.cmdTopic(r.nodeId, "tv"), payload)
    }

    private fun hasLearnedKey(key: String): Boolean =
        learnedCommands.containsKey(key.uppercase())

    fun togglePower() {
        val np = !_power.value
        if (hasLearnedKey("POWER_ON") || hasLearnedKey("POWER_OFF") || hasLearnedKey("POWER")) {
            val key = if (np) "POWER_ON" else "POWER_OFF"
            if (hasLearnedKey(key)) {
                sendKey(key)
            } else {
                sendKey("POWER")
            }
            _power.value = np
            return
        }
        sendKey("POWER")
        _power.value = np
    }

    fun power() = togglePower()
    fun mute() = sendKey("MUTE")
    fun tvAv() = sendKey("TV_AV")

    fun volUp() = sendKey("VOL_UP")
    fun volDown() = sendKey("VOL_DOWN")
    fun chUp() = sendKey("CH_UP")
    fun chDown() = sendKey("CH_DOWN")

    fun menu() = sendKey("MENU")
    fun exit() = sendKey("EXIT")

    fun digit(n: Int) {
        if (n !in 0..9) return
        channelBuffer.append(n)
        if (channelBuffer.length >= 3) {
            flushChannel()
        } else {
            scheduleChannelSend()
        }
    }

    fun dash() = sendKey("DASH")

    fun navUp() = sendKey("UP")
    fun navDown() = sendKey("DOWN")
    fun navLeft() = sendKey("LEFT")
    fun navRight() = sendKey("RIGHT")
    fun ok() = sendKey("OK")

    fun home() = sendKey("HOME")
    fun back() = sendKey("BACK")
    fun more() = sendKey("MORE")

    private fun scheduleChannelSend() {
        channelJob?.cancel()
        channelJob = viewModelScope.launch {
            delay(channelDelayMs)
            flushChannel()
        }
    }

    private fun flushChannel() {
        if (channelBuffer.isEmpty()) return
        sendChannel(channelBuffer.toString())
        channelBuffer.clear()
    }

    private fun sendChannel(channel: String) {
        val r = remote ?: return
        val payload = JSONObject().apply {
            put("cmd", "channel")
            put("brand", r.brand)
            put("type", r.deviceType.ifBlank { "TV" })
            put("index", r.codeSetIndex)
            put("channel", channel)
        }.toString()
        publish(MqttTopics.cmdTopic(r.nodeId, "tv"), payload)
    }

    override fun deleteRemote(remoteId: String) {
        val id = remote?.id ?: remoteId.toLongOrNull() ?: remoteIdLong ?: return
        viewModelScope.launch { if (remote != null) repo.delete(remote!!) else repo.deleteById(id) }
    }

}
