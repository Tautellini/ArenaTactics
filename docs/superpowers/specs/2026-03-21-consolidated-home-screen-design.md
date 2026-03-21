# Consolidated Home Screen Design

## Goal

Collapse the three-screen selection flow (AddonSelection → AddonHub → TacticsGameModeSelection) into a single smart home screen with inline cascading selection steps.

## Problem

The current flow requires 2–3 full-screen navigations before reaching any content screen. AddonHub offers only two choices (Tactics / Class Guides). TacticsGameModeSelection offers bracket tiles. Both are full screens for what are essentially single-choice prompts, creating unnecessary navigation overhead.

## Solution

The home screen becomes a cascading launcher. Selection rows appear progressively inline: addon row → section row → bracket row. Tapping the final item navigates immediately with no extra button.

---

## Architecture

### Screens removed

| Screen | Reason |
|---|---|
| `AddonHubScreen` | Collapsed into home |
| `TacticsGameModeSelectionScreen` | Collapsed into home |

### Sealed class changes

Remove from `Screen`:
- `Screen.AddonHub`
- `Screen.GameModeSelection`

### ViewModel

Replace `AddonSelectionViewModel` with `HomeViewModel` in `presentation/HomeViewModel.kt`. The state classes (`HomeState`, `GameModeRowState`) live in the same file.

Unlike other ViewModels in the project which use a flat top-level `sealed class` with `Loading/Success/Error`, `HomeViewModel` introduces a nested `GameModeRowState` field inside `HomeState.Success`. This is intentional: the addon list and game mode list have independent loading lifecycles — addons load once at startup, game modes load lazily on demand. A flat top-level state cannot represent both independently without losing the addon list on game-mode load.

```kotlin
class HomeViewModel(
    private val addonRepository: AddonRepository,
    private val gameModeRepository: GameModeRepository
) : ViewModel() {
    val state: StateFlow<HomeState>

    // Addons are loaded in init {} — no public loadAddons() method.
    // init { viewModelScope.launch { try { ... } catch (t: Throwable) { ... } } }

    // Called when addon is tapped. Transitions gameModeRow: Idle → Loading → Ready/Error.
    // Guard: if gameModeRow is already Ready for this addonId, do nothing (avoids spinner flash on re-tap).
    // Must use catch(Throwable) in the coroutine body; on Throwable emits GameModeRowState.Error.
    fun loadGameModes(addonId: String)

    // Called when addon is deselected. Resets gameModeRow to Idle.
    fun resetGameModes()
}

sealed class HomeState {
    data object Loading : HomeState()
    data class Error(val message: String) : HomeState()
    data class Success(
        val addons: List<Addon>,
        val gameModeRow: GameModeRowState = GameModeRowState.Idle
    ) : HomeState()
}

sealed class GameModeRowState {
    data object Idle : GameModeRowState()        // no addon selected yet
    data object Loading : GameModeRowState()     // fetching game modes
    data class Ready(val modes: List<GameMode>) : GameModeRowState()
    data class Error(val message: String) : GameModeRowState()
}
```

`HomeViewModel` requires `addonRepository` and `gameModeRepository`. Both are already constructed at the top of `App.kt` — no new repository instantiation is needed.

### UI selection state (local to composable)

`HomeSelection` and `Section` are defined at file scope inside `AddonSelectionScreen.kt` with `private` visibility. They are UI-only types (not domain/data models) and must NOT be extracted to separate files. The project rule "do not place all Models in a singular file" applies to data/domain models, not composable-local state helpers.

```kotlin
private data class HomeSelection(
    val addon: Addon? = null,
    val section: Section? = null
)
private enum class Section { TACTICS, CLASS_GUIDES }
```

`HomeSelection` is local composable state (`remember { mutableStateOf(HomeSelection()) }`). It is not part of the ViewModel.

### ViewModel lifecycle and back-navigation

`HomeViewModel` lives in the `ViewModelStore` of the `AddonSelection` `NavBackStackEntry` — it survives while that entry is on the back stack. When the user navigates to a content screen and then returns to home (via breadcrumb or back), `HomeViewModel.state` still holds whatever `GameModeRowState` was set. Meanwhile, `HomeSelection` local state is re-created fresh (no selections).

