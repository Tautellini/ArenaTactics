package net.tautellini.arenatactics.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals

class RepositoryParsingTest {
    @Test
    fun gameModeDeserializes() {
        val json = """[{"id":"tbc","name":"TBC 2v2","description":"desc","classPoolId":"tbc","compositionSetId":"tbc_2v2"}]"""
        val result = parseGameModes(json)
        assertEquals(1, result.size)
        assertEquals("tbc", result[0].id)
    }

    @Test
    fun wowClassDeserializes() {
        val json = """[{"id":"rogue","name":"Rogue","color":"#FFF569"}]"""
        val result = parseWowClasses(json)
        assertEquals("Rogue", result[0].name)
    }

    @Test
    fun compositionCanonicalId() {
        val json = """[{"class1Id":"mage","class2Id":"rogue"}]"""
        val result = parseCompositions(json)
        assertEquals("mage_rogue", result[0].id)
    }

    @Test
    fun gearPhaseDeserializes() {
        val json = """{"phase":1,"classId":"rogue","items":[{"wowheadId":28210,"name":"Gladiator's Leather Helm","slot":"Head","icon":"inv_helmet_04","enchant":"Glyph of Ferocity","gems":["Relentless Earthstorm Diamond"]}]}"""
        val result = parseGearPhase(json)
        assertEquals(1, result.phase)
        assertEquals(1, result.items.size)
        assertEquals(28210, result.items[0].wowheadId)
        assertEquals("inv_helmet_04", result.items[0].icon)
    }

    @Test
    fun matchupDeserializes() {
        val json = """[{"id":"mage_rogue_vs_druid_warrior","enemyClass1Id":"druid","enemyClass2Id":"warrior","strategyMarkdown":"## Kill Target\nWarrior"}]"""
        val result = parseMatchups(json)
        assertEquals("mage_rogue_vs_druid_warrior", result[0].id)
    }
}
