package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.ClassDistributionEntry
import net.tautellini.arenatactics.data.model.LadderEntry
import net.tautellini.arenatactics.data.model.LadderIndex
import net.tautellini.arenatactics.data.model.LadderSnapshot
import net.tautellini.arenatactics.data.model.SpecDistribution
import net.tautellini.arenatactics.data.model.WowClass
import net.tautellini.arenatactics.data.repository.AddonRepository
import net.tautellini.arenatactics.data.repository.CompositionRepository
import net.tautellini.arenatactics.data.repository.LadderRepository

private const val PAGE_SIZE = 100

sealed class LadderState {
    data object Loading : LadderState()

    data class Success(
        val index: List<LadderIndex>,
        val snapshots: Map<String, LadderSnapshot>,
        val classes: List<WowClass>,
        val selectedRegion: String = "us",
        val selectedBracket: String = "2v2",
        val selectedClassId: String? = null,
        val currentPage: Int = 0
    ) : LadderState() {
        val currentSnapshot: LadderSnapshot?
            get() = snapshots["${selectedRegion}_$selectedBracket"]

        val availableRegions: List<String>
            get() = index.map { it.region }.distinct()

        val availableBrackets: List<String>
            get() = index.filter { it.region == selectedRegion }.map { it.bracket }

        val classDistribution: List<ClassDistributionEntry>
            get() {
                val entries = currentSnapshot?.topEntries ?: return emptyList()
                val counts = entries.mapNotNull { it.classId }
                    .groupingBy { it }.eachCount()
                if (counts.isEmpty()) return emptyList()
                val total = counts.values.sum()
                return counts.entries
                    .sortedByDescending { it.value }
                    .map { (classId, count) ->
                        ClassDistributionEntry(classId, count, ((count * 1000.0 / total).toLong() / 10.0))
                    }
            }

        /** Spec distribution derived from top entries' specId (from profile lookups). */
        val topSpecDistribution: List<SpecDistribution>
            get() {
                val entries = currentSnapshot?.topEntries ?: return emptyList()
                val counts = entries.mapNotNull { it.specId }
                    .groupingBy { it }.eachCount()
                if (counts.isEmpty()) return emptyList()
                val total = counts.values.sum()
                return counts.entries
                    .sortedByDescending { it.value }
                    .map { (specId, count) ->
                        SpecDistribution(specId, count, ((count * 1000.0 / total).toLong() / 10.0))
                    }
            }

        val filteredEntries: List<LadderEntry>
            get() {
                val entries = currentSnapshot?.topEntries ?: return emptyList()
                return if (selectedClassId == null) entries
                else entries.filter { it.classId == selectedClassId }
            }

        val pagedEntries: List<LadderEntry>
            get() = filteredEntries.drop(currentPage * PAGE_SIZE).take(PAGE_SIZE)

        val totalFilteredPages: Int
            get() = ((filteredEntries.size + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtLeast(1)
    }

    data class Empty(val message: String) : LadderState()
    data class Error(val message: String) : LadderState()
}

class LadderViewModel(
    private val addonId: String,
    private val ladderRepository: LadderRepository,
    private val addonRepository: AddonRepository,
    private val compositionRepository: CompositionRepository
) : ViewModel() {
    private val _state = MutableStateFlow<LadderState>(LadderState.Loading)
    val state: StateFlow<LadderState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                val index = ladderRepository.getIndex(addonId)
                if (index.isEmpty()) {
                    LadderState.Empty("No ladder data available yet.\nRun the fetch script to populate data.")
                } else {
                    val snapshots = mutableMapOf<String, LadderSnapshot>()
                    for (entry in index) {
                        try {
                            val key = "${entry.region}_${entry.bracket}"
                            snapshots[key] = ladderRepository.getSnapshot(addonId, entry.region, entry.bracket)
                        } catch (_: Throwable) { /* skip broken files */ }
                    }

                    // Load WoW classes for the filter bar
                    val addon = addonRepository.getById(addonId)
                    val classes = if (addon != null) {
                        try { compositionRepository.getClasses(addon.classPoolId) } catch (_: Throwable) { emptyList() }
                    } else emptyList()

                    val firstRegion = index.firstOrNull()?.region ?: "us"
                    val firstBracket = index.firstOrNull()?.bracket ?: "2v2"
                    LadderState.Success(index, snapshots, classes, firstRegion, firstBracket)
                }
            } catch (e: Throwable) {
                LadderState.Error(e.message ?: "Failed to load ladder data")
            }
        }
    }

    fun selectRegion(region: String) {
        val s = _state.value as? LadderState.Success ?: return
        val bracket = s.index
            .filter { it.region == region }
            .firstOrNull { it.bracket == s.selectedBracket }
            ?.bracket
            ?: s.index.filter { it.region == region }.firstOrNull()?.bracket
            ?: s.selectedBracket
        _state.value = s.copy(selectedRegion = region, selectedBracket = bracket, currentPage = 0)
    }

    fun selectBracket(bracket: String) {
        val s = _state.value as? LadderState.Success ?: return
        _state.value = s.copy(selectedBracket = bracket, currentPage = 0)
    }

    fun selectClass(classId: String?) {
        val s = _state.value as? LadderState.Success ?: return
        _state.value = s.copy(selectedClassId = classId, currentPage = 0)
    }

    fun setPage(page: Int) {
        val s = _state.value as? LadderState.Success ?: return
        _state.value = s.copy(currentPage = page.coerceIn(0, s.totalFilteredPages - 1))
    }
}
