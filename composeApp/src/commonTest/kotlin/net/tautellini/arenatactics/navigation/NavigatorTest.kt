package net.tautellini.arenatactics.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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

    @Test
    fun fromPathRoot() {
        assertEquals(Screen.GameModeSelection, Screen.fromPath("/"))
    }

    @Test
    fun fromPathCompositionSelection() {
        assertEquals(Screen.CompositionSelection("tbc"), Screen.fromPath("/modes/tbc"))
    }

    @Test
    fun fromPathGearView() {
        assertEquals(
            Screen.GearView("tbc", "mage_rogue"),
            Screen.fromPath("/modes/tbc/comp/mage_rogue/gear")
        )
    }

    @Test
    fun fromPathMatchupList() {
        assertEquals(
            Screen.MatchupList("tbc", "mage_rogue"),
            Screen.fromPath("/modes/tbc/comp/mage_rogue/matchups")
        )
    }

    @Test
    fun fromPathMatchupDetail() {
        assertEquals(
            Screen.MatchupDetail("tbc", "mage_rogue", "mage_rogue_vs_druid_warrior"),
            Screen.fromPath("/modes/tbc/comp/mage_rogue/matchups/mage_rogue_vs_druid_warrior")
        )
    }

    @Test
    fun fromPathStripsBasePrefixFlexibly() {
        // Works with a GitHub Pages-style base prefix
        assertEquals(
            Screen.CompositionSelection("tbc"),
            Screen.fromPath("/ArenaTactics/modes/tbc")
        )
    }

    @Test
    fun fromPathUnknownReturnsGameModeSelection() {
        assertEquals(Screen.GameModeSelection, Screen.fromPath("/unknown/route"))
    }

    @Test
    fun buildStackForMatchupDetailHasFullAncestors() {
        val detail = Screen.MatchupDetail("tbc", "mage_rogue", "some_matchup")
        val stack = Screen.buildStack(detail)
        assertEquals(4, stack.size)
        assertIs<Screen.GameModeSelection>(stack[0])
        assertEquals(Screen.CompositionSelection("tbc"), stack[1])
        assertEquals(Screen.MatchupList("tbc", "mage_rogue"), stack[2])
        assertEquals(detail, stack[3])
    }

    @Test
    fun navigatorInitialisedWithDeepLinkStack() {
        val stack = Screen.buildStack(Screen.GearView("tbc", "mage_rogue"))
        val nav = Navigator(stack)
        assertEquals(Screen.GearView("tbc", "mage_rogue"), nav.current)
        nav.pop()
        assertEquals(Screen.CompositionSelection("tbc"), nav.current)
    }
}
