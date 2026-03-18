package net.tautellini.arenatactics.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class NavigatorTest {
    @Test
    fun initialScreenIsGameModeSelection() {
        val nav = Navigator()
        assertEquals(Screen.GameModeSelection, nav.current)
    }

    @Test
    fun pushAddsToStack() {
        val nav = Navigator()
        nav.push(Screen.CompositionSelection("tbc"))
        assertEquals(Screen.CompositionSelection("tbc"), nav.current)
    }

    @Test
    fun popReturnsToParent() {
        val nav = Navigator()
        nav.push(Screen.CompositionSelection("tbc"))
        nav.pop()
        assertEquals(Screen.GameModeSelection, nav.current)
    }

    @Test
    fun popOnRootDoesNothing() {
        val nav = Navigator()
        nav.pop()
        assertEquals(Screen.GameModeSelection, nav.current)
    }
}
