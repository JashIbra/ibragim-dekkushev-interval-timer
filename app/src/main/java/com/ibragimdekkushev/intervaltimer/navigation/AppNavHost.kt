package com.ibragimdekkushev.intervaltimer.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.ibragimdekkushev.intervaltimer.presentation.load.navigation.LoadTimerRoute
import com.ibragimdekkushev.intervaltimer.presentation.load.navigation.loadTimerScreen
import com.ibragimdekkushev.intervaltimer.presentation.workout.navigation.navigateToWorkout
import com.ibragimdekkushev.intervaltimer.presentation.workout.navigation.workoutScreen

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = LoadTimerRoute,
    ) {
        loadTimerScreen(
            onTimerLoaded = { timerId ->
                navController.navigateToWorkout(timerId)
            }
        )
        workoutScreen(
            onBack = navController::popBackStack
        )
    }
}
