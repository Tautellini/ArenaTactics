# Paper Doll Gear Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the flat gear list with a WoW character screen–style paper doll layout showing item icons fetched from Wowhead's CDN, with real Wowhead tooltips on hover via a cursor-following DOM overlay.

**Architecture:** Three independent layers: (1) GearItem data model gains an `icon` field with all 4 JSON files updated, (2) GearScreen.kt's private `GearTabContent` is replaced with a paper doll layout using Coil 3 AsyncImage for icons and FlowRow for responsive two-column layout, (3) two new platform interop functions (`showWowheadTooltip`/`hideWowheadTooltip`) create a cursor-following DOM anchor element that Wowhead's `tooltips.js` attaches to.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.10.0, Coil 3.1.0 (`coil-compose` + `coil-network-ktor3`), Wowhead `tooltips.js`, `@JsFun` for Kotlin/Wasm interop.

---

## File Structure

| File | Change |
|---|---|
| `gradle/libs.versions.toml` | Add `coil3 = "3.1.0"` + 2 library entries |
| `composeApp/build.gradle.kts` | Add Coil 3 to `commonMain.dependencies` |
| `data/model/Models.kt` | Add `icon: String` to `GearItem` |
| `commonTest/.../RepositoryParsingTest.kt` | Update `gearPhaseDeserializes` test JSON to include `icon` field |
| `composeResources/files/gear/gear_rogue_phase1.json` | Add `"icon"` to all 17 items |
| `composeResources/files/gear/gear_rogue_phase2.json` | Add `"icon"` to all 17 items |
| `composeResources/files/gear/gear_mage_phase1.json` | Add `"icon"` to all 17 items |
| `composeResources/files/gear/gear_mage_phase2.json` | Add `"icon"` to all 17 items |
| `Platform.kt` | Add 2 new `expect` declarations alongside existing 5 |
| `Platform.jvm.kt` | Add 2 no-op `actual` implementations alongside existing 5 |
| `Platform.js.kt` | Add 2 `actual` implementations using `kotlinx.browser` DOM API |
| `Platform.wasmJs.kt` | Add 2 `actual` implementations using `@JsFun` helpers |
| `webMain/main.kt` | Inject `whTooltips` config + `tooltips.js` before `ComposeViewport` |
| `presentation/screens/GearScreen.kt` | Replace private `GearTabContent` only; add `PaperDoll`, `GearSlot`, `EmptyGearSlot`, `EmptyIconPlaceholder`; preserve `CompositionHubScreen` |

---

## Task 1: Add Coil 3 Dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `composeApp/build.gradle.kts`

- [ ] **Step 1: Add Coil 3 to version catalog**

Edit `gradle/libs.versions.toml`. Add under `[versions]`:
```toml
coil3 = "3.1.0"
```
Add under `[libraries]`:
```toml
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil3" }
coil-network-ktor = { module = "io.coil-kt.coil3:coil-network-ktor3", version.ref = "coil3" }
```

- [ ] **Step 2: Add Coil 3 to commonMain dependencies**

In `composeApp/build.gradle.kts`, add to `commonMain.dependencies { }`:
```kotlin
implementation(libs.coil.compose)
implementation(libs.coil.network.ktor)
```

- [ ] **Step 3: Verify build resolves dependencies**
```bash
./gradlew :composeApp:compileKotlinJvm
```
Expected: BUILD SUCCESSFUL. If there is a Ktor version conflict reported, add `ktor = "3.1.0"` to `[versions]` and add explicit `ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }` under `[libraries]` + `implementation(libs.ktor.client.core)` to commonMain.

- [ ] **Step 4: Commit**
```bash
git add gradle/libs.versions.toml composeApp/build.gradle.kts
git commit -m "feat: add Coil 3 dependency for async image loading"
```

---

## Task 2: Update GearItem Data Model

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/model/Models.kt`
- Modify: `composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/data/repository/RepositoryParsingTest.kt`

- [ ] **Step 1: Write the failing test first**

In `RepositoryParsingTest.kt`, update `gearPhaseDeserializes` to include the new `icon` field:
```kotlin
@Test
fun gearPhaseDeserializes() {
    val json = """{"phase":1,"classId":"rogue","items":[{"wowheadId":28210,"name":"Gladiator's Leather Helm","slot":"Head","icon":"inv_helmet_04","enchant":"Glyph of Ferocity","gems":["Relentless Earthstorm Diamond"]}]}"""
    val result = parseGearPhase(json)
    assertEquals(1, result.phase)
    assertEquals(1, result.items.size)
    assertEquals(28210, result.items[0].wowheadId)
    assertEquals("inv_helmet_04", result.items[0].icon)
}
```

- [ ] **Step 2: Run test — verify it fails**
```bash
./gradlew :composeApp:jvmTest --tests "*.RepositoryParsingTest.gearPhaseDeserializes"
```
Expected: FAIL — `icon` field not found / deserialization error.

- [ ] **Step 3: Add `icon` field to `GearItem` in `Models.kt`**

Replace the existing `GearItem` data class:
```kotlin
@Serializable
data class GearItem(
    val wowheadId: Int,
    val name: String,
    val slot: String,
    val icon: String = "inv_misc_questionmark",  // default = placeholder if missing from JSON
    val enchant: String? = null,
    val gems: List<String> = emptyList()
)
```

Note: `icon` has a default value so existing JSON files without the field still parse. This is defensive — the JSON files will be updated in Task 3.

- [ ] **Step 4: Run test — verify it passes**
```bash
./gradlew :composeApp:jvmTest --tests "*.RepositoryParsingTest.gearPhaseDeserializes"
```
Expected: PASS

- [ ] **Step 5: Run all tests**
```bash
./gradlew :composeApp:jvmTest
```
Expected: All tests PASS

- [ ] **Step 6: Commit**
```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/model/Models.kt \
  composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/data/repository/RepositoryParsingTest.kt
