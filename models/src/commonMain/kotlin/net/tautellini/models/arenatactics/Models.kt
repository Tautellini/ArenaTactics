package net.tautellini.models.arenatactics

import kotlinx.serialization.Serializable

@Serializable
data class WowClass(
    val id: String,
    val name: String,
    val color: String,
    val iconName: String
)

@Serializable
enum class SpecRole { DPS, HEALER }

@Serializable
data class WowSpec(
    val id: String,
    val name: String,
    val classId: String,
    val iconName: String,
    val role: SpecRole,
    val hasData: Boolean = true
)

@Serializable
enum class CompositionTier { DOMINANT, STRONG, PLAYABLE, OTHERS }

@Serializable
data class Composition(
    val specIds: List<String>,
    val tier: CompositionTier,
    val hasData: Boolean
) {
    init {
        require(specIds == specIds.sorted()) { "specIds must be sorted: $specIds" }
    }
    val id: String get() = specIds.joinToString("_")
}

@Serializable
data class GearItem(
    val wowheadId: Int,
    val name: String,
    val slot: String,
    val icon: String = "inv_misc_questionmark",
    val enchant: String? = null,
    val gems: List<String> = emptyList()
)

@Serializable
data class GearPhase(
    val phase: Int,
    val classId: String,
    val items: List<GearItem>
)

@Serializable
data class Matchup(
    val id: String,
    val enemySpecIds: List<String>,
    val strategyMarkdown: String
)
