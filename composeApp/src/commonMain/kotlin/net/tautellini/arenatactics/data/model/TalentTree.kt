package net.tautellini.arenatactics.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TalentTreeDefinition(
    val classId: String,
    val trees: List<TalentTreeData>
)

@Serializable
data class TalentTreeData(
    val name: String,
    val icon: String,
    val talents: List<TalentDefinition>
)

@Serializable
data class TalentDefinition(
    val id: Int,
    val name: String,
    val icon: String,
    val row: Int,
    val col: Int,
    val maxRank: Int,
    val prerequisiteId: Int? = null
)