git commit -m "feat: add icon field to GearItem data model"
```

---

## Task 3: Update Gear JSON Files with Icon Names

**Files:**
- Modify: `composeApp/src/commonMain/composeResources/files/gear/gear_rogue_phase1.json`
- Modify: `composeApp/src/commonMain/composeResources/files/gear/gear_rogue_phase2.json`
- Modify: `composeApp/src/commonMain/composeResources/files/gear/gear_mage_phase1.json`
- Modify: `composeApp/src/commonMain/composeResources/files/gear/gear_mage_phase2.json`

Icon URL pattern: `https://wow.zamimg.com/images/wow/icons/medium/{icon}.jpg`
If an icon shows as broken at runtime, check the correct name at `https://www.wowhead.com/tbc/item={wowheadId}` — the icon name is displayed on the item's Wowhead page. The `GearSlot` composable shows a fallback placeholder for missing icons.

- [ ] **Step 1: Update `gear_rogue_phase1.json`**

Replace all item entries to add `"icon"` field (add after `"slot"` in each object):

```json
{
  "phase": 1,
  "classId": "rogue",
  "items": [
    { "wowheadId": 28210, "name": "Gladiator's Leather Helm",       "slot": "Head",       "icon": "inv_helmet_04",              "enchant": "Glyph of Ferocity",                "gems": ["Relentless Earthstorm Diamond", "Shifting Nightseye"] },
    { "wowheadId": 28738, "name": "Pendant of the Peril",           "slot": "Neck",       "icon": "inv_jewelry_necklace_25",    "enchant": null,                               "gems": ["Jagged Crimson Spinel"] },
    { "wowheadId": 28214, "name": "Gladiator's Leather Spaulders",  "slot": "Shoulders",  "icon": "inv_shoulder_23",            "enchant": "Greater Inscription of Vengeance", "gems": ["Bright Blood Garnet"] },
    { "wowheadId": 28256, "name": "Drape of the Dark Reavers",      "slot": "Back",       "icon": "inv_misc_cape_11",           "enchant": "Enchant Cloak - Subtlety",         "gems": [] },
    { "wowheadId": 28208, "name": "Gladiator's Leather Tunic",      "slot": "Chest",      "icon": "inv_chest_leather_04",       "enchant": "Enchant Chest - Exceptional Stats", "gems": ["Bright Blood Garnet", "Bright Blood Garnet", "Shifting Nightseye"] },
    { "wowheadId": 28278, "name": "Bracers of Maliciousness",       "slot": "Wrists",     "icon": "inv_bracer_07",              "enchant": "Enchant Bracer - Assault",         "gems": [] },
    { "wowheadId": 28211, "name": "Gladiator's Leather Gloves",     "slot": "Hands",      "icon": "inv_gauntlets_29",           "enchant": "Enchant Gloves - Superior Agility", "gems": ["Bright Blood Garnet"] },
    { "wowheadId": 31913, "name": "Vindicator's Leather Belt",      "slot": "Waist",      "icon": "inv_belt_08",                "enchant": null,                               "gems": ["Bright Blood Garnet"] },
    { "wowheadId": 28213, "name": "Gladiator's Leather Legguards",  "slot": "Legs",       "icon": "inv_pants_leather_09",       "enchant": "Nethercobra Leg Armor",            "gems": ["Bright Blood Garnet", "Bright Blood Garnet"] },
    { "wowheadId": 28209, "name": "Gladiator's Leather Boots",      "slot": "Feet",       "icon": "inv_boots_08",               "enchant": "Enchant Boots - Cat's Swiftness",  "gems": ["Bright Blood Garnet"] },
    { "wowheadId": 28304, "name": "Gladiator's Band of Triumph",    "slot": "Ring",       "icon": "inv_jewelry_ring_28",        "enchant": "Enchant Ring - Striking",          "gems": [] },
    { "wowheadId": 28765, "name": "Band of the Ranger-General",     "slot": "Ring",       "icon": "inv_jewelry_ring_35",        "enchant": "Enchant Ring - Striking",          "gems": [] },
    { "wowheadId": 29131, "name": "Medallion of the Horde",         "slot": "Trinket",    "icon": "inv_jewelry_necklace_31",    "enchant": null,                               "gems": [] },
    { "wowheadId": 29383, "name": "Bloodlust Brooch",               "slot": "Trinket",    "icon": "inv_jewelry_trinket_iron_01","enchant": null,                               "gems": [] },
    { "wowheadId": 28272, "name": "Gladiator's Shanker",            "slot": "Main Hand",  "icon": "inv_sword_48",               "enchant": "Enchant Weapon - Mongoose",        "gems": [] },
    { "wowheadId": 28270, "name": "Gladiator's Shiv",               "slot": "Off Hand",   "icon": "inv_sword_47",               "enchant": "Enchant Weapon - Mongoose",        "gems": [] },
    { "wowheadId": 28275, "name": "Gladiator's War Edge",           "slot": "Ranged",     "icon": "inv_throwingknife_07",       "enchant": null,                               "gems": [] }
  ]
}
```

