package net.tautellini.arenatactics.domain

import net.tautellini.arenatactics.data.model.Composition
import net.tautellini.arenatactics.data.model.WowClass
import net.tautellini.arenatactics.data.model.WowSpec

data class RichComposition(
    val composition: Composition,
    val specs: List<WowSpec>,    // parallel to composition.specIds; length == GameMode.teamSize
    val classes: List<WowClass>  // specs[i]'s parent class, used for badge colour
)
