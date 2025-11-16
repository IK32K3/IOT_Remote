package com.example.iot.domain.usecase

import com.example.iot.data.RemoteRepository
import com.example.iot.data.local.RemoteProfile
import javax.inject.Inject

class SaveRemoteUseCase @Inject constructor(
    private val repo: RemoteRepository
) {
    suspend operator fun invoke(p: RemoteProfile): Long = repo.save(p)
}