- [ ] **Step 2: Update `gear_rogue_phase2.json`**

```json
{
  "phase": 2,
  "classId": "rogue",
  "items": [
    { "wowheadId": 31017, "name": "Merciless Gladiator's Leather Helm",      "slot": "Head",       "icon": "inv_helmet_96",               "enchant": "Glyph of Ferocity",                "gems": ["Relentless Earthstorm Diamond", "Shifting Nightseye", "Jagged Crimson Spinel"] },
    { "wowheadId": 28738, "name": "Pendant of the Peril",                    "slot": "Neck",       "icon": "inv_jewelry_necklace_25",     "enchant": null,                               "gems": ["Jagged Crimson Spinel"] },
    { "wowheadId": 31021, "name": "Merciless Gladiator's Leather Spaulders", "slot": "Shoulders",  "icon": "inv_shoulder_45",             "enchant": "Greater Inscription of Vengeance", "gems": ["Bright Blood Garnet"] },
    { "wowheadId": 28256, "name": "Drape of the Dark Reavers",               "slot": "Back",       "icon": "inv_misc_cape_11",            "enchant": "Enchant Cloak - Subtlety",         "gems": [] },
    { "wowheadId": 31015, "name": "Merciless Gladiator's Leather Tunic",     "slot": "Chest",      "icon": "inv_chest_leather_09",        "enchant": "Enchant Chest - Exceptional Stats", "gems": ["Bright Blood Garnet", "Bright Blood Garnet", "Shifting Nightseye"] },
    { "wowheadId": 34462, "name": "Vindicator's Leather Bracers",            "slot": "Wrists",     "icon": "inv_bracer_14",               "enchant": "Enchant Bracer - Assault",         "gems": [] },
    { "wowheadId": 31018, "name": "Merciless Gladiator's Leather Gloves",    "slot": "Hands",      "icon": "inv_gauntlets_55",            "enchant": "Enchant Gloves - Superior Agility", "gems": ["Bright Blood Garnet"] },
    { "wowheadId": 34467, "name": "Merciless Gladiator's Leather Belt",      "slot": "Waist",      "icon": "inv_belt_26",                 "enchant": null,                               "gems": ["Bright Blood Garnet"] },
    { "wowheadId": 31020, "name": "Merciless Gladiator's Leather Legguards", "slot": "Legs",       "icon": "inv_pants_leather_16",        "enchant": "Nethercobra Leg Armor",            "gems": ["Bright Blood Garnet", "Bright Blood Garnet"] },
    { "wowheadId": 31016, "name": "Merciless Gladiator's Leather Boots",     "slot": "Feet",       "icon": "inv_boots_09",                "enchant": "Enchant Boots - Cat's Swiftness",  "gems": ["Bright Blood Garnet"] },
    { "wowheadId": 31854, "name": "Merciless Gladiator's Band of Triumph",   "slot": "Ring",       "icon": "inv_jewelry_ring_28",         "enchant": "Enchant Ring - Striking",          "gems": [] },
    { "wowheadId": 28765, "name": "Band of the Ranger-General",              "slot": "Ring",       "icon": "inv_jewelry_ring_35",         "enchant": "Enchant Ring - Striking",          "gems": [] },
    { "wowheadId": 29131, "name": "Medallion of the Horde",                  "slot": "Trinket",    "icon": "inv_jewelry_necklace_31",     "enchant": null,                               "gems": [] },
    { "wowheadId": 31193, "name": "Tsunami Talisman",                        "slot": "Trinket",    "icon": "inv_misc_monsterscales_05",   "enchant": null,                               "gems": [] },
    { "wowheadId": 31826, "name": "Merciless Gladiator's Shanker",           "slot": "Main Hand",  "icon": "inv_sword_62",                "enchant": "Enchant Weapon - Mongoose",        "gems": [] },
    { "wowheadId": 31827, "name": "Merciless Gladiator's Shiv",              "slot": "Off Hand",   "icon": "inv_sword_61",                "enchant": "Enchant Weapon - Mongoose",        "gems": [] },
    { "wowheadId": 32040, "name": "Merciless Gladiator's War Edge",          "slot": "Ranged",     "icon": "inv_throwingknife_07",        "enchant": null,                               "gems": [] }
  ]
}
```

