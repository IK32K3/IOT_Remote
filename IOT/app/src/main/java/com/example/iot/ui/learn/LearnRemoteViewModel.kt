package com.example.iot.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iot.core.mqtt.MqttTopics
import com.example.iot.domain.model.DeviceType
import com.example.iot.domain.model.IrLearningEvent
import com.example.iot.domain.model.LearnedCommandDraft
import com.example.iot.domain.usecase.ObserveIrLearningUseCase
import com.example.iot.domain.usecase.PublishUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class LearnRemoteViewModel @Inject constructor(
    private val publish: PublishUseCase,
    private val observeLearning: ObserveIrLearningUseCase
) : ViewModel() {

    enum class LearnStatus { IDLE, LEARNING, SUCCESS, ERROR }

    data class LearnKeyState(
        val status: LearnStatus = LearnStatus.IDLE,
        val command: LearnedCommandDraft? = null,
        val error: String? = null
    )

    private val _deviceType = MutableStateFlow(DeviceType.AC)
    val deviceType: StateFlow<DeviceType> = _deviceType.asStateFlow()

    private val _statusMessage = MutableStateFlow("Chọn một nút để bắt đầu học lệnh.")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _states = MutableStateFlow<Map<String, LearnKeyState>>(emptyMap())
    val states: StateFlow<Map<String, LearnKeyState>> = _states.asStateFlow()

    private var mandatoryKeys: Set<String> = emptySet()
    private var nodeId: String = ""
    private var initialized = false

    val canSave: StateFlow<Boolean> = combine(states, deviceType) { map, _ ->
        if (mandatoryKeys.isEmpty()) return@combine map.values.any { it.command != null }
        mandatoryKeys.all { key -> map[key]?.command != null }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun configure(
        deviceType: DeviceType,
        nodeId: String,
        keys: Set<String>,
        mandatoryKeys: Set<String>
    ) {
        if (initialized) return
        initialized = true
        this.nodeId = nodeId
        this.mandatoryKeys = mandatoryKeys.map { it.uppercase() }.toSet()
        _deviceType.value = deviceType
        _states.value = keys.associate { it.uppercase() to LearnKeyState() }

        viewModelScope.launch {
            observeLearning(nodeId)
                .filter { it.device == deviceType }
                .collect { handleEvent(it) }
        }
    }

    private fun handleEvent(event: IrLearningEvent) {
        val key = event.key.uppercase()
        _states.update { current ->
            val existing = current[key] ?: LearnKeyState()
            if (!event.success) {
                _statusMessage.value = event.error ?: "Không thể học lệnh cho $key"
                current + (key to existing.copy(status = LearnStatus.ERROR, command = null, error = event.error))
            } else {
                val protocol = event.protocol
                val code = event.code
                val bits = event.bits
                if (protocol.isNullOrBlank() || code.isNullOrBlank() || bits == null) {
                    _statusMessage.value = "Thiếu dữ liệu cho lệnh $key"
                    current + (key to existing.copy(status = LearnStatus.ERROR, command = null, error = "missing_payload"))
                } else {
                    val draft = LearnedCommandDraft(
                        deviceType = _deviceType.value,
                        key = key,
                        protocol = protocol,
                        code = code,
                        bits = bits
                    )
                    _statusMessage.value = "Đã học xong nút $key"
                    current + (key to existing.copy(status = LearnStatus.SUCCESS, command = draft, error = null))
                }
            }
        }
    }

    fun startLearning(key: String) {
        val normalized = key.uppercase()
        if (!_states.value.containsKey(normalized)) return
        _statusMessage.value = "Đang chờ tín hiệu cho nút $normalized..."
        _states.update { current ->
            current + (normalized to LearnKeyState(status = LearnStatus.LEARNING))
        }
        val payload = JSONObject().apply {
            put("device", _deviceType.value.name)
            put("cmd", "learn")
            put("key", normalized)
        }.toString()
        publish(MqttTopics.learnRequestTopic(nodeId), payload)
    }

    fun learnedCommands(): List<LearnedCommandDraft> =
        _states.value.values.mapNotNull { it.command }
}