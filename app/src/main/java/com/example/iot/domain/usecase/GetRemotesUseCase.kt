package com.example.iot.domain.usecase

import com.example.iot.data.RemoteRepository
import com.example.iot.data.local.RemoteProfile
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRemotesUseCase @Inject constructor(
    private val repo: RemoteRepository
) {
    operator fun invoke(): Flow<List<RemoteProfile>> = repo.all()
}
