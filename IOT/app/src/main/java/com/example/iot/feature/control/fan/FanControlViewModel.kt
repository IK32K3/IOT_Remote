package com.example.iot.feature.control.fan

import androidx.lifecycle.viewModelScope
import com.example.iot.core.mqtt.MqttTopics
import com.example.iot.data.RemoteRepository
import com.example.iot.data.local.RemoteProfile
import com.example.iot.domain.model.LearnedCommand
import com.example.iot.domain.usecase.DeleteLearnedCommandsUseCase
import com.example.iot.domain.usecase.ObserveFanStateUseCase
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
    private val observeFan: ObserveFanStateUseCase,
    private val observeLearned: ObserveLearnedCommandsUseCase,
    private val deleteLearned: DeleteLearnedCommandsUseCase
) : BaseControlViewModel() {

    private var remote: RemoteProfile? = null

    private val _power = MutableStateFlow(false)
    val power: StateFlow<Boolean> = _power

    private val _speed = MutableStateFlow(0)
    val speed: StateFlow<Int> = _speed

    private val _swing = MutableStateFlow(false)
    val swing: StateFlow<Boolean> = _swing

    private val _learnedCommands = MutableStateFlow<Map<String, LearnedCommand>>(emptyMap())

    private val nodes: StateFlow<Map<String, Boolean>> =
        observeNodes().stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    override val isNodeOnline: StateFlow<Boolean> =
        combine(nodes, _nodeId) { m, id -> m[id] == true }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    override fun load(remoteId: String) {
        val id = remoteId.toLongOrNull() ?: return
        viewModelScope.launch {
            remote = repo.getById(id)
            remote?.let { r ->
                _title.value = "${r.brand} Fan ${r.name}".trim()
                _nodeId.value = r.nodeId
                observeLearnedCommands(id)
                observeFan(r.nodeId).collect { s ->
                    _power.value = s.power
                    _speed.value = s.speed
                    _swing.value = s.swing
                }
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

    private fun sendLearnedCommand(key: String) {
        val r = remote ?: return
        val cmd = _learnedCommands.value[key.uppercase()] ?: return
        val payload = JSONObject().apply {
            put("device", "FAN")
            put("key", key.uppercase())
            put("protocol", cmd.protocol)
            put("code", cmd.code)
            put("bits", cmd.bits)
        }.toString()
        publish(MqttTopics.emitIrTopic(r.nodeId), payload)
    }

    // --- các nút ---
    fun togglePower() {
        if (hasCustomCommands()) {
            val next = !_power.value
            sendLearnedCommand("POWER")
            _power.value = next
        } else {
            sendKey("POWER")
        }
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
            _speed.value = (_speed.value + 1).coerceAtMost(5)
        } else {
            sendKey("SPEED_UP")
        }
    }

    fun speedDown() {
        if (hasCustomCommands()) {
            sendLearnedCommand("SPEED_DOWN")
            _speed.value = (_speed.value - 1).coerceAtLeast(0)
        } else {
            sendKey("SPEED_DOWN")
        }
    }

    fun toggleSwing() {
        if (hasCustomCommands()) {
            sendLearnedCommand("SWING")
            _swing.value = !_swing.value
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

    fun deleteRemote() {
        val r = remote ?: return
        viewModelScope.launch {
            deleteLearned(r.id)
            repo.delete(r)
        }
    }
}