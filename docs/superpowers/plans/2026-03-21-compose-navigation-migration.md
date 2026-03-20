# Compose Navigation Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the custom `Navigator` (StateFlow back-stack) with `androidx.navigation:navigation-compose`, adding fade+scale screen transitions, shared element transitions (shield logo, spec badges), a persistent AppShell with breadcrumb navigation, and correct browser forward/back support.

**Architecture:** `SharedTransitionLayout` wraps `NavHost` (with transitions) in `App.kt`. `Screen` sealed class gains `@Serializable` for type-safe routes. All screens swap `Navigator` for `NavHostController`. `ShieldCanvas` is extracted to a shared component so both `GameModeSelectionScreen` (large, centered) and `AppHeader` (small, top-left) can use the same canvas with a `sharedElement` modifier injected by the caller.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.10.0, `org.jetbrains.androidx.navigation:navigation-compose` (KMP artifact — NOT the Android-only `androidx.navigation:navigation-compose`), `androidx.compose.animation.SharedTransitionLayout`.

**Source root:** `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/`

**Run tests:** `./gradlew :composeApp:allTests`
**Run app:** `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`

---

## File Map

| Action | File | Purpose |
|---|---|---|
| Modify | `gradle/libs.versions.toml` | Add `navigation-compose` version alias |
| Modify | `composeApp/build.gradle.kts` | Add `navigation-compose` commonMain dep |
| Modify | `navigation/Screen.kt` | Add `@Serializable`, add `NavBackStackEntry.toScreen()` |
| **Delete** | `navigation/Navigator.kt` | Replaced by `NavHostController` |
| **Delete** | `navigation/NavigatorTest.kt` → rename | Tests for `Screen.fromPath`/`buildStack` survive |
| **Create** | `navigation/ScreenNavigationTest.kt` | Replaces NavigatorTest: tests Screen static methods only |
| Modify | `Platform.kt` (+ all 3 actuals) | Add `getCurrentPath(): String` expect/actual |
| **Create** | `presentation/screens/components/ShieldLogo.kt` | Extracted from GameModeSelectionScreen; `internal`; `modifier` param |
| **Create** | `presentation/screens/components/AppHeader.kt` | Small shield + breadcrumb chips; shown on all non-home screens |
| Modify | `App.kt` | Full rewrite: `SharedTransitionLayout` + `NavHost` (with transitions) + URL bridge |
| Modify | `presentation/screens/GameModeSelectionScreen.kt` | Remove `ShieldLogo`, add `shieldModifier`, `Navigator` → `NavHostController` |
| Modify | `presentation/screens/CompositionSelectionScreen.kt` | Remove back button row; `Navigator` → `NavHostController` |
| Modify | `presentation/screens/components/CompositionCard.kt` | Fix hardcoded `specs[0]/[1]`; add `specBadgeModifier` lambda |
| Modify | `presentation/screens/GearScreen.kt` | Remove back button; `Navigator` → `NavHostController`; `initialTab` param; shared element spec badges; AppHeader label |
| Modify | `presentation/screens/MatchupListScreen.kt` | `Navigator` → `NavHostController` |
| Modify | `presentation/screens/MatchupDetailScreen.kt` | Remove back button; `Navigator` → `NavHostController`; AppHeader label |
| **Delete** | `presentation/screens/components/BackButton.kt` | No longer used |

---

## Task 1: Add navigation-compose dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `composeApp/build.gradle.kts`

- [ ] **Step 1: Look up the correct version**

Run this WebSearch to find the `org.jetbrains.androidx.navigation:navigation-compose` version compatible with CMP 1.10.0:

Search: `org.jetbrains.androidx.navigation navigation-compose maven central 2025 2026`

Or check: https://maven.pkg.jetbrains.space/public/p/compose/dev/org/jetbrains/androidx/navigation/navigation-compose/

The artifact group is `org.jetbrains.androidx.navigation`, artifact `navigation-compose`. As of early 2026 with CMP 1.10.0, expect a version around `2.9.x-alpha` or `2.8.x`. Use the latest stable or alpha that matches.

- [ ] **Step 2: Add version alias to `gradle/libs.versions.toml`**

In the `[versions]` section, add:
```toml
navigation-compose = "2.9.0-alpha04"   # verify the exact version in Step 1
```

In the `[libraries]` section, add:
```toml
navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "navigation-compose" }
```

- [ ] **Step 3: Add dependency to `composeApp/build.gradle.kts`**

In `commonMain.dependencies { ... }`, add after the existing lifecycle lines:
```kotlin
implementation(libs.navigation.compose)
```

- [ ] **Step 4: Verify the build compiles**

```bash
./gradlew :composeApp:compileKotlinJvm
```

Expected: BUILD SUCCESSFUL with no errors. If version is wrong, adjust version in `libs.versions.toml` until it resolves.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml composeApp/build.gradle.kts
git commit -m "build: add org.jetbrains.androidx.navigation:navigation-compose dependency"
```

---

## Task 2: Add @Serializable to Screen and NavBackStackEntry.toScreen()

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/navigation/Screen.kt`

The `Screen` sealed class needs `@Serializable` on each type so `navigation-compose` can use it for type-safe routes. We also add `NavBackStackEntry.toScreen()` here since it bridges the nav library back to our domain type.

