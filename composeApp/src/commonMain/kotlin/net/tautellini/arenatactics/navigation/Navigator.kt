package net.tautellini.arenatactics.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.tautellini.arenatactics.pushNavigationState

class Navigator(initialStack: List<Screen> = listOf(Screen.GameModeSelection)) {
    private val _stack = MutableStateFlow(initialStack)
    val stack: StateFlow<List<Screen>> = _stack.asStateFlow()

    val current: Screen get() = _stack.value.last()

    fun push(screen: Screen) {
        _stack.value = _stack.value + screen
        pushNavigationState(screen.path)
    }

    fun pop() {
        if (_stack.value.size > 1) {
            _stack.value = _stack.value.dropLast(1)
        }
    }
}
