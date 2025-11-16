package com.example.iot.domain.usecase

import com.example.iot.data.MqttRepository
import com.example.iot.domain.model.IrLearningEvent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveIrLearningUseCase @Inject constructor(
    private val repo: MqttRepository
) {
    operator fun invoke(nodeId: String): Flow<IrLearningEvent> = repo.observeIrLearning(nodeId)
}