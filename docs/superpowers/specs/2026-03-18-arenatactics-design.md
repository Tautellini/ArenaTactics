# ArenaTactics — Design Spec
**Date:** 2026-03-18
**Project:** ArenaTactics — WoW TBC Classic 2v2 Arena Companion
**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, WASM/JS + JVM targets

---

## Overview

A fully static, client-side companion tool for World of Warcraft: The Burning Crusade Classic (Anniversary) 2v2 Arena. Allows users to select their team composition, view phase-based Best in Slot gear (with enchants and gems), and explore matchup strategies against enemy compositions.

Initial scope: Rogue/Mage composition only, with ~20-25 realistic/meta enemy matchups. Designed to be extended to additional game modes and expansions.

**Footer on AddonSelectionScreen:** `"Made with love for Kizaru"`

---

## Architecture

### Approach: Screen ViewModels + Simple Navigator (Approach B)

Three clean layers, all in `commonMain` except platform interop:

### Data Layer (`data/`)
- `model/` — `@Serializable` Kotlin data classes, no business logic
- `repository/` — `GearRepository`, `MatchupRepository`, `CompositionRepository`
  - Load JSON from `composeResources/files/` via `Res.readBytes(...)`
  - Parse with `kotlinx.serialization`
  - Expose `suspend` functions returning domain models
  - No caching needed — data is small and static

### Domain Layer (`domain/`)
- `CompositionGenerator` — generates valid compositions from a game mode's class pool, filtered by its composition set whitelist
- Matchup lookup is a simple map by ID — no further business logic

### Presentation Layer (`presentation/`)
- One ViewModel per screen holding `StateFlow<ScreenState>` (Loading / Success / Error)
- Shared `Navigator` held at root level
- Root `App.kt` collects navigator state and renders the top screen

### Platform Interop
- `expect/actual`: `refreshWowheadTooltips()` in `jsMain`/`wasmJsMain` (calls `WH.refreshLinks()`), no-op in `jvmMain`
- `expect/actual`: `openUrl(url: String)` for item click → new tab
- Wowhead `power.js` injected once at startup in `webMain/main.kt`

### New Dependency
- `kotlinx.serialization` plugin + runtime (the only addition to the project)

---

## Navigation

### Navigator
- Holds `MutableStateFlow<List<Screen>>` as a back stack
- `push(screen)` appends; `pop()` removes last
- On web: each `push()` also calls `history.pushState()` via JS interop so the browser back button works

### Screen Sealed Class

```kotlin
sealed class Screen {
    data object AddonSelection : Screen()
    data class CompositionSelection(val gameModeId: String) : Screen()
    data class GearView(val gameModeId: String, val compositionId: String) : Screen()
    data class MatchupList(val gameModeId: String, val compositionId: String) : Screen()
    data class MatchupDetail(
        val gameModeId: String,
        val compositionId: String,
        val matchupId: String
    ) : Screen()
}
```

### Screen Flow
```
AddonSelection
    └─> CompositionSelection("tbc_anniversary_2v2")
            ├─> GearView("tbc_anniversary_2v2", "rogue_mage")       [tab]
            └─> MatchupList("tbc_anniversary_2v2", "rogue_mage")    [tab]
                    └─> MatchupDetail(..., "rogue_mage_vs_warrior_druid")
```

`GearView` and `MatchupList` are sibling tabs within a composition hub — switching tabs does not push to the back stack. Only navigating into a `MatchupDetail` pushes.

The `gameModeId` flows through every screen, enabling future game modes without navigation changes.

---

## Data Models

```kotlin
@Serializable
data class GameMode(
    val id: String,
    val name: String,
    val description: String,
    val classPoolId: String,       // references class_pools/{classPoolId}.json
    val compositionSetId: String   // references composition_sets/{compositionSetId}.json
)

@Serializable
data class WowClass(
    val id: String,    // e.g. "rogue"
    val name: String,  // e.g. "Rogue"
    val color: String  // hex e.g. "#FFF569"
)

@Serializable
data class Composition(
    val class1Id: String,
    val class2Id: String
) {
    val id get() = "${class1Id}_${class2Id}"
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
    val id: String,             // e.g. "rogue_mage_vs_warrior_druid"
    val enemyClass1Id: String,
    val enemyClass2Id: String,
    val strategyMarkdown: String  // free-form markdown
)
```

---

## Static Asset Files

All bundled in `composeResources/files/`:

```
files/
├── game_modes.json                          # List<GameMode>
├── class_pools/
│   └── tbc.json                             # List<WowClass> — 9 TBC classes
├── composition_sets/
│   └── tbc_2v2.json                         # List<Composition> — ~20-25 meta comps
├── gear/
│   ├── gear_rogue_phase1.json               # GearPhase — Rogue S1 full BiS
│   ├── gear_rogue_phase2.json               # GearPhase — Rogue S2 full BiS
│   ├── gear_mage_phase1.json                # GearPhase — Mage S1 full BiS
│   └── gear_mage_phase2.json                # GearPhase — Mage S2 full BiS
└── matchups/
    └── matchups_rogue_mage.json             # List<Matchup> — all Rogue/Mage matchups
```

**Extensibility:** Adding a new class pool, expansion, or composition is a file drop — no schema changes required. Gear files are split by class+phase so adding a new class is a new file + registration in the repository.

---

## Wowhead Tooltip Integration

### Script Injection
In `webMain/main.kt`, before mounting the Compose app:
```kotlin
val script = document.createElement("script")
script.setAttribute("src", "https://wow.zamimg.com/widgets/power.js")
document.head!!.appendChild(script)
```

