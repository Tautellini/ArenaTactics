package net.tautellini.arenatactics.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals

class RepositoryParsingTest {

    @Test
    fun gameModeDeserializes() {
        val json = """[{
            "id": "tbc_anniversary_2v2",
            "name": "TBC 2v2",
            "description": "desc",
            "teamSize": 2,
            "specPoolId": "tbc",
            "classPoolId": "tbc",
            "compositionSetId": "tbc_2v2",
            "iconName": "achievement_arena_2v2_7",
            "hasData": true
        }]"""
        val result = parseGameModes(json)
        assertEquals(1, result.size)
        assertEquals("tbc_anniversary_2v2", result[0].id)
        assertEquals(2, result[0].teamSize)
        assertEquals("tbc", result[0].specPoolId)
    }

    @Test
    fun wowClassDeserializes() {
        val json = """[{"id":"rogue","name":"Rogue","color":"#FFF569","iconName":"classicon_rogue"}]"""
        val result = parseWowClasses(json)
        assertEquals("Rogue", result[0].name)
        assertEquals("classicon_rogue", result[0].iconName)
    }

    @Test
    fun compositionCanonicalId() {
        // specIds are sorted on parse; verify the id is the sorted join
        val json = """[{"specIds":["rogue_subtlety","priest_discipline"],"tier":"DOMINANT","hasData":true}]"""
        val result = parseCompositions(json)
        assertEquals("priest_discipline_rogue_subtlety", result[0].id)
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
        val json = """[{
            "id": "mage_frost_rogue_subtlety_vs_druid_restoration_warrior_arms",
            "enemySpecIds": ["druid_restoration", "warrior_arms"],
            "strategyMarkdown": "## Kill Target\nWarrior"
        }]"""
        val result = parseMatchups(json)
        assertEquals("mage_frost_rogue_subtlety_vs_druid_restoration_warrior_arms", result[0].id)
        assertEquals(listOf("druid_restoration", "warrior_arms"), result[0].enemySpecIds)
    }
}
