package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.LadderIndex
import net.tautellini.arenatactics.data.model.LadderSnapshot
import net.tautellini.arenatactics.data.repository.LadderRepository

sealed class LadderState {
    data object Loading : LadderState()
    data class Success(
        val index: List<LadderIndex>,
        val snapshots: Map<String, LadderSnapshot>,  // key = "region_bracket"
        val selectedRegion: String = "us",
        val selectedBracket: String = "2v2"
    ) : LadderState() {
        val currentSnapshot: LadderSnapshot?
            get() = snapshots["${selectedRegion}_$selectedBracket"]

        val availableRegions: List<String>
            get() = index.map { it.region }.distinct()

        val availableBrackets: List<String>
            get() = index.filter { it.region == selectedRegion }.map { it.bracket }
    }
    data class Empty(val message: String) : LadderState()
    data class Error(val message: String) : LadderState()
}

class LadderViewModel(
    private val addonId: String,
    private val ladderRepository: LadderRepository
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
                    val firstRegion = index.firstOrNull()?.region ?: "us"
                    val firstBracket = index.firstOrNull()?.bracket ?: "2v2"
                    LadderState.Success(index, snapshots, firstRegion, firstBracket)
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
        _state.value = s.copy(selectedRegion = region, selectedBracket = bracket)
    }

    fun selectBracket(bracket: String) {
        val s = _state.value as? LadderState.Success ?: return
        _state.value = s.copy(selectedBracket = bracket)
    }
}
