package com.handnote.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.handnote.app.ui.navigation.Screen
import com.handnote.app.ui.screens.*

import androidx.lifecycle.viewmodel.compose.viewModel
import com.handnote.app.ui.viewmodel.MainViewModel
import com.handnote.app.ui.viewmodel.ViewModelFactory
import com.handnote.app.util.FileLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModelFactory: ViewModelFactory) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // 创建共享的 ViewModel
    val viewModel: MainViewModel = viewModel(factory = viewModelFactory)
    
    // 调试日志（安全调用，避免初始化问题）
    LaunchedEffect(Unit) {
        try {
            FileLogger.d("MainScreen", "MainScreen composable called")
            FileLogger.d("MainScreen", "ViewModel created successfully")
        } catch (e: Exception) {
            // 静默失败，不影响 UI
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val screens = listOf(
                    Screen.Calendar to Icons.Filled.DateRange,
                    Screen.Feed to Icons.Filled.List,
                    Screen.Config to Icons.Filled.Settings,
                    Screen.Settings to Icons.Filled.MoreVert
                )

                screens.forEach { (screen, icon) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Calendar.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Calendar.route) {
                CalendarScreen(viewModel = viewModel)
            }
            composable(Screen.Feed.route) {
                FeedScreen(viewModel = viewModel)
            }
            composable(Screen.Config.route) {
                ConfigScreen(viewModel = viewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