**This is the desired behaviour:** the home screen always starts clean on re-entry. The user must tap an addon again to re-trigger the section row. The stale `GameModeRowState.Ready` data in the ViewModel is not visible because `HomeSelection.addon == null` — the section row is hidden. When the user taps the same addon again, `loadGameModes` is called — but because the ViewModel guards against re-fetching when already `Ready` for that addonId, no spinner flash occurs.

No `SavedStateHandle`, explicit keying, or `LaunchedEffect` cleanup is needed.

### Class Guides vs Tactics ViewModel interaction

- Tapping **"Tactics"**: sets `selection.section = TACTICS`; bracket row animates in using already-loaded `GameModeRowState.Ready` data. No additional ViewModel call.
- Tapping **"Class Guides"**: navigates immediately to `Screen.ClassGuideList(addonId)`. No ViewModel call — the ClassGuideList screen owns its own loading.

### Section tile availability

The section row always shows both tiles (Tactics and Class Guides). Disabled tiles render at 35% alpha and are not clickable — matching the existing `hasData` pattern.

| Tile | Disabled when |
|---|---|
| Tactics | `GameModeRowState.Loading` (spinner shown, not clickable), `GameModeRowState.Error` (error text shown inline, see below), or `GameModeRowState.Ready` with all modes having `hasData = false` |
| Class Guides | Never disabled — ClassGuideList handles its own empty state |

**`GameModeRowState.Error` UI:** Render a short inline error message in place of the Tactics tile icon — e.g. `Text("Failed to load", color = TextSecondary, fontSize = 11.sp)`. No retry button. The user can tap the addon again (step 7 above resets; then tapping it again calls `loadGameModes` again).

**Section row `AnimatedVisibility` condition:** `visible = selection.addon != null`. This condition is independent of `GameModeRowState` — the section row stays visible even when `GameModeRowState.Error`, so the error message is shown and the user can deselect the addon to retry.

---

## Layout

Single `Column` with `verticalScroll`. Rows stack vertically; new rows appear below as selections are made.

The shield logo block (`ShieldLogoBlock` + shimmer animation + `shieldModifier` for shared element) is preserved exactly as-is from the current `AddonSelectionScreen`. The "Made with love for Kizaru" footer text is preserved at the bottom of the outer `Box`.

```
┌─────────────────────────────────┐
│        [Shield + "Arena         │
│              Tactics"]          │
│                                 │
│  "Select your game"             │
│  [ TBC ] [ Wrath ] [ Retail ]   │  ← always visible
│                                 │
│  "What are you looking for?"    │  ← AnimatedVisibility (addon selected)
│  [ Tactics ]  [ Class Guides ]  │
│                                 │
│  "Select your bracket"          │  ← AnimatedVisibility (section = TACTICS)
│  [ 2v2 ]  [ 3v3 ]  [ 5v5 ]     │
│                                 │
│       Made with love for Kizaru │  ← bottom-pinned, unchanged
└─────────────────────────────────┘
```

The outer `Box(fillMaxSize)` + inner `Column` structure from the current screen is preserved. The inner `Column` gains `verticalScroll` and the two `AnimatedVisibility` row blocks.

Section tiles and bracket tiles use `FlowRow` (same as the existing addon row) — no fixed `Row` with hardcoded item count.

---

## Interaction Model

1. Screen loads → addon tiles visible only
2. Addon tapped → `selection.addon` set; ViewModel calls `loadGameModes(addonId)`; section row animates in immediately (Tactics tile shows spinner until `GameModeRowState.Ready`)
3. **Tapping a different addon while section or bracket rows are visible** → reset `selection` to `HomeSelection(addon = newAddon)`; call `viewModel.loadGameModes(newAddon.id)`; section row stays visible because `selection.addon != null` (now showing state for new addon); bracket row collapses because `selection.section == null`
4. "Class Guides" tapped → navigate immediately to `Screen.ClassGuideList(addonId)`; no ViewModel interaction
5. "Tactics" tapped → `selection.section = TACTICS`; bracket row animates in (game modes already in `GameModeRowState.Ready`)
6. Bracket tapped → navigate immediately to `Screen.CompositionSelection(addonId, modeId)`
7. Tapping already-selected addon → sets `selection = HomeSelection()`; calls `viewModel.resetGameModes()`; section + bracket rows collapse

