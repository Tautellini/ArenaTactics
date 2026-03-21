package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.Addon
import net.tautellini.arenatactics.data.model.GameMode
import net.tautellini.arenatactics.data.repository.AddonRepository
import net.tautellini.arenatactics.data.repository.GameModeRepository

class HomeViewModel(
    private val addonRepository: AddonRepository,
    private val gameModeRepository: GameModeRepository
) : ViewModel() {

    private val _state = MutableStateFlow<HomeState>(HomeState.Loading)
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private var lastLoadedAddonId: String? = null

    init {
        viewModelScope.launch {
            try {
                val addons = addonRepository.getAll()
                _state.value = HomeState.Success(addons)
            } catch (t: Throwable) {
                _state.value = HomeState.Error(t.message ?: "Failed to load addons")
            }
        }
    }

    fun loadGameModes(addonId: String) {
        // Guard: if already Ready for the same addonId, do nothing
        val current = _state.value
        if (current is HomeState.Success &&
            current.gameModeRow is GameModeRowState.Ready &&
            lastLoadedAddonId == addonId) return

        val successState = current as? HomeState.Success ?: return

        viewModelScope.launch {
            _state.value = successState.copy(gameModeRow = GameModeRowState.Loading)
            try {
                val modes = gameModeRepository.getByAddon(addonId)
                lastLoadedAddonId = addonId
                _state.value = (_state.value as HomeState.Success).copy(
                    gameModeRow = GameModeRowState.Ready(modes)
                )
            } catch (t: Throwable) {
                _state.value = (_state.value as? HomeState.Success)?.copy(
                    gameModeRow = GameModeRowState.Error(t.message ?: "Failed to load game modes")
                ) ?: _state.value
            }
        }
    }

    fun resetGameModes() {
        lastLoadedAddonId = null
        val current = _state.value
        if (current is HomeState.Success) {
            _state.value = current.copy(gameModeRow = GameModeRowState.Idle)
        }
    }
}

sealed class HomeState {
    data object Loading : HomeState()
    data class Error(val message: String) : HomeState()
    data class Success(
        val addons: List<Addon>,
        val gameModeRow: GameModeRowState = GameModeRowState.Idle
    ) : HomeState()
}

sealed class GameModeRowState {
    data object Idle : GameModeRowState()
    data object Loading : GameModeRowState()
    data class Ready(val modes: List<GameMode>) : GameModeRowState()
    data class Error(val message: String) : GameModeRowState()
}
