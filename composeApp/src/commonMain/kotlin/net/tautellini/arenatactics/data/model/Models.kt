package net.tautellini.arenatactics.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GameMode(
    val id: String,
    val name: String,
    val description: String,
    val teamSize: Int,
    val specPoolId: String,
    val classPoolId: String,
    val compositionSetId: String
)

@Serializable
data class WowClass(
    val id: String,
    val name: String,
    val color: String,
    val iconName: String  // stored for future use; UI currently uses spec icon only
)

@Serializable
enum class SpecRole { DPS, HEALER }

@Serializable
data class WowSpec(
    val id: String,       // format: "{classId}_{specName}" e.g. "rogue_subtlety"
    val name: String,     // spec name only e.g. "Subtlety"
    val classId: String,
    val iconName: String, // Wowhead icon slug e.g. "ability_stealth"
    val role: SpecRole
)

@Serializable
enum class CompositionTier { DOMINANT, STRONG, PLAYABLE, OTHERS }

@Serializable
data class Composition(
    val specIds: List<String>,  // sorted; length == GameMode.teamSize
    val tier: CompositionTier,
    val hasData: Boolean
) {
    // Lookup key only — never parse back into spec IDs (underscores are ambiguous)
    val id: String get() = specIds.sorted().joinToString("_")
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
    val enemySpecIds: List<String>,  // sorted; length == GameMode.teamSize
    val strategyMarkdown: String
)