- [ ] **Step 1: Add serializable annotations and toScreen() extension**

Replace the contents of `Screen.kt` with:

```kotlin
package net.tautellini.arenatactics.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable
    data object GameModeSelection : Screen()

    @Serializable
    data class CompositionSelection(val gameModeId: String) : Screen()

    @Serializable
    data class GearView(val gameModeId: String, val compositionId: String) : Screen()

    @Serializable
    data class MatchupList(val gameModeId: String, val compositionId: String) : Screen()

    @Serializable
    data class MatchupDetail(
        val gameModeId: String,
        val compositionId: String,
        val matchupId: String
    ) : Screen()

    val path: String get() = when (this) {
        is GameModeSelection    -> "/"
        is CompositionSelection -> "/modes/$gameModeId"
        is GearView             -> "/modes/$gameModeId/comp/$compositionId/gear"
        is MatchupList          -> "/modes/$gameModeId/comp/$compositionId/matchups"
        is MatchupDetail        -> "/modes/$gameModeId/comp/$compositionId/matchups/$matchupId"
    }

    companion object {
        fun fromPath(pathname: String): Screen {
            val segments = pathname.trim('/').split('/').filter { it.isNotEmpty() }
            val modesIdx = segments.indexOf("modes")
            if (modesIdx == -1) return GameModeSelection

            val rel = segments.drop(modesIdx)
            val gameModeId = rel.getOrNull(1) ?: return GameModeSelection
            if (rel.getOrNull(2) != "comp") return CompositionSelection(gameModeId)

            val compositionId = rel.getOrNull(3) ?: return CompositionSelection(gameModeId)
            return when (rel.getOrNull(4)) {
                "gear"     -> GearView(gameModeId, compositionId)
                "matchups" -> {
                    val matchupId = rel.getOrNull(5)
                    if (matchupId != null) MatchupDetail(gameModeId, compositionId, matchupId)
                    else MatchupList(gameModeId, compositionId)
                }
                else -> CompositionSelection(gameModeId)
            }
        }

        fun buildStack(screen: Screen): List<Screen> = when (screen) {
            is GameModeSelection    -> listOf(screen)
            is CompositionSelection -> listOf(GameModeSelection, screen)
            is GearView             -> listOf(GameModeSelection, CompositionSelection(screen.gameModeId), screen)
            is MatchupList          -> listOf(GameModeSelection, CompositionSelection(screen.gameModeId), screen)
            is MatchupDetail        -> listOf(
                GameModeSelection,
                CompositionSelection(screen.gameModeId),
                MatchupList(screen.gameModeId, screen.compositionId),
                screen
            )
        }
    }
}

/**
 * Converts a NavBackStackEntry to the corresponding [Screen] by reading typed route args.
 * Used in the web URL bridge to push browser history on destination changes.
 */
/**
 * NOTE on hasRoute API: `navigation-compose` KMP provides `destination.hasRoute(KClass)`.
 * If the compiler cannot find it, try the reified form `destination.hasRoute<T>()` instead.
 * Both are equivalent — use whichever the library version exposes.
 */
fun NavBackStackEntry.toScreen(): Screen = when {
    destination.hasRoute(Screen.GameModeSelection::class)    -> Screen.GameModeSelection
    destination.hasRoute(Screen.CompositionSelection::class) ->
        toRoute<Screen.CompositionSelection>().let { Screen.CompositionSelection(it.gameModeId) }
    destination.hasRoute(Screen.GearView::class)             ->
        toRoute<Screen.GearView>().let { Screen.GearView(it.gameModeId, it.compositionId) }
    destination.hasRoute(Screen.MatchupList::class)          ->
        toRoute<Screen.MatchupList>().let { Screen.MatchupList(it.gameModeId, it.compositionId) }
    destination.hasRoute(Screen.MatchupDetail::class)        ->
        toRoute<Screen.MatchupDetail>().let { Screen.MatchupDetail(it.gameModeId, it.compositionId, it.matchupId) }
    else                                                     -> Screen.GameModeSelection
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :composeApp:compileKotlinJvm
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/navigation/Screen.kt
git commit -m "feat: add @Serializable to Screen routes and NavBackStackEntry.toScreen() extension"
```

---

## Task 3: Add getCurrentPath() platform expect/actuals

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/Platform.kt`
- Modify: `composeApp/src/jvmMain/kotlin/net/tautellini/arenatactics/Platform.jvm.kt`
- Modify: `composeApp/src/jsMain/kotlin/net/tautellini/arenatactics/Platform.js.kt`
- Modify: `composeApp/src/wasmJsMain/kotlin/net/tautellini/arenatactics/Platform.wasmJs.kt`

`getInitialPath()` returns the path only at startup. `getCurrentPath()` is needed in the browser popstate callback to read the URL at the moment back/forward is pressed.

- [ ] **Step 1: Add expect to `Platform.kt`**

Add after the existing `getInitialPath()` line:
```kotlin
// Returns the current URL pathname (called from popstate callback). Returns "/" on JVM.
expect fun getCurrentPath(): String
```

- [ ] **Step 2: Add actual to `Platform.jvm.kt`**

Add:
```kotlin
actual fun getCurrentPath(): String = "/"
```

- [ ] **Step 3: Add actual to `Platform.js.kt`**

Add:
```kotlin
actual fun getCurrentPath(): String = window.location.pathname
```

- [ ] **Step 4: Add actual to `Platform.wasmJs.kt`**

Add:
```kotlin
actual fun getCurrentPath(): String = window.location.pathname
```

- [ ] **Step 5: Verify compilation**

```bash
./gradlew :composeApp:compileKotlinJvm
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/Platform.kt \
        composeApp/src/jvmMain/kotlin/net/tautellini/arenatactics/Platform.jvm.kt \
        composeApp/src/jsMain/kotlin/net/tautellini/arenatactics/Platform.js.kt \
        composeApp/src/wasmJsMain/kotlin/net/tautellini/arenatactics/Platform.wasmJs.kt
