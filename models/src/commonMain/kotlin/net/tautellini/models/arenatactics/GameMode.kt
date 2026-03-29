package net.tautellini.models.arenatactics

import kotlinx.serialization.Serializable

@Serializable
data class GameMode(
    val id: String,
    val name: String,
    val description: String,
    val teamSize: Int,
    val addonId: String,
    val compositionSetId: String,
    val iconName: String,
    val hasData: Boolean
)
