package com.ibragimdekkushev.intervaltimer.data.repository

import com.ibragimdekkushev.intervaltimer.data.remote.IntervalTimerApiService
import com.ibragimdekkushev.intervaltimer.data.remote.dto.toDomain
import com.ibragimdekkushev.intervaltimer.domain.model.IntervalTimer
import com.ibragimdekkushev.intervaltimer.domain.repository.IntervalTimerRepository
import javax.inject.Inject

class IntervalTimerRepositoryImpl @Inject constructor(
    private val apiService: IntervalTimerApiService,
) : IntervalTimerRepository {

    override suspend fun getIntervalTimer(id: Int): Result<IntervalTimer> = runCatching {
        apiService.getIntervalTimer(id).toDomain()
    }
}
