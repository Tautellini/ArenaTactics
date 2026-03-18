# ArenaTactics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the ArenaTactics 2v2 Arena companion app — a Kotlin Multiplatform (WASM/JS + JVM) Compose app with game mode selection, composition selection, phase-based BiS gear viewer, and matchup strategy browser for Rogue/Mage.

**Architecture:** Screen ViewModels + simple Navigator back stack. Three layers: Data (repositories loading JSON from composeResources), Domain (CompositionGenerator), Presentation (Compose screens). Platform interop via four `expect/actual` functions (including `registerPopCallback` for browser back button wiring).

**Tech Stack:** Kotlin 2.3.0, Compose Multiplatform 1.10.0, kotlinx.serialization 1.8.0, Material3 1.10.0-alpha05, androidx.lifecycle ViewModel.

---

## File Structure

### Delete (boilerplate)
- `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/Greeting.kt`

### Rewrite completely
- `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/App.kt`
- `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/Platform.kt`
- `composeApp/src/jsMain/kotlin/net/tautellini/arenatactics/Platform.js.kt`
- `composeApp/src/wasmJsMain/kotlin/net/tautellini/arenatactics/Platform.wasmJs.kt`
- `composeApp/src/jvmMain/kotlin/net/tautellini/arenatactics/Platform.jvm.kt`
- `composeApp/src/webMain/kotlin/net/tautellini/arenatactics/main.kt`

### Modify
- `gradle/libs.versions.toml` — add kotlinx.serialization
- `composeApp/build.gradle.kts` — add serialization plugin + dependency
- `composeApp/src/jvmMain/kotlin/net/tautellini/arenatactics/main.kt` — keep as-is (no change needed)

### Create (commonMain)
```
kotlin/net/tautellini/arenatactics/
├── data/
│   ├── model/Models.kt
│   ├── repository/GameModeRepository.kt
│   ├── repository/CompositionRepository.kt
│   ├── repository/GearRepository.kt
│   └── repository/MatchupRepository.kt
├── domain/CompositionGenerator.kt
├── navigation/Screen.kt
├── navigation/Navigator.kt
├── presentation/
│   ├── theme/Theme.kt
│   ├── GameModeSelectionViewModel.kt
│   ├── CompositionSelectionViewModel.kt
│   ├── GearViewModel.kt
│   ├── MatchupListViewModel.kt
│   ├── MatchupDetailViewModel.kt
│   └── screens/
│       ├── GameModeSelectionScreen.kt
│       ├── CompositionSelectionScreen.kt
│       ├── GearScreen.kt
│       ├── MatchupListScreen.kt
│       ├── MatchupDetailScreen.kt
│       └── components/
│           ├── ClassBadge.kt
│           ├── CompositionCard.kt
│           ├── ItemRow.kt
│           └── MarkdownText.kt
```

### Create (resources)
```
composeResources/files/
├── game_modes.json
├── class_pools/tbc.json
├── composition_sets/tbc_2v2.json
├── gear/
│   ├── gear_rogue_phase1.json
│   ├── gear_rogue_phase2.json
│   ├── gear_mage_phase1.json
│   └── gear_mage_phase2.json
└── matchups/matchups_rogue_mage.json
```

### Create (tests — commonTest)
```
kotlin/net/tautellini/arenatactics/
├── data/repository/RepositoryParsingTest.kt
├── navigation/NavigatorTest.kt
└── domain/CompositionGeneratorTest.kt
```

---

## Task 1: Add Dependency + Remove Boilerplate

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `composeApp/build.gradle.kts`
- Delete: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/Greeting.kt`

- [ ] **Step 1: Add kotlinx.serialization to version catalog**

Edit `gradle/libs.versions.toml` — add under `[versions]`:
```toml
kotlinx-serialization = "1.8.0"
```
Add under `[libraries]`:
```toml
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
```
Add under `[plugins]`:
```toml
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

- [ ] **Step 2: Apply plugin and add dependency in composeApp/build.gradle.kts**

Add `alias(libs.plugins.kotlinSerialization)` to the `plugins { }` block.

Add to `commonMain.dependencies { }`:
```kotlin
implementation(libs.kotlinx.serialization.json)
```

- [ ] **Step 3: Delete Greeting.kt**
```bash
rm composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/Greeting.kt
```

- [ ] **Step 4: Verify build compiles**
```bash
./gradlew :composeApp:compileKotlinJvm
```
Expected: BUILD SUCCESSFUL (may have errors about App.kt referencing Greeting — that's fine, we rewrite App.kt next)

- [ ] **Step 5: Commit**
```bash
git add gradle/libs.versions.toml composeApp/build.gradle.kts
git commit -m "chore: add kotlinx.serialization dependency"
```

---

## Task 2: Platform Interop

**Files:**
- Rewrite: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/Platform.kt`
- Rewrite: `composeApp/src/jvmMain/kotlin/net/tautellini/arenatactics/Platform.jvm.kt`
- Rewrite: `composeApp/src/jsMain/kotlin/net/tautellini/arenatactics/Platform.js.kt`
- Rewrite: `composeApp/src/wasmJsMain/kotlin/net/tautellini/arenatactics/Platform.wasmJs.kt`

- [ ] **Step 1: Rewrite commonMain Platform.kt**

```kotlin
package net.tautellini.arenatactics

expect fun refreshWowheadTooltips()
expect fun openUrl(url: String)
expect fun pushNavigationState(path: String)
// Registers a callback invoked by the browser's popstate event (back button).
// On non-web platforms this is a no-op.
expect fun registerPopCallback(callback: () -> Unit)
```

- [ ] **Step 2: Rewrite Platform.jvm.kt**

```kotlin
package net.tautellini.arenatactics

actual fun refreshWowheadTooltips() {}
actual fun openUrl(url: String) {}
actual fun pushNavigationState(path: String) {}
actual fun registerPopCallback(callback: () -> Unit) {}
```

- [ ] **Step 3: Rewrite Platform.js.kt**

```kotlin
package net.tautellini.arenatactics

private var popCallback: (() -> Unit)? = null

actual fun refreshWowheadTooltips() {
    js("if (typeof WH !== 'undefined') WH.refreshLinks()")
}

actual fun openUrl(url: String) {
    js("window.open(url, '_blank')")
}

actual fun pushNavigationState(path: String) {
    js("history.pushState(null, '', path)")
}

actual fun registerPopCallback(callback: () -> Unit) {
    popCallback = callback
    js("window.onpopstate = function() { net.tautellini.arenatactics.invokePopCallback() }")
}

fun invokePopCallback() { popCallback?.invoke() }
```

- [ ] **Step 4: Rewrite Platform.wasmJs.kt**

```kotlin
package net.tautellini.arenatactics

import kotlinx.browser.window

private var popCallback: (() -> Unit)? = null

actual fun refreshWowheadTooltips() {
    js("if (typeof WH !== 'undefined') WH.refreshLinks()")
}

actual fun openUrl(url: String) {
    window.open(url, "_blank")
}

actual fun pushNavigationState(path: String) {
    window.history.pushState(null, "", path)
}

actual fun registerPopCallback(callback: () -> Unit) {
    popCallback = callback
    window.onpopstate = { popCallback?.invoke() }
}
```

- [ ] **Step 5: Verify build**
```bash
./gradlew :composeApp:compileKotlinJvm
```
Expected: BUILD SUCCESSFUL (App.kt still references Greeting — fix in Task 9 when we rewrite App.kt)

- [ ] **Step 6: Commit**
```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/Platform.kt \
  composeApp/src/jvmMain/kotlin/net/tautellini/arenatactics/Platform.jvm.kt \
  composeApp/src/jsMain/kotlin/net/tautellini/arenatactics/Platform.js.kt \
  composeApp/src/wasmJsMain/kotlin/net/tautellini/arenatactics/Platform.wasmJs.kt
git commit -m "feat: replace boilerplate Platform with navigation/tooltip interop"
```

---

## Task 3: Data Models

**Files:**
- Create: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/model/Models.kt`

- [ ] **Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/data/repository/RepositoryParsingTest.kt`:

```kotlin
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
        val json = """{"phase":1,"classId":"rogue","items":[{"wowheadId":28210,"name":"Gladiator's Leather Helm","slot":"Head","enchant":"Glyph of Ferocity","gems":["Relentless Earthstorm Diamond"]}]}"""
        val result = parseGearPhase(json)
        assertEquals(1, result.phase)
        assertEquals(1, result.items.size)
        assertEquals(28210, result.items[0].wowheadId)
    }

    @Test
    fun matchupDeserializes() {
        val json = """[{"id":"mage_rogue_vs_druid_warrior","enemyClass1Id":"druid","enemyClass2Id":"warrior","strategyMarkdown":"## Kill Target\nWarrior"}]"""
        val result = parseMatchups(json)
        assertEquals("mage_rogue_vs_druid_warrior", result[0].id)
    }
}
```

- [ ] **Step 2: Run test — verify it fails**
```bash
./gradlew :composeApp:jvmTest --tests "*.RepositoryParsingTest"
```
Expected: FAIL — `parseGameModes` not found

- [ ] **Step 3: Create Models.kt**

```kotlin
package net.tautellini.arenatactics.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GameMode(
    val id: String,
    val name: String,
    val description: String,
    val classPoolId: String,
    val compositionSetId: String
)

@Serializable
data class WowClass(
    val id: String,
    val name: String,
    val color: String
)

@Serializable
data class Composition(
    val class1Id: String,
    val class2Id: String
) {
    val id: String get() = "${class1Id}_${class2Id}"
}

