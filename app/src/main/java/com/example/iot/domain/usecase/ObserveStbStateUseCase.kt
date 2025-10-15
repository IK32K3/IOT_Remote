package com.example.iot.domain.usecase

import com.example.iot.data.MqttRepository
import com.example.iot.domain.model.StbState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveStbStateUseCase @Inject constructor(
    private val repo: MqttRepository
) {
    operator fun invoke(nodeId: String): Flow<StbState> = repo.observeStbState(nodeId)
}
