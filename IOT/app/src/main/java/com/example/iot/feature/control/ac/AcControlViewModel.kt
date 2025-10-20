package com.example.iot.feature.control.ac

import androidx.lifecycle.viewModelScope
import com.example.iot.core.mqtt.MqttTopics
import com.example.iot.data.RemoteRepository
import com.example.iot.data.local.RemoteProfile
import com.example.iot.domain.usecase.ObserveAcStateUseCase
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
) : BaseControlViewModel() {

    private var remote: RemoteProfile? = null

    // UI state
    private val _power = MutableStateFlow(false)
    val power: StateFlow<Boolean> = _power

    private val _mode = MutableStateFlow("cool")
    val mode: StateFlow<String> = _mode

    private val _temp = MutableStateFlow(24)
    val temp: StateFlow<Int> = _temp

    private val _fan = MutableStateFlow("auto")
    val fan: StateFlow<String> = _fan

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
                _title.value = "${r.brand} ${r.type} ${r.codeSetIndex}"
                _nodeId.value = r.nodeId
                observe(r.nodeId)
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

    // —— Commands ——
    fun togglePower() {
        val r = remote ?: return
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

    fun deleteRemote() {
        val r = remote ?: return
        viewModelScope.launch {
            repo.delete(r)         // hoặc: repo.deleteById(r.id)
        }
    }
}