@Serializable
data class GearItem(
    val wowheadId: Int,
    val name: String,
    val slot: String,
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
    val enemyClass1Id: String,
    val enemyClass2Id: String,
    val strategyMarkdown: String
)
```

- [ ] **Step 4: Run test — verify it still fails (parse functions not yet created)**
```bash
./gradlew :composeApp:jvmTest --tests "*.RepositoryParsingTest"
```

- [ ] **Step 5: Commit models**
```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/model/Models.kt \
  composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/data/repository/RepositoryParsingTest.kt
git commit -m "feat: add domain data models and parsing tests"
```

---

## Task 4: Navigation

**Files:**
- Create: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/navigation/Screen.kt`
- Create: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/navigation/Navigator.kt`
- Create: `composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/navigation/NavigatorTest.kt`

- [ ] **Step 1: Write failing tests**

Create `NavigatorTest.kt`:
```kotlin
package net.tautellini.arenatactics.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NavigatorTest {
    @Test
    fun initialScreenIsGameModeSelection() {
        val nav = Navigator()
        assertEquals(Screen.GameModeSelection, nav.current)
    }

    @Test
    fun pushAddsToStack() {
        val nav = Navigator()
        nav.push(Screen.CompositionSelection("tbc"))
        assertEquals(Screen.CompositionSelection("tbc"), nav.current)
    }

    @Test
    fun popReturnsToParent() {
        val nav = Navigator()
        nav.push(Screen.CompositionSelection("tbc"))
        nav.pop()
        assertEquals(Screen.GameModeSelection, nav.current)
    }

    @Test
    fun popOnRootDoesNothing() {
        val nav = Navigator()
        nav.pop()
        assertEquals(Screen.GameModeSelection, nav.current)
    }
}
```

- [ ] **Step 2: Run test — verify fails**
```bash
./gradlew :composeApp:jvmTest --tests "*.NavigatorTest"
```
Expected: FAIL — `Navigator` not found

- [ ] **Step 3: Create Screen.kt**

```kotlin
package net.tautellini.arenatactics.navigation

sealed class Screen {
    data object GameModeSelection : Screen()
    data class CompositionSelection(val gameModeId: String) : Screen()
    data class GearView(val gameModeId: String, val compositionId: String) : Screen()
    data class MatchupList(val gameModeId: String, val compositionId: String) : Screen()
    data class MatchupDetail(
        val gameModeId: String,
        val compositionId: String,
        val matchupId: String
    ) : Screen()

    val path: String get() = when (this) {
        is GameModeSelection -> "/"
        is CompositionSelection -> "/modes/$gameModeId"
        is GearView -> "/modes/$gameModeId/comp/$compositionId/gear"
        is MatchupList -> "/modes/$gameModeId/comp/$compositionId/matchups"
        is MatchupDetail -> "/modes/$gameModeId/comp/$compositionId/matchups/$matchupId"
    }
}
```

- [ ] **Step 4: Create Navigator.kt**

```kotlin
package net.tautellini.arenatactics.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.tautellini.arenatactics.pushNavigationState

class Navigator {
    private val _stack = MutableStateFlow<List<Screen>>(listOf(Screen.GameModeSelection))
    val stack: StateFlow<List<Screen>> = _stack.asStateFlow()

    val current: Screen get() = _stack.value.last()

    fun push(screen: Screen) {
        _stack.value = _stack.value + screen
        pushNavigationState(screen.path)
    }

    fun pop() {
        if (_stack.value.size > 1) {
            _stack.value = _stack.value.dropLast(1)
        }
    }
}
```

- [ ] **Step 5: Run tests — verify pass**
```bash
./gradlew :composeApp:jvmTest --tests "*.NavigatorTest"
```
Expected: PASS

- [ ] **Step 6: Commit**
```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/navigation/ \
  composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/navigation/
git commit -m "feat: add Screen sealed class and Navigator back stack"
```

---

## Task 5: Repositories + Domain

**Files:**
- Create: `data/repository/GameModeRepository.kt`
- Create: `data/repository/CompositionRepository.kt`
- Create: `data/repository/GearRepository.kt`
- Create: `data/repository/MatchupRepository.kt`
- Create: `domain/CompositionGenerator.kt`
- Create: `commonTest/.../domain/CompositionGeneratorTest.kt`

- [ ] **Step 1: Write failing CompositionGenerator test**

Create `CompositionGeneratorTest.kt`:
```kotlin
package net.tautellini.arenatactics.domain

import net.tautellini.arenatactics.data.model.Composition
import net.tautellini.arenatactics.data.model.WowClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompositionGeneratorTest {
    private val classes = listOf(
        WowClass("druid", "Druid", "#FF7D0A"),
        WowClass("mage", "Mage", "#69CCF0"),
        WowClass("rogue", "Rogue", "#FFF569")
    )
    private val whitelist = listOf(
        Composition("druid", "mage"),
        Composition("mage", "rogue")
    )

    @Test
    fun generatesOnlyWhitelistedCompositions() {
        val result = CompositionGenerator.generate(classes, whitelist)
        assertEquals(2, result.size)
    }

    @Test
    fun canonicalOrderEnforced() {
        // whitelist has "mage_rogue"; classes are in order druid, mage, rogue
        val result = CompositionGenerator.generate(classes, whitelist)
        assertTrue(result.all { it.class1Id <= it.class2Id })
    }

    @Test
    fun enrichesWithClassObjects() {
        val result = CompositionGenerator.generate(classes, whitelist)
        val maRo = result.first { it.composition.id == "mage_rogue" }
        assertEquals("Mage", maRo.class1.name)
        assertEquals("Rogue", maRo.class2.name)
    }
}
```

- [ ] **Step 2: Run test — verify fails**
```bash
./gradlew :composeApp:jvmTest --tests "*.CompositionGeneratorTest"
```

- [ ] **Step 3: Create CompositionGenerator.kt**

```kotlin
package net.tautellini.arenatactics.domain

import net.tautellini.arenatactics.data.model.Composition
import net.tautellini.arenatactics.data.model.WowClass

data class RichComposition(
    val composition: Composition,
    val class1: WowClass,
    val class2: WowClass
)

object CompositionGenerator {
    fun generate(classes: List<WowClass>, whitelist: List<Composition>): List<RichComposition> {
        val classMap = classes.associateBy { it.id }
        return whitelist.mapNotNull { comp ->
            val c1 = classMap[comp.class1Id] ?: return@mapNotNull null
            val c2 = classMap[comp.class2Id] ?: return@mapNotNull null
            RichComposition(comp, c1, c2)
        }
    }
}
```

- [ ] **Step 4: Run test — verify passes**
```bash
./gradlew :composeApp:jvmTest --tests "*.CompositionGeneratorTest"
```
Expected: PASS

- [ ] **Step 5: Create repositories with internal parse functions**

Create `GameModeRepository.kt`:
```kotlin
package net.tautellini.arenatactics.data.repository

import arenatactics.composeapp.generated.resources.Res
import kotlinx.serialization.json.Json
import net.tautellini.arenatactics.data.model.GameMode

internal fun parseGameModes(jsonString: String): List<GameMode> =
    appJson.decodeFromString(jsonString)

class GameModeRepository {
    suspend fun getAll(): List<GameMode> {
        val bytes = Res.readBytes("files/game_modes.json")
        return parseGameModes(bytes.decodeToString())
    }
}
```

Create `CompositionRepository.kt`:
```kotlin
package net.tautellini.arenatactics.data.repository

import arenatactics.composeapp.generated.resources.Res
import net.tautellini.arenatactics.data.model.Composition
import net.tautellini.arenatactics.data.model.WowClass

internal fun parseWowClasses(jsonString: String): List<WowClass> =
    appJson.decodeFromString(jsonString)

internal fun parseCompositions(jsonString: String): List<Composition> {
    val raw: List<Composition> = appJson.decodeFromString(jsonString)
    return raw.map { comp ->
        val sorted = listOf(comp.class1Id, comp.class2Id).sorted()
        Composition(sorted[0], sorted[1])
    }
}

class CompositionRepository {
    private val classCache = mutableMapOf<String, List<WowClass>>()
    private val compCache = mutableMapOf<String, Map<String, Composition>>()

    suspend fun getClasses(classPoolId: String): List<WowClass> {
        return classCache.getOrPut(classPoolId) {
            val bytes = Res.readBytes("files/class_pools/$classPoolId.json")
            parseWowClasses(bytes.decodeToString())
        }
    }

    suspend fun getCompositions(compositionSetId: String): List<Composition> {
        return compCache.getOrPut(compositionSetId) {
            val bytes = Res.readBytes("files/composition_sets/$compositionSetId.json")
            parseCompositions(bytes.decodeToString())
        }.values.toList()
    }

    suspend fun getById(compositionId: String, compositionSetId: String): Composition? {
        return getCompositions(compositionSetId).find { it.id == compositionId }
    }
}
```

Create `GearRepository.kt`:
```kotlin
package net.tautellini.arenatactics.data.repository

import arenatactics.composeapp.generated.resources.Res
import net.tautellini.arenatactics.data.model.GearPhase

internal fun parseGearPhase(jsonString: String): GearPhase =
    appJson.decodeFromString(jsonString)

class GearRepository(private val compositionRepository: CompositionRepository) {
    suspend fun getGearForComposition(
        compositionId: String,
        compositionSetId: String
    ): Map<String, List<GearPhase>> {
        val comp = compositionRepository.getById(compositionId, compositionSetId) ?: return emptyMap()
        val classIds = listOf(comp.class1Id, comp.class2Id)
        return classIds.associateWith { classId -> loadPhasesForClass(classId) }
    }

    private suspend fun loadPhasesForClass(classId: String): List<GearPhase> {
        val phases = mutableListOf<GearPhase>()
        for (phase in 1..10) {
            val bytes = tryReadBytes("files/gear/gear_${classId}_phase${phase}.json") ?: break
            phases.add(parseGearPhase(bytes.decodeToString()))
        }
        return phases
    }

