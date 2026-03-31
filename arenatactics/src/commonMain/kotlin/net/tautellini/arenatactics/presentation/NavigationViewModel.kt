package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.repository.AddonRepository
import net.tautellini.arenatactics.data.repository.GameModeRepository
import net.tautellini.arenatactics.data.repository.LadderRepository
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.models.arenatactics.Addon
import net.tautellini.models.arenatactics.GameMode

enum class NavSection { TACTICS, META, LADDER }

data class NavigationState(
    val addons: List<Addon> = emptyList(),
    val gameModes: List<GameMode> = emptyList(),
    val addonsWithLadder: Set<String> = emptySet(),
    val selectedAddonId: String? = null,
    val selectedSection: NavSection? = null,
    val selectedGameModeId: String? = null,
    val isDrawerOpen: Boolean = false,
    val isLoading: Boolean = true
)

class NavigationViewModel(
    private val addonRepository: AddonRepository,
    private val gameModeRepository: GameModeRepository,
    private val ladderRepository: LadderRepository
) : ViewModel() {
    private val _state = MutableStateFlow(NavigationState())
    val state: StateFlow<NavigationState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val addons = addonRepository.getAll()
                val addonsWithLadder = addons.filter { addon ->
                    try { ladderRepository.getIndex(addon.id).isNotEmpty() } catch (_: Throwable) { false }
                }.map { it.id }.toSet()

                _state.value = _state.value.copy(
                    addons = addons,
                    addonsWithLadder = addonsWithLadder,
                    isLoading = false
                )

                // Auto-select first addon with data, or load game modes
                // for an addon already set by deep-link sync
                val currentAddonId = _state.value.selectedAddonId
                if (currentAddonId == null) {
                    val defaultAddon = addons.firstOrNull { it.hasData }
                    if (defaultAddon != null) {
                        selectAddon(defaultAddon.id)
                    }
                } else if (_state.value.gameModes.isEmpty()) {
                    // Deep-link already set the addon but game modes weren't loaded yet
                    try {
                        val modes = gameModeRepository.getByAddon(currentAddonId)
                        _state.value = _state.value.copy(gameModes = modes)
                    } catch (_: Throwable) {}
                }
            } catch (_: Throwable) {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun selectAddon(addonId: String) {
        if (_state.value.selectedAddonId == addonId) return
        _state.value = _state.value.copy(
            selectedAddonId = addonId,
            selectedSection = null,
            selectedGameModeId = null,
            gameModes = emptyList()
        )
        viewModelScope.launch {
            try {
                val modes = gameModeRepository.getByAddon(addonId)
                _state.value = _state.value.copy(gameModes = modes)
            } catch (_: Throwable) {}
        }
    }

    fun selectSection(section: NavSection) {
        _state.value = _state.value.copy(
            selectedSection = section,
            selectedGameModeId = null
        )
    }

    fun selectBracket(gameModeId: String) {
        _state.value = _state.value.copy(selectedGameModeId = gameModeId)
    }

    fun toggleDrawer() {
        _state.value = _state.value.copy(isDrawerOpen = !_state.value.isDrawerOpen)
    }

    fun closeDrawer() {
        _state.value = _state.value.copy(isDrawerOpen = false)
    }

    fun syncFromScreen(screen: Screen) {
        val current = _state.value
        when (screen) {
            is Screen.Dashboard -> {
                // Keep addon, clear section/bracket
                _state.value = current.copy(selectedSection = null, selectedGameModeId = null)
            }
            is Screen.CompositionSelection -> {
                ensureAddon(screen.addonId)
                _state.value = _state.value.copy(
                    selectedSection = NavSection.TACTICS,
                    selectedGameModeId = screen.gameModeId
                )
            }
            is Screen.MatchupList -> {
                ensureAddon(screen.addonId)
                _state.value = _state.value.copy(
                    selectedSection = NavSection.TACTICS,
                    selectedGameModeId = screen.gameModeId
                )
            }
            is Screen.MatchupDetail -> {
                ensureAddon(screen.addonId)
                _state.value = _state.value.copy(
                    selectedSection = NavSection.TACTICS,
                    selectedGameModeId = screen.gameModeId
                )
            }
            is Screen.Meta -> {
                ensureAddon(screen.addonId)
                _state.value = _state.value.copy(
                    selectedSection = NavSection.META,
                    selectedGameModeId = null
                )
            }
            is Screen.ClassGuideList -> {
                ensureAddon(screen.addonId)
                _state.value = _state.value.copy(
                    selectedSection = NavSection.META,
                    selectedGameModeId = null
                )
            }
            is Screen.SpecGuide -> {
                ensureAddon(screen.addonId)
                _state.value = _state.value.copy(
                    selectedSection = NavSection.META,
                    selectedGameModeId = null
                )
            }
            is Screen.Ladder -> {
                ensureAddon(screen.addonId)
                _state.value = _state.value.copy(
                    selectedSection = NavSection.LADDER,
                    selectedGameModeId = null
                )
            }
            is Screen.PlayerDetail -> {
                ensureAddon(screen.addonId)
                _state.value = _state.value.copy(
                    selectedSection = NavSection.LADDER,
                    selectedGameModeId = null
                )
            }
            is Screen.AuthCallback -> {} // No sidebar state change for auth callback
        }
    }

    private fun ensureAddon(addonId: String) {
        if (_state.value.selectedAddonId != addonId) {
            _state.value = _state.value.copy(selectedAddonId = addonId)
            viewModelScope.launch {
                try {
                    val modes = gameModeRepository.getByAddon(addonId)
                    _state.value = _state.value.copy(gameModes = modes)
                } catch (_: Throwable) {}
            }
        }
    }
}
