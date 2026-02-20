package com.handnote.app.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Calendar : Screen("calendar", "日程")
    object Feed : Screen("feed", "沉淀")
    object Config : Screen("config", "配置")
    object Settings : Screen("settings", "设置")
}

