package com.ibragimdekkushev.intervaltimer.presentation.workout.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.ibragimdekkushev.intervaltimer.MainActivity
import com.ibragimdekkushev.intervaltimer.R
import com.ibragimdekkushev.intervaltimer.presentation.workout.TimerStatus
import kotlin.math.ceil

internal object TimerNotification {

    const val CHANNEL_ID = "workout_timer"
    const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService<NotificationManager>() ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_workout),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_workout_desc)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun build(
        context: Context,
        title: String,
        status: TimerStatus,
        currentIntervalTitle: String?,
        remainingInIntervalMs: Long,
        totalRemainingMs: Long,
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val (contentText, subText) = when (status) {
            TimerStatus.Running -> {
                val interval = currentIntervalTitle.orEmpty()
                "$interval · ${formatMs(remainingInIntervalMs)}" to
                        context.getString(R.string.timer_total, formatMs(totalRemainingMs))
            }
            TimerStatus.Paused -> {
                context.getString(R.string.status_paused) to
                        context.getString(R.string.timer_total, formatMs(totalRemainingMs))
            }
            TimerStatus.Finished -> {
                context.getString(R.string.timer_status_finished) to null
            }
            TimerStatus.Idle -> {
                context.getString(R.string.timer_status_idle) to null
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(contentIntent)
            .setOngoing(status != TimerStatus.Finished)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)

        if (subText != null) builder.setSubText(subText)

        when (status) {
            TimerStatus.Running -> builder.addAction(
                NotificationCompat.Action(
                    0,
                    context.getString(R.string.btn_pause),
                    actionIntent(context, TimerService.ACTION_PAUSE),
                )
            )
            TimerStatus.Paused -> builder.addAction(
                NotificationCompat.Action(
                    0,
                    context.getString(R.string.btn_resume),
                    actionIntent(context, TimerService.ACTION_RESUME),
                )
            )
            else -> Unit
        }

        if (status == TimerStatus.Running || status == TimerStatus.Paused) {
            builder.addAction(
                NotificationCompat.Action(
                    0,
                    context.getString(R.string.btn_reset),
                    actionIntent(context, TimerService.ACTION_RESET),
                )
            )
        }

        return builder.build()
    }

    private fun actionIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, TimerService::class.java).apply { this.action = action }
        return PendingIntent.getService(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun formatMs(ms: Long): String {
        val totalSeconds = ceil(ms / 1000.0).toLong().coerceAtLeast(0L)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}
