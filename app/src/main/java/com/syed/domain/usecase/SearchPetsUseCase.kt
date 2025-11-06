package com.syed.domain.usecase

import com.syed.domain.model.Pet
import com.syed.domain.repository.PetRepository
import com.syed.domain.util.Result
import kotlinx.coroutines.flow.Flow

class SearchPetsUseCase(
    private val repository: PetRepository,
) {
    suspend operator fun invoke(query: String): Flow<Result<List<Pet>>> = repository.searchPets(query)
}
