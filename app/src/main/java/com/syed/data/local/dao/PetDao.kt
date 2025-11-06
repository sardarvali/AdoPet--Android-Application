package com.syed.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.syed.data.local.entities.PetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Query("SELECT * FROM pets WHERE available = 1 ORDER BY dateAdded DESC")
    fun getAllAvailablePets(): Flow<List<PetEntity>>

    @Query("SELECT * FROM pets WHERE id = :petId")
    fun getPetById(petId: String): Flow<PetEntity?>

    @Query("SELECT * FROM pets WHERE type = :type AND available = 1")
    fun getPetsByType(type: String): Flow<List<PetEntity>>

    @Query("SELECT * FROM pets WHERE adopted = 1 ORDER BY lastUpdated DESC")
    fun getAdoptedPets(): Flow<List<PetEntity>>

    @Query("SELECT * FROM pets WHERE name LIKE '%' || :query || '%' OR breed LIKE '%' || :query || '%'")
    fun searchPets(query: String): Flow<List<PetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPet(pet: PetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pets: List<PetEntity>)

    @Update
    suspend fun updatePet(pet: PetEntity)

    @Delete
    suspend fun deletePet(pet: PetEntity)

    @Query("DELETE FROM pets WHERE cachedAt < :timestamp")
    suspend fun deleteOldCache(timestamp: Long)

    @Query("DELETE FROM pets")
    suspend fun clearAll()
}
