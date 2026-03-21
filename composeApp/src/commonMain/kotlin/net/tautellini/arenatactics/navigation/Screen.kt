package net.tautellini.arenatactics.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable
    data object GameModeSelection : Screen()

    @Serializable
    data class CompositionSelection(val gameModeId: String) : Screen()

    @Serializable
    data class GearView(val gameModeId: String, val compositionId: String) : Screen()

    @Serializable
    data class MatchupList(val gameModeId: String, val compositionId: String) : Screen()

    @Serializable
    data class MatchupDetail(
        val gameModeId: String,
        val compositionId: String,
        val matchupId: String
    ) : Screen()

    val path: String get() = when (this) {
        is GameModeSelection    -> "/"
        is CompositionSelection -> "/modes/$gameModeId"
        is GearView             -> "/modes/$gameModeId/comp/$compositionId/gear"
        is MatchupList          -> "/modes/$gameModeId/comp/$compositionId/matchups"
        is MatchupDetail        -> "/modes/$gameModeId/comp/$compositionId/matchups/$matchupId"
    }

    companion object {
        /**
         * Parses a URL pathname into a Screen. Works regardless of any base-path prefix.
         */
        fun fromPath(pathname: String): Screen {
            val segments = pathname.trim('/').split('/').filter { it.isNotEmpty() }
            val modesIdx = segments.indexOf("modes")
            if (modesIdx == -1) return GameModeSelection

            val rel = segments.drop(modesIdx)
            val gameModeId = rel.getOrNull(1) ?: return GameModeSelection
            if (rel.getOrNull(2) != "comp") return CompositionSelection(gameModeId)

            val compositionId = rel.getOrNull(3) ?: return CompositionSelection(gameModeId)
            return when (rel.getOrNull(4)) {
                "gear"     -> GearView(gameModeId, compositionId)
                "matchups" -> {
                    val matchupId = rel.getOrNull(5)
                    if (matchupId != null) MatchupDetail(gameModeId, compositionId, matchupId)
                    else MatchupList(gameModeId, compositionId)
                }
                else -> CompositionSelection(gameModeId)
            }
        }

        /**
         * Builds the full navigation back-stack for a screen so Back works
         * correctly when a deep link is opened directly.
         */
        fun buildStack(screen: Screen): List<Screen> = when (screen) {
            is GameModeSelection    -> listOf(screen)
            is CompositionSelection -> listOf(GameModeSelection, screen)
            is GearView             -> listOf(GameModeSelection, CompositionSelection(screen.gameModeId), screen)
            is MatchupList          -> listOf(GameModeSelection, CompositionSelection(screen.gameModeId), screen)
            is MatchupDetail        -> listOf(
                GameModeSelection,
                CompositionSelection(screen.gameModeId),
                MatchupList(screen.gameModeId, screen.compositionId),
                screen
            )
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
    val route = destination.route ?: return Screen.GameModeSelection
    return when {
        "GameModeSelection"    in route -> Screen.GameModeSelection
        "MatchupDetail"        in route -> toRoute<Screen.MatchupDetail>()
            .let { Screen.MatchupDetail(it.gameModeId, it.compositionId, it.matchupId) }
        "GearView"             in route -> toRoute<Screen.GearView>()
            .let { Screen.GearView(it.gameModeId, it.compositionId) }
        "MatchupList"          in route -> toRoute<Screen.MatchupList>()
            .let { Screen.MatchupList(it.gameModeId, it.compositionId) }
        "CompositionSelection" in route -> toRoute<Screen.CompositionSelection>()
            .let { Screen.CompositionSelection(it.gameModeId) }
        else                            -> Screen.GameModeSelection
    }
}
