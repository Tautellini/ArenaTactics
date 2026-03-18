package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.Matchup
import net.tautellini.arenatactics.data.model.WowClass
import net.tautellini.arenatactics.data.repository.CompositionRepository
import net.tautellini.arenatactics.data.repository.GameModeRepository
import net.tautellini.arenatactics.data.repository.MatchupRepository

sealed class MatchupListState {
    data object Loading : MatchupListState()
    data class Success(
        val matchups: List<Matchup>,
        val classMap: Map<String, WowClass>
    ) : MatchupListState()
    data class Error(val message: String) : MatchupListState()
}

class MatchupListViewModel(
    private val gameModeId: String,
    private val compositionId: String,
    private val gameModeRepository: GameModeRepository,
    private val compositionRepository: CompositionRepository,
    private val matchupRepository: MatchupRepository
) : ViewModel() {
    private val _state = MutableStateFlow<MatchupListState>(MatchupListState.Loading)
    val state: StateFlow<MatchupListState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                val mode = gameModeRepository.getAll().first { it.id == gameModeId }
                val classes = compositionRepository.getClasses(mode.classPoolId)
                val classMap = classes.associateBy { it.id }
                val matchups = matchupRepository.getForComposition(compositionId)
                MatchupListState.Success(matchups, classMap)
            } catch (e: Throwable) {
                MatchupListState.Error(e.message ?: "Failed to load matchups")
            }
        }
    }
}
