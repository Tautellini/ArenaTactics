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

All model types require `@Serializable`. The two new enums (`SpecRole`, `CompositionTier`) also need `@Serializable`. JSON string values are uppercase to match Kotlin's default enum serialisation (e.g. `"DPS"`, `"DOMINANT"`).

### `WowSpec` (new)
```kotlin
@Serializable
data class WowSpec(
    val id: String,       // format: "{classId}_{specName}" e.g. "rogue_subtlety"
    val name: String,     // spec name only e.g. "Subtlety"
    val classId: String,  // e.g. "rogue"
    val iconName: String, // Wowhead icon slug e.g. "ability_stealth"
    val role: SpecRole    // DPS or HEALER
)

@Serializable
enum class SpecRole { DPS, HEALER }
```

### `WowClass` (updated — adds `iconName`)
```kotlin
@Serializable
data class WowClass(
    val id: String,
    val name: String,
    val color: String,    // hex, used for spec badge colour
    val iconName: String  // stored for future use; UI currently uses spec icon only
)
```

### `Composition` (updated — replaces class IDs, adds tier + availability)
```kotlin
@Serializable
data class Composition(
    val specIds: List<String>,      // sorted; length == GameMode.teamSize
    val tier: CompositionTier,
    val hasData: Boolean
) {
    // Used as a lookup key only — never parsed back into spec IDs
    val id get() = specIds.sorted().joinToString("_")
}

@Serializable
enum class CompositionTier { DOMINANT, STRONG, PLAYABLE, OTHERS }
```

### `Matchup` (updated — enemy refs become spec IDs)
```kotlin
@Serializable
data class Matchup(
    val id: String,
    val enemySpecIds: List<String>, // sorted; length == GameMode.teamSize
    val strategyMarkdown: String
)
```

