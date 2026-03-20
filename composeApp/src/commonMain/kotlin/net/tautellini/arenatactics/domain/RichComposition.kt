package net.tautellini.arenatactics.domain

import net.tautellini.arenatactics.data.model.Composition
import net.tautellini.arenatactics.data.model.WowClass
import net.tautellini.arenatactics.data.model.WowSpec

data class RichComposition(
    val composition: Composition,
    val spec1: WowSpec,
    val spec2: WowSpec,
    val class1: WowClass,  // spec1's parent class, used for badge colour
    val class2: WowClass   // spec2's parent class, used for badge colour
)
