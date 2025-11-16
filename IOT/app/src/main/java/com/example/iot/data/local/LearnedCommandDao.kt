package com.example.iot.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LearnedCommandDao {
    @Query("SELECT * FROM learned_commands WHERE remoteId = :remoteId")
    fun observeByRemote(remoteId: Long): Flow<List<LearnedCommandEntity>>

    @Query("SELECT DISTINCT remoteId FROM learned_commands")
    fun observeRemoteIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(commands: List<LearnedCommandEntity>)

    @Query("DELETE FROM learned_commands WHERE remoteId = :remoteId")
    suspend fun deleteByRemote(remoteId: Long)

    @Query("SELECT * FROM learned_commands WHERE remoteId = :remoteId AND `key` = :key LIMIT 1")
    suspend fun getCommand(remoteId: Long, key: String): LearnedCommandEntity?
}