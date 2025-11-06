package com.syed.domain.repository

import com.syed.domain.model.Pet
import com.syed.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface PetRepository {
    suspend fun getPets(type: String): Flow<Result<List<Pet>>>

    suspend fun getPetById(id: String): Result<Pet>

    suspend fun addPet(pet: Pet): Result<String>

    suspend fun updatePet(pet: Pet): Result<Unit>

    suspend fun deletePet(id: String): Result<Unit>

    suspend fun searchPets(query: String): Flow<Result<List<Pet>>>

    suspend fun refreshPets(type: String): Result<List<Pet>>
}