### `GameMode` (updated — adds `teamSize` and `specPoolId`)
```kotlin
@Serializable
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

### `RichComposition` (replaces existing — domain layer)
`CompositionGenerator` is **deleted**. Its responsibility moves to `CompositionRepository`, which enriches `Composition` into `RichComposition` directly after loading. `RichComposition` replaces `class1`/`class2: WowClass` with `spec1`/`spec2: WowSpec`, and exposes the parent `WowClass` of each spec for colour.

```kotlin
data class RichComposition(
    val composition: Composition,
    val spec1: WowSpec,
    val spec2: WowSpec,
    val class1: WowClass,   // spec1's parent, used for badge colour
    val class2: WowClass    // spec2's parent, used for badge colour
)
```

For 3v3 / 5v5, this model will need revisiting (a `List<Pair<WowSpec, WowClass>>` would be appropriate). For 2v2 the fixed pair is sufficient and simpler for the UI.

### `WowheadIcons` (new utility — `commonMain`)
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
Adds `iconName` to each class entry (stored for future use).

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
All 95 valid 2v2 pairs from the 14 TBC specs (no double-healer, mirrors allowed). Curated tiers are explicit; Others covers every remaining valid pair.

```json
[
  { "specIds": ["rogue_subtlety", "rogue_subtlety"],       "tier": "DOMINANT",  "hasData": true  },
  { "specIds": ["priest_discipline", "rogue_subtlety"],    "tier": "DOMINANT",  "hasData": true  },
  { "specIds": ["mage_frost", "rogue_subtlety"],           "tier": "DOMINANT",  "hasData": true  },
  { "specIds": ["druid_restoration", "rogue_subtlety"],    "tier": "DOMINANT",  "hasData": true  },
  { "specIds": ["priest_discipline", "warlock_affliction"],"tier": "DOMINANT",  "hasData": true  },
  { "specIds": ["mage_frost", "priest_discipline"],        "tier": "DOMINANT",  "hasData": true  },
  { "specIds": ["druid_restoration", "warlock_affliction"],"tier": "DOMINANT",  "hasData": true  },

  { "specIds": ["druid_restoration", "warrior_arms"],          "tier": "STRONG", "hasData": false },
  { "specIds": ["druid_restoration", "hunter_marksmanship"],   "tier": "STRONG", "hasData": false },
  { "specIds": ["priest_discipline", "warrior_arms"],          "tier": "STRONG", "hasData": false },
  { "specIds": ["druid_feral", "rogue_subtlety"],              "tier": "STRONG", "hasData": false },
  { "specIds": ["hunter_marksmanship", "priest_discipline"],   "tier": "STRONG", "hasData": false },
  { "specIds": ["paladin_retribution", "shaman_restoration"],  "tier": "STRONG", "hasData": false },

  { "specIds": ["rogue_subtlety", "warlock_affliction"],       "tier": "PLAYABLE", "hasData": false },
  { "specIds": ["priest_shadow", "rogue_subtlety"],            "tier": "PLAYABLE", "hasData": false },
  { "specIds": ["paladin_holy", "warrior_arms"],               "tier": "PLAYABLE", "hasData": false },
  { "specIds": ["shaman_restoration", "warrior_arms"],         "tier": "PLAYABLE", "hasData": false },
  { "specIds": ["paladin_holy", "warlock_affliction"],         "tier": "PLAYABLE", "hasData": false },
  { "specIds": ["druid_balance", "rogue_subtlety"],            "tier": "PLAYABLE", "hasData": false },
  { "specIds": ["mage_frost", "priest_shadow"],                "tier": "PLAYABLE", "hasData": false },
  { "specIds": ["mage_frost", "paladin_holy"],                 "tier": "PLAYABLE", "hasData": false },
  { "specIds": ["druid_restoration", "shaman_enhancement"],    "tier": "PLAYABLE", "hasData": false },
  { "specIds": ["paladin_retribution", "rogue_subtlety"],      "tier": "PLAYABLE", "hasData": false },

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
  { "specIds": ["hunter_marksmanship", "warlock_affliction"],  "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_feral", "warlock_affliction"],          "tier": "OTHERS", "hasData": false },
  { "specIds": ["paladin_retribution", "warlock_affliction"],  "tier": "OTHERS", "hasData": false },
  { "specIds": ["shaman_enhancement", "warlock_affliction"],   "tier": "OTHERS", "hasData": false },
  { "specIds": ["priest_shadow", "warlock_affliction"],        "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_balance", "warlock_affliction"],        "tier": "OTHERS", "hasData": false },
  { "specIds": ["shaman_restoration", "warlock_affliction"],   "tier": "OTHERS", "hasData": false },
  { "specIds": ["hunter_marksmanship", "warrior_arms"],        "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_feral", "warrior_arms"],                "tier": "OTHERS", "hasData": false },
  { "specIds": ["paladin_retribution", "warrior_arms"],        "tier": "OTHERS", "hasData": false },
  { "specIds": ["priest_shadow", "warrior_arms"],              "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_balance", "warrior_arms"],              "tier": "OTHERS", "hasData": false },
  { "specIds": ["shaman_enhancement", "warrior_arms"],         "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_feral", "hunter_marksmanship"],         "tier": "OTHERS", "hasData": false },
  { "specIds": ["hunter_marksmanship", "paladin_retribution"], "tier": "OTHERS", "hasData": false },
  { "specIds": ["hunter_marksmanship", "shaman_enhancement"],  "tier": "OTHERS", "hasData": false },
  { "specIds": ["hunter_marksmanship", "priest_shadow"],       "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_balance", "hunter_marksmanship"],       "tier": "OTHERS", "hasData": false },
  { "specIds": ["hunter_marksmanship", "paladin_holy"],        "tier": "OTHERS", "hasData": false },
  { "specIds": ["hunter_marksmanship", "shaman_restoration"],  "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_feral", "paladin_retribution"],         "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_feral", "shaman_enhancement"],          "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_feral", "priest_shadow"],               "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_balance", "druid_feral"],               "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_feral", "priest_discipline"],           "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_feral", "paladin_holy"],                "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_feral", "shaman_restoration"],          "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_feral", "druid_restoration"],           "tier": "OTHERS", "hasData": false },
  { "specIds": ["paladin_retribution", "shaman_enhancement"],  "tier": "OTHERS", "hasData": false },
  { "specIds": ["paladin_retribution", "priest_shadow"],       "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_balance", "paladin_retribution"],       "tier": "OTHERS", "hasData": false },
  { "specIds": ["paladin_retribution", "priest_discipline"],   "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_restoration", "paladin_retribution"],   "tier": "OTHERS", "hasData": false },
  { "specIds": ["paladin_holy", "paladin_retribution"],        "tier": "OTHERS", "hasData": false },
  { "specIds": ["shaman_enhancement", "priest_shadow"],        "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_balance", "shaman_enhancement"],        "tier": "OTHERS", "hasData": false },
  { "specIds": ["priest_discipline", "shaman_enhancement"],    "tier": "OTHERS", "hasData": false },
  { "specIds": ["paladin_holy", "shaman_enhancement"],         "tier": "OTHERS", "hasData": false },
  { "specIds": ["shaman_enhancement", "shaman_restoration"],   "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_balance", "priest_shadow"],             "tier": "OTHERS", "hasData": false },
  { "specIds": ["priest_discipline", "priest_shadow"],         "tier": "OTHERS", "hasData": false },
  { "specIds": ["paladin_holy", "priest_shadow"],              "tier": "OTHERS", "hasData": false },
  { "specIds": ["priest_shadow", "shaman_restoration"],        "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_restoration", "priest_shadow"],         "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_balance", "priest_discipline"],         "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_balance", "paladin_holy"],              "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_balance", "shaman_restoration"],        "tier": "OTHERS", "hasData": false },
  { "specIds": ["druid_balance", "druid_restoration"],         "tier": "OTHERS", "hasData": false },
  { "specIds": ["rogue_subtlety", "warrior_arms"],             "tier": "OTHERS", "hasData": false },
  { "specIds": ["hunter_marksmanship", "rogue_subtlety"],      "tier": "OTHERS", "hasData": false },
  { "specIds": ["rogue_subtlety", "shaman_enhancement"],       "tier": "OTHERS", "hasData": false },
  { "specIds": ["rogue_subtlety", "shaman_restoration"],       "tier": "OTHERS", "hasData": false },
  { "specIds": ["paladin_holy", "rogue_subtlety"],             "tier": "OTHERS", "hasData": false },
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

