package com.ibragimdekkushev.intervaltimer.domain.repository

import com.ibragimdekkushev.intervaltimer.domain.model.IntervalTimer

interface IntervalTimerRepository {
    suspend fun getIntervalTimer(id: Int): Result<IntervalTimer>
}
