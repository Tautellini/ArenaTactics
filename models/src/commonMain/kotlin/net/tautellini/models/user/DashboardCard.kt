package net.tautellini.models.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class DashboardCard {
    abstract val id: String

    @Serializable
    @SerialName("spec_meta_gear")
    data class SpecMetaGear(
        override val id: String,
        val addonId: String,
        val specId: String
    ) : DashboardCard()

    @Serializable
    @SerialName("spec_distribution")
    data class SpecDistribution(
        override val id: String,
        val addonId: String,
        val bracket: String,
        val region: String = "us"
    ) : DashboardCard()

    @Serializable
    @SerialName("top_players")
    data class TopPlayers(
        override val id: String,
        val addonId: String,
        val bracket: String,
        val region: String = "us"
    ) : DashboardCard()

    @Serializable
    @SerialName("composition_tier")
    data class CompositionTier(
        override val id: String,
        val addonId: String,
        val gameModeId: String
    ) : DashboardCard()

    @Serializable
    @SerialName("class_distribution")
    data class ClassDistribution(
        override val id: String,
        val addonId: String,
        val bracket: String,
        val region: String = "us"
    ) : DashboardCard()
}
