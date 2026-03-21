# Addon Navigation Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure the app entry flow to Addon → Section (Tactics / Class Guides), move gear out of Tactics into a per-spec Class Guides section, and remove the tabbed CompositionHubScreen.

**Architecture:** Addon becomes the top-level data concept owning spec/class pools; GameMode is scoped to Tactics (team size, composition set); Class Guides branch directly off the addon level since gear is class-scoped. Navigation is rebuilt around a new 8-variant Screen sealed class.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Compose Navigation, kotlinx.serialization, Coil3, Material3

---

## File Map

### New files
| File | Responsibility |
|---|---|
| `composeApp/src/commonMain/kotlin/.../data/model/Addon.kt` | `Addon` data class |
| `composeApp/src/commonMain/kotlin/.../data/model/GameMode.kt` | `GameMode` extracted from `Models.kt` |
| `composeApp/src/commonMain/composeResources/files/addons.json` | Addon seed data |
| `composeApp/src/commonMain/kotlin/.../data/repository/AddonRepository.kt` | Loads/parses addons.json |
| `composeApp/src/commonMain/kotlin/.../presentation/AddonSelectionViewModel.kt` | Loads all Addons |
| `composeApp/src/commonMain/kotlin/.../presentation/AddonHubViewModel.kt` | Loads one Addon by id |
| `composeApp/src/commonMain/kotlin/.../presentation/GameModeSelectionViewModel.kt` (new) | Loads GameModes by addonId for Tactics |
| `composeApp/src/commonMain/kotlin/.../presentation/ClassGuideListViewModel.kt` | Loads specs from addon pool |
| `composeApp/src/commonMain/kotlin/.../presentation/SpecGuideViewModel.kt` | Loads spec + gear phases |
| `composeApp/src/commonMain/kotlin/.../presentation/screens/AddonSelectionScreen.kt` | Home: addon tiles |
| `composeApp/src/commonMain/kotlin/.../presentation/screens/AddonHubScreen.kt` | Section picker |
| `composeApp/src/commonMain/kotlin/.../presentation/screens/TacticsGameModeSelectionScreen.kt` | Tactics: game mode picker |
| `composeApp/src/commonMain/kotlin/.../presentation/screens/ClassGuideListScreen.kt` | Spec grid |
| `composeApp/src/commonMain/kotlin/.../presentation/screens/SpecGuideScreen.kt` | Per-spec gear |

### Modified files
| File | Change |
|---|---|
| `data/model/Models.kt` | Remove `GameMode` (extracted) |
| `composeResources/files/game_modes.json` | Add `addonId`, then in Task 15 remove `specPoolId`/`classPoolId` |
| `data/repository/GameModeRepository.kt` | Add `getByAddon(addonId)` |
| `data/repository/GearRepository.kt` | Add `getGearForSpec(classId)`, remove `getGearForComposition` + CompositionRepository dep |
| `presentation/CompositionSelectionViewModel.kt` | Add `addonId`, source pool IDs from Addon |
| `presentation/MatchupListViewModel.kt` | Add `addonId`, source pool IDs from Addon |
| `presentation/MatchupDetailViewModel.kt` | Add `addonId`, source pool IDs from Addon |
| `presentation/screens/CompositionSelectionScreen.kt` | Navigate to `MatchupList` instead of `GearView` |
| `presentation/screens/MatchupListScreen.kt` | Add `addonId` param |
| `presentation/screens/components/AppHeader.kt` | New breadcrumb labels, home nav ref |
| `navigation/Screen.kt` | Full rewrite: new sealed class, fromPath, buildStack, toScreen |
| `App.kt` | Full wiring rewrite |
| `commonTest/.../navigation/ScreenNavigationTest.kt` | Full rewrite for new routes |
| `commonTest/.../data/repository/RepositoryParsingTest.kt` | Add addon test, update GameMode test |

### Deleted files
| File | Reason |
|---|---|
| `presentation/GearViewModel.kt` | Replaced by `SpecGuideViewModel.kt` |
| `presentation/GameModeSelectionViewModel.kt` | Replaced by `AddonSelectionViewModel.kt` |
| `presentation/screens/GearScreen.kt` | CompositionHubScreen + GearTabContent → replaced by SpecGuideScreen |
| `presentation/screens/GameModeSelectionScreen.kt` | Replaced by AddonSelectionScreen |

---

## Task 1: Addon model + AddonRepository + seed data

**Files:**
- Create: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/model/Addon.kt`
- Create: `composeApp/src/commonMain/composeResources/files/addons.json`
- Create: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/repository/AddonRepository.kt`
- Modify: `composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/data/repository/RepositoryParsingTest.kt`

- [ ] **Step 1: Write the failing test** — add `addonDeserializes` to `RepositoryParsingTest.kt`

```kotlin
@Test
fun addonDeserializes() {
    val json = """[{
        "id": "tbc_anniversary",
        "name": "TBC Anniversary",
        "description": "The Burning Crusade Anniversary",
        "iconName": "achievement_arena_2v2_7",
        "specPoolId": "tbc",
        "classPoolId": "tbc",
        "hasData": true
    }]"""
    val result = parseAddons(json)
    assertEquals(1, result.size)
    assertEquals("tbc_anniversary", result[0].id)
    assertEquals("tbc", result[0].specPoolId)
}
```

- [ ] **Step 2: Run test — confirm it fails with `Unresolved reference: parseAddons`**

```bash
./gradlew :composeApp:jvmTest --tests "net.tautellini.arenatactics.data.repository.RepositoryParsingTest.addonDeserializes"
```

- [ ] **Step 3: Create `data/model/Addon.kt`**

```kotlin
package net.tautellini.arenatactics.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Addon(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String,
    val specPoolId: String,
    val classPoolId: String,
    val hasData: Boolean
)
```

- [ ] **Step 4: Create `data/repository/AddonRepository.kt`**

```kotlin
package net.tautellini.arenatactics.data.repository

import arenatactics.composeapp.generated.resources.Res
import net.tautellini.arenatactics.data.model.Addon

internal fun parseAddons(jsonString: String): List<Addon> =
    appJson.decodeFromString(jsonString)

class AddonRepository {
    suspend fun getAll(): List<Addon> {
        val bytes = Res.readBytes("files/addons.json")
        return parseAddons(bytes.decodeToString())
    }

    suspend fun getById(id: String): Addon? = getAll().find { it.id == id }
}
```

- [ ] **Step 5: Create `composeResources/files/addons.json`**

```json
[
  {
    "id": "tbc_anniversary",
    "name": "TBC Anniversary",
    "description": "The Burning Crusade Anniversary",
    "iconName": "achievement_arena_2v2_7",
    "specPoolId": "tbc",
    "classPoolId": "tbc",
    "hasData": true
  }
]
```

- [ ] **Step 6: Run test — confirm it passes**

```bash
./gradlew :composeApp:jvmTest --tests "net.tautellini.arenatactics.data.repository.RepositoryParsingTest.addonDeserializes"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/model/Addon.kt \
        composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/repository/AddonRepository.kt \
        composeApp/src/commonMain/composeResources/files/addons.json \
        composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/data/repository/RepositoryParsingTest.kt
git commit -m "feat: add Addon model, AddonRepository, and seed data"
```

---

## Task 2: GameMode model migration + GameModeRepository.getByAddon

