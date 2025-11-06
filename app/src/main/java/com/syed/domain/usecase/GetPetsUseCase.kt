package com.syed.domain.usecase

import com.syed.domain.model.Pet
import com.syed.domain.repository.PetRepository
import com.syed.domain.util.Result
import kotlinx.coroutines.flow.Flow

class GetPetsUseCase(
    private val repository: PetRepository,
) {
    suspend operator fun invoke(
        type: String,
        forceRefresh: Boolean = false,
    ): Flow<Result<List<Pet>>> =
        if (forceRefresh) {
            kotlinx.coroutines.flow.flow {
                emit(Result.Loading)
                val result = repository.refreshPets(type)
                when (result) {
                    is Result.Success -> emit(Result.Success(result.data))
                    is Result.Error -> emit(Result.Error(result.message, result.throwable))
                    is Result.Loading -> emit(Result.Loading)
                }
            }
        } else {
            repository.getPets(type)
        }
}
