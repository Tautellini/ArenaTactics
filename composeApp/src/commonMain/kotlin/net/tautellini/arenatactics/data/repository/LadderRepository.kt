package net.tautellini.arenatactics.data.repository

import arenatactics.composeapp.generated.resources.Res
import net.tautellini.arenatactics.data.model.ItemTooltipData
import net.tautellini.arenatactics.data.model.LadderIndex
import net.tautellini.arenatactics.data.model.LadderSnapshot
import net.tautellini.arenatactics.data.model.PlayerProfile

class LadderRepository {
    private val snapshotCache = mutableMapOf<String, LadderSnapshot>()
    private val indexCache = mutableMapOf<String, List<LadderIndex>>()
    private val playerCache = mutableMapOf<String, Map<String, PlayerProfile>>()
    private val itemCache = mutableMapOf<String, Map<String, ItemTooltipData>>()

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

    suspend fun getSnapshot(addonId: String, region: String, bracket: String): LadderSnapshot {
        val key = "${addonId}/${region}_$bracket"
        return snapshotCache.getOrPut(key) {
            val bytes = Res.readBytes("files/ladder/$addonId/${region}_$bracket.json")
            appJson.decodeFromString(bytes.decodeToString())
        }
    }

    /** Load all player profiles for an addon/region. Keyed by characterId string. */
    suspend fun getPlayerProfiles(addonId: String, region: String): Map<String, PlayerProfile> {
        val key = "${addonId}/$region"
        return playerCache.getOrPut(key) {
            try {
                val bytes = Res.readBytes("files/ladder/$addonId/players_$region.json")
                appJson.decodeFromString(bytes.decodeToString())
            } catch (_: Throwable) {
                emptyMap()
            }
        }
    }

    suspend fun getPlayerProfile(addonId: String, region: String, characterId: String): PlayerProfile? {
        return getPlayerProfiles(addonId, region)[characterId]
    }

    /** Load item tooltip data for an addon/region. Keyed by itemId string. */
    suspend fun getItems(addonId: String, region: String): Map<String, ItemTooltipData> {
        val key = "${addonId}/$region"
        return itemCache.getOrPut(key) {
            try {
                val bytes = Res.readBytes("files/ladder/$addonId/items_$region.json")
                appJson.decodeFromString(bytes.decodeToString())
            } catch (_: Throwable) {
                emptyMap()
            }
        }
    }

    /** Get a single item's tooltip data, searching across regions. */
    suspend fun getItem(addonId: String, itemId: Int): ItemTooltipData? {
        for (region in listOf("us", "eu")) {
            val items = getItems(addonId, region)
            items[itemId.toString()]?.let { return it }
        }
        return null
    }
}
