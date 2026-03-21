package net.tautellini.arenatactics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import net.tautellini.arenatactics.data.repository.*
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.navigation.toScreen
import net.tautellini.arenatactics.presentation.*
import net.tautellini.arenatactics.presentation.screens.*
import net.tautellini.arenatactics.presentation.screens.components.AppHeader
import net.tautellini.arenatactics.presentation.theme.ArenaTacticsTheme
import net.tautellini.arenatactics.presentation.theme.Background

@Composable
fun App() {
    val gameModeRepository = remember { GameModeRepository() }
    val specRepository = remember { SpecRepository() }
    val compositionRepository = remember { CompositionRepository(specRepository) }
    val gearRepository = remember { GearRepository(compositionRepository) }
    val matchupRepository = remember { MatchupRepository() }

    val navController = rememberNavController()

    // Track whether the current nav change was triggered by the browser popstate
    // so we don't push it back to browser history.
    var browserPopPending by remember { mutableStateOf(false) }

    // Web URL bridge — push browser history state on each in-app navigation
    LaunchedEffect(navController) {
        snapshotFlow { navController.currentBackStackEntry }
            .collect { entry ->
                if (browserPopPending) {
                    browserPopPending = false
                    return@collect
                }
                val path = entry?.toScreen()?.path ?: return@collect
                try { pushNavigationState(path) } catch (_: Throwable) {}
            }
    }

    // Browser back/forward button support
    DisposableEffect(navController) {
        registerPopCallback {
            browserPopPending = true
            val target = Screen.fromPath(getCurrentPath())
            if (target is Screen.GameModeSelection) {
                navController.popBackStack<Screen.GameModeSelection>(inclusive = false)
            } else {
                navController.navigate(target) {
                    popUpTo<Screen.GameModeSelection>()
                    launchSingleTop = true
                }
            }
        }
        onDispose { registerPopCallback {} }
    }

    // Deep-link initialization: navigate to the stack implied by the initial URL
    LaunchedEffect(Unit) {
        val initialScreen = Screen.fromPath(getInitialPath())
        if (initialScreen !is Screen.GameModeSelection) {
            // browserPopPending prevents URL pushes during the init navigations
            browserPopPending = true
            Screen.buildStack(initialScreen).drop(1).forEachIndexed { index, screen ->
                if (index > 0) browserPopPending = true // suppress each push except first handled above
                navController.navigate(screen)
            }
            browserPopPending = false
        }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = currentBackStackEntry?.toScreen() ?: Screen.GameModeSelection

    ArenaTacticsTheme {
        SharedTransitionLayout {
            val sharedScope = this

            Column(Modifier.fillMaxSize().background(Background)) {

                // Persistent AppHeader on all non-home screens
                AnimatedVisibility(
                    visible = currentScreen !is Screen.GameModeSelection,
                    enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { -it },
                    exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it }
                ) {
                    val animScope = this
                    val shieldMod = with(sharedScope) {
                        Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState(key = "shield"),
                            animatedVisibilityScope = animScope
                        )
                    }
                    AppHeader(
                        currentScreen = currentScreen,
                        onNavigate = { target ->
                            if (target is Screen.GameModeSelection) {
                                navController.popBackStack<Screen.GameModeSelection>(inclusive = false)
                            } else {
                                navController.navigate(target) {
                                    popUpTo<Screen.GameModeSelection>()
                                    launchSingleTop = true
                                }
                            }
                        },
                        shieldModifier = shieldMod
                    )
                }

                NavHost(
                    navController = navController,
                    startDestination = Screen.GameModeSelection,
                    modifier = Modifier.weight(1f),
                    enterTransition = {
                        fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.95f)
                    },
                    exitTransition = {
                        fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 1.05f)
                    },
                    popEnterTransition = {
                        fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 1.05f)
                    },
                    popExitTransition = {
                        fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.95f)
                    }
                ) {
                    composable<Screen.GameModeSelection> {
                        val vm = viewModel { GameModeSelectionViewModel(gameModeRepository) }
                        val shieldMod = with(sharedScope) {
                            Modifier.sharedElement(
                                sharedContentState = rememberSharedContentState(key = "shield"),
                                animatedVisibilityScope = this@composable
                            )
                        }
                        GameModeSelectionScreen(
                            viewModel = vm,
                            onNavigate = { navController.navigate(it) },
                            shieldModifier = shieldMod
                        )
                    }

                    composable<Screen.CompositionSelection> { entry ->
                        val screen = entry.toRoute<Screen.CompositionSelection>()
                        val vm = viewModel(key = screen.gameModeId) {
                            CompositionSelectionViewModel(
                                screen.gameModeId,
                                gameModeRepository,
                                compositionRepository
                            )
                        }
                        CompositionSelectionScreen(
                            gameModeId = screen.gameModeId,
                            viewModel = vm,
                            onNavigate = { navController.navigate(it) }
                        )
                    }

                    composable<Screen.GearView> { entry ->
                        val screen = entry.toRoute<Screen.GearView>()
                        val gearVm = viewModel(key = "gear_${screen.compositionId}") {
                            GearViewModel(
                                screen.gameModeId, screen.compositionId,
                                gameModeRepository, compositionRepository, gearRepository
                            )
                        }
                        val matchupVm = viewModel(key = "matchups_${screen.compositionId}") {
                            MatchupListViewModel(
                                screen.gameModeId, screen.compositionId,
                                gameModeRepository, compositionRepository, matchupRepository
                            )
                        }
                        CompositionHubScreen(
                            gameModeId = screen.gameModeId,
                            compositionId = screen.compositionId,
                            gearViewModel = gearVm,
                            matchupListViewModel = matchupVm,
                            initialTab = CompositionTab.GEAR,
                            onNavigate = { navController.navigate(it) }
                        )
                    }

                    composable<Screen.MatchupList> { entry ->
                        val screen = entry.toRoute<Screen.MatchupList>()
                        val gearVm = viewModel(key = "gear_${screen.compositionId}") {
                            GearViewModel(
                                screen.gameModeId, screen.compositionId,
                                gameModeRepository, compositionRepository, gearRepository
                            )
                        }
                        val matchupVm = viewModel(key = "matchups_${screen.compositionId}") {
                            MatchupListViewModel(
                                screen.gameModeId, screen.compositionId,
                                gameModeRepository, compositionRepository, matchupRepository
                            )
                        }
                        CompositionHubScreen(
                            gameModeId = screen.gameModeId,
                            compositionId = screen.compositionId,
                            gearViewModel = gearVm,
                            matchupListViewModel = matchupVm,
                            initialTab = CompositionTab.MATCHUPS,
                            onNavigate = { navController.navigate(it) }
                        )
                    }

                    composable<Screen.MatchupDetail> { entry ->
                        val screen = entry.toRoute<Screen.MatchupDetail>()
                        val vm = viewModel(key = screen.matchupId) {
                            MatchupDetailViewModel(
                                screen.gameModeId, screen.compositionId, screen.matchupId,
                                gameModeRepository, compositionRepository, matchupRepository
                            )
                        }
                        MatchupDetailScreen(viewModel = vm)
                    }
                }
            }
        }
    }
}