git commit -m "feat: add getCurrentPath() platform expect/actual for browser popstate bridge"
```

---

## Task 4: Extract ShieldLogo to shared component

**Files:**
- Create: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/components/ShieldLogo.kt`
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/GameModeSelectionScreen.kt`

`ShieldLogo` is currently `private` inside `GameModeSelectionScreen.kt`. Both `GameModeSelectionScreen` (large version) and `AppHeader` (small version) need to call it. Extract it to `components/ShieldLogo.kt` as `internal`, with a `modifier: Modifier = Modifier` parameter so the caller can inject a `sharedElement` modifier.

- [ ] **Step 1: Create `ShieldLogo.kt`**

Create `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/components/ShieldLogo.kt`:

```kotlin
package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import net.tautellini.arenatactics.presentation.theme.Accent
import net.tautellini.arenatactics.presentation.theme.CardColor
import net.tautellini.arenatactics.presentation.theme.Surface

private fun DrawScope.shieldPath(w: Float, h: Float): Path = Path().apply {
    val r = w * 0.08f
    moveTo(r, 0f)
    lineTo(w - r, 0f)
    quadraticTo(w, 0f, w, r)
    lineTo(w, h * 0.58f)
    cubicTo(w, h * 0.82f, w * 0.5f, h, w * 0.5f, h)
    cubicTo(0f, h * 0.82f, 0f, h * 0.58f, 0f, h * 0.58f)
    lineTo(0f, r)
    quadraticTo(0f, 0f, r, 0f)
    close()
}

/**
 * Draws the shield logo canvas. The caller MUST provide sizing via [modifier]
 * (e.g. `Modifier.size(220.dp, 250.dp)` for home screen, `Modifier.size(28.dp, 32.dp)` for AppHeader).
 * Do NOT chain .size() inside this composable — it would override the caller's size.
 */
@Composable
internal fun ShieldCanvas(modifier: Modifier = Modifier, shimmerX: Float = -1f) {
    Canvas(modifier = modifier) {
        val path = shieldPath(size.width, size.height)

        drawPath(
            path,
            brush = Brush.verticalGradient(
                listOf(Surface, CardColor),
                startY = 0f,
                endY = size.height
            )
        )
        drawPath(
            path,
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                startY = 0f,
                endY = size.height * 0.4f
            )
        )
        val sx = shimmerX * size.width
        drawPath(
            path,
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.0f to Color.Transparent,
                    0.4f to Color.Transparent,
                    0.5f to Color.White.copy(alpha = 0.18f),
                    0.6f to Color.Transparent,
                    1.0f to Color.Transparent
                ),
                start = Offset(sx, 0f),
                end = Offset(sx + size.width, size.height)
            )
        )
        drawPath(
            path,
            color = Accent,
            style = Stroke(
                width = 1.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        val inset = 6.dp.toPx()
        val innerPath = Path().apply {
            val r = (size.width * 0.08f) - inset
            moveTo(inset + r, inset)
            lineTo(size.width - inset - r, inset)
            quadraticTo(size.width - inset, inset, size.width - inset, inset + r)
            lineTo(size.width - inset, size.height * 0.56f)
            cubicTo(
                size.width - inset, size.height * 0.81f,
                size.width * 0.5f, size.height - inset,
                size.width * 0.5f, size.height - inset
            )
            cubicTo(
                inset, size.height * 0.81f,
                inset, size.height * 0.56f,
                inset, size.height * 0.56f
            )
            lineTo(inset, inset + r)
            quadraticTo(inset, inset, inset + r, inset)
            close()
        }
        drawPath(
            innerPath,
            color = Accent.copy(alpha = 0.25f),
            style = Stroke(width = 0.8.dp.toPx())
        )
    }
}
```

- [ ] **Step 2: Update `GameModeSelectionScreen.kt` — remove old ShieldLogo/shieldPath, import ShieldCanvas**

In `GameModeSelectionScreen.kt`:
1. Delete the `private fun DrawScope.shieldPath(...)` function (lines ~102-113).
2. Delete the entire `private fun ShieldLogo()` composable (lines ~115-233) — it contains the `Canvas`, shimmer animation, and the "Arena / Tactics" text.
3. Replace the deleted `ShieldLogo()` call (currently inside `Column`) with a new `ShieldLogo` composable defined locally in the same file that imports `ShieldCanvas` and still has the shimmer animation + text overlay. This avoids moving the shimmer animation logic to the shared component (the shimmer is specific to the home screen's full-size version).

The new `private fun ShieldLogo(modifier: Modifier = Modifier)` in `GameModeSelectionScreen.kt`:

```kotlin
@Composable
private fun ShieldLogo(modifier: Modifier = Modifier) {
    val cinzel = cinzelDecorative()
    val transition = rememberInfiniteTransition(label = "shield-shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-x"
    )

    Box(contentAlignment = Alignment.Center) {
        // Pass size here so callers can inject sharedElement() BEFORE the size constraint
        ShieldCanvas(modifier = modifier.size(220.dp, 250.dp), shimmerX = shimmerX)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(
                "Arena",
                fontFamily = cinzel,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 26.sp,
                letterSpacing = 3.sp
            )
            Text(
                "Tactics",
                fontFamily = cinzel,
                fontWeight = FontWeight.Normal,
                color = Accent,
                fontSize = 16.sp,
                letterSpacing = 5.sp
            )
        }
    }
}
```

4. The `GameModeSelectionScreen` composable now accepts `shieldModifier: Modifier = Modifier` and passes it to `ShieldLogo(modifier = shieldModifier)`.

Update the function signature:
```kotlin
@Composable
fun GameModeSelectionScreen(
    viewModel: GameModeSelectionViewModel,
    navController: NavHostController,
    shieldModifier: Modifier = Modifier
)
```

5. Replace `navigator.push(Screen.CompositionSelection(mode.id))` with `navController.navigate(Screen.CompositionSelection(mode.id))`.

6. Remove imports: `net.tautellini.arenatactics.navigation.Navigator` and `net.tautellini.arenatactics.presentation.screens.components.BackButton` (if present).

7. Add imports: `androidx.navigation.NavHostController`, `net.tautellini.arenatactics.presentation.screens.components.ShieldCanvas`.

- [ ] **Step 3: Verify compilation (Navigator still exists at this point)**

```bash
./gradlew :composeApp:compileKotlinJvm
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/components/ShieldLogo.kt \
        composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/GameModeSelectionScreen.kt
