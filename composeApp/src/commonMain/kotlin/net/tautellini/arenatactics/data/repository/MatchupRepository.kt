package net.tautellini.arenatactics.data.repository

import net.tautellini.arenatactics.data.api.ArenaApi
import net.tautellini.models.arenatactics.Matchup

class MatchupRepository(private val api: ArenaApi) {
    private val cache = mutableMapOf<String, Map<String, Matchup>>()

    suspend fun getForComposition(compositionId: String): List<Matchup> {
        return getCache(compositionId).values.toList()
    }

    suspend fun getById(compositionId: String, matchupId: String): Matchup? {
        return getCache(compositionId)[matchupId]
    }

    private suspend fun getCache(compositionId: String): Map<String, Matchup> {
        return cache.getOrPut(compositionId) {
            val matchups = api.getMatchups(compositionId)
            matchups.associateBy { it.id }
        }
    }
}
