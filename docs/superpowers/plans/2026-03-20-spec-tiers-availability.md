# Spec, Tiers & Availability — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace class-level composition model with spec-level, add composition tiers and availability flag, and update the selection screen to show tier-grouped compositions with Wowhead spec icons.

**Architecture:** `WowSpec` becomes the primary arena unit (holds classId for colour lookup). `Composition` carries `specIds: List<String>`, `tier`, and `hasData`. `CompositionRepository` absorbs the deleted `CompositionGenerator` and enriches compositions into `RichComposition`. The selection UI groups by tier; `hasData: false` cards render as an unclickable grey shimmer. All spec-slot fields are `List<String>` to support future 3v3/5v5 brackets without model changes.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, kotlinx.serialization, Coil 3 (`io.coil-kt.coil3:coil-compose`), Wowhead icon CDN.

---

## File Map

| Action | Path |
|--------|------|
| Modify | `composeApp/src/commonMain/kotlin/.../data/model/Models.kt` |
| Create | `composeApp/src/commonMain/kotlin/.../data/model/WowheadIcons.kt` |
| Create | `composeApp/src/commonMain/kotlin/.../domain/RichComposition.kt` |
| **Delete** | `composeApp/src/commonMain/kotlin/.../domain/CompositionGenerator.kt` |
| **Delete** | `composeApp/src/commonTest/kotlin/.../domain/CompositionGeneratorTest.kt` |
| Create | `composeApp/src/commonMain/kotlin/.../data/repository/SpecRepository.kt` |
| Modify | `composeApp/src/commonMain/kotlin/.../data/repository/CompositionRepository.kt` |
| Modify | `composeApp/src/commonMain/kotlin/.../data/repository/GearRepository.kt` |
| Modify | `composeApp/src/commonTest/kotlin/.../data/repository/RepositoryParsingTest.kt` |
| (no change) | `composeApp/src/commonMain/kotlin/.../data/repository/MatchupRepository.kt` |
| (no change) | `composeApp/src/commonMain/kotlin/.../data/repository/GameModeRepository.kt` |
| Modify | `composeApp/src/commonMain/kotlin/.../presentation/CompositionSelectionViewModel.kt` |
| Modify | `composeApp/src/commonMain/kotlin/.../presentation/MatchupListViewModel.kt` |
| Modify | `composeApp/src/commonMain/kotlin/.../presentation/MatchupDetailViewModel.kt` |
| Create | `composeApp/src/commonMain/kotlin/.../presentation/screens/components/SpecBadge.kt` |
| Modify | `composeApp/src/commonMain/kotlin/.../presentation/screens/components/CompositionCard.kt` |
| Modify | `composeApp/src/commonMain/kotlin/.../presentation/screens/CompositionSelectionScreen.kt` |
| Modify | `composeApp/src/commonMain/kotlin/.../presentation/screens/MatchupListScreen.kt` |
| Modify | `composeApp/src/commonMain/kotlin/.../presentation/screens/MatchupDetailScreen.kt` |
| Modify | `composeApp/src/commonMain/kotlin/.../App.kt` |
| Create | `composeApp/src/commonTest/kotlin/.../data/repository/SpecRepositoryTest.kt` |
| Create | `composeApp/src/commonTest/kotlin/.../data/repository/CompositionRepositoryTest.kt` |
| Modify | `composeApp/src/commonMain/composeResources/files/class_pools/tbc.json` |
| Modify | `composeApp/src/commonMain/composeResources/files/game_modes.json` |
| Create | `composeApp/src/commonMain/composeResources/files/spec_pools/tbc.json` |
| Replace | `composeApp/src/commonMain/composeResources/files/composition_sets/tbc_2v2.json` |
| **Delete** | `composeApp/src/commonMain/composeResources/files/matchups/matchups_mage_rogue.json` |
| Create | `composeApp/src/commonMain/composeResources/files/matchups/matchups_mage_frost_rogue_subtlety.json` |
| Create | `composeApp/src/commonMain/composeResources/files/matchups/matchups_priest_discipline_rogue_subtlety.json` |
| Create | `composeApp/src/commonMain/composeResources/files/matchups/matchups_mage_frost_priest_discipline.json` |
| Create | `composeApp/src/commonMain/composeResources/files/matchups/matchups_druid_restoration_rogue_subtlety.json` |
| Create | `composeApp/src/commonMain/composeResources/files/matchups/matchups_priest_discipline_warlock_affliction.json` |
| Create | `composeApp/src/commonMain/composeResources/files/matchups/matchups_druid_restoration_warlock_affliction.json` |
| Create | `composeApp/src/commonMain/composeResources/files/matchups/matchups_rogue_subtlety_rogue_subtlety.json` |

> **Note:** The `...` in paths is `net/tautellini/arenatactics`. All paths are under `composeApp/src/`.

> **Compilation note:** After Task 1 changes `Models.kt`, the project will not compile until Tasks 4 and 4b (GearRepository + RepositoryParsingTest) are complete. Tasks 1–4b form an atomic migration unit. Run tests only after Task 4b.

---

### Task 1: Update data models

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/model/Models.kt`
- Create: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/model/WowheadIcons.kt`
- Create: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/domain/RichComposition.kt`
- Delete: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/domain/CompositionGenerator.kt`
- Delete: `composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/domain/CompositionGeneratorTest.kt`
- Create: `composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/data/model/WowheadIconsTest.kt`

- [ ] **Step 1: Write the WowheadIcons test first**

Create `composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/data/model/WowheadIconsTest.kt`:

```kotlin
package net.tautellini.arenatactics.data.model

import kotlin.test.Test
import kotlin.test.assertEquals

class WowheadIconsTest {
    @Test
    fun mediumUrlIsCorrect() {
        assertEquals(
            "https://wow.zamimg.com/images/wow/icons/medium/ability_stealth.jpg",
            WowheadIcons.medium("ability_stealth")
        )
    }

    @Test
    fun largeUrlIsCorrect() {
        assertEquals(
            "https://wow.zamimg.com/images/wow/icons/large/ability_stealth.jpg",
            WowheadIcons.large("ability_stealth")
        )
    }
}
```

- [ ] **Step 2: Replace Models.kt entirely**

```kotlin
package net.tautellini.arenatactics.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GameMode(
    val id: String,
    val name: String,
    val description: String,
    val teamSize: Int,
    val specPoolId: String,
    val classPoolId: String,
    val compositionSetId: String
)

@Serializable
data class WowClass(
    val id: String,
    val name: String,
    val color: String,
    val iconName: String  // stored for future use; UI currently uses spec icon only
)

@Serializable
enum class SpecRole { DPS, HEALER }

@Serializable
data class WowSpec(
    val id: String,       // format: "{classId}_{specName}" e.g. "rogue_subtlety"
    val name: String,     // spec name only e.g. "Subtlety"
    val classId: String,
    val iconName: String, // Wowhead icon slug e.g. "ability_stealth"
    val role: SpecRole
)

@Serializable
enum class CompositionTier { DOMINANT, STRONG, PLAYABLE, OTHERS }

@Serializable
data class Composition(
    val specIds: List<String>,  // sorted; length == GameMode.teamSize
    val tier: CompositionTier,
    val hasData: Boolean
) {
    // Lookup key only — never parse back into spec IDs (underscores are ambiguous)
    val id: String get() = specIds.sorted().joinToString("_")
}

@Serializable
data class GearItem(
    val wowheadId: Int,
    val name: String,
    val slot: String,
    val icon: String = "inv_misc_questionmark",
    val enchant: String? = null,
    val gems: List<String> = emptyList()
)

@Serializable
data class GearPhase(
    val phase: Int,
    val classId: String,
    val items: List<GearItem>
)

@Serializable
data class Matchup(
    val id: String,
    val enemySpecIds: List<String>,  // sorted; length == GameMode.teamSize
    val strategyMarkdown: String
)
```

