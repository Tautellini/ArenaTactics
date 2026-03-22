package net.tautellini.arenatactics.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LadderIndex(
    val region: String,
    val bracket: String,
    val file: String
)

@Serializable
data class LadderSnapshot(
    val region: String,
    val bracket: String,
    val seasonId: Int,
    val fetchedAt: String,
    val totalEntries: Int,
    val ratingCutoffs: Map<String, Int> = emptyMap(),
    val specDistribution: List<SpecDistribution> = emptyList(),
    val topEntries: List<LadderEntry> = emptyList()
)

@Serializable
data class SpecDistribution(
    val specId: String,
    val count: Int,
    val percentage: Double
)

@Serializable
data class LadderEntry(
    val rank: Int,
    val characterName: String,
    val realmSlug: String = "",
    val rating: Int,
    val wins: Int = 0,
    val losses: Int = 0,
    val classId: String? = null
)

/** Runtime-only — computed from top entries, not serialized. */
data class ClassDistributionEntry(
    val classId: String,
    val count: Int,
    val percentage: Double
)
