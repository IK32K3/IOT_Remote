package com.example.iot.domain.usecase

import com.example.iot.data.LearnedCommandRepository
import javax.inject.Inject

class DeleteLearnedCommandsUseCase @Inject constructor(
    private val repo: LearnedCommandRepository
) {
    suspend operator fun invoke(remoteId: Long) {
        repo.deleteByRemote(remoteId)
    }
}