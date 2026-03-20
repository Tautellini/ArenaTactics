# Spec, Tiers & Availability — Design Spec
**Date:** 2026-03-20
**Scope:** TBC Anniversary 2v2 (first iteration); model must support 3v3, 5v5, and other addons without changes.

---

## Problem

The current data model operates at class granularity (`WowClass`, `Composition.class1Id/class2Id`). Arena compositions are always spec-specific (e.g. "Arms Warrior / Restoration Druid", not "Warrior / Druid"). The model also has no concept of composition tiers or data availability. The composition selection screen shows a flat list with no editorial grouping.

---

## Goals

1. Introduce `WowSpec` as the primary unit — specs know their parent class.
2. Add composition tiers: **Dominant, Strong, Playable, Others**.
3. Add an availability flag so comps without data can appear but be unselectable.
4. Show Wowhead spec icons on all composition cards.
5. Keep all models list-based so they work for 2v2, 3v3, and 5v5 without structural changes.
6. Enemy matchups are drawn exclusively from the same composition set.

---

## Data Model Changes

### `WowSpec` (new)
```kotlin
data class WowSpec(
    val id: String,       // format: "{classId}_{specName}" e.g. "rogue_subtlety"
    val name: String,     // spec name only e.g. "Subtlety"
    val classId: String,  // e.g. "rogue"
    val iconName: String, // Wowhead icon slug e.g. "ability_stealth"
    val role: SpecRole    // DPS or HEALER
)

enum class SpecRole { DPS, HEALER }
```

### `WowClass` (updated — adds `iconName`)
```kotlin
data class WowClass(
    val id: String,
    val name: String,
    val color: String,    // hex, used for spec badge colour
    val iconName: String  // stored for future use; UI currently uses spec icon only
)
```

### `Composition` (updated — replaces class IDs, adds tier + availability)
```kotlin
data class Composition(
    val specIds: List<String>,      // sorted; length == bracket teamSize
    val tier: CompositionTier,
    val hasData: Boolean
) {
    val id get() = specIds.sorted().joinToString("_")
}

enum class CompositionTier { DOMINANT, STRONG, PLAYABLE, OTHERS }
```

### `Matchup` (updated — enemy refs become spec IDs)
```kotlin
data class Matchup(
    val id: String,
    val enemySpecIds: List<String>, // sorted; length == bracket teamSize
    val strategyMarkdown: String
)
```

### `GameMode` (updated — adds `teamSize` and `specPoolId`)
```kotlin
data class GameMode(
    val id: String,
    val name: String,
    val description: String,
    val teamSize: Int,        // 2 for 2v2, 3 for 3v3, etc.
    val specPoolId: String,   // e.g. "tbc"
    val classPoolId: String,
    val compositionSetId: String
)
```

### `RichComposition` (updated — domain layer)
Wraps `Composition` with full `WowSpec` objects and their parent `WowClass` (for colour).

### `WowheadIcons` (new utility)
```kotlin
object WowheadIcons {
    private const val BASE = "https://wow.zamimg.com/images/wow/icons"
    fun medium(iconName: String) = "$BASE/medium/$iconName.jpg"
    fun large(iconName: String)  = "$BASE/large/$iconName.jpg"
}
```

---

## JSON File Changes

### New: `files/spec_pools/tbc.json`
One entry per TBC arena-relevant spec. No tank specs included.

```json
[
  { "id": "rogue_subtlety",     "name": "Subtlety",    "classId": "rogue",    "iconName": "ability_stealth",                   "role": "DPS"    },
  { "id": "mage_frost",         "name": "Frost",       "classId": "mage",     "iconName": "spell_frost_frostbolt02",           "role": "DPS"    },
  { "id": "warlock_affliction", "name": "Affliction",  "classId": "warlock",  "iconName": "spell_shadow_deathcoil",            "role": "DPS"    },
  { "id": "warrior_arms",       "name": "Arms",        "classId": "warrior",  "iconName": "ability_warrior_savageblow",        "role": "DPS"    },
  { "id": "hunter_marksmanship","name": "Marksmanship","classId": "hunter",   "iconName": "ability_hunter_focusedaim",         "role": "DPS"    },
  { "id": "druid_feral",        "name": "Feral",       "classId": "druid",    "iconName": "ability_druid_catform",             "role": "DPS"    },
  { "id": "paladin_retribution","name": "Retribution", "classId": "paladin",  "iconName": "spell_holy_auraoflight",            "role": "DPS"    },
  { "id": "shaman_enhancement", "name": "Enhancement", "classId": "shaman",   "iconName": "spell_shaman_improvedstormstrike",  "role": "DPS"    },
  { "id": "priest_shadow",      "name": "Shadow",      "classId": "priest",   "iconName": "spell_shadow_shadowwordpain",       "role": "DPS"    },
  { "id": "druid_balance",      "name": "Balance",     "classId": "druid",    "iconName": "spell_nature_starfall",             "role": "DPS"    },
  { "id": "priest_discipline",  "name": "Discipline",  "classId": "priest",   "iconName": "spell_holy_powerwordshield",        "role": "HEALER" },
  { "id": "druid_restoration",  "name": "Restoration", "classId": "druid",    "iconName": "spell_nature_healingtouch",         "role": "HEALER" },
  { "id": "shaman_restoration", "name": "Restoration", "classId": "shaman",   "iconName": "spell_nature_magicimmunity",        "role": "HEALER" },
  { "id": "paladin_holy",       "name": "Holy",        "classId": "paladin",  "iconName": "spell_holy_holybolt",               "role": "HEALER" }
]
```

