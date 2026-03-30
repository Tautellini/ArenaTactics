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
import net.tautellini.models.arenatactics.TalentTreeDefinition
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import net.tautellini.arenatactics.data.repository.AddonRepository
import net.tautellini.arenatactics.data.repository.CompositionRepository
import net.tautellini.arenatactics.data.repository.LadderRepository
import net.tautellini.arenatactics.data.repository.TalentTreeRepository
import net.tautellini.models.arenatactics.ItemTooltipData
import net.tautellini.models.arenatactics.SpecMeta

sealed class MetaState {
    data object Loading : MetaState()
    data class Success(
        val specs: List<WowSpec>,
        val classMap: Map<String, WowClass>,
        val specsWithData: Set<String>
    ) : MetaState()
    data class Error(val message: String) : MetaState()
}

sealed class SpecMetaState {
    data object Idle : SpecMetaState()
    data object Loading : SpecMetaState()
    data class Ready(
        val spec: WowSpec,
        val wowClass: WowClass,
        val meta: SpecMeta,
        val items: Map<String, ItemTooltipData> = emptyMap(),
        val talentTree: TalentTreeDefinition? = null
    ) : SpecMetaState()
}

class MetaViewModel(
    private val addonId: String,
    private val addonRepository: AddonRepository,
    private val compositionRepository: CompositionRepository,
    private val ladderRepository: LadderRepository,
    private val talentTreeRepository: TalentTreeRepository
) : ViewModel() {
    private val _state = MutableStateFlow<MetaState>(MetaState.Loading)
    val state: StateFlow<MetaState> = _state.asStateFlow()

    private val _selectedClassId = MutableStateFlow<String?>(null)
    val selectedClassId: StateFlow<String?> = _selectedClassId.asStateFlow()

    private val _selectedSpecId = MutableStateFlow<String?>(null)
    val selectedSpecId: StateFlow<String?> = _selectedSpecId.asStateFlow()

    private val _specMetaState = MutableStateFlow<SpecMetaState>(SpecMetaState.Idle)
    val specMetaState: StateFlow<SpecMetaState> = _specMetaState.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                val addon = addonRepository.getById(addonId)
                    ?: throw IllegalArgumentException("Unknown addon: $addonId")
                val specs = compositionRepository.getSpecs(addon.specPoolId)
                    .sortedWith(compareBy({ when (it.role) { SpecRole.DPS -> 0; else -> 1 } }, { it.name }))
                val classes = compositionRepository.getClasses(addon.classPoolId)
                val classMap = classes.associateBy { it.id }

                // Determine which specs have data from ladder snapshots
                val specsWithData = mutableSetOf<String>()
                val indices = ladderRepository.getIndex(addonId)
                for (idx in indices) {
                    try {
                        val snapshot = ladderRepository.getSnapshot(addonId, idx.region, idx.bracket)
                        // Try specDistribution first, fall back to topEntries
                        if (snapshot.specDistribution.isNotEmpty()) {
                            snapshot.specDistribution.forEach { specsWithData.add(it.specId) }
                        } else {
                            snapshot.topEntries.forEach { entry ->
                                entry.specId?.let { specsWithData.add(it) }
                            }
                        }
                    } catch (_: Throwable) {}
                }

                MetaState.Success(specs, classMap, specsWithData)
            } catch (e: Throwable) {
                MetaState.Error(e.message ?: "Failed to load meta data")
            }
        }
    }

    fun selectClass(classId: String?) {
        val toggled = if (_selectedClassId.value == classId) null else classId
        _selectedClassId.value = toggled
        _selectedSpecId.value = null
        _specMetaState.value = SpecMetaState.Idle
    }

    fun selectSpec(specId: String?) {
        val toggled = if (_selectedSpecId.value == specId) null else specId
        _selectedSpecId.value = toggled

        if (toggled == null) {
            _specMetaState.value = SpecMetaState.Idle
            return
        }

        val success = _state.value as? MetaState.Success ?: return
        val spec = success.specs.find { it.id == toggled } ?: return
        val wowClass = success.classMap[spec.classId] ?: return

        _specMetaState.value = SpecMetaState.Loading
        viewModelScope.launch {
            val meta = ladderRepository.getSpecMeta(toggled)
            if (meta == null) {
                _specMetaState.value = SpecMetaState.Idle
                return@launch
            }

            // Fetch item tooltips for all items in the meta slot breakdowns
            val itemIds = meta.slotBreakdowns
                .flatMap { slot -> slot.items.map { it.itemId } }
                .filter { it > 0 }
                .distinct()
            val items = itemIds.map { itemId ->
                async {
                    try {
                        val item = ladderRepository.getItem(addonId, itemId)
                        if (item != null) itemId.toString() to item else null
                    } catch (_: Throwable) { null }
                }
            }.awaitAll().filterNotNull().toMap()

            val talentTree = try {
                talentTreeRepository.getTree(addonId, spec.classId)
            } catch (_: Throwable) { null }
            _specMetaState.value = SpecMetaState.Ready(spec, wowClass, meta, items, talentTree)
        }
    }
}
