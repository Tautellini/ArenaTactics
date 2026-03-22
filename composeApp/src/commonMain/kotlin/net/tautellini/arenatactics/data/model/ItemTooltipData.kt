package net.tautellini.arenatactics.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ItemTooltipData(
    val itemId: Int,
    val name: String,
    val icon: String? = null,
    val quality: String? = null,
    val slotName: String? = null,
    val itemSubclass: String? = null,
    val binding: String? = null,
    val armor: Int? = null,
    val stats: List<String> = emptyList(),
    val spells: List<String> = emptyList(),
    val weaponDamage: String? = null,
    val weaponSpeed: String? = null,
    val weaponDps: String? = null,
    val setName: String? = null,
    val setEffects: List<String> = emptyList(),
    val requiredLevel: String? = null,
    val requiredClasses: String? = null
)