### Updated: `files/class_pools/tbc.json`
Adds `iconName` to each class entry.

### Updated: `files/game_modes.json`
```json
[{
  "id": "tbc_anniversary_2v2",
  "name": "TBC Anniversary 2v2",
  "description": "World of Warcraft: The Burning Crusade Classic (Anniversary) — 2v2 Arena",
  "teamSize": 2,
  "specPoolId": "tbc",
  "classPoolId": "tbc",
  "compositionSetId": "tbc_2v2"
}]
```

### Updated: `files/composition_sets/tbc_2v2.json`
```json
[
  { "specIds": ["rogue_subtlety", "rogue_subtlety"],      "tier": "DOMINANT",  "hasData": true  },
  { "specIds": ["rogue_subtlety", "priest_discipline"],   "tier": "DOMINANT",  "hasData": true  },
  { "specIds": ["mage_frost", "rogue_subtlety"],          "tier": "DOMINANT",  "hasData": true  },
  { "specIds": ["druid_restoration", "rogue_subtlety"],   "tier": "DOMINANT",  "hasData": true  },
  { "specIds": ["warlock_affliction", "priest_discipline"],"tier": "DOMINANT", "hasData": true  },
  { "specIds": ["mage_frost", "priest_discipline"],       "tier": "DOMINANT",  "hasData": true  },
  { "specIds": ["warlock_affliction", "druid_restoration"],"tier": "DOMINANT", "hasData": true  },

  { "specIds": ["druid_restoration", "warrior_arms"],     "tier": "STRONG",    "hasData": false },
  { "specIds": ["druid_restoration", "hunter_marksmanship"],"tier": "STRONG",  "hasData": false },
  { "specIds": ["priest_discipline", "warrior_arms"],     "tier": "STRONG",    "hasData": false },
  { "specIds": ["druid_feral", "rogue_subtlety"],         "tier": "STRONG",    "hasData": false },
  { "specIds": ["hunter_marksmanship", "priest_discipline"],"tier": "STRONG",  "hasData": false },
  { "specIds": ["paladin_retribution", "shaman_restoration"],"tier": "STRONG", "hasData": false },

  { "specIds": ["rogue_subtlety", "warlock_affliction"],  "tier": "PLAYABLE",  "hasData": false },
  { "specIds": ["priest_shadow", "rogue_subtlety"],       "tier": "PLAYABLE",  "hasData": false },
  { "specIds": ["paladin_holy", "warrior_arms"],          "tier": "PLAYABLE",  "hasData": false },
  { "specIds": ["shaman_restoration", "warrior_arms"],    "tier": "PLAYABLE",  "hasData": false },
  { "specIds": ["paladin_holy", "warlock_affliction"],    "tier": "PLAYABLE",  "hasData": false },
  { "specIds": ["druid_balance", "rogue_subtlety"],       "tier": "PLAYABLE",  "hasData": false },
  { "specIds": ["mage_frost", "priest_shadow"],           "tier": "PLAYABLE",  "hasData": false },
  { "specIds": ["mage_frost", "paladin_holy"],            "tier": "PLAYABLE",  "hasData": false },
  { "specIds": ["druid_restoration", "shaman_enhancement"],"tier": "PLAYABLE", "hasData": false },
  { "specIds": ["paladin_retribution", "rogue_subtlety"], "tier": "PLAYABLE",  "hasData": false },

  // Others: all remaining valid pairs (no double-healer, no tanks) — hasData: false until curated
  { "specIds": ["mage_frost", "warlock_affliction"],           "tier": "OTHERS", "hasData": false },
  { "specIds": ["mage_frost", "warrior_arms"],                 "tier": "OTHERS", "hasData": false },
  { "specIds": ["mage_frost", "hunter_marksmanship"],          "tier": "OTHERS", "hasData": false },
  { "specIds": ["mage_frost", "druid_feral"],                  "tier": "OTHERS", "hasData": false },
  { "specIds": ["mage_frost", "paladin_retribution"],          "tier": "OTHERS", "hasData": false },
  { "specIds": ["mage_frost", "shaman_enhancement"],           "tier": "OTHERS", "hasData": false },
  { "specIds": ["mage_frost", "druid_balance"],                "tier": "OTHERS", "hasData": false },
  { "specIds": ["mage_frost", "shaman_restoration"],           "tier": "OTHERS", "hasData": false },
  { "specIds": ["mage_frost", "druid_restoration"],            "tier": "OTHERS", "hasData": false },
  { "specIds": ["warlock_affliction", "warrior_arms"],         "tier": "OTHERS", "hasData": false },
  { "specIds": ["warlock_affliction", "hunter_marksmanship"],  "tier": "OTHERS", "hasData": false },
  { "specIds": ["warlock_affliction", "druid_feral"],          "tier": "OTHERS", "hasData": false },
  { "specIds": ["warlock_affliction", "paladin_retribution"],  "tier": "OTHERS", "hasData": false },
  { "specIds": ["warlock_affliction", "shaman_enhancement"],   "tier": "OTHERS", "hasData": false },
  { "specIds": ["warlock_affliction", "priest_shadow"],        "tier": "OTHERS", "hasData": false },
  { "specIds": ["warlock_affliction", "druid_balance"],        "tier": "OTHERS", "hasData": false },
  { "specIds": ["warlock_affliction", "shaman_restoration"],   "tier": "OTHERS", "hasData": false },
  { "specIds": ["warlock_affliction", "paladin_holy"],         "tier": "OTHERS", "hasData": false },
  { "specIds": ["warrior_arms", "hunter_marksmanship"],        "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_feral", "warrior_arms"],                "tier": "OTHERS", "hasData": false },
  { "specIds": ["paladin_retribution", "warrior_arms"],        "tier": "OTHERS", "hasData": false },
  { "specIds": ["priest_shadow", "warrior_arms"],              "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_balance", "warrior_arms"],              "tier": "OTHERS", "hasData": false },
  { "specIds": ["shaman_enhancement", "warrior_arms"],         "tier": "OTHERS", "hasData": false },
  { "specIds": ["paladin_holy", "warrior_arms"],               "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_feral", "hunter_marksmanship"],         "tier": "OTHERS", "hasData": false },
  { "specIds": ["paladin_retribution", "hunter_marksmanship"], "tier": "OTHERS", "hasData": false },
  { "specIds": ["shaman_enhancement", "hunter_marksmanship"],  "tier": "OTHERS", "hasData": false },
  { "specIds": ["priest_shadow", "hunter_marksmanship"],       "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_balance", "hunter_marksmanship"],       "tier": "OTHERS", "hasData": false },
  { "specIds": ["paladin_holy", "hunter_marksmanship"],        "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_feral", "paladin_retribution"],         "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_feral", "shaman_enhancement"],          "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_feral", "priest_shadow"],               "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_feral", "druid_balance"],               "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_feral", "priest_discipline"],           "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_feral", "paladin_holy"],                "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_feral", "shaman_restoration"],          "tier": "OTHERS", "hasData": false },
  { "specIds": ["paladin_retribution", "shaman_enhancement"],  "tier": "OTHERS", "hasData": false },
  { "specIds": ["paladin_retribution", "priest_shadow"],       "tier": "OTHERS", "hasData": false },
  { "specIds": ["paladin_retribution", "druid_balance"],       "tier": "OTHERS", "hasData": false },
  { "specIds": ["paladin_retribution", "priest_discipline"],   "tier": "OTHERS", "hasData": false },
  { "specIds": ["paladin_retribution", "druid_restoration"],   "tier": "OTHERS", "hasData": false },
  { "specIds": ["shaman_enhancement", "priest_shadow"],        "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_balance", "shaman_enhancement"],        "tier": "OTHERS", "hasData": false },
  { "specIds": ["shaman_enhancement", "priest_discipline"],    "tier": "OTHERS", "hasData": false },
  { "specIds": ["shaman_enhancement", "paladin_holy"],         "tier": "OTHERS", "hasData": false },
  { "specIds": ["priest_shadow", "druid_balance"],             "tier": "OTHERS", "hasData": false },
  { "specIds": ["priest_shadow", "priest_discipline"],         "tier": "OTHERS", "hasData": false },
  { "specIds": ["priest_shadow", "paladin_holy"],              "tier": "OTHERS", "hasData": false },
  { "specIds": ["priest_shadow", "shaman_restoration"],        "tier": "OTHERS", "hasData": false },
  { "specIds": ["priest_shadow", "druid_restoration"],         "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_balance", "priest_discipline"],         "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_balance", "paladin_holy"],              "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_balance", "shaman_restoration"],        "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_balance", "druid_restoration"],         "tier": "OTHERS", "hasData": false },
  { "specIds": ["rogue_subtlety", "warrior_arms"],             "tier": "OTHERS", "hasData": false },
  { "specIds": ["rogue_subtlety", "hunter_marksmanship"],      "tier": "OTHERS", "hasData": false },
  { "specIds": ["rogue_subtlety", "shaman_enhancement"],       "tier": "OTHERS", "hasData": false },
  { "specIds": ["rogue_subtlety", "shaman_restoration"],       "tier": "OTHERS", "hasData": false },
  { "specIds": ["rogue_subtlety", "paladin_holy"],             "tier": "OTHERS", "hasData": false },
  { "specIds": ["mage_frost", "mage_frost"],                   "tier": "OTHERS", "hasData": false },
  { "specIds": ["warlock_affliction", "warlock_affliction"],   "tier": "OTHERS", "hasData": false },
  { "specIds": ["warrior_arms", "warrior_arms"],               "tier": "OTHERS", "hasData": false },
  { "specIds": ["hunter_marksmanship", "hunter_marksmanship"], "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_feral", "druid_feral"],                 "tier": "OTHERS", "hasData": false },
  { "specIds": ["paladin_retribution", "paladin_retribution"], "tier": "OTHERS", "hasData": false },
  { "specIds": ["shaman_enhancement", "shaman_enhancement"],   "tier": "OTHERS", "hasData": false },
  { "specIds": ["priest_shadow", "priest_shadow"],             "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_balance", "druid_balance"],             "tier": "OTHERS", "hasData": false }
]
```

