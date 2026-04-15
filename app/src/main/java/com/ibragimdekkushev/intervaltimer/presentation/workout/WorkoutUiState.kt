package com.ibragimdekkushev.intervaltimer.presentation.workout

import com.ibragimdekkushev.intervaltimer.domain.model.IntervalTimer

sealed interface WorkoutUiState {
    data object Loading : WorkoutUiState
    data class Error(val message: String) : WorkoutUiState
    data class Ready(
        val timer: IntervalTimer,
        val status: TimerStatus,
        val currentIntervalIndex: Int,
        val remainingInCurrentIntervalMs: Long,
        val totalRemainingMs: Long,
    ) : WorkoutUiState
}

enum class TimerStatus { Idle, Running, Paused, Finished }

enum class SoundEvent { Start, IntervalTransition, Finish }
