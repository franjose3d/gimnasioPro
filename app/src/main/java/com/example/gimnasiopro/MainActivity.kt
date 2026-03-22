package com.example.gimnasiopro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.gimnasiopro.presentation.navigation.AppNavGraph
import com.example.gimnasiopro.presentation.navigation.Screen
import com.example.gimnasiopro.ui.theme.GimnasioProTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GimnasioProTheme {
                val navController = rememberNavController()
                AppNavGraph(
                    navController    = navController,
                    startDestination = Screen.Main.route
                )
            }
        }
    }
}
