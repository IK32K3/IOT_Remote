package com.example.iot.domain.usecase

import com.example.iot.data.MqttRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveNodesUseCase @Inject constructor(
    private val repo: MqttRepository
) {
    operator fun invoke(): StateFlow<Map<String, Boolean>> = repo.nodesOnline()
}
