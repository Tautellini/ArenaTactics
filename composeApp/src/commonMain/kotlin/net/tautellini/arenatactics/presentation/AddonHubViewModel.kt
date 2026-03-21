package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.Addon
import net.tautellini.arenatactics.data.repository.AddonRepository

sealed class AddonHubState {
    data object Loading : AddonHubState()
    data class Success(val addon: Addon) : AddonHubState()
    data class Error(val message: String) : AddonHubState()
}

class AddonHubViewModel(
    private val addonId: String,
    private val addonRepository: AddonRepository
) : ViewModel() {
    private val _state = MutableStateFlow<AddonHubState>(AddonHubState.Loading)
    val state: StateFlow<AddonHubState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                val addon = addonRepository.getById(addonId)
                    ?: throw IllegalArgumentException("Unknown addon: $addonId")
                AddonHubState.Success(addon)
            } catch (e: Throwable) {
                AddonHubState.Error(e.message ?: "Failed to load addon")
            }
        }
    }
}
