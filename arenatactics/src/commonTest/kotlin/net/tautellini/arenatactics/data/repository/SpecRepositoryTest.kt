package net.tautellini.arenatactics.data.repository

import kotlinx.serialization.json.Json
import net.tautellini.models.arenatactics.SpecRole
import net.tautellini.models.arenatactics.WowSpec
import kotlin.test.Test
import kotlin.test.assertEquals

private val json = Json { ignoreUnknownKeys = true }

class SpecRepositoryTest {

    private val validJson = """
        [
          { "id": "rogue_subtlety", "name": "Subtlety", "classId": "rogue",
            "iconName": "ability_stealth", "role": "DPS" },
          { "id": "priest_discipline", "name": "Discipline", "classId": "priest",
            "iconName": "spell_holy_powerwordshield", "role": "HEALER" }
        ]
    """.trimIndent()

    @Test
    fun parsesSpecList() {
        val specs = json.decodeFromString<List<WowSpec>>(validJson)
        assertEquals(2, specs.size)
    }

    @Test
    fun parsesSpecFields() {
        val spec = json.decodeFromString<List<WowSpec>>(validJson).first { it.id == "rogue_subtlety" }
        assertEquals("Subtlety", spec.name)
        assertEquals("rogue", spec.classId)
        assertEquals("ability_stealth", spec.iconName)
        assertEquals(SpecRole.DPS, spec.role)
    }

    @Test
    fun parsesHealerRole() {
        val spec = json.decodeFromString<List<WowSpec>>(validJson).first { it.id == "priest_discipline" }
        assertEquals(SpecRole.HEALER, spec.role)
    }
}
