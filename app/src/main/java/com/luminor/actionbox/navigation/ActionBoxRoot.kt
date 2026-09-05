package com.luminor.actionbox.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.ui.actions.ActionEditorScreen
import com.luminor.actionbox.ui.agenda.AgendaScreen
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.home.HomeScreen
import com.luminor.actionbox.ui.motion.LocalNavAnimatedVisibilityScope
import com.luminor.actionbox.ui.motion.LocalSharedTransitionScope
import com.luminor.actionbox.ui.motion.MotionDuration
import com.luminor.actionbox.ui.motion.pressScale
import com.luminor.actionbox.ui.organize.OrganizeScreen
import com.luminor.actionbox.ui.organize.ProjectDetailScreen
import com.luminor.actionbox.ui.organize.notes.NoteDetailScreen
import com.luminor.actionbox.ui.saved.SavedDetailScreen
import com.luminor.actionbox.ui.saved.SavedScreen
import com.luminor.actionbox.ui.settings.SettingsScreen

private data class BottomDestination(val route: String, val label: String, val icon: ImageVector)

private val bottomDestinations = listOf(
    BottomDestination("today", "Hoje", ActionBoxIcons.Home),
    BottomDestination("agenda", "Agenda", ActionBoxIcons.Agenda),
    BottomDestination("organize", "Organizar", ActionBoxIcons.Organize),
    BottomDestination("saved", "Depois", ActionBoxIcons.Saved)
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ActionBoxRoot(viewModel: ActionViewModel) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val rootRoutes = bottomDestinations.map { it.route }.toSet()

    LaunchedEffect(Unit) { viewModel.message.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(Unit) {
        viewModel.navigateHome.collect {
            navController.navigate("today") {
                popUpTo("today") { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    SharedTransitionLayout {
        val sharedScope = this
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (currentRoute in rootRoutes) {
                    ActionBottomNavigation(
                        selectedRoute = currentRoute,
                        onSelect = { route ->
                            navController.navigate(route) {
                                popUpTo("today") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "today",
                modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
                enterTransition = {
                    fadeIn(tween(MotionDuration.Standard)) + slideInHorizontally(tween(MotionDuration.Standard)) { it / 12 }
                },
                exitTransition = {
                    fadeOut(tween(MotionDuration.Fast)) + slideOutHorizontally(tween(MotionDuration.Fast)) { -it / 18 }
                },
                popEnterTransition = {
                    fadeIn(tween(MotionDuration.Standard)) + slideInHorizontally(tween(MotionDuration.Standard)) { -it / 12 }
                },
                popExitTransition = {
                    fadeOut(tween(MotionDuration.Fast)) + slideOutHorizontally(tween(MotionDuration.Fast)) { it / 18 }
                }
            ) {
                composable("today") {
                    SharedDestination(sharedScope, this) {
                        HomeScreen(
                            viewModel = viewModel,
                            onSettings = { navController.navigate("settings") },
                            onActionOpen = { navController.navigate("action/$it") }
                        )
                    }
                }
                composable("agenda") {
                    SharedDestination(sharedScope, this) {
                        AgendaScreen(viewModel, onActionOpen = { navController.navigate("action/$it") })
                    }
                }
                composable("organize") {
                    SharedDestination(sharedScope, this) {
                        OrganizeScreen(
                            viewModel = viewModel,
                            onProjectOpen = { navController.navigate("project/$it") },
                            onNoteOpen = { navController.navigate("note/$it") }
                        )
                    }
                }
                composable("saved") {
                    SharedDestination(sharedScope, this) {
                        SavedScreen(viewModel, onOpenDetail = { navController.navigate("saved/$it") })
                    }
                }
                composable("settings") { SharedDestination(sharedScope, this) { SettingsScreen(viewModel, onBack = { navController.popBackStack() }) } }
                composable("action/{id}") { entry ->
                    SharedDestination(sharedScope, this) {
                        val id = entry.arguments?.getString("id")?.toLongOrNull() ?: return@SharedDestination
                        val all by viewModel.all.collectAsStateWithLifecycle()
                        val action = all.firstOrNull { it.id == id }
                        if (action != null) ActionEditorScreen(viewModel, action, onBack = { navController.popBackStack() })
                    }
                }
                composable("note/{id}") { entry ->
                    SharedDestination(sharedScope, this) {
                        val id = entry.arguments?.getString("id")?.toLongOrNull() ?: return@SharedDestination
                        val notes by viewModel.notes.collectAsStateWithLifecycle()
                        val note = notes.firstOrNull { it.id == id }
                        if (note != null) NoteDetailScreen(viewModel, note, onBack = { navController.popBackStack() })
                    }
                }
                composable("project/{id}") { entry ->
                    SharedDestination(sharedScope, this) {
                        val id = entry.arguments?.getString("id")?.toLongOrNull() ?: return@SharedDestination
                        ProjectDetailScreen(viewModel, id, onBack = { navController.popBackStack() })
                    }
                }
                composable("saved/{id}") { entry ->
                    SharedDestination(sharedScope, this) {
                        val id = entry.arguments?.getString("id")?.toLongOrNull() ?: return@SharedDestination
                        SavedDetailScreen(viewModel, id, onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedDestination(
    sharedScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalSharedTransitionScope provides sharedScope,
        LocalNavAnimatedVisibilityScope provides animatedVisibilityScope,
        content = content
    )
}

@Composable
private fun ActionBottomNavigation(selectedRoute: String?, onSelect: (String) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomDestinations.forEach { item ->
                val selected = selectedRoute == item.route
                val tint by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "bottom-tint"
                )
                val scale by animateFloatAsState(if (selected) 1.06f else 1f, label = "bottom-scale")
                val indicatorScale by animateFloatAsState(if (selected) 1f else 0f, label = "bottom-indicator")
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .pressScale(0.97f)
                        .clickable {
                            if (!selected) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelect(item.route)
                            }
                        }
                        .semantics { this.selected = selected; contentDescription = item.label }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(23.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                    )
                    Text(item.label, style = MaterialTheme.typography.labelMedium, color = tint)
                    Surface(
                        modifier = Modifier.size(width = 22.dp, height = 3.dp).graphicsLayer { scaleX = indicatorScale },
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.primary
                    ) {}
                }
            }
        }
    }
}
