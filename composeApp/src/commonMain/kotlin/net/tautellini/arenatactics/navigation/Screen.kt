package net.tautellini.arenatactics.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable data object AddonSelection : Screen()
    @Serializable data class AddonHub(val addonId: String) : Screen()
    @Serializable data class GameModeSelection(val addonId: String) : Screen()
    @Serializable data class CompositionSelection(val addonId: String, val gameModeId: String) : Screen()
    @Serializable data class MatchupList(val addonId: String, val gameModeId: String, val compositionId: String) : Screen()
    @Serializable data class MatchupDetail(val addonId: String, val gameModeId: String, val compositionId: String, val matchupId: String) : Screen()
    @Serializable data class ClassGuideList(val addonId: String) : Screen()
    @Serializable data class SpecGuide(val addonId: String, val classId: String, val specId: String) : Screen()

    val path: String get() = when (this) {
        is AddonSelection       -> "/"
        is AddonHub             -> "/$addonId"
        is GameModeSelection    -> "/$addonId/tactics"
        is CompositionSelection -> "/$addonId/tactics/$gameModeId"
        is MatchupList          -> "/$addonId/tactics/$gameModeId/$compositionId/matchups"
        is MatchupDetail        -> "/$addonId/tactics/$gameModeId/$compositionId/matchups/$matchupId"
        is ClassGuideList       -> "/$addonId/guides"
        is SpecGuide            -> "/$addonId/guides/$classId/$specId"
    }

    companion object {
        fun fromPath(pathname: String): Screen {
            val segs = pathname.trim('/').split('/').filter { it.isNotEmpty() }
            val addonId = segs.getOrNull(0) ?: return AddonSelection
            return when (val section = segs.getOrNull(1)) {
                null      -> AddonHub(addonId)
                "tactics" -> {
                    val modeId = segs.getOrNull(2) ?: return GameModeSelection(addonId)
                    val compId = segs.getOrNull(3) ?: return CompositionSelection(addonId, modeId)
                    // missing or non-"matchups" segment → fall back to composition selection
                    if (segs.getOrNull(4) != "matchups") return CompositionSelection(addonId, modeId)
                    val matchupId = segs.getOrNull(5)
                    if (matchupId != null) MatchupDetail(addonId, modeId, compId, matchupId)
                    else MatchupList(addonId, modeId, compId)
                }
                "guides"  -> {
                    val classId = segs.getOrNull(2) ?: return ClassGuideList(addonId)
                    val specId  = segs.getOrNull(3) ?: return ClassGuideList(addonId)
                    SpecGuide(addonId, classId, specId)
                }
                else      -> AddonSelection
            }
        }

        fun buildStack(screen: Screen): List<Screen> = when (screen) {
            is AddonSelection       -> listOf(screen)
            is AddonHub             -> listOf(AddonSelection, screen)
            is GameModeSelection    -> listOf(AddonSelection, AddonHub(screen.addonId), screen)
            is CompositionSelection -> listOf(AddonSelection, AddonHub(screen.addonId), GameModeSelection(screen.addonId), screen)
            is MatchupList          -> listOf(AddonSelection, AddonHub(screen.addonId), GameModeSelection(screen.addonId), CompositionSelection(screen.addonId, screen.gameModeId), screen)
            is MatchupDetail        -> listOf(AddonSelection, AddonHub(screen.addonId), GameModeSelection(screen.addonId), CompositionSelection(screen.addonId, screen.gameModeId), MatchupList(screen.addonId, screen.gameModeId, screen.compositionId), screen)
            is ClassGuideList       -> listOf(AddonSelection, AddonHub(screen.addonId), screen)
            is SpecGuide            -> listOf(AddonSelection, AddonHub(screen.addonId), ClassGuideList(screen.addonId), screen)
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
        "GameModeSelection"    in route -> toRoute<Screen.GameModeSelection>()
            .let { Screen.GameModeSelection(it.addonId) }
        "ClassGuideList"       in route -> toRoute<Screen.ClassGuideList>()
            .let { Screen.ClassGuideList(it.addonId) }
        "SpecGuide"            in route -> toRoute<Screen.SpecGuide>()
            .let { Screen.SpecGuide(it.addonId, it.classId, it.specId) }
        "AddonHub"             in route -> toRoute<Screen.AddonHub>()
            .let { Screen.AddonHub(it.addonId) }
        else                            -> Screen.AddonSelection // should not happen; all routes are registered above
    }
}
