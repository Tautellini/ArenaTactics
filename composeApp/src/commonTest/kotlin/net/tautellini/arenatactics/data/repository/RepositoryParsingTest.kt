package net.tautellini.arenatactics.data.repository

import kotlinx.serialization.json.Json
import net.tautellini.models.arenatactics.*
import kotlin.test.Test
import kotlin.test.assertEquals

private val json = Json { ignoreUnknownKeys = true }

class RepositoryParsingTest {

    @Test
    fun gameModeDeserializes() {
        val raw = """[{
            "id": "tbc_anniversary_2v2",
            "name": "TBC 2v2",
            "description": "desc",
            "teamSize": 2,
            "addonId": "tbc_anniversary",
            "compositionSetId": "tbc_2v2",
            "iconName": "achievement_arena_2v2_7",
            "hasData": true
        }]"""
        val result = json.decodeFromString<List<GameMode>>(raw)
        assertEquals(1, result.size)
        assertEquals("tbc_anniversary_2v2", result[0].id)
        assertEquals("tbc_anniversary", result[0].addonId)
        assertEquals(2, result[0].teamSize)
    }

    @Test
    fun wowClassDeserializes() {
        val raw = """[{"id":"rogue","name":"Rogue","color":"#FFF569","iconName":"classicon_rogue"}]"""
        val result = json.decodeFromString<List<WowClass>>(raw)
        assertEquals("Rogue", result[0].name)
        assertEquals("classicon_rogue", result[0].iconName)
    }

    @Test
    fun compositionCanonicalId() {
        val raw = """[{"specIds":["priest_discipline","rogue_subtlety"],"tier":"DOMINANT","hasData":true}]"""
        val result = json.decodeFromString<List<Composition>>(raw)
        assertEquals("priest_discipline_rogue_subtlety", result[0].id)
    }

    @Test
    fun gearPhaseDeserializes() {
        val raw = """{"phase":1,"classId":"rogue","items":[{"wowheadId":28210,"name":"Gladiator's Leather Helm","slot":"Head","icon":"inv_helmet_04","enchant":"Glyph of Ferocity","gems":["Relentless Earthstorm Diamond"]}]}"""
        val result = json.decodeFromString<GearPhase>(raw)
        assertEquals(1, result.phase)
        assertEquals(1, result.items.size)
        assertEquals(28210, result.items[0].wowheadId)
        assertEquals("inv_helmet_04", result.items[0].icon)
    }

    @Test
    fun addonDeserializes() {
        val raw = """[{
            "id": "tbc_anniversary",
            "name": "TBC Anniversary",
            "shortName": "TBC",
            "description": "The Burning Crusade Anniversary",
            "accentColor": "#4FC978",
            "specPoolId": "tbc",
            "classPoolId": "tbc",
            "hasData": true
        }]"""
        val result = json.decodeFromString<List<Addon>>(raw)
        assertEquals(1, result.size)
        assertEquals("tbc_anniversary", result[0].id)
        assertEquals("tbc", result[0].specPoolId)
        assertEquals("TBC", result[0].shortName)
    }

    @Test
    fun matchupDeserializes() {
        val raw = """[{
            "id": "mage_frost_rogue_subtlety_vs_druid_restoration_warrior_arms",
            "enemySpecIds": ["druid_restoration", "warrior_arms"],
            "strategyMarkdown": "## Kill Target\nWarrior"
        }]"""
        val result = json.decodeFromString<List<Matchup>>(raw)
        assertEquals("mage_frost_rogue_subtlety_vs_druid_restoration_warrior_arms", result[0].id)
        assertEquals(listOf("druid_restoration", "warrior_arms"), result[0].enemySpecIds)
    }
}
