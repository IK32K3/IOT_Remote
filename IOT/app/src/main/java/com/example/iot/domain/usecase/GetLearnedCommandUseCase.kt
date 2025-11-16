package com.example.iot.domain.usecase

import com.example.iot.data.LearnedCommandRepository
import com.example.iot.domain.model.DeviceType
import com.example.iot.domain.model.LearnedCommand
import javax.inject.Inject

class GetLearnedCommandUseCase @Inject constructor(
    private val repo: LearnedCommandRepository
) {
    suspend operator fun invoke(remoteId: Long, key: String): LearnedCommand? {
        val entity = repo.commandForKey(remoteId, key)
        return entity?.let {
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