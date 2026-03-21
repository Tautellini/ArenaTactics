package net.tautellini.arenatactics.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ScreenNavigationTest {

    // ─── fromPath ────────────────────────────────────────────────────────────

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
        assertEquals(
            Screen.CompositionSelection("tbc"),
            Screen.fromPath("/ArenaTactics/modes/tbc")
        )
    }

    @Test
    fun fromPathUnknownReturnsGameModeSelection() {
        assertEquals(Screen.GameModeSelection, Screen.fromPath("/unknown/route"))
    }

    // ─── buildStack ──────────────────────────────────────────────────────────

    @Test
    fun buildStackForGameModeSelectionIsSingleElement() {
        val stack = Screen.buildStack(Screen.GameModeSelection)
        assertEquals(1, stack.size)
        assertIs<Screen.GameModeSelection>(stack[0])
    }

    @Test
    fun buildStackForCompositionSelectionHasRoot() {
        val screen = Screen.CompositionSelection("tbc")
        val stack = Screen.buildStack(screen)
        assertEquals(2, stack.size)
        assertIs<Screen.GameModeSelection>(stack[0])
        assertEquals(screen, stack[1])
    }

    @Test
    fun buildStackForGearViewHasCorrectAncestors() {
        val screen = Screen.GearView("tbc", "mage_rogue")
        val stack = Screen.buildStack(screen)
        assertEquals(3, stack.size)
        assertIs<Screen.GameModeSelection>(stack[0])
        assertEquals(Screen.CompositionSelection("tbc"), stack[1])
        assertEquals(screen, stack[2])
    }

    @Test
    fun buildStackForMatchupListHasCorrectAncestors() {
        val screen = Screen.MatchupList("tbc", "mage_rogue")
        val stack = Screen.buildStack(screen)
        assertEquals(3, stack.size)
        assertIs<Screen.GameModeSelection>(stack[0])
        assertEquals(Screen.CompositionSelection("tbc"), stack[1])
        assertEquals(screen, stack[2])
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

    // ─── path ────────────────────────────────────────────────────────────────

    @Test
    fun pathRoundTrip() {
        val screens = listOf(
            Screen.GameModeSelection,
            Screen.CompositionSelection("tbc"),
            Screen.GearView("tbc", "mage_rogue"),
            Screen.MatchupList("tbc", "mage_rogue"),
            Screen.MatchupDetail("tbc", "mage_rogue", "mage_rogue_vs_druid_warrior")
        )
        screens.forEach { screen ->
            assertEquals(screen, Screen.fromPath(screen.path), "round-trip failed for $screen")
        }
    }
}
