package com.ibragimdekkushev.intervaltimer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IntervalTimerResponse(
    val timer: TimerDto,
)

@Serializable
data class TimerDto(
    @SerialName("timer_id") val timerId: Int,
    val title: String,
    @SerialName("total_time") val totalTime: Int,
    val intervals: List<IntervalDto>,
)

@Serializable
data class IntervalDto(
    val title: String,
    val time: Int,
)