git commit -m "refactor: extract ShieldCanvas to shared component, add shieldModifier param to GameModeSelectionScreen"
```

---

## Task 5: Migrate NavigatorTest → ScreenNavigationTest

**Files:**
- Create: `composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/navigation/ScreenNavigationTest.kt`
- Delete: `composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/navigation/NavigatorTest.kt`

The tests that test `Navigator.push/pop` become irrelevant (NavController handles that). The tests that test `Screen.fromPath()` and `Screen.buildStack()` are valuable and must be kept — they cover the web URL bridge logic.

- [ ] **Step 1: Write the failing test (verify old test still works first)**

```bash
./gradlew :composeApp:jvmTest --tests "net.tautellini.arenatactics.navigation.NavigatorTest"
```

Expected: PASS (all 9 tests pass).

- [ ] **Step 2: Create `ScreenNavigationTest.kt` with the valuable tests**

Create `composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/navigation/ScreenNavigationTest.kt`:

```kotlin
package net.tautellini.arenatactics.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ScreenNavigationTest {

    @Test
    fun fromPathRoot() {
        assertEquals(Screen.GameModeSelection, Screen.fromPath("/"))
    }

    @Test
    fun fromPathCompositionSelection() {
        assertEquals(Screen.CompositionSelection("tbc"), Screen.fromPath("/modes/tbc"))
    }

    @Test
    fun fromPathGearView() {
        assertEquals(
            Screen.GearView("tbc", "mage_rogue"),
            Screen.fromPath("/modes/tbc/comp/mage_rogue/gear")
        )
    }

    @Test
    fun fromPathMatchupList() {
        assertEquals(
            Screen.MatchupList("tbc", "mage_rogue"),
            Screen.fromPath("/modes/tbc/comp/mage_rogue/matchups")
        )
    }

    @Test
    fun fromPathMatchupDetail() {
        assertEquals(
            Screen.MatchupDetail("tbc", "mage_rogue", "mage_rogue_vs_druid_warrior"),
            Screen.fromPath("/modes/tbc/comp/mage_rogue/matchups/mage_rogue_vs_druid_warrior")
        )
    }

    @Test
    fun fromPathStripsBasePrefixFlexibly() {
        assertEquals(
            Screen.CompositionSelection("tbc"),
            Screen.fromPath("/ArenaTactics/modes/tbc")
        )
    }

    @Test
    fun fromPathUnknownReturnsGameModeSelection() {
        assertEquals(Screen.GameModeSelection, Screen.fromPath("/unknown/route"))
    }

    @Test
    fun buildStackForMatchupDetailHasFullAncestors() {
        val detail = Screen.MatchupDetail("tbc", "mage_rogue", "some_matchup")
        val stack = Screen.buildStack(detail)
        assertEquals(4, stack.size)
        assertIs<Screen.GameModeSelection>(stack[0])
        assertEquals(Screen.CompositionSelection("tbc"), stack[1])
        assertEquals(Screen.MatchupList("tbc", "mage_rogue"), stack[2])
        assertEquals(detail, stack[3])
    }

    @Test
    fun buildStackForGearViewHasThreeEntries() {
        val stack = Screen.buildStack(Screen.GearView("tbc", "mage_rogue"))
        assertEquals(3, stack.size)
        assertIs<Screen.GameModeSelection>(stack[0])
        assertEquals(Screen.CompositionSelection("tbc"), stack[1])
        assertEquals(Screen.GearView("tbc", "mage_rogue"), stack[2])
    }
}
```

- [ ] **Step 3: Run new tests to verify they pass**

```bash
./gradlew :composeApp:jvmTest --tests "net.tautellini.arenatactics.navigation.ScreenNavigationTest"
```

Expected: PASS (9 tests).

- [ ] **Step 4: Delete `NavigatorTest.kt`**

```bash
rm composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/navigation/NavigatorTest.kt
```

- [ ] **Step 5: Run all tests to verify nothing broken**

```bash
./gradlew :composeApp:allTests
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/navigation/ScreenNavigationTest.kt
git rm composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/navigation/NavigatorTest.kt
git commit -m "test: replace NavigatorTest with ScreenNavigationTest (Screen.fromPath/buildStack coverage only)"
```

---

## Task 6: Create AppHeader component

**Files:**
- Create: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/components/AppHeader.kt`

