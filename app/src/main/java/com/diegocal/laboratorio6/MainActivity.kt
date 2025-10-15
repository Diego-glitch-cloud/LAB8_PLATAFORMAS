package com.diegocal.laboratorio6

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.diegocal.laboratorio6.ui.theme.Laboratorio6Theme
import com.diegocal.laboratorio6.ui.theme.ThemeState
import com.diegocal.laboratorio6.ui.theme.rememberThemeState
import com.diegocal.laboratorio6.ui.views.DetailsScreen
import com.diegocal.laboratorio6.ui.views.PexelsScreen
import com.diegocal.laboratorio6.ui.views.ProfileScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeState = rememberThemeState()
            Laboratorio6Theme(isDarkTheme = themeState.isDark.value) {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        PexelsScreen(
                            navController = navController,
                            themeState = themeState
                        )
                    }
                    composable(
                        "details/{photoId}",
                        arguments = listOf(navArgument("photoId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        DetailsScreen(
                            navController = navController,
                            photoId = backStackEntry.arguments?.getInt("photoId") ?: 0
                        )
                    }
                    composable("profile") {
                        ProfileScreen(
                            navController = navController,
                            themeState = themeState
                        )
                    }
                }
            }
        }
    }
}