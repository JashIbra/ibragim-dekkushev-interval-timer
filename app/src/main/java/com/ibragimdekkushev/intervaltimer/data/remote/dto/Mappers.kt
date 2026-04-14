package com.ibragimdekkushev.intervaltimer.data.remote.dto

import com.ibragimdekkushev.intervaltimer.domain.model.Interval
import com.ibragimdekkushev.intervaltimer.domain.model.IntervalTimer

fun IntervalTimerResponse.toDomain(): IntervalTimer = timer.toDomain()

fun TimerDto.toDomain(): IntervalTimer = IntervalTimer(
    id = timerId,
    title = title,
    totalTime = totalTime,
    intervals = intervals.map { it.toDomain() },
)

fun IntervalDto.toDomain(): Interval = Interval(
    title = title,
    duration = time,
)