---

## Animation

- Row entry: `fadeIn() + slideInVertically(initialOffsetY = { it / 4 })`
- Row exit: `fadeOut() + slideOutVertically()`
- Selected tile: `Primary`-colored border + elevated background
- Completed-row tiles (non-selected): 60% alpha — visible as context, not competing with active row
- Tactics tile while `GameModeRowState.Loading`: inline `CircularProgressIndicator` replaces tile icon; tile not clickable

---

## Navigation & Breadcrumbs

### Updated `Screen.buildStack`

Delete the `is AddonHub` and `is GameModeSelection` branches from `buildStack`. Replace every remaining branch that previously included `AddonHub(screen.addonId)` or `GameModeSelection(screen.addonId)` with the simplified chains below — do not leave those constructors in any branch. The updated chains:

| Screen | New ancestor chain |
|---|---|
| `CompositionSelection` | `[AddonSelection, CompositionSelection]` |
| `MatchupList` | `[AddonSelection, CompositionSelection, MatchupList]` |
| `MatchupDetail` | `[AddonSelection, CompositionSelection, MatchupList, MatchupDetail]` |
| `ClassGuideList` | `[AddonSelection, ClassGuideList]` |
| `SpecGuide` | `[AddonSelection, ClassGuideList, SpecGuide]` |

### `breadcrumbLabel()` updates

`CompositionSelection` label changes to `"${gameModeId.formatId()} Comps"` (e.g. "2v2 Comps"), reusing the existing `formatId()` utility with a " Comps" suffix. This label appears wherever `CompositionSelection` is an ancestor chip (MatchupList, MatchupDetail).

Remove the `is Screen.AddonHub` and `is Screen.GameModeSelection` cases from `breadcrumbLabel()`.

The `CompositionSelection` case in `AppHeader.kt` currently reads `gameModeId.formatId()` — change it in-place to `"${gameModeId.formatId()} Comps"`. Do not add a duplicate case.

```
Home  ›  2v2 Comps                          (on CompositionSelection)
Home  ›  2v2 Comps  ›  Matchups             (on MatchupList)
Home  ›  2v2 Comps  ›  Matchups  ›  Detail  (on MatchupDetail)
Home  ›  Class Guides                       (on ClassGuideList)
Home  ›  Class Guides  ›  Mage              (on SpecGuide)
```

### URL routing — `fromPath` changes

Two specific edits in the `fromPath` `when` block:

```kotlin
// BEFORE:
null      -> AddonHub(addonId)                    // line 34 — CHANGE to:
null      -> AddonSelection

// BEFORE (inside "tactics" branch):
val modeId = segs.getOrNull(2) ?: return GameModeSelection(addonId)   // line 36 — CHANGE to:
val modeId = segs.getOrNull(2) ?: return AddonSelection
```

The `else -> AddonSelection` fallback at line 49 is unchanged.

`Screen.path` for `AddonHub` and `GameModeSelection` disappears automatically when those sealed class entries are deleted — no explicit changes to `Screen.path` needed.

### `toScreen()` changes

Delete these two branches (lines 85–86 and 91–92 in the current file):

```kotlin
// DELETE:
"GameModeSelection" in route -> toRoute<Screen.GameModeSelection>()
    .let { Screen.GameModeSelection(it.addonId) }

// DELETE:
"AddonHub" in route -> toRoute<Screen.AddonHub>()
    .let { Screen.AddonHub(it.addonId) }
```

The remaining branches and the `else` fallback are unchanged. `AddonSelection` remains first (line 78) and is checked before all others.

---

## Files Touched

