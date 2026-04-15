package com.ibragimdekkushev.intervaltimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ibragimdekkushev.intervaltimer.navigation.AppNavHost
import com.ibragimdekkushev.intervaltimer.presentation.ui.theme.IntervalTimerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IntervalTimerTheme {
                AppNavHost()
            }
        }
    }
}