    private suspend fun tryReadBytes(path: String): ByteArray? = try {
        Res.readBytes(path)
    } catch (e: Exception) {
        null
    }
}
```

Create `MatchupRepository.kt`:
```kotlin
package net.tautellini.arenatactics.data.repository

import arenatactics.composeapp.generated.resources.Res
import net.tautellini.arenatactics.data.model.Matchup

internal fun parseMatchups(jsonString: String): List<Matchup> =
    appJson.decodeFromString(jsonString)

class MatchupRepository {
    private var cache: Map<String, Matchup>? = null

    suspend fun getForComposition(compositionId: String): List<Matchup> {
        return getCache(compositionId).values.toList()
    }

    suspend fun getById(compositionId: String, matchupId: String): Matchup? {
        return getCache(compositionId)[matchupId]
    }

    private suspend fun getCache(compositionId: String): Map<String, Matchup> {
        return cache ?: run {
            val bytes = Res.readBytes("files/matchups/matchups_$compositionId.json")
            val matchups = parseMatchups(bytes.decodeToString())
            matchups.associateBy { it.id }.also { cache = it }
        }
    }
}
```

Create a shared Json instance at `data/repository/AppJson.kt`:
```kotlin
package net.tautellini.arenatactics.data.repository

import kotlinx.serialization.json.Json

internal val appJson = Json { ignoreUnknownKeys = true }
```

- [ ] **Step 6: Run parsing tests — verify pass**
```bash
./gradlew :composeApp:jvmTest --tests "*.RepositoryParsingTest"
```
Expected: PASS (the parse* functions are now defined in the repository files)

- [ ] **Step 7: Run all tests**
```bash
./gradlew :composeApp:jvmTest
```
Expected: All tests PASS

- [ ] **Step 8: Commit**
```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/data/ \
  composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/domain/ \
  composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/domain/
git commit -m "feat: add repositories, domain layer, and all parsing tests"
```

---

## Task 6: Static Data — Game Modes, Classes, Compositions

**Files:**
- Create: `composeApp/src/commonMain/composeResources/files/game_modes.json`
- Create: `composeApp/src/commonMain/composeResources/files/class_pools/tbc.json`
- Create: `composeApp/src/commonMain/composeResources/files/composition_sets/tbc_2v2.json`

- [ ] **Step 1: Create game_modes.json**

```json
[
  {
    "id": "tbc_anniversary_2v2",
    "name": "TBC Anniversary 2v2",
    "description": "World of Warcraft: The Burning Crusade Classic (Anniversary) — 2v2 Arena",
    "classPoolId": "tbc",
    "compositionSetId": "tbc_2v2"
  }
]
```

- [ ] **Step 2: Create class_pools/tbc.json**

```json
[
  { "id": "druid",   "name": "Druid",   "color": "#FF7D0A" },
  { "id": "hunter",  "name": "Hunter",  "color": "#ABD473" },
  { "id": "mage",    "name": "Mage",    "color": "#69CCF0" },
  { "id": "paladin", "name": "Paladin", "color": "#F58CBA" },
  { "id": "priest",  "name": "Priest",  "color": "#FFFFFF" },
  { "id": "rogue",   "name": "Rogue",   "color": "#FFF569" },
  { "id": "shaman",  "name": "Shaman",  "color": "#0070DE" },
  { "id": "warlock", "name": "Warlock", "color": "#9482C9" },
  { "id": "warrior", "name": "Warrior", "color": "#C79C6E" }
]
```

- [ ] **Step 3: Create composition_sets/tbc_2v2.json**

Note: `class1Id` must always be alphabetically less than `class2Id`. Only Rogue/Mage is listed for the initial release — add more as gear/matchup data is authored.

```json
[
  { "class1Id": "mage", "class2Id": "rogue" }
]
```

- [ ] **Step 4: Commit**
```bash
git add composeApp/src/commonMain/composeResources/files/
git commit -m "feat: add game mode, class pool, and composition set JSON data"
```

---

## Task 7: Static Data — Rogue Gear

**Files:**
- Create: `composeResources/files/gear/gear_rogue_phase1.json`
- Create: `composeResources/files/gear/gear_rogue_phase2.json`

- [ ] **Step 1: Create gear_rogue_phase1.json (Season 1 — Gladiator's Leather)**

```json
{
  "phase": 1,
  "classId": "rogue",
  "items": [
    { "wowheadId": 28210, "name": "Gladiator's Leather Helm",       "slot": "Head",       "enchant": "Glyph of Ferocity",               "gems": ["Relentless Earthstorm Diamond", "Shifting Nightseye"] },
    { "wowheadId": 28738, "name": "Pendant of the Peril",           "slot": "Neck",       "enchant": null,                              "gems": ["Jagged Crimson Spinel"] },
    { "wowheadId": 28214, "name": "Gladiator's Leather Spaulders",  "slot": "Shoulders",  "enchant": "Greater Inscription of Vengeance", "gems": ["Bright Blood Garnet"] },
    { "wowheadId": 28256, "name": "Drape of the Dark Reavers",      "slot": "Back",       "enchant": "Enchant Cloak - Subtlety",        "gems": [] },
    { "wowheadId": 28208, "name": "Gladiator's Leather Tunic",      "slot": "Chest",      "enchant": "Enchant Chest - Exceptional Stats","gems": ["Bright Blood Garnet", "Bright Blood Garnet", "Shifting Nightseye"] },
    { "wowheadId": 28278, "name": "Bracers of Maliciousness",       "slot": "Wrists",     "enchant": "Enchant Bracer - Assault",        "gems": [] },
    { "wowheadId": 28211, "name": "Gladiator's Leather Gloves",     "slot": "Hands",      "enchant": "Enchant Gloves - Superior Agility","gems": ["Bright Blood Garnet"] },
    { "wowheadId": 31913, "name": "Vindicator's Leather Belt",      "slot": "Waist",      "enchant": null,                              "gems": ["Bright Blood Garnet"] },
    { "wowheadId": 28213, "name": "Gladiator's Leather Legguards",  "slot": "Legs",       "enchant": "Nethercobra Leg Armor",           "gems": ["Bright Blood Garnet", "Bright Blood Garnet"] },
    { "wowheadId": 28209, "name": "Gladiator's Leather Boots",      "slot": "Feet",       "enchant": "Enchant Boots - Cat's Swiftness", "gems": ["Bright Blood Garnet"] },
    { "wowheadId": 28304, "name": "Gladiator's Band of Triumph",    "slot": "Ring",       "enchant": "Enchant Ring - Striking",         "gems": [] },
    { "wowheadId": 28765, "name": "Band of the Ranger-General",     "slot": "Ring",       "enchant": "Enchant Ring - Striking",         "gems": [] },
    { "wowheadId": 29131, "name": "Medallion of the Horde",         "slot": "Trinket",    "enchant": null,                              "gems": [] },
    { "wowheadId": 29383, "name": "Bloodlust Brooch",               "slot": "Trinket",    "enchant": null,                              "gems": [] },
    { "wowheadId": 28272, "name": "Gladiator's Shanker",            "slot": "Main Hand",  "enchant": "Enchant Weapon - Mongoose",       "gems": [] },
    { "wowheadId": 28270, "name": "Gladiator's Shiv",               "slot": "Off Hand",   "enchant": "Enchant Weapon - Mongoose",       "gems": [] },
    { "wowheadId": 28275, "name": "Gladiator's War Edge",           "slot": "Ranged",     "enchant": null,                              "gems": [] }
  ]
}
```

- [ ] **Step 2: Create gear_rogue_phase2.json (Season 2 — Merciless Gladiator's Leather)**

```json
{
  "phase": 2,
  "classId": "rogue",
  "items": [
    { "wowheadId": 31017, "name": "Merciless Gladiator's Leather Helm",      "slot": "Head",      "enchant": "Glyph of Ferocity",                "gems": ["Relentless Earthstorm Diamond", "Shifting Nightseye", "Jagged Crimson Spinel"] },
    { "wowheadId": 28738, "name": "Pendant of the Peril",                    "slot": "Neck",      "enchant": null,                               "gems": ["Jagged Crimson Spinel"] },
    { "wowheadId": 31021, "name": "Merciless Gladiator's Leather Spaulders", "slot": "Shoulders", "enchant": "Greater Inscription of Vengeance",  "gems": ["Bright Blood Garnet"] },
    { "wowheadId": 28256, "name": "Drape of the Dark Reavers",               "slot": "Back",      "enchant": "Enchant Cloak - Subtlety",          "gems": [] },
    { "wowheadId": 31015, "name": "Merciless Gladiator's Leather Tunic",     "slot": "Chest",     "enchant": "Enchant Chest - Exceptional Stats",  "gems": ["Bright Blood Garnet", "Bright Blood Garnet", "Shifting Nightseye"] },
    { "wowheadId": 34462, "name": "Vindicator's Leather Bracers",            "slot": "Wrists",    "enchant": "Enchant Bracer - Assault",           "gems": [] },
    { "wowheadId": 31018, "name": "Merciless Gladiator's Leather Gloves",    "slot": "Hands",     "enchant": "Enchant Gloves - Superior Agility",  "gems": ["Bright Blood Garnet"] },
    { "wowheadId": 34467, "name": "Merciless Gladiator's Leather Belt",      "slot": "Waist",     "enchant": null,                                "gems": ["Bright Blood Garnet"] },
    { "wowheadId": 31020, "name": "Merciless Gladiator's Leather Legguards", "slot": "Legs",      "enchant": "Nethercobra Leg Armor",              "gems": ["Bright Blood Garnet", "Bright Blood Garnet"] },
    { "wowheadId": 31016, "name": "Merciless Gladiator's Leather Boots",     "slot": "Feet",      "enchant": "Enchant Boots - Cat's Swiftness",    "gems": ["Bright Blood Garnet"] },
    { "wowheadId": 31854, "name": "Merciless Gladiator's Band of Triumph",   "slot": "Ring",      "enchant": "Enchant Ring - Striking",            "gems": [] },
    { "wowheadId": 28765, "name": "Band of the Ranger-General",              "slot": "Ring",      "enchant": "Enchant Ring - Striking",            "gems": [] },
    { "wowheadId": 29131, "name": "Medallion of the Horde",                  "slot": "Trinket",   "enchant": null,                                "gems": [] },
    { "wowheadId": 31193, "name": "Tsunami Talisman",                        "slot": "Trinket",   "enchant": null,                                "gems": [] },
    { "wowheadId": 31826, "name": "Merciless Gladiator's Shanker",           "slot": "Main Hand", "enchant": "Enchant Weapon - Mongoose",          "gems": [] },
    { "wowheadId": 31827, "name": "Merciless Gladiator's Shiv",              "slot": "Off Hand",  "enchant": "Enchant Weapon - Mongoose",          "gems": [] },
    { "wowheadId": 32040, "name": "Merciless Gladiator's War Edge",          "slot": "Ranged",    "enchant": null,                                "gems": [] }
  ]
}
```

- [ ] **Step 3: Commit**
```bash
git add composeApp/src/commonMain/composeResources/files/gear/gear_rogue_phase1.json \
  composeApp/src/commonMain/composeResources/files/gear/gear_rogue_phase2.json
