package com.ibragimdekkushev.intervaltimer.presentation.workout

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibragimdekkushev.intervaltimer.R
import com.ibragimdekkushev.intervaltimer.domain.model.IntervalTimer
import com.ibragimdekkushev.intervaltimer.domain.repository.IntervalTimerRepository
import com.ibragimdekkushev.intervaltimer.presentation.workout.sound.SoundPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val repository: IntervalTimerRepository,
    private val soundPlayer: SoundPlayer,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val timerId: Int = checkNotNull(savedStateHandle[ARG_TIMER_ID])

    private var timer: IntervalTimer? = null

    private var startedAtMs: Long
        get() = savedStateHandle[KEY_STARTED_AT] ?: 0L
        set(value) { savedStateHandle[KEY_STARTED_AT] = value }

    private var accumulatedPauseMs: Long
        get() = savedStateHandle[KEY_ACCUMULATED_PAUSE] ?: 0L
        set(value) { savedStateHandle[KEY_ACCUMULATED_PAUSE] = value }

    private var pausedAtMs: Long
        get() = savedStateHandle[KEY_PAUSED_AT] ?: 0L
        set(value) { savedStateHandle[KEY_PAUSED_AT] = value }

    private val _uiState = MutableStateFlow<WorkoutUiState>(WorkoutUiState.Loading)
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    private val _soundEvent = MutableSharedFlow<SoundEvent>(extraBufferCapacity = 3)

    init {
        loadTimer()
        launchTickLoop()
        _soundEvent
            .onEach { event ->
                when (event) {
                    SoundEvent.Start -> soundPlayer.playStart()
                    SoundEvent.IntervalTransition -> soundPlayer.playIntervalTransition()
                    SoundEvent.Finish -> soundPlayer.playFinish()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadTimer() {
        viewModelScope.launch {
            repository.getIntervalTimer(timerId)
                .onSuccess { loaded ->
                    timer = loaded
                    val restoredStatus = savedStateHandle.get<String>(KEY_STATUS)
                        ?.let { runCatching { TimerStatus.valueOf(it) }.getOrNull() }
                        ?: TimerStatus.Idle
                    applyStatus(loaded, restoredStatus)
                }
                .onFailure {
                    _uiState.value = WorkoutUiState.Error(context.getString(R.string.error_load_failed))
                }
        }
    }

    fun start() {
        val t = timer ?: return
        startedAtMs = SystemClock.elapsedRealtime()
        accumulatedPauseMs = 0L
        saveStatus(TimerStatus.Running)
        _soundEvent.tryEmit(SoundEvent.Start)
        applyStatus(t, TimerStatus.Running)
    }

    fun pause() {
        val t = timer ?: return
        pausedAtMs = SystemClock.elapsedRealtime()
        saveStatus(TimerStatus.Paused)
        applyStatus(t, TimerStatus.Paused)
    }

    fun resume() {
        val t = timer ?: return
        accumulatedPauseMs += SystemClock.elapsedRealtime() - pausedAtMs
        saveStatus(TimerStatus.Running)
        applyStatus(t, TimerStatus.Running)
    }

    fun reset() {
        val t = timer ?: return
        startedAtMs = 0L
        accumulatedPauseMs = 0L
        pausedAtMs = 0L
        saveStatus(TimerStatus.Idle)
        applyStatus(t, TimerStatus.Idle)
    }

    private fun launchTickLoop() {
        viewModelScope.launch {
            while (true) {
                delay(100L)
                val state = _uiState.value
                if (state is WorkoutUiState.Ready && state.status == TimerStatus.Running) {
                    recalculate(state)
                }
            }
        }
    }

    private fun recalculate(current: WorkoutUiState.Ready) {
        val t = timer ?: return
        val elapsedMs = SystemClock.elapsedRealtime() - startedAtMs - accumulatedPauseMs
        val totalMs = t.totalTime * 1000L

        if (elapsedMs >= totalMs) {
            finish(t)
            return
        }

        var remaining = elapsedMs
        for ((index, interval) in t.intervals.withIndex()) {
            val durationMs = interval.duration * 1000L
            if (remaining < durationMs) {
                if (index != current.currentIntervalIndex) {
                    _soundEvent.tryEmit(SoundEvent.IntervalTransition)
                }
                _uiState.value = current.copy(
                    currentIntervalIndex = index,
                    remainingInCurrentIntervalMs = durationMs - remaining,
                    totalRemainingMs = totalMs - elapsedMs,
                )
                return
            }
            remaining -= durationMs
        }
    }

    private fun finish(t: IntervalTimer) {
        saveStatus(TimerStatus.Finished)
        _soundEvent.tryEmit(SoundEvent.Finish)
        _uiState.value = WorkoutUiState.Ready(
            timer = t,
            status = TimerStatus.Finished,
            currentIntervalIndex = t.intervals.lastIndex,
            remainingInCurrentIntervalMs = 0L,
            totalRemainingMs = 0L,
        )
    }

    private fun applyStatus(t: IntervalTimer, status: TimerStatus) {
        when (status) {
            TimerStatus.Idle -> _uiState.value = WorkoutUiState.Ready(
                timer = t,
                status = TimerStatus.Idle,
                currentIntervalIndex = 0,
                remainingInCurrentIntervalMs = t.intervals.first().duration * 1000L,
                totalRemainingMs = t.totalTime * 1000L,
            )
            TimerStatus.Running -> {
                val running = WorkoutUiState.Ready(
                    timer = t,
                    status = TimerStatus.Running,
                    currentIntervalIndex = (_uiState.value as? WorkoutUiState.Ready)?.currentIntervalIndex ?: 0,
                    remainingInCurrentIntervalMs = t.intervals.first().duration * 1000L,
                    totalRemainingMs = t.totalTime * 1000L,
                )
                _uiState.value = running
                recalculate(running)
            }
            TimerStatus.Paused -> {
                val elapsedMs = pausedAtMs - startedAtMs - accumulatedPauseMs
                val totalMs = t.totalTime * 1000L
                var remaining = elapsedMs
                for ((index, interval) in t.intervals.withIndex()) {
                    val durationMs = interval.duration * 1000L
                    if (remaining < durationMs) {
                        _uiState.value = WorkoutUiState.Ready(
                            timer = t,
                            status = TimerStatus.Paused,
                            currentIntervalIndex = index,
                            remainingInCurrentIntervalMs = durationMs - remaining,
                            totalRemainingMs = totalMs - elapsedMs,
                        )
                        return
                    }
                    remaining -= durationMs
                }
            }
            TimerStatus.Finished -> finish(t)
        }
    }

    private fun saveStatus(status: TimerStatus) {
        savedStateHandle[KEY_STATUS] = status.name
    }

    companion object {
        const val ARG_TIMER_ID = "timerId"
        private const val KEY_STARTED_AT = "started_at"
        private const val KEY_ACCUMULATED_PAUSE = "accumulated_pause"
        private const val KEY_PAUSED_AT = "paused_at"
        private const val KEY_STATUS = "timer_status"
    }
}