`AppHeader` is the persistent top bar shown on every screen except `GameModeSelection`. It contains the small animated shield (shared element target) and breadcrumb navigation chips.

The breadcrumb list is built from `Screen.buildStack(currentScreen)` — deterministic, no internal NavController APIs needed. Non-last chips call `navController.popBackStack(destination, inclusive = false)`.

The current screen's dynamic label (for GearView/MatchupList/MatchupDetail — requires resolved composition name) is passed in as `currentLabel: String?`. Static labels are derived from the Screen type.

- [ ] **Step 1: Create `AppHeader.kt`**

```kotlin
package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.theme.Accent
import net.tautellini.arenatactics.presentation.theme.TextPrimary
import net.tautellini.arenatactics.presentation.theme.TextSecondary

/**
 * Persistent top bar shown on all screens except [Screen.GameModeSelection].
 *
 * @param currentScreen  the currently displayed screen (used to build breadcrumb list)
 * @param currentLabel   dynamic label for the current screen; null falls back to staticLabel()
 * @param shieldModifier pre-computed modifier for the shield canvas — caller injects the
 *                       sharedElement() modifier from App.kt where SharedTransitionScope is
 *                       accessible. Default is just the header size with no shared element.
 */
@Composable
fun AppHeader(
    currentScreen: Screen,
    navController: NavHostController,
    shieldModifier: Modifier = Modifier.size(28.dp, 32.dp),
    currentLabel: String? = null,
    modifier: Modifier = Modifier
) {
    val breadcrumbs = Screen.buildStack(currentScreen)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Small shield — shieldModifier injected by App.kt contains the sharedElement() call
        val shimmerTransition = rememberInfiniteTransition(label = "header-shield-shimmer")
        val shimmerX by shimmerTransition.animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer-x"
        )
        ShieldCanvas(
            modifier = shieldModifier,
            shimmerX = shimmerX
        )

        Spacer(Modifier.width(8.dp))

        // Breadcrumb chips
        breadcrumbs.forEachIndexed { index, screen ->
            val isLast = index == breadcrumbs.lastIndex
            val label = if (isLast && currentLabel != null) currentLabel else screen.staticLabel()

            if (index > 0) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }

            Text(
                text = label,
                color = if (isLast) TextPrimary else TextSecondary,
                fontSize = 13.sp,
                fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                modifier = if (!isLast) Modifier.clickable {
                    navController.popBackStack(screen, inclusive = false)
                } else Modifier
            )
        }
    }
}

private fun Screen.staticLabel(): String = when (this) {
    is Screen.GameModeSelection    -> "Select Mode"
    is Screen.CompositionSelection -> "Select Composition"
    is Screen.GearView             -> "Gear"       // overridden by currentLabel at call site
    is Screen.MatchupList          -> "Matchups"   // overridden by currentLabel at call site
    is Screen.MatchupDetail        -> "Matchup"    // overridden by currentLabel at call site
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :composeApp:compileKotlinJvm
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/components/AppHeader.kt
git commit -m "feat: add AppHeader component with shared shield and breadcrumb navigation"
```

---

## Task 7: Update CompositionCard for dynamic spec iteration

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/components/CompositionCard.kt`

`CompositionCard` currently hardcodes `richComposition.specs[0]` and `richComposition.classes[1]` — two fixed indices. This must be changed to dynamic iteration so:
1. It works for 3v3/5v5 compositions (future-proofing)
2. Each `SpecBadge` can receive a `sharedElement` modifier keyed by `specId`

Add a `specBadgeModifier: (specId: String) -> Modifier` parameter (defaults to `{ Modifier }`). Callers in `CompositionSelectionScreen` will inject the shared element modifier.

- [ ] **Step 1: Update `CompositionCard.kt`**

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
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    specBadgeModifier: (specId: String) -> Modifier = { Modifier }
) {
    val hasData = richComposition.composition.hasData
    Surface(
        color = CardColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .alpha(if (hasData) 1f else 0.35f)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            richComposition.specs.zip(richComposition.classes).forEach { (spec, wowClass) ->
                SpecBadge(
                    spec = spec,
                    wowClass = wowClass,
                    modifier = specBadgeModifier(spec.id)
                )
            }
        }
    }
}
```

- [ ] **Step 2: Verify compilation and existing tests pass**

```bash
./gradlew :composeApp:allTests
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/components/CompositionCard.kt
git commit -m "refactor: CompositionCard iterates specs dynamically; add specBadgeModifier hook"
```

