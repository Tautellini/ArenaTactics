package net.tautellini.arenatactics.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ScreenNavigationTest {

    // ─── fromPath ────────────────────────────────────────────────────────────

    @Test fun fromPathRoot() =
        assertEquals(Screen.AddonSelection, Screen.fromPath("/"))

    @Test fun fromPathAddonHub() =
        assertEquals(Screen.AddonHub("tbc_anniversary"), Screen.fromPath("/tbc_anniversary"))

    @Test fun fromPathGameModeSelection() =
        assertEquals(Screen.GameModeSelection("tbc_anniversary"), Screen.fromPath("/tbc_anniversary/tactics"))

    @Test fun fromPathCompositionSelection() =
        assertEquals(
            Screen.CompositionSelection("tbc_anniversary", "tbc_anniversary_2v2"),
            Screen.fromPath("/tbc_anniversary/tactics/tbc_anniversary_2v2")
        )

    @Test fun fromPathMatchupList() =
        assertEquals(
            Screen.MatchupList("tbc_anniversary", "tbc_anniversary_2v2", "mage_rogue"),
            Screen.fromPath("/tbc_anniversary/tactics/tbc_anniversary_2v2/mage_rogue/matchups")
        )

    @Test fun fromPathMatchupDetail() =
        assertEquals(
            Screen.MatchupDetail("tbc_anniversary", "tbc_anniversary_2v2", "mage_rogue", "some_matchup"),
            Screen.fromPath("/tbc_anniversary/tactics/tbc_anniversary_2v2/mage_rogue/matchups/some_matchup")
        )

    @Test fun fromPathClassGuideList() =
        assertEquals(Screen.ClassGuideList("tbc_anniversary"), Screen.fromPath("/tbc_anniversary/guides"))

    @Test fun fromPathSpecGuide() =
        assertEquals(
            Screen.SpecGuide("tbc_anniversary", "druid", "druid_restoration"),
            Screen.fromPath("/tbc_anniversary/guides/druid/druid_restoration")
        )

    @Test fun fromPathUnknownReturnsAddonSelection() =
        assertEquals(Screen.AddonSelection, Screen.fromPath("/unknown/route/that/has/many/segments"))

    // ─── buildStack ──────────────────────────────────────────────────────────

    @Test fun buildStackAddonSelectionIsSingle() {
        val stack = Screen.buildStack(Screen.AddonSelection)
        assertEquals(1, stack.size)
        assertIs<Screen.AddonSelection>(stack[0])
    }

    @Test fun buildStackAddonHub() {
        val screen = Screen.AddonHub("tbc_anniversary")
        val stack = Screen.buildStack(screen)
        assertEquals(2, stack.size)
        assertIs<Screen.AddonSelection>(stack[0])
        assertEquals(screen, stack[1])
    }

    @Test fun buildStackGameModeSelection() {
        val screen = Screen.GameModeSelection("tbc_anniversary")
        val stack = Screen.buildStack(screen)
        assertEquals(3, stack.size)
        assertIs<Screen.AddonSelection>(stack[0])
        assertEquals(Screen.AddonHub("tbc_anniversary"), stack[1])
        assertEquals(screen, stack[2])
    }

    @Test fun buildStackCompositionSelection() {
        val screen = Screen.CompositionSelection("tbc_anniversary", "tbc_anniversary_2v2")
        val stack = Screen.buildStack(screen)
        assertEquals(4, stack.size)
        assertIs<Screen.AddonSelection>(stack[0])
        assertEquals(Screen.AddonHub("tbc_anniversary"), stack[1])
        assertEquals(Screen.GameModeSelection("tbc_anniversary"), stack[2])
        assertEquals(screen, stack[3])
    }

    @Test fun buildStackMatchupList() {
        val screen = Screen.MatchupList("tbc_anniversary", "tbc_anniversary_2v2", "mage_rogue")
        val stack = Screen.buildStack(screen)
        assertEquals(5, stack.size)
        assertEquals(screen, stack[4])
    }

    @Test fun buildStackMatchupDetail() {
        val screen = Screen.MatchupDetail("tbc_anniversary", "tbc_anniversary_2v2", "mage_rogue", "m1")
        val stack = Screen.buildStack(screen)
        assertEquals(6, stack.size)
        assertEquals(Screen.MatchupList("tbc_anniversary", "tbc_anniversary_2v2", "mage_rogue"), stack[4])
        assertEquals(screen, stack[5])
    }

    @Test fun buildStackClassGuideList() {
        val screen = Screen.ClassGuideList("tbc_anniversary")
        val stack = Screen.buildStack(screen)
        assertEquals(3, stack.size)
        assertEquals(Screen.AddonHub("tbc_anniversary"), stack[1])
        assertEquals(screen, stack[2])
    }

    @Test fun buildStackSpecGuide() {
        val screen = Screen.SpecGuide("tbc_anniversary", "druid", "druid_restoration")
        val stack = Screen.buildStack(screen)
        assertEquals(4, stack.size)
        assertEquals(Screen.ClassGuideList("tbc_anniversary"), stack[2])
        assertEquals(screen, stack[3])
    }

    // ─── path round-trip ─────────────────────────────────────────────────────

    @Test fun pathRoundTrip() {
        val screens = listOf(
            Screen.AddonSelection,
            Screen.AddonHub("tbc_anniversary"),
            Screen.GameModeSelection("tbc_anniversary"),
            Screen.CompositionSelection("tbc_anniversary", "tbc_anniversary_2v2"),
            Screen.MatchupList("tbc_anniversary", "tbc_anniversary_2v2", "mage_rogue"),
            Screen.MatchupDetail("tbc_anniversary", "tbc_anniversary_2v2", "mage_rogue", "some_matchup"),
            Screen.ClassGuideList("tbc_anniversary"),
            Screen.SpecGuide("tbc_anniversary", "druid", "druid_restoration")
        )
        screens.forEach { screen ->
            assertEquals(screen, Screen.fromPath(screen.path), "round-trip failed for $screen")
        }
    }
}
