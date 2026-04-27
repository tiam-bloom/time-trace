package com.timetrace.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.timetrace.ui.screens.AppListScreen
import com.timetrace.ui.screens.DashboardScreen
import com.timetrace.ui.screens.SettingsScreen

@Composable
fun TimeTraceNavHost(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen()
        }
        composable(Screen.AppList.route) {
            AppListScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
