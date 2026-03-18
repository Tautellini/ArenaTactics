package net.tautellini.arenatactics.domain

import net.tautellini.arenatactics.data.model.Composition
import net.tautellini.arenatactics.data.model.WowClass

data class RichComposition(
    val composition: Composition,
    val class1: WowClass,
    val class2: WowClass
)

object CompositionGenerator {
    fun generate(classes: List<WowClass>, whitelist: List<Composition>): List<RichComposition> {
        val classMap = classes.associateBy { it.id }
        return whitelist.mapNotNull { comp ->
            val c1 = classMap[comp.class1Id] ?: return@mapNotNull null
            val c2 = classMap[comp.class2Id] ?: return@mapNotNull null
            RichComposition(comp, c1, c2)
        }
    }
}
