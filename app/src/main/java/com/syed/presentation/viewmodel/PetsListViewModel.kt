package com.syed.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syed.di.AppModule
import com.syed.domain.model.Pet
import com.syed.domain.usecase.GetPetsUseCase
import com.syed.domain.usecase.SearchPetsUseCase
import com.syed.presentation.ui.state.PetsListUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PetsListViewModel : ViewModel() {
    private val repository = AppModule.providePetRepository()
    private val getPetsUseCase = GetPetsUseCase(repository)
    private val searchPetsUseCase = SearchPetsUseCase(repository)

    private val _uiState = MutableStateFlow(PetsListUiState())
    val uiState: StateFlow<PetsListUiState> = _uiState.asStateFlow()

    fun loadPets(
        type: String,
        forceRefresh: Boolean = false,
    ) {
        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isLoading = !forceRefresh,
                    isRefreshing = forceRefresh,
                    error = null,
                )

            getPetsUseCase(type, forceRefresh).collect { result: com.syed.domain.util.Result<List<Pet>> ->
                when (result) {
                    is com.syed.domain.util.Result.Loading -> {
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = !forceRefresh,
                                isRefreshing = forceRefresh,
                            )
                    }
                    is com.syed.domain.util.Result.Success<List<Pet>> -> {
                        _uiState.value =
                            _uiState.value.copy(
                                pets = result.data,
                                isLoading = false,
                                isRefreshing = false,
                                error = null,
                            )
                    }
                    is com.syed.domain.util.Result.Error -> {
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = result.message,
                            )
                    }
                }
            }
        }
    }

    fun searchPets(query: String) {
        if (query.isBlank()) {
            loadPets("all")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            searchPetsUseCase(query).collect { result: com.syed.domain.util.Result<List<Pet>> ->
                when (result) {
                    is com.syed.domain.util.Result.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    is com.syed.domain.util.Result.Success<List<Pet>> -> {
                        _uiState.value =
                            _uiState.value.copy(
                                pets = result.data,
                                isLoading = false,
                                error = null,
                            )
                    }
                    is com.syed.domain.util.Result.Error -> {
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                error = result.message,
                            )
                    }
                }
            }
        }
    }

    fun retry(type: String) {
        loadPets(type, forceRefresh = true)
    }
}
