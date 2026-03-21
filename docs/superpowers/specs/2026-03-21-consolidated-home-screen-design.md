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

Replace `AddonSelectionViewModel` with `HomeViewModel`:

```kotlin
class HomeViewModel(
    private val addonRepository: AddonRepository,
    private val gameModeRepository: GameModeRepository
) : ViewModel() {
    val state: StateFlow<HomeState>

    fun loadAddons()
    fun loadGameModes(addonId: String)  // called when addon selected
}

sealed class HomeState {
    data object Loading : HomeState()
    data class Error(val message: String) : HomeState()
    data class Success(
        val addons: List<Addon>,
        val gameModes: List<GameMode> = emptyList()
    ) : HomeState()
}
```

### UI selection state (local to composable)

```kotlin
data class HomeSelection(
    val addon: Addon? = null,
    val section: Section? = null
)
enum class Section { TACTICS, CLASS_GUIDES }
```

---

## Layout

Single `Column` with `verticalScroll`. Rows stack vertically; new rows appear below as selections are made.

```
┌─────────────────────────────────┐
│        [Shield + "Arena         │
│              Tactics"]          │
│                                 │
│  "Select your game"             │
│  [ TBC ] [ Wrath ] [ Retail ]   │  ← always visible
│                                 │
│  "What are you looking for?"    │  ← AnimatedVisibility
│  [ Tactics ]  [ Class Guides ]  │
│                                 │
│  "Select your bracket"          │  ← AnimatedVisibility (Tactics only)
│  [ 2v2 ]  [ 3v3 ]  [ 5v5 ]     │
└─────────────────────────────────┘
```

---

## Interaction Model

1. Screen loads → addon tiles visible only
2. Addon tapped → `selection.addon` set; ViewModel fetches game modes for that addon; section row animates in once data is ready
3. "Class Guides" tapped → navigate immediately to `Screen.ClassGuideList(addonId)`
4. "Tactics" tapped → `selection.section = TACTICS`; bracket row animates in (game modes already loaded)
5. Bracket tapped → navigate immediately to `Screen.CompositionSelection(addonId, modeId)`
6. Tapping already-selected addon → deselects, collapses rows below, resets to initial state

---

## Animation

- Row entry: `fadeIn() + slideInVertically(initialOffsetY = { it / 4 })`
- Row exit: `fadeOut() + slideOutVertically()`
- Selected tile: `Primary`-colored border + elevated background
- Completed-row tiles (non-selected): 60% alpha — visible as context, not competing with active row
- If game modes take >300ms to load after addon tap: inline micro-spinner within the section row area

---

## Navigation & Breadcrumbs

### Updated `Screen.buildStack` ancestor chains

| Screen | Ancestor chain |
|---|---|
| `CompositionSelection` | `[AddonSelection, CompositionSelection]` |
| `MatchupList` | `[AddonSelection, CompositionSelection, MatchupList]` |
| `MatchupDetail` | `[AddonSelection, CompositionSelection, MatchupList, MatchupDetail]` |
| `ClassGuideList` | `[AddonSelection, ClassGuideList]` |
| `SpecGuide` | `[AddonSelection, ClassGuideList, SpecGuide]` |

### `breadcrumbLabel()` for `CompositionSelection`

Shows formatted addonId + modeId to restore context lost by removing intermediate screens:

```
Home  ›  TBC 2v2 Comps
Home  ›  Guides  ›  Mage
```

### Back navigation

Navigating back to `AddonSelection` resets the home screen to its initial state (no selections, addon row only).

---

## Files Touched

| File | Change |
|---|---|
| `navigation/Screen.kt` | Remove `AddonHub`, `GameModeSelection`; update `buildStack`, `fromPath`, `toScreen` |
| `App.kt` | Remove NavHost destinations for `AddonHub`, `GameModeSelection`; wire `HomeViewModel` |
| `presentation/HomeViewModel.kt` | New — replaces `AddonSelectionViewModel` |
| `presentation/screens/AddonSelectionScreen.kt` | Full rewrite — cascading home screen |
| `presentation/screens/AddonHubScreen.kt` | Delete |
| `presentation/screens/TacticsGameModeSelectionScreen.kt` | Delete |
| `presentation/AddonSelectionViewModel.kt` | Delete — replaced by `HomeViewModel` |
| `presentation/GameModeSelectionViewModel.kt` | Delete — merged into `HomeViewModel` |
| `presentation/screens/components/AppHeader.kt` | Update `breadcrumbLabel()` for removed screens |

---

## Out of Scope

- Persisting the last selection across app restarts
- Animated transitions between home and content screens (existing shared element for shield is unchanged)
- Any changes to content screens (CompositionSelection, MatchupList, etc.)