---

## Task 8: The big migration — App.kt + all screens + delete Navigator/BackButton

This is an atomic task. All these files must compile together. Do not commit partial changes — commit only when everything compiles and the app runs.

**Files:**
- Modify: `App.kt`
- Modify: `presentation/screens/CompositionSelectionScreen.kt`
- Modify: `presentation/screens/GearScreen.kt`
- Modify: `presentation/screens/MatchupListScreen.kt`
- Modify: `presentation/screens/MatchupDetailScreen.kt`
- Delete: `navigation/Navigator.kt`
- Delete: `presentation/screens/components/BackButton.kt`

### Step 8.1 — Update `CompositionSelectionScreen.kt`

- [ ] **Step 1: Rewrite `CompositionSelectionScreen.kt`**

Remove the `BackButton` header row and the `Spacer(Modifier.height(24.dp))` after it. Replace `Navigator` with `NavHostController`. Pass `specBadgeModifier` using `sharedElement` keyed by specId. The screen now receives `SharedTransitionScope` and `AnimatedContentScope` from the call site via a `specBadgeModifier` lambda:

```kotlin
package net.tautellini.arenatactics.presentation.screens

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import net.tautellini.arenatactics.data.model.CompositionTier
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.CompositionSelectionState
import net.tautellini.arenatactics.presentation.CompositionSelectionViewModel
import net.tautellini.arenatactics.presentation.screens.components.CompositionCard
import net.tautellini.arenatactics.presentation.theme.*

private fun CompositionTier.label() = when (this) {
    CompositionTier.DOMINANT -> "Dominant"
    CompositionTier.STRONG   -> "Strong"
    CompositionTier.PLAYABLE -> "Playable"
    CompositionTier.OTHERS   -> "Others"
}

@Composable
fun SharedTransitionScope.CompositionSelectionScreen(
    gameModeId: String,
    viewModel: CompositionSelectionViewModel,
    navController: NavHostController,
    animatedVisibilityScope: AnimatedContentScope
) {
    val state by viewModel.state.collectAsState()
    var othersExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(Background).padding(24.dp)
    ) {
        when (val s = state) {
            is CompositionSelectionState.Loading ->
                CircularProgressIndicator(color = Accent)
            is CompositionSelectionState.Error ->
                Text(s.message, color = TextSecondary)
            is CompositionSelectionState.Success -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                            if (othersExpanded) {
                                item {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        comps.forEach { rich ->
                                            CompositionCard(
                                                richComposition = rich,
                                                onClick = if (rich.composition.hasData) {
                                                    { navController.navigate(Screen.GearView(gameModeId, rich.composition.id)) }
                                                } else null,
                                                specBadgeModifier = { specId ->
                                                    Modifier.sharedElement(
                                                        rememberSharedContentState("spec_badge_$specId"),
                                                        animatedVisibilityScope
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            item { TierHeader(label = tier.label()) }
                            item {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    comps.forEach { rich ->
                                        CompositionCard(
                                            richComposition = rich,
                                            onClick = if (rich.composition.hasData) {
                                                { navController.navigate(Screen.GearView(gameModeId, rich.composition.id)) }
                                            } else null,
                                            specBadgeModifier = { specId ->
                                                Modifier.sharedElement(
                                                    rememberSharedContentState("spec_badge_$specId"),
                                                    animatedVisibilityScope
                                                )
                                            }
                                        )
                                    }
                                }
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
        Text(text = label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        if (expandable) {
            Icon(
                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = TextSecondary
            )
        }
    }
}
```

### Step 8.2 — Update `GearScreen.kt`

- [ ] **Step 2: Update `GearScreen.kt`**

Changes:
- Remove `Navigator` import + `BackButton` import + `BackButton` usage in the header row
- Remove the header `Row` containing `BackButton` + spec badges — the AppHeader now handles this
- Add `navController: NavHostController` parameter to `CompositionHubScreen`
- Add `initialTab: CompositionTab = CompositionTab.GEAR` parameter to `CompositionHubScreen`
- Initialize `var selectedTab` from `initialTab` using `remember { mutableStateOf(initialTab) }`
- Pass `navController` to `MatchupListScreen`
- Thread `SharedTransitionScope` and `AnimatedContentScope` through to apply `sharedElement` to spec badge icons in the hub header

Updated `CompositionHubScreen` signature:
```kotlin
@Composable
fun SharedTransitionScope.CompositionHubScreen(
    gameModeId: String,
    compositionId: String,
    gearViewModel: GearViewModel,
    matchupListViewModel: MatchupListViewModel,
    navController: NavHostController,
    animatedVisibilityScope: AnimatedContentScope,
    initialTab: CompositionTab = CompositionTab.GEAR
)
```

The spec badge header row (previously after `BackButton`) moves to a standalone `Row` at the top (no back button). Each `SpecBadge` gets a `sharedElement` modifier:

```kotlin
// In the Column after AppHeader (AppHeader is rendered outside this composable in App.kt)
Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    richComposition?.specs?.zip(richComposition.classes)?.forEach { (spec, wowClass) ->
        SpecBadge(
            spec = spec,
            wowClass = wowClass,
            modifier = Modifier.sharedElement(
                rememberSharedContentState("spec_badge_${spec.id}"),
                animatedVisibilityScope
            )
        )
    }
}
```

