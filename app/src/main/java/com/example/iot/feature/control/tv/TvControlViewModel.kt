package com.example.iot.feature.control.tv

import androidx.lifecycle.viewModelScope
import com.example.iot.feature.control.common.BaseControlViewModel
import com.example.iot.domain.usecase.GetRemotesUseCase
import com.example.iot.domain.usecase.ObserveNodesUseCase
import com.example.iot.domain.usecase.PublishUseCase
import com.example.iot.data.RemoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class TvControlViewModel @Inject constructor(
    private val getRemotes: GetRemotesUseCase,
    private val observeNodes: ObserveNodesUseCase,
    private val publish: PublishUseCase,
    private val remoteRepo: RemoteRepository
) : BaseControlViewModel() {

    private val _online = MutableStateFlow(true)
    override val isNodeOnline: StateFlow<Boolean> = _online

    private val _power = MutableStateFlow(false)
    val power: StateFlow<Boolean> = _power

    private var topic: String = "" // iot/nodes/{nodeId}/ir/tv

    override fun load(remoteId: String) {
        viewModelScope.launch {
            val remote = getRemotes.getById(remoteId)   // hàm getById trong usecase/repo của bạn
            _title.value = "${remote.brand} ${remote.name}"
            _nodeId.value = remote.nodeId
            topic = "iot/nodes/${remote.nodeId}/ir/tv"

            // online/offline
            launch {
                observeNodes().collect { nodes ->
                    _online.value = nodes.any { it.id == remote.nodeId && it.online }
                }
            }
        }
    }

    /* ==== publish helpers ==== */
    private fun sendKey(key: String) = viewModelScope.launch {
        // ví dụ payload đơn giản; tùy phần ESP bạn có thể đổi thành {"key":"..."}
        val payload = """{"key":"$key"}"""
        publish(topic, payload)
    }

    /* ==== actions bound từ UI ==== */
    fun togglePower() {
        _power.value = !_power.value
        sendKey("power")
    }
    fun mute()            = sendKey("mute")
    fun tvAv()            = sendKey("tvav")

    fun volUp()           = sendKey("vol+")
    fun volDown()         = sendKey("vol-")
    fun chUp()            = sendKey("ch+")
    fun chDown()          = sendKey("ch-")

    fun menu()            = sendKey("menu")
    fun exit()            = sendKey("exit")

    fun digit(n: Int)     = sendKey(n.toString())

    fun navUp()           = sendKey("up")
    fun navDown()         = sendKey("down")
    fun navLeft()         = sendKey("left")
    fun navRight()        = sendKey("right")
    fun ok()              = sendKey("ok")

    fun delete(remoteId: String) = viewModelScope.launch {
        remoteRepo.delete(remoteId)
    }
}
