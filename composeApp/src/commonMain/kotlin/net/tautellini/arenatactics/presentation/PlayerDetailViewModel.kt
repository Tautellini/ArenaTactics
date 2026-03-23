package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.ItemTooltipData
import net.tautellini.arenatactics.data.model.PlayerProfile
import net.tautellini.arenatactics.data.model.TalentTreeDefinition
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
                val items = try { ladderRepository.getItems(addonId, region) } catch (_: Throwable) { emptyMap() }
                val talentTree = try {
                    player?.classId?.let { talentTreeRepository.getTree(addonId, it) }
                } catch (_: Throwable) { null }
                if (player != null) PlayerDetailState.Success(player, items, talentTree)
                else PlayerDetailState.Error("Player not found")
            } catch (e: Throwable) {
                PlayerDetailState.Error(e.message ?: "Failed to load player")
            }
        }
    }
}
