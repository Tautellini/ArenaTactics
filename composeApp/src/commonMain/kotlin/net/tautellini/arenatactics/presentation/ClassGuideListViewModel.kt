package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.SpecRole
import net.tautellini.arenatactics.data.model.WowClass
import net.tautellini.arenatactics.data.model.WowSpec
import net.tautellini.arenatactics.data.repository.AddonRepository
import net.tautellini.arenatactics.data.repository.CompositionRepository
import net.tautellini.arenatactics.data.repository.LadderRepository

sealed class ClassGuideListState {
    data object Loading : ClassGuideListState()
    data class Success(
        val specs: List<WowSpec>,
        val classMap: Map<String, WowClass>,
        val specsWithData: Set<String>
    ) : ClassGuideListState()
    data class Error(val message: String) : ClassGuideListState()
}

class ClassGuideListViewModel(
    private val addonId: String,
    private val addonRepository: AddonRepository,
    private val compositionRepository: CompositionRepository,
    private val ladderRepository: LadderRepository
) : ViewModel() {
    private val _state = MutableStateFlow<ClassGuideListState>(ClassGuideListState.Loading)
    val state: StateFlow<ClassGuideListState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                val addon = addonRepository.getById(addonId)
                    ?: throw IllegalArgumentException("Unknown addon: $addonId")
                val specs = compositionRepository.getSpecs(addon.specPoolId)
                    .sortedWith(compareBy({ when (it.role) { SpecRole.DPS -> 0; else -> 1 } }, { it.name }))
                val classes = compositionRepository.getClasses(addon.classPoolId)
                val classMap = classes.associateBy { it.id }

                // Determine which specs have player data from ladder profiles
                val specsWithData = mutableSetOf<String>()
                for (region in listOf("us", "eu")) {
                    try {
                        val players = ladderRepository.getPlayerProfiles(addonId, region)
                        for (player in players.values) {
                            player.specId?.let { specsWithData.add(it) }
                        }
                    } catch (_: Throwable) {}
                }

                ClassGuideListState.Success(specs, classMap, specsWithData)
            } catch (e: Throwable) {
                ClassGuideListState.Error(e.message ?: "Failed to load class guides")
            }
        }
    }
}
