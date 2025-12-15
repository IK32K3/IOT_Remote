package com.example.iot.feature.control.dvd

import androidx.lifecycle.viewModelScope
import com.example.iot.core.mqtt.MqttTopics
import com.example.iot.data.RemoteRepository
import com.example.iot.data.local.RemoteProfile
import com.example.iot.domain.model.LearnedCommand
import com.example.iot.domain.model.DeviceType
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

enum class DvdPage { BASIC, DIGITS, MORE }

@HiltViewModel
class DvdControlViewModel @Inject constructor(
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

    private val _page = MutableStateFlow(DvdPage.BASIC)
    val page: StateFlow<DvdPage> = _page

    private val _learnedCommands =
        MutableStateFlow<Map<String, LearnedCommand>>(emptyMap())
    private val learnedCommands: Map<String, LearnedCommand>
        get() = _learnedCommands.value

    override fun load(remoteId: String) {
        val id = remoteId.toLongOrNull() ?: return
        remoteIdLong = id
        viewModelScope.launch {
            remote = repo.getById(id)
            remote?.let { r ->
                _title.value = if (r.name.isNotBlank()) r.name else "${r.brand} DVD".trim()
                _nodeId.value = r.nodeId
                observeLearned(r.id).collect { list ->
                    _learnedCommands.value = list.associateBy { it.key.uppercase() }
                }
            }
        }
    }

    fun showBasic() = _page.tryEmit(DvdPage.BASIC)
    fun showDigits() = _page.tryEmit(DvdPage.DIGITS)
    fun showMore() = _page.tryEmit(DvdPage.MORE)

    private fun sendKey(key: String) {
        val r = remote ?: return
        val learned = learnedCommands[key.uppercase()]
        val payload = JSONObject().apply {
            put("cmd", "key")
            put("device", DeviceType.DVD.name.lowercase())
            put("brand", r.brand)
            put("type", r.deviceType.ifBlank { "DVD" })
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
        publish(MqttTopics.cmdTopic(r.nodeId, "dvd"), payload)
    }

    fun power() = sendKey("POWER")
    fun mute() = sendKey("MUTE")
    fun eject() = sendKey("EJECT")

    fun volUp() = sendKey("VOL_UP")
    fun volDown() = sendKey("VOL_DOWN")

    fun rew() = sendKey("REW")
    fun ff() = sendKey("FF")
    fun playPause() = sendKey("PLAY_PAUSE")
    fun stop() = sendKey("STOP")
    fun prev() = sendKey("PREV")
    fun next() = sendKey("NEXT")

    fun menu() = sendKey("MENU")
    fun exit() = sendKey("EXIT")
    fun home() = sendKey("HOME")

    fun ok() = sendKey("OK")
    fun up() = sendKey("UP")
    fun down() = sendKey("DOWN")
    fun left() = sendKey("LEFT")
    fun right() = sendKey("RIGHT")

    fun digit(d: Int) = sendKey("DIGIT_$d")
    fun dash() = sendKey("DASH")
    fun back() = sendKey("BACK")

    fun title() = sendKey("TITLE")
    fun subtitle() = sendKey("SUBTITLE")
    fun red() = sendKey("RED")
    fun green() = sendKey("GREEN")
    fun blue() = sendKey("BLUE")
    fun yellow() = sendKey("YELLOW")

    override fun deleteRemote(remoteId: String) {
        val id = remote?.id ?: remoteId.toLongOrNull() ?: remoteIdLong ?: return
        viewModelScope.launch {
            if (remote != null) repo.delete(remote!!) else repo.deleteById(id)
        }
    }
}
