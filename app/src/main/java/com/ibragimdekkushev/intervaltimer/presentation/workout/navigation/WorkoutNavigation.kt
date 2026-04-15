package com.ibragimdekkushev.intervaltimer.presentation.workout.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ibragimdekkushev.intervaltimer.presentation.workout.WorkoutScreen
import kotlinx.serialization.Serializable

@Serializable
data class WorkoutRoute(val timerId: Int)

fun NavController.navigateToWorkout(timerId: Int) = navigate(WorkoutRoute(timerId))

fun NavGraphBuilder.workoutScreen(onBack: () -> Unit) {
    composable<WorkoutRoute> {
        WorkoutScreen(onBack = onBack)
    }
}
