package net.tautellini.arenatactics.data.repository

import arenatactics.composeapp.generated.resources.Res
import net.tautellini.arenatactics.data.model.Matchup

internal fun parseMatchups(jsonString: String): List<Matchup> =
    appJson.decodeFromString(jsonString)

class MatchupRepository {
    private val cache = mutableMapOf<String, Map<String, Matchup>>()

    suspend fun getForComposition(compositionId: String): List<Matchup> {
        return getCache(compositionId).values.toList()
    }

    suspend fun getById(compositionId: String, matchupId: String): Matchup? {
        return getCache(compositionId)[matchupId]
    }

    private suspend fun getCache(compositionId: String): Map<String, Matchup> {
        return cache.getOrPut(compositionId) {
            val bytes = Res.readBytes("files/matchups/matchups_$compositionId.json")
            val matchups = parseMatchups(bytes.decodeToString())
            matchups.associateBy { it.id }
        }
    }
}
