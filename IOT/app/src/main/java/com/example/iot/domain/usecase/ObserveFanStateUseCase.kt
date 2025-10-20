package com.example.iot.domain.usecase

import com.example.iot.data.MqttRepository
import com.example.iot.domain.model.FanState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFanStateUseCase @Inject constructor(
    private val repo: MqttRepository
) {
    operator fun invoke(nodeId: String): Flow<FanState> = repo.observeFanState(nodeId)
}
