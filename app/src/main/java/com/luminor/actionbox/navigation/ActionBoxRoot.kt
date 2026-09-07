package com.luminor.actionbox.navigation

import com.luminor.actionbox.R
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
import androidx.compose.material3.SnackbarResult
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
import com.luminor.actionbox.ui.RootViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.luminor.actionbox.ui.DetailContent
import com.luminor.actionbox.ui.actions.ActionEditorScreen
import com.luminor.actionbox.ui.agenda.AgendaScreen
import com.luminor.actionbox.ui.capture.CaptureViewModel
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.events.AppUiEvent
import com.luminor.actionbox.ui.home.HomeScreen
import com.luminor.actionbox.ui.motion.LocalNavAnimatedVisibilityScope
import com.luminor.actionbox.ui.motion.LocalSharedTransitionScope
import com.luminor.actionbox.ui.motion.MotionDuration
import com.luminor.actionbox.ui.motion.pressScale
import com.luminor.actionbox.ui.organize.OrganizeScreen
import com.luminor.actionbox.ui.organize.ProjectDetailScreen
import com.luminor.actionbox.ui.organize.lists.ListDetailScreen
import com.luminor.actionbox.ui.organize.notes.NoteDetailScreen
import com.luminor.actionbox.ui.organize.routines.RoutineDetailScreen
import com.luminor.actionbox.ui.saved.SavedDetailScreen
import com.luminor.actionbox.ui.saved.SavedScreen
import com.luminor.actionbox.ui.search.GlobalSearchScreen
import com.luminor.actionbox.ui.search.GlobalSearchViewModel
import com.luminor.actionbox.ui.settings.SettingsScreen
import com.luminor.actionbox.ui.trash.TrashScreen
import com.luminor.actionbox.ui.trash.TrashViewModel

private data class BottomDestination(val route: String, val label: Int, val icon: ImageVector)

