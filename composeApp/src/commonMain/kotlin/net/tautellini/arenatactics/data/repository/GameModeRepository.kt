package net.tautellini.arenatactics.data.repository

import arenatactics.composeapp.generated.resources.Res
import net.tautellini.arenatactics.data.model.GameMode

internal fun parseGameModes(jsonString: String): List<GameMode> =
    appJson.decodeFromString(jsonString)

open class GameModeRepository {
    open suspend fun getAll(): List<GameMode> {
        val bytes = Res.readBytes("files/game_modes.json")
        return parseGameModes(bytes.decodeToString())
    }

    open suspend fun getByAddon(addonId: String): List<GameMode> =
        getAll().filter { it.addonId == addonId }
}
