package com.example.iot.data

import com.example.iot.data.local.LearnedCommandDao
import com.example.iot.data.local.LearnedCommandEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LearnedCommandRepository @Inject constructor(
    private val dao: LearnedCommandDao
) {
    fun commandsForRemote(remoteId: Long): Flow<List<LearnedCommandEntity>> =
        dao.observeByRemote(remoteId)

    fun remoteIdsWithCommands(): Flow<Set<Long>> =
        dao.observeRemoteIds().map { it.toSet() }

    suspend fun replaceForRemote(remoteId: Long, commands: List<LearnedCommandEntity>) {
        dao.deleteByRemote(remoteId)
        if (commands.isNotEmpty()) {
            dao.insertAll(commands)
        }
    }

    suspend fun deleteByRemote(remoteId: Long) {
        dao.deleteByRemote(remoteId)
    }

    suspend fun commandForKey(remoteId: Long, key: String): LearnedCommandEntity? =
        dao.getCommand(remoteId, key.uppercase())
}