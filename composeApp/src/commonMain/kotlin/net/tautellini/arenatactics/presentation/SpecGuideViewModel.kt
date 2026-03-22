package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.WowClass
import net.tautellini.arenatactics.data.model.WowSpec
import net.tautellini.arenatactics.data.repository.AddonRepository
import net.tautellini.arenatactics.data.repository.CompositionRepository
import net.tautellini.arenatactics.data.repository.LadderRepository
import net.tautellini.arenatactics.data.repository.SpecRepository
import net.tautellini.arenatactics.domain.SpecMeta
import net.tautellini.arenatactics.domain.computeSpecMeta

sealed class SpecGuideState {
    data object Loading : SpecGuideState()
    data class Success(
        val spec: WowSpec,
        val wowClass: WowClass,
        val meta: SpecMeta
    ) : SpecGuideState()
    data class Error(val message: String) : SpecGuideState()
}

class SpecGuideViewModel(
    private val addonId: String,
    private val classId: String,
    private val specId: String,
    private val addonRepository: AddonRepository,
    private val specRepository: SpecRepository,
    private val compositionRepository: CompositionRepository,
    private val ladderRepository: LadderRepository
) : ViewModel() {
    private val _state = MutableStateFlow<SpecGuideState>(SpecGuideState.Loading)
    val state: StateFlow<SpecGuideState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                val addon = addonRepository.getById(addonId)
                    ?: throw IllegalArgumentException("Unknown addon: $addonId")
                val spec = specRepository.getById(addon.specPoolId, specId)
                    ?: throw IllegalArgumentException("Unknown spec: $specId")
                val classes = compositionRepository.getClasses(addon.classPoolId)
                val wowClass = classes.find { it.id == classId }
                    ?: throw IllegalArgumentException("Unknown class: $classId")

                // Aggregate from all regions' player profiles
                val allPlayers = listOf("us", "eu").flatMap { region ->
                    try {
                        ladderRepository.getPlayerProfiles(addonId, region).values.toList()
                    } catch (_: Throwable) { emptyList() }
                }

                val meta = computeSpecMeta(specId, allPlayers)
                SpecGuideState.Success(spec, wowClass, meta)
            } catch (e: Throwable) {
                SpecGuideState.Error(e.message ?: "Failed to load spec guide")
            }
        }
    }
}
