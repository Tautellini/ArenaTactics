package net.tautellini.arenatactics.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GameMode(
    val id: String,
    val name: String,
    val description: String,
    val teamSize: Int,
    val addonId: String,
    val specPoolId: String,
    val classPoolId: String,
    val compositionSetId: String,
    val iconName: String,
    val hasData: Boolean
)