- [ ] **Step 3: Create WowheadIcons.kt**

```kotlin
package net.tautellini.arenatactics.data.model

object WowheadIcons {
    private const val BASE = "https://wow.zamimg.com/images/wow/icons"
    fun medium(iconName: String) = "$BASE/medium/$iconName.jpg"
    fun large(iconName: String) = "$BASE/large/$iconName.jpg"
}
```

- [ ] **Step 4: Create RichComposition.kt**

```kotlin
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
```

- [ ] **Step 5: Delete CompositionGenerator.kt and CompositionGeneratorTest.kt**

Delete these two files. The generator's logic moves into `CompositionRepository` in Task 4.

- [ ] **Step 6: Commit (project will not compile yet — that is expected)**

```bash
git add -A
git commit -m "feat: update data models for spec/tier/availability (compilation restored in Task 4)"
```

---

### Task 2: Write JSON data files

**Files:**
- Modify: `composeApp/src/commonMain/composeResources/files/class_pools/tbc.json`
- Modify: `composeApp/src/commonMain/composeResources/files/game_modes.json`
- Create: `composeApp/src/commonMain/composeResources/files/spec_pools/tbc.json`
- Replace: `composeApp/src/commonMain/composeResources/files/composition_sets/tbc_2v2.json`
- Delete: `composeApp/src/commonMain/composeResources/files/matchups/matchups_mage_rogue.json`

- [ ] **Step 1: Update class_pools/tbc.json — add iconName to each entry**

```json
[
  { "id": "druid",   "name": "Druid",   "color": "#FF7D0A", "iconName": "classicon_druid"   },
  { "id": "hunter",  "name": "Hunter",  "color": "#ABD473", "iconName": "classicon_hunter"  },
  { "id": "mage",    "name": "Mage",    "color": "#69CCF0", "iconName": "classicon_mage"    },
  { "id": "paladin", "name": "Paladin", "color": "#F58CBA", "iconName": "classicon_paladin" },
  { "id": "priest",  "name": "Priest",  "color": "#FFFFFF", "iconName": "classicon_priest"  },
  { "id": "rogue",   "name": "Rogue",   "color": "#FFF569", "iconName": "classicon_rogue"   },
  { "id": "shaman",  "name": "Shaman",  "color": "#0070DE", "iconName": "classicon_shaman"  },
  { "id": "warlock", "name": "Warlock", "color": "#9482C9", "iconName": "classicon_warlock" },
  { "id": "warrior", "name": "Warrior", "color": "#C79C6E", "iconName": "classicon_warrior" }
]
```

- [ ] **Step 2: Update game_modes.json — add teamSize and specPoolId**

```json
[
  {
    "id": "tbc_anniversary_2v2",
    "name": "TBC Anniversary 2v2",
    "description": "World of Warcraft: The Burning Crusade Classic (Anniversary) — 2v2 Arena",
    "teamSize": 2,
    "specPoolId": "tbc",
    "classPoolId": "tbc",
    "compositionSetId": "tbc_2v2"
  }
]
```

- [ ] **Step 3: Create spec_pools/tbc.json**

Create directory `files/spec_pools/` and add `tbc.json`:

```json
[
  { "id": "rogue_subtlety",      "name": "Subtlety",    "classId": "rogue",    "iconName": "ability_stealth",                  "role": "DPS"    },
  { "id": "mage_frost",          "name": "Frost",       "classId": "mage",     "iconName": "spell_frost_frostbolt02",          "role": "DPS"    },
  { "id": "warlock_affliction",  "name": "Affliction",  "classId": "warlock",  "iconName": "spell_shadow_deathcoil",           "role": "DPS"    },
  { "id": "warrior_arms",        "name": "Arms",        "classId": "warrior",  "iconName": "ability_warrior_savageblow",       "role": "DPS"    },
  { "id": "hunter_marksmanship", "name": "Marksmanship","classId": "hunter",   "iconName": "ability_hunter_focusedaim",        "role": "DPS"    },
  { "id": "druid_feral",         "name": "Feral",       "classId": "druid",    "iconName": "ability_druid_catform",            "role": "DPS"    },
  { "id": "paladin_retribution", "name": "Retribution", "classId": "paladin",  "iconName": "spell_holy_auraoflight",           "role": "DPS"    },
  { "id": "shaman_enhancement",  "name": "Enhancement", "classId": "shaman",   "iconName": "spell_shaman_improvedstormstrike", "role": "DPS"    },
  { "id": "priest_shadow",       "name": "Shadow",      "classId": "priest",   "iconName": "spell_shadow_shadowwordpain",      "role": "DPS"    },
  { "id": "druid_balance",       "name": "Balance",     "classId": "druid",    "iconName": "spell_nature_starfall",            "role": "DPS"    },
  { "id": "priest_discipline",   "name": "Discipline",  "classId": "priest",   "iconName": "spell_holy_powerwordshield",       "role": "HEALER" },
  { "id": "druid_restoration",   "name": "Restoration", "classId": "druid",    "iconName": "spell_nature_healingtouch",        "role": "HEALER" },
  { "id": "shaman_restoration",  "name": "Restoration", "classId": "shaman",   "iconName": "spell_nature_magicimmunity",       "role": "HEALER" },
  { "id": "paladin_holy",        "name": "Holy",        "classId": "paladin",  "iconName": "spell_holy_holybolt",              "role": "HEALER" }
]
```

- [ ] **Step 4: Replace composition_sets/tbc_2v2.json**

