package com.example.iot.feature.control.stb

import androidx.lifecycle.viewModelScope
import com.example.iot.core.mqtt.MqttTopics
import com.example.iot.data.RemoteRepository
import com.example.iot.data.local.RemoteProfile
import com.example.iot.domain.usecase.ObserveNodesUseCase
import com.example.iot.domain.usecase.ObserveStbStateUseCase
import com.example.iot.domain.usecase.PublishUseCase
import com.example.iot.feature.control.common.BaseControlViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

enum class StbPage { BASIC, DIGITS, MORE }

@HiltViewModel
class StbControlViewModel @Inject constructor(
    private val repo: RemoteRepository,
    private val publish: PublishUseCase,
    observeNodes: ObserveNodesUseCase,
    private val observeStbState: ObserveStbStateUseCase
) : BaseControlViewModel() {

    private val nodes = observeNodes()
    private val _page = MutableStateFlow(StbPage.BASIC)
    val page: StateFlow<StbPage> = _page

    override val isNodeOnline: StateFlow<Boolean> =
        combine(nodes, _nodeId) { m, id -> m[id] == true }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var remote: RemoteProfile? = null

    private val _power = MutableStateFlow(false)
    val power: StateFlow<Boolean> = _power

    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted

    override fun load(remoteId: String) {
        val id = remoteId.toLongOrNull() ?: return
        viewModelScope.launch {
            remote = repo.getById(id)
            remote?.let { r ->
                _title.value = "${r.brand} STB ${r.name}"
                _nodeId.value = r.nodeId
                observeStbState(r.nodeId).collect { s ->
                    _power.value = s.power
                    _muted.value = s.muted }
            }
        }
    }

    fun showBasic() = _page.tryEmit(StbPage.BASIC)
    fun showDigits() = _page.tryEmit(StbPage.DIGITS)
    fun showMore() = _page.tryEmit(StbPage.MORE)

    private fun sendKey(key: String) {
        val r = remote ?: return
        val payload = JSONObject().apply {
            put("cmd", "key")
            put("brand", r.brand); put("type", "STB"); put("index", r.codeSetIndex)
            put("key", key)
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

    // ===== More =====
    fun info() = sendKey("INFO")
    fun stop() = sendKey("STOP")
    fun subtitle() = sendKey("SUBTITLE")
    fun hash() = sendKey("HASH")
    fun star() = sendKey("STAR")
    fun a() = sendKey("A")
    fun b() = sendKey("B")
    fun c() = sendKey("C")
    fun d() = sendKey("D")
    fun epg() = sendKey("EPG")
    fun beijingWindow() = sendKey("BEIJING_WINDOW")

    fun deleteRemote() {
        val r = remote ?: return
        viewModelScope.launch { repo.delete(r) }
    }

}