| File | Change |
|---|---|
| `navigation/Screen.kt` | Remove `AddonHub`, `GameModeSelection` sealed entries; update `buildStack`, `fromPath`, `toScreen()` as specified above |
| `App.kt` | Remove NavHost destinations for `AddonHub`, `GameModeSelection`; replace `import ...AddonSelectionViewModel` with `import ...HomeViewModel`; update the `composable<Screen.AddonSelection>` block to: `val viewModel = viewModel { HomeViewModel(addonRepository, gameModeRepository) }` then call `AddonSelectionScreen(viewModel, onNavigate, shieldModifier)` |
| `presentation/HomeViewModel.kt` | New — `HomeViewModel`, `HomeState`, `GameModeRowState` |
| `presentation/screens/AddonSelectionScreen.kt` | Full rewrite — cascading home screen; `HomeSelection` + `Section` defined here |
| `presentation/screens/AddonHubScreen.kt` | Delete |
| `presentation/screens/TacticsGameModeSelectionScreen.kt` | Delete |
| `presentation/AddonSelectionViewModel.kt` | Delete — replaced by `HomeViewModel` |
| `presentation/AddonHubViewModel.kt` | Delete — dead code once `AddonHubScreen` is deleted |
| `presentation/GameModeSelectionViewModel.kt` | Delete — merged into `HomeViewModel` |
| `presentation/screens/components/AppHeader.kt` | Update `breadcrumbLabel()`: new `CompositionSelection` label; remove deleted screen cases |

---

## Implementation Notes

- **New `AddonSelectionScreen` composable signature:**
  ```kotlin
  @Composable
  fun AddonSelectionScreen(
      viewModel: HomeViewModel,
      onNavigate: (Screen) -> Unit,
      shieldModifier: Modifier = Modifier
  )
  ```
  The parameter name and `shieldModifier` default are unchanged; only the ViewModel type changes.

- **`ScreenNavigationTest.kt`** (`commonTest/kotlin/.../navigation/ScreenNavigationTest.kt`) must be updated — it references `Screen.AddonHub` and `Screen.GameModeSelection` which will no longer exist. Required changes:
  - `fromPathAddonHub`: change expected value to `Screen.AddonSelection`
  - `fromPathGameModeSelection`: change expected value to `Screen.AddonSelection`
  - `buildStackAddonHub`: delete this test (the screen no longer exists)
  - `buildStackGameModeSelection`: delete this test
  - `buildStackCompositionSelection`: update to expect size 2, `stack[0]=AddonSelection`, `stack[1]=CompositionSelection`
  - `buildStackMatchupList`: update to expect size 3
  - `buildStackMatchupDetail`: update to expect size 4, `stack[2]=MatchupList`, `stack[3]=MatchupDetail`
  - `buildStackClassGuideList`: update to expect size 2, `stack[0]=AddonSelection`, `stack[1]=ClassGuideList`
  - `buildStackSpecGuide`: update to expect size 3, `stack[1]=ClassGuideList`, `stack[2]=SpecGuide`
  - `pathRoundTrip`: remove the two `Screen.AddonHub` and `Screen.GameModeSelection` entries from the list

- **`initialSkipCount` in `App.kt`**: The URL bridge deep-link logic computes `Screen.buildStack(Screen.fromPath(...))` and uses the stack size to determine how many synthetic navigation events to skip. After updating `buildStack`, the stack depths shrink. All cases are automatically correct — verified:
  | Deep link | Old stack size | New stack size |
  |---|---|---|
  | `/tbc/tactics/2v2/druid...` → `CompositionSelection` | 4 | 2 |
  | `/tbc/guides` → `ClassGuideList` | 3 | 2 |
  | `/tbc/guides/druid/druid_resto` → `SpecGuide` | 4 | 3 |
  No manual override is needed.

- **`AddonSelectionState` deletion**: Verified — `AddonSelectionState` is referenced only in `AddonSelectionViewModel.kt` and `AddonSelectionScreen.kt`. Both are being replaced/deleted. No other file is broken.

- **Sealed class serialization**: `AddonHub` and `GameModeSelection` are `@Serializable data class`. Deleting them removes their `when` branches from `Screen.path` automatically. No serialization registration or companion-object glue outside `Screen.kt` references these entries.

- **Scroll state**: Use `rememberScrollState()` for the inner `Column`. No saved instance state needed — the home screen always starts at position 0 on re-entry.

---

## Out of Scope

- Persisting the last selection across app restarts
- Animated transitions between home and content screens (existing shared element for shield is unchanged)
- Any changes to content screens (CompositionSelection, MatchupList, etc.)
