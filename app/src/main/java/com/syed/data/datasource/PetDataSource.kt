package com.syed.data.datasource

import com.syed.domain.model.Pet
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface PetDataSource {
    suspend fun getPets(
        type: String,
        limit: Int = 50,
    ): List<Pet>

    suspend fun getPetById(id: String): Pet?

    suspend fun addPet(pet: Pet): String

    suspend fun updatePet(pet: Pet)

    suspend fun deletePet(id: String)

    suspend fun searchPets(query: String): List<Pet>
}
