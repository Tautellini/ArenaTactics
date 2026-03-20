package net.tautellini.arenatactics.data.repository

import arenatactics.composeapp.generated.resources.Res
import net.tautellini.arenatactics.data.model.WowSpec

internal fun parseWowSpecs(jsonString: String): List<WowSpec> =
    appJson.decodeFromString(jsonString)

class SpecRepository {
    private val cache = mutableMapOf<String, List<WowSpec>>()

    suspend fun getSpecs(specPoolId: String): List<WowSpec> {
        return cache.getOrPut(specPoolId) {
            val bytes = Res.readBytes("files/spec_pools/$specPoolId.json")
            parseWowSpecs(bytes.decodeToString())
        }
    }

    suspend fun getById(specPoolId: String, id: String): WowSpec? =
        getSpecs(specPoolId).find { it.id == id }
}
