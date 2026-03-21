# Consolidated Home Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Collapse AddonHub + TacticsGameModeSelection into the home screen as inline cascading selection steps, removing 2 redundant navigation screens.

**Architecture:** `HomeViewModel` replaces `AddonSelectionViewModel`, managing both addon list (loaded in `init`) and game modes (loaded lazily on addon tap). Local composable state (`HomeSelection`) drives which rows are visible. Three `AnimatedVisibility` rows cascade: addon → section → bracket.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, AndroidX ViewModel, Compose Navigation, Compose `AnimatedVisibility`, `FlowRow`

**Spec:** `docs/superpowers/specs/2026-03-21-consolidated-home-screen-design.md`

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `navigation/Screen.kt` | Modify (×2) | Task 1: fix `fromPath`/`buildStack`/`toScreen`; Task 5: delete sealed entries |
| `commonTest/.../ScreenNavigationTest.kt` | Modify | Task 1: update tests to match new nav logic |
| `presentation/HomeViewModel.kt` | Create | Task 2: `HomeViewModel`, `HomeState`, `GameModeRowState` |
| `presentation/screens/AddonSelectionScreen.kt` | Full rewrite | Task 3: cascading home screen |
| `App.kt` | Modify (×2) | Task 3: wire `HomeViewModel`; Task 4: remove dead destinations |
| `presentation/screens/AddonHubScreen.kt` | Delete | Task 4 |
| `presentation/screens/TacticsGameModeSelectionScreen.kt` | Delete | Task 4 |
| `presentation/AddonSelectionViewModel.kt` | Delete | Task 4 |
| `presentation/AddonHubViewModel.kt` | Delete | Task 4 |
| `presentation/GameModeSelectionViewModel.kt` | Delete | Task 4 |
| `presentation/screens/components/AppHeader.kt` | Modify | Task 5: update `breadcrumbLabel()` |

---

## Task 1: Update Screen.kt navigation logic + ScreenNavigationTest

**Context:** `Screen.AddonHub` and `Screen.GameModeSelection` sealed entries will be deleted in Task 5. This task updates the *logic* (`fromPath`, `buildStack`, `toScreen`) first, while the entries still exist. Tests verify the new logic before we delete anything.

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/navigation/Screen.kt`
- Modify: `composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/navigation/ScreenNavigationTest.kt`

- [ ] **Step 1: Update `fromPath` — two line changes**

In `Screen.kt`, find the `fromPath` companion function. Make two edits:

```kotlin
// Line 34 — BEFORE:
null      -> AddonHub(addonId)
// AFTER:
null      -> AddonSelection

