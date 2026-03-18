package net.tautellini.arenatactics.domain

import net.tautellini.arenatactics.data.model.Composition
import net.tautellini.arenatactics.data.model.WowClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompositionGeneratorTest {
    private val classes = listOf(
        WowClass("druid", "Druid", "#FF7D0A"),
        WowClass("mage", "Mage", "#69CCF0"),
        WowClass("rogue", "Rogue", "#FFF569")
    )
    private val whitelist = listOf(
        Composition("druid", "mage"),
        Composition("mage", "rogue")
    )

    @Test
    fun generatesOnlyWhitelistedCompositions() {
        val result = CompositionGenerator.generate(classes, whitelist)
        assertEquals(2, result.size)
    }

    @Test
    fun canonicalOrderEnforced() {
        val result = CompositionGenerator.generate(classes, whitelist)
        assertTrue(result.all { it.composition.class1Id <= it.composition.class2Id })
    }

    @Test
    fun enrichesWithClassObjects() {
        val result = CompositionGenerator.generate(classes, whitelist)
        val maRo = result.first { it.composition.id == "mage_rogue" }
        assertEquals("Mage", maRo.class1.name)
        assertEquals("Rogue", maRo.class2.name)
    }
}
