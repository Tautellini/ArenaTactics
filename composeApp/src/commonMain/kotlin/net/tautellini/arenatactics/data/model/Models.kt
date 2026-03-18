package net.tautellini.arenatactics.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GameMode(
    val id: String,
    val name: String,
    val description: String,
    val classPoolId: String,
    val compositionSetId: String
)

@Serializable
data class WowClass(
    val id: String,
    val name: String,
    val color: String
)

@Serializable
data class Composition(
    val class1Id: String,
    val class2Id: String
) {
    val id: String get() = "${class1Id}_${class2Id}"
}

@Serializable
data class GearItem(
    val wowheadId: Int,
    val name: String,
    val slot: String,
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
    val enemyClass1Id: String,
    val enemyClass2Id: String,
    val strategyMarkdown: String
)
