package com.example.iot.domain.usecase

import com.example.iot.data.MqttRepository
import com.example.iot.domain.model.TvState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTvStateUseCase @Inject constructor(
    private val repo: MqttRepository
) {
    operator fun invoke(nodeId: String): Flow<TvState> = repo.observeTvState(nodeId)
}
