package net.tautellini.arenatactics.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Addon(
    val id: String,
    val name: String,
    val shortName: String,
    val description: String,
    val accentColor: String,
    val specPoolId: String,
    val classPoolId: String,
    val hasData: Boolean
)
