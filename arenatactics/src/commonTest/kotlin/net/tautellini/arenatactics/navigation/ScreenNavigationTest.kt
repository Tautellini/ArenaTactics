package net.tautellini.arenatactics.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ScreenNavigationTest {

    // ─── fromPath ────────────────────────────────────────────────────────────

    @Test fun fromPathRoot() =
        assertEquals(Screen.AddonSelection, Screen.fromPath("/"))

    @Test fun fromPathAddonIdFallsBackToHome() =
        assertEquals(Screen.AddonSelection, Screen.fromPath("/tbc_anniversary"))

    @Test fun fromPathTacticsMissingModeFallsBackToHome() =
        assertEquals(Screen.AddonSelection, Screen.fromPath("/tbc_anniversary/tactics"))

    @Test fun fromPathCompositionSelection() =
        assertEquals(
            Screen.CompositionSelection("tbc_anniversary", "tbc_anniversary_2v2"),
            Screen.fromPath("/tbc_anniversary/tactics/2v2")
        )

    @Test fun fromPathMatchupList() =
        assertEquals(
            Screen.MatchupList("tbc_anniversary", "tbc_anniversary_2v2", "mage_frost_rogue_subtlety"),
            Screen.fromPath("/tbc_anniversary/tactics/2v2/mage_frost+rogue_subtlety")
        )

    @Test fun fromPathMatchupDetail() =
        assertEquals(
            Screen.MatchupDetail(
                "tbc_anniversary", "tbc_anniversary_2v2",
                "mage_frost_rogue_subtlety",
                "mage_frost_rogue_subtlety_vs_druid_restoration_warrior_arms"
            ),
            Screen.fromPath("/tbc_anniversary/tactics/2v2/mage_frost+rogue_subtlety/vs/druid_restoration+warrior_arms")
        )

    @Test fun fromPathClassGuideList() =
        assertEquals(Screen.ClassGuideList("tbc_anniversary"), Screen.fromPath("/tbc_anniversary/guides"))

    @Test fun fromPathSpecGuide() =
        assertEquals(
            Screen.SpecGuide("tbc_anniversary", "druid", "druid_restoration"),
            Screen.fromPath("/tbc_anniversary/guides/druid_restoration")
        )

    @Test fun fromPathUnknownReturnsAddonSelection() =
        assertEquals(Screen.AddonSelection, Screen.fromPath("/unknown/route/that/has/many/segments"))

    // ─── buildStack ──────────────────────────────────────────────────────────

    @Test fun buildStackAddonSelectionIsSingle() {
        val stack = Screen.buildStack(Screen.AddonSelection)
        assertEquals(1, stack.size)
        assertIs<Screen.AddonSelection>(stack[0])
    }

    @Test fun buildStackCompositionSelection() {
        val screen = Screen.CompositionSelection("tbc_anniversary", "tbc_anniversary_2v2")
        val stack = Screen.buildStack(screen)
        assertEquals(2, stack.size)
        assertIs<Screen.AddonSelection>(stack[0])
        assertEquals(screen, stack[1])
    }

    @Test fun buildStackMatchupList() {
        val screen = Screen.MatchupList("tbc_anniversary", "tbc_anniversary_2v2", "mage_frost_rogue_subtlety")
        val stack = Screen.buildStack(screen)
        assertEquals(3, stack.size)
        assertIs<Screen.AddonSelection>(stack[0])
        assertEquals(Screen.CompositionSelection("tbc_anniversary", "tbc_anniversary_2v2"), stack[1])
        assertEquals(screen, stack[2])
    }

    @Test fun buildStackMatchupDetail() {
        val screen = Screen.MatchupDetail(
            "tbc_anniversary", "tbc_anniversary_2v2",
            "mage_frost_rogue_subtlety",
            "mage_frost_rogue_subtlety_vs_druid_restoration_warrior_arms"
        )
        val stack = Screen.buildStack(screen)
        assertEquals(4, stack.size)
        assertIs<Screen.AddonSelection>(stack[0])
        assertEquals(Screen.CompositionSelection("tbc_anniversary", "tbc_anniversary_2v2"), stack[1])
        assertEquals(Screen.MatchupList("tbc_anniversary", "tbc_anniversary_2v2", "mage_frost_rogue_subtlety"), stack[2])
        assertEquals(screen, stack[3])
    }

    @Test fun buildStackClassGuideList() {
        val screen = Screen.ClassGuideList("tbc_anniversary")
        val stack = Screen.buildStack(screen)
        assertEquals(2, stack.size)
        assertIs<Screen.AddonSelection>(stack[0])
        assertEquals(screen, stack[1])
    }

    @Test fun buildStackSpecGuide() {
        val screen = Screen.SpecGuide("tbc_anniversary", "druid", "druid_restoration")
        val stack = Screen.buildStack(screen)
        assertEquals(3, stack.size)
        assertIs<Screen.AddonSelection>(stack[0])
        assertEquals(Screen.ClassGuideList("tbc_anniversary"), stack[1])
        assertEquals(screen, stack[2])
    }

    // ─── path round-trip ─────────────────────────────────────────────────────

    @Test fun pathRoundTrip() {
        val screens = listOf(
            Screen.AddonSelection,
            Screen.CompositionSelection("tbc_anniversary", "tbc_anniversary_2v2"),
            Screen.MatchupList("tbc_anniversary", "tbc_anniversary_2v2", "mage_frost_rogue_subtlety"),
            Screen.MatchupDetail(
                "tbc_anniversary", "tbc_anniversary_2v2",
                "mage_frost_rogue_subtlety",
                "mage_frost_rogue_subtlety_vs_druid_restoration_warrior_arms"
            ),
            Screen.ClassGuideList("tbc_anniversary"),
            Screen.SpecGuide("tbc_anniversary", "druid", "druid_restoration")
        )
        screens.forEach { screen ->
            assertEquals(screen, Screen.fromPath(screen.path), "round-trip failed for $screen (path=${screen.path})")
        }
    }

    // ─── URL format verification ─────────────────────────────────────────────

    @Test fun urlsAreCompact() {
        assertEquals(
            "/tbc_anniversary/tactics/2v2",
            Screen.CompositionSelection("tbc_anniversary", "tbc_anniversary_2v2").path
        )
        assertEquals(
            "/tbc_anniversary/tactics/2v2/mage_frost+rogue_subtlety",
            Screen.MatchupList("tbc_anniversary", "tbc_anniversary_2v2", "mage_frost_rogue_subtlety").path
        )
        assertEquals(
            "/tbc_anniversary/tactics/2v2/mage_frost+rogue_subtlety/vs/druid_restoration+warrior_arms",
            Screen.MatchupDetail(
                "tbc_anniversary", "tbc_anniversary_2v2",
                "mage_frost_rogue_subtlety",
                "mage_frost_rogue_subtlety_vs_druid_restoration_warrior_arms"
            ).path
        )
        assertEquals(
            "/tbc_anniversary/guides/druid_restoration",
            Screen.SpecGuide("tbc_anniversary", "druid", "druid_restoration").path
        )
    }
}