git commit -m "feat: add Rogue Phase 1 and Phase 2 BiS gear data"
```

---

## Task 8: Static Data — Mage Gear

**Files:**
- Create: `composeResources/files/gear/gear_mage_phase1.json`
- Create: `composeResources/files/gear/gear_mage_phase2.json`

- [ ] **Step 1: Create gear_mage_phase1.json (Season 1 — Gladiator's Silk)**

```json
{
  "phase": 1,
  "classId": "mage",
  "items": [
    { "wowheadId": 27843, "name": "Gladiator's Silk Cowl",          "slot": "Head",      "enchant": "Glyph of Power",                    "gems": ["Chaotic Skyfire Diamond", "Runed Blood Garnet"] },
    { "wowheadId": 31012, "name": "Vindicator's Pendant of Conquest","slot": "Neck",      "enchant": null,                               "gems": ["Runed Blood Garnet"] },
    { "wowheadId": 27847, "name": "Gladiator's Silk Amice",          "slot": "Shoulders", "enchant": "Greater Inscription of Discipline", "gems": ["Runed Blood Garnet"] },
    { "wowheadId": 28971, "name": "Cloak of Arcane Evasion",         "slot": "Back",      "enchant": "Enchant Cloak - Subtlety",          "gems": [] },
    { "wowheadId": 27841, "name": "Gladiator's Silk Raiment",        "slot": "Chest",     "enchant": "Enchant Chest - Exceptional Stats",  "gems": ["Runed Blood Garnet", "Runed Blood Garnet", "Veiled Flame Spessarite"] },
    { "wowheadId": 31936, "name": "Vindicator's Silk Cuffs",         "slot": "Wrists",    "enchant": "Enchant Bracer - Spellpower",        "gems": [] },
    { "wowheadId": 27844, "name": "Gladiator's Silk Handguards",     "slot": "Hands",     "enchant": "Enchant Gloves - Spell Strike",     "gems": ["Runed Blood Garnet"] },
    { "wowheadId": 31935, "name": "Vindicator's Silk Belt",          "slot": "Waist",     "enchant": null,                                "gems": ["Runed Blood Garnet"] },
    { "wowheadId": 27846, "name": "Gladiator's Silk Trousers",       "slot": "Legs",      "enchant": "Runic Spellthread",                 "gems": ["Runed Blood Garnet", "Runed Blood Garnet"] },
    { "wowheadId": 27842, "name": "Gladiator's Silk Footwraps",      "slot": "Feet",      "enchant": "Enchant Boots - Boar's Speed",      "gems": ["Runed Blood Garnet"] },
    { "wowheadId": 28305, "name": "Gladiator's Band of Dominance",   "slot": "Ring",      "enchant": "Enchant Ring - Spellpower",         "gems": [] },
    { "wowheadId": 29281, "name": "Violet Signet of the Archmage",   "slot": "Ring",      "enchant": "Enchant Ring - Spellpower",         "gems": [] },
    { "wowheadId": 29131, "name": "Medallion of the Horde",          "slot": "Trinket",   "enchant": null,                                "gems": [] },
    { "wowheadId": 29376, "name": "Icon of the Silver Crescent",     "slot": "Trinket",   "enchant": null,                                "gems": [] },
    { "wowheadId": 28190, "name": "Gladiator's Spellblade",          "slot": "Main Hand", "enchant": "Enchant Weapon - Spellsurge",       "gems": [] },
    { "wowheadId": 28293, "name": "Gladiator's Endgame",             "slot": "Off Hand",  "enchant": null,                                "gems": [] },
    { "wowheadId": 27854, "name": "Gladiator's Touch of Defeat",     "slot": "Wand",      "enchant": null,                                "gems": [] }
  ]
}
```

- [ ] **Step 2: Create gear_mage_phase2.json (Season 2 — Merciless Gladiator's Silk)**

```json
{
  "phase": 2,
  "classId": "mage",
  "items": [
    { "wowheadId": 31011, "name": "Merciless Gladiator's Silk Cowl",          "slot": "Head",      "enchant": "Glyph of Power",                    "gems": ["Chaotic Skyfire Diamond", "Runed Blood Garnet", "Veiled Flame Spessarite"] },
    { "wowheadId": 31012, "name": "Vindicator's Pendant of Conquest",          "slot": "Neck",      "enchant": null,                               "gems": ["Runed Blood Garnet"] },
    { "wowheadId": 31008, "name": "Merciless Gladiator's Silk Amice",          "slot": "Shoulders", "enchant": "Greater Inscription of Discipline", "gems": ["Runed Blood Garnet"] },
    { "wowheadId": 28971, "name": "Cloak of Arcane Evasion",                   "slot": "Back",      "enchant": "Enchant Cloak - Subtlety",          "gems": [] },
    { "wowheadId": 31007, "name": "Merciless Gladiator's Silk Raiment",        "slot": "Chest",     "enchant": "Enchant Chest - Exceptional Stats",  "gems": ["Runed Blood Garnet", "Runed Blood Garnet", "Veiled Flame Spessarite"] },
    { "wowheadId": 34473, "name": "Vindicator's Silk Cuffs",                   "slot": "Wrists",    "enchant": "Enchant Bracer - Spellpower",        "gems": [] },
    { "wowheadId": 31009, "name": "Merciless Gladiator's Silk Handguards",     "slot": "Hands",     "enchant": "Enchant Gloves - Spell Strike",     "gems": ["Runed Blood Garnet"] },
    { "wowheadId": 34474, "name": "Vindicator's Silk Belt",                    "slot": "Waist",     "enchant": null,                                "gems": ["Runed Blood Garnet"] },
    { "wowheadId": 31010, "name": "Merciless Gladiator's Silk Trousers",       "slot": "Legs",      "enchant": "Runic Spellthread",                 "gems": ["Runed Blood Garnet", "Runed Blood Garnet"] },
    { "wowheadId": 31006, "name": "Merciless Gladiator's Silk Footwraps",      "slot": "Feet",      "enchant": "Enchant Boots - Boar's Speed",      "gems": ["Runed Blood Garnet"] },
    { "wowheadId": 31852, "name": "Merciless Gladiator's Band of Dominance",   "slot": "Ring",      "enchant": "Enchant Ring - Spellpower",         "gems": [] },
    { "wowheadId": 29281, "name": "Violet Signet of the Archmage",             "slot": "Ring",      "enchant": "Enchant Ring - Spellpower",         "gems": [] },
    { "wowheadId": 29131, "name": "Medallion of the Horde",                    "slot": "Trinket",   "enchant": null,                                "gems": [] },
    { "wowheadId": 29376, "name": "Icon of the Silver Crescent",               "slot": "Trinket",   "enchant": null,                                "gems": [] },
    { "wowheadId": 31818, "name": "Merciless Gladiator's Spellblade",          "slot": "Main Hand", "enchant": "Enchant Weapon - Soulfrost",        "gems": [] },
    { "wowheadId": 31857, "name": "Merciless Gladiator's Endgame",             "slot": "Off Hand",  "enchant": null,                                "gems": [] },
    { "wowheadId": 31858, "name": "Merciless Gladiator's Touch of Defeat",     "slot": "Wand",      "enchant": null,                                "gems": [] }
  ]
}
```

- [ ] **Step 3: Commit**
```bash
git add composeApp/src/commonMain/composeResources/files/gear/gear_mage_phase1.json \
  composeApp/src/commonMain/composeResources/files/gear/gear_mage_phase2.json