- [ ] **Step 3: Update `gear_mage_phase1.json`**

```json
{
  "phase": 1,
  "classId": "mage",
  "items": [
    { "wowheadId": 27843, "name": "Gladiator's Silk Cowl",           "slot": "Head",      "icon": "inv_helmet_47",              "enchant": "Glyph of Power",                    "gems": ["Chaotic Skyfire Diamond", "Runed Blood Garnet"] },
    { "wowheadId": 31012, "name": "Vindicator's Pendant of Conquest","slot": "Neck",      "icon": "inv_jewelry_necklace_27",    "enchant": null,                               "gems": ["Runed Blood Garnet"] },
    { "wowheadId": 27847, "name": "Gladiator's Silk Amice",          "slot": "Shoulders", "icon": "inv_shoulder_30",            "enchant": "Greater Inscription of Discipline", "gems": ["Runed Blood Garnet"] },
    { "wowheadId": 28971, "name": "Cloak of Arcane Evasion",         "slot": "Back",      "icon": "inv_misc_cape_18",           "enchant": "Enchant Cloak - Subtlety",          "gems": [] },
    { "wowheadId": 27841, "name": "Gladiator's Silk Raiment",        "slot": "Chest",     "icon": "inv_chest_cloth_04",         "enchant": "Enchant Chest - Exceptional Stats",  "gems": ["Runed Blood Garnet", "Runed Blood Garnet", "Veiled Flame Spessarite"] },
    { "wowheadId": 31936, "name": "Vindicator's Silk Cuffs",         "slot": "Wrists",    "icon": "inv_bracer_20",              "enchant": "Enchant Bracer - Spellpower",        "gems": [] },
    { "wowheadId": 27844, "name": "Gladiator's Silk Handguards",     "slot": "Hands",     "icon": "inv_gauntlets_42",           "enchant": "Enchant Gloves - Spell Strike",     "gems": ["Runed Blood Garnet"] },
    { "wowheadId": 31935, "name": "Vindicator's Silk Belt",          "slot": "Waist",     "icon": "inv_belt_13",                "enchant": null,                                "gems": ["Runed Blood Garnet"] },
    { "wowheadId": 27846, "name": "Gladiator's Silk Trousers",       "slot": "Legs",      "icon": "inv_pants_cloth_12",         "enchant": "Runic Spellthread",                 "gems": ["Runed Blood Garnet", "Runed Blood Garnet"] },
    { "wowheadId": 27842, "name": "Gladiator's Silk Footwraps",      "slot": "Feet",      "icon": "inv_boots_cloth_04",         "enchant": "Enchant Boots - Boar's Speed",      "gems": ["Runed Blood Garnet"] },
    { "wowheadId": 28305, "name": "Gladiator's Band of Dominance",   "slot": "Ring",      "icon": "inv_jewelry_ring_28",        "enchant": "Enchant Ring - Spellpower",         "gems": [] },
    { "wowheadId": 29281, "name": "Violet Signet of the Archmage",   "slot": "Ring",      "icon": "inv_misc_gem_ruby_02",       "enchant": "Enchant Ring - Spellpower",         "gems": [] },
    { "wowheadId": 29131, "name": "Medallion of the Horde",          "slot": "Trinket",   "icon": "inv_jewelry_necklace_31",    "enchant": null,                                "gems": [] },
    { "wowheadId": 29376, "name": "Icon of the Silver Crescent",     "slot": "Trinket",   "icon": "inv_misc_gem_variety_02",    "enchant": null,                                "gems": [] },
    { "wowheadId": 28190, "name": "Gladiator's Spellblade",          "slot": "Main Hand", "icon": "inv_sword_74",               "enchant": "Enchant Weapon - Spellsurge",       "gems": [] },
    { "wowheadId": 28293, "name": "Gladiator's Endgame",             "slot": "Off Hand",  "icon": "inv_misc_orb_05",            "enchant": null,                                "gems": [] },
    { "wowheadId": 27854, "name": "Gladiator's Touch of Defeat",     "slot": "Wand",      "icon": "inv_wand_14",                "enchant": null,                                "gems": [] }
  ]
}
```

- [ ] **Step 4: Update `gear_mage_phase2.json`**

