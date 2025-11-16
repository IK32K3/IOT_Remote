package com.example.iot.domain.usecase

import com.example.iot.data.LearnedCommandRepository
import com.example.iot.data.local.LearnedCommandEntity
import com.example.iot.domain.model.LearnedCommandDraft
import javax.inject.Inject

class SaveLearnedCommandsUseCase @Inject constructor(
    private val repo: LearnedCommandRepository
) {
    suspend operator fun invoke(remoteId: Long, drafts: List<LearnedCommandDraft>) {
        if (drafts.isEmpty()) {
            repo.deleteByRemote(remoteId)
            return
        }
        val entities = drafts.map {
            LearnedCommandEntity(
                remoteId = remoteId,
                deviceType = it.deviceType.name,
                key = it.key.uppercase(),
                protocol = it.protocol,
                code = it.code,
                bits = it.bits
            )
        }
        repo.replaceForRemote(remoteId, entities)
    }
}