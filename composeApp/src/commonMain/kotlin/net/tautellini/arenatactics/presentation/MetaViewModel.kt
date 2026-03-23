package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.ItemTooltipData
import net.tautellini.arenatactics.data.model.PlayerProfile
import net.tautellini.arenatactics.data.model.SpecRole
import net.tautellini.arenatactics.data.model.WowClass
import net.tautellini.arenatactics.data.model.WowSpec
import net.tautellini.arenatactics.data.model.TalentTreeDefinition
import net.tautellini.arenatactics.data.repository.AddonRepository
import net.tautellini.arenatactics.data.repository.CompositionRepository
import net.tautellini.arenatactics.data.repository.LadderRepository
import net.tautellini.arenatactics.data.repository.TalentTreeRepository
import net.tautellini.arenatactics.domain.SpecMeta
import net.tautellini.arenatactics.domain.computeSpecMeta

sealed class MetaState {
    data object Loading : MetaState()
    data class Success(
        val specs: List<WowSpec>,
        val classMap: Map<String, WowClass>,
        val specsWithData: Set<String>,
        val allPlayers: List<PlayerProfile>,
        val allItems: Map<String, ItemTooltipData>
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

    private val specMetaCache = mutableMapOf<String, SpecMeta>()

    init {
        viewModelScope.launch {
            _state.value = try {
                val addon = addonRepository.getById(addonId)
                    ?: throw IllegalArgumentException("Unknown addon: $addonId")
                val specs = compositionRepository.getSpecs(addon.specPoolId)
                    .sortedWith(compareBy({ when (it.role) { SpecRole.DPS -> 0; else -> 1 } }, { it.name }))
                val classes = compositionRepository.getClasses(addon.classPoolId)
                val classMap = classes.associateBy { it.id }

                // Load all player profiles from both regions
                val allPlayers = listOf("us", "eu").flatMap { region ->
                    try {
                        ladderRepository.getPlayerProfiles(addonId, region).values.toList()
                    } catch (_: Throwable) { emptyList() }
                }

                // Load all item tooltip data from both regions
                val allItems = mutableMapOf<String, ItemTooltipData>()
                for (region in listOf("us", "eu")) {
                    try { allItems.putAll(ladderRepository.getItems(addonId, region)) } catch (_: Throwable) {}
                }

                // Determine which specs have player data
                val specsWithData = mutableSetOf<String>()
                for (player in allPlayers) {
                    player.specId?.let { specsWithData.add(it) }
                }

                MetaState.Success(specs, classMap, specsWithData, allPlayers, allItems)
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
            // Check cache first
            val meta = specMetaCache.getOrPut(toggled) {
                computeSpecMeta(toggled, success.allPlayers)
            }
            val talentTree = try {
                talentTreeRepository.getTree(addonId, spec.classId)
            } catch (_: Throwable) { null }
            _specMetaState.value = SpecMetaState.Ready(spec, wowClass, meta, talentTree)
        }
    }
}
