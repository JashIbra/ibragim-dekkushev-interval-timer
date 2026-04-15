package com.ibragimdekkushev.intervaltimer.presentation.workout

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibragimdekkushev.intervaltimer.R
import com.ibragimdekkushev.intervaltimer.domain.model.IntervalTimer
import com.ibragimdekkushev.intervaltimer.domain.repository.IntervalTimerRepository
import com.ibragimdekkushev.intervaltimer.presentation.workout.service.TimerRuntimeState
import com.ibragimdekkushev.intervaltimer.presentation.workout.service.TimerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val repository: IntervalTimerRepository,
    savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val timerId: Int = checkNotNull(savedStateHandle[ARG_TIMER_ID])

    private var timer: IntervalTimer? = null
    private var binder: TimerService.LocalBinder? = null
    private var observeJob: Job? = null

    private val _uiState = MutableStateFlow<WorkoutUiState>(WorkoutUiState.Loading)
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val local = service as? TimerService.LocalBinder ?: return
            binder = local
            observeJob?.cancel()
            observeJob = local.state
                .onEach { runtime -> onRuntimeUpdate(runtime) }
                .launchIn(viewModelScope)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
            observeJob?.cancel()
        }
    }

    init {
        loadTimer()
        bindService()
    }

    fun start() = sendAction(TimerService.ACTION_START, withTimerId = true, asForeground = true)

    fun pause() = sendAction(TimerService.ACTION_PAUSE)

    fun resume() = sendAction(TimerService.ACTION_RESUME)

    fun reset() = sendAction(TimerService.ACTION_RESET)

    private fun loadTimer() {
        viewModelScope.launch {
            repository.getIntervalTimer(timerId)
                .onSuccess { loaded ->
                    timer = loaded
                    if (_uiState.value is WorkoutUiState.Loading) {
                        _uiState.value = idleState(loaded)
                    }
                }
                .onFailure {
                    _uiState.value = WorkoutUiState.Error(
                        context.getString(R.string.error_load_failed)
                    )
                }
        }
    }

    private fun bindService() {
        val intent = Intent(context, TimerService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun onRuntimeUpdate(runtime: TimerRuntimeState?) {
        if (runtime == null) {
            val t = timer ?: return
            _uiState.value = idleState(t)
            return
        }
        if (runtime.timer.id == timerId && timer == null) {
            timer = runtime.timer
        }
        _uiState.value = WorkoutUiState.Ready(
            timer = runtime.timer,
            status = runtime.status,
            currentIntervalIndex = runtime.currentIntervalIndex,
            remainingInCurrentIntervalMs = runtime.remainingInCurrentIntervalMs,
            totalRemainingMs = runtime.totalRemainingMs,
        )
    }

    private fun idleState(t: IntervalTimer) = WorkoutUiState.Ready(
        timer = t,
        status = TimerStatus.Idle,
        currentIntervalIndex = 0,
        remainingInCurrentIntervalMs = t.intervals.first().duration * 1000L,
        totalRemainingMs = t.totalTime * 1000L,
    )

    private fun sendAction(
        action: String,
        withTimerId: Boolean = false,
        asForeground: Boolean = false,
    ) {
        val intent = Intent(context, TimerService::class.java).apply {
            this.action = action
            if (withTimerId) putExtra(TimerService.EXTRA_TIMER_ID, timerId)
        }
        if (asForeground) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
    }

    override fun onCleared() {
        observeJob?.cancel()
        sendAction(TimerService.ACTION_STOP)
        runCatching { context.unbindService(connection) }
        super.onCleared()
    }

    companion object {
        const val ARG_TIMER_ID = "timerId"
    }
}
