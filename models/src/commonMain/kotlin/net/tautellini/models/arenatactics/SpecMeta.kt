package net.tautellini.models.arenatactics

import kotlinx.serialization.Serializable

@Serializable
data class SpecMeta(
    val specId: String,
    val sampleSize: Int,
    val slotBreakdowns: List<SlotBreakdown>,
    val popularTalentBuilds: List<TalentBuildEntry>
)

@Serializable
data class SlotBreakdown(
    val slot: String,
    val items: List<ItemUsage>,
    val enchants: List<EnchantUsage>
)

@Serializable
data class ItemUsage(
    val itemId: Int,
    val name: String,
    val quality: String?,
    val count: Int,
    val percentage: Double
)

@Serializable
data class EnchantUsage(
    val name: String,
    val count: Int,
    val percentage: Double
)

@Serializable
data class TalentBuildEntry(
    val label: String,
    val trees: List<TalentTreePair>,
    val count: Int,
    val percentage: Double,
    val talentSelections: List<TalentSelection> = emptyList()
)

@Serializable
data class TalentTreePair(
    val treeName: String,
    val spentPoints: Int
)
