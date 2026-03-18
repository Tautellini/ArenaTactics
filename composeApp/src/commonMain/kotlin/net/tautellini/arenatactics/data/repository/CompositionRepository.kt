package net.tautellini.arenatactics.data.repository

import arenatactics.composeapp.generated.resources.Res
import net.tautellini.arenatactics.data.model.Composition
import net.tautellini.arenatactics.data.model.WowClass

internal fun parseWowClasses(jsonString: String): List<WowClass> =
    appJson.decodeFromString(jsonString)

internal fun parseCompositions(jsonString: String): List<Composition> {
    val raw: List<Composition> = appJson.decodeFromString(jsonString)
    return raw.map { comp ->
        val sorted = listOf(comp.class1Id, comp.class2Id).sorted()
        Composition(sorted[0], sorted[1])
    }
}

class CompositionRepository {
    private val classCache = mutableMapOf<String, List<WowClass>>()
    private val compCache = mutableMapOf<String, Map<String, Composition>>()

    suspend fun getClasses(classPoolId: String): List<WowClass> {
        return classCache.getOrPut(classPoolId) {
            val bytes = Res.readBytes("files/class_pools/$classPoolId.json")
            parseWowClasses(bytes.decodeToString())
        }
    }

    suspend fun getCompositions(compositionSetId: String): List<Composition> {
        return compCache.getOrPut(compositionSetId) {
            val bytes = Res.readBytes("files/composition_sets/$compositionSetId.json")
            parseCompositions(bytes.decodeToString()).associateBy { it.id }
        }.values.toList()
    }

    suspend fun getById(compositionId: String, compositionSetId: String): Composition? {
        return getCompositions(compositionSetId).find { it.id == compositionId }
    }
}
