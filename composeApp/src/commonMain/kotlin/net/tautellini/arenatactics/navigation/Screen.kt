package net.tautellini.arenatactics.navigation

sealed class Screen {
    data object GameModeSelection : Screen()
    data class CompositionSelection(val gameModeId: String) : Screen()
    data class GearView(val gameModeId: String, val compositionId: String) : Screen()
    data class MatchupList(val gameModeId: String, val compositionId: String) : Screen()
    data class MatchupDetail(
        val gameModeId: String,
        val compositionId: String,
        val matchupId: String
    ) : Screen()

    val path: String get() = when (this) {
        is GameModeSelection -> "/"
        is CompositionSelection -> "/modes/$gameModeId"
        is GearView -> "/modes/$gameModeId/comp/$compositionId/gear"
        is MatchupList -> "/modes/$gameModeId/comp/$compositionId/matchups"
        is MatchupDetail -> "/modes/$gameModeId/comp/$compositionId/matchups/$matchupId"
    }

    companion object {
        /**
         * Parses a URL pathname into a Screen. Works regardless of any base-path prefix
         * (e.g. /ArenaTactics/modes/... and /modes/... both resolve correctly) by
         * finding the "modes" segment rather than assuming it starts at index 0.
         */
        fun fromPath(pathname: String): Screen {
            val segments = pathname.trim('/').split('/').filter { it.isNotEmpty() }
            val modesIdx = segments.indexOf("modes")
            if (modesIdx == -1) return GameModeSelection

            val rel = segments.drop(modesIdx) // ["modes", gameModeId, ...]
            val gameModeId = rel.getOrNull(1) ?: return GameModeSelection
            if (rel.getOrNull(2) != "comp") return CompositionSelection(gameModeId)

            val compositionId = rel.getOrNull(3) ?: return CompositionSelection(gameModeId)
            return when (rel.getOrNull(4)) {
                "gear" -> GearView(gameModeId, compositionId)
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
            is GameModeSelection -> listOf(screen)
            is CompositionSelection -> listOf(GameModeSelection, screen)
            is GearView -> listOf(GameModeSelection, CompositionSelection(screen.gameModeId), screen)
            is MatchupList -> listOf(GameModeSelection, CompositionSelection(screen.gameModeId), screen)
            is MatchupDetail -> listOf(
                GameModeSelection,
                CompositionSelection(screen.gameModeId),
                MatchupList(screen.gameModeId, screen.compositionId),
                screen
            )
        }
    }
}