private val bottomDestinations = listOf(
    BottomDestination("today", R.string.text_hoje, ActionBoxIcons.Home),
    BottomDestination("agenda", R.string.text_agenda, ActionBoxIcons.Agenda),
    BottomDestination("organize", R.string.text_organizar, ActionBoxIcons.Organize),
    BottomDestination("saved", R.string.text_depois, ActionBoxIcons.Saved)
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ActionBoxRoot(viewModel: RootViewModel, captureViewModel: CaptureViewModel) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    val context = androidx.compose.ui.platform.LocalContext.current
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val rootRoutes = bottomDestinations.map { it.route }.toSet()

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is AppUiEvent.Message -> snackbar.showSnackbar(event.text.resolve(context))
                is AppUiEvent.Undo -> {
                    val result = snackbar.showSnackbar(
                        message = event.text.resolve(context),
                        actionLabel = "DESFAZER",
                        withDismissAction = true
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undo(event)
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        captureViewModel.navigateHome.collect {
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
                            viewModel = hiltViewModel(),
                            captureViewModel = captureViewModel,
                            onHistory = { navController.navigate("history") },
                            onSettings = { navController.navigate("settings") },
                            onActionOpen = { navController.navigate("action/$it") },
                            onSearch = { navController.navigate("search") }
                        )
                    }
                }
                composable("agenda") {
                    SharedDestination(sharedScope, this) {
                        AgendaScreen(hiltViewModel(), onActionOpen = { navController.navigate("action/$it") })
                    }
                }
                composable("organize") {
                    SharedDestination(sharedScope, this) {
                        OrganizeScreen(
                            organizeViewModel = hiltViewModel(),
                            notesViewModel = hiltViewModel(),
                            onProjectOpen = { navController.navigate("project/$it") },
                            onListOpen = { navController.navigate("list/$it") },
                            onRoutineOpen = { navController.navigate("routine/$it") },
                            onNoteOpen = { navController.navigate("note/$it") },
                            onSearch = { navController.navigate("search") }
                        )
                    }
                }
                composable("saved") {
                    SharedDestination(sharedScope, this) {
                        SavedScreen(hiltViewModel(), onOpenDetail = { navController.navigate("saved/$it") })
                    }
                }
                composable("settings") {
                    SharedDestination(sharedScope, this) {
                        SettingsScreen(
                            viewModel = hiltViewModel(),
                            onBack = { navController.popBackStack() },
                            onTrash = { navController.navigate("trash") }
                        )
                    }
                }
                composable("search") {
                    SharedDestination(sharedScope, this) {
                        val searchViewModel = hiltViewModel<GlobalSearchViewModel>()
                        GlobalSearchScreen(
                            viewModel = searchViewModel,
                            onBack = { navController.popBackStack() },
                            onOpenAction = { navController.navigate("action/$it") },
                            onOpenProject = { navController.navigate("project/$it") },
                            onOpenNote = { navController.navigate("note/$it") },
                            onOpenList = { navController.navigate("list/$it") },
                            onOpenSaved = { navController.navigate("saved/$it") }
                        )
                    }
                }
                composable("trash") {
                    SharedDestination(sharedScope, this) {
                        val trashViewModel = hiltViewModel<TrashViewModel>()
                        TrashScreen(trashViewModel, onBack = { navController.popBackStack() })
                    }
                }
                composable("history") {
                    com.luminor.actionbox.ui.history.HistoryScreen(hiltViewModel(), onBack = { navController.popBackStack() })
                }
                composable("action/{id}") {
                    SharedDestination(sharedScope, this) {
                        val detailViewModel = hiltViewModel<com.luminor.actionbox.ui.actions.ActionEditorViewModel>()
                        val detail by detailViewModel.detail.collectAsStateWithLifecycle()
                        DetailContent(detail, onBack = { navController.popBackStack() }, onRetry = { detailViewModel.retry() }) { item ->
                            ActionEditorScreen(detailViewModel, item, onBack = { navController.popBackStack() }, onNoteOpen = { navController.navigate("note/$it") })
                        }
                    }
                }
                composable("note/{id}") {
                    SharedDestination(sharedScope, this) {
                        val detailViewModel = hiltViewModel<com.luminor.actionbox.ui.organize.notes.NoteDetailViewModel>()
                        val detail by detailViewModel.detail.collectAsStateWithLifecycle()
                        DetailContent(detail, onBack = { navController.popBackStack() }, onRetry = { detailViewModel.retry() }) { item ->
                            NoteDetailScreen(detailViewModel, item, onBack = { navController.popBackStack() })
                        }
                    }
                }
                composable("project/{id}") {
                    SharedDestination(sharedScope, this) {
                        val detailViewModel = hiltViewModel<com.luminor.actionbox.ui.organize.ProjectViewModel>()
                        val detail by detailViewModel.detail.collectAsStateWithLifecycle()
                        DetailContent(detail, onBack = { navController.popBackStack() }, onRetry = { detailViewModel.retry() }) { item ->
                            ProjectDetailScreen(detailViewModel, item.id, onBack = { navController.popBackStack() }, onNoteOpen = { navController.navigate("note/$it") })
                        }
                    }
                }
                composable("list/{id}") {
                    SharedDestination(sharedScope, this) {
                        val detailViewModel = hiltViewModel<com.luminor.actionbox.ui.organize.lists.ListViewModel>()
                        val detail by detailViewModel.detail.collectAsStateWithLifecycle()
                        DetailContent(detail, onBack = { navController.popBackStack() }, onRetry = { detailViewModel.retry() }) { item ->
                            ListDetailScreen(detailViewModel, item.id, onBack = { navController.popBackStack() })
                        }
                    }
                }
                composable("routine/{id}") {
                    SharedDestination(sharedScope, this) {
                        val detailViewModel = hiltViewModel<com.luminor.actionbox.ui.organize.routines.RoutineViewModel>()
                        val detail by detailViewModel.detail.collectAsStateWithLifecycle()
                        DetailContent(detail, onBack = { navController.popBackStack() }, onRetry = { detailViewModel.retry() }) { item ->
                            RoutineDetailScreen(detailViewModel, item.id, onBack = { navController.popBackStack() })
                        }
                    }
                }
                composable("saved/{id}") {
                    SharedDestination(sharedScope, this) {
                        val detailViewModel = hiltViewModel<com.luminor.actionbox.ui.saved.SavedDetailViewModel>()
                        val detail by detailViewModel.detail.collectAsStateWithLifecycle()
                        DetailContent(detail, onBack = { navController.popBackStack() }, onRetry = { detailViewModel.retry() }) { item ->
                            SavedDetailScreen(detailViewModel, item.id, onBack = { navController.popBackStack() })
                        }
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
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

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
                        .semantics { this.selected = selected; contentDescription = textResources.getString(item.label) }
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
                    Text(textResources.getString(item.label), style = MaterialTheme.typography.labelMedium, color = tint)
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