**Total: 95 entries** (7 Dominant + 6 Strong + 10 Playable + 72 Others). Covers all valid pairs from 14 specs with no double-healer combinations. Mirror comps beyond `rogue_subtlety/rogue_subtlety` are in Others.

### Matchup files
- Renamed from `matchups_{classId1}_{classId2}.json` to `matchups_{sortedSpecIds}.json`
  - e.g. `matchups_priest_discipline_rogue_subtlety.json`
- `enemyClass1Id`/`enemyClass2Id` replaced by `enemySpecIds: List<String>`
- Matchup IDs follow the same pattern: `{comp_id}_vs_{enemy_comp_id}`

---

## Repository Changes

### New: `SpecRepository`
Loads and caches `WowSpec` list from `files/spec_pools/{specPoolId}.json`.
Methods: `getSpecs(): List<WowSpec>`, `getById(id: String): WowSpec?`

### Updated: `CompositionRepository`
- Loads specs via `SpecRepository` in addition to classes
- Reads `specIds`, `tier`, `hasData` from composition set JSON
- Sorting enforced on load as a safety normalisation
- Enriches `Composition` → `RichComposition` by joining specs and their parent classes; replaces the deleted `CompositionGenerator`
- Asserts `specIds.size == gameMode.teamSize` for every entry; throws on violation
- If a `specId` in the composition set is not found in the spec pool (`getById` returns null): throw with a descriptive message — this is a data authoring error and should be loud, not silently skipped

### Updated: `MatchupRepository`
- File lookup uses spec-based composition ID
- When the matchup file does not exist: return empty list (normal for `hasData: false` compositions — not an error)
- When `hasData: true` and the file is missing: emit `MatchupListState.Error` (same mechanism used by existing repositories)
- Validates that all `enemySpecIds` correspond to a composition present in the current composition set; logs a warning and skips invalid entries

### Updated: `GameModeRepository`
- Deserialises `teamSize` and `specPoolId` from game modes JSON

---

## ViewModel & State Changes

### `CompositionSelectionViewModel`
`CompositionSelectionState.Success` changes from `List<RichComposition>` to a grouped structure:

```kotlin
data class Success(
    val grouped: Map<CompositionTier, List<RichComposition>>
)
```

The grouping is done in the ViewModel (not the composable), ordered by tier: DOMINANT → STRONG → PLAYABLE → OTHERS.

### `MatchupListViewModel`
`MatchupListState.Success` replaces `classMap: Map<String, WowClass>` with:
```kotlin
data class Success(
    val matchups: List<Matchup>,
    val specMap: Map<String, WowSpec>,
    val classMap: Map<String, WowClass>   // still needed for spec → class colour lookup
)
```

### `MatchupDetailViewModel`
Same change: carries both `specMap` and `classMap` in its `Success` state.

---

## UI Changes

### `CompositionSelectionScreen`
- Replaces flat list with four labelled sections: **Dominant → Strong → Playable → Others**
- Each section has a tier header
- Others section is collapsible, **collapsed by default** (72 entries)
- Cards with `hasData: false` render with a greyed-out shimmer and are **not clickable** — the `onClick` lambda is set to `null` / the card uses `enabled = false`; no navigation is triggered

### `CompositionCard`
- Updated to accept `RichComposition` with two `WowSpec` + `WowClass` pairs
- Loads icon via `WowheadIcons.medium(spec.iconName)` using the existing Coil setup
- Disabled state uses alpha + no ripple to produce the shimmer effect

### `MatchupListScreen` / `MatchupDetailScreen`
- Enemy comp display updated from class names to spec names using `specMap`
- Icon display uses same spec badge component

---

## Constraints & Notes

- All `specIds` lists are sorted on write and re-normalised on read, same as the existing class ID convention.
- `Composition.id` is a lookup key only — it must never be parsed back into spec IDs, as spec IDs themselves contain underscores making the join ambiguous.
- The `role` field on `WowSpec` is available for validation tooling but is not used for any runtime logic.
- The existing `mage_rogue` composition entry and `matchups_mage_rogue.json` file are deleted and replaced with spec-based equivalents.
- `CompositionGenerator` is deleted; its logic moves into `CompositionRepository`.
- For future 3v3/5v5 support, `RichComposition`'s fixed `spec1`/`spec2` pair will need to become a `List<Pair<WowSpec, WowClass>>`; this is a known limitation documented here so it is not forgotten.