The `MatchupListScreen` call inside `CompositionHubScreen` must pass `navController` instead of `navigator`.

### Step 8.3 — Update `MatchupListScreen.kt`

- [ ] **Step 3: Update `MatchupListScreen.kt`**

Replace `navigator: Navigator` with `navController: NavHostController`. Replace:
```kotlin
navigator.push(Screen.MatchupDetail(gameModeId, compositionId, matchup.id))
```
with:
```kotlin
navController.navigate(Screen.MatchupDetail(gameModeId, compositionId, matchup.id))
```

Remove `Navigator` and `BackButton` imports. Add `NavHostController` import.

### Step 8.4 — Update `MatchupDetailScreen.kt`

- [ ] **Step 4: Update `MatchupDetailScreen.kt`**

Remove `BackButton` usage and its surrounding `Row`. Remove the `navigator: Navigator` parameter entirely (no navigation actions in `MatchupDetailScreen` — back is handled by the system back button via `NavController`). Remove `Navigator` and `BackButton` imports.

The header row in `MatchupDetailState.Success` becomes just the "vs" text + spec badges, no back button:
```kotlin
Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Text("vs", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
    matchup.enemySpecIds.forEach { specId ->
        val spec  = s.specMap[specId]  ?: return@forEach
        val clazz = s.classMap[spec.classId] ?: return@forEach
        SpecBadge(spec, clazz, modifier = Modifier.padding(end = 6.dp))
    }
}
```

### Step 8.5 — Rewrite `App.kt`

- [ ] **Step 5: Rewrite `App.kt`**

```kotlin
package net.tautellini.arenatactics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import net.tautellini.arenatactics.data.repository.*
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.navigation.toScreen
import net.tautellini.arenatactics.presentation.*
import net.tautellini.arenatactics.presentation.screens.*
import net.tautellini.arenatactics.presentation.screens.components.AppHeader
import net.tautellini.arenatactics.presentation.theme.ArenaTacticsTheme
import net.tautellini.arenatactics.presentation.theme.Background

@Composable
fun App() {
    val gameModeRepository  = remember { GameModeRepository() }
    val specRepository      = remember { SpecRepository() }
    val compositionRepository = remember { CompositionRepository(specRepository) }
    val gearRepository      = remember { GearRepository(compositionRepository) }
    val matchupRepository   = remember { MatchupRepository() }

    val navController: NavHostController = rememberNavController()

    // Restore deep-link back stack on first launch
    val initialScreen = remember { Screen.fromPath(getInitialPath()) }
    val initialStack  = remember { Screen.buildStack(initialScreen) }
    LaunchedEffect(Unit) {
        initialStack.drop(1).forEach { screen ->
            navController.navigate(screen) { launchSingleTop = true }
        }
    }

    // Sync NavController → browser history
    var isRestoringFromBrowser by remember { mutableStateOf(false) }
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow
            .drop(initialStack.size - 1)
            .collect { entry ->
                if (!isRestoringFromBrowser) {
                    pushNavigationState(entry.toScreen().path)
                }
                isRestoringFromBrowser = false
            }
    }

    // Sync browser back/forward → NavController
    DisposableEffect(navController) {
        registerPopCallback {
            isRestoringFromBrowser = true
            val screen = Screen.fromPath(getCurrentPath())
            val stack  = Screen.buildStack(screen)
            navController.navigate(Screen.GameModeSelection) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
            stack.drop(1).forEach { s ->
                navController.navigate(s) { launchSingleTop = true }
            }
        }
        onDispose { registerPopCallback {} }
    }

    val currentEntry by navController.currentBackStackEntryAsState()
    val currentScreen = remember(currentEntry) {
        currentEntry?.toScreen() ?: Screen.GameModeSelection
    }
    val isHomeScreen = currentScreen is Screen.GameModeSelection

    ArenaTacticsTheme {
        SharedTransitionLayout {
            // Capture SharedTransitionScope before entering nested lambdas.
            // AnimatedVisibility and composable{} lambdas replace 'this', so we save it.
            val sharedScope = this

            Column(modifier = Modifier.fillMaxSize().background(Background)) {
                // AppHeader: visible on all screens except home
                AnimatedVisibility(
                    visible = !isHomeScreen,
                    enter = fadeIn(tween(300)),
                    exit  = fadeOut(tween(200))
                ) {
                    // 'this' here is AnimatedVisibilityScope.
                    // Compute shieldModifier using captured sharedScope so SharedTransitionScope
                    // is available while animatedVisibilityScope (this) provides the scope.
                    val shieldMod = with(sharedScope) {
                        Modifier
                            .size(28.dp, 32.dp)
                            .sharedElement(
                                rememberSharedContentState("shield"),
                                this@AnimatedVisibility
                            )
                    }
                    AppHeader(
                        currentScreen = currentScreen,
                        navController = navController,
                        shieldModifier = shieldMod
                    )
                }

                NavHost(
                    navController = navController,
                    startDestination = Screen.GameModeSelection,
                    modifier = Modifier.weight(1f),
                    enterTransition    = { fadeIn(tween(300))  + scaleIn(tween(300),  initialScale = 0.95f) },
                    exitTransition     = { fadeOut(tween(200)) + scaleOut(tween(200), targetScale  = 1.05f) },
                    popEnterTransition = { fadeIn(tween(300))  + scaleIn(tween(300),  initialScale = 1.05f) },
                    popExitTransition  = { fadeOut(tween(200)) + scaleOut(tween(200), targetScale  = 0.95f) }
                ) {
                    composable<Screen.GameModeSelection> {
                        val vm = viewModel { GameModeSelectionViewModel(gameModeRepository) }
                        GameModeSelectionScreen(
                            viewModel = vm,
                            navController = navController,
                            shieldModifier = Modifier.sharedElement(
                                rememberSharedContentState("shield"),
                                this
                            )
                        )
                    }

                    composable<Screen.CompositionSelection> {
                        // Use toRoute<T>() directly — the type-safe API for extracting route args
                        val route = it.toRoute<Screen.CompositionSelection>()
                        val vm = viewModel(key = route.gameModeId) {
                            CompositionSelectionViewModel(
                                route.gameModeId, gameModeRepository, compositionRepository
                            )
                        }
                        with(sharedScope) {
                            CompositionSelectionScreen(
                                gameModeId = route.gameModeId,
                                viewModel = vm,
                                navController = navController,
                                animatedVisibilityScope = this@composable
                            )
                        }
                    }

                    composable<Screen.GearView> {
                        val route = it.toRoute<Screen.GearView>()
                        val gearVm = viewModel(key = "gear_${route.compositionId}") {
                            GearViewModel(
                                route.gameModeId, route.compositionId,
                                gameModeRepository, compositionRepository, gearRepository
                            )
                        }
                        val matchupVm = viewModel(key = "matchups_${route.compositionId}") {
                            MatchupListViewModel(
                                route.gameModeId, route.compositionId,
                                gameModeRepository, compositionRepository, matchupRepository
                            )
                        }
                        with(sharedScope) {
                            CompositionHubScreen(
                                gameModeId = route.gameModeId,
                                compositionId = route.compositionId,
                                gearViewModel = gearVm,
                                matchupListViewModel = matchupVm,
                                navController = navController,
                                animatedVisibilityScope = this@composable,
                                initialTab = CompositionTab.GEAR
                            )
                        }
                    }

                    composable<Screen.MatchupList> {
                        val route = it.toRoute<Screen.MatchupList>()
                        val gearVm = viewModel(key = "gear_${route.compositionId}") {
                            GearViewModel(
                                route.gameModeId, route.compositionId,
                                gameModeRepository, compositionRepository, gearRepository
                            )
                        }
                        val matchupVm = viewModel(key = "matchups_${route.compositionId}") {
                            MatchupListViewModel(
                                route.gameModeId, route.compositionId,
                                gameModeRepository, compositionRepository, matchupRepository
                            )
                        }
                        with(sharedScope) {
                            CompositionHubScreen(
                                gameModeId = route.gameModeId,
                                compositionId = route.compositionId,
                                gearViewModel = gearVm,
                                matchupListViewModel = matchupVm,
                                navController = navController,
                                animatedVisibilityScope = this@composable,
                                initialTab = CompositionTab.MATCHUPS
                            )
                        }
                    }

                    composable<Screen.MatchupDetail> {
                        val route = it.toRoute<Screen.MatchupDetail>()
                        val vm = viewModel(key = route.matchupId) {
                            MatchupDetailViewModel(
                                route.gameModeId, route.compositionId, route.matchupId,
                                gameModeRepository, compositionRepository, matchupRepository
                            )
                        }
                        // navController not passed — MatchupDetailScreen has no outbound navigation
                        MatchupDetailScreen(viewModel = vm)
                    }
                }
            }
        }
    }
}
```

