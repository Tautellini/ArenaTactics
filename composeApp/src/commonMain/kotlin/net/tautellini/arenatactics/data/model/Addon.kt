package net.tautellini.arenatactics.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Addon(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String,
    val specPoolId: String,
    val classPoolId: String,
    val hasData: Boolean
)