```json
{
  "phase": 2,
  "classId": "mage",
  "items": [
    { "wowheadId": 31011, "name": "Merciless Gladiator's Silk Cowl",          "slot": "Head",      "icon": "inv_helmet_90",              "enchant": "Glyph of Power",                    "gems": ["Chaotic Skyfire Diamond", "Runed Blood Garnet", "Veiled Flame Spessarite"] },
    { "wowheadId": 31012, "name": "Vindicator's Pendant of Conquest",          "slot": "Neck",      "icon": "inv_jewelry_necklace_27",    "enchant": null,                               "gems": ["Runed Blood Garnet"] },
    { "wowheadId": 31008, "name": "Merciless Gladiator's Silk Amice",          "slot": "Shoulders", "icon": "inv_shoulder_58",            "enchant": "Greater Inscription of Discipline", "gems": ["Runed Blood Garnet"] },
    { "wowheadId": 28971, "name": "Cloak of Arcane Evasion",                   "slot": "Back",      "icon": "inv_misc_cape_18",           "enchant": "Enchant Cloak - Subtlety",          "gems": [] },
    { "wowheadId": 31007, "name": "Merciless Gladiator's Silk Raiment",        "slot": "Chest",     "icon": "inv_chest_cloth_09",         "enchant": "Enchant Chest - Exceptional Stats",  "gems": ["Runed Blood Garnet", "Runed Blood Garnet", "Veiled Flame Spessarite"] },
    { "wowheadId": 34473, "name": "Vindicator's Silk Cuffs",                   "slot": "Wrists",    "icon": "inv_bracer_20",              "enchant": "Enchant Bracer - Spellpower",        "gems": [] },
    { "wowheadId": 31009, "name": "Merciless Gladiator's Silk Handguards",     "slot": "Hands",     "icon": "inv_gauntlets_55",           "enchant": "Enchant Gloves - Spell Strike",     "gems": ["Runed Blood Garnet"] },
    { "wowheadId": 34474, "name": "Vindicator's Silk Belt",                    "slot": "Waist",     "icon": "inv_belt_26",                "enchant": null,                                "gems": ["Runed Blood Garnet"] },
    { "wowheadId": 31010, "name": "Merciless Gladiator's Silk Trousers",       "slot": "Legs",      "icon": "inv_pants_cloth_16",         "enchant": "Runic Spellthread",                 "gems": ["Runed Blood Garnet", "Runed Blood Garnet"] },
    { "wowheadId": 31006, "name": "Merciless Gladiator's Silk Footwraps",      "slot": "Feet",      "icon": "inv_boots_cloth_06",         "enchant": "Enchant Boots - Boar's Speed",      "gems": ["Runed Blood Garnet"] },
    { "wowheadId": 31852, "name": "Merciless Gladiator's Band of Dominance",   "slot": "Ring",      "icon": "inv_jewelry_ring_28",        "enchant": "Enchant Ring - Spellpower",         "gems": [] },
    { "wowheadId": 29281, "name": "Violet Signet of the Archmage",             "slot": "Ring",      "icon": "inv_misc_gem_ruby_02",       "enchant": "Enchant Ring - Spellpower",         "gems": [] },
    { "wowheadId": 29131, "name": "Medallion of the Horde",                    "slot": "Trinket",   "icon": "inv_jewelry_necklace_31",    "enchant": null,                                "gems": [] },
    { "wowheadId": 29376, "name": "Icon of the Silver Crescent",               "slot": "Trinket",   "icon": "inv_misc_gem_variety_02",    "enchant": null,                                "gems": [] },
    { "wowheadId": 31818, "name": "Merciless Gladiator's Spellblade",          "slot": "Main Hand", "icon": "inv_sword_82",               "enchant": "Enchant Weapon - Soulfrost",        "gems": [] },
    { "wowheadId": 31857, "name": "Merciless Gladiator's Endgame",             "slot": "Off Hand",  "icon": "inv_misc_orb_05",            "enchant": null,                                "gems": [] },
    { "wowheadId": 31858, "name": "Merciless Gladiator's Touch of Defeat",     "slot": "Wand",      "icon": "inv_wand_17",                "enchant": null,                                "gems": [] }
  ]
}
```

- [ ] **Step 5: Commit**
```bash
git add composeApp/src/commonMain/composeResources/files/gear/
git commit -m "feat: add Wowhead icon names to all gear JSON data"
```

---