git commit -m "feat: add Mage Phase 1 and Phase 2 BiS gear data"
```

---

## Task 9: Static Data — Matchups

**Files:**
- Create: `composeResources/files/matchups/matchups_mage_rogue.json`

Note: the file is named after the composition ID `mage_rogue` (canonical alphabetical order).

- [ ] **Step 1: Create matchups_mage_rogue.json**

```json
[
  {
    "id": "mage_rogue_vs_druid_warrior",
    "enemyClass1Id": "druid",
    "enemyClass2Id": "warrior",
    "strategyMarkdown": "## Kill Target\nWarrior\n\n## Win Condition\nPoly the Druid and burst the Warrior while unhealed. Rogue opens with Cheap Shot → Kidney Shot chain. Mage keeps Poly on Druid — re-poly immediately if it breaks.\n\n---\n\n## Key Notes\n\n- If Warrior pops Recklessness, use Frost Nova + Blink to reset distance.\n- Watch for Druid going Bear Form to trinket Poly — have CS ready.\n- If Warrior trinkets, re-CC with Gouge → Blind rotation.\n- Don't tunnel — switch to Druid briefly to force cooldown usage, then back to Warrior."
  },
  {
    "id": "mage_rogue_vs_priest_warrior",
    "enemyClass1Id": "priest",
    "enemyClass2Id": "warrior",
    "strategyMarkdown": "## Kill Target\nWarrior\n\n## Win Condition\nCounterspell / Silence the Priest during burst windows. Rogue bursts Warrior. Mage applies Frostbolt slow and interrupts every heal possible.\n\n---\n\n## Key Notes\n\n- Shadow Priest variant: Respect Psychic Scream. Break fear with Trinket or Berserker Rage if available.\n- Holy Priest: Easier target due to no shadow form — CS on Greater Heal wins.\n- Warrior must never be in range to land a full Bladestorm without being Frost Nova'd."
  },
  {
    "id": "mage_rogue_vs_mage_rogue",
    "enemyClass1Id": "mage",
    "enemyClass2Id": "rogue",
    "strategyMarkdown": "## Kill Target\nEither — prefer enemy Mage\n\n## Win Condition\nThis is a mirror. CS the enemy Mage to prevent burst. Your Mage should stay near your Rogue to benefit from Smoke Screen. Whoever lands the first non-trinketed CC chain usually wins.\n\n---\n\n## Key Notes\n\n- Counterspell wars are the core of this matchup — never waste yours.\n- Detect Invisibility or Detect Traps can reveal stealthed enemy Rogue early.\n- If enemy Rogue opens on your Mage, Frost Nova + Blink immediately.\n- Don't both target the same player until CC is locked on the other."
  },
  {
    "id": "mage_rogue_vs_rogue_warrior",
    "enemyClass1Id": "rogue",
    "enemyClass2Id": "warrior",
    "strategyMarkdown": "## Kill Target\nWarrior\n\n## Win Condition\nHeavy cleave comp — they will train your Rogue hard. Your Mage must peel with Frost Nova and sheep the enemy Rogue. Kill Warrior while enemy Rogue is CCd.\n\n---\n\n## Key Notes\n\n- This matchup is about surviving their initial burst then recovering control.\n- Ice Block is a critical survival tool — save it for Rogue + Warrior overlap.\n- Your Rogue should use Evasion when double-trained to survive the burst window.\n- Once you get them separated with Poly, the Warrior should die quickly."
  },
  {
    "id": "mage_rogue_vs_priest_warlock",
    "enemyClass1Id": "priest",
    "enemyClass2Id": "warlock",
    "strategyMarkdown": "## Kill Target\nWarlock\n\n## Win Condition\nInterrupt Drain Life with Counterspell. Rogue bursts Warlock with Blind on Priest. Silence / CS prevents Priest from healing during burst window.\n\n---\n\n## Key Notes\n\n- Fear is the biggest threat — both players need PvP Trinkets. Trinket Fear immediately to avoid chain CC.\n- Warlock will try to Curse of Exhaustion + Drain Life kite — Blink is your answer.\n- If Priest is Shadow: they deal significant damage themselves; consider killing Priest first if Warlock is defensive spec.\n- Counterspell should be saved for big heals (Greater Heal / Flash Heal) not DoT casts."
  },
  {
    "id": "mage_rogue_vs_druid_warlock",
    "enemyClass1Id": "druid",
    "enemyClass2Id": "warlock",
    "strategyMarkdown": "## Kill Target\nWarlock\n\n## Win Condition\nPoly the Druid repeatedly. Rogue bursts Warlock. Mage interrupts Drain Life with Counterspell. This is a manageable matchup if Poly is kept on Druid.\n\n---\n\n## Key Notes\n\n- Druid will use Nature's Grasp defensively — don't stand in melee as Mage.\n- Warlock Fear: trinket immediately, do not let them chain Fear into a Poly from Druid.\n- If Druid swaps to Tree of Life, expect sustained healing — switch damage to Druid briefly to force a defensive cooldown.\n- Counterspell Drain Life, not Shadowbolt — the sustained drain is the real threat."
  },
  {
    "id": "mage_rogue_vs_rogue_warlock",
    "enemyClass1Id": "rogue",
    "enemyClass2Id": "warlock",
    "strategyMarkdown": "## Kill Target\nWarlock\n\n## Win Condition\nFrost Nova the Warlock, Rogue + Mage burst while Rogue has the enemy Rogue controlled. CC chain the Warlock during burst windows.\n\n---\n\n## Key Notes\n\n- Enemy Rogue will attempt to Blind your Rogue at the same time you try to Blind theirs — anticipate the timing.\n- Warlock will Fear — your PvP Trinket should break it. Rogue uses Berserker Rage if available.\n- Rogue/Warlock is a sustained pressure comp. Don't let them kite you — stay aggressive."
  },
  {
    "id": "mage_rogue_vs_druid_rogue",
    "enemyClass1Id": "druid",
    "enemyClass2Id": "rogue",
    "strategyMarkdown": "## Kill Target\nDruid\n\n## Win Condition\nOne of the hardest matchups. The enemy Druid has unmatched mobility and mana regeneration. Focus sustained pressure on the Druid — force cooldown usage. Your Rogue should Blind the enemy Rogue when he tries to peel for Druid.\n\n---\n\n## Key Notes\n\n- Druid will shift out of Polymorph instantly — don't spam Poly. Use it to buy a GCD during burst.\n- Druid's HoTs mean you need to burst hard after a CS, not spread damage.\n- If Druid goes into Moonkin, treat him as a damage dealer and consider swapping target.\n- Patience is key — they play for attrition, you need to create 1v1 windows."
  },
  {
    "id": "mage_rogue_vs_druid_mage",
    "enemyClass1Id": "druid",
    "enemyClass2Id": "mage",
    "strategyMarkdown": "## Kill Target\nEnemy Mage\n\n## Win Condition\nCS the enemy Mage immediately on burst windows. Your Mage mirrors their Mage while your Rogue kills them. Poly the Druid to prevent healing.\n\n---\n\n## Key Notes\n\n- Enemy Mage will attempt to CS your Mage — position behind a pillar to LoS.\n- If enemy Mage has Ice Block, coordinate the burst to burn it, then finish.\n- Druid will spam Rejuvenation on the enemy Mage — your burst needs to overcome HoT healing."
  },
  {
    "id": "mage_rogue_vs_mage_priest",
    "enemyClass1Id": "mage",
    "enemyClass2Id": "priest",
    "strategyMarkdown": "## Kill Target\nEnemy Mage\n\n## Win Condition\nCS the enemy Mage during Rogue burst windows. Blind the Priest. Enemy Mage dies before Priest can recover. Clean up Priest after.\n\n---\n\n## Key Notes\n\n- Shadow Priest adds heavy pressure — don't let Vampiric Embrace sustain them.\n- If Holy Priest: straightforward. CS Greater Heal, Rogue kills Mage fast.\n- Enemy Mage will try to CS your Mage too — be ready to Blink out of burst range."
  },
  {
    "id": "mage_rogue_vs_druid_hunter",
    "enemyClass1Id": "druid",
    "enemyClass2Id": "hunter",
    "strategyMarkdown": "## Kill Target\nHunter\n\n## Win Condition\nAvoid Freeze Trap entirely. Poly the Druid, Rogue closes on Hunter. Use Blink to break Frost Trap if caught. Burst Hunter during Druid Poly.\n\n---\n\n## Key Notes\n\n- Hunter has Deterrence — coordinate burst to avoid wasting it.\n- Druid will Innervate after mana drain — don't waste CC on low-mana Druid.\n- Watch for Hunter's Scatter Shot breaking your Rogue's CCs.\n- Freezing Trap on your Rogue — Mage Ice Block shares DR with Frost effects, so trinket or wait out the short duration."
  },
  {
    "id": "mage_rogue_vs_hunter_priest",
    "enemyClass1Id": "hunter",
    "enemyClass2Id": "priest",
    "strategyMarkdown": "## Kill Target\nHunter\n\n## Win Condition\nSilence / CS Priest to prevent healing. Rogue closes on Hunter and bursts. Mage uses Blink to close gap if Hunter Disengage.\n\n---\n\n## Key Notes\n\n- Scatter Shot into Freeze Trap is their win condition — never stand together as a team.\n- Rogue uses Blind on Priest during burst window on Hunter.\n- Shadow Priest + Hunter can have insane sustained DPS — don't let the fight go long."
  },
  {
    "id": "mage_rogue_vs_hunter_rogue",
    "enemyClass1Id": "hunter",
    "enemyClass2Id": "rogue",
    "strategyMarkdown": "## Kill Target\nHunter\n\n## Win Condition\nSmoke Screen negates Hunter's ranged damage on your Rogue. Poly the enemy Rogue briefly while your Rogue kills Hunter. Mage Frost Nova keeps Hunter in place.\n\n---\n\n## Key Notes\n\n- Smoke Screen is extremely strong here — use it aggressively during burst.\n- Enemy Rogue will try to Blind your Mage to prevent Poly. Rogue should Blind their Rogue first.\n- Hunter's Disengage + Trap combination: never stand on a blue circle (Frost Trap already placed)."
  },
  {
    "id": "mage_rogue_vs_hunter_warrior",
    "enemyClass1Id": "hunter",
    "enemyClass2Id": "warrior",
    "strategyMarkdown": "## Kill Target\nHunter\n\n## Win Condition\nSheep the Warrior, kill Hunter. Heavy cleave comp but both targets are controllable. Frost Nova Warrior to prevent him reaching your Mage.\n\n---\n\n## Key Notes\n\n- Keep Warrior sheeped — he's the cleave threat, not the primary kill target.\n- Hunter Disengage makes burst tricky — use Rogue's Cheap Shot to lock him down.\n- Counterspell is not very useful here (Hunter is physical) — use it to interrupt Warrior Shouts if needed."
  },
  {
    "id": "mage_rogue_vs_rogue_shaman",
    "enemyClass1Id": "rogue",
    "enemyClass2Id": "shaman",
    "strategyMarkdown": "## Kill Target\nShaman\n\n## Win Condition\nInterrupt Earth Shield application with Counterspell or Kick. Poly the enemy Rogue when Shaman is in danger. Burst Shaman during CS lockout.\n\n---\n\n## Key Notes\n\n- Shaman's Purge will remove your Mage's Mana Shield — reapply when safe.\n- Grounding Totem negates Poly — Kick it or wait for it to expire before casting.\n- Shaman healing is powerful but interruptible — CS on every Healing Wave cast.\n- Enemy Rogue will peel for Shaman with Blind on your Mage."
  },
  {
    "id": "mage_rogue_vs_mage_shaman",
    "enemyClass1Id": "mage",
    "enemyClass2Id": "shaman",
    "strategyMarkdown": "## Kill Target\nEnemy Mage\n\n## Win Condition\nCS the enemy Mage, Rogue bursts. Shaman alone cannot sustain their Mage under coordinated pressure with healing interrupts.\n\n---\n\n## Key Notes\n\n- Grounding Totem will eat your Poly — always destroy it before sheeping.\n- Shaman will Wind Shear your Mage's Frostbolts — be aware of interrupt lockout.\n- Focus the enemy Mage hard — Shaman's off-heals aren't enough if Mage dies fast."
  },
  {
    "id": "mage_rogue_vs_shaman_warrior",
    "enemyClass1Id": "shaman",
    "enemyClass2Id": "warrior",
    "strategyMarkdown": "## Kill Target\nWarrior\n\n## Win Condition\nInterrupt Shaman heals with CS. Frost Nova Warrior, burst him down. Rogue uses Blind on Shaman during burst.\n\n---\n\n## Key Notes\n\n- Shaman's totems provide powerful utility — Tremor Totem removes Fear (not relevant here), Grounding eats Poly.\n- Destroy Grounding Totem before every Poly attempt.\n- Warrior's charge gap closer is dangerous for Mage — keep pillars between you."
  },
  {
    "id": "mage_rogue_vs_hunter_shaman",
    "enemyClass1Id": "hunter",
    "enemyClass2Id": "shaman",
    "strategyMarkdown": "## Kill Target\nShaman\n\n## Win Condition\nRogue locks down Shaman with stuns while Mage CS interrupts heals. Hunter is largely ignored until Shaman dies.\n\n---\n\n## Key Notes\n\n- Grounding Totem will eat Poly — kick it before attempting CC.\n- Scatter Shot from Hunter will break your Rogue's CC chains — stay spread.\n- Once Shaman dies, 1v1 Hunter with Rogue is straightforward."
  },
  {
    "id": "mage_rogue_vs_paladin_warrior",
    "enemyClass1Id": "paladin",
    "enemyClass2Id": "warrior",
    "strategyMarkdown": "## Kill Target\nWarrior\n\n## Win Condition\nCS Divine Shield (Bubble) the moment Paladin casts it. With Bubble CS'd, burst Warrior immediately. Rogue should save Kidney Shot for the post-bubble window.\n\n---\n\n## Key Notes\n\n- This is one of the hardest matchups. Paladin has near-unlimited mana and cannot be out-healed.\n- Counterspell timing on Bubble is everything — a missed CS loses the game.\n- You cannot outlast them on mana. Create burst windows and commit fully.\n- Warrior will be very aggressive — use Frost Nova + Blink to peel off your Mage."
  },
  {
    "id": "mage_rogue_vs_paladin_rogue",
    "enemyClass1Id": "paladin",
    "enemyClass2Id": "rogue",
    "strategyMarkdown": "## Kill Target\nPaladin\n\n## Win Condition\nCS Divine Shield. Rogue bursts Paladin while enemy Rogue is Polyed. Paladin dies before they can recover without bubble.\n\n---\n\n## Key Notes\n\n- Enemy Rogue's Blind will be aimed at your Mage — anticipate and position behind pillars.\n- Without bubble, Paladins are surprisingly killable under coordinated burst.\n- Watch for Paladin's Repentance — it's a cast that CCs your Rogue. Kick or LoS it."
  },
  {
    "id": "mage_rogue_vs_paladin_warlock",
    "enemyClass1Id": "paladin",
    "enemyClass2Id": "warlock",
    "strategyMarkdown": "## Kill Target\nWarlock\n\n## Win Condition\nSave CS for Divine Shield. Fear from Warlock is the primary threat — trinket it. Rogue Blinds Paladin while Mage bursts Warlock with CS on Drain Life.\n\n---\n\n## Key Notes\n\n- Paladin/Warlock sustains through Fear + slow Paladin heals. Create burst windows after CS'ing bubble.\n- Warlock's Curse of Exhaustion makes kiting difficult — Blink is critical.\n- If Warlock goes Demon Form (Metamorphosis didn't exist in TBC — this is a Felguard comp), destroy the pet."
  },
  {
    "id": "mage_rogue_vs_mage_paladin",
    "enemyClass1Id": "mage",
    "enemyClass2Id": "paladin",
    "strategyMarkdown": "## Kill Target\nEnemy Mage\n\n## Win Condition\nCS the enemy Mage. Rogue burst kills Mage while Paladin is LoS'd behind a pillar. Paladin cannot heal what he cannot see.\n\n---\n\n## Key Notes\n\n- This is a LoS-heavy matchup. Force Paladin to walk out from behind pillars to heal.\n- CS Divine Shield to prevent Paladin from saving the Mage during burst.\n- Enemy Mage's Ice Block gives them one free emergency — plan burst around burning it first."
  },
  {
    "id": "mage_rogue_vs_hunter_paladin",
    "enemyClass1Id": "hunter",
    "enemyClass2Id": "paladin",
    "strategyMarkdown": "## Kill Target\nHunter\n\n## Win Condition\nCS Paladin's Divine Shield / heals. Rogue closes on Hunter using pillars. Freeze Trap avoidance is critical.\n\n---\n\n## Key Notes\n\n- Hunter Disengage + Paladin peel is their defensive rotation. Aggressive re-engagement after every disengage.\n- Paladin's Repentance on your Rogue enables Hunter free damage — kick Repentance cast.\n- If both healers (Divine Shield + Mend Pet) are active simultaneously, sit on Hunter and wait them out."
  },
  {
    "id": "mage_rogue_vs_priest_rogue",
    "enemyClass1Id": "priest",
    "enemyClass2Id": "rogue",
    "strategyMarkdown": "## Kill Target\nPriest\n\n## Win Condition\nBlind the enemy Rogue. Rogue + Mage burst the Priest with CS on heals. Without a Rogue peel, Priest dies quickly under burst.\n\n---\n\n## Key Notes\n\n- Priest's Psychic Scream: stand near pillars to LoS the fear.\n- Shadow Priest variant: Respectable damage output — CS Vampiric Touch/Mind Blast, not just heals.\n- Enemy Rogue's Blind will target your Mage — position carefully."
  },
  {
    "id": "mage_rogue_vs_shaman_warlock",
    "enemyClass1Id": "shaman",
    "enemyClass2Id": "warlock",
    "strategyMarkdown": "## Kill Target\nWarlock\n\n## Win Condition\nInterrupt Shaman heals. Rogue Blinds Shaman during Warlock burst. CS Drain Life on Warlock.\n\n---\n\n## Key Notes\n\n- Grounding Totem will absorb Poly — destroy it before CCing Shaman.\n- Warlock Fear is the primary threat — PvP Trinket the Fear and re-engage.\n- Wind Shear from Shaman will lock out your Mage's Frost school — avoid standing in Shaman's melee range."
  }
]
```

- [ ] **Step 2: Commit**
```bash
git add composeApp/src/commonMain/composeResources/files/matchups/
git commit -m "feat: add Rogue/Mage matchup strategy data (25 compositions)"
```

---

## Task 10: UI Theme

**Files:**
- Create: `presentation/theme/Theme.kt`

- [ ] **Step 1: Create Theme.kt**

```kotlin
package net.tautellini.arenatactics.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// Background layers
val Background   = Color(0xFF0D0D0F)
val Surface      = Color(0xFF16161A)
val CardColor    = Color(0xFF1C1C21)
val CardElevated = Color(0xFF222228)

