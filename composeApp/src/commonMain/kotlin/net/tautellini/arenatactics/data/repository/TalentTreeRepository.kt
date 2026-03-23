package net.tautellini.arenatactics.data.repository

import arenatactics.composeapp.generated.resources.Res
import net.tautellini.arenatactics.data.model.TalentTreeDefinition

class TalentTreeRepository {
    private val cache = mutableMapOf<String, TalentTreeDefinition?>()

    suspend fun getTree(addonId: String, classId: String): TalentTreeDefinition? {
        val key = "${addonId}_$classId"
        if (key in cache) return cache[key]

        return try {
            // Map addon ID to talent tree pool (e.g. tbc_anniversary -> tbc)
            val pool = when {
                addonId.startsWith("tbc") -> "tbc"
                else -> addonId
            }
            val bytes = Res.readBytes("files/talent_trees/$pool/$classId.json")
            val tree = appJson.decodeFromString<TalentTreeDefinition>(bytes.decodeToString())
            cache[key] = tree
            tree
        } catch (_: Throwable) {
            cache[key] = null
            null
        }
    }
}
