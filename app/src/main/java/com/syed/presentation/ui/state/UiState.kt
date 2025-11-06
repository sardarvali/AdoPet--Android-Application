package com.syed.presentation.ui.state

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()

    data class Success<T>(
        val data: T,
    ) : UiState<T>()

    data class Error(
        val message: String,
    ) : UiState<Nothing>()

    object Idle : UiState<Nothing>()
}

data class PetsListUiState(
    val pets: List<com.syed.domain.model.Pet> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false,
)
