package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.GearPhase
import net.tautellini.arenatactics.data.repository.CompositionRepository
import net.tautellini.arenatactics.data.repository.GameModeRepository
import net.tautellini.arenatactics.data.repository.GearRepository
import net.tautellini.arenatactics.domain.RichComposition

sealed class GearState {
    data object Loading : GearState()
    data class Success(
        val richComposition: RichComposition,
        val gearByClass: Map<String, List<GearPhase>>,
        val classNames: Map<String, String>
    ) : GearState()
    data class Error(val message: String) : GearState()
}

class GearViewModel(
    private val gameModeId: String,
    private val compositionId: String,
    private val gameModeRepository: GameModeRepository,
    private val compositionRepository: CompositionRepository,
    private val gearRepository: GearRepository
) : ViewModel() {
    private val _state = MutableStateFlow<GearState>(GearState.Loading)
    val state: StateFlow<GearState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                val mode = gameModeRepository.getAll().first { it.id == gameModeId }
                val richComps = compositionRepository.getRichCompositions(
                    mode.specPoolId, mode.classPoolId, mode.compositionSetId, mode.teamSize
                )
                val richComp = richComps.first { it.composition.id == compositionId }
                val classNameMap = richComp.classes.associate { it.id to it.name }
                val gear = gearRepository.getGearForComposition(compositionId, mode.compositionSetId)
                GearState.Success(richComp, gear, classNameMap)
            } catch (e: Throwable) {
                GearState.Error(e.message ?: "Failed to load gear")
            }
        }
    }
}
