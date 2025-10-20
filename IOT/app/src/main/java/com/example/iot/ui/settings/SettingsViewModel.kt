package com.example.iot.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iot.data.prefs.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository
) : ViewModel() {

    data class UiState(val host: String, val port: Int, val node: String)

    private val _state = MutableStateFlow(UiState("10.0.2.2", 1883, "esp-bedroom"))
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            repo.settings.collectLatest { s ->
                _state.value = UiState(s.brokerHost, s.brokerPort, s.defaultNode)
            }
        }
    }

    fun save(host: String, port: Int, node: String) {
        viewModelScope.launch { repo.save(host, port, node) }
    }
}
