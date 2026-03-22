package net.tautellini.arenatactics.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable data object AddonSelection : Screen()
    @Serializable data class CompositionSelection(val addonId: String, val gameModeId: String) : Screen()
    @Serializable data class MatchupList(val addonId: String, val gameModeId: String, val compositionId: String) : Screen()
    @Serializable data class MatchupDetail(val addonId: String, val gameModeId: String, val compositionId: String, val matchupId: String) : Screen()
    @Serializable data class ClassGuideList(val addonId: String) : Screen()
    @Serializable data class SpecGuide(val addonId: String, val classId: String, val specId: String) : Screen()
    @Serializable data class Ladder(val addonId: String) : Screen()

    /**
     * Compact browser URL. Internal IDs are unchanged; only the URL is shortened.
     *
     * Examples:
     *   /tbc_anniversary/tactics/2v2
     *   /tbc_anniversary/tactics/2v2/mage_frost+rogue_subtlety
     *   /tbc_anniversary/tactics/2v2/mage_frost+rogue_subtlety/vs/druid_restoration+warrior_arms
     *   /tbc_anniversary/guides/hunter_marksmanship
     */
    val path: String get() = when (this) {
        is AddonSelection       -> "/"
        is CompositionSelection -> "/$addonId/tactics/${gameModeId.shortBracket(addonId)}"
        is MatchupList          -> "/$addonId/tactics/${gameModeId.shortBracket(addonId)}/${compositionId.specIdsToPlus()}"
        is MatchupDetail        -> "/$addonId/tactics/${gameModeId.shortBracket(addonId)}/${compositionId.specIdsToPlus()}/vs/${matchupId.enemyPart()}"
        is ClassGuideList       -> "/$addonId/guides"
        is SpecGuide            -> "/$addonId/guides/$specId"
        is Ladder               -> "/$addonId/ladder"
    }

    companion object {
        fun fromPath(pathname: String): Screen {
            val segs = pathname.trim('/').split('/').filter { it.isNotEmpty() }
            val addonId = segs.getOrNull(0) ?: return AddonSelection
            return when (val section = segs.getOrNull(1)) {
                null      -> AddonSelection
                "tactics" -> {
                    val shortBracket = segs.getOrNull(2) ?: return AddonSelection
                    val modeId = shortBracket.expandBracket(addonId)
                    val compSlug = segs.getOrNull(3) ?: return CompositionSelection(addonId, modeId)
                    val compId = compSlug.plusToSpecIds()
                    if (segs.getOrNull(4) != "vs") return MatchupList(addonId, modeId, compId)
                    val enemySlug = segs.getOrNull(5) ?: return MatchupList(addonId, modeId, compId)
                    val matchupId = "${compId}_vs_${enemySlug.plusToSpecIds()}"
                    MatchupDetail(addonId, modeId, compId, matchupId)
                }
                "guides"  -> {
                    val specId = segs.getOrNull(2) ?: return ClassGuideList(addonId)
                    val classId = specId.substringBefore('_')
                    SpecGuide(addonId, classId, specId)
                }
                "ladder"  -> Ladder(addonId)
                else      -> AddonSelection
            }
        }

        fun buildStack(screen: Screen): List<Screen> = when (screen) {
            is AddonSelection       -> listOf(screen)
            is CompositionSelection -> listOf(AddonSelection, screen)
            is MatchupList          -> listOf(AddonSelection, CompositionSelection(screen.addonId, screen.gameModeId), screen)
            is MatchupDetail        -> listOf(AddonSelection, CompositionSelection(screen.addonId, screen.gameModeId), MatchupList(screen.addonId, screen.gameModeId, screen.compositionId), screen)
            is ClassGuideList       -> listOf(AddonSelection, screen)
            is SpecGuide            -> listOf(AddonSelection, ClassGuideList(screen.addonId), screen)
            is Ladder               -> listOf(AddonSelection, screen)

        }
    }
}

/**
 * Converts a NavBackStackEntry to the corresponding [Screen] by reading typed route args.
 * Used in the web URL bridge to push browser history on destination changes.
 *
 * Uses destination.route string matching since the KMP navigation-compose version
 * does not expose a type-safe hasRoute<T>() overload. The route string always
 * contains the class simple name, so substring matching is stable.
 * MatchupDetail is checked before MatchupList to avoid false positive on "Matchup".
 */
fun NavBackStackEntry.toScreen(): Screen {
    val route = destination.route ?: return Screen.AddonSelection
    return when {
        "AddonSelection"       in route -> Screen.AddonSelection
        "MatchupDetail"        in route -> toRoute<Screen.MatchupDetail>()
            .let { Screen.MatchupDetail(it.addonId, it.gameModeId, it.compositionId, it.matchupId) }
        "MatchupList"          in route -> toRoute<Screen.MatchupList>()
            .let { Screen.MatchupList(it.addonId, it.gameModeId, it.compositionId) }
        "CompositionSelection" in route -> toRoute<Screen.CompositionSelection>()
            .let { Screen.CompositionSelection(it.addonId, it.gameModeId) }
        "ClassGuideList"       in route -> toRoute<Screen.ClassGuideList>()
            .let { Screen.ClassGuideList(it.addonId) }
        "SpecGuide"            in route -> toRoute<Screen.SpecGuide>()
            .let { Screen.SpecGuide(it.addonId, it.classId, it.specId) }
        "Ladder"               in route -> toRoute<Screen.Ladder>()
            .let { Screen.Ladder(it.addonId) }
        else                            -> Screen.AddonSelection // should not happen; all routes are registered above
    }
}

// ─── URL shortening helpers ─────────────────────────────────────────────────

/** "tbc_anniversary_2v2" → "2v2" (strip addonId prefix + underscore). */
private fun String.shortBracket(addonId: String): String =
    removePrefix("${addonId}_").ifEmpty { this }

/** "2v2" → "tbc_anniversary_2v2" (restore full game mode ID). */
private fun String.expandBracket(addonId: String): String =
    if (startsWith(addonId)) this else "${addonId}_$this"

/** "mage_frost_rogue_subtlety" → "mage_frost+rogue_subtlety" (underscore between specs → plus). */
private fun String.specIdsToPlus(): String =
    split('_').chunkedAsSpecIds().joinToString("+")

/** "mage_frost+rogue_subtlety" → "mage_frost_rogue_subtlety" (plus → underscore). */
private fun String.plusToSpecIds(): String =
    split('+').joinToString("_")

/**
 * "mage_frost_rogue_subtlety_vs_druid_restoration_warrior_arms"
 *  → "druid_restoration+warrior_arms"
 */
private fun String.enemyPart(): String {
    val vsIdx = indexOf("_vs_")
    if (vsIdx < 0) return this
    return substring(vsIdx + 4).specIdsToPlus()
}

/**
 * Groups a flat list of underscore-split tokens back into "class_spec" pairs.
 * e.g. ["mage", "frost", "rogue", "subtlety"] → ["mage_frost", "rogue_subtlety"]
 */
private fun List<String>.chunkedAsSpecIds(): List<String> =
    chunked(2) { it.joinToString("_") }
