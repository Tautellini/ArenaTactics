package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.Addon
import net.tautellini.arenatactics.data.repository.AddonRepository

sealed class AddonSelectionState {
    data object Loading : AddonSelectionState()
    data class Success(val addons: List<Addon>) : AddonSelectionState()
    data class Error(val message: String) : AddonSelectionState()
}

class AddonSelectionViewModel(
    private val repository: AddonRepository
) : ViewModel() {
    private val _state = MutableStateFlow<AddonSelectionState>(AddonSelectionState.Loading)
    val state: StateFlow<AddonSelectionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                AddonSelectionState.Success(repository.getAll())
            } catch (e: Throwable) {
                AddonSelectionState.Error(e.message ?: "Failed to load addons")
            }
        }
    }
}
