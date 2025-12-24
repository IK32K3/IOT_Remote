package com.example.iot.feature.control.ac

import androidx.lifecycle.viewModelScope
import com.example.iot.core.mqtt.MqttTopics
import com.example.iot.data.RemoteRepository
import com.example.iot.data.local.RemoteProfile
import com.example.iot.domain.model.LearnedCommand
import com.example.iot.domain.usecase.DeleteLearnedCommandsUseCase
import com.example.iot.domain.usecase.ObserveAcStateUseCase
import com.example.iot.domain.usecase.ObserveLearnedCommandsUseCase
import com.example.iot.domain.usecase.ObserveNodesUseCase
import com.example.iot.domain.usecase.PublishUseCase
import com.example.iot.feature.control.common.BaseControlViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class AcControlViewModel @Inject constructor(
    private val repo: RemoteRepository,
    private val publish: PublishUseCase,
    private val observeAcState: ObserveAcStateUseCase,
    observeNodes: ObserveNodesUseCase,
    private val observeLearned: ObserveLearnedCommandsUseCase,
    private val deleteLearned: DeleteLearnedCommandsUseCase,
) : BaseControlViewModel() {

    private var remote: RemoteProfile? = null
    private var remoteIdLong: Long? = null

    // UI state
    private val _power = MutableStateFlow(false)
    val power: StateFlow<Boolean> = _power

    private val _mode = MutableStateFlow("cool")
    val mode: StateFlow<String> = _mode

    private val _temp = MutableStateFlow(24)
    val temp: StateFlow<Int> = _temp

    private val _fan = MutableStateFlow("auto")
    val fan: StateFlow<String> = _fan

    private val _learnedCommands = MutableStateFlow<Map<String, LearnedCommand>>(emptyMap())
    private val learnedCommands: Map<String, LearnedCommand> get() = _learnedCommands.value

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
                    else -> "${r.brand} ${r.type} ${r.codeSetIndex}".trim()
                }
                _nodeId.value = r.nodeId
                observeLearnedCommands(id)
                observe(r.nodeId)
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

    private fun observe(nodeId: String) {
        viewModelScope.launch {
            observeAcState(nodeId).collectLatest { s ->
                _power.value = s.power
                _mode.value = s.mode
                _temp.value = s.temp
                _fan.value  = s.fan
            }
        }
    }

    private fun hasCustomCommands(): Boolean = _learnedCommands.value.isNotEmpty()

    private fun sendLearnedCommand(key: String): Boolean {
        val r = remote ?: return false
        val cmd = learnedCommands[key.uppercase()] ?: return false
        val payload = JSONObject().apply {
            put("device", "ac")
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

    // —— Commands ——
    fun togglePower() {
        val r = remote ?: return
        if (hasCustomCommands()) {
            val np = !_power.value
            val key = if (np) "POWER_ON" else "POWER_OFF"
            val sent = sendLearnedCommand(key) || sendLearnedCommand("POWER")
            if (sent) {
                _power.value = np
                return
            }
        }
        val np = !_power.value
        _power.value = np
        val payload = JSONObject().apply {
            put("cmd", "power"); put("value", np)
            put("brand", r.brand); put("type", r.type); put("index", r.codeSetIndex)
        }.toString()
        publish(MqttTopics.testIrTopic(r.nodeId), payload)
    }

    fun setTemp(newTemp: Int) {
        val r = remote ?: return
        if (hasCustomCommands()) {
            val diff = (newTemp - _temp.value).coerceIn(-5, 5)
            when {
                diff > 0 -> repeat(diff) { sendLearnedCommand("TEMP_UP") }
                diff < 0 -> repeat(-diff) { sendLearnedCommand("TEMP_DOWN") }
            }
            if (diff != 0) {
                _temp.value = (_temp.value + diff).coerceIn(16, 30)
            }
            return
        }
        val t = newTemp.coerceIn(16, 30)
        _temp.value = t
        val payload = JSONObject().apply {
            put("cmd", "set")
            put("brand", r.brand); put("type", r.type); put("index", r.codeSetIndex)
            put("temp", t); put("mode", _mode.value); put("fan", _fan.value)
        }.toString()
        publish(MqttTopics.testIrTopic(r.nodeId), payload)
    }

    fun cycleMode() {
        val r = remote ?: return
        if (hasCustomCommands()) {
            sendLearnedCommand("MODE")
            val next = when (_mode.value) {
                "cool" -> "dry"
                "dry" -> "fan"
                "fan" -> "heat"
                "heat" -> "auto"
                else -> "cool"
            }
            _mode.value = next
            return
        }
        val next = when (_mode.value) {
            "cool" -> "dry"; "dry" -> "fan"; "fan" -> "heat"; "heat" -> "auto"; else -> "cool"
        }
        _mode.value = next
        val payload = JSONObject().apply {
            put("cmd", "set")
            put("brand", r.brand); put("type", r.type); put("index", r.codeSetIndex)
            put("temp", _temp.value); put("mode", next); put("fan", _fan.value)
        }.toString()
        publish(MqttTopics.testIrTopic(r.nodeId), payload)
    }

    fun fanKey() {
        if (hasCustomCommands()) {
            sendLearnedCommand("FAN")
        }
    }

    fun swingKey() {
        if (hasCustomCommands()) {
            sendLearnedCommand("SWING")
        }
    }

    fun coolKey() {
        if (hasCustomCommands()) {
            sendLearnedCommand("COOL")
        }
    }

    fun heatKey() {
        if (hasCustomCommands()) {
            sendLearnedCommand("HEAT")
        }
    }

    fun turboKey() {
        if (hasCustomCommands()) {
            sendLearnedCommand("TURBO")
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