### Item Link Structure
Each item renders as a Wowhead anchor:
```
https://www.wowhead.com/tbc/item={wowheadId}
```
Wowhead's `power.js` attaches rich tooltips automatically on hover.

### Tooltip Refresh expect/actual
```kotlin
// commonMain
expect fun refreshWowheadTooltips()

// jsMain + wasmJsMain
actual fun refreshWowheadTooltips() {
    js("if (typeof WH !== 'undefined') WH.refreshLinks()")
}

// jvmMain
actual fun refreshWowheadTooltips() { /* no-op */ }
```
Called via `LaunchedEffect` after gear lists render.

### Click Behavior
Clicking an item opens `https://www.wowhead.com/tbc/item={wowheadId}` in a new tab.

### Fallback
If `power.js` fails to load, items still render with their local name, slot, enchant, and gems. No broken UI.

---

## UI Design System

### Color Palette

| Token | Hex | Usage |
|---|---|---|
| Background | `#0D0D0F` | App background |
| Surface | `#16161A` | Screen surfaces |
| Card | `#1C1C21` | Cards, list items |
| CardElevated | `#222228` | Hovered/selected state |
| Accent | `#C89B3C` | WoW gold — CTAs, highlights, active states |
| TextPrimary | `#E8E1D6` | Main text |
| TextSecondary | `#8A8490` | Labels, metadata |
| Divider | `#2A2A32` | Separators |

### Class Colors (Standard WoW)
| Class | Hex |
|---|---|
| Rogue | `#FFF569` |
| Mage | `#69CCF0` |
| Warrior | `#C79C6E` |
| Druid | `#FF7D0A` |
| Priest | `#FFFFFF` |
| Warlock | `#9482C9` |
| Hunter | `#ABD473` |
| Paladin | `#F58CBA` |
| Shaman | `#0070DE` |

### Typography
- Font: **Inter** (loaded via Google Fonts in `index.html`)
- Weights: 400 regular, 500 medium, 600 semibold

### Component Patterns
- **Composition cards** — horizontal pair of class-colored pills, card border, subtle hover lift
- **Gear rows** — Wowhead item icon (16×16) + item name as Wowhead anchor + enchant chip + colored gem dots
- **Matchup cards** — two class-colored badges for enemy comp + card hover state
- **Matchup detail** — full-width minimal markdown renderer (handles `##` headers, `**bold**`, `*italic*`, bullet lists, `---` dividers)

### Spacing & Shape
- Base unit: 8px. Scale: 8 / 12 / 16 / 24 / 32px
- Card corner radius: 8px
- Separation via background contrast, not heavy borders

---

## Screen Inventory

| Screen | ViewModel | Key Data |
|---|---|---|
| `AddonSelectionScreen` | `AddonSelectionViewModel` | `List<GameMode>` from `game_modes.json` |
| `CompositionSelectionScreen` | `CompositionSelectionViewModel` | `List<WowClass>` + `List<Composition>` from game mode's pool/set |
| `GearScreen` (tabbed hub) | `GearViewModel` | `List<GearPhase>` for both classes in selected composition |
| `MatchupListScreen` | `MatchupListViewModel` | `List<Matchup>` + class lookup for enemy badge rendering |
| `MatchupDetailScreen` | `MatchupDetailViewModel` | Single `Matchup` with rendered markdown |

---

## Source Layout

```
composeApp/src/
├── commonMain/kotlin/net/tautellini/arenatactics/
│   ├── App.kt
│   ├── data/
│   │   ├── model/          (GameMode, WowClass, Composition, GearItem, GearPhase, Matchup)
│   │   └── repository/     (GearRepository, MatchupRepository, CompositionRepository)
│   ├── domain/
│   │   └── CompositionGenerator.kt
│   ├── navigation/
│   │   ├── Navigator.kt
│   │   └── Screen.kt
│   └── presentation/
│       ├── AddonSelectionViewModel.kt
│       ├── CompositionSelectionViewModel.kt
│       ├── GearViewModel.kt
│       ├── MatchupListViewModel.kt
│       ├── MatchupDetailViewModel.kt
│       └── screens/
│           ├── AddonSelectionScreen.kt
│           ├── CompositionSelectionScreen.kt
│           ├── GearScreen.kt
│           ├── MatchupListScreen.kt
│           ├── MatchupDetailScreen.kt
│           └── components/   (ItemRow, ClassBadge, CompositionCard, MarkdownText)
├── commonMain/composeResources/files/
│   ├── game_modes.json
│   ├── class_pools/tbc.json
│   ├── composition_sets/tbc_2v2.json
│   ├── gear/gear_{class}_phase{n}.json
│   └── matchups/matchups_rogue_mage.json
├── jsMain/kotlin/.../
│   └── Platform.js.kt         (refreshWowheadTooltips, openUrl actuals)
├── wasmJsMain/kotlin/.../
│   └── Platform.wasmJs.kt     (refreshWowheadTooltips, openUrl actuals)
├── jvmMain/kotlin/.../
│   ├── main.kt
│   └── Platform.jvm.kt        (no-op actuals)
└── webMain/kotlin/.../
    └── main.kt                (script injection + ComposeViewport mount)
```

---

## Out of Scope
- Backend of any kind
- Blizzard API integration
- User authentication or persistence
- Compositions other than Rogue/Mage (initial release)
- Game modes other than TBC Anniversary 2v2 (initial release; architecture supports adding them)