// Line 36 — BEFORE (inside "tactics" branch):
val modeId = segs.getOrNull(2) ?: return GameModeSelection(addonId)
// AFTER:
val modeId = segs.getOrNull(2) ?: return AddonSelection
```

- [ ] **Step 2: Update `buildStack` — simplify 5 branches**

Replace the body of `buildStack` with:

```kotlin
fun buildStack(screen: Screen): List<Screen> = when (screen) {
    is AddonSelection       -> listOf(screen)
    is AddonHub             -> listOf(AddonSelection, screen)
    is GameModeSelection    -> listOf(AddonSelection, screen)
    is CompositionSelection -> listOf(AddonSelection, screen)
    is MatchupList          -> listOf(AddonSelection, CompositionSelection(screen.addonId, screen.gameModeId), screen)
    is MatchupDetail        -> listOf(AddonSelection, CompositionSelection(screen.addonId, screen.gameModeId), MatchupList(screen.addonId, screen.gameModeId, screen.compositionId), screen)
    is ClassGuideList       -> listOf(AddonSelection, screen)
    is SpecGuide            -> listOf(AddonSelection, ClassGuideList(screen.addonId), screen)
}
```

> Note: `AddonHub` and `GameModeSelection` branches are kept (entries still exist) but their chains are now minimal — they'll be removed entirely in Task 5.

- [ ] **Step 3: Run tests to confirm current state (expected: FAIL on fromPath + buildStack tests)**

```bash
./gradlew :composeApp:jvmTest --tests "net.tautellini.arenatactics.navigation.ScreenNavigationTest"
```

Expected: Tests `fromPathAddonHub`, `fromPathGameModeSelection`, `buildStackGameModeSelection`, `buildStackCompositionSelection`, `buildStackMatchupList`, `buildStackMatchupDetail`, `buildStackClassGuideList`, `buildStackSpecGuide`, `pathRoundTrip` will fail. (`buildStackAddonHub` will still pass — its chain is now `[AddonSelection, screen]` size 2, which the old test already asserts.) This confirms the tests need updating.

- [ ] **Step 4: Update `ScreenNavigationTest.kt`**

Replace the full test file with:

```kotlin
package net.tautellini.arenatactics.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ScreenNavigationTest {

    // ─── fromPath ────────────────────────────────────────────────────────────

    @Test fun fromPathRoot() =
        assertEquals(Screen.AddonSelection, Screen.fromPath("/"))

    @Test fun fromPathAddonIdFallsBackToHome() =
        assertEquals(Screen.AddonSelection, Screen.fromPath("/tbc_anniversary"))

    @Test fun fromPathTacticsMissingModeFallsBackToHome() =
        assertEquals(Screen.AddonSelection, Screen.fromPath("/tbc_anniversary/tactics"))

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

    @Test fun buildStackCompositionSelection() {
        val screen = Screen.CompositionSelection("tbc_anniversary", "tbc_anniversary_2v2")
        val stack = Screen.buildStack(screen)
        assertEquals(2, stack.size)
        assertIs<Screen.AddonSelection>(stack[0])
        assertEquals(screen, stack[1])
    }

    @Test fun buildStackMatchupList() {
        val screen = Screen.MatchupList("tbc_anniversary", "tbc_anniversary_2v2", "mage_rogue")
        val stack = Screen.buildStack(screen)
        assertEquals(3, stack.size)
        assertIs<Screen.AddonSelection>(stack[0])
        assertEquals(Screen.CompositionSelection("tbc_anniversary", "tbc_anniversary_2v2"), stack[1])
        assertEquals(screen, stack[2])
    }

    @Test fun buildStackMatchupDetail() {
        val screen = Screen.MatchupDetail("tbc_anniversary", "tbc_anniversary_2v2", "mage_rogue", "m1")
        val stack = Screen.buildStack(screen)
        assertEquals(4, stack.size)
        assertIs<Screen.AddonSelection>(stack[0])
        assertEquals(Screen.CompositionSelection("tbc_anniversary", "tbc_anniversary_2v2"), stack[1])
        assertEquals(Screen.MatchupList("tbc_anniversary", "tbc_anniversary_2v2", "mage_rogue"), stack[2])
        assertEquals(screen, stack[3])
    }

    @Test fun buildStackClassGuideList() {
        val screen = Screen.ClassGuideList("tbc_anniversary")
        val stack = Screen.buildStack(screen)
        assertEquals(2, stack.size)
        assertIs<Screen.AddonSelection>(stack[0])
        assertEquals(screen, stack[1])
    }

    @Test fun buildStackSpecGuide() {
        val screen = Screen.SpecGuide("tbc_anniversary", "druid", "druid_restoration")
        val stack = Screen.buildStack(screen)
        assertEquals(3, stack.size)
        assertIs<Screen.AddonSelection>(stack[0])
        assertEquals(Screen.ClassGuideList("tbc_anniversary"), stack[1])
        assertEquals(screen, stack[2])
    }

    // ─── path round-trip ─────────────────────────────────────────────────────

    @Test fun pathRoundTrip() {
        val screens = listOf(
            Screen.AddonSelection,
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

- [ ] **Step 5: Run tests — expect all to pass**

```bash
./gradlew :composeApp:jvmTest --tests "net.tautellini.arenatactics.navigation.ScreenNavigationTest"
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/navigation/Screen.kt
git add composeApp/src/commonTest/kotlin/net/tautellini/arenatactics/navigation/ScreenNavigationTest.kt
git commit -m "refactor: simplify Screen navigation — remove AddonHub/GameMode from buildStack and fromPath"
```

---

## Task 2: Create HomeViewModel

**Context:** New ViewModel that merges `AddonSelectionViewModel` (load all addons in init) and `GameModeSelectionViewModel` (load game modes lazily on demand). Both state types (`HomeState`, `GameModeRowState`) live in this file — they are ViewModel state types, not domain models.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/HomeViewModel.kt`

- [ ] **Step 1: Create `HomeViewModel.kt`**

```kotlin
package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.tautellini.arenatactics.data.model.Addon
import net.tautellini.arenatactics.data.model.GameMode
import net.tautellini.arenatactics.data.repository.AddonRepository
import net.tautellini.arenatactics.data.repository.GameModeRepository

sealed class HomeState {
    data object Loading : HomeState()
    data class Error(val message: String) : HomeState()
    data class Success(
        val addons: List<Addon>,
        val gameModeRow: GameModeRowState = GameModeRowState.Idle
    ) : HomeState()
}

sealed class GameModeRowState {
    data object Idle : GameModeRowState()
    data object Loading : GameModeRowState()
    data class Ready(val modes: List<GameMode>) : GameModeRowState()
    data class Error(val message: String) : GameModeRowState()
}

class HomeViewModel(
    private val addonRepository: AddonRepository,
    private val gameModeRepository: GameModeRepository
) : ViewModel() {

    private val _state = MutableStateFlow<HomeState>(HomeState.Loading)
    val state: StateFlow<HomeState> = _state.asStateFlow()

    // Tracks which addonId was last successfully loaded to avoid redundant reloads
    private var lastLoadedAddonId: String? = null

    init {
        viewModelScope.launch {
            _state.value = try {
                HomeState.Success(addonRepository.getAll())
            } catch (t: Throwable) {
                HomeState.Error(t.message ?: "Failed to load games")
            }
        }
    }

    /**
     * Load game modes for [addonId]. Guards against redundant reloads:
     * if the current state is already Ready for this addonId, does nothing.
     * For a different addonId, always reloads.
     */
    fun loadGameModes(addonId: String) {
        val current = _state.value as? HomeState.Success ?: return
        if (current.gameModeRow is GameModeRowState.Ready && lastLoadedAddonId == addonId) return
        _state.update { (it as HomeState.Success).copy(gameModeRow = GameModeRowState.Loading) }
        viewModelScope.launch {
            val result = try {
                GameModeRowState.Ready(gameModeRepository.getByAddon(addonId))
            } catch (t: Throwable) {
                GameModeRowState.Error(t.message ?: "Failed to load brackets")
            }
            if (result is GameModeRowState.Ready) lastLoadedAddonId = addonId
            _state.update { s ->
                if (s is HomeState.Success) s.copy(gameModeRow = result) else s
            }
        }
    }

    /** Deselects addon — resets game mode row to Idle and clears last loaded addon. */
    fun resetGameModes() {
        lastLoadedAddonId = null
        _state.update { s ->
            if (s is HomeState.Success) s.copy(gameModeRow = GameModeRowState.Idle) else s
        }
    }
}
```

- [ ] **Step 2: Verify the project compiles (HomeViewModel.kt is additive — nothing else changed)**

```bash
./gradlew :composeApp:compileKotlinJvm
```

Expected: BUILD SUCCESSFUL — no compile errors.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/HomeViewModel.kt
git commit -m "feat: add HomeViewModel with lazy game-mode loading and deselect guard"
```

---

## Task 3: Rewrite AddonSelectionScreen + wire HomeViewModel in App.kt

**Context:** Full rewrite of the home screen. The screen now has three cascading rows driven by local `HomeSelection` state. Also update the single `composable<Screen.AddonSelection>` block in `App.kt` to use `HomeViewModel` — do NOT touch any other NavHost block yet.

**Files:**
- Modify (full rewrite): `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/AddonSelectionScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/App.kt`

- [ ] **Step 1: Rewrite `AddonSelectionScreen.kt`**

```kotlin
package net.tautellini.arenatactics.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import net.tautellini.arenatactics.data.model.Addon
import net.tautellini.arenatactics.data.model.GameMode
import net.tautellini.arenatactics.data.model.WowheadIcons
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.GameModeRowState
import net.tautellini.arenatactics.presentation.HomeState
import net.tautellini.arenatactics.presentation.HomeViewModel
import net.tautellini.arenatactics.presentation.screens.components.ShieldCanvas
import net.tautellini.arenatactics.presentation.theme.*

private data class HomeSelection(
    val addon: Addon? = null,
    val section: Section? = null
)

private enum class Section { TACTICS, CLASS_GUIDES }

@Composable
fun AddonSelectionScreen(
    viewModel: HomeViewModel,
    onNavigate: (Screen) -> Unit,
    shieldModifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var selection by remember { mutableStateOf(HomeSelection()) }

    Box(modifier = Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(40.dp),
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            ShieldLogoBlock(modifier = shieldModifier)

            when (val s = state) {
                is HomeState.Loading -> CircularProgressIndicator(color = Primary)
                is HomeState.Error   -> Text(s.message, color = TextSecondary)
                is HomeState.Success -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // ── Addon row ──────────────────────────────────────
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
                                val isSelected = selection.addon?.id == addon.id
                                val isAnySelected = selection.addon != null
                                AddonTile(
                                    addon = addon,
                                    isSelected = isSelected,
                                    alpha = when {
                                        !addon.hasData -> 0.35f
                                        isAnySelected && !isSelected -> 0.6f
                                        else -> 1f
                                    },
                                    onClick = if (addon.hasData) ({
                                        if (isSelected) {
                                            selection = HomeSelection()
                                            viewModel.resetGameModes()
                                        } else {
                                            if (selection.addon != null) viewModel.resetGameModes()
                                            selection = HomeSelection(addon = addon)
                                            viewModel.loadGameModes(addon.id)
                                        }
                                    }) else null
                                )
                            }
                        }

                        // ── Section row ────────────────────────────────────
                        AnimatedVisibility(
                            visible = selection.addon != null,
                            enter = fadeIn() + slideInVertically { it / 4 },
                            exit  = fadeOut() + slideOutVertically()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    "What are you looking for?",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    letterSpacing = 2.sp
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    SectionTile(
                                        icon = Icons.Rounded.AutoAwesome,
                                        title = "Tactics",
                                        isSelected = selection.section == Section.TACTICS,
                                        gameModeRow = s.gameModeRow,
                                        onClick = { selection = selection.copy(section = Section.TACTICS) }
                                    )
                                    SectionTile(
                                        icon = Icons.Rounded.MenuBook,
                                        title = "Class Guides",
                                        isSelected = false,
                                        gameModeRow = null,
                                        onClick = {
                                            selection.addon?.let { onNavigate(Screen.ClassGuideList(it.id)) }
                                        }
                                    )
                                }
                            }
                        }

                        // ── Bracket row ────────────────────────────────────
                        AnimatedVisibility(
                            visible = selection.section == Section.TACTICS,
                            enter = fadeIn() + slideInVertically { it / 4 },
                            exit  = fadeOut() + slideOutVertically()
                        ) {
                            val gmRow = s.gameModeRow
                            if (gmRow is GameModeRowState.Ready) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        "Select your bracket",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        letterSpacing = 2.sp
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                                        verticalArrangement = Arrangement.spacedBy(24.dp)
                                    ) {
                                        gmRow.modes.forEach { mode ->
                                            GameModeTile(mode) {
                                                onNavigate(Screen.CompositionSelection(mode.addonId, mode.id))
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

        Text(
            text = "Made with love for Kizaru",
            color = TextSecondary.copy(alpha = 0.5f),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
        )
    }
}

// ─── Private composables ──────────────────────────────────────────────────────

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
private fun AddonTile(
    addon: Addon,
    isSelected: Boolean,
    alpha: Float,
    onClick: (() -> Unit)?
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .alpha(alpha)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(shape)
                .then(if (isSelected) Modifier.border(2.dp, Primary, shape) else Modifier)
        ) {
            AsyncImage(
                model = WowheadIcons.large(addon.iconName),
                contentDescription = addon.name,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            addon.name,
            color = if (isSelected) TextPrimary else TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
        Text(addon.description, color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SectionTile(
    icon: ImageVector,
    title: String,
    isSelected: Boolean,
    gameModeRow: GameModeRowState?,  // null = Class Guides (never disabled)
    onClick: () -> Unit
) {
    val isLoading = gameModeRow is GameModeRowState.Loading
    val isError   = gameModeRow is GameModeRowState.Error
    val allUnavailable = gameModeRow is GameModeRowState.Ready &&
            gameModeRow.modes.none { it.hasData }
    val isDisabled = isLoading || isError || allUnavailable

    val shape = RoundedCornerShape(16.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .widthIn(min = 120.dp, max = 200.dp)
            .clip(shape)
            .background(CardColor)
            .border(0.5.dp, if (isSelected) Primary else DividerColor, shape)
            .alpha(if (isDisabled && !isLoading && !isError) 0.35f else 1f)
            .then(if (!isDisabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                isLoading -> CircularProgressIndicator(
                    color = Primary,
                    modifier = Modifier.size(40.dp)
                )
                isError   -> Text(
                    "Failed to load",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
                else      -> androidx.compose.material3.Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isSelected) Primary else TextSecondary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Text(
                title,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
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
            contentDescription = mode.name,
            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp))
        )
        Text(mode.description, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(mode.name, color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}
```

- [ ] **Step 2: Update the `composable<Screen.AddonSelection>` block in `App.kt`**

Replace the import line:
```kotlin
// REMOVE:
import net.tautellini.arenatactics.presentation.AddonSelectionViewModel
// ADD:
import net.tautellini.arenatactics.presentation.HomeViewModel
```

Replace the composable block (lines 159–168):
```kotlin
composable<Screen.AddonSelection> {
    val vm = viewModel { HomeViewModel(addonRepository, gameModeRepository) }
    val shieldMod = with(sharedScope) {
        Modifier.sharedElement(
            sharedContentState = rememberSharedContentState(key = "shield"),
            animatedVisibilityScope = this@composable
        )
    }
    AddonSelectionScreen(viewModel = vm, onNavigate = { navController.navigate(it) }, shieldModifier = shieldMod)
}
```

- [ ] **Step 3: Verify compile**

```bash
./gradlew :composeApp:compileKotlinJvm
```

Expected: BUILD SUCCESSFUL. (App.kt still has AddonHub and GameModeSelection NavHost blocks — those reference old imports that still exist — so no errors yet.)

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/AddonSelectionScreen.kt
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/App.kt
git commit -m "feat: rewrite home screen as cascading launcher with inline selection rows"
```

---

## Task 4: Delete dead files + remove dead NavHost blocks from App.kt

**Context:** Now that `AddonSelectionScreen` no longer depends on `AddonSelectionViewModel`, and `HomeViewModel` is wired in `App.kt`, we can delete all the dead ViewModels and Screens, and remove the `composable<Screen.AddonHub>` and `composable<Screen.GameModeSelection>` blocks from the NavHost.

**Files:**
- Delete: `presentation/screens/AddonHubScreen.kt`
- Delete: `presentation/screens/TacticsGameModeSelectionScreen.kt`
- Delete: `presentation/AddonSelectionViewModel.kt`
- Delete: `presentation/AddonHubViewModel.kt`
- Delete: `presentation/GameModeSelectionViewModel.kt`
- Modify: `App.kt` — remove dead NavHost blocks + dead imports

- [ ] **Step 1: Delete the 5 dead files**

```bash
rm composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/AddonHubScreen.kt
rm composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/TacticsGameModeSelectionScreen.kt
rm composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/AddonSelectionViewModel.kt
rm composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/AddonHubViewModel.kt
rm composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/GameModeSelectionViewModel.kt
```

- [ ] **Step 2: Remove dead NavHost blocks from `App.kt`**

Remove these two `composable` blocks from the `NavHost` in `App.kt`:

```kotlin
// REMOVE the entire AddonHub composable block:
composable<Screen.AddonHub> { entry ->
    val screen = entry.toRoute<Screen.AddonHub>()
    val vm = viewModel(key = screen.addonId) { AddonHubViewModel(screen.addonId, addonRepository) }
    AddonHubScreen(viewModel = vm, onNavigate = { navController.navigate(it) })
}

// REMOVE the entire GameModeSelection composable block:
composable<Screen.GameModeSelection> { entry ->
    val screen = entry.toRoute<Screen.GameModeSelection>()
    val vm = viewModel(key = screen.addonId) { GameModeSelectionViewModel(screen.addonId, gameModeRepository) }
    TacticsGameModeSelectionScreen(addonId = screen.addonId, viewModel = vm, onNavigate = { navController.navigate(it) })
}
```

- [ ] **Step 3: Remove dead imports from `App.kt`**

Remove these import lines (no longer referenced):
```kotlin
import net.tautellini.arenatactics.presentation.AddonHubViewModel
import net.tautellini.arenatactics.presentation.GameModeSelectionViewModel
import net.tautellini.arenatactics.presentation.screens.AddonHubScreen
import net.tautellini.arenatactics.presentation.screens.TacticsGameModeSelectionScreen
```

- [ ] **Step 4: Verify compile**

```bash
./gradlew :composeApp:compileKotlinJvm
```

Expected: BUILD SUCCESSFUL. (Screen.kt still has `Screen.AddonHub` and `Screen.GameModeSelection` entries — that's fine for now.)

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: delete AddonHub and GameModeSelection screens and ViewModels"
```

---

## Task 5: Remove sealed entries from Screen.kt + update AppHeader.kt breadcrumbs

**Context:** Final cleanup. Remove `Screen.AddonHub` and `Screen.GameModeSelection` from the sealed class (nothing in the codebase references them anymore), remove their dead branches from `buildStack` and `toScreen()`, and update `breadcrumbLabel()` in `AppHeader.kt`.

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/navigation/Screen.kt`
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/components/AppHeader.kt`

- [ ] **Step 1: Delete `Screen.AddonHub` and `Screen.GameModeSelection` from the sealed class**

In `Screen.kt`, remove lines 10–11:
```kotlin
// REMOVE:
@Serializable data class AddonHub(val addonId: String) : Screen()
@Serializable data class GameModeSelection(val addonId: String) : Screen()
```

- [ ] **Step 2: Remove their `buildStack` branches**

In `buildStack`, remove:
```kotlin
// REMOVE:
is AddonHub          -> listOf(AddonSelection, screen)
is GameModeSelection -> listOf(AddonSelection, screen)
```

- [ ] **Step 3: Remove their `toScreen()` branches**

In `toScreen()`, delete these two blocks:
```kotlin
// REMOVE (lines ~85-86):
"GameModeSelection" in route -> toRoute<Screen.GameModeSelection>()
    .let { Screen.GameModeSelection(it.addonId) }

// REMOVE (lines ~91-92):
"AddonHub" in route -> toRoute<Screen.AddonHub>()
    .let { Screen.AddonHub(it.addonId) }
```

- [ ] **Step 4: Update `breadcrumbLabel()` in `AppHeader.kt`**

Find the `breadcrumbLabel()` extension function at the bottom of `AppHeader.kt`. Make two changes:

**Remove** the `AddonHub` and `GameModeSelection` cases:
```kotlin
// REMOVE:
is Screen.AddonHub          -> addonId.formatId()
is Screen.GameModeSelection -> "Tactics"
```

**Update** the `CompositionSelection` case (currently `gameModeId.formatId()`):
```kotlin
// BEFORE:
is Screen.CompositionSelection -> gameModeId.formatId()
// AFTER:
is Screen.CompositionSelection -> "${gameModeId.formatId()} Comps"
```

- [ ] **Step 5: Verify compile + run all tests**

```bash
./gradlew :composeApp:compileKotlinJvm
./gradlew :composeApp:allTests
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/navigation/Screen.kt
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/components/AppHeader.kt
git commit -m "refactor: remove AddonHub and GameModeSelection from Screen sealed class and breadcrumbs"
```

---

## Final Verification

- [ ] **Run full test suite**

```bash
./gradlew :composeApp:allTests
```

Expected: All tests pass. No references to `Screen.AddonHub` or `Screen.GameModeSelection` anywhere.

- [ ] **Smoke test the app**

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

Manually verify:
1. Home screen shows shield + addon tiles (e.g. TBC Anniversary)
2. Tapping TBC → section row fades in with "Tactics" + "Class Guides" tiles
3. "Class Guides" → navigates directly to Class Guides screen; breadcrumb shows `Home › Class Guides`
4. Tapping "Tactics" → bracket row fades in with 2v2/3v3/5v5 tiles
5. Tapping "2v2" → navigates to CompositionSelection; breadcrumb shows `Home › 2v2 Comps`
6. Clicking "Home" breadcrumb → returns to home with no selections (clean state)
7. Deep link `/tbc_anniversary/tactics/tbc_anniversary_2v2` → lands on CompositionSelection correctly
