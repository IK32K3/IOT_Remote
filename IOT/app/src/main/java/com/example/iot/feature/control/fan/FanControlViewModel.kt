package com.example.iot.feature.control.fan

import androidx.lifecycle.viewModelScope
import com.example.iot.core.mqtt.MqttTopics
import com.example.iot.data.RemoteRepository
import com.example.iot.data.local.RemoteProfile
import com.example.iot.domain.model.LearnedCommand
import com.example.iot.domain.usecase.DeleteLearnedCommandsUseCase
import com.example.iot.domain.usecase.ObserveLearnedCommandsUseCase
import com.example.iot.domain.usecase.ObserveNodesUseCase
import com.example.iot.domain.usecase.PublishUseCase
import com.example.iot.feature.control.common.BaseControlViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class FanControlViewModel @Inject constructor(
    private val repo: RemoteRepository,
    private val publish: PublishUseCase,
    observeNodes: ObserveNodesUseCase,
    private val observeLearned: ObserveLearnedCommandsUseCase,
    private val deleteLearned: DeleteLearnedCommandsUseCase
) : BaseControlViewModel() {

    private var remote: RemoteProfile? = null
    private var remoteIdLong: Long? = null
    private val _power = MutableStateFlow(false)

    private val _learnedCommands = MutableStateFlow<Map<String, LearnedCommand>>(emptyMap())

    private val nodes: StateFlow<Map<String, Boolean>> =
        observeNodes().stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    override val isNodeOnline: StateFlow<Boolean> =
        combine(nodes, _nodeId) { m, id -> m[id] == true }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    override fun load(remoteId: String) {
        val id = remoteId.toLongOrNull() ?: return
        remoteIdLong = id
        viewModelScope.launch {
            remote = repo.getById(id)
            remote?.let { r ->
                _title.value = when {
                    r.name.isNotBlank() -> r.name
                    else -> "${r.brand} Fan".trim()
                }
                _nodeId.value = r.nodeId
                observeLearnedCommands(id)
            }
        }
    }

    private fun observeLearnedCommands(remoteId: Long) {
        viewModelScope.launch {
            observeLearned(remoteId).collect { list ->
                _learnedCommands.value = list.associateBy { it.key.uppercase() }
            }
        }
    }

    private fun sendKey(key: String) {
        val r = remote ?: return
        val payload = JSONObject().apply {
            put("cmd", "key")
            put("brand", r.brand); put("type", "FAN")
            put("index", r.codeSetIndex)
            put("key", key)
        }.toString()
        publish(MqttTopics.cmdTopic(r.nodeId, "fan"), payload)
    }

    private fun hasCustomCommands(): Boolean = _learnedCommands.value.isNotEmpty()

    private fun sendLearnedCommand(key: String): Boolean {
        val r = remote ?: return false
        val cmd = _learnedCommands.value[key.uppercase()] ?: return false
        val payload = JSONObject().apply {
            put("device", "fan")
            put("cmd", "key")
            put("key", key.uppercase())
            put("ir", JSONObject().apply {
                put("protocol", cmd.protocol)
                put("code", cmd.code)
                put("bits", cmd.bits)
            })
        }.toString()
        publish(MqttTopics.nodeCommandTopic(r.nodeId), payload)
        return true
    }

    // --- các nút ---
    fun togglePower() {
        val np = !_power.value
        if (hasCustomCommands()) {
            val key = if (np) "POWER_ON" else "POWER_OFF"
            val sent = sendLearnedCommand(key) || sendLearnedCommand("POWER")
            if (sent) {
                _power.value = np
                return
            }
        }
        sendKey("POWER")
        _power.value = np
    }

    fun timer() {
        if (hasCustomCommands()) {
            sendLearnedCommand("TIMER")
        } else {
            sendKey("TIMER")
        }
    }

    fun speedUp() {
        if (hasCustomCommands()) {
            sendLearnedCommand("SPEED_UP")
        } else {
            sendKey("SPEED_UP")
        }
    }

    fun speedDown() {
        if (hasCustomCommands()) {
            sendLearnedCommand("SPEED_DOWN")
        } else {
            sendKey("SPEED_DOWN")
        }
    }

    fun toggleSwing() {
        if (hasCustomCommands()) {
            sendLearnedCommand("SWING")
        } else {
            sendKey("SWING")
        }
    }

    fun type() {
        if (hasCustomCommands()) {
            sendLearnedCommand("TYPE")
        } else {
            sendKey("TYPE")
        }
    }

    override fun deleteRemote(remoteId: String) {
        val id = remote?.id ?: remoteId.toLongOrNull() ?: remoteIdLong ?: return
        viewModelScope.launch {
            deleteLearned(id)
            if (remote != null) repo.delete(remote!!) else repo.deleteById(id)
        }
    }
}
