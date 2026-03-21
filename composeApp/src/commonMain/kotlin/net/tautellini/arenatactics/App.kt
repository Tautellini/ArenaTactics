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
import kotlinx.coroutines.flow.drop
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
import net.tautellini.arenatactics.presentation.AddonHubViewModel
import net.tautellini.arenatactics.presentation.HomeViewModel
import net.tautellini.arenatactics.presentation.ClassGuideListViewModel
import net.tautellini.arenatactics.presentation.CompositionSelectionViewModel
import net.tautellini.arenatactics.presentation.GameModeSelectionViewModel
import net.tautellini.arenatactics.presentation.MatchupDetailViewModel
import net.tautellini.arenatactics.presentation.MatchupListViewModel
import net.tautellini.arenatactics.presentation.SpecGuideViewModel
import net.tautellini.arenatactics.presentation.screens.AddonHubScreen
import net.tautellini.arenatactics.presentation.screens.AddonSelectionScreen
import net.tautellini.arenatactics.presentation.screens.ClassGuideListScreen
import net.tautellini.arenatactics.presentation.screens.CompositionSelectionScreen
import net.tautellini.arenatactics.presentation.screens.MatchupDetailScreen
import net.tautellini.arenatactics.presentation.screens.MatchupListScreen
import net.tautellini.arenatactics.presentation.screens.SpecGuideScreen
import net.tautellini.arenatactics.presentation.screens.TacticsGameModeSelectionScreen
import net.tautellini.arenatactics.presentation.screens.components.AppHeader
import net.tautellini.arenatactics.presentation.theme.ArenaTacticsTheme
import net.tautellini.arenatactics.presentation.theme.Background

@Composable
fun App() {
    val addonRepository = remember { AddonRepository() }
    val gameModeRepository = remember { GameModeRepository() }
    val specRepository = remember { SpecRepository() }
    val compositionRepository = remember { CompositionRepository(specRepository) }
    val gearRepository = remember { GearRepository() }
    val matchupRepository = remember { MatchupRepository() }

    val navController = rememberNavController()

    // How many snapshotFlow emissions to skip at startup:
    //   1 for the initial sync emit (AddonSelection) +
    //   (deep-link stack depth - 1) for each nav during deep-link init.
    val initialSkipCount = remember {
        Screen.buildStack(Screen.fromPath(getInitialPath())).size
    }

    // Web URL bridge — push browser history state on each in-app navigation.
    // We drop the initial N emissions that correspond to startup/deep-link init
    // so we never call pushNavigationState for navigations the browser already
    // has in its history. Browser-triggered navigations (popstate) are handled
    // separately and must NOT push state; they use a plain ref flag to skip one
    // emission without triggering recomposition.
    val skipNextPush = remember { booleanArrayOf(false) }
    LaunchedEffect(navController) {
        snapshotFlow { navController.currentBackStackEntry }
            .drop(initialSkipCount)
            .collect { entry ->
                if (skipNextPush[0]) {
                    skipNextPush[0] = false
                    return@collect
                }
                val path = entry?.toScreen()?.path ?: return@collect
                try { pushNavigationState(path) } catch (_: Throwable) {}
            }
    }

    // Browser back/forward button support.
    // isBack=true  → navigateUp() pops exactly one entry (matches one browser-back step).
    // isBack=false → navigate(target) adds the target on top (restores forward stack).
    // Neither calls pushNavigationState; the browser URL is already correct.
    DisposableEffect(navController) {
        registerPopCallback { isBack ->
            skipNextPush[0] = true
            if (isBack) {
                navController.navigateUp()
            } else {
                navController.navigate(Screen.fromPath(getCurrentPath()))
            }
        }
        onDispose { registerPopCallback { _ -> } }
    }

    // Deep-link initialization: build the nav stack implied by the initial URL.
    // These navigations are covered by initialSkipCount so no URL push occurs.
    LaunchedEffect(Unit) {
        val initialScreen = Screen.fromPath(getInitialPath())
        if (initialScreen !is Screen.AddonSelection) {
            Screen.buildStack(initialScreen).drop(1).forEach { screen ->
                navController.navigate(screen)
            }
        }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = currentBackStackEntry?.toScreen() ?: Screen.AddonSelection

    ArenaTacticsTheme {
        SharedTransitionLayout {
            val sharedScope = this

            Column(Modifier.fillMaxSize().background(Background)) {

                // Persistent AppHeader on all non-home screens
                AnimatedVisibility(
                    visible = currentScreen !is Screen.AddonSelection,
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
                            if (target is Screen.AddonSelection) {
                                navController.popBackStack<Screen.AddonSelection>(inclusive = false)
                            } else {
                                navController.navigate(target) {
                                    popUpTo<Screen.AddonSelection>()
                                    launchSingleTop = true
                                }
                            }
                        },
                        shieldModifier = shieldMod
                    )
                }

                NavHost(
                    navController = navController,
                    startDestination = Screen.AddonSelection,
                    modifier = Modifier.weight(1f),
                    enterTransition  = { fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.95f) },
                    exitTransition   = { fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 1.05f) },
                    popEnterTransition = { fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 1.05f) },
                    popExitTransition  = { fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.95f) }
                ) {
                    composable<Screen.AddonSelection> {
                        val vm = viewModel { HomeViewModel(addonRepository, gameModeRepository) }
                        val shieldMod = with(sharedScope) {
                            Modifier.sharedElement(
                                sharedContentState = rememberSharedContentState(key = "shield"),
                                animatedVisibilityScope = this@composable
                            )
                        }
                        AddonSelectionScreen(viewModel = vm, onNavigate = { navController.navigate(it) }, shieldModifier = shieldMod)
                    }
                    composable<Screen.AddonHub> { entry ->
                        val screen = entry.toRoute<Screen.AddonHub>()
                        val vm = viewModel(key = screen.addonId) { AddonHubViewModel(screen.addonId, addonRepository) }
                        AddonHubScreen(viewModel = vm, onNavigate = { navController.navigate(it) })
                    }
                    composable<Screen.GameModeSelection> { entry ->
                        val screen = entry.toRoute<Screen.GameModeSelection>()
                        val vm = viewModel(key = screen.addonId) { GameModeSelectionViewModel(screen.addonId, gameModeRepository) }
                        TacticsGameModeSelectionScreen(addonId = screen.addonId, viewModel = vm, onNavigate = { navController.navigate(it) })
                    }
                    composable<Screen.CompositionSelection> { entry ->
                        val screen = entry.toRoute<Screen.CompositionSelection>()
                        val vm = viewModel(key = "${screen.addonId}_${screen.gameModeId}") {
                            CompositionSelectionViewModel(
                                screen.addonId, screen.gameModeId,
                                addonRepository, gameModeRepository, compositionRepository
                            )
                        }
                        CompositionSelectionScreen(
                            addonId = screen.addonId,
                            gameModeId = screen.gameModeId,
                            viewModel = vm,
                            onNavigate = { navController.navigate(it) }
                        )
                    }
                    composable<Screen.MatchupList> { entry ->
                        val screen = entry.toRoute<Screen.MatchupList>()
                        val vm = viewModel(key = "matchups_${screen.addonId}_${screen.compositionId}") {
                            MatchupListViewModel(
                                screen.addonId, screen.gameModeId, screen.compositionId,
                                addonRepository, compositionRepository, matchupRepository
                            )
                        }
                        MatchupListScreen(
                            addonId = screen.addonId,
                            gameModeId = screen.gameModeId,
                            compositionId = screen.compositionId,
                            viewModel = vm,
                            onNavigate = { navController.navigate(it) }
                        )
                    }
                    composable<Screen.MatchupDetail> { entry ->
                        val screen = entry.toRoute<Screen.MatchupDetail>()
                        val vm = viewModel(key = "detail_${screen.compositionId}_${screen.matchupId}") {
                            MatchupDetailViewModel(
                                screen.addonId, screen.gameModeId, screen.compositionId, screen.matchupId,
                                addonRepository, compositionRepository, matchupRepository
                            )
                        }
                        MatchupDetailScreen(viewModel = vm)
                    }
                    composable<Screen.ClassGuideList> { entry ->
                        val screen = entry.toRoute<Screen.ClassGuideList>()
                        val vm = viewModel(key = "guides_${screen.addonId}") {
                            ClassGuideListViewModel(screen.addonId, addonRepository, compositionRepository)
                        }
                        ClassGuideListScreen(addonId = screen.addonId, viewModel = vm, onNavigate = { navController.navigate(it) })
                    }
                    composable<Screen.SpecGuide> { entry ->
                        val screen = entry.toRoute<Screen.SpecGuide>()
                        val vm = viewModel(key = "spec_${screen.addonId}_${screen.specId}") {
                            SpecGuideViewModel(
                                screen.addonId, screen.classId, screen.specId,
                                addonRepository, specRepository, compositionRepository, gearRepository
                            )
                        }
                        SpecGuideScreen(viewModel = vm)
                    }
                }
            }
        }
    }
}
