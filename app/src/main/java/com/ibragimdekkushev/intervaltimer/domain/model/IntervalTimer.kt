package com.ibragimdekkushev.intervaltimer.domain.model

data class IntervalTimer(
    val id: Int,
    val title: String,
    val totalTime: Int,
    val intervals: List<Interval>,
)

data class Interval(
    val title: String,
    val duration: Int,
)