```json
[
  { "specIds": ["rogue_subtlety", "rogue_subtlety"],        "tier": "DOMINANT",  "hasData": true  },
  { "specIds": ["priest_discipline", "rogue_subtlety"],     "tier": "DOMINANT",  "hasData": true  },
  { "specIds": ["mage_frost", "rogue_subtlety"],            "tier": "DOMINANT",  "hasData": true  },
  { "specIds": ["druid_restoration", "rogue_subtlety"],     "tier": "DOMINANT",  "hasData": true  },
  { "specIds": ["priest_discipline", "warlock_affliction"], "tier": "DOMINANT",  "hasData": true  },
  { "specIds": ["mage_frost", "priest_discipline"],         "tier": "DOMINANT",  "hasData": true  },
  { "specIds": ["druid_restoration", "warlock_affliction"], "tier": "DOMINANT",  "hasData": true  },

  { "specIds": ["druid_restoration", "warrior_arms"],           "tier": "STRONG",   "hasData": false },
  { "specIds": ["druid_restoration", "hunter_marksmanship"],    "tier": "STRONG",   "hasData": false },
  { "specIds": ["priest_discipline", "warrior_arms"],           "tier": "STRONG",   "hasData": false },
  { "specIds": ["druid_feral", "rogue_subtlety"],               "tier": "STRONG",   "hasData": false },
  { "specIds": ["hunter_marksmanship", "priest_discipline"],    "tier": "STRONG",   "hasData": false },
  { "specIds": ["paladin_retribution", "shaman_restoration"],   "tier": "STRONG",   "hasData": false },

  { "specIds": ["rogue_subtlety", "warlock_affliction"],        "tier": "PLAYABLE", "hasData": false },
  { "specIds": ["priest_shadow", "rogue_subtlety"],             "tier": "PLAYABLE", "hasData": false },
  { "specIds": ["paladin_holy", "warrior_arms"],                "tier": "PLAYABLE", "hasData": false },
  { "specIds": ["shaman_restoration", "warrior_arms"],          "tier": "PLAYABLE", "hasData": false },
  { "specIds": ["paladin_holy", "warlock_affliction"],          "tier": "PLAYABLE", "hasData": false },
  { "specIds": ["druid_balance", "rogue_subtlety"],             "tier": "PLAYABLE", "hasData": false },
  { "specIds": ["mage_frost", "priest_shadow"],                 "tier": "PLAYABLE", "hasData": false },
  { "specIds": ["mage_frost", "paladin_holy"],                  "tier": "PLAYABLE", "hasData": false },
  { "specIds": ["druid_restoration", "shaman_enhancement"],     "tier": "PLAYABLE", "hasData": false },
  { "specIds": ["paladin_retribution", "rogue_subtlety"],       "tier": "PLAYABLE", "hasData": false },

  { "specIds": ["mage_frost", "warlock_affliction"],            "tier": "OTHERS",   "hasData": false },
  { "specIds": ["mage_frost", "warrior_arms"],                  "tier": "OTHERS",   "hasData": false },
  { "specIds": ["mage_frost", "hunter_marksmanship"],           "tier": "OTHERS",   "hasData": false },
  { "specIds": ["mage_frost", "druid_feral"],                   "tier": "OTHERS",   "hasData": false },
  { "specIds": ["mage_frost", "paladin_retribution"],           "tier": "OTHERS",   "hasData": false },
  { "specIds": ["mage_frost", "shaman_enhancement"],            "tier": "OTHERS",   "hasData": false },
  { "specIds": ["mage_frost", "druid_balance"],                 "tier": "OTHERS",   "hasData": false },
  { "specIds": ["mage_frost", "shaman_restoration"],            "tier": "OTHERS",   "hasData": false },
  { "specIds": ["mage_frost", "druid_restoration"],             "tier": "OTHERS",   "hasData": false },
  { "specIds": ["warlock_affliction", "warrior_arms"],          "tier": "OTHERS",   "hasData": false },
  { "specIds": ["hunter_marksmanship", "warlock_affliction"],   "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_feral", "warlock_affliction"],           "tier": "OTHERS",   "hasData": false },
  { "specIds": ["paladin_retribution", "warlock_affliction"],   "tier": "OTHERS",   "hasData": false },
  { "specIds": ["shaman_enhancement", "warlock_affliction"],    "tier": "OTHERS",   "hasData": false },
  { "specIds": ["priest_shadow", "warlock_affliction"],         "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_balance", "warlock_affliction"],         "tier": "OTHERS",   "hasData": false },
  { "specIds": ["shaman_restoration", "warlock_affliction"],    "tier": "OTHERS",   "hasData": false },
  { "specIds": ["hunter_marksmanship", "warrior_arms"],         "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_feral", "warrior_arms"],                 "tier": "OTHERS",   "hasData": false },
  { "specIds": ["paladin_retribution", "warrior_arms"],         "tier": "OTHERS",   "hasData": false },
  { "specIds": ["priest_shadow", "warrior_arms"],               "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_balance", "warrior_arms"],               "tier": "OTHERS",   "hasData": false },
  { "specIds": ["shaman_enhancement", "warrior_arms"],          "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_feral", "hunter_marksmanship"],          "tier": "OTHERS",   "hasData": false },
  { "specIds": ["hunter_marksmanship", "paladin_retribution"],  "tier": "OTHERS",   "hasData": false },
  { "specIds": ["hunter_marksmanship", "shaman_enhancement"],   "tier": "OTHERS",   "hasData": false },
  { "specIds": ["hunter_marksmanship", "priest_shadow"],        "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_balance", "hunter_marksmanship"],        "tier": "OTHERS",   "hasData": false },
  { "specIds": ["hunter_marksmanship", "paladin_holy"],         "tier": "OTHERS",   "hasData": false },
  { "specIds": ["hunter_marksmanship", "shaman_restoration"],   "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_feral", "paladin_retribution"],          "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_feral", "shaman_enhancement"],           "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_feral", "priest_shadow"],                "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_balance", "druid_feral"],                "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_feral", "priest_discipline"],            "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_feral", "paladin_holy"],                 "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_feral", "shaman_restoration"],           "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_feral", "druid_restoration"],            "tier": "OTHERS",   "hasData": false },
  { "specIds": ["paladin_retribution", "shaman_enhancement"],   "tier": "OTHERS",   "hasData": false },
  { "specIds": ["paladin_retribution", "priest_shadow"],        "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_balance", "paladin_retribution"],        "tier": "OTHERS",   "hasData": false },
  { "specIds": ["paladin_retribution", "priest_discipline"],    "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_restoration", "paladin_retribution"],    "tier": "OTHERS",   "hasData": false },
  { "specIds": ["paladin_holy", "paladin_retribution"],         "tier": "OTHERS",   "hasData": false },
  { "specIds": ["shaman_enhancement", "priest_shadow"],         "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_balance", "shaman_enhancement"],         "tier": "OTHERS",   "hasData": false },
  { "specIds": ["priest_discipline", "shaman_enhancement"],     "tier": "OTHERS",   "hasData": false },
  { "specIds": ["paladin_holy", "shaman_enhancement"],          "tier": "OTHERS",   "hasData": false },
  { "specIds": ["shaman_enhancement", "shaman_restoration"],    "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_balance", "priest_shadow"],              "tier": "OTHERS",   "hasData": false },
  { "specIds": ["priest_discipline", "priest_shadow"],          "tier": "OTHERS",   "hasData": false },
  { "specIds": ["paladin_holy", "priest_shadow"],               "tier": "OTHERS",   "hasData": false },
  { "specIds": ["priest_shadow", "shaman_restoration"],         "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_restoration", "priest_shadow"],          "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_balance", "priest_discipline"],          "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_balance", "paladin_holy"],               "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_balance", "shaman_restoration"],         "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_balance", "druid_restoration"],          "tier": "OTHERS",   "hasData": false },
  { "specIds": ["rogue_subtlety", "warrior_arms"],              "tier": "OTHERS",   "hasData": false },
  { "specIds": ["hunter_marksmanship", "rogue_subtlety"],       "tier": "OTHERS",   "hasData": false },
  { "specIds": ["rogue_subtlety", "shaman_enhancement"],        "tier": "OTHERS",   "hasData": false },
  { "specIds": ["rogue_subtlety", "shaman_restoration"],        "tier": "OTHERS",   "hasData": false },
  { "specIds": ["paladin_holy", "rogue_subtlety"],              "tier": "OTHERS",   "hasData": false },
  { "specIds": ["mage_frost", "mage_frost"],                    "tier": "OTHERS",   "hasData": false },
  { "specIds": ["warlock_affliction", "warlock_affliction"],    "tier": "OTHERS",   "hasData": false },
  { "specIds": ["warrior_arms", "warrior_arms"],                "tier": "OTHERS",   "hasData": false },
  { "specIds": ["hunter_marksmanship", "hunter_marksmanship"],  "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_feral", "druid_feral"],                  "tier": "OTHERS",   "hasData": false },
  { "specIds": ["paladin_retribution", "paladin_retribution"],  "tier": "OTHERS",   "hasData": false },
  { "specIds": ["shaman_enhancement", "shaman_enhancement"],    "tier": "OTHERS",   "hasData": false },
  { "specIds": ["priest_shadow", "priest_shadow"],              "tier": "OTHERS",   "hasData": false },
  { "specIds": ["druid_balance", "druid_balance"],              "tier": "OTHERS",   "hasData": false }
]
```

