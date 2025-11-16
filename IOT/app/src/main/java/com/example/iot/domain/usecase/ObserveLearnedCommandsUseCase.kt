package com.example.iot.domain.usecase

import com.example.iot.data.LearnedCommandRepository
import com.example.iot.domain.model.DeviceType
import com.example.iot.domain.model.LearnedCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveLearnedCommandsUseCase @Inject constructor(
    private val repo: LearnedCommandRepository
) {
    operator fun invoke(remoteId: Long): Flow<List<LearnedCommand>> =
        repo.commandsForRemote(remoteId).map { entities ->
            entities.map {
                LearnedCommand(
                    remoteId = it.remoteId,
                    deviceType = DeviceType.from(it.deviceType),
                    key = it.key,
                    protocol = it.protocol,
                    code = it.code,
                    bits = it.bits
                )
            }
        }
}