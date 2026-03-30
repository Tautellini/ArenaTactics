package net.tautellini.arenatactics

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.drop
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import net.tautellini.arenatactics.data.api.ArenaApi
import net.tautellini.arenatactics.data.repository.*
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.navigation.toScreen
import net.tautellini.arenatactics.presentation.*
import net.tautellini.arenatactics.presentation.screens.*
import net.tautellini.arenatactics.presentation.screens.components.Sidebar
import net.tautellini.arenatactics.presentation.screens.components.TopBar
import net.tautellini.arenatactics.presentation.theme.ArenaTacticsTheme
import net.tautellini.arenatactics.presentation.theme.BgVoid

@Composable
fun App() {
    val api = remember { ArenaApi() }
    val addonRepository = remember { AddonRepository(api) }
    val gameModeRepository = remember { GameModeRepository(api) }
    val specRepository = remember { SpecRepository(api) }
    val compositionRepository = remember { CompositionRepository(api, specRepository) }
    val gearRepository = remember { GearRepository(api) }
    val matchupRepository = remember { MatchupRepository(api) }
    val ladderRepository = remember { LadderRepository(api) }
    val talentTreeRepository = remember { TalentTreeRepository() }

    val navigationViewModel = remember { NavigationViewModel(addonRepository, gameModeRepository, ladderRepository) }
    val dashboardViewModel = remember { DashboardViewModel(addonRepository, gameModeRepository, compositionRepository, ladderRepository) }
    val navController = rememberNavController()

    // ── Web URL bridge (unchanged logic) ──
    val initialSkipCount = remember {
        Screen.buildStack(Screen.fromPath(getInitialPath())).size
    }

    val skipNextPush = remember { booleanArrayOf(false) }
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow
            .drop(initialSkipCount)
            .collect { entry ->
                try {
                    if (skipNextPush[0]) {
                        skipNextPush[0] = false
                        return@collect
                    }
                    val screen = entry.toScreen()
                    pushNavigationState(screen.path)
                } catch (_: Throwable) {}
            }
    }

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

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = currentBackStackEntry?.toScreen() ?: Screen.Dashboard

    // Deep-link initialization — runs once the NavHost graph is available
    // (currentBackStackEntry becomes non-null after NavHost composes)
    val deepLinkHandled = remember { booleanArrayOf(false) }
    LaunchedEffect(currentBackStackEntry) {
        if (!deepLinkHandled[0] && currentBackStackEntry != null) {
            deepLinkHandled[0] = true
            val initialScreen = Screen.fromPath(getInitialPath())
            if (initialScreen !is Screen.Dashboard) {
                Screen.buildStack(initialScreen).drop(1).forEach { screen ->
                    navController.navigate(screen)
                }
            }
        }
    }

    // Reactively sync sidebar state whenever the visible screen changes
    LaunchedEffect(currentScreen) {
        navigationViewModel.syncFromScreen(currentScreen)
    }
    val navState by navigationViewModel.state.collectAsState()

    ArenaTacticsTheme {
        BoxWithConstraints(Modifier.fillMaxSize().background(BgVoid)) {
            val isCompact = maxWidth < 768.dp

            Row(Modifier.fillMaxSize()) {
                // ── Desktop sidebar ──
                if (!isCompact) {
                    Sidebar(
                        state = navState,
                        onSelectAddon = { addonId ->
                            navigationViewModel.selectAddon(addonId)
                            navController.navigate(Screen.Dashboard) {
                                popUpTo<Screen.Dashboard> { inclusive = true }
                            }
                        },
                        onSelectSection = { section ->
                            navigationViewModel.selectSection(section)
                            val addonId = navState.selectedAddonId ?: return@Sidebar
                            when (section) {
                                NavSection.META -> navController.navigate(Screen.Meta(addonId)) {
                                    popUpTo<Screen.Dashboard>()
                                    launchSingleTop = true
                                }
                                NavSection.LADDER -> navController.navigate(Screen.Ladder(addonId)) {
                                    popUpTo<Screen.Dashboard>()
                                    launchSingleTop = true
                                }
                                NavSection.TACTICS -> {} // Wait for bracket selection
                            }
                        },
                        onSelectBracket = { gameModeId ->
                            navigationViewModel.selectBracket(gameModeId)
                            val addonId = navState.selectedAddonId ?: return@Sidebar
                            navController.navigate(Screen.CompositionSelection(addonId, gameModeId)) {
                                popUpTo<Screen.Dashboard>()
                                launchSingleTop = true
                            }
                        },
                        onGoHome = {
                            navController.navigate(Screen.Dashboard) {
                                popUpTo<Screen.Dashboard> { inclusive = true }
                            }
                        }
                    )
                }

                // ── Main content ──
                Column(Modifier.weight(1f)) {
                    TopBar(
                        currentScreen = currentScreen,
                        addonName = navState.addons.find { it.id == navState.selectedAddonId }?.shortName,
                        bracketLabel = navState.gameModes.find { it.id == navState.selectedGameModeId }?.let { "${it.teamSize}v${it.teamSize}" },
                        isCompact = isCompact,
                        onToggleDrawer = { navigationViewModel.toggleDrawer() },
                        onNavigateHome = {
                            navController.navigate(Screen.Dashboard) {
                                popUpTo<Screen.Dashboard> { inclusive = true }
                            }
                        },
                        onNavigateBack = { target -> navController.navigate(target) }
                    )

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Dashboard,
                        modifier = Modifier.weight(1f),
                        enterTransition  = { fadeIn(tween(200)) },
                        exitTransition   = { fadeOut(tween(150)) },
                        popEnterTransition = { fadeIn(tween(200)) },
                        popExitTransition  = { fadeOut(tween(150)) }
                    ) {
                        composable<Screen.Dashboard> {
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                selectedAddonId = navState.selectedAddonId
                            )
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
                        composable<Screen.Meta> { entry ->
                            val screen = entry.toRoute<Screen.Meta>()
                            val vm = viewModel(key = "meta_${screen.addonId}") {
                                MetaViewModel(screen.addonId, addonRepository, compositionRepository, ladderRepository, talentTreeRepository)
                            }
                            MetaScreen(viewModel = vm)
                        }
                        composable<Screen.ClassGuideList> { entry ->
                            val screen = entry.toRoute<Screen.ClassGuideList>()
                            val vm = viewModel(key = "guides_${screen.addonId}") {
                                ClassGuideListViewModel(screen.addonId, addonRepository, compositionRepository, ladderRepository)
                            }
                            ClassGuideListScreen(addonId = screen.addonId, viewModel = vm, onNavigate = { navController.navigate(it) })
                        }
                        composable<Screen.SpecGuide> { entry ->
                            val screen = entry.toRoute<Screen.SpecGuide>()
                            val vm = viewModel(key = "spec_${screen.addonId}_${screen.specId}") {
                                SpecGuideViewModel(
                                    screen.addonId, screen.classId, screen.specId,
                                    addonRepository, specRepository, compositionRepository, ladderRepository
                                )
                            }
                            SpecGuideScreen(viewModel = vm)
                        }
                        composable<Screen.Ladder> { entry ->
                            val screen = entry.toRoute<Screen.Ladder>()
                            val vm = viewModel(key = "ladder_${screen.addonId}") {
                                LadderViewModel(screen.addonId, ladderRepository, addonRepository, compositionRepository)
                            }
                            LadderScreen(viewModel = vm, onNavigate = { navController.navigate(it) })
                        }
                        composable<Screen.PlayerDetail> { entry ->
                            val screen = entry.toRoute<Screen.PlayerDetail>()
                            val vm = viewModel(key = "player_${screen.characterId}") {
                                PlayerDetailViewModel(screen.addonId, screen.region, screen.characterId, ladderRepository, talentTreeRepository)
                            }
                            PlayerDetailScreen(viewModel = vm)
                        }
                    }
                }
            }

            // ── Mobile drawer overlay ──
            if (isCompact && navState.isDrawerOpen) {
                Row(Modifier.fillMaxSize()) {
                    Sidebar(
                        state = navState,
                        onSelectAddon = { addonId ->
                            navigationViewModel.selectAddon(addonId)
                            navigationViewModel.closeDrawer()
                            navController.navigate(Screen.Dashboard) {
                                popUpTo<Screen.Dashboard> { inclusive = true }
                            }
                        },
                        onSelectSection = { section ->
                            navigationViewModel.selectSection(section)
                            navigationViewModel.closeDrawer()
                            val addonId = navState.selectedAddonId ?: return@Sidebar
                            when (section) {
                                NavSection.META -> navController.navigate(Screen.Meta(addonId)) {
                                    popUpTo<Screen.Dashboard>()
                                    launchSingleTop = true
                                }
                                NavSection.LADDER -> navController.navigate(Screen.Ladder(addonId)) {
                                    popUpTo<Screen.Dashboard>()
                                    launchSingleTop = true
                                }
                                NavSection.TACTICS -> {} // Wait for bracket
                            }
                        },
                        onSelectBracket = { gameModeId ->
                            navigationViewModel.selectBracket(gameModeId)
                            navigationViewModel.closeDrawer()
                            val addonId = navState.selectedAddonId ?: return@Sidebar
                            navController.navigate(Screen.CompositionSelection(addonId, gameModeId)) {
                                popUpTo<Screen.Dashboard>()
                                launchSingleTop = true
                            }
                        },
                        onGoHome = {
                            navigationViewModel.closeDrawer()
                            navController.navigate(Screen.Dashboard) {
                                popUpTo<Screen.Dashboard> { inclusive = true }
                            }
                        }
                    )
                    // Scrim
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(BgVoid.copy(alpha = 0.6f))
                            .clickable { navigationViewModel.closeDrawer() }
                    )
                }
            }
        }
    }
}
