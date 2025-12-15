package com.example.iot.feature.control.projector

import androidx.lifecycle.viewModelScope
import com.example.iot.core.mqtt.MqttTopics
import com.example.iot.data.RemoteRepository
import com.example.iot.data.local.RemoteProfile
import com.example.iot.domain.model.DeviceType
import com.example.iot.domain.model.LearnedCommand
import com.example.iot.domain.usecase.ObserveLearnedCommandsUseCase
import com.example.iot.domain.usecase.ObserveNodesUseCase
import com.example.iot.domain.usecase.PublishUseCase
import com.example.iot.feature.control.common.BaseControlViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

enum class ProjectorPage { BASIC, DIGITS, MORE }

@HiltViewModel
class ProjectorControlViewModel @Inject constructor(
    private val repo: RemoteRepository,
    private val publish: PublishUseCase,
    observeNodes: ObserveNodesUseCase,
    private val observeLearned: ObserveLearnedCommandsUseCase,
) : BaseControlViewModel() {

    override val isNodeOnline: StateFlow<Boolean> =
        combine(observeNodes(), _nodeId) { map, id -> map[id] == true }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var remote: RemoteProfile? = null
    private var remoteIdLong: Long? = null

    private val _page = MutableStateFlow(ProjectorPage.BASIC)
    val page: StateFlow<ProjectorPage> = _page

    private val _learnedCommands = MutableStateFlow<Map<String, LearnedCommand>>(emptyMap())
    private val learnedCommands: Map<String, LearnedCommand> get() = _learnedCommands.value

    override fun load(remoteId: String) {
        val id = remoteId.toLongOrNull() ?: return
        remoteIdLong = id
        viewModelScope.launch {
            remote = repo.getById(id)
            remote?.let { r ->
                _title.value = if (r.name.isNotBlank()) r.name else "${r.brand} Projector".trim()
                _nodeId.value = r.nodeId
                observeLearned(r.id).collect { list ->
                    _learnedCommands.value = list.associateBy { it.key.uppercase() }
                }
            }
        }
    }

    fun showBasic() = _page.tryEmit(ProjectorPage.BASIC)
    fun showDigits() = _page.tryEmit(ProjectorPage.DIGITS)
    fun showMore() = _page.tryEmit(ProjectorPage.MORE)

    private fun sendKey(key: String) {
        val r = remote ?: return
        val learned = learnedCommands[key.uppercase()]
        val payload = JSONObject().apply {
            put("cmd", "key")
            put("device", DeviceType.PROJECTOR.name.lowercase())
            put("brand", r.brand)
            put("type", r.deviceType.ifBlank { "PROJECTOR" })
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
        publish(MqttTopics.cmdTopic(r.nodeId, "projector"), payload)
    }

    fun power() = sendKey("POWER")
    fun mute() = sendKey("MUTE")
    fun freeze() = sendKey("FREEZE")
    fun source() = sendKey("SOURCE")

    fun volUp() = sendKey("VOL_UP")
    fun volDown() = sendKey("VOL_DOWN")
    fun pageUp() = sendKey("PAGE_UP")
    fun pageDown() = sendKey("PAGE_DOWN")
    fun zoomIn() = sendKey("ZOOM_IN")
    fun zoomOut() = sendKey("ZOOM_OUT")

    fun menu() = sendKey("MENU")
    fun exit() = sendKey("EXIT")
    fun info() = sendKey("INFO")
    fun back() = sendKey("BACK")
    fun video() = sendKey("VIDEO")
    fun trapUp() = sendKey("TRAP_UP")
    fun trapDown() = sendKey("TRAP_DOWN")
    fun usb() = sendKey("USB")

    fun ok() = sendKey("OK")
    fun up() = sendKey("UP")
    fun down() = sendKey("DOWN")
    fun left() = sendKey("LEFT")
    fun right() = sendKey("RIGHT")

    fun digit(d: Int) = sendKey("DIGIT_$d")
    fun dash() = sendKey("DASH")

    override fun deleteRemote(remoteId: String) {
        val id = remote?.id ?: remoteId.toLongOrNull() ?: remoteIdLong ?: return
        viewModelScope.launch { if (remote != null) repo.delete(remote!!) else repo.deleteById(id) }
    }
}
