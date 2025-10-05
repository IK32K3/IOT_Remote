package com.example.iot.domain.usecase

import com.example.iot.data.MqttRepository
import com.example.iot.domain.model.AcState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAcStateUseCase @Inject constructor(
    private val repo: MqttRepository
) {
    operator fun invoke(nodeId: String): Flow<AcState> = repo.observeAcState(nodeId)
}