**Files:**
- Create: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/model/GameMode.kt`
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/model/Models.kt`
- Modify: `composeApp/src/commonMain/composeResources/files/game_modes.json`
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/repository/GameModeRepository.kt`
- Modify: `composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/data/repository/RepositoryParsingTest.kt`

> **Note:** We add `addonId` to `GameMode` now but keep `specPoolId`/`classPoolId` temporarily so existing ViewModels still compile. They are removed in Task 15 once all ViewModels migrate to using the Addon.

- [ ] **Step 1: Write the failing test** — update `gameModeDeserializes` in `RepositoryParsingTest.kt` to use the new shape with `addonId`

```kotlin
@Test
fun gameModeDeserializes() {
    val json = """[{
        "id": "tbc_anniversary_2v2",
        "name": "TBC 2v2",
        "description": "desc",
        "teamSize": 2,
        "addonId": "tbc_anniversary",
        "specPoolId": "tbc",
        "classPoolId": "tbc",
        "compositionSetId": "tbc_2v2",
        "iconName": "achievement_arena_2v2_7",
        "hasData": true
    }]"""
    val result = parseGameModes(json)
    assertEquals(1, result.size)
    assertEquals("tbc_anniversary_2v2", result[0].id)
    assertEquals("tbc_anniversary", result[0].addonId)
    assertEquals(2, result[0].teamSize)
}
```

- [ ] **Step 2: Run test — confirm it fails** (GameMode has no `addonId` field yet)

```bash
./gradlew :composeApp:jvmTest --tests "net.tautellini.arenatactics.data.repository.RepositoryParsingTest.gameModeDeserializes"
```

- [ ] **Step 3: Create `data/model/GameMode.kt`** — `addonId` added, `specPoolId`/`classPoolId` kept for now

```kotlin
package net.tautellini.arenatactics.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GameMode(
    val id: String,
    val name: String,
    val description: String,
    val teamSize: Int,
    val addonId: String,
    val specPoolId: String,
    val classPoolId: String,
    val compositionSetId: String,
    val iconName: String,
    val hasData: Boolean
)
```

- [ ] **Step 4: Remove `GameMode` from `Models.kt`** — delete lines 6–16 (the `GameMode` data class block). The other models (`WowClass`, `WowSpec`, etc.) stay in `Models.kt`.

- [ ] **Step 5: Update `game_modes.json`** — add `addonId` to all three entries

```json
[
  {
    "id": "tbc_anniversary_2v2",
    "name": "TBC Anniversary",
    "description": "2v2 Arena",
    "teamSize": 2,
    "addonId": "tbc_anniversary",
    "specPoolId": "tbc",
    "classPoolId": "tbc",
    "compositionSetId": "tbc_2v2",
    "iconName": "achievement_arena_2v2_7",
    "hasData": true
  },
  {
    "id": "tbc_anniversary_3v3",
    "name": "TBC Anniversary",
    "description": "3v3 Arena",
    "teamSize": 3,
    "addonId": "tbc_anniversary",
    "specPoolId": "tbc",
    "classPoolId": "tbc",
    "compositionSetId": "tbc_2v2",
    "iconName": "achievement_arena_3v3_7",
    "hasData": false
  },
  {
    "id": "tbc_anniversary_5v5",
    "name": "TBC Anniversary",
    "description": "5v5 Arena",
    "teamSize": 5,
    "addonId": "tbc_anniversary",
    "specPoolId": "tbc",
    "classPoolId": "tbc",
    "compositionSetId": "tbc_2v2",
    "iconName": "achievement_arena_5v5_7",
    "hasData": false
  }
]
```

- [ ] **Step 6: Add `getByAddon` to `GameModeRepository.kt`**

```kotlin
suspend fun getByAddon(addonId: String): List<GameMode> =
    getAll().filter { it.addonId == addonId }
```

- [ ] **Step 7: Run test — confirm it passes**

```bash
./gradlew :composeApp:jvmTest --tests "net.tautellini.arenatactics.data.repository.RepositoryParsingTest.gameModeDeserializes"
```

- [ ] **Step 8: Run all tests to confirm nothing else broke**

```bash
./gradlew :composeApp:jvmTest
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 9: Commit**

```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/model/GameMode.kt \
        composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/model/Models.kt \
        composeApp/src/commonMain/composeResources/files/game_modes.json \
        composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/repository/GameModeRepository.kt \
        composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/data/repository/RepositoryParsingTest.kt
git commit -m "feat: extract GameMode model, add addonId field, add getByAddon"
```

---

## Task 3: GearRepository — add getGearForSpec

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/repository/GearRepository.kt`

> Add `getGearForSpec(classId)` now. `getGearForComposition` and the `CompositionRepository` constructor param are removed in Task 15 once `GearViewModel` (the only caller) is deleted.

- [ ] **Step 1: Add `getGearForSpec` to `GearRepository`** — delegates to existing `loadPhasesForClass`

In `GearRepository.kt`, add after the existing `getGearForComposition` method:

```kotlin
suspend fun getGearForSpec(classId: String): List<GearPhase> =
    loadPhasesForClass(classId)
```

- [ ] **Step 2: Run all tests to confirm nothing broke**

```bash
./gradlew :composeApp:jvmTest
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/repository/GearRepository.kt
git commit -m "feat: add GearRepository.getGearForSpec(classId)"
```

---

## Task 4: Screen.kt rewrite + App.kt skeleton + ScreenNavigationTest

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/navigation/Screen.kt`
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/App.kt`
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/components/AppHeader.kt`
- Modify: `composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/navigation/ScreenNavigationTest.kt`

> After rewriting Screen.kt, App.kt and AppHeader.kt will fail to compile. We update them to a compilable skeleton in this task. NavHost destinations are stubs — filled in Tasks 5–14.

- [ ] **Step 1: Rewrite `ScreenNavigationTest.kt`** with tests for the new routes (write before implementation)

```kotlin
package net.tautellini.arenatactics.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ScreenNavigationTest {

    // ─── fromPath ────────────────────────────────────────────────────────────

    @Test fun fromPathRoot() =
        assertEquals(Screen.AddonSelection, Screen.fromPath("/"))

    @Test fun fromPathAddonHub() =
        assertEquals(Screen.AddonHub("tbc_anniversary"), Screen.fromPath("/tbc_anniversary"))

    @Test fun fromPathGameModeSelection() =
        assertEquals(Screen.GameModeSelection("tbc_anniversary"), Screen.fromPath("/tbc_anniversary/tactics"))

    @Test fun fromPathCompositionSelection() =
        assertEquals(
            Screen.CompositionSelection("tbc_anniversary", "tbc_anniversary_2v2"),
            Screen.fromPath("/tbc_anniversary/tactics/tbc_anniversary_2v2")
        )

    @Test fun fromPathMatchupList() =
        assertEquals(
            Screen.MatchupList("tbc_anniversary", "tbc_anniversary_2v2", "mage_rogue"),
            Screen.fromPath("/tbc_anniversary/tactics/tbc_anniversary_2v2/mage_rogue/matchups")
        )

    @Test fun fromPathMatchupDetail() =
        assertEquals(
            Screen.MatchupDetail("tbc_anniversary", "tbc_anniversary_2v2", "mage_rogue", "some_matchup"),
            Screen.fromPath("/tbc_anniversary/tactics/tbc_anniversary_2v2/mage_rogue/matchups/some_matchup")
        )

    @Test fun fromPathClassGuideList() =
        assertEquals(Screen.ClassGuideList("tbc_anniversary"), Screen.fromPath("/tbc_anniversary/guides"))

    @Test fun fromPathSpecGuide() =
        assertEquals(
            Screen.SpecGuide("tbc_anniversary", "druid", "druid_restoration"),
            Screen.fromPath("/tbc_anniversary/guides/druid/druid_restoration")
        )

    @Test fun fromPathUnknownReturnsAddonSelection() =
        assertEquals(Screen.AddonSelection, Screen.fromPath("/unknown/route/that/has/many/segments"))

    // ─── buildStack ──────────────────────────────────────────────────────────

    @Test fun buildStackAddonSelectionIsSingle() {
        val stack = Screen.buildStack(Screen.AddonSelection)
        assertEquals(1, stack.size)
        assertIs<Screen.AddonSelection>(stack[0])
    }

    @Test fun buildStackAddonHub() {
        val screen = Screen.AddonHub("tbc_anniversary")
        val stack = Screen.buildStack(screen)
        assertEquals(2, stack.size)
        assertIs<Screen.AddonSelection>(stack[0])
        assertEquals(screen, stack[1])
    }

    @Test fun buildStackGameModeSelection() {
        val screen = Screen.GameModeSelection("tbc_anniversary")
        val stack = Screen.buildStack(screen)
        assertEquals(3, stack.size)
        assertIs<Screen.AddonSelection>(stack[0])
        assertEquals(Screen.AddonHub("tbc_anniversary"), stack[1])
        assertEquals(screen, stack[2])
    }

    @Test fun buildStackCompositionSelection() {
        val screen = Screen.CompositionSelection("tbc_anniversary", "tbc_anniversary_2v2")
        val stack = Screen.buildStack(screen)
        assertEquals(4, stack.size)
        assertEquals(Screen.GameModeSelection("tbc_anniversary"), stack[2])
        assertEquals(screen, stack[3])
    }

    @Test fun buildStackMatchupList() {
        val screen = Screen.MatchupList("tbc_anniversary", "tbc_anniversary_2v2", "mage_rogue")
        val stack = Screen.buildStack(screen)
        assertEquals(5, stack.size)
        assertEquals(screen, stack[4])
    }

    @Test fun buildStackMatchupDetail() {
        val screen = Screen.MatchupDetail("tbc_anniversary", "tbc_anniversary_2v2", "mage_rogue", "m1")
        val stack = Screen.buildStack(screen)
        assertEquals(6, stack.size)
        assertEquals(Screen.MatchupList("tbc_anniversary", "tbc_anniversary_2v2", "mage_rogue"), stack[4])
        assertEquals(screen, stack[5])
    }

    @Test fun buildStackClassGuideList() {
        val screen = Screen.ClassGuideList("tbc_anniversary")
        val stack = Screen.buildStack(screen)
        assertEquals(3, stack.size)
        assertEquals(Screen.AddonHub("tbc_anniversary"), stack[1])
        assertEquals(screen, stack[2])
    }

    @Test fun buildStackSpecGuide() {
        val screen = Screen.SpecGuide("tbc_anniversary", "druid", "druid_restoration")
        val stack = Screen.buildStack(screen)
        assertEquals(4, stack.size)
        assertEquals(Screen.ClassGuideList("tbc_anniversary"), stack[2])
        assertEquals(screen, stack[3])
    }

    // ─── path round-trip ─────────────────────────────────────────────────────

    @Test fun pathRoundTrip() {
        val screens = listOf(
            Screen.AddonSelection,
            Screen.AddonHub("tbc_anniversary"),
            Screen.GameModeSelection("tbc_anniversary"),
            Screen.CompositionSelection("tbc_anniversary", "tbc_anniversary_2v2"),
            Screen.MatchupList("tbc_anniversary", "tbc_anniversary_2v2", "mage_rogue"),
            Screen.MatchupDetail("tbc_anniversary", "tbc_anniversary_2v2", "mage_rogue", "some_matchup"),
            Screen.ClassGuideList("tbc_anniversary"),
            Screen.SpecGuide("tbc_anniversary", "druid", "druid_restoration")
        )
        screens.forEach { screen ->
            assertEquals(screen, Screen.fromPath(screen.path), "round-trip failed for $screen")
        }
    }
}
```

