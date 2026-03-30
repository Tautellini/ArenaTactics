package net.tautellini.arenatactics.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import net.tautellini.arenatactics.ApiConfig
import net.tautellini.models.arenatactics.*

class ArenaApi(private val client: HttpClient = ApiClient.httpClient) {
    private val base = ApiConfig.ARENA_TACTICS

    suspend fun getAddons(): List<Addon> =
        client.get("$base/addons").body()

    suspend fun getAddon(addonId: String): Addon =
        client.get("$base/addons/$addonId").body()

    suspend fun getGameModes(addonId: String? = null): List<GameMode> {
        val url = if (addonId != null) "$base/game-modes?addonId=$addonId" else "$base/game-modes"
        return client.get(url).body()
    }

    suspend fun getSpecPool(specPoolId: String): List<WowSpec> =
        client.get("$base/spec-pools/$specPoolId").body()

    suspend fun getClassPool(classPoolId: String): List<WowClass> =
        client.get("$base/class-pools/$classPoolId").body()

    suspend fun getCompositionSet(compositionSetId: String): List<Composition> =
        client.get("$base/composition-sets/$compositionSetId").body()

    suspend fun getMatchups(compositionId: String): List<Matchup> =
        client.get("$base/matchups/$compositionId").body()

    suspend fun getMatchup(compositionId: String, matchupId: String): Matchup =
        client.get("$base/matchups/$compositionId/$matchupId").body()

    suspend fun getGearForClass(classId: String): List<GearPhase> =
        client.get("$base/gear/$classId").body()

    suspend fun getLadderIndex(addonId: String): List<LadderIndex> =
        client.get("$base/ladder/$addonId/index").body()

    suspend fun getLadderSnapshot(addonId: String, region: String, bracket: String): LadderSnapshot =
        client.get("$base/ladder/$addonId/snapshots/$region/$bracket").body()

    suspend fun getPlayerProfile(addonId: String, region: String, characterId: String): PlayerProfile =
        client.get("$base/ladder/$addonId/players/$region/$characterId").body()

    suspend fun getItem(addonId: String, itemId: Int): ItemTooltipData =
        client.get("$base/ladder/$addonId/items/$itemId").body()

    suspend fun getItemsBatch(addonId: String, itemIds: List<Int>): Map<String, ItemTooltipData> =
        client.get("$base/ladder/$addonId/items") {
            parameter("ids", itemIds.joinToString(","))
        }.body()

    suspend fun getSpecMeta(specId: String): SpecMeta =
        client.get("$base/spec-meta/$specId").body()
}
