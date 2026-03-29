package net.tautellini.arenatactics.data.repository

import net.tautellini.arenatactics.data.api.ArenaApi
import net.tautellini.models.arenatactics.Composition
import net.tautellini.models.arenatactics.RichComposition
import net.tautellini.models.arenatactics.SpecRole
import net.tautellini.models.arenatactics.WowClass
import net.tautellini.models.arenatactics.WowSpec

internal fun enrichCompositions(
    compositions: List<Composition>,
    specMap: Map<String, WowSpec>,
    classMap: Map<String, WowClass>,
    teamSize: Int
): List<RichComposition> {
    return compositions.map { comp ->
        require(comp.specIds.size == teamSize) {
            "Composition '${comp.id}' has ${comp.specIds.size} specs but teamSize is $teamSize"
        }
        val specsAndClasses = comp.specIds
            .map { specId ->
                val spec = specMap[specId] ?: error("Unknown specId '$specId' — not in spec pool")
                val wowClass = classMap[spec.classId] ?: error("Unknown classId '${spec.classId}' for spec '${spec.id}'")
                spec to wowClass
            }
            .sortedBy { (spec, _) -> if (spec.role == SpecRole.DPS) 0 else 1 }
        RichComposition(
            composition = comp,
            specs = specsAndClasses.map { it.first },
            classes = specsAndClasses.map { it.second }
        )
    }
}

class CompositionRepository(
    private val api: ArenaApi,
    private val specRepository: SpecRepository
) {
    private val classCache = mutableMapOf<String, List<WowClass>>()
    private val compCache = mutableMapOf<String, List<Composition>>()

    suspend fun getClasses(classPoolId: String): List<WowClass> {
        return classCache.getOrPut(classPoolId) {
            api.getClassPool(classPoolId)
        }
    }

    suspend fun getSpecs(specPoolId: String): List<WowSpec> =
        specRepository.getSpecs(specPoolId)

    suspend fun getCompositions(compositionSetId: String): List<Composition> {
        return compCache.getOrPut(compositionSetId) {
            api.getCompositionSet(compositionSetId)
        }
    }

    suspend fun getRichCompositions(
        specPoolId: String,
        classPoolId: String,
        compositionSetId: String,
        teamSize: Int
    ): List<RichComposition> {
        val specs = specRepository.getSpecs(specPoolId)
        val specMap = specs.associateBy { it.id }
        val classes = getClasses(classPoolId)
        val classMap = classes.associateBy { it.id }
        val comps = getCompositions(compositionSetId)
        return enrichCompositions(comps, specMap, classMap, teamSize)
    }

    suspend fun getById(compositionId: String, compositionSetId: String): Composition? =
        getCompositions(compositionSetId).find { it.id == compositionId }
}
