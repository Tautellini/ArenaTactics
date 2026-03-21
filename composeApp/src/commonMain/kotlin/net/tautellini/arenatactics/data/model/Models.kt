package net.tautellini.arenatactics.data.model

import kotlinx.serialization.Serializable

@Serializable
data class WowClass(
    val id: String,
    val name: String,
    val color: String,
    val iconName: String  // stored for future use; UI currently uses spec icon only
)

@Serializable
enum class SpecRole { DPS, HEALER }  // TANK omitted: TBC 2v2/3v3 arena has no tank role

@Serializable
data class WowSpec(
    val id: String,       // format: "{classId}_{specName}" e.g. "rogue_subtlety"
    val name: String,     // spec name only e.g. "Subtlety"
    val classId: String,
    val iconName: String, // Wowhead icon slug e.g. "ability_stealth"
    val role: SpecRole,
    val hasData: Boolean = true
)

@Serializable
enum class CompositionTier { DOMINANT, STRONG, PLAYABLE, OTHERS }

@Serializable
data class Composition(
    val specIds: List<String>,  // sorted; length == GameMode.teamSize
    val tier: CompositionTier,
    val hasData: Boolean
) {
    init {
        require(specIds == specIds.sorted()) { "specIds must be sorted: $specIds" }
    }
    // Lookup key only — never parse back into spec IDs (underscores are ambiguous)
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
    val enemySpecIds: List<String>,  // sorted; length == GameMode.teamSize
    val strategyMarkdown: String
)