## Task 4: Platform Interop — Wowhead Tooltip Functions

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/Platform.kt`
- Modify: `composeApp/src/jvmMain/kotlin/net/tautellini/arenatactics/Platform.jvm.kt`
- Modify: `composeApp/src/jsMain/kotlin/net/tautellini/arenatactics/Platform.js.kt`
- Modify: `composeApp/src/wasmJsMain/kotlin/net/tautellini/arenatactics/Platform.wasmJs.kt`

- [ ] **Step 1: Add `expect` declarations to `Platform.kt`**

Append to the end of the file (do NOT remove or change the existing 5 declarations):
```kotlin
// Shows a Wowhead tooltip for the given item ID at the cursor position.
// On web: creates/updates a 1×1 invisible DOM anchor at the cursor, triggers WH tooltip.
// On JVM: no-op.
expect fun showWowheadTooltip(itemId: Int, cursorX: Float, cursorY: Float)
expect fun hideWowheadTooltip()
```

- [ ] **Step 2: Add no-op `actual` to `Platform.jvm.kt`**

Append to the end of the file (alongside the existing 5 `actual` functions):
```kotlin
actual fun showWowheadTooltip(itemId: Int, cursorX: Float, cursorY: Float) {}
actual fun hideWowheadTooltip() {}
```

- [ ] **Step 3: Add DOM `actual` to `Platform.js.kt`**

Add the following imports at the top of `Platform.js.kt`:
```kotlin
import org.w3c.dom.HTMLAnchorElement
```

Append to the end of the file (alongside the existing 5 `actual` functions):
```kotlin
private var whTooltipEl: HTMLAnchorElement? = null

actual fun showWowheadTooltip(itemId: Int, cursorX: Float, cursorY: Float) {
    val el = whTooltipEl ?: run {
        val a = document.createElement("a") as HTMLAnchorElement
        a.id = "wh-tt"
        document.body!!.appendChild(a)
        whTooltipEl = a
        a
    }
    el.href = "https://www.wowhead.com/tbc/item=$itemId"
    el.setAttribute("data-wowhead", "item=$itemId&domain=tbc")
    el.style.cssText =
        "position:fixed;left:${cursorX}px;top:${cursorY}px;width:1px;height:1px;opacity:0;pointer-events:none;"
    js("if (window.WH) WH.refreshLinks()")
    js("var e=document.getElementById('wh-tt'); if(e) e.dispatchEvent(new MouseEvent('mouseover',{bubbles:true}))")
}

actual fun hideWowheadTooltip() {
    js("var e=document.getElementById('wh-tt'); if(e) e.dispatchEvent(new MouseEvent('mouseout',{bubbles:true}))")
}
```

- [ ] **Step 4: Add `@JsFun` `actual` to `Platform.wasmJs.kt`**

Append to the end of the file (alongside the existing 5 `actual` functions). The `@JsFun` approach is required because `js()` in Kotlin/Wasm only accepts string literals — all variable references must be JS function parameters:
```kotlin
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""(id, x, y) => {
    var el = document.getElementById('wh-tt');
    if (!el) { el = document.createElement('a'); el.id = 'wh-tt'; document.body.appendChild(el); }
    el.href = 'https://www.wowhead.com/tbc/item=' + id;
    el.setAttribute('data-wowhead', 'item=' + id + '&domain=tbc');
    el.style.cssText = 'position:fixed;left:' + x + 'px;top:' + y + 'px;width:1px;height:1px;opacity:0;pointer-events:none;';
    if (window.WH) WH.refreshLinks();
    el.dispatchEvent(new MouseEvent('mouseover', {bubbles:true}));
}""")
private external fun showWowheadTooltipJs(id: Int, x: Float, y: Float)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""() => {
    var el = document.getElementById('wh-tt');
    if (el) el.dispatchEvent(new MouseEvent('mouseout', {bubbles:true}));
}""")
private external fun hideWowheadTooltipJs()

actual fun showWowheadTooltip(itemId: Int, cursorX: Float, cursorY: Float) =
    showWowheadTooltipJs(itemId, cursorX, cursorY)

actual fun hideWowheadTooltip() = hideWowheadTooltipJs()
```

- [ ] **Step 5: Verify JVM build compiles**
```bash
./gradlew :composeApp:compileKotlinJvm
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**
```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/Platform.kt \
  composeApp/src/jvmMain/kotlin/net/tautellini/arenatactics/Platform.jvm.kt \
  composeApp/src/jsMain/kotlin/net/tautellini/arenatactics/Platform.js.kt \
  composeApp/src/wasmJsMain/kotlin/net/tautellini/arenatactics/Platform.wasmJs.kt
git commit -m "feat: add showWowheadTooltip/hideWowheadTooltip platform interop"
```

---

## Task 5: Inject Wowhead `tooltips.js` in webMain

**Files:**
- Modify: `composeApp/src/webMain/kotlin/net/tautellini/arenatactics/main.kt`

- [ ] **Step 1: Add script injection**

Replace the entire file contents:
```kotlin
package net.tautellini.arenatactics

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Wowhead tooltip config — disable auto-scanning of page links;
    // we trigger tooltips programmatically via showWowheadTooltip().
    val cfg = document.createElement("script")
    cfg.textContent = "const whTooltips = {colorLinks:false, iconizeLinks:false, renameLinks:false};"
    document.head!!.appendChild(cfg)

    // Wowhead tooltip script (current API — replaces the old power.js)
    val tooltipScript = document.createElement("script")
    tooltipScript.setAttribute("src", "https://wow.zamimg.com/js/tooltips.js")
    document.head!!.appendChild(tooltipScript)

    // popstate wiring handled inside App.kt via registerPopCallback + DisposableEffect

    ComposeViewport(document.body!!) {
        App()
    }
}
```

