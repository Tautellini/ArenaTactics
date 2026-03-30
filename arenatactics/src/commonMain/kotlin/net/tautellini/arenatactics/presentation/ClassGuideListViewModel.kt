package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.models.arenatactics.SpecRole
import net.tautellini.models.arenatactics.WowClass
import net.tautellini.models.arenatactics.WowSpec
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

                // Determine which specs have data — try first snapshot only
                val specsWithData = mutableSetOf<String>()
                val indices = ladderRepository.getIndex(addonId)
                for (idx in indices) {
                    try {
                        val snapshot = ladderRepository.getSnapshot(addonId, idx.region, idx.bracket)
                        if (snapshot.specDistribution.isNotEmpty()) {
                            snapshot.specDistribution.forEach { specsWithData.add(it.specId) }
                        } else {
                            snapshot.topEntries.forEach { entry ->
                                entry.specId?.let { specsWithData.add(it) }
                            }
                        }
                        if (specsWithData.isNotEmpty()) break
                    } catch (_: Throwable) {}
                }

                // If no ladder data, assume all specs are available
                if (specsWithData.isEmpty()) {
                    specs.forEach { specsWithData.add(it.id) }
                }

                ClassGuideListState.Success(specs, classMap, specsWithData)
            } catch (e: Throwable) {
                ClassGuideListState.Error(e.message ?: "Failed to load class guides")
            }
        }
    }
}
