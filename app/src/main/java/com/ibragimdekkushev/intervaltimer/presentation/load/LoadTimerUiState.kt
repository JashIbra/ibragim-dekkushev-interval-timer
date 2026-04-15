package com.ibragimdekkushev.intervaltimer.presentation.load

sealed interface LoadTimerUiState {
    data object Idle : LoadTimerUiState
    data object Loading : LoadTimerUiState
    data class Error(val message: String) : LoadTimerUiState
    data class Success(val timerId: Int) : LoadTimerUiState
}
