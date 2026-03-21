package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.CompositionTier
import net.tautellini.arenatactics.data.model.SpecRole
import net.tautellini.arenatactics.data.repository.AddonRepository
import net.tautellini.arenatactics.data.repository.CompositionRepository
import net.tautellini.arenatactics.data.repository.GameModeRepository
import net.tautellini.arenatactics.domain.RichComposition

sealed class CompositionSelectionState {
    data object Loading : CompositionSelectionState()
    data class Success(val grouped: Map<CompositionTier, List<RichComposition>>) : CompositionSelectionState()
    data class Error(val message: String) : CompositionSelectionState()
}

class CompositionSelectionViewModel(
    private val addonId: String,
    private val gameModeId: String,
    private val addonRepository: AddonRepository,
    private val gameModeRepository: GameModeRepository,
    private val compositionRepository: CompositionRepository
) : ViewModel() {
    private val _state = MutableStateFlow<CompositionSelectionState>(CompositionSelectionState.Loading)
    val state: StateFlow<CompositionSelectionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                val addon = addonRepository.getById(addonId)
                    ?: throw IllegalArgumentException("Unknown addon: $addonId")
                val mode = gameModeRepository.getAll().first { it.id == gameModeId }
                val rich = compositionRepository.getRichCompositions(
                    specPoolId       = addon.specPoolId,
                    classPoolId      = addon.classPoolId,
                    compositionSetId = mode.compositionSetId,
                    teamSize         = mode.teamSize
                )
                val grouped = CompositionTier.entries
                    .associateWith { tier ->
                        rich.filter { it.composition.tier == tier }
                            .sortedWith(
                                compareByDescending<RichComposition> { it.composition.hasData }
                                    .thenBy { rc -> rc.specs.count { it.role == SpecRole.HEALER } }
                                    .thenBy { rc -> rc.specs.joinToString { it.name } }
                            )
                    }
                    .filterValues { it.isNotEmpty() }
                CompositionSelectionState.Success(grouped)
            } catch (e: Throwable) {
                CompositionSelectionState.Error(e.message ?: "Failed to load compositions")
            }
        }
    }
}