### Matchup files
- Renamed from `matchups_{classId1}_{classId2}.json` to `matchups_{sortedSpecIds}.json`
  - e.g. `matchups_priest_discipline_rogue_subtlety.json`
- `enemyClass1Id`/`enemyClass2Id` replaced by `enemySpecIds: List<String>`
- Matchup IDs follow the same pattern: `{comp_id}_vs_{enemy_comp_id}`

---

## Repository Changes

### New: `SpecRepository`
Loads and caches `WowSpec` list from `files/spec_pools/{specPoolId}.json`.

### Updated: `CompositionRepository`
- Loads specs via `SpecRepository` in addition to classes
- Reads `specIds`, `tier`, `hasData` from composition set JSON
- Sorting is still enforced on load as a safety normalisation

### Updated: `MatchupRepository`
- File lookup uses spec-based composition ID
- Validates that all `enemySpecIds` in a matchup file correspond to a composition that exists in the current composition set; logs a warning and skips any that don't

### Updated: `GameModeRepository`
- Deserialises `teamSize` and `specPoolId` from game modes JSON

---

## UI Changes

### `CompositionSelectionScreen`
- Replaces flat list with four labelled sections: **Dominant → Strong → Playable → Others**
- Each section has a tier header
- Others section is collapsible
- Cards with `hasData: false` render with a greyed-out shimmer and are not clickable
- Each card shows two spec badges: spec icon (Wowhead medium) + spec name, coloured by class colour

### `CompositionCard`
- Updated to accept `RichComposition` with two `WowSpec` + `WowClass` pairs
- Loads icon via `WowheadIcons.medium(spec.iconName)` using the existing Coil setup

### `MatchupListScreen` / `MatchupDetailScreen`
- Enemy comp display updated from class names to spec names
- Icon display uses same spec badge component

---

## Constraints & Notes

- All `specIds` lists are sorted on write and re-normalised on read, same as the existing class ID convention.
- The `role` field on `WowSpec` is available for validation tooling and future filtering but is not used for any runtime generation of compositions.
- The existing `mage_rogue` matchup file and composition entry will be deleted and replaced with spec-based equivalents.
- `teamSize` on `GameMode` must equal `specIds.size` for all compositions in that game mode's composition set; repositories should assert this on load.
