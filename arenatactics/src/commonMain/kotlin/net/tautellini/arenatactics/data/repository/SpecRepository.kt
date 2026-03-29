package net.tautellini.arenatactics.data.repository

import net.tautellini.arenatactics.data.api.ArenaApi
import net.tautellini.models.arenatactics.WowSpec

class SpecRepository(private val api: ArenaApi) {
    private val cache = mutableMapOf<String, List<WowSpec>>()

    suspend fun getSpecs(specPoolId: String): List<WowSpec> {
        return cache.getOrPut(specPoolId) {
            api.getSpecPool(specPoolId)
        }
    }

    suspend fun getById(specPoolId: String, id: String): WowSpec? =
        getSpecs(specPoolId).find { it.id == id }
}
