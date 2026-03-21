package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.GearPhase
import net.tautellini.arenatactics.data.model.WowClass
import net.tautellini.arenatactics.data.model.WowSpec
import net.tautellini.arenatactics.data.repository.AddonRepository
import net.tautellini.arenatactics.data.repository.CompositionRepository
import net.tautellini.arenatactics.data.repository.GearRepository
import net.tautellini.arenatactics.data.repository.SpecRepository

sealed class SpecGuideState {
    data object Loading : SpecGuideState()
    data class Success(
        val spec: WowSpec,
        val wowClass: WowClass,
        val phases: List<GearPhase>
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
    private val gearRepository: GearRepository
) : ViewModel() {
    private val _state = MutableStateFlow<SpecGuideState>(SpecGuideState.Loading)
    val state: StateFlow<SpecGuideState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                val addon    = addonRepository.getById(addonId)
                    ?: throw IllegalArgumentException("Unknown addon: $addonId")
                val spec     = specRepository.getById(addon.specPoolId, specId)
                    ?: throw IllegalArgumentException("Unknown spec: $specId")
                val classes  = compositionRepository.getClasses(addon.classPoolId)
                val wowClass = classes.find { it.id == classId }
                    ?: throw IllegalArgumentException("Unknown class: $classId")
                val phases   = gearRepository.getGearForSpec(classId)
                SpecGuideState.Success(spec, wowClass, phases)
            } catch (e: Throwable) {
                SpecGuideState.Error(e.message ?: "Failed to load spec guide")
            }
        }
    }
}
