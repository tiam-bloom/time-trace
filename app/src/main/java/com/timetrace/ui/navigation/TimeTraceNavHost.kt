package com.timetrace.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.timetrace.ui.screens.AppDetailScreen
import com.timetrace.ui.screens.AppListScreen
import com.timetrace.ui.screens.DashboardScreen
import com.timetrace.ui.screens.SettingsScreen
import com.timetrace.ui.screens.StatsScreen

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
        composable(Screen.Stats.route) {
            StatsScreen(
                onNavigateToAppDetail = { packageName, period, appName ->
                    navController.navigate(
                        Screen.AppDetail.createRoute(packageName, period, appName)
                    )
                }
            )
        }
        composable(Screen.AppList.route) {
            AppListScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
        composable(
            route = Screen.AppDetail.route,
            arguments = listOf(
                navArgument("packageName") { type = NavType.StringType },
                navArgument("period") { type = NavType.StringType },
                navArgument("appName") { type = NavType.StringType }
            )
        ) {
            AppDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
