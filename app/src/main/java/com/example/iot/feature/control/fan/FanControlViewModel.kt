package com.example.iot.feature.control.fan

import androidx.lifecycle.viewModelScope
import com.example.iot.core.mqtt.MqttTopics
import com.example.iot.data.RemoteRepository
import com.example.iot.data.local.RemoteProfile
import com.example.iot.domain.usecase.ObserveFanStateUseCase
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
    private val observeFan: ObserveFanStateUseCase
) : BaseControlViewModel() {

    private var remote: RemoteProfile? = null

    private val _power = MutableStateFlow(false)
    val power: StateFlow<Boolean> = _power

    private val _speed = MutableStateFlow(0)
    val speed: StateFlow<Int> = _speed

    private val _swing = MutableStateFlow(false)
    val swing: StateFlow<Boolean> = _swing

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
                observeFan(r.nodeId).collect { s ->
                    _power.value = s.power
                    _speed.value = s.speed
                    _swing.value = s.swing
                }
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

    // --- các nút ---
    fun togglePower() { sendKey("POWER") }
    fun timer()       { sendKey("TIMER") }
    fun speedUp()     { sendKey("SPEED_UP") }
    fun speedDown()   { sendKey("SPEED_DOWN") }
    fun toggleSwing() { sendKey("SWING") }
    fun type()        { sendKey("TYPE") }

    fun deleteRemote() {
        val r = remote ?: return
        viewModelScope.launch { repo.delete(r) }
    }
}
