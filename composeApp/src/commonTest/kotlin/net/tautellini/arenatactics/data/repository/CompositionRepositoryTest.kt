package net.tautellini.arenatactics.data.repository

import net.tautellini.arenatactics.data.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CompositionRepositoryTest {

    private val specsJson = """
        [
          { "id": "rogue_subtlety",    "name": "Subtlety",   "classId": "rogue",  "iconName": "ability_stealth",             "role": "DPS"    },
          { "id": "priest_discipline", "name": "Discipline", "classId": "priest", "iconName": "spell_holy_powerwordshield",  "role": "HEALER" }
        ]
    """.trimIndent()

    private val classesJson = """
        [
          { "id": "rogue",  "name": "Rogue",  "color": "#FFF569", "iconName": "classicon_rogue"  },
          { "id": "priest", "name": "Priest", "color": "#FFFFFF", "iconName": "classicon_priest" }
        ]
    """.trimIndent()

    private val compositionsJson = """
        [
          { "specIds": ["priest_discipline", "rogue_subtlety"], "tier": "DOMINANT", "hasData": true }
        ]
    """.trimIndent()

    @Test
    fun parseCompositionsSortsSpecIds() {
        val comps = parseCompositions(compositionsJson)
        assertEquals(listOf("priest_discipline", "rogue_subtlety"), comps.first().specIds)
    }

    @Test
    fun parseCompositionsReadsTierAndHasData() {
        val comp = parseCompositions(compositionsJson).first()
        assertEquals(CompositionTier.DOMINANT, comp.tier)
        assertEquals(true, comp.hasData)
    }

    @Test
    fun enrichReturnsRichCompositionWithCorrectSpecs() {
        val specs = parseWowSpecs(specsJson)
        val classes = parseWowClasses(classesJson)
        val comps = parseCompositions(compositionsJson)
        val specMap = specs.associateBy { it.id }
        val classMap = classes.associateBy { it.id }

        val rich = enrichCompositions(comps, specMap, classMap, teamSize = 2)

        assertEquals(1, rich.size)
        assertEquals("Discipline", rich.first().specs[0].name)
        assertEquals("Subtlety", rich.first().specs[1].name)
        assertEquals("Priest", rich.first().classes[0].name)
        assertEquals("Rogue", rich.first().classes[1].name)
    }

    @Test
    fun enrichThrowsOnUnknownSpecId() {
        val comps = parseCompositions("""
            [{ "specIds": ["unknown_spec", "rogue_subtlety"], "tier": "OTHERS", "hasData": false }]
        """.trimIndent())
        val specMap = parseWowSpecs(specsJson).associateBy { it.id }
        val classMap = parseWowClasses(classesJson).associateBy { it.id }

        assertFailsWith<IllegalStateException> {
            enrichCompositions(comps, specMap, classMap, teamSize = 2)
        }
    }

    @Test
    fun enrichThrowsOnTeamSizeMismatch() {
        val comps = parseCompositions(compositionsJson)  // has 2 specs
        val specMap = parseWowSpecs(specsJson).associateBy { it.id }
        val classMap = parseWowClasses(classesJson).associateBy { it.id }

        assertFailsWith<IllegalArgumentException> {
            enrichCompositions(comps, specMap, classMap, teamSize = 3)
        }
    }
}
