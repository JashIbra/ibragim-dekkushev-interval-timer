package com.ibragimdekkushev.intervaltimer

import android.app.Application
import com.ibragimdekkushev.intervaltimer.presentation.workout.service.TimerNotification
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class IntervalTimerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TimerNotification.ensureChannel(this)
    }
}
