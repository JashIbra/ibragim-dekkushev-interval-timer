package com.ibragimdekkushev.intervaltimer.presentation.workout.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ibragimdekkushev.intervaltimer.domain.model.IntervalTimer
import com.ibragimdekkushev.intervaltimer.domain.repository.IntervalTimerRepository
import com.ibragimdekkushev.intervaltimer.presentation.workout.TimerStatus
import com.ibragimdekkushev.intervaltimer.R
import com.ibragimdekkushev.intervaltimer.presentation.workout.sound.SoundPlayer
import com.ibragimdekkushev.intervaltimer.presentation.workout.sound.VoiceAnnouncer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimerRuntimeState(
    val timer: IntervalTimer,
    val status: TimerStatus,
    val currentIntervalIndex: Int,
    val remainingInCurrentIntervalMs: Long,
    val totalRemainingMs: Long,
)

@AndroidEntryPoint
class TimerService : Service() {

    @Inject
    lateinit var repository: IntervalTimerRepository

    @Inject
    lateinit var soundPlayer: SoundPlayer

    @Inject
    lateinit var voiceAnnouncer: VoiceAnnouncer

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null

    private val _state = MutableStateFlow<TimerRuntimeState?>(null)

    private var startedAtMs: Long = 0L
    private var accumulatedPauseMs: Long = 0L
    private var pausedAtMs: Long = 0L
    private var isForeground = false

    inner class LocalBinder : Binder() {
        val state: StateFlow<TimerRuntimeState?> get() = _state.asStateFlow()
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val timerId = intent.getIntExtra(EXTRA_TIMER_ID, -1)
                if (timerId >= 0) handleStart(timerId)
            }

            ACTION_PAUSE -> handlePause()
            ACTION_RESUME -> handleResume()
            ACTION_RESET -> handleReset()
            ACTION_STOP -> stopEverything()
        }
        return START_NOT_STICKY
    }

    private fun handleStart(timerId: Int) {
        val loaded = _state.value?.timer
        if (loaded != null && loaded.id == timerId) {
            restartCurrent(loaded)
        } else {
            scope.launch {
                repository.getIntervalTimer(timerId)
                    .onSuccess { timer -> restartCurrent(timer) }
                    .onFailure { stopEverything() }
            }
        }
    }

    private fun restartCurrent(timer: IntervalTimer) {
        startedAtMs = SystemClock.elapsedRealtime()
        accumulatedPauseMs = 0L
        pausedAtMs = 0L
        _state.value = TimerRuntimeState(
            timer = timer,
            status = TimerStatus.Running,
            currentIntervalIndex = 0,
            remainingInCurrentIntervalMs = timer.intervals.first().duration * 1000L,
            totalRemainingMs = timer.totalTime * 1000L,
        )
        promoteToForeground()
        soundPlayer.playStart()
        voiceAnnouncer.announce(timer.intervals.first().title)
        launchTickLoop()
    }

    private fun handlePause() {
        val current = _state.value ?: return
        if (current.status != TimerStatus.Running) return
        pausedAtMs = SystemClock.elapsedRealtime()
        _state.value = current.copy(status = TimerStatus.Paused)
        updateNotification()
    }

    private fun handleResume() {
        val current = _state.value ?: return
        if (current.status != TimerStatus.Paused) return
        accumulatedPauseMs += SystemClock.elapsedRealtime() - pausedAtMs
        _state.value = current.copy(status = TimerStatus.Running)
        updateNotification()
    }

    private fun handleReset() {
        tickJob?.cancel()
        startedAtMs = 0L
        accumulatedPauseMs = 0L
        pausedAtMs = 0L
        val timer = _state.value?.timer
        if (timer != null) {
            _state.value = TimerRuntimeState(
                timer = timer,
                status = TimerStatus.Idle,
                currentIntervalIndex = 0,
                remainingInCurrentIntervalMs = timer.intervals.first().duration * 1000L,
                totalRemainingMs = timer.totalTime * 1000L,
            )
        }
        dropForeground()
    }

    private fun stopEverything() {
        tickJob?.cancel()
        _state.value = null
        dropForeground()
        stopSelf()
    }

    private fun launchTickLoop() {
        tickJob?.cancel()
        tickJob = scope.launch {
            var notificationTick = 0
            while (isActive) {
                delay(100L)
                val current = _state.value ?: break
                if (current.status != TimerStatus.Running) continue
                recalculate(current)
                if (++notificationTick >= 10) {
                    notificationTick = 0
                    updateNotification()
                }
            }
        }
    }

    private fun recalculate(current: TimerRuntimeState) {
        val t = current.timer
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
                    soundPlayer.playIntervalTransition()
                    voiceAnnouncer.announce(interval.title)
                }
                _state.value = current.copy(
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
        tickJob?.cancel()
        soundPlayer.playFinish()
        voiceAnnouncer.announce(getString(R.string.voice_finished))
        _state.value = TimerRuntimeState(
            timer = t,
            status = TimerStatus.Finished,
            currentIntervalIndex = t.intervals.lastIndex,
            remainingInCurrentIntervalMs = 0L,
            totalRemainingMs = 0L,
        )
        updateNotification()
        stopForeground(STOP_FOREGROUND_DETACH)
        isForeground = false
    }

    private fun promoteToForeground() {
        val notification = buildNotification() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                TimerNotification.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(TimerNotification.NOTIFICATION_ID, notification)
        }
        isForeground = true
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun updateNotification() {
        if (!isForeground) return
        if (!hasNotificationPermission()) return
        val notification = buildNotification() ?: return
        NotificationManagerCompat.from(this).notify(
            TimerNotification.NOTIFICATION_ID,
            notification,
        )
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun buildNotification() = _state.value?.let { s ->
        TimerNotification.build(
            context = this,
            title = s.timer.title,
            status = s.status,
            currentIntervalTitle = s.timer.intervals.getOrNull(s.currentIntervalIndex)?.title,
            remainingInIntervalMs = s.remainingInCurrentIntervalMs,
            totalRemainingMs = s.totalRemainingMs,
        )
    }

    private fun dropForeground() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        isForeground = false
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopEverything()
    }

    override fun onDestroy() {
        tickJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
        voiceAnnouncer.stop()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "intervaltimer.action.START"
        const val ACTION_PAUSE = "intervaltimer.action.PAUSE"
        const val ACTION_RESUME = "intervaltimer.action.RESUME"
        const val ACTION_RESET = "intervaltimer.action.RESET"
        const val ACTION_STOP = "intervaltimer.action.STOP"
        const val EXTRA_TIMER_ID = "timer_id"
    }
}
