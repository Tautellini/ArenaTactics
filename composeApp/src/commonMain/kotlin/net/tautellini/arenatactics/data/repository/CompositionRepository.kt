package net.tautellini.arenatactics.data.repository

import arenatactics.composeapp.generated.resources.Res
import kotlinx.serialization.Serializable
import net.tautellini.arenatactics.data.model.Composition
import net.tautellini.arenatactics.data.model.CompositionTier
import net.tautellini.arenatactics.data.model.WowClass
import net.tautellini.arenatactics.data.model.WowSpec
import net.tautellini.arenatactics.domain.RichComposition

/** Surrogate used during JSON parsing to avoid triggering Composition.init sort-check. */
@Serializable
private data class CompositionDto(
    val specIds: List<String>,
    val tier: CompositionTier,
    val hasData: Boolean
)

internal fun parseWowClasses(jsonString: String): List<WowClass> =
    appJson.decodeFromString(jsonString)

internal fun parseCompositions(jsonString: String): List<Composition> {
    val dtos: List<CompositionDto> = appJson.decodeFromString(jsonString)
    // Normalise spec order on load — defensive against unsorted JSON
    return dtos.map { dto ->
        Composition(specIds = dto.specIds.sorted(), tier = dto.tier, hasData = dto.hasData)
    }
}

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
        val specs = comp.specIds.map { specId ->
            specMap[specId] ?: error("Unknown specId '$specId' — not in spec pool")
        }
        val classes = specs.map { spec ->
            classMap[spec.classId] ?: error("Unknown classId '${spec.classId}' for spec '${spec.id}'")
        }
        RichComposition(composition = comp, specs = specs, classes = classes)
    }
}

class CompositionRepository(
    private val specRepository: SpecRepository
) {
    private val classCache = mutableMapOf<String, List<WowClass>>()
    private val compCache  = mutableMapOf<String, List<Composition>>()

    suspend fun getClasses(classPoolId: String): List<WowClass> {
        return classCache.getOrPut(classPoolId) {
            val bytes = Res.readBytes("files/class_pools/$classPoolId.json")
            parseWowClasses(bytes.decodeToString())
        }
    }

    suspend fun getSpecs(specPoolId: String): List<WowSpec> =
        specRepository.getSpecs(specPoolId)

    suspend fun getCompositions(compositionSetId: String): List<Composition> {
        return compCache.getOrPut(compositionSetId) {
            val bytes = Res.readBytes("files/composition_sets/$compositionSetId.json")
            parseCompositions(bytes.decodeToString())
        }
    }

    suspend fun getRichCompositions(
        specPoolId: String,
        classPoolId: String,
        compositionSetId: String,
        teamSize: Int
    ): List<RichComposition> {
        val specs    = specRepository.getSpecs(specPoolId)
        val specMap  = specs.associateBy { it.id }
        val classes  = getClasses(classPoolId)
        val classMap = classes.associateBy { it.id }
        val comps    = getCompositions(compositionSetId)
        return enrichCompositions(comps, specMap, classMap, teamSize)
    }

    suspend fun getById(compositionId: String, compositionSetId: String): Composition? =
        getCompositions(compositionSetId).find { it.id == compositionId }
}
