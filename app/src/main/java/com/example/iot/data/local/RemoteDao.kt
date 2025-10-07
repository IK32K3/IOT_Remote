package com.example.iot.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: RemoteProfile): Long

    @Query("SELECT * FROM remote_profiles ORDER BY createdAt DESC")
    fun getAll(): Flow<List<RemoteProfile>>

    @Query("SELECT * FROM remote_profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RemoteProfile?

    @Delete
    suspend fun delete(entity: RemoteProfile)

    @Query("DELETE FROM remote_profiles WHERE id = :id")
    suspend fun deleteById(id: Long)
}
