package com.example.iot.data

import com.example.iot.data.local.RemoteDao
import com.example.iot.data.local.RemoteProfile
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteRepository @Inject constructor(
    private val dao: RemoteDao
) {
    fun all(): Flow<List<RemoteProfile>> = dao.getAll()
    suspend fun save(p: RemoteProfile) = dao.insert(p)

    suspend fun getById(id: Long): RemoteProfile? = dao.getById(id)

    suspend fun delete(profile: RemoteProfile) = dao.delete(profile)

    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
