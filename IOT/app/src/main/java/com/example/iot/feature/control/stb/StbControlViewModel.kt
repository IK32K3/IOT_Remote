package com.example.iot.feature.control.stb

import androidx.lifecycle.viewModelScope
import com.example.iot.core.mqtt.MqttTopics
import com.example.iot.data.RemoteRepository
import com.example.iot.data.local.RemoteProfile
import com.example.iot.domain.usecase.ObserveNodesUseCase
import com.example.iot.domain.usecase.ObserveLearnedCommandsUseCase
import com.example.iot.domain.model.LearnedCommand
import com.example.iot.domain.usecase.PublishUseCase
import com.example.iot.feature.control.common.BaseControlViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

enum class StbPage { BASIC, DIGITS }

@HiltViewModel
class StbControlViewModel @Inject constructor(
    private val repo: RemoteRepository,
    private val publish: PublishUseCase,
    observeNodes: ObserveNodesUseCase,
    private val observeLearned: ObserveLearnedCommandsUseCase
) : BaseControlViewModel() {

    private val nodes = observeNodes()
    private val _page = MutableStateFlow(StbPage.BASIC)
    val page: StateFlow<StbPage> = _page

    override val isNodeOnline: StateFlow<Boolean> =
        combine(nodes, _nodeId) { m, id -> m[id] == true }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var remote: RemoteProfile? = null
    private var remoteIdLong: Long? = null

    private val _learnedCommands = MutableStateFlow<Map<String, LearnedCommand>>(emptyMap())
    private val learnedCommands: Map<String, LearnedCommand> get() = _learnedCommands.value

    override fun load(remoteId: String) {
        val id = remoteId.toLongOrNull() ?: return
        remoteIdLong = id
        viewModelScope.launch {
            remote = repo.getById(id)
            remote?.let { r ->
                _title.value = "${r.brand} STB ${r.name}"
                _nodeId.value = r.nodeId
                observeLearned(r.id).collect { list ->
                    _learnedCommands.value = list.associateBy { it.key.uppercase() }
                }
            }
        }
    }

    fun showBasic() { _page.value = StbPage.BASIC }
    fun showDigits() { _page.value = StbPage.DIGITS }

    private fun sendKey(key: String) {
        val r = remote ?: return
        val learned = learnedCommands[key.uppercase()]
        val payload = JSONObject().apply {
            put("cmd", "key")
            put("brand", r.brand)
            put("type", r.deviceType.ifBlank { "STB" })
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
        publish(MqttTopics.cmdTopic(r.nodeId, "stb"), payload)
    }

    // ===== Basic controls =====
    fun power() = sendKey("POWER")
    fun mute() = sendKey("MUTE")
    fun tvAv() = sendKey("TV_AV")
    fun volUp() = sendKey("VOL_UP")
    fun volDown() = sendKey("VOL_DOWN")
    fun pageUp() = sendKey("PAGE_UP")
    fun pageDown() = sendKey("PAGE_DOWN")
    fun chUp() = sendKey("CH_UP")
    fun chDown() = sendKey("CH_DOWN")
    fun menu() = sendKey("MENU")
    fun exit() = sendKey("EXIT")
    fun ok() = sendKey("OK")
    fun up() = sendKey("UP")
    fun down() = sendKey("DOWN")
    fun left() = sendKey("LEFT")
    fun right() = sendKey("RIGHT")

    // ===== Digits
    fun digit(d: Int) = sendKey("DIGIT_$d")
    fun dash() = sendKey("DASH")
    fun back() = sendKey("BACK")
    fun more() = sendKey("MORE")

    override fun deleteRemote(remoteId: String) {
        val id = remote?.id ?: remoteId.toLongOrNull() ?: remoteIdLong ?: return
        viewModelScope.launch { if (remote != null) repo.delete(remote!!) else repo.deleteById(id) }
    }

}
