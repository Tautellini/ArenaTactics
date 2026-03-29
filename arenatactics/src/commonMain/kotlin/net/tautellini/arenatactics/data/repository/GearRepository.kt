package net.tautellini.arenatactics.data.repository

import net.tautellini.arenatactics.data.api.ArenaApi
import net.tautellini.models.arenatactics.GearPhase

class GearRepository(private val api: ArenaApi) {
    suspend fun getGearForSpec(classId: String): List<GearPhase> {
        return try {
            api.getGearForClass(classId)
        } catch (_: Throwable) {
            emptyList()
        }
    }
}