- [ ] **Step 2: Commit**
```bash
git add composeApp/src/webMain/kotlin/net/tautellini/arenatactics/main.kt
git commit -m "feat: inject Wowhead tooltips.js in webMain"
```

---

## Task 6: Paper Doll Gear Screen Composables

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/GearScreen.kt`

This task **replaces the private `GearTabContent` function** and adds new private composables below it. The public `CompositionHubScreen` and `CompositionTab` enum at the top of the file are **untouched**.

- [ ] **Step 1: Verify build before starting**
```bash
./gradlew :composeApp:compileKotlinJvm
```
Expected: BUILD SUCCESSFUL. Establish clean baseline before editing.

- [ ] **Step 2: Replace `GearTabContent` and add paper doll composables**

Replace everything from line 88 (`@Composable private fun GearTabContent`) to the end of the file with the following complete implementation:

```kotlin
// ─── Slot ordering for paper doll layout ────────────────────────────────────

private val LEFT_SLOTS   = listOf("Head", "Neck", "Shoulders", "Back", "Chest", "Wrists")
private val RIGHT_SLOTS  = listOf("Hands", "Waist", "Legs", "Feet", "Ring", "Ring")
private val BOTTOM_SLOTS = listOf("Trinket", "Trinket", "Main Hand", "Off Hand", "Ranged")

/** "Wand" (Mage) occupies the same bottom-row position as "Ranged" (Rogue). */
private fun normalizeSlot(s: String) = if (s == "Wand") "Ranged" else s

/**
 * Maps a flat item list to an ordered slot list.
 * Handles duplicate slots (Ring×2, Trinket×2) by consuming items greedily.
 * Returns null for slots with no matching item.
 */
private fun mapItemsToSlots(items: List<GearItem>, slotList: List<String>): List<GearItem?> {
    val remaining = items.toMutableList()
    return slotList.map { slot ->
        val idx = remaining.indexOfFirst { normalizeSlot(it.slot) == slot }
        if (idx >= 0) remaining.removeAt(idx) else null
    }
}

