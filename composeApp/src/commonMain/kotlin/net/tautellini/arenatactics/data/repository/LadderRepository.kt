package net.tautellini.arenatactics.data.repository

import arenatactics.composeapp.generated.resources.Res
import net.tautellini.arenatactics.data.model.LadderIndex
import net.tautellini.arenatactics.data.model.LadderSnapshot

class LadderRepository {
    private val snapshotCache = mutableMapOf<String, LadderSnapshot>()
    private val indexCache = mutableMapOf<String, List<LadderIndex>>()

    /** Returns the list of available ladder snapshots for an addon. */
    suspend fun getIndex(addonId: String): List<LadderIndex> {
        return indexCache.getOrPut(addonId) {
            try {
                val bytes = Res.readBytes("files/ladder/$addonId/index.json")
                appJson.decodeFromString(bytes.decodeToString())
            } catch (_: Throwable) {
                emptyList()
            }
        }
    }

    /** Load a specific ladder snapshot by addon, region, and bracket. */
    suspend fun getSnapshot(addonId: String, region: String, bracket: String): LadderSnapshot {
        val key = "${addonId}/${region}_$bracket"
        return snapshotCache.getOrPut(key) {
            val bytes = Res.readBytes("files/ladder/$addonId/${region}_$bracket.json")
            appJson.decodeFromString(bytes.decodeToString())
        }
    }
}