- [ ] **Step 5: Delete matchups/matchups_mage_rogue.json**

This file will be replaced by spec-based matchup files in Task 11.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: add spec pool, update class pool and game modes, replace composition set"
```

---

### Task 3: Implement SpecRepository

**Files:**
- Create: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/repository/SpecRepository.kt`
- Create: `composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/data/repository/SpecRepositoryTest.kt`

- [ ] **Step 1: Write the failing test**

Create `SpecRepositoryTest.kt`. We test the pure parse function (same pattern as existing `parseWowClasses`):

```kotlin
package net.tautellini.arenatactics.data.repository

import net.tautellini.arenatactics.data.model.SpecRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
        val specs = parseWowSpecs(validJson)
        assertEquals(2, specs.size)
    }

    @Test
    fun parsesSpecFields() {
        val spec = parseWowSpecs(validJson).first { it.id == "rogue_subtlety" }
        assertEquals("Subtlety", spec.name)
        assertEquals("rogue", spec.classId)
        assertEquals("ability_stealth", spec.iconName)
        assertEquals(SpecRole.DPS, spec.role)
    }

    @Test
    fun parsesHealerRole() {
        val spec = parseWowSpecs(validJson).first { it.id == "priest_discipline" }
        assertEquals(SpecRole.HEALER, spec.role)
    }
}
```

- [ ] **Step 2: Run the test — verify it fails with "unresolved reference: parseWowSpecs"**

```bash
gradlew.bat :composeApp:jvmTest --tests "net.tautellini.arenatactics.data.repository.SpecRepositoryTest"
```

Expected: compilation error — `parseWowSpecs` not defined yet.

- [ ] **Step 3: Create SpecRepository.kt**

```kotlin
package net.tautellini.arenatactics.data.repository

import arenatactics.composeapp.generated.resources.Res
import net.tautellini.arenatactics.data.model.WowSpec

internal fun parseWowSpecs(jsonString: String): List<WowSpec> =
    appJson.decodeFromString(jsonString)

class SpecRepository {
    private val cache = mutableMapOf<String, List<WowSpec>>()

    suspend fun getSpecs(specPoolId: String): List<WowSpec> {
        return cache.getOrPut(specPoolId) {
            val bytes = Res.readBytes("files/spec_pools/$specPoolId.json")
            parseWowSpecs(bytes.decodeToString())
        }
    }

    suspend fun getById(specPoolId: String, id: String): WowSpec? =
        getSpecs(specPoolId).find { it.id == id }
}
```

- [ ] **Step 4: Run tests — verify they pass**

```bash
gradlew.bat :composeApp:jvmTest --tests "net.tautellini.arenatactics.data.repository.SpecRepositoryTest"
```

Expected: 3 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add SpecRepository with parsing tests"
```

---

### Task 4: Update CompositionRepository — spec enrichment replaces CompositionGenerator

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/repository/CompositionRepository.kt`
- Create: `composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/data/repository/CompositionRepositoryTest.kt`

> After this task, the project should compile again.

- [ ] **Step 1: Write the failing tests**

```kotlin
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
        assertEquals("Discipline", rich.first().spec1.name)
        assertEquals("Subtlety", rich.first().spec2.name)
        assertEquals("Priest", rich.first().class1.name)
        assertEquals("Rogue", rich.first().class2.name)
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
```

- [ ] **Step 2: Run tests — expect compilation failure**

```bash
gradlew.bat :composeApp:jvmTest --tests "net.tautellini.arenatactics.data.repository.CompositionRepositoryTest"
```

Expected: compile error — `enrichCompositions` not defined, `parseCompositions` signature mismatch.

- [ ] **Step 3: Replace CompositionRepository.kt**

```kotlin
package net.tautellini.arenatactics.data.repository

import arenatactics.composeapp.generated.resources.Res
import net.tautellini.arenatactics.data.model.Composition
import net.tautellini.arenatactics.data.model.WowClass
import net.tautellini.arenatactics.data.model.WowSpec
import net.tautellini.arenatactics.domain.RichComposition

internal fun parseWowClasses(jsonString: String): List<WowClass> =
    appJson.decodeFromString(jsonString)

internal fun parseCompositions(jsonString: String): List<Composition> {
    val raw: List<Composition> = appJson.decodeFromString(jsonString)
    // Normalise spec order on load — defensive against unsorted JSON
    return raw.map { comp -> comp.copy(specIds = comp.specIds.sorted()) }
}

internal fun enrichCompositions(
    compositions: List<Composition>,
    specMap: Map<String, WowSpec>,
    classMap: Map<String, WowClass>,
    teamSize: Int
): List<RichComposition> {
    return compositions.map { comp ->
        require(comp.specIds.size == teamSize) {
            "Composition '${comp.id}' has ${comp.specIds.size} specs but teamSize is $teamSize"
        }
        val spec1 = specMap[comp.specIds[0]]
            ?: error("Unknown specId '${comp.specIds[0]}' — not in spec pool")
        val spec2 = specMap[comp.specIds[1]]
            ?: error("Unknown specId '${comp.specIds[1]}' — not in spec pool")
        val class1 = classMap[spec1.classId]
            ?: error("Unknown classId '${spec1.classId}' for spec '${spec1.id}'")
        val class2 = classMap[spec2.classId]
            ?: error("Unknown classId '${spec2.classId}' for spec '${spec2.id}'")
        RichComposition(comp, spec1, spec2, class1, class2)
    }
}

class CompositionRepository(
    private val specRepository: SpecRepository
) {
    private val classCache = mutableMapOf<String, List<WowClass>>()
    private val compCache  = mutableMapOf<String, List<Composition>>()

    suspend fun getClasses(classPoolId: String): List<WowClass> {
        return classCache.getOrPut(classPoolId) {
            val bytes = Res.readBytes("files/class_pools/$classPoolId.json")
            parseWowClasses(bytes.decodeToString())
        }
    }

    suspend fun getSpecs(specPoolId: String): List<WowSpec> =
        specRepository.getSpecs(specPoolId)

    suspend fun getCompositions(compositionSetId: String): List<Composition> {
        return compCache.getOrPut(compositionSetId) {
            val bytes = Res.readBytes("files/composition_sets/$compositionSetId.json")
            parseCompositions(bytes.decodeToString())
        }
    }

    suspend fun getRichCompositions(
        specPoolId: String,
        classPoolId: String,
        compositionSetId: String,
        teamSize: Int
    ): List<RichComposition> {
        val specs    = specRepository.getSpecs(specPoolId)
        val specMap  = specs.associateBy { it.id }
        val classes  = getClasses(classPoolId)
        val classMap = classes.associateBy { it.id }
        val comps    = getCompositions(compositionSetId)
        return enrichCompositions(comps, specMap, classMap, teamSize)
    }

    suspend fun getById(compositionId: String, compositionSetId: String): Composition? =
        getCompositions(compositionSetId).find { it.id == compositionId }
}
```

