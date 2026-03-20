package net.tautellini.arenatactics.data.repository

import arenatactics.composeapp.generated.resources.Res
import net.tautellini.arenatactics.data.model.GearPhase

internal fun parseGearPhase(jsonString: String): GearPhase =
    appJson.decodeFromString(jsonString)

class GearRepository(private val compositionRepository: CompositionRepository) {
    suspend fun getGearForComposition(
        compositionId: String,
        compositionSetId: String
    ): Map<String, List<GearPhase>> {
        val comp = compositionRepository.getById(compositionId, compositionSetId) ?: return emptyMap()
        // Spec IDs follow {classId}_{specName} format (e.g. "rogue_subtlety" → "rogue").
        // Class IDs never contain underscores, so substringBefore("_") is always correct.
        val classIds = comp.specIds.map { it.substringBefore("_") }.distinct()
        return classIds.associateWith { classId -> loadPhasesForClass(classId) }
    }

    private suspend fun loadPhasesForClass(classId: String): List<GearPhase> {
        val phases = mutableListOf<GearPhase>()
        // Cap at MAX_PHASES to avoid unnecessary 404 requests on the web target.
        // On Kotlin/Wasm, a failed HTTP fetch throws a Throwable that does not
        // extend Exception, crashing the coroutine if not caught as Throwable.
        for (phase in 1..MAX_PHASES) {
            val bytes = tryReadBytes("files/gear/gear_${classId}_phase${phase}.json") ?: break
            phases.add(parseGearPhase(bytes.decodeToString()))
        }
        return phases
    }

    private suspend fun tryReadBytes(path: String): ByteArray? = try {
        Res.readBytes(path)
    } catch (e: Throwable) {
        null
    }

    companion object {
        // Increase when Phase 3+ gear files are added.
        private const val MAX_PHASES = 2
    }
}
