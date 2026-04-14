package com.ibragimdekkushev.intervaltimer.data.remote

import com.ibragimdekkushev.intervaltimer.data.remote.dto.IntervalTimerResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface IntervalTimerApiService {

    @GET("api/interval-timers/{id}")
    suspend fun getIntervalTimer(@Path("id") id: Int): IntervalTimerResponse
}