- [ ] **Step 4: Run the CompositionRepository tests — verify they pass**

```bash
gradlew.bat :composeApp:jvmTest --tests "net.tautellini.arenatactics.data.repository.CompositionRepositoryTest"
```

Expected: 5 tests PASS.

- [ ] **Step 5: Commit (project still won't compile until Task 4b fixes GearRepository and RepositoryParsingTest)**

```bash
git add -A
git commit -m "feat: CompositionRepository absorbs CompositionGenerator, adds spec enrichment"
```

---

### Task 4b: Fix GearRepository and update RepositoryParsingTest

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/repository/GearRepository.kt`
- Modify: `composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/data/repository/RepositoryParsingTest.kt`

> After this task, the project compiles and all tests should pass.

- [ ] **Step 1: Fix GearRepository.kt — replace class1Id/class2Id with specIds**

`Composition` no longer has `class1Id`/`class2Id`. Derive class IDs from spec IDs. Spec IDs follow the documented format `{classId}_{specName}` where class IDs are always single words (no underscores), so `substringBefore("_")` is correct and safe.

Change line 15 only:

```kotlin
// Before:
val classIds = listOf(comp.class1Id, comp.class2Id)

// After:
// Spec IDs follow {classId}_{specName} format (e.g. "rogue_subtlety" → "rogue").
// Class IDs never contain underscores, so substringBefore("_") is always correct.
val classIds = comp.specIds.map { it.substringBefore("_") }.distinct()
```

> `.distinct()` handles mirror comps (e.g. `rogue_subtlety / rogue_subtlety`) so gear is only loaded once per class.

- [ ] **Step 2: Update RepositoryParsingTest.kt — all four tests use old model fields**

Replace the entire file:

```kotlin
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
            "compositionSetId": "tbc_2v2"
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
```

- [ ] **Step 3: Run all tests — verify everything passes**

```bash
gradlew.bat :composeApp:allTests
```

Expected: all tests PASS. (App.kt still won't compile until Task 10 — that's fine for test runs which use `jvmTest`.)

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "fix: update GearRepository and RepositoryParsingTest for new Composition model"
```

---

### Task 5: Verify MatchupRepository and GameModeRepository require no code changes

**Files:** None to modify.

`MatchupRepository` deserialises `Matchup` via `appJson.decodeFromString` — the model change in Task 1 is sufficient. No code changes needed.

**Missing-file behaviour:** If `hasData: false` cards are correctly disabled in the UI (Task 8), `MatchupListViewModel` is never invoked for those compositions, so `MatchupRepository` never attempts to load their (absent) files. For `hasData: true` comps, if the file is missing, `Res.readBytes` throws, which is caught by the ViewModel's `catch (e: Throwable)` and emits `MatchupListState.Error`. This matches the spec requirement. No code change needed.

`GameModeRepository` calls `parseGameModes` which deserialises via the updated `GameMode` model — the new required fields (`teamSize`, `specPoolId`) are provided by the updated JSON in Task 2. No code change needed.

- [ ] **Step 1: Confirm by inspection that MatchupRepository.kt and GameModeRepository.kt have no references to removed model fields**

Open each file. Confirm:
- `MatchupRepository.kt` references only `Matchup` (no `enemyClass1Id`/`enemyClass2Id`)
- `GameModeRepository.kt` references only `GameMode` (fields accessed by name nowhere in the file)

Both files only call `appJson.decodeFromString` and store/return the result — no field access. No changes needed.

- [ ] **Step 2: Commit (empty — just a checkpoint)**

```bash
git commit --allow-empty -m "chore: confirm MatchupRepository and GameModeRepository need no changes"
```

---

### Task 6: Update ViewModels

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/CompositionSelectionViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/MatchupListViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/MatchupDetailViewModel.kt`

- [ ] **Step 1: Replace CompositionSelectionViewModel.kt**

`Success` now carries `Map<CompositionTier, List<RichComposition>>` grouped in tier order. The `CompositionRepository` now takes a `SpecRepository` param (wired in App.kt in Task 9).

```kotlin
package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.CompositionTier
import net.tautellini.arenatactics.data.repository.CompositionRepository
import net.tautellini.arenatactics.data.repository.GameModeRepository
import net.tautellini.arenatactics.domain.RichComposition

sealed class CompositionSelectionState {
    data object Loading : CompositionSelectionState()
    data class Success(
        val grouped: Map<CompositionTier, List<RichComposition>>
    ) : CompositionSelectionState()
    data class Error(val message: String) : CompositionSelectionState()
}

class CompositionSelectionViewModel(
    private val gameModeId: String,
    private val gameModeRepository: GameModeRepository,
    private val compositionRepository: CompositionRepository
) : ViewModel() {
    private val _state = MutableStateFlow<CompositionSelectionState>(CompositionSelectionState.Loading)
    val state: StateFlow<CompositionSelectionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                val mode = gameModeRepository.getAll().first { it.id == gameModeId }
                val rich = compositionRepository.getRichCompositions(
                    specPoolId        = mode.specPoolId,
                    classPoolId       = mode.classPoolId,
                    compositionSetId  = mode.compositionSetId,
                    teamSize          = mode.teamSize
                )
                val grouped = CompositionTier.entries
                    .associateWith { tier -> rich.filter { it.composition.tier == tier } }
                    .filterValues { it.isNotEmpty() }
                CompositionSelectionState.Success(grouped)
            } catch (e: Throwable) {
                CompositionSelectionState.Error(e.message ?: "Failed to load compositions")
            }
        }
    }
}
```

- [ ] **Step 2: Replace MatchupListViewModel.kt**

Adds `specMap` to `Success`. Adds `SpecRepository` dependency (delegated through `CompositionRepository.getSpecs`).

```kotlin
package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.Matchup
import net.tautellini.arenatactics.data.model.WowClass
import net.tautellini.arenatactics.data.model.WowSpec
import net.tautellini.arenatactics.data.repository.CompositionRepository
import net.tautellini.arenatactics.data.repository.GameModeRepository
import net.tautellini.arenatactics.data.repository.MatchupRepository

