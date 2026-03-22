package net.tautellini.arenatactics.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayerProfile(
    val characterId: Long = 0,
    val name: String,
    val realmSlug: String = "",
    val classId: String? = null,
    val specId: String? = null,
    val race: String? = null,
    val guild: String? = null,
    val faction: String? = null,
    val level: Int? = null,
    val equipment: List<EquippedItem> = emptyList(),
    val talentGroups: List<TalentGroup> = emptyList(),
    val pvpBrackets: Map<String, PvpBracketRating> = emptyMap()
)

@Serializable
data class EquippedItem(
    val slot: String,
    val itemId: Int = 0,
    val name: String,
    val quality: String? = null,
    val enchant: String? = null,
    val gems: List<String> = emptyList()
)

@Serializable
data class TalentGroup(
    val isActive: Boolean,
    val specializations: List<TalentTreeSpec> = emptyList()
)

@Serializable
data class TalentTreeSpec(
    val treeName: String,
    val spentPoints: Int
)

@Serializable
data class PvpBracketRating(
    val rating: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0
)
