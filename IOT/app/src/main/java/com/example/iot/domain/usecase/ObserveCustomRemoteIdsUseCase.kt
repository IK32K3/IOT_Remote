
package com.example.iot.domain.usecase

import com.example.iot.data.LearnedCommandRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveCustomRemoteIdsUseCase @Inject constructor(
    private val repo: LearnedCommandRepository
) {
    operator fun invoke(): Flow<Set<Long>> = repo.remoteIdsWithCommands()
}