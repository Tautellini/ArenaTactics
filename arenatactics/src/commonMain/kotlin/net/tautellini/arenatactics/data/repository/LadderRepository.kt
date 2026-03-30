package net.tautellini.arenatactics.data.repository

import net.tautellini.arenatactics.data.api.ArenaApi
import net.tautellini.models.arenatactics.ItemTooltipData
import net.tautellini.models.arenatactics.LadderIndex
import net.tautellini.models.arenatactics.LadderSnapshot
import net.tautellini.models.arenatactics.PlayerProfile
import net.tautellini.models.arenatactics.SpecMeta

class LadderRepository(private val api: ArenaApi) {
    private val snapshotCache = mutableMapOf<String, LadderSnapshot>()
    private val indexCache = mutableMapOf<String, List<LadderIndex>>()
    private val playerCache = mutableMapOf<String, PlayerProfile>()
    private val itemCache = mutableMapOf<String, ItemTooltipData>()
    private val specMetaCache = mutableMapOf<String, SpecMeta>()

    suspend fun getIndex(addonId: String): List<LadderIndex> {
        return indexCache.getOrPut(addonId) {
            try {
                api.getLadderIndex(addonId)
            } catch (_: Throwable) {
                emptyList()
            }
        }
    }

    suspend fun getSnapshot(addonId: String, region: String, bracket: String): LadderSnapshot {
        val key = "${addonId}/${region}_$bracket"
        return snapshotCache.getOrPut(key) {
            api.getLadderSnapshot(addonId, region, bracket)
        }
    }

    suspend fun getPlayerProfile(addonId: String, region: String, characterId: String): PlayerProfile? {
        val key = "${addonId}/${region}_$characterId"
        return playerCache.getOrPut(key) {
            try {
                api.getPlayerProfile(addonId, region, characterId)
            } catch (_: Throwable) {
                return null
            }
        }
    }

    suspend fun getItem(addonId: String, itemId: Int): ItemTooltipData? {
        val key = "${addonId}_$itemId"
        itemCache[key]?.let { return it }
        return try {
            api.getItem(addonId, itemId).also { itemCache[key] = it }
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun getItemsBatch(addonId: String, itemIds: List<Int>): Map<String, ItemTooltipData> {
        // Check cache first, only fetch missing items
        val result = mutableMapOf<String, ItemTooltipData>()
        val missing = mutableListOf<Int>()
        for (id in itemIds) {
            val cached = itemCache["${addonId}_$id"]
            if (cached != null) result[id.toString()] = cached else missing.add(id)
        }
        if (missing.isEmpty()) return result

        return try {
            val fetched = api.getItemsBatch(addonId, missing)
            fetched.forEach { (id, item) ->
                itemCache["${addonId}_$id"] = item
                result[id] = item
            }
            result
        } catch (_: Throwable) {
            result
        }
    }

    suspend fun getSpecMeta(specId: String): SpecMeta? {
        return specMetaCache.getOrPut(specId) {
            try {
                api.getSpecMeta(specId)
            } catch (_: Throwable) {
                return null
            }
        }
    }
}
