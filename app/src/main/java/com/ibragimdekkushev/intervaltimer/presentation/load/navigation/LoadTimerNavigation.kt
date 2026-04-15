package com.ibragimdekkushev.intervaltimer.presentation.load.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ibragimdekkushev.intervaltimer.presentation.load.LoadTimerScreen
import kotlinx.serialization.Serializable

@Serializable
data object LoadTimerRoute

fun NavGraphBuilder.loadTimerScreen(onTimerLoaded: (timerId: Int) -> Unit) {
    composable<LoadTimerRoute> {
        LoadTimerScreen(onTimerLoaded = onTimerLoaded)
    }
}
