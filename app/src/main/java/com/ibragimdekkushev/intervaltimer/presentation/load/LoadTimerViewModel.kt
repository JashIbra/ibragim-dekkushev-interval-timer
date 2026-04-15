package com.ibragimdekkushev.intervaltimer.presentation.load

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibragimdekkushev.intervaltimer.R
import com.ibragimdekkushev.intervaltimer.domain.repository.IntervalTimerRepository
import com.ibragimdekkushev.intervaltimer.presentation.ui.extention.toReadableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoadTimerViewModel @Inject constructor(
    private val repository: IntervalTimerRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoadTimerUiState>(LoadTimerUiState.Idle)
    val uiState: StateFlow<LoadTimerUiState> = _uiState.asStateFlow()

    fun loadTimer(id: String) {
        val timerId = id.trim().toIntOrNull() ?: run {
            _uiState.value = LoadTimerUiState.Error(context.getString(R.string.error_invalid_id))
            return
        }
        viewModelScope.launch {
            _uiState.value = LoadTimerUiState.Loading
            repository.getIntervalTimer(timerId)
                .onSuccess { _uiState.value = LoadTimerUiState.Success(timerId) }
                .onFailure { _uiState.value = LoadTimerUiState.Error(it.toReadableMessage(context)) }
        }
    }

    fun resetToIdle() {
        _uiState.value = LoadTimerUiState.Idle
    }
}
