package net.tautellini.arenatactics.data.repository

import net.tautellini.arenatactics.data.api.ArenaApi
import net.tautellini.models.arenatactics.GameMode

class GameModeRepository(private val api: ArenaApi) {
    private var cache: List<GameMode>? = null

    suspend fun getAll(): List<GameMode> {
        return cache ?: api.getGameModes().also { cache = it }
    }

    suspend fun getByAddon(addonId: String): List<GameMode> =
        getAll().filter { it.addonId == addonId }
}
