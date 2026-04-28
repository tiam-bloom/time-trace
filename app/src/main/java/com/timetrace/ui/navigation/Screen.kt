package com.timetrace.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Stats : Screen("stats")
    data object AppList : Screen("app_list")
    data object Settings : Screen("settings")
}
