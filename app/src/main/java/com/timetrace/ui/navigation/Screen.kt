package com.timetrace.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Stats : Screen("stats")
    data object AppList : Screen("app_list")
    data object Settings : Screen("settings")
    data object AppDetail : Screen("app_detail/{packageName}/{period}/{appName}") {
        fun createRoute(packageName: String, period: String, appName: String): String {
            return "app_detail/${Uri.encode(packageName)}/${Uri.encode(period)}/${Uri.encode(appName)}"
        }
    }
}
