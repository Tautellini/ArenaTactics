package net.tautellini.arenatactics

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import net.tautellini.arenatactics.data.repository.*
import net.tautellini.arenatactics.navigation.Navigator
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.*
import net.tautellini.arenatactics.presentation.screens.*
import net.tautellini.arenatactics.presentation.theme.ArenaTacticsTheme

@Composable
fun App() {
    val gameModeRepository = remember { GameModeRepository() }
    val compositionRepository = remember { CompositionRepository() }
    val gearRepository = remember { GearRepository(compositionRepository) }
    val matchupRepository = remember { MatchupRepository() }
    val navigator = remember {
        val initialScreen = Screen.fromPath(getInitialPath())
        Navigator(Screen.buildStack(initialScreen))
    }

    // Wire browser back button to navigator.pop()
    DisposableEffect(navigator) {
        registerPopCallback { navigator.pop() }
        onDispose { registerPopCallback {} }
    }

    val stack by navigator.stack.collectAsState()

    ArenaTacticsTheme {
        SelectionContainer {
        when (val screen = stack.last()) {
            is Screen.GameModeSelection -> {
                val vm = viewModel { GameModeSelectionViewModel(gameModeRepository) }
                GameModeSelectionScreen(vm, navigator)
            }
            is Screen.CompositionSelection -> {
                val vm = viewModel(key = screen.gameModeId) {
                    CompositionSelectionViewModel(
                        screen.gameModeId,
                        gameModeRepository,
                        compositionRepository
                    )
                }
                CompositionSelectionScreen(screen.gameModeId, vm, navigator)
            }
            is Screen.GearView -> {
                val gearVm = viewModel(key = "gear_${screen.compositionId}") {
                    GearViewModel(
                        screen.gameModeId,
                        screen.compositionId,
                        gameModeRepository,
                        compositionRepository,
                        gearRepository
                    )
                }
                val matchupVm = viewModel(key = "matchups_${screen.compositionId}") {
                    MatchupListViewModel(
                        screen.gameModeId,
                        screen.compositionId,
                        gameModeRepository,
                        compositionRepository,
                        matchupRepository
                    )
                }
                CompositionHubScreen(
                    screen.gameModeId,
                    screen.compositionId,
                    gearVm,
                    matchupVm,
                    navigator
                )
            }
            is Screen.MatchupList -> {
                val gearVm = viewModel(key = "gear_${screen.compositionId}") {
                    GearViewModel(
                        screen.gameModeId,
                        screen.compositionId,
                        gameModeRepository,
                        compositionRepository,
                        gearRepository
                    )
                }
                val matchupVm = viewModel(key = "matchups_${screen.compositionId}") {
                    MatchupListViewModel(
                        screen.gameModeId,
                        screen.compositionId,
                        gameModeRepository,
                        compositionRepository,
                        matchupRepository
                    )
                }
                CompositionHubScreen(
                    screen.gameModeId,
                    screen.compositionId,
                    gearVm,
                    matchupVm,
                    navigator
                )
            }
            is Screen.MatchupDetail -> {
                val vm = viewModel(key = screen.matchupId) {
                    MatchupDetailViewModel(
                        screen.gameModeId,
                        screen.compositionId,
                        screen.matchupId,
                        gameModeRepository,
                        compositionRepository,
                        matchupRepository
                    )
                }
                MatchupDetailScreen(vm, navigator)
            }
        }
        } // SelectionContainer
    }
}
