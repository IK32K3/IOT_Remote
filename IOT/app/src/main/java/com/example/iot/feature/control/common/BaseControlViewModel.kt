package com.example.iot.feature.control.common

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseControlViewModel : ViewModel() {
    protected val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title

    protected val _nodeId = MutableStateFlow("")
    abstract val isNodeOnline: StateFlow<Boolean>

    abstract fun load(remoteId: String)
    abstract fun deleteRemote(remoteId: String)
}
