package com.syed.data.repository

import com.syed.data.datasource.PetDataSource
import com.syed.domain.model.Pet
import com.syed.domain.repository.PetRepository
import com.syed.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Named

class PetRepositoryImpl
    @Inject
    constructor(
        @Named("firebase") private val remoteDataSource: PetDataSource,
    ) : PetRepository {
        override suspend fun getPets(type: String): Flow<Result<List<Pet>>> =
            flow {
                emit(Result.Loading)
                try {
                    val pets = remoteDataSource.getPets(type)
                    emit(Result.Success(pets))
                } catch (e: Exception) {
                    emit(Result.Error(e.message ?: "Unknown error occurred"))
                }
            }

        override suspend fun getPetById(id: String): Result<Pet> =
            try {
                val pet = remoteDataSource.getPetById(id)
                if (pet != null) {
                    Result.Success(pet)
                } else {
                    Result.Error("Pet not found")
                }
            } catch (e: Exception) {
                Result.Error(e.message ?: "Unknown error occurred")
            }

        override suspend fun addPet(pet: Pet): Result<String> =
            try {
                val id = remoteDataSource.addPet(pet)
                Result.Success(id)
            } catch (e: Exception) {
                Result.Error(e.message ?: "Failed to add pet")
            }

        override suspend fun updatePet(pet: Pet): Result<Unit> =
            try {
                remoteDataSource.updatePet(pet)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e.message ?: "Failed to update pet")
            }

        override suspend fun deletePet(id: String): Result<Unit> =
            try {
                remoteDataSource.deletePet(id)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e.message ?: "Failed to delete pet")
            }

        override suspend fun searchPets(query: String): Flow<Result<List<Pet>>> =
            flow {
                emit(Result.Loading)
                try {
                    val pets = remoteDataSource.searchPets(query)
                    emit(Result.Success(pets))
                } catch (e: Exception) {
                    emit(Result.Error(e.message ?: "Search failed"))
                }
            }

        override suspend fun refreshPets(type: String): Result<List<Pet>> =
            try {
                val pets = remoteDataSource.getPets(type)
                Result.Success(pets)
            } catch (e: Exception) {
                Result.Error(e.message ?: "Refresh failed")
            }
    }
