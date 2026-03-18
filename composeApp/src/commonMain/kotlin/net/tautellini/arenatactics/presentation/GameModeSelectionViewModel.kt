package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.GameMode
import net.tautellini.arenatactics.data.repository.GameModeRepository

sealed class GameModeSelectionState {
    data object Loading : GameModeSelectionState()
    data class Success(val modes: List<GameMode>) : GameModeSelectionState()
    data class Error(val message: String) : GameModeSelectionState()
}

class GameModeSelectionViewModel(
    private val repository: GameModeRepository
) : ViewModel() {
    private val _state = MutableStateFlow<GameModeSelectionState>(GameModeSelectionState.Loading)
    val state: StateFlow<GameModeSelectionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                GameModeSelectionState.Success(repository.getAll())
            } catch (e: Exception) {
                GameModeSelectionState.Error(e.message ?: "Failed to load game modes")
            }
        }
    }
}
