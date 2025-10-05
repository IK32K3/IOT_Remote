package com.example.iot.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iot.domain.usecase.ObserveNodesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class NodeSelectViewModel @Inject constructor(
    observeNodes: ObserveNodesUseCase
) : ViewModel() {
    // hiển thị tất cả node (online/offline). Bạn có thể filter nếu thích
    val nodes: StateFlow<List<String>> =
        observeNodes().map { it.keys.sorted() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}
