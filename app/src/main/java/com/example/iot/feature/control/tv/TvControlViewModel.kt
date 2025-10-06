package com.example.iot.feature.control.tv

import androidx.lifecycle.viewModelScope
import com.example.iot.core.mqtt.MqttTopics
import com.example.iot.data.RemoteRepository
import com.example.iot.data.local.RemoteProfile
import com.example.iot.domain.usecase.ObserveNodesUseCase
import com.example.iot.domain.usecase.ObserveTvStateUseCase
import com.example.iot.domain.usecase.PublishUseCase
import com.example.iot.feature.control.common.BaseControlViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class TvControlViewModel @Inject constructor(
    private val repo: RemoteRepository,
    private val publish: PublishUseCase,
    observeNodes: ObserveNodesUseCase,
    private val observeTvState: ObserveTvStateUseCase
) : BaseControlViewModel() {

    private var remote: RemoteProfile? = null

    private val _power  = MutableStateFlow(false);  val power: StateFlow<Boolean> = _power
    private val _muted  = MutableStateFlow(false)
    private val _volume = MutableStateFlow(0)
    private val _channel= MutableStateFlow(1)

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
                _title.value = "${r.brand} TV ${r.codeSetIndex}"
                _nodeId.value = r.nodeId

                // collect TV state từ broker
                observeTvState(r.nodeId).collect { s ->
                    _power.value = s.power
                    _muted.value = s.muted
                    _volume.value = s.volume
                    _channel.value = s.channel
                }
            }
        }
    }

    private fun sendKey(key: String) {
        val r = remote ?: return
        val topic = MqttTopics.cmdTopic(r.nodeId, "tv")
        val payload = JSONObject().apply {
            put("cmd", "key")
            put("brand", r.brand); put("type", "TV"); put("index", r.codeSetIndex)
            put("key", key)
        }.toString()
        publish(topic, payload)
    }

    // ==== Đủ nút ====
    // Power / Mute / Input
    fun togglePower() { _power.value = !_power.value; sendKey("POWER") }
    fun toggleMute()  { _muted.value = !_muted.value; sendKey("MUTE") }
    fun tvAv()        = sendKey("TV_AV")     // hoặc "INPUT" tùy firmware

    // Volume / Channel
    fun volUp()   = sendKey("VOL_UP")
    fun volDown() = sendKey("VOL_DOWN")
    fun chUp()    = sendKey("CH_UP")
    fun chDown()  = sendKey("CH_DOWN")

    // Menu / Exit / Home / Back
    fun menu()    = sendKey("MENU")
    fun exit()    = sendKey("EXIT")
    fun home()    = sendKey("HOME")
    fun back()    = sendKey("BACK")

    // D-pad + OK
    fun ok()      = sendKey("OK")
    fun dirUp()   = sendKey("UP")
    fun dirDown() = sendKey("DOWN")
    fun dirLeft() = sendKey("LEFT")
    fun dirRight()= sendKey("RIGHT")

    // Digits
    fun digit(d: Int) = sendKey("DIGIT_$d")  // 0..9
    fun dash()        = sendKey("DASH")      // −/−−
}