**Note on AppHeader label for GearView/MatchupList:** The `AppHeader` in `App.kt` currently passes `currentLabel = null` for `GearView`/`MatchupList` — this falls back to static "Gear"/"Matchups" labels. If a dynamic composition name is desired in the breadcrumb (e.g., "Frost Mage / Sub Rogue"), the `GearViewModel` state must be read at the `App.kt` level and passed as `currentLabel`. This can be implemented as a follow-up if desired.

### Step 8.6 — Delete Navigator.kt and BackButton.kt

- [ ] **Step 6: Delete the files**

```bash
rm composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/navigation/Navigator.kt
rm composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/components/BackButton.kt
```

### Step 8.7 — Verify everything compiles and tests pass

- [ ] **Step 7: Full compile + test**

```bash
./gradlew :composeApp:allTests
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 8: Smoke test the app**

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

Verify:
- Home screen shows centered shield + mode tiles
- Navigating to a composition shows AppHeader with "Select Mode > Select Composition" breadcrumbs and small animated shield
- Navigating to gear shows breadcrumbs, spec badge icons animate into position from the card
- Browser Back button navigates back through the stack
- Browser Forward button navigates forward correctly
- Fade+scale transitions are visible between screens

- [ ] **Step 9: Commit**

```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/App.kt
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/CompositionSelectionScreen.kt
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/GearScreen.kt
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/MatchupListScreen.kt
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/MatchupDetailScreen.kt
git rm composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/navigation/Navigator.kt
git rm composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/components/BackButton.kt
git commit -m "feat: migrate to Compose Navigation with AnimatedNavHost, shared element transitions, and AppHeader breadcrumbs"
```
