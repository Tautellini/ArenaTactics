package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.models.arenatactics.ItemTooltipData
import net.tautellini.models.arenatactics.PlayerProfile
import net.tautellini.models.arenatactics.TalentTreeDefinition
import net.tautellini.arenatactics.data.repository.LadderRepository
import net.tautellini.arenatactics.data.repository.TalentTreeRepository

sealed class PlayerDetailState {
    data object Loading : PlayerDetailState()
    data class Success(
        val player: PlayerProfile,
        val items: Map<String, ItemTooltipData>,
        val talentTree: TalentTreeDefinition? = null
    ) : PlayerDetailState()
    data class Error(val message: String) : PlayerDetailState()
}

class PlayerDetailViewModel(
    private val addonId: String,
    private val region: String,
    private val characterId: String,
    private val ladderRepository: LadderRepository,
    private val talentTreeRepository: TalentTreeRepository
) : ViewModel() {
    private val _state = MutableStateFlow<PlayerDetailState>(PlayerDetailState.Loading)
    val state: StateFlow<PlayerDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                val player = ladderRepository.getPlayerProfile(addonId, region, characterId)
                if (player == null) {
                    PlayerDetailState.Error("Player not found")
                } else {
                    // Fetch item tooltips for equipped items in parallel
                    val itemIds = player.equipment.map { it.itemId }.filter { it > 0 }.distinct()
                    val items = itemIds.map { itemId ->
                        async {
                            try {
                                val item = ladderRepository.getItem(addonId, itemId)
                                if (item != null) itemId.toString() to item else null
                            } catch (_: Throwable) { null }
                        }
                    }.awaitAll().filterNotNull().toMap()

                    val talentTree = try {
                        player.classId?.let { talentTreeRepository.getTree(addonId, it) }
                    } catch (_: Throwable) { null }

                    PlayerDetailState.Success(player, items, talentTree)
                }
            } catch (e: Throwable) {
                PlayerDetailState.Error(e.message ?: "Failed to load player")
            }
        }
    }
}