sealed class MatchupListState {
    data object Loading : MatchupListState()
    data class Success(
        val matchups: List<Matchup>,
        val specMap:  Map<String, WowSpec>,
        val classMap: Map<String, WowClass>
    ) : MatchupListState()
    data class Error(val message: String) : MatchupListState()
}

class MatchupListViewModel(
    private val gameModeId: String,
    private val compositionId: String,
    private val gameModeRepository: GameModeRepository,
    private val compositionRepository: CompositionRepository,
    private val matchupRepository: MatchupRepository
) : ViewModel() {
    private val _state = MutableStateFlow<MatchupListState>(MatchupListState.Loading)
    val state: StateFlow<MatchupListState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                val mode     = gameModeRepository.getAll().first { it.id == gameModeId }
                val specs    = compositionRepository.getSpecs(mode.specPoolId)
                val specMap  = specs.associateBy { it.id }
                val classes  = compositionRepository.getClasses(mode.classPoolId)
                val classMap = classes.associateBy { it.id }
                val matchups = matchupRepository.getForComposition(compositionId)
                MatchupListState.Success(matchups, specMap, classMap)
            } catch (e: Throwable) {
                MatchupListState.Error(e.message ?: "Failed to load matchups")
            }
        }
    }
}
```

- [ ] **Step 3: Replace MatchupDetailViewModel.kt**

```kotlin
package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.Matchup
import net.tautellini.arenatactics.data.model.WowClass
import net.tautellini.arenatactics.data.model.WowSpec
import net.tautellini.arenatactics.data.repository.CompositionRepository
import net.tautellini.arenatactics.data.repository.GameModeRepository
import net.tautellini.arenatactics.data.repository.MatchupRepository

sealed class MatchupDetailState {
    data object Loading : MatchupDetailState()
    data class Success(
        val matchup:  Matchup,
        val specMap:  Map<String, WowSpec>,
        val classMap: Map<String, WowClass>
    ) : MatchupDetailState()
    data class Error(val message: String) : MatchupDetailState()
}

class MatchupDetailViewModel(
    private val gameModeId: String,
    private val compositionId: String,
    private val matchupId: String,
    private val gameModeRepository: GameModeRepository,
    private val compositionRepository: CompositionRepository,
    private val matchupRepository: MatchupRepository
) : ViewModel() {
    private val _state = MutableStateFlow<MatchupDetailState>(MatchupDetailState.Loading)
    val state: StateFlow<MatchupDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                val mode     = gameModeRepository.getAll().first { it.id == gameModeId }
                val specs    = compositionRepository.getSpecs(mode.specPoolId)
                val specMap  = specs.associateBy { it.id }
                val classes  = compositionRepository.getClasses(mode.classPoolId)
                val classMap = classes.associateBy { it.id }
                val matchup  = matchupRepository.getById(compositionId, matchupId)
                    ?: throw IllegalArgumentException("Matchup not found: $matchupId")
                MatchupDetailState.Success(matchup, specMap, classMap)
            } catch (e: Throwable) {
                MatchupDetailState.Error(e.message ?: "Failed to load matchup")
            }
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: update ViewModels for spec model — grouped compositions, specMap in matchup states"
```

---

### Task 7: Create SpecBadge component + update CompositionCard

**Files:**
- Create: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/components/SpecBadge.kt`
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/components/CompositionCard.kt`

- [ ] **Step 1: Create SpecBadge.kt**

`SpecBadge` shows a Wowhead spec icon next to the spec name, styled with the class colour (same look as `ClassBadge`).

```kotlin
package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import net.tautellini.arenatactics.data.model.WowClass
import net.tautellini.arenatactics.data.model.WowSpec
import net.tautellini.arenatactics.data.model.WowheadIcons
import net.tautellini.arenatactics.presentation.theme.Background
import net.tautellini.arenatactics.presentation.theme.classColor

