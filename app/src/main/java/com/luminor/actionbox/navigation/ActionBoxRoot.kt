package com.luminor.actionbox.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.ui.agenda.AgendaScreen
import com.luminor.actionbox.ui.capture.CaptureScreen
import com.luminor.actionbox.ui.home.HomeScreen
import com.luminor.actionbox.ui.organize.OrganizeScreen
import com.luminor.actionbox.ui.saved.SavedScreen
import com.luminor.actionbox.ui.settings.SettingsScreen

private data class BottomDestination(val route: String, val label: String, val emoji: String)

@Composable
fun ActionBoxRoot(viewModel: ActionViewModel) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val bottom = listOf(
        BottomDestination("today", "Hoje", "⌂"),
        BottomDestination("agenda", "Agenda", "▦"),
        BottomDestination("capture", "Criar", "+"),
        BottomDestination("organize", "Organizar", "▣"),
        BottomDestination("saved", "Depois", "🔖")
    )

    LaunchedEffect(Unit) { viewModel.message.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(Unit) {
        viewModel.navigateHome.collect {
            navController.navigate("today") {
                popUpTo("today") { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottom = currentRoute != "settings"

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBottom) {
                NavigationBar {
                    bottom.forEach { item ->
                        val selected = backStack?.destination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo("today") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(item.emoji) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(navController = navController, startDestination = "today", modifier = Modifier.padding(padding)) {
            composable("today") { HomeScreen(viewModel, onSettings = { navController.navigate("settings") }) }
            composable("agenda") { AgendaScreen(viewModel) }
            composable("capture") { CaptureScreen(viewModel) }
            composable("organize") { OrganizeScreen(viewModel) }
            composable("saved") { SavedScreen(viewModel) }
            composable("settings") { SettingsScreen(viewModel, onBack = { navController.popBackStack() }) }
        }
    }
}
