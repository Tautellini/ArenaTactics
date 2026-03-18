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
}
