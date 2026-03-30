package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.repository.AddonRepository
import net.tautellini.arenatactics.data.repository.CompositionRepository
import net.tautellini.arenatactics.data.repository.GameModeRepository
import net.tautellini.arenatactics.data.repository.LadderRepository
import net.tautellini.models.arenatactics.*

data class DashboardData(
    val ladderSnapshots: Map<String, LadderSnapshot> = emptyMap(), // bracket → snapshot
    val topCompositions: List<RichComposition> = emptyList(),
    val classDistribution: List<ClassDistributionEntry> = emptyList(),
    val topPlayers: List<LadderEntry> = emptyList(),
    val isLoading: Boolean = true
)

class DashboardViewModel(
    private val addonRepository: AddonRepository,
    private val gameModeRepository: GameModeRepository,
    private val compositionRepository: CompositionRepository,
    private val ladderRepository: LadderRepository
) : ViewModel() {
    private val _data = MutableStateFlow(DashboardData())
    val data: StateFlow<DashboardData> = _data.asStateFlow()

    private var loadedAddonId: String? = null

    fun loadForAddon(addonId: String) {
        if (addonId == loadedAddonId) return
        loadedAddonId = addonId

        _data.value = DashboardData(isLoading = true)

        viewModelScope.launch {
            try {
                val addon = addonRepository.getById(addonId) ?: return@launch

                // Load ladder snapshots across all brackets/regions
                val snapshots = mutableMapOf<String, LadderSnapshot>()
                val topPlayersAll = mutableListOf<LadderEntry>()
                val classCounts = mutableMapOf<String, Int>()

                val indices = ladderRepository.getIndex(addonId)
                for (idx in indices) {
                    try {
                        val snapshot = ladderRepository.getSnapshot(addonId, idx.region, idx.bracket)
                        val key = "${idx.region.uppercase()} ${idx.bracket}"
                        snapshots[key] = snapshot

                        // Aggregate top players
                        for (entry in snapshot.topEntries.take(10)) {
                            if (topPlayersAll.none { it.characterName == entry.characterName }) {
                                topPlayersAll.add(entry)
                            }
                        }

                        // Aggregate class distribution
                        for (entry in snapshot.topEntries) {
                            val classId = entry.classId ?: continue
                            classCounts[classId] = (classCounts[classId] ?: 0) + 1
                        }
                    } catch (_: Throwable) {}
                }

                val totalPlayers = classCounts.values.sum().coerceAtLeast(1)
                val classDistribution = classCounts.entries
                    .sortedByDescending { it.value }
                    .map { (classId, count) ->
                        ClassDistributionEntry(classId, count, (count * 1000.0 / totalPlayers) / 10.0)
                    }

                // Load top compositions from the first available game mode
                val topComps = try {
                    val modes = gameModeRepository.getByAddon(addonId)
                    val firstMode = modes.firstOrNull { it.hasData }
                    if (firstMode != null) {
                        compositionRepository.getRichCompositions(
                            addon.specPoolId, addon.classPoolId,
                            firstMode.compositionSetId, firstMode.teamSize
                        ).take(5)
                    } else emptyList()
                } catch (_: Throwable) { emptyList() }

                _data.value = DashboardData(
                    ladderSnapshots = snapshots,
                    topCompositions = topComps,
                    classDistribution = classDistribution,
                    topPlayers = topPlayersAll.sortedByDescending { it.rating }.take(5),
                    isLoading = false
                )
            } catch (_: Throwable) {
                _data.value = DashboardData(isLoading = false)
            }
        }
    }
}
