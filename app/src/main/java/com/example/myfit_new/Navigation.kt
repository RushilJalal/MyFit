package com.example.myfit_new

import ProfileScreen
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable



@Composable
fun Navigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavigationItem.Steps.route
    ) {
        composable(NavigationItem.Steps.route) {
            StepsScreen()
        }
        composable(NavigationItem.Journal.route) {
            JournalScreen()
        }
        composable(NavigationItem.Profile.route) {
            ProfileScreen()
        }
    }
}