// ─── GearTabContent (replaces the old flat-list version) ────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GearTabContent(viewModel: GearViewModel) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is GearState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Accent)
        }
        is GearState.Error -> Text(s.message, color = TextSecondary, modifier = Modifier.padding(24.dp))
        is GearState.Success -> {
            var selectedPhase by remember { mutableStateOf(1) }

            // Collect available phase numbers from the first class's data
            val availablePhases = remember(s) {
                s.gearByClass.values.firstOrNull()?.map { it.phase }?.sorted() ?: listOf(1)
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Phase tabs
                Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                    availablePhases.forEach { phase ->
                        val selected = phase == selectedPhase
                        Box(
                            modifier = Modifier
                                .clickable { selectedPhase = phase }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Phase $phase",
                                color = if (selected) Accent else TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
                HorizontalDivider(color = DividerColor)

                // Two paper dolls in a flow row (side-by-side on wide screens, stacked on narrow)
                FlowRow(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    s.gearByClass.forEach { (classId, phases) ->
                        val className = s.classNames[classId] ?: classId
                        val phase = phases.find { it.phase == selectedPhase } ?: phases.firstOrNull()
                        if (phase != null) {
                            PaperDoll(
                                classId = classId,
                                className = className,
                                phase = phase,
                                modifier = Modifier
                                    .weight(1f)
                                    .widthIn(min = 280.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── PaperDoll ───────────────────────────────────────────────────────────────

@Composable
private fun PaperDoll(
    classId: String,
    className: String,
    phase: GearPhase,
    modifier: Modifier = Modifier
) {
    val leftItems   = remember(phase) { mapItemsToSlots(phase.items, LEFT_SLOTS) }
    val rightItems  = remember(phase) { mapItemsToSlots(phase.items, RIGHT_SLOTS) }
    val bottomItems = remember(phase) { mapItemsToSlots(phase.items, BOTTOM_SLOTS) }

    Surface(
        color = CardColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.padding(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top: left slots | class icon center | right slots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left column
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    leftItems.zip(LEFT_SLOTS).forEach { (item, slot) ->
                        if (item != null) GearSlot(item) else EmptyGearSlot(slot)
                    }
                }

                // Center: class icon + name
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AsyncImage(
                        model = "https://wow.zamimg.com/images/wow/icons/large/classicon_$classId.jpg",
                        contentDescription = className,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, classColor(classId), RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = className,
                        color = classColor(classId),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Right column
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    rightItems.zip(RIGHT_SLOTS).forEach { (item, slot) ->
                        if (item != null) GearSlot(item) else EmptyGearSlot(slot)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(Modifier.height(8.dp))

            // Bottom row: trinkets + weapons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                bottomItems.zip(BOTTOM_SLOTS).forEach { (item, slot) ->
                    if (item != null) GearSlot(item, modifier = Modifier.weight(1f))
                    else EmptyGearSlot(slot, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ─── GearSlot ────────────────────────────────────────────────────────────────

@Composable
private fun GearSlot(item: GearItem, modifier: Modifier = Modifier) {
    val wowheadUrl = "https://www.wowhead.com/tbc/item=${item.wowheadId}"
    Column(
        modifier = modifier
            .widthIn(min = 60.dp, max = 80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Surface)
            .clickable { openUrl(wowheadUrl) }
            .pointerInput(item.wowheadId) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pos = event.changes.firstOrNull()?.position
                        when (event.type) {
                            PointerEventType.Enter ->
                                pos?.let { showWowheadTooltip(item.wowheadId, it.x, it.y) }
                            PointerEventType.Exit -> hideWowheadTooltip()
                        }
                    }
                }
            }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SubcomposeAsyncImage(
            model = "https://wow.zamimg.com/images/wow/icons/medium/${item.icon}.jpg",
            contentDescription = item.name,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)),
            error = { EmptyIconPlaceholder() }
        )
        Text(
            text = item.name,
            color = Accent,
            fontSize = 10.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
        if (item.enchant != null) {
            Text(
                text = "✦",
                color = TextSecondary,
                fontSize = 9.sp
            )
        }
    }
}

// ─── EmptyGearSlot ───────────────────────────────────────────────────────────

@Composable
private fun EmptyGearSlot(slotName: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .widthIn(min = 60.dp, max = 80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CardElevated)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Surface),
            contentAlignment = Alignment.Center
        ) {
            Text("?", color = TextSecondary, fontSize = 18.sp)
        }
        Text(
            text = slotName,
            color = TextSecondary,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

// ─── EmptyIconPlaceholder (used by SubcomposeAsyncImage error state) ─────────

@Composable
private fun EmptyIconPlaceholder() {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(CardElevated),
        contentAlignment = Alignment.Center
    ) {
        Text("?", color = TextSecondary, fontSize = 18.sp)
    }
}
```

- [ ] **Step 3: Update imports at top of `GearScreen.kt`**

The new code uses several new imports. Replace the import section at the top of `GearScreen.kt` with:
```kotlin
package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import net.tautellini.arenatactics.data.model.GearItem
import net.tautellini.arenatactics.data.model.GearPhase
import net.tautellini.arenatactics.hideWowheadTooltip
import net.tautellini.arenatactics.navigation.Navigator
import net.tautellini.arenatactics.openUrl
import net.tautellini.arenatactics.presentation.GearState
import net.tautellini.arenatactics.presentation.GearViewModel
import net.tautellini.arenatactics.presentation.MatchupListViewModel
import net.tautellini.arenatactics.presentation.screens.components.BackButton
import net.tautellini.arenatactics.presentation.theme.*
import net.tautellini.arenatactics.showWowheadTooltip
```

- [ ] **Step 4: Verify JVM compilation**
```bash
./gradlew :composeApp:compileKotlinJvm
```
Expected: BUILD SUCCESSFUL. If there are import errors, add missing imports from the Compose/Coil API.

- [ ] **Step 5: Run all tests**
```bash
./gradlew :composeApp:jvmTest
```
Expected: All tests PASS (paper doll composables have no tests — they're pure UI).

- [ ] **Step 6: Smoke test desktop app**
```bash
./gradlew :composeApp:run
```
Expected: Desktop window opens. Navigate to Mage/Rogue gear screen. Should show the paper doll layout with two paper dolls (one Mage, one Rogue). Phase 1/2 tabs visible. Item icons may not load on desktop (no network Coil engine configured for JVM in this setup) — this is acceptable; the placeholder `?` box shows instead.

- [ ] **Step 7: Smoke test web (wasmJs)**
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```
Expected: Browser opens. Navigate to gear screen. Item icons should load from Wowhead CDN. Hovering an item should show a Wowhead tooltip popup. Phase tabs switch both paper dolls simultaneously.

- [ ] **Step 8: Commit**
```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/GearScreen.kt
git commit -m "feat: replace flat gear list with paper doll layout + Wowhead icon/tooltip integration"
```

---

## Final Checklist

- [ ] `./gradlew :composeApp:jvmTest` — all tests pass
- [ ] `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` — gear screen shows two paper dolls side by side
- [ ] Phase tab switching updates both paper dolls simultaneously
- [ ] Item icons load from `wow.zamimg.com/images/wow/icons/medium/`
- [ ] Class icons (classicon_rogue, classicon_mage) load in center of each doll
- [ ] Hovering a slot shows Wowhead tooltip popup with item stats
- [ ] Clicking a slot opens the Wowhead item page in a new tab
- [ ] Wrong/missing icons show the `?` placeholder box gracefully
- [ ] Layout wraps to single column on narrow browser windows
- [ ] `CompositionHubScreen` tabs (Gear / Matchups) still work correctly