// Text
val TextPrimary   = Color(0xFFE8E1D6)
val TextSecondary = Color(0xFF8A8490)

// Accent
val Accent = Color(0xFFC89B3C)

// Divider
val DividerColor = Color(0xFF2A2A32)

// WoW class colors
val ClassColors = mapOf(
    "druid"   to Color(0xFFFF7D0A),
    "hunter"  to Color(0xFFABD473),
    "mage"    to Color(0xFF69CCF0),
    "paladin" to Color(0xFFF58CBA),
    "priest"  to Color(0xFFFFFFFF),
    "rogue"   to Color(0xFFFFF569),
    "shaman"  to Color(0xFF0070DE),
    "warlock" to Color(0xFF9482C9),
    "warrior" to Color(0xFFC79C6E)
)

fun classColor(classId: String): Color = ClassColors[classId] ?: TextPrimary

private val DarkColors = darkColorScheme(
    background = Background,
    surface = Surface,
    primary = Accent,
    onPrimary = Background,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun ArenaTacticsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
```

- [ ] **Step 2: Commit**
```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/theme/
git commit -m "feat: add ArenaTactics dark theme with WoW class colors"
```

---

## Task 11: Shared UI Components

**Files:**
- Create: `presentation/screens/components/ClassBadge.kt`
- Create: `presentation/screens/components/CompositionCard.kt`
- Create: `presentation/screens/components/ItemRow.kt`
- Create: `presentation/screens/components/MarkdownText.kt`

- [ ] **Step 1: Create ClassBadge.kt**

```kotlin
package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.presentation.theme.Background
import net.tautellini.arenatactics.presentation.theme.classColor

@Composable
fun ClassBadge(classId: String, className: String, modifier: Modifier = Modifier) {
    val color = classColor(classId)
    Text(
        text = className,
        color = Background,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
```

- [ ] **Step 2: Create CompositionCard.kt**

```kotlin
package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.tautellini.arenatactics.data.model.WowClass
import net.tautellini.arenatactics.domain.RichComposition
import net.tautellini.arenatactics.presentation.theme.CardColor

@Composable
fun CompositionCard(
    richComposition: RichComposition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = CardColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClassBadge(richComposition.class1.id, richComposition.class1.name)
            ClassBadge(richComposition.class2.id, richComposition.class2.name)
        }
    }
}
```

- [ ] **Step 3: Create ItemRow.kt**

```kotlin
package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.data.model.GearItem
import net.tautellini.arenatactics.openUrl
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun ItemRow(item: GearItem, modifier: Modifier = Modifier) {
    val wowheadUrl = "https://www.wowhead.com/tbc/item=${item.wowheadId}"
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { openUrl(wowheadUrl) }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.slot,
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.width(80.dp)
            )
            Text(
                text = item.name,
                color = Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        if (item.enchant != null || item.gems.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(start = 88.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (item.enchant != null) {
                    Text(
                        text = "✦ ${item.enchant}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                if (item.gems.isNotEmpty()) {
                    Text(
                        text = item.gems.joinToString(" · ") { "◆ $it" },
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 4: Create MarkdownText.kt**

```kotlin
package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        markdown.lines().forEach { line ->
            when {
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## "),
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("•", color = Accent, fontSize = 13.sp)
                        Text(
                            text = parseInline(line.drop(2)),
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                    }
                }
                line.trim() == "---" -> {
                    Divider(
                        color = DividerColor,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                line.isEmpty() -> {
                    Spacer(Modifier.height(4.dp))
                }
                else -> {
                    Text(
                        text = parseInline(line),
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

internal fun parseInline(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i]); i++
                }
            }
            text.startsWith("*", i) -> {
                val end = text.indexOf("*", i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i]); i++
                }
            }
            else -> { append(text[i]); i++ }
        }
    }
}
```

- [ ] **Step 5: Commit**
```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/components/
git commit -m "feat: add shared UI components (ClassBadge, CompositionCard, ItemRow, MarkdownText)"
```

---

## Task 12: GameModeSelection Screen

**Files:**
- Create: `presentation/GameModeSelectionViewModel.kt`
- Create: `presentation/screens/GameModeSelectionScreen.kt`

- [ ] **Step 1: Create GameModeSelectionViewModel.kt**

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
    private val repository: GameModeRepository
) : ViewModel() {
    private val _state = MutableStateFlow<GameModeSelectionState>(GameModeSelectionState.Loading)
    val state: StateFlow<GameModeSelectionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                GameModeSelectionState.Success(repository.getAll())
            } catch (e: Exception) {
                GameModeSelectionState.Error(e.message ?: "Failed to load game modes")
            }
        }
    }
}
```

- [ ] **Step 2: Create GameModeSelectionScreen.kt**

```kotlin
package net.tautellini.arenatactics.presentation.screens

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
import net.tautellini.arenatactics.data.model.GameMode
import net.tautellini.arenatactics.navigation.Navigator
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.GameModeSelectionState
import net.tautellini.arenatactics.presentation.GameModeSelectionViewModel
import net.tautellini.arenatactics.presentation.screens.components.CompositionCard
import net.tautellini.arenatactics.presentation.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface

@Composable
fun GameModeSelectionScreen(
    viewModel: GameModeSelectionViewModel,
    navigator: Navigator
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize().background(Background)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Spacer(Modifier.height(48.dp))
            Text(
                text = "ArenaTactics",
                color = Accent,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Select your arena bracket",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            when (val s = state) {
                is GameModeSelectionState.Loading -> {
                    CircularProgressIndicator(color = Accent, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                is GameModeSelectionState.Error -> {
                    Text(s.message, color = TextSecondary)
                }
                is GameModeSelectionState.Success -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(s.modes) { mode ->
                            GameModeCard(mode) {
                                navigator.push(Screen.CompositionSelection(mode.id))
                            }
                        }
                    }
                }
            }
        }

        // Footer
        Text(
            text = "Made with love for Kizaru",
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}

@Composable
private fun GameModeCard(mode: GameMode, onClick: () -> Unit) {
    Surface(
        color = CardColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(mode.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(mode.description, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
```

- [ ] **Step 3: Commit**
```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/GameModeSelectionViewModel.kt \
  composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/GameModeSelectionScreen.kt
git commit -m "feat: add GameModeSelection screen and ViewModel"
```

---

## Task 13: CompositionSelection Screen

**Files:**
- Create: `presentation/CompositionSelectionViewModel.kt`
- Create: `presentation/screens/CompositionSelectionScreen.kt`

- [ ] **Step 1: Create CompositionSelectionViewModel.kt**

```kotlin
package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.GameMode
import net.tautellini.arenatactics.data.repository.CompositionRepository
import net.tautellini.arenatactics.data.repository.GameModeRepository
import net.tautellini.arenatactics.domain.CompositionGenerator
import net.tautellini.arenatactics.domain.RichComposition

sealed class CompositionSelectionState {
    data object Loading : CompositionSelectionState()
    data class Success(val compositions: List<RichComposition>) : CompositionSelectionState()
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
                val classes = compositionRepository.getClasses(mode.classPoolId)
                val compositions = compositionRepository.getCompositions(mode.compositionSetId)
                val rich = CompositionGenerator.generate(classes, compositions)
                CompositionSelectionState.Success(rich)
            } catch (e: Exception) {
                CompositionSelectionState.Error(e.message ?: "Failed to load compositions")
            }
        }
    }
}
```

- [ ] **Step 2: Create CompositionSelectionScreen.kt**

```kotlin
package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
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
import net.tautellini.arenatactics.navigation.Navigator
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.CompositionSelectionState
import net.tautellini.arenatactics.presentation.CompositionSelectionViewModel
import net.tautellini.arenatactics.presentation.screens.components.CompositionCard
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun CompositionSelectionScreen(
    gameModeId: String,
    viewModel: CompositionSelectionViewModel,
    navigator: Navigator
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(Background).padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton { navigator.pop() }
            Spacer(Modifier.width(12.dp))
            Text("Select Composition", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(24.dp))

        when (val s = state) {
            is CompositionSelectionState.Loading -> CircularProgressIndicator(color = Accent)
            is CompositionSelectionState.Error -> Text(s.message, color = TextSecondary)
            is CompositionSelectionState.Success -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(s.compositions) { rich ->
                        CompositionCard(rich) {
                            navigator.push(Screen.GearView(gameModeId, rich.composition.id))
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Commit**
```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/CompositionSelectionViewModel.kt \
  composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/CompositionSelectionScreen.kt
git commit -m "feat: add CompositionSelection screen and ViewModel"
```

---

## Task 14: Gear Screen

**Files:**
- Create: `presentation/GearViewModel.kt`
- Create: `presentation/screens/GearScreen.kt`

- [ ] **Step 1: Create GearViewModel.kt**

```kotlin
package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.GearPhase
import net.tautellini.arenatactics.data.repository.CompositionRepository
import net.tautellini.arenatactics.data.repository.GameModeRepository
import net.tautellini.arenatactics.data.repository.GearRepository

sealed class GearState {
    data object Loading : GearState()
    data class Success(
        val gearByClass: Map<String, List<GearPhase>>,  // classId -> list of phases
        val classNames: Map<String, String>              // classId -> display name
    ) : GearState()
    data class Error(val message: String) : GearState()
}

class GearViewModel(
    private val gameModeId: String,
    private val compositionId: String,
    private val gameModeRepository: GameModeRepository,
    private val compositionRepository: CompositionRepository,
    private val gearRepository: GearRepository
) : ViewModel() {
    private val _state = MutableStateFlow<GearState>(GearState.Loading)
    val state: StateFlow<GearState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                val mode = gameModeRepository.getAll().first { it.id == gameModeId }
                val classes = compositionRepository.getClasses(mode.classPoolId)
                val classNameMap = classes.associate { it.id to it.name }
                val gear = gearRepository.getGearForComposition(compositionId, mode.compositionSetId)
                GearState.Success(gear, classNameMap)
            } catch (e: Exception) {
                GearState.Error(e.message ?: "Failed to load gear")
            }
        }
    }
}
```

- [ ] **Step 2: Create GearScreen.kt**

```kotlin
package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.navigation.Navigator
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.GearState
import net.tautellini.arenatactics.presentation.GearViewModel
import net.tautellini.arenatactics.presentation.MatchupListViewModel
import net.tautellini.arenatactics.presentation.screens.components.ItemRow
import net.tautellini.arenatactics.presentation.theme.*
import net.tautellini.arenatactics.refreshWowheadTooltips

enum class CompositionTab { GEAR, MATCHUPS }

@Composable
fun CompositionHubScreen(
    gameModeId: String,
    compositionId: String,
    gearViewModel: GearViewModel,
    matchupListViewModel: MatchupListViewModel,
    navigator: Navigator
) {
    var selectedTab by remember { mutableStateOf(CompositionTab.GEAR) }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton { navigator.pop() }
            Spacer(Modifier.width(12.dp))
            Text(compositionId.replace("_", "/").replaceFirstChar { it.uppercase() },
                color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        // Tabs
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            CompositionTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = if (selected) Accent else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
        Divider(color = DividerColor)

        when (selectedTab) {
            CompositionTab.GEAR -> GearTabContent(gearViewModel)
            CompositionTab.MATCHUPS -> MatchupListScreen(
                gameModeId = gameModeId,
                compositionId = compositionId,
                viewModel = matchupListViewModel,
                navigator = navigator
            )
        }
    }
}

@Composable
private fun GearTabContent(viewModel: GearViewModel) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        if (state is GearState.Success) refreshWowheadTooltips()
    }

    when (val s = state) {
        is GearState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Accent)
        }
        is GearState.Error -> Text(s.message, color = TextSecondary, modifier = Modifier.padding(24.dp))
        is GearState.Success -> {
            LazyColumn {
                s.gearByClass.forEach { (classId, phases) ->
                    val className = s.classNames[classId] ?: classId
                    phases.forEach { phase ->
                        item {
                            Text(
                                text = "$className — Phase ${phase.phase}",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 8.dp)
                            )
                        }
                        items(phase.items) { item ->
                            ItemRow(item)
                            Divider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Commit**
```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/GearViewModel.kt \
  composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/GearScreen.kt
git commit -m "feat: add Gear screen with tabbed composition hub"
```

---

## Task 15: Matchup List + Detail Screens

**Files:**
- Create: `presentation/MatchupListViewModel.kt`
- Create: `presentation/MatchupDetailViewModel.kt`
- Create: `presentation/screens/MatchupListScreen.kt`
- Create: `presentation/screens/MatchupDetailScreen.kt`

- [ ] **Step 1: Create MatchupListViewModel.kt**

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
import net.tautellini.arenatactics.data.repository.CompositionRepository
import net.tautellini.arenatactics.data.repository.GameModeRepository
import net.tautellini.arenatactics.data.repository.MatchupRepository

sealed class MatchupListState {
    data object Loading : MatchupListState()
    data class Success(
        val matchups: List<Matchup>,
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
                val mode = gameModeRepository.getAll().first { it.id == gameModeId }
                val classes = compositionRepository.getClasses(mode.classPoolId)
                val classMap = classes.associateBy { it.id }
                val matchups = matchupRepository.getForComposition(compositionId)
                MatchupListState.Success(matchups, classMap)
            } catch (e: Exception) {
                MatchupListState.Error(e.message ?: "Failed to load matchups")
            }
        }
    }
}
```

- [ ] **Step 2: Create MatchupDetailViewModel.kt**

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
import net.tautellini.arenatactics.data.repository.CompositionRepository
import net.tautellini.arenatactics.data.repository.GameModeRepository
import net.tautellini.arenatactics.data.repository.MatchupRepository

sealed class MatchupDetailState {
    data object Loading : MatchupDetailState()
    data class Success(val matchup: Matchup, val classMap: Map<String, WowClass>) : MatchupDetailState()
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
                val mode = gameModeRepository.getAll().first { it.id == gameModeId }
                val classes = compositionRepository.getClasses(mode.classPoolId)
                val classMap = classes.associateBy { it.id }
                val matchup = matchupRepository.getById(compositionId, matchupId)
                    ?: throw IllegalArgumentException("Matchup not found: $matchupId")
                MatchupDetailState.Success(matchup, classMap)
            } catch (e: Exception) {
                MatchupDetailState.Error(e.message ?: "Failed to load matchup")
            }
        }
    }
}
```

- [ ] **Step 3: Create MatchupListScreen.kt**

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.navigation.Navigator
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.MatchupListState
import net.tautellini.arenatactics.presentation.MatchupListViewModel
import net.tautellini.arenatactics.presentation.screens.components.ClassBadge
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
        is MatchupListState.Error -> Text(s.message, color = TextSecondary, modifier = Modifier.padding(24.dp))
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
                            val c1 = s.classMap[matchup.enemyClass1Id]
                            val c2 = s.classMap[matchup.enemyClass2Id]
                            if (c1 != null) ClassBadge(c1.id, c1.name)
                            if (c2 != null) ClassBadge(c2.id, c2.name)
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 4: Create MatchupDetailScreen.kt**

```kotlin
package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import net.tautellini.arenatactics.presentation.screens.components.ClassBadge
import net.tautellini.arenatactics.presentation.screens.components.MarkdownText
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
            is MatchupDetailState.Error -> Text(s.message, color = TextSecondary, modifier = Modifier.padding(24.dp))
            is MatchupDetailState.Success -> {
                val matchup = s.matchup
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackButton { navigator.pop() }
                    Spacer(Modifier.width(12.dp))
                    Text("vs", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                    val c1 = s.classMap[matchup.enemyClass1Id]
                    val c2 = s.classMap[matchup.enemyClass2Id]
                    if (c1 != null) ClassBadge(c1.id, c1.name, modifier = Modifier.padding(end = 6.dp))
                    if (c2 != null) ClassBadge(c2.id, c2.name)
                }
                // Strategy content
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
```

- [ ] **Step 5: Commit**
```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/MatchupListViewModel.kt \
  composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/MatchupDetailViewModel.kt \
  composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/MatchupListScreen.kt \
  composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/MatchupDetailScreen.kt
git commit -m "feat: add Matchup list and detail screens with ViewModels"
```

---

## Task 16: App.kt Wiring + webMain

**Files:**
- Rewrite: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/App.kt`
- Rewrite: `composeApp/src/webMain/kotlin/net/tautellini/arenatactics/main.kt`
- Add: `presentation/screens/components/BackButton.kt` (referenced above)

- [ ] **Step 1: Create BackButton.kt**

```kotlin
package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.presentation.theme.TextSecondary

@Composable
fun BackButton(onClick: () -> Unit) {
    Text(
        text = "← Back",
        color = TextSecondary,
        fontSize = 13.sp,
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp)
    )
}
```

- [ ] **Step 2: Rewrite App.kt**

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
    val gameModeRepository = remember { GameModeRepository() }
    val compositionRepository = remember { CompositionRepository() }
    val gearRepository = remember { GearRepository(compositionRepository) }
    val matchupRepository = remember { MatchupRepository() }
    val navigator = remember { Navigator() }
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
            is Screen.GearView, is Screen.MatchupList -> {
                // Both GearView and MatchupList share the composition hub screen (tabs)
                val gameModeId = when (screen) {
                    is Screen.GearView -> screen.gameModeId
                    is Screen.MatchupList -> screen.gameModeId
                    else -> return@ArenaTacticsTheme
                }
                val compositionId = when (screen) {
                    is Screen.GearView -> screen.compositionId
                    is Screen.MatchupList -> screen.compositionId
                    else -> return@ArenaTacticsTheme
                }
                val gearVm = viewModel(key = "gear_$compositionId") {
                    GearViewModel(gameModeId, compositionId, gameModeRepository, compositionRepository, gearRepository)
                }
                val matchupVm = viewModel(key = "matchups_$compositionId") {
                    MatchupListViewModel(gameModeId, compositionId, gameModeRepository, compositionRepository, matchupRepository)
                }
                CompositionHubScreen(gameModeId, compositionId, gearVm, matchupVm, navigator)
            }
            is Screen.MatchupDetail -> {
                val vm = viewModel(key = screen.matchupId) {
                    MatchupDetailViewModel(screen.gameModeId, screen.compositionId, screen.matchupId,
                        gameModeRepository, compositionRepository, matchupRepository)
                }
                MatchupDetailScreen(vm, navigator)
            }
        }
    }
}
```

- [ ] **Step 3: Rewrite webMain/main.kt**

```kotlin
package net.tautellini.arenatactics

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Inject Wowhead tooltip script
    val script = document.createElement("script")
    script.setAttribute("src", "https://wow.zamimg.com/widgets/power.js")
    document.head!!.appendChild(script)

    // popstate (browser back button) is wired inside App.kt via registerPopCallback
    // after the Navigator is created, so no wiring is needed here.

    ComposeViewport(document.body!!) {
        App()
    }
}
```

- [ ] **Step 3b: Wire popstate in App.kt**

In `App.kt`, add a `DisposableEffect` immediately after `val navigator = remember { Navigator() }`:

```kotlin
val navigator = remember { Navigator() }

// Wire browser back button → navigator.pop()
DisposableEffect(navigator) {
    registerPopCallback { navigator.pop() }
    onDispose { registerPopCallback {} }
}
```

- [ ] **Step 4: Run full build to catch compilation errors**
```bash
./gradlew :composeApp:compileKotlinJvm
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Run all tests**
```bash
./gradlew :composeApp:jvmTest
```
Expected: All tests PASS

- [ ] **Step 6: Run the desktop app to smoke test manually**
```bash
./gradlew :composeApp:run
```
Expected: Desktop window opens showing ArenaTactics — navigate through all screens

- [ ] **Step 7: Run the web (WASM) build**
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```
Expected: Browser opens, app loads, navigation works, Wowhead tooltips appear on hover

- [ ] **Step 8: Final commit**
```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/App.kt \
  composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/components/BackButton.kt \
  composeApp/src/webMain/kotlin/net/tautellini/arenatactics/main.kt
git commit -m "feat: wire App.kt navigation and complete webMain entry point"
```

---

## Final Checklist

- [ ] `./gradlew :composeApp:jvmTest` — all tests pass
- [ ] `./gradlew :composeApp:run` — desktop app navigates all 5 screens correctly
- [ ] `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` — web app loads and tooltips work
- [ ] Footer "Made with love for Kizaru" visible on GameModeSelection screen
- [ ] Gear items link to Wowhead on click
- [ ] Matchup strategies render correctly with markdown formatting
