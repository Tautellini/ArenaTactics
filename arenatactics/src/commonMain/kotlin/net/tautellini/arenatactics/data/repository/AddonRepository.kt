package net.tautellini.arenatactics.data.repository

import net.tautellini.arenatactics.data.api.ArenaApi
import net.tautellini.models.arenatactics.Addon

class AddonRepository(private val api: ArenaApi) {
    private var cache: List<Addon>? = null

    suspend fun getAll(): List<Addon> {
        return cache ?: api.getAddons().also { cache = it }
    }

    suspend fun getById(id: String): Addon? = getAll().find { it.id == id }
}