@Composable
fun SpecBadge(
    spec: WowSpec,
    wowClass: WowClass,
    modifier: Modifier = Modifier
) {
    val color = classColor(wowClass.id)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = WowheadIcons.medium(spec.iconName),
            contentDescription = spec.name,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = spec.name,
            color = Background,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
```

- [ ] **Step 2: Update CompositionCard.kt**

Replace `ClassBadge` calls with `SpecBadge`. Accept `onClick: (() -> Unit)?` — `null` means the card is disabled (`hasData: false`).

```kotlin
package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import net.tautellini.arenatactics.domain.RichComposition
import net.tautellini.arenatactics.presentation.theme.CardColor

@Composable
fun CompositionCard(
    richComposition: RichComposition,
    onClick: (() -> Unit)?,         // null = hasData:false — card is unselectable
    modifier: Modifier = Modifier
) {
    val hasData = richComposition.composition.hasData
    Surface(
        color = CardColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (hasData) 1f else 0.35f)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpecBadge(richComposition.spec1, richComposition.class1)
            SpecBadge(richComposition.spec2, richComposition.class2)
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat: add SpecBadge with Wowhead icon, update CompositionCard for spec model and hasData shimmer"
```

---

### Task 8: Update CompositionSelectionScreen

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/CompositionSelectionScreen.kt`

The screen now renders four tier sections. The Others section is collapsible and starts collapsed.

- [ ] **Step 1: Replace CompositionSelectionScreen.kt**

```kotlin
package net.tautellini.arenatactics.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.data.model.CompositionTier
import net.tautellini.arenatactics.domain.RichComposition
import net.tautellini.arenatactics.navigation.Navigator
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.CompositionSelectionState
import net.tautellini.arenatactics.presentation.CompositionSelectionViewModel
import net.tautellini.arenatactics.presentation.screens.components.BackButton
import net.tautellini.arenatactics.presentation.screens.components.CompositionCard
import net.tautellini.arenatactics.presentation.theme.*

private fun CompositionTier.label() = when (this) {
    CompositionTier.DOMINANT -> "Dominant"
    CompositionTier.STRONG   -> "Strong"
    CompositionTier.PLAYABLE -> "Playable"
    CompositionTier.OTHERS   -> "Others"
}

@Composable
fun CompositionSelectionScreen(
    gameModeId: String,
    viewModel: CompositionSelectionViewModel,
    navigator: Navigator
) {
    val state by viewModel.state.collectAsState()
    var othersExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(Background).padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton { navigator.pop() }
            Spacer(Modifier.width(12.dp))
            Text(
                "Select Composition",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(24.dp))
        when (val s = state) {
            is CompositionSelectionState.Loading ->
                CircularProgressIndicator(color = Accent)
            is CompositionSelectionState.Error ->
                Text(s.message, color = TextSecondary)
            is CompositionSelectionState.Success -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CompositionTier.entries.forEach { tier ->
                        val comps = s.grouped[tier] ?: return@forEach
                        if (tier == CompositionTier.OTHERS) {
                            item {
                                TierHeader(
                                    label = tier.label(),
                                    expandable = true,
                                    expanded = othersExpanded,
                                    onToggle = { othersExpanded = !othersExpanded }
                                )
                            }
                            item {
                                // A nested LazyColumn is not permitted inside LazyColumn.
                                // Using Column + forEach here is intentional — AnimatedVisibility
                                // defers animation but all 72 items are composed while collapsed.
                                // Acceptable for this list size; revisit if performance degrades.
                                AnimatedVisibility(visible = othersExpanded) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        comps.forEach { rich ->
                                            CompositionCard(
                                                richComposition = rich,
                                                onClick = if (rich.composition.hasData) {
                                                    { navigator.push(Screen.GearView(gameModeId, rich.composition.id)) }
                                                } else null
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            item { TierHeader(label = tier.label()) }
                            items(comps) { rich ->
                                CompositionCard(
                                    richComposition = rich,
                                    onClick = if (rich.composition.hasData) {
                                        { navigator.push(Screen.GearView(gameModeId, rich.composition.id)) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TierHeader(
    label: String,
    expandable: Boolean = false,
    expanded: Boolean = false,
    onToggle: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (expandable) Modifier.clickable(onClick = onToggle) else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (expandable) {
            Text(
                text = if (expanded) "▲" else "▼",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add -A
git commit -m "feat: CompositionSelectionScreen shows tier sections; Others collapsible, shimmer for no-data cards"
```

---

### Task 9: Update MatchupListScreen and MatchupDetailScreen

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/MatchupListScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/MatchupDetailScreen.kt`

- [ ] **Step 1: Update MatchupListScreen.kt**

Replace `classMap[matchup.enemyClass1Id]` lookups with spec-based lookups via `specMap` + `classMap` for colour, using `SpecBadge`.

```kotlin
package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.navigation.Navigator
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.MatchupListState
import net.tautellini.arenatactics.presentation.MatchupListViewModel
import net.tautellini.arenatactics.presentation.screens.components.SpecBadge
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun MatchupListScreen(
    gameModeId: String,
    compositionId: String,
    viewModel: MatchupListViewModel,
    navigator: Navigator
) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is MatchupListState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Accent)
        }
        is MatchupListState.Error -> Text(
            s.message,
            color = TextSecondary,
            modifier = Modifier.padding(24.dp)
        )
        is MatchupListState.Success -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(Background),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(s.matchups) { matchup ->
                    Surface(
                        color = CardColor,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().clickable {
                            navigator.push(Screen.MatchupDetail(gameModeId, compositionId, matchup.id))
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("vs", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(20.dp))
                            matchup.enemySpecIds.forEach { specId ->
                                val spec  = s.specMap[specId]  ?: return@forEach
                                val clazz = s.classMap[spec.classId] ?: return@forEach
                                SpecBadge(spec, clazz)
                            }
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Update MatchupDetailScreen.kt**

```kotlin
package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.navigation.Navigator
import net.tautellini.arenatactics.presentation.MatchupDetailState
import net.tautellini.arenatactics.presentation.MatchupDetailViewModel
import net.tautellini.arenatactics.presentation.screens.components.BackButton
import net.tautellini.arenatactics.presentation.screens.components.MarkdownText
import net.tautellini.arenatactics.presentation.screens.components.SpecBadge
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun MatchupDetailScreen(
    viewModel: MatchupDetailViewModel,
    navigator: Navigator
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        when (val s = state) {
            is MatchupDetailState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
            is MatchupDetailState.Error -> Text(
                s.message,
                color = TextSecondary,
                modifier = Modifier.padding(24.dp)
            )
            is MatchupDetailState.Success -> {
                val matchup = s.matchup
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackButton { navigator.pop() }
                    Spacer(Modifier.width(12.dp))
                    Text("vs", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                    matchup.enemySpecIds.forEach { specId ->
                        val spec  = s.specMap[specId]  ?: return@forEach
                        val clazz = s.classMap[spec.classId] ?: return@forEach
                        SpecBadge(spec, clazz, modifier = Modifier.padding(end = 6.dp))
                    }
                }
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        MarkdownText(matchup.strategyMarkdown)
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat: update MatchupListScreen and MatchupDetailScreen to use SpecBadge"
```

---

### Task 10: Wire SpecRepository into App.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/App.kt`

- [ ] **Step 1: Update App.kt — add SpecRepository, inject into CompositionRepository**

Only two lines change: add `specRepository`, update `compositionRepository` constructor.

```kotlin
val specRepository        = remember { SpecRepository() }
val compositionRepository = remember { CompositionRepository(specRepository) }
```

Full updated App.kt:

```kotlin
package net.tautellini.arenatactics

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import net.tautellini.arenatactics.data.repository.*
import net.tautellini.arenatactics.navigation.Navigator
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.*
import net.tautellini.arenatactics.presentation.screens.*
import net.tautellini.arenatactics.presentation.theme.ArenaTacticsTheme

@Composable
fun App() {
    val gameModeRepository    = remember { GameModeRepository() }
    val specRepository        = remember { SpecRepository() }
    val compositionRepository = remember { CompositionRepository(specRepository) }
    val gearRepository        = remember { GearRepository(compositionRepository) }
    val matchupRepository     = remember { MatchupRepository() }
    val navigator = remember {
        val initialScreen = Screen.fromPath(getInitialPath())
        Navigator(Screen.buildStack(initialScreen))
    }

    DisposableEffect(navigator) {
        registerPopCallback { navigator.pop() }
        onDispose { registerPopCallback {} }
    }

    val stack by navigator.stack.collectAsState()

    ArenaTacticsTheme {
        when (val screen = stack.last()) {
            is Screen.GameModeSelection -> {
                val vm = viewModel { GameModeSelectionViewModel(gameModeRepository) }
                GameModeSelectionScreen(vm, navigator)
            }
            is Screen.CompositionSelection -> {
                val vm = viewModel(key = screen.gameModeId) {
                    CompositionSelectionViewModel(screen.gameModeId, gameModeRepository, compositionRepository)
                }
                CompositionSelectionScreen(screen.gameModeId, vm, navigator)
            }
            is Screen.GearView -> {
                val gearVm = viewModel(key = "gear_${screen.compositionId}") {
                    GearViewModel(screen.gameModeId, screen.compositionId, gameModeRepository, compositionRepository, gearRepository)
                }
                val matchupVm = viewModel(key = "matchups_${screen.compositionId}") {
                    MatchupListViewModel(screen.gameModeId, screen.compositionId, gameModeRepository, compositionRepository, matchupRepository)
                }
                CompositionHubScreen(screen.gameModeId, screen.compositionId, gearVm, matchupVm, navigator)
            }
            is Screen.MatchupList -> {
                val gearVm = viewModel(key = "gear_${screen.compositionId}") {
                    GearViewModel(screen.gameModeId, screen.compositionId, gameModeRepository, compositionRepository, gearRepository)
                }
                val matchupVm = viewModel(key = "matchups_${screen.compositionId}") {
                    MatchupListViewModel(screen.gameModeId, screen.compositionId, gameModeRepository, compositionRepository, matchupRepository)
                }
                CompositionHubScreen(screen.gameModeId, screen.compositionId, gearVm, matchupVm, navigator)
            }
            is Screen.MatchupDetail -> {
                val vm = viewModel(key = screen.matchupId) {
                    MatchupDetailViewModel(screen.gameModeId, screen.compositionId, screen.matchupId, gameModeRepository, compositionRepository, matchupRepository)
                }
                MatchupDetailScreen(vm, navigator)
            }
        }
    }
}
```

- [ ] **Step 2: Run all tests**

```bash
gradlew.bat :composeApp:allTests
```

Expected: all tests PASS.

- [ ] **Step 3: Attempt a build**

```bash
gradlew.bat :composeApp:jsBrowserDevelopmentRun
```

Expected: compiles and runs. The app shows tier-grouped composition list. **Note:** Dominant comps will be selectable but clicking through to the matchup list will show empty (matchup files not created yet — that's Task 11).

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: wire SpecRepository into App.kt — migration complete"
```

---

### Task 11: Migrate matchup data

**Files:**
- Create: `composeApp/src/commonMain/composeResources/files/matchups/matchups_mage_frost_rogue_subtlety.json`
- Create: `composeApp/src/commonMain/composeResources/files/matchups/matchups_priest_discipline_rogue_subtlety.json`
- Create: `composeApp/src/commonMain/composeResources/files/matchups/matchups_mage_frost_priest_discipline.json`
- Create: `composeApp/src/commonMain/composeResources/files/matchups/matchups_druid_restoration_rogue_subtlety.json`
- Create: `composeApp/src/commonMain/composeResources/files/matchups/matchups_priest_discipline_warlock_affliction.json`
- Create: `composeApp/src/commonMain/composeResources/files/matchups/matchups_druid_restoration_warlock_affliction.json`

The old `matchups_mage_rogue.json` (deleted in Task 2) contained matchup strategies for the Frost Mage / Subtlety Rogue composition. These must be migrated to the new spec-based format in `matchups_mage_frost_rogue_subtlety.json`.

**Spec mapping for enemy IDs** (the strategy content is reused verbatim).

> **Disambiguation note:** The old matchup data uses `priest` as an enemy class without specifying Discipline vs Shadow. All priest entries are mapped to `priest_discipline` (the healer) since every old `priest + X` matchup describes fighting a healing priest. If a matchup's strategy explicitly describes Shadow Priest behaviour, update the `enemySpecIds` to `priest_shadow` manually.

| Old `enemyClass1Id_enemyClass2Id` | New `enemySpecIds` (sorted) |
|---|---|
| `druid` + `warrior` | `["druid_restoration", "warrior_arms"]` |
| `priest` + `warrior` | `["priest_discipline", "warrior_arms"]` |
| `mage` + `rogue` | `["mage_frost", "rogue_subtlety"]` |
| `rogue` + `warrior` | `["rogue_subtlety", "warrior_arms"]` |
| `priest` + `warlock` | `["priest_discipline", "warlock_affliction"]` |
| `druid` + `warlock` | `["druid_restoration", "warlock_affliction"]` |
| `rogue` + `warlock` | `["rogue_subtlety", "warlock_affliction"]` |
| `druid` + `rogue` | `["druid_restoration", "rogue_subtlety"]` |
| `druid` + `mage` | `["druid_restoration", "mage_frost"]` |
| `mage` + `priest` | `["mage_frost", "priest_discipline"]` |
| `druid` + `hunter` | `["druid_restoration", "hunter_marksmanship"]` |
| `hunter` + `priest` | `["hunter_marksmanship", "priest_discipline"]` |
| `hunter` + `rogue` | `["hunter_marksmanship", "rogue_subtlety"]` |
| `hunter` + `warrior` | `["hunter_marksmanship", "warrior_arms"]` |
| `rogue` + `shaman` | `["rogue_subtlety", "shaman_restoration"]` |
| `mage` + `shaman` | `["mage_frost", "shaman_restoration"]` |
| `shaman` + `warrior` | `["shaman_restoration", "warrior_arms"]` |
| `hunter` + `shaman` | `["hunter_marksmanship", "shaman_restoration"]` |
| `paladin` + `warrior` | `["paladin_holy", "warrior_arms"]` |
| `paladin` + `rogue` | `["paladin_holy", "rogue_subtlety"]` |
| `paladin` + `warlock` | `["paladin_holy", "warlock_affliction"]` |
| `mage` + `paladin` | `["mage_frost", "paladin_holy"]` |
| `hunter` + `paladin` | `["hunter_marksmanship", "paladin_holy"]` |
| `priest` + `rogue` | `["priest_discipline", "rogue_subtlety"]` |
| `shaman` + `warlock` | `["shaman_restoration", "warlock_affliction"]` |

**ID format:** `{comp_id}_vs_{enemy_comp_id}` where both IDs are sorted spec IDs joined by `_`.
Example: `mage_frost_rogue_subtlety_vs_druid_restoration_warrior_arms`

- [ ] **Step 1: Create matchups_mage_frost_rogue_subtlety.json**

Copy all 25 entries from the deleted `matchups_mage_rogue.json`, replacing:
- `"id"` → new format e.g. `"mage_frost_rogue_subtlety_vs_druid_restoration_warrior_arms"`
- `"enemyClass1Id"` + `"enemyClass2Id"` → `"enemySpecIds": ["sorted_spec1", "sorted_spec2"]`
- Keep all `"strategyMarkdown"` values verbatim

- [ ] **Step 2: Create stub files for the other 5 Dominant comps**

Each file is an empty JSON array. These become selectable in the UI (showing an empty matchup list) until strategy content is authored.

`matchups_priest_discipline_rogue_subtlety.json`:
```json
[]
```

`matchups_mage_frost_priest_discipline.json`:
```json
[]
```

`matchups_druid_restoration_rogue_subtlety.json`:
```json
[]
```

`matchups_priest_discipline_warlock_affliction.json`:
```json
[]
```

`matchups_druid_restoration_warlock_affliction.json`:
```json
[]
```

Note: `matchups_rogue_subtlety_rogue_subtlety.json` (the mirror comp) is also Dominant and `hasData: true`. Create it too:

`matchups_rogue_subtlety_rogue_subtlety.json`:
```json
[]
```

- [ ] **Step 3: Smoke-test the app end-to-end**

```bash
gradlew.bat :composeApp:jsBrowserDevelopmentRun
```

Verify:
1. Composition selection screen shows four tier sections
2. Dominant comps are clickable; Strong/Playable/Others show grey shimmer
3. Clicking a Dominant comp opens the matchup list (may be empty for stubs)
4. Clicking the Frost Mage / Subtlety Rogue comp shows the 25 migrated matchups
5. Clicking a matchup opens the detail screen with spec badges in the header

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: migrate matchup data to spec-based format; add stubs for remaining Dominant comps"
```