- [ ] **Step 2: Run tests — confirm they fail to compile** (Screen.AddonSelection etc. don't exist)

```bash
./gradlew :composeApp:jvmTest --tests "net.tautellini.arenatactics.navigation.ScreenNavigationTest"
```

Expected: compile error mentioning `Screen.AddonSelection`

- [ ] **Step 3: Rewrite `Screen.kt`**

```kotlin
package net.tautellini.arenatactics.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable data object AddonSelection : Screen()
    @Serializable data class AddonHub(val addonId: String) : Screen()
    @Serializable data class GameModeSelection(val addonId: String) : Screen()
    @Serializable data class CompositionSelection(val addonId: String, val gameModeId: String) : Screen()
    @Serializable data class MatchupList(val addonId: String, val gameModeId: String, val compositionId: String) : Screen()
    @Serializable data class MatchupDetail(val addonId: String, val gameModeId: String, val compositionId: String, val matchupId: String) : Screen()
    @Serializable data class ClassGuideList(val addonId: String) : Screen()
    @Serializable data class SpecGuide(val addonId: String, val classId: String, val specId: String) : Screen()

    val path: String get() = when (this) {
        is AddonSelection      -> "/"
        is AddonHub            -> "/$addonId"
        is GameModeSelection   -> "/$addonId/tactics"
        is CompositionSelection -> "/$addonId/tactics/$gameModeId"
        is MatchupList         -> "/$addonId/tactics/$gameModeId/$compositionId/matchups"
        is MatchupDetail       -> "/$addonId/tactics/$gameModeId/$compositionId/matchups/$matchupId"
        is ClassGuideList      -> "/$addonId/guides"
        is SpecGuide           -> "/$addonId/guides/$classId/$specId"
    }

    companion object {
        fun fromPath(pathname: String): Screen {
            val segs = pathname.trim('/').split('/').filter { it.isNotEmpty() }
            val addonId = segs.getOrNull(0) ?: return AddonSelection
            // Guard: if addonId looks like an old-style "modes" anchor, treat as home
            if (addonId == "modes") return AddonSelection
            return when (val section = segs.getOrNull(1)) {
                null      -> AddonHub(addonId)
                "tactics" -> {
                    val modeId = segs.getOrNull(2) ?: return GameModeSelection(addonId)
                    val compId = segs.getOrNull(3) ?: return CompositionSelection(addonId, modeId)
                    if (segs.getOrNull(4) != "matchups") return CompositionSelection(addonId, modeId)
                    val matchupId = segs.getOrNull(5)
                    if (matchupId != null) MatchupDetail(addonId, modeId, compId, matchupId)
                    else MatchupList(addonId, modeId, compId)
                }
                "guides"  -> {
                    val classId = segs.getOrNull(2) ?: return ClassGuideList(addonId)
                    val specId  = segs.getOrNull(3) ?: return ClassGuideList(addonId)
                    SpecGuide(addonId, classId, specId)
                }
                else      -> AddonSelection
            }
        }

        fun buildStack(screen: Screen): List<Screen> = when (screen) {
            is AddonSelection    -> listOf(screen)
            is AddonHub          -> listOf(AddonSelection, screen)
            is GameModeSelection -> listOf(AddonSelection, AddonHub(screen.addonId), screen)
            is CompositionSelection -> listOf(AddonSelection, AddonHub(screen.addonId), GameModeSelection(screen.addonId), screen)
            is MatchupList       -> listOf(AddonSelection, AddonHub(screen.addonId), GameModeSelection(screen.addonId), CompositionSelection(screen.addonId, screen.gameModeId), screen)
            is MatchupDetail     -> listOf(AddonSelection, AddonHub(screen.addonId), GameModeSelection(screen.addonId), CompositionSelection(screen.addonId, screen.gameModeId), MatchupList(screen.addonId, screen.gameModeId, screen.compositionId), screen)
            is ClassGuideList    -> listOf(AddonSelection, AddonHub(screen.addonId), screen)
            is SpecGuide         -> listOf(AddonSelection, AddonHub(screen.addonId), ClassGuideList(screen.addonId), screen)
        }
    }
}

fun NavBackStackEntry.toScreen(): Screen {
    val route = destination.route ?: return Screen.AddonSelection
    return when {
        "AddonSelection"    in route -> Screen.AddonSelection
        "MatchupDetail"     in route -> toRoute<Screen.MatchupDetail>()
            .let { Screen.MatchupDetail(it.addonId, it.gameModeId, it.compositionId, it.matchupId) }
        "MatchupList"       in route -> toRoute<Screen.MatchupList>()
            .let { Screen.MatchupList(it.addonId, it.gameModeId, it.compositionId) }
        "CompositionSelection" in route -> toRoute<Screen.CompositionSelection>()
            .let { Screen.CompositionSelection(it.addonId, it.gameModeId) }
        "GameModeSelection" in route -> toRoute<Screen.GameModeSelection>()
            .let { Screen.GameModeSelection(it.addonId) }
        "ClassGuideList"    in route -> toRoute<Screen.ClassGuideList>()
            .let { Screen.ClassGuideList(it.addonId) }
        "SpecGuide"         in route -> toRoute<Screen.SpecGuide>()
            .let { Screen.SpecGuide(it.addonId, it.classId, it.specId) }
        "AddonHub"          in route -> toRoute<Screen.AddonHub>()
            .let { Screen.AddonHub(it.addonId) }
        else                         -> Screen.AddonSelection
    }
}
```

- [ ] **Step 4: Update `AppHeader.kt` to compile with the new Screen variants**

Replace the `breadcrumbLabel()` extension and fix the home tap reference:

```kotlin
// Replace the old breadcrumbLabel() at the bottom of AppHeader.kt:
private fun Screen.breadcrumbLabel(): String = when (this) {
    is Screen.AddonSelection    -> "Home"
    is Screen.AddonHub          -> addonId.formatId()
    is Screen.GameModeSelection -> "Tactics"
    is Screen.CompositionSelection -> gameModeId.formatId()
    is Screen.MatchupList       -> "Matchups"
    is Screen.MatchupDetail     -> "Detail"
    is Screen.ClassGuideList    -> "Class Guides"
    is Screen.SpecGuide         -> specId.formatId()
}
```

Also update the home tap on line 54 of `AppHeader.kt`:
```kotlin
// Change:  onNavigate(Screen.GameModeSelection)
// To:      onNavigate(Screen.AddonSelection)
Box(modifier = Modifier.clickable { onNavigate(Screen.AddonSelection) }) {
```

And in `App.kt` update the AppHeader visibility guard (search for `!is Screen.GameModeSelection`):
```kotlin
// Change:  currentScreen !is Screen.GameModeSelection
// To:      currentScreen !is Screen.AddonSelection
visible = currentScreen !is Screen.AddonSelection,
```

- [ ] **Step 5: Update `App.kt` to a compilable skeleton** — replace all NavHost composable destinations with stubs that use the new Screen types. Also update the deep-link init reference.

In `App.kt`, replace the `val currentScreen = ...` line and the deep-link block:
```kotlin
// Change all references to Screen.GameModeSelection (data object) → Screen.AddonSelection
// Deep-link init block:
if (initialScreen !is Screen.AddonSelection) {
    Screen.buildStack(initialScreen).drop(1).forEach { screen ->
        navController.navigate(screen)
    }
}
// currentScreen fallback:
val currentScreen = currentBackStackEntry?.toScreen() ?: Screen.AddonSelection
```

Replace the entire `NavHost { ... }` block with a skeleton that at minimum compiles. Keep `composable<Screen.AddonSelection>` as a placeholder, etc.:

```kotlin
NavHost(
    navController = navController,
    startDestination = Screen.AddonSelection,
    modifier = Modifier.weight(1f),
    enterTransition  = { fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.95f) },
    exitTransition   = { fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 1.05f) },
    popEnterTransition = { fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 1.05f) },
    popExitTransition  = { fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.95f) }
) {
    composable<Screen.AddonSelection> { /* TODO Task 5 */ }
    composable<Screen.AddonHub> { /* TODO Task 6 */ }
    composable<Screen.GameModeSelection> { /* TODO Task 7 */ }
    composable<Screen.CompositionSelection> { /* TODO Task 8 */ }
    composable<Screen.MatchupList> { /* TODO Task 9 */ }
    composable<Screen.MatchupDetail> { /* TODO Task 10 */ }
    composable<Screen.ClassGuideList> { /* TODO Task 11 */ }
    composable<Screen.SpecGuide> { /* TODO Task 12 */ }
}
```

Also update the `popBackStack` call in `onNavigate`:
```kotlin
if (target is Screen.AddonSelection) {
    navController.popBackStack<Screen.AddonSelection>(inclusive = false)
```

- [ ] **Step 6: Run ScreenNavigationTest — confirm all pass**

```bash
./gradlew :composeApp:jvmTest --tests "net.tautellini.arenatactics.navigation.ScreenNavigationTest"
```

Expected: `BUILD SUCCESSFUL`, all tests green.

- [ ] **Step 7: Run all tests**

```bash
./gradlew :composeApp:jvmTest
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: Commit**

```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/navigation/Screen.kt \
        composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/App.kt \
        composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/components/AppHeader.kt \
        composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/navigation/ScreenNavigationTest.kt
git commit -m "feat: rewrite Screen sealed class for addon navigation"
```

---

## Task 5: AddonSelectionViewModel + AddonSelectionScreen

**Files:**
- Create: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/AddonSelectionViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/AddonSelectionScreen.kt`

- [ ] **Step 1: Create `AddonSelectionViewModel.kt`**

```kotlin
package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.Addon
import net.tautellini.arenatactics.data.repository.AddonRepository

sealed class AddonSelectionState {
    data object Loading : AddonSelectionState()
    data class Success(val addons: List<Addon>) : AddonSelectionState()
    data class Error(val message: String) : AddonSelectionState()
}

class AddonSelectionViewModel(
    private val repository: AddonRepository
) : ViewModel() {
    private val _state = MutableStateFlow<AddonSelectionState>(AddonSelectionState.Loading)
    val state: StateFlow<AddonSelectionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                AddonSelectionState.Success(repository.getAll())
            } catch (e: Throwable) {
                AddonSelectionState.Error(e.message ?: "Failed to load addons")
            }
        }
    }
}
```

- [ ] **Step 2: Create `AddonSelectionScreen.kt`** — mirrors the look of the old `GameModeSelectionScreen` but shows addons

```kotlin
package net.tautellini.arenatactics.presentation.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import net.tautellini.arenatactics.data.model.Addon
import net.tautellini.arenatactics.data.model.WowheadIcons
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.AddonSelectionState
import net.tautellini.arenatactics.presentation.AddonSelectionViewModel
import net.tautellini.arenatactics.presentation.screens.components.ShieldCanvas
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun AddonSelectionScreen(
    viewModel: AddonSelectionViewModel,
    onNavigate: (Screen) -> Unit,
    shieldModifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(40.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            ShieldLogoBlock(modifier = shieldModifier)

            when (val s = state) {
                is AddonSelectionState.Loading -> CircularProgressIndicator(color = Primary)
                is AddonSelectionState.Error   -> Text(s.message, color = TextSecondary)
                is AddonSelectionState.Success -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Select your game",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            letterSpacing = 2.sp
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            s.addons.forEach { addon ->
                                AddonTile(addon) { onNavigate(Screen.AddonHub(addon.id)) }
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "Made with love for Kizaru",
            color = TextSecondary.copy(alpha = 0.5f),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
        )
    }
}

@Composable
private fun ShieldLogoBlock(modifier: Modifier = Modifier) {
    val cinzel = cinzelDecorative()
    val transition = rememberInfiniteTransition(label = "shield-shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer-x"
    )
    Box(contentAlignment = Alignment.Center) {
        ShieldCanvas(modifier = modifier.size(220.dp, 250.dp), shimmerX = shimmerX)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text("Arena", fontFamily = cinzel, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 26.sp, letterSpacing = 3.sp)
            Text("Tactics", fontFamily = cinzel, fontWeight = FontWeight.Normal, color = Primary, fontSize = 16.sp, letterSpacing = 5.sp)
        }
    }
}

@Composable
private fun AddonTile(addon: Addon, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .alpha(if (addon.hasData) 1f else 0.35f)
            .then(if (addon.hasData) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(4.dp)
    ) {
        AsyncImage(
            model = WowheadIcons.large(addon.iconName),
            contentDescription = addon.name,
            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp))
        )
        Text(addon.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(addon.description, color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}
```

- [ ] **Step 3: Wire `AddonSelectionScreen` into App.kt** — replace the `composable<Screen.AddonSelection> { /* TODO */ }` stub:

```kotlin
composable<Screen.AddonSelection> {
    val vm = viewModel { AddonSelectionViewModel(addonRepository) }
    val shieldMod = with(sharedScope) {
        Modifier.sharedElement(
            sharedContentState = rememberSharedContentState(key = "shield"),
            animatedVisibilityScope = this@composable
        )
    }
    AddonSelectionScreen(viewModel = vm, onNavigate = { navController.navigate(it) }, shieldModifier = shieldMod)
}
```

Also add `addonRepository` to the `App()` remembered repos block:
```kotlin
val addonRepository = remember { AddonRepository() }
```

And update the AppHeader's `sharedElement` composable to reference the correct AnimatedVisibility scope (search for `animatedVisibilityScope = animScope` and ensure it stays correct — no change needed here since the structure is the same).

- [ ] **Step 4: Run the app and verify the home screen shows "TBC Anniversary" as an addon tile**

```bash
./gradlew :composeApp:run
```

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/AddonSelectionViewModel.kt \
        composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/AddonSelectionScreen.kt \
        composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/App.kt
git commit -m "feat: AddonSelectionScreen — home shows addon tiles"
```

---

## Task 6: AddonHubViewModel + AddonHubScreen

**Files:**
- Create: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/AddonHubViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/AddonHubScreen.kt`

- [ ] **Step 1: Create `AddonHubViewModel.kt`**

```kotlin
package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.Addon
import net.tautellini.arenatactics.data.repository.AddonRepository

sealed class AddonHubState {
    data object Loading : AddonHubState()
    data class Success(val addon: Addon) : AddonHubState()
    data class Error(val message: String) : AddonHubState()
}

class AddonHubViewModel(
    private val addonId: String,
    private val addonRepository: AddonRepository
) : ViewModel() {
    private val _state = MutableStateFlow<AddonHubState>(AddonHubState.Loading)
    val state: StateFlow<AddonHubState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                val addon = addonRepository.getById(addonId)
                    ?: throw IllegalArgumentException("Unknown addon: $addonId")
                AddonHubState.Success(addon)
            } catch (e: Throwable) {
                AddonHubState.Error(e.message ?: "Failed to load addon")
            }
        }
    }
}
```

- [ ] **Step 2: Create `AddonHubScreen.kt`** — two large section tiles: Tactics and Class Guides

```kotlin
package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MenuBook
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.AddonHubState
import net.tautellini.arenatactics.presentation.AddonHubViewModel
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun AddonHubScreen(
    addonId: String,
    viewModel: AddonHubViewModel,
    onNavigate: (Screen) -> Unit
) {
    val state by viewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
        when (val s = state) {
            is AddonHubState.Loading -> CircularProgressIndicator(color = Primary)
            is AddonHubState.Error   -> Text(s.message, color = TextSecondary, modifier = Modifier.padding(24.dp))
            is AddonHubState.Success -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        s.addon.name,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "What are you looking for?",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SectionTile(
                            icon = Icons.Rounded.AutoAwesome,
                            title = "Tactics",
                            subtitle = "Compositions & matchup guides",
                            onClick = { onNavigate(Screen.GameModeSelection(addonId)) }
                        )
                        SectionTile(
                            icon = Icons.Rounded.MenuBook,
                            title = "Class Guides",
                            subtitle = "Best-in-slot gear per spec",
                            onClick = { onNavigate(Screen.ClassGuideList(addonId)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        color = CardColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .widthIn(min = 160.dp, max = 280.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(40.dp))
            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
    }
}
```

- [ ] **Step 3: Wire into App.kt** — replace the `AddonHub` stub:

```kotlin
composable<Screen.AddonHub> { entry ->
    val screen = entry.toRoute<Screen.AddonHub>()
    val vm = viewModel(key = screen.addonId) { AddonHubViewModel(screen.addonId, addonRepository) }
    AddonHubScreen(addonId = screen.addonId, viewModel = vm, onNavigate = { navController.navigate(it) })
}
```

- [ ] **Step 4: Run the app and verify tapping "TBC Anniversary" shows the section picker**

```bash
./gradlew :composeApp:run
```

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/AddonHubViewModel.kt \
        composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/AddonHubScreen.kt \
        composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/App.kt
git commit -m "feat: AddonHubScreen — Tactics / Class Guides section picker"
```

---

## Task 7: Tactics GameModeSelectionViewModel + GameModeSelectionScreen

**Files:**
- Create: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/GameModeSelectionViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/TacticsGameModeSelectionScreen.kt`

- [ ] **Step 1: Create `GameModeSelectionViewModel.kt`** (Tactics version — loads modes by addonId)

```kotlin
package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.GameMode
import net.tautellini.arenatactics.data.repository.GameModeRepository

sealed class GameModeSelectionState {
    data object Loading : GameModeSelectionState()
    data class Success(val modes: List<GameMode>) : GameModeSelectionState()
    data class Error(val message: String) : GameModeSelectionState()
}

class GameModeSelectionViewModel(
    private val addonId: String,
    private val repository: GameModeRepository
) : ViewModel() {
    private val _state = MutableStateFlow<GameModeSelectionState>(GameModeSelectionState.Loading)
    val state: StateFlow<GameModeSelectionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                GameModeSelectionState.Success(repository.getByAddon(addonId))
            } catch (e: Throwable) {
                GameModeSelectionState.Error(e.message ?: "Failed to load game modes")
            }
        }
    }
}
```

- [ ] **Step 2: Create `TacticsGameModeSelectionScreen.kt`** — same tile layout as old GameModeSelectionScreen but navigates to CompositionSelection with addonId

```kotlin
package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import net.tautellini.arenatactics.data.model.GameMode
import net.tautellini.arenatactics.data.model.WowheadIcons
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.GameModeSelectionState
import net.tautellini.arenatactics.presentation.GameModeSelectionViewModel
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun TacticsGameModeSelectionScreen(
    addonId: String,
    viewModel: GameModeSelectionViewModel,
    onNavigate: (Screen) -> Unit
) {
    val state by viewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text("Select your bracket", color = TextSecondary, fontSize = 13.sp, letterSpacing = 2.sp)
            when (val s = state) {
                is GameModeSelectionState.Loading -> CircularProgressIndicator(color = Primary)
                is GameModeSelectionState.Error   -> Text(s.message, color = TextSecondary)
                is GameModeSelectionState.Success -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        s.modes.forEach { mode ->
                            GameModeTile(mode) {
                                onNavigate(Screen.CompositionSelection(addonId, mode.id))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameModeTile(mode: GameMode, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .alpha(if (mode.hasData) 1f else 0.35f)
            .then(if (mode.hasData) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(4.dp)
    ) {
        AsyncImage(
            model = WowheadIcons.large(mode.iconName),
            contentDescription = "${mode.description}",
            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp))
        )
        Text(mode.description, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(mode.name, color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}
```

- [ ] **Step 3: Wire into App.kt** — replace the `GameModeSelection` stub:

```kotlin
composable<Screen.GameModeSelection> { entry ->
    val screen = entry.toRoute<Screen.GameModeSelection>()
    val vm = viewModel(key = screen.addonId) { GameModeSelectionViewModel(screen.addonId, gameModeRepository) }
    TacticsGameModeSelectionScreen(addonId = screen.addonId, viewModel = vm, onNavigate = { navController.navigate(it) })
}
```

- [ ] **Step 4: Run app and verify Tactics path shows 2v2/3v3/5v5**

```bash
./gradlew :composeApp:run
```

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/GameModeSelectionViewModel.kt \
        composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/TacticsGameModeSelectionScreen.kt \
        composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/App.kt
git commit -m "feat: Tactics game mode selection screen"
```

---

## Task 8: Update CompositionSelectionViewModel + Screen

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/CompositionSelectionViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/CompositionSelectionScreen.kt`

- [ ] **Step 1: Update `CompositionSelectionViewModel.kt`** — add `addonId`, source pool IDs from `AddonRepository`

```kotlin
class CompositionSelectionViewModel(
    private val addonId: String,
    private val gameModeId: String,
    private val addonRepository: AddonRepository,
    private val gameModeRepository: GameModeRepository,
    private val compositionRepository: CompositionRepository
) : ViewModel() {
    // ... same _state / state boilerplate ...

    init {
        viewModelScope.launch {
            _state.value = try {
                val addon = addonRepository.getById(addonId)
                    ?: throw IllegalArgumentException("Unknown addon: $addonId")
                val mode = gameModeRepository.getAll().first { it.id == gameModeId }
                val rich = compositionRepository.getRichCompositions(
                    specPoolId       = addon.specPoolId,
                    classPoolId      = addon.classPoolId,
                    compositionSetId = mode.compositionSetId,
                    teamSize         = mode.teamSize
                )
                val grouped = CompositionTier.entries
                    .associateWith { tier ->
                        rich.filter { it.composition.tier == tier }
                            .sortedWith(
                                compareByDescending<RichComposition> { it.composition.hasData }
                                    .thenBy { rc -> rc.specs.count { it.role == SpecRole.HEALER } }
                                    .thenBy { rc -> rc.specs.joinToString { it.name } }
                            )
                    }
                    .filterValues { it.isNotEmpty() }
                CompositionSelectionState.Success(grouped)
            } catch (e: Throwable) {
                CompositionSelectionState.Error(e.message ?: "Failed to load compositions")
            }
        }
    }
}
```

- [ ] **Step 2: Update `CompositionSelectionScreen.kt`** — add `addonId` param, navigate to `MatchupList` instead of `GearView`

Change the function signature to accept `addonId`:
```kotlin
fun CompositionSelectionScreen(
    addonId: String,
    gameModeId: String,
    viewModel: CompositionSelectionViewModel,
    onNavigate: (Screen) -> Unit
)
```

Update the two `onNavigate` calls (in both the OTHERS and non-OTHERS branches) from:
```kotlin
// Old:
{ onNavigate(Screen.GearView(gameModeId, rich.composition.id)) }
// New:
{ onNavigate(Screen.MatchupList(addonId, gameModeId, rich.composition.id)) }
```

- [ ] **Step 3: Wire into App.kt** — replace the `CompositionSelection` stub:

```kotlin
composable<Screen.CompositionSelection> { entry ->
    val screen = entry.toRoute<Screen.CompositionSelection>()
    val vm = viewModel(key = "${screen.addonId}_${screen.gameModeId}") {
        CompositionSelectionViewModel(
            screen.addonId, screen.gameModeId,
            addonRepository, gameModeRepository, compositionRepository
        )
    }
    CompositionSelectionScreen(
        addonId = screen.addonId,
        gameModeId = screen.gameModeId,
        viewModel = vm,
        onNavigate = { navController.navigate(it) }
    )
}
```

- [ ] **Step 4: Run app and verify selecting 2v2 shows the comp grid and tapping a comp navigates to MatchupList (even though it's still a stub)**

```bash
./gradlew :composeApp:run
```

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/CompositionSelectionViewModel.kt \
        composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/CompositionSelectionScreen.kt \
        composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/App.kt
git commit -m "feat: update CompositionSelection for addon navigation"
```

---

## Task 9: Update MatchupListViewModel + MatchupListScreen

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/MatchupListViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/MatchupListScreen.kt`

- [ ] **Step 1: Update `MatchupListViewModel.kt`** — add `addonId`, source pool IDs from `AddonRepository`

```kotlin
class MatchupListViewModel(
    private val addonId: String,
    private val gameModeId: String,
    private val compositionId: String,
    private val addonRepository: AddonRepository,
    private val compositionRepository: CompositionRepository,
    private val matchupRepository: MatchupRepository
) : ViewModel() {
    // ... _state / state boilerplate unchanged ...

    init {
        viewModelScope.launch {
            _state.value = try {
                val addon    = addonRepository.getById(addonId)
                    ?: throw IllegalArgumentException("Unknown addon: $addonId")
                val specs    = compositionRepository.getSpecs(addon.specPoolId)
                val specMap  = specs.associateBy { it.id }
                val classes  = compositionRepository.getClasses(addon.classPoolId)
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

Remove `gameModeRepository` from constructor — it is no longer needed.

- [ ] **Step 2: Update `MatchupListScreen.kt`** — add `addonId` param; update the `MatchupDetail` navigation call

Change function signature:
```kotlin
fun MatchupListScreen(
    addonId: String,
    gameModeId: String,
    compositionId: String,
    viewModel: MatchupListViewModel,
    onNavigate: (Screen) -> Unit
)
```

Update the `onNavigate` call in the matchup `Surface.clickable`:
```kotlin
// Old: Screen.MatchupDetail(gameModeId, compositionId, matchup.id)
// New:
onNavigate(Screen.MatchupDetail(addonId, gameModeId, compositionId, matchup.id))
```

- [ ] **Step 3: Wire into App.kt** — replace the `MatchupList` stub:

```kotlin
composable<Screen.MatchupList> { entry ->
    val screen = entry.toRoute<Screen.MatchupList>()
    val vm = viewModel(key = "matchups_${screen.addonId}_${screen.compositionId}") {
        MatchupListViewModel(
            screen.addonId, screen.gameModeId, screen.compositionId,
            addonRepository, compositionRepository, matchupRepository
        )
    }
    MatchupListScreen(
        addonId = screen.addonId,
        gameModeId = screen.gameModeId,
        compositionId = screen.compositionId,
        viewModel = vm,
        onNavigate = { navController.navigate(it) }
    )
}
```

- [ ] **Step 4: Run app and verify the matchup list is reachable and shows matchup cards**

```bash
./gradlew :composeApp:run
```

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/MatchupListViewModel.kt \
        composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/MatchupListScreen.kt \
        composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/App.kt
git commit -m "feat: update MatchupList for addon navigation"
```

---

## Task 10: Update MatchupDetailViewModel + wire into App.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/MatchupDetailViewModel.kt`

- [ ] **Step 1: Update `MatchupDetailViewModel.kt`** — add `addonId`, source pool IDs from `AddonRepository`

```kotlin
class MatchupDetailViewModel(
    private val addonId: String,
    private val gameModeId: String,
    private val compositionId: String,
    private val matchupId: String,
    private val addonRepository: AddonRepository,
    private val compositionRepository: CompositionRepository,
    private val matchupRepository: MatchupRepository
) : ViewModel() {
    // ... _state / state boilerplate unchanged ...

    init {
        viewModelScope.launch {
            _state.value = try {
                val addon    = addonRepository.getById(addonId)
                    ?: throw IllegalArgumentException("Unknown addon: $addonId")
                val specs    = compositionRepository.getSpecs(addon.specPoolId)
                val specMap  = specs.associateBy { it.id }
                val classes  = compositionRepository.getClasses(addon.classPoolId)
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

Remove `gameModeRepository` from constructor.

- [ ] **Step 2: Wire into App.kt** — replace the `MatchupDetail` stub:

```kotlin
composable<Screen.MatchupDetail> { entry ->
    val screen = entry.toRoute<Screen.MatchupDetail>()
    val vm = viewModel(key = screen.matchupId) {
        MatchupDetailViewModel(
            screen.addonId, screen.gameModeId, screen.compositionId, screen.matchupId,
            addonRepository, compositionRepository, matchupRepository
        )
    }
    MatchupDetailScreen(viewModel = vm)
}
```

- [ ] **Step 3: Run app and verify the full Tactics path works end-to-end**

```bash
./gradlew :composeApp:run
```

Navigate: Home → TBC Anniversary → Tactics → 2v2 → Rogue/Priest → a matchup → matchup detail. Verify the back stack works correctly.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/MatchupDetailViewModel.kt \
        composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/App.kt
git commit -m "feat: update MatchupDetail for addon navigation — Tactics path complete"
```

---

## Task 11: ClassGuideListViewModel + ClassGuideListScreen

**Files:**
- Create: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/ClassGuideListViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/ClassGuideListScreen.kt`

- [ ] **Step 1: Create `ClassGuideListViewModel.kt`**

```kotlin
package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.SpecRole
import net.tautellini.arenatactics.data.model.WowClass
import net.tautellini.arenatactics.data.model.WowSpec
import net.tautellini.arenatactics.data.repository.AddonRepository
import net.tautellini.arenatactics.data.repository.CompositionRepository

sealed class ClassGuideListState {
    data object Loading : ClassGuideListState()
    data class Success(
        val specs: List<WowSpec>,
        val classMap: Map<String, WowClass>
    ) : ClassGuideListState()
    data class Error(val message: String) : ClassGuideListState()
}

class ClassGuideListViewModel(
    private val addonId: String,
    private val addonRepository: AddonRepository,
    private val compositionRepository: CompositionRepository
) : ViewModel() {
    private val _state = MutableStateFlow<ClassGuideListState>(ClassGuideListState.Loading)
    val state: StateFlow<ClassGuideListState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                val addon   = addonRepository.getById(addonId)
                    ?: throw IllegalArgumentException("Unknown addon: $addonId")
                val specs   = compositionRepository.getSpecs(addon.specPoolId)
                    .sortedWith(compareBy({ if (it.role == SpecRole.DPS) 0 else 1 }, { it.name }))
                val classes = compositionRepository.getClasses(addon.classPoolId)
                val classMap = classes.associateBy { it.id }
                ClassGuideListState.Success(specs, classMap)
            } catch (e: Throwable) {
                ClassGuideListState.Error(e.message ?: "Failed to load class guides")
            }
        }
    }
}
```

- [ ] **Step 2: Create `ClassGuideListScreen.kt`** — adaptive grid of spec badges, clickable

```kotlin
package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.data.model.WowSpec
import net.tautellini.arenatactics.data.model.WowClass
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.ClassGuideListState
import net.tautellini.arenatactics.presentation.ClassGuideListViewModel
import net.tautellini.arenatactics.presentation.screens.components.SpecBadge
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun ClassGuideListScreen(
    addonId: String,
    viewModel: ClassGuideListViewModel,
    onNavigate: (Screen) -> Unit
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Background).padding(24.dp)) {
        Text(
            "Class Guides",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        when (val s = state) {
            is ClassGuideListState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            is ClassGuideListState.Error   -> Text(s.message, color = TextSecondary)
            is ClassGuideListState.Success -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(s.specs) { spec ->
                        val wowClass = s.classMap[spec.classId]
                        SpecGuideCard(spec = spec, wowClass = wowClass) {
                            onNavigate(Screen.SpecGuide(addonId, spec.classId, spec.id))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecGuideCard(spec: WowSpec, wowClass: WowClass?, onClick: () -> Unit) {
    Surface(
        color = CardColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            if (wowClass != null) {
                SpecBadge(spec = spec, wowClass = wowClass)
            }
            Text(
                spec.name,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (wowClass != null) {
                Text(wowClass.name, color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}
```

- [ ] **Step 3: Wire into App.kt** — replace the `ClassGuideList` stub:

```kotlin
composable<Screen.ClassGuideList> { entry ->
    val screen = entry.toRoute<Screen.ClassGuideList>()
    val vm = viewModel(key = "guides_${screen.addonId}") {
        ClassGuideListViewModel(screen.addonId, addonRepository, compositionRepository)
    }
    ClassGuideListScreen(addonId = screen.addonId, viewModel = vm, onNavigate = { navController.navigate(it) })
}
```

- [ ] **Step 4: Run app and verify Class Guides shows an adaptive grid of spec cards**

```bash
./gradlew :composeApp:run
```

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/ClassGuideListViewModel.kt \
        composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/ClassGuideListScreen.kt \
        composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/App.kt
git commit -m "feat: ClassGuideListScreen — adaptive spec grid"
```

---

## Task 12: SpecGuideViewModel + SpecGuideScreen

**Files:**
- Create: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/SpecGuideViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/SpecGuideScreen.kt`

- [ ] **Step 1: Create `SpecGuideViewModel.kt`**

```kotlin
package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.GearPhase
import net.tautellini.arenatactics.data.model.WowClass
import net.tautellini.arenatactics.data.model.WowSpec
import net.tautellini.arenatactics.data.repository.AddonRepository
import net.tautellini.arenatactics.data.repository.CompositionRepository
import net.tautellini.arenatactics.data.repository.GearRepository
import net.tautellini.arenatactics.data.repository.SpecRepository

sealed class SpecGuideState {
    data object Loading : SpecGuideState()
    data class Success(
        val spec: WowSpec,
        val wowClass: WowClass,
        val phases: List<GearPhase>
    ) : SpecGuideState()
    data class Error(val message: String) : SpecGuideState()
}

class SpecGuideViewModel(
    private val addonId: String,
    private val classId: String,
    private val specId: String,
    private val addonRepository: AddonRepository,
    private val specRepository: SpecRepository,
    private val compositionRepository: CompositionRepository,
    private val gearRepository: GearRepository
) : ViewModel() {
    private val _state = MutableStateFlow<SpecGuideState>(SpecGuideState.Loading)
    val state: StateFlow<SpecGuideState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                val addon    = addonRepository.getById(addonId)
                    ?: throw IllegalArgumentException("Unknown addon: $addonId")
                val spec     = specRepository.getById(addon.specPoolId, specId)
                    ?: throw IllegalArgumentException("Unknown spec: $specId")
                val classes  = compositionRepository.getClasses(addon.classPoolId)
                val wowClass = classes.find { it.id == classId }
                    ?: throw IllegalArgumentException("Unknown class: $classId")
                val phases   = gearRepository.getGearForSpec(classId)
                SpecGuideState.Success(spec, wowClass, phases)
            } catch (e: Throwable) {
                SpecGuideState.Error(e.message ?: "Failed to load spec guide")
            }
        }
    }
}
```

- [ ] **Step 2: Create `SpecGuideScreen.kt`** — reuses the paper-doll gear display from `GearScreen.kt`

The `SpecGuideScreen` renders the same `PaperDoll`, `GearSlot`, `EmptyGearSlot`, slot constants, and `mapItemsToSlots` helper that currently live in `GearScreen.kt`. Copy these private declarations into `SpecGuideScreen.kt` (they will be deleted with `GearScreen.kt` in Task 15).

```kotlin
package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.data.model.GearItem
import net.tautellini.arenatactics.data.model.GearPhase
import net.tautellini.arenatactics.openUrl
import net.tautellini.arenatactics.presentation.SpecGuideState
import net.tautellini.arenatactics.presentation.SpecGuideViewModel
import net.tautellini.arenatactics.presentation.screens.components.GearIcon
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun SpecGuideScreen(viewModel: SpecGuideViewModel) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is SpecGuideState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        is SpecGuideState.Error -> Text(s.message, color = TextSecondary, modifier = Modifier.padding(24.dp))
        is SpecGuideState.Success -> {
            var selectedPhase by remember { mutableStateOf(1) }
            val availablePhases = remember(s) { s.phases.map { it.phase }.sorted().ifEmpty { listOf(1) } }

            Column(modifier = Modifier.fillMaxSize().background(Background)) {
                // Spec header
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GearIcon(
                        url = "https://wow.zamimg.com/images/wow/icons/large/classicon_${s.wowClass.id}.jpg",
                        contentDescription = s.wowClass.name,
                        modifier = Modifier.size(48.dp),
                        cornerRadius = 8.dp,
                        borderColor = classColor(s.wowClass.id),
                        borderWidth = 2.dp
                    )
                    Column {
                        Text(s.spec.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(s.wowClass.name, color = TextSecondary, fontSize = 13.sp)
                    }
                }

                // Phase tabs
                Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                    availablePhases.forEach { phase ->
                        val selected = phase == selectedPhase
                        Box(modifier = Modifier.clickable { selectedPhase = phase }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                "Phase $phase",
                                color = if (selected) Primary else TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
                HorizontalDivider(color = DividerColor)

                // Paper doll
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val phase = s.phases.find { it.phase == selectedPhase } ?: s.phases.firstOrNull()
                    if (phase != null) {
                        PaperDoll(
                            classId = s.wowClass.id,
                            className = s.wowClass.name,
                            phase = phase,
                            modifier = Modifier.widthIn(min = 280.dp, max = 420.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Slot ordering ───────────────────────────────────────────────────────────

private val LEFT_SLOTS   = listOf("Head", "Neck", "Shoulders", "Back", "Chest", "Wrists")
private val RIGHT_SLOTS  = listOf("Hands", "Waist", "Legs", "Feet", "Ring", "Ring")
private val BOTTOM_SLOTS = listOf("Trinket", "Trinket", "Main Hand", "Off Hand", "Ranged")

private fun normalizeSlot(s: String) = if (s == "Wand") "Ranged" else s

private fun mapItemsToSlots(items: List<GearItem>, slotList: List<String>): List<GearItem?> {
    val remaining = items.toMutableList()
    return slotList.map { slot ->
        val idx = remaining.indexOfFirst { normalizeSlot(it.slot) == slot }
        if (idx >= 0) remaining.removeAt(idx) else null
    }
}

// ─── PaperDoll ───────────────────────────────────────────────────────────────

@Composable
private fun PaperDoll(classId: String, className: String, phase: GearPhase, modifier: Modifier = Modifier) {
    val leftItems   = remember(phase) { mapItemsToSlots(phase.items, LEFT_SLOTS) }
    val rightItems  = remember(phase) { mapItemsToSlots(phase.items, RIGHT_SLOTS) }
    val bottomItems = remember(phase) { mapItemsToSlots(phase.items, BOTTOM_SLOTS) }

    Surface(color = CardColor, shape = RoundedCornerShape(12.dp), modifier = modifier.padding(4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    leftItems.zip(LEFT_SLOTS).forEach { (item, slot) ->
                        if (item != null) GearSlot(item) else EmptyGearSlot(slot)
                    }
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    GearIcon(
                        url = "https://wow.zamimg.com/images/wow/icons/large/classicon_$classId.jpg",
                        contentDescription = className,
                        modifier = Modifier.size(72.dp),
                        cornerRadius = 8.dp,
                        borderColor = classColor(classId),
                        borderWidth = 2.dp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(className, color = classColor(classId), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    rightItems.zip(RIGHT_SLOTS).forEach { (item, slot) ->
                        if (item != null) GearSlot(item) else EmptyGearSlot(slot)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                bottomItems.zip(BOTTOM_SLOTS).forEach { (item, slot) ->
                    if (item != null) GearSlot(item, modifier = Modifier.weight(1f))
                    else EmptyGearSlot(slot, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GearSlot(item: GearItem, modifier: Modifier = Modifier) {
    val wowheadUrl = "https://www.wowhead.com/tbc/item=${item.wowheadId}"
    Column(
        modifier = modifier.widthIn(min = 60.dp, max = 80.dp).clip(RoundedCornerShape(8.dp)).background(Surface).clickable { openUrl(wowheadUrl) }.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GearIcon(url = "https://wow.zamimg.com/images/wow/icons/medium/${item.icon}.jpg", contentDescription = item.name, modifier = Modifier.size(48.dp), cornerRadius = 6.dp, wowheadItemId = item.wowheadId)
        Text(item.name, color = Primary, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, lineHeight = 12.sp)
        if (item.enchant != null) Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(10.dp), tint = Primary)
    }
}

@Composable
private fun EmptyGearSlot(slotName: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.widthIn(min = 60.dp, max = 80.dp).clip(RoundedCornerShape(8.dp)).background(CardElevated).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(Surface), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.HelpOutline, contentDescription = null, modifier = Modifier.size(24.dp), tint = TextSecondary)
        }
        Text(slotName, color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}
```

- [ ] **Step 3: Wire into App.kt** — replace the `SpecGuide` stub:

```kotlin
composable<Screen.SpecGuide> { entry ->
    val screen = entry.toRoute<Screen.SpecGuide>()
    val vm = viewModel(key = "spec_${screen.addonId}_${screen.specId}") {
        SpecGuideViewModel(
            screen.addonId, screen.classId, screen.specId,
            addonRepository, specRepository, compositionRepository, gearRepository
        )
    }
    SpecGuideScreen(viewModel = vm)
}
```

Also add `specRepository` to the `App()` remembered repos block (it is currently created inside `compositionRepository`'s constructor — make it an explicit top-level `remember` and pass it to both):
```kotlin
val specRepository = remember { SpecRepository() }
val compositionRepository = remember { CompositionRepository(specRepository) }
// ... rest unchanged
```

- [ ] **Step 4: Run app and verify Class Guides path works end-to-end**

```bash
./gradlew :composeApp:run
```

Navigate: Home → TBC Anniversary → Class Guides → pick a spec → verify gear phases show.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/SpecGuideViewModel.kt \
        composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/SpecGuideScreen.kt \
        composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/App.kt
git commit -m "feat: SpecGuideScreen — per-spec gear guide with paper doll"
```

---

## Task 13: Final cleanup — remove dead code and finalize GameMode model

**Files:**
- Delete: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/GearViewModel.kt`
- Delete: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/GameModeSelectionViewModel.kt` (old one)
- Delete: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/GearScreen.kt`
- Delete: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/GameModeSelectionScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/model/GameMode.kt`
- Modify: `composeApp/src/commonMain/composeResources/files/game_modes.json`
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/repository/GearRepository.kt`
- Modify: `composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/data/repository/RepositoryParsingTest.kt`

- [ ] **Step 1: Remove `specPoolId` and `classPoolId` from `GameMode.kt`**

```kotlin
@Serializable
data class GameMode(
    val id: String,
    val name: String,
    val description: String,
    val teamSize: Int,
    val addonId: String,
    val compositionSetId: String,
    val iconName: String,
    val hasData: Boolean
)
```

- [ ] **Step 2: Remove `specPoolId` and `classPoolId` from `game_modes.json`**

Remove those keys from all three entries. The file should only have: `id`, `name`, `description`, `teamSize`, `addonId`, `compositionSetId`, `iconName`, `hasData`.

- [ ] **Step 3: Update `RepositoryParsingTest.gameModeDeserializes`** — remove `specPoolId` assertion and JSON field:

```kotlin
@Test
fun gameModeDeserializes() {
    val json = """[{
        "id": "tbc_anniversary_2v2",
        "name": "TBC 2v2",
        "description": "desc",
        "teamSize": 2,
        "addonId": "tbc_anniversary",
        "compositionSetId": "tbc_2v2",
        "iconName": "achievement_arena_2v2_7",
        "hasData": true
    }]"""
    val result = parseGameModes(json)
    assertEquals(1, result.size)
    assertEquals("tbc_anniversary_2v2", result[0].id)
    assertEquals("tbc_anniversary", result[0].addonId)
    assertEquals(2, result[0].teamSize)
}
```

- [ ] **Step 4: Remove `getGearForComposition` and the `CompositionRepository` constructor param from `GearRepository.kt`**

```kotlin
class GearRepository {
    suspend fun getGearForSpec(classId: String): List<GearPhase> =
        loadPhasesForClass(classId)

    private suspend fun loadPhasesForClass(classId: String): List<GearPhase> { ... }
    private suspend fun tryReadBytes(path: String): ByteArray? { ... }

    companion object { private const val MAX_PHASES = 2 }
}
```

- [ ] **Step 5: Delete dead files**

```bash
git rm composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/GearViewModel.kt
git rm "composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/GameModeSelectionViewModel.kt"
git rm composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/GearScreen.kt
git rm composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/GameModeSelectionScreen.kt
```

- [ ] **Step 6: Remove `gearRepository` constructor arg from `App.kt`**

```kotlin
// Old:
val gearRepository = remember { GearRepository(compositionRepository) }
// New:
val gearRepository = remember { GearRepository() }
```

Also remove any remaining references to `gameModeRepository` in the NavHost composables that no longer use it.

- [ ] **Step 7: Run all tests**

```bash
./gradlew :composeApp:jvmTest
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: Run the app and do a full smoke test of both paths**

```bash
./gradlew :composeApp:run
```

Verify:
- Home → TBC Anniversary → Tactics → 2v2 → comp → matchups → matchup detail → back navigation works
- Home → TBC Anniversary → Class Guides → spec → gear phase 1 → gear phase 2
- Browser back/forward works (web: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`)

- [ ] **Step 9: Commit**

```bash
git add \
  composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/model/GameMode.kt \
  composeApp/src/commonMain/composeResources/files/game_modes.json \
  composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/repository/GearRepository.kt \
  composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/App.kt \
  composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/data/repository/RepositoryParsingTest.kt
git commit -m "feat: complete addon navigation redesign — remove dead code, finalize GameMode model"
```

---

## Verification

- [ ] Run full test suite: `./gradlew :composeApp:allTests` — expect `BUILD SUCCESSFUL`
- [ ] Run desktop app: `./gradlew :composeApp:run` — smoke test both Tactics and Class Guides paths
- [ ] Run web app: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` — verify URL bridge works (deep links, back/forward)
- [ ] Check `.superpowers/` is in `.gitignore` (add if missing)
