package com.syed.data.local.dao

import androidx.room.*
import com.syed.data.local.entities.AdoptionRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdoptionRequestDao {
    @Query("SELECT * FROM adoption_requests WHERE userId = :userId ORDER BY requestDate DESC")
    fun getRequestsByUser(userId: String): Flow<List<AdoptionRequestEntity>>

    @Query("SELECT * FROM adoption_requests WHERE status = :status ORDER BY requestDate DESC")
    fun getRequestsByStatus(status: String): Flow<List<AdoptionRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: AdoptionRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(requests: List<AdoptionRequestEntity>)

    @Update
    suspend fun updateRequest(request: AdoptionRequestEntity)

    @Delete
    suspend fun deleteRequest(request: AdoptionRequestEntity)

    @Query("DELETE FROM adoption_requests")
    suspend fun clearAll()
}
