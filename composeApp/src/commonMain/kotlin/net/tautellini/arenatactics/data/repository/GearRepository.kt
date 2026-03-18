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
        val classIds = listOf(comp.class1Id, comp.class2Id)
        return classIds.associateWith { classId -> loadPhasesForClass(classId) }
    }

    private suspend fun loadPhasesForClass(classId: String): List<GearPhase> {
        val phases = mutableListOf<GearPhase>()
        for (phase in 1..10) {
            val bytes = tryReadBytes("files/gear/gear_${classId}_phase${phase}.json") ?: break
            phases.add(parseGearPhase(bytes.decodeToString()))
        }
        return phases
    }

    private suspend fun tryReadBytes(path: String): ByteArray? = try {
        Res.readBytes(path)
    } catch (e: Exception) {
        null
    }
}
