# ArenaTactics — Design Spec
**Date:** 2026-03-18
**Project:** ArenaTactics — WoW TBC Classic 2v2 Arena Companion
**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, WASM/JS + JVM targets

---

## Overview

A fully static, client-side companion tool for World of Warcraft: The Burning Crusade Classic (Anniversary) 2v2 Arena. Allows users to select their team composition, view phase-based Best in Slot gear (with enchants and gems), and explore matchup strategies against enemy compositions.

Initial scope: Rogue/Mage composition only, with ~20-25 realistic/meta enemy matchups. Designed to be extended to additional game modes and expansions.

**Footer on GameModeSelectionScreen:** `"Made with love for Kizaru"`

---

## Architecture

### Approach: Screen ViewModels + Simple Navigator (Approach B)

Three clean layers, all in `commonMain` except platform interop:

### Data Layer (`data/`)
- `model/` — `@Serializable` Kotlin data classes, no business logic
- `repository/` — `GearRepository`, `MatchupRepository`, `CompositionRepository`
  - Load JSON from `composeResources/files/` via `Res.readBytes(...)`
  - Parse with `kotlinx.serialization-json`
  - Expose `suspend` functions returning domain models
  - No caching needed — data is small and static

### Domain Layer (`domain/`)
- `CompositionGenerator` — generates valid compositions from a game mode's class pool, filtered by its composition set whitelist
- Matchup lookup is a simple `Map<String, Matchup>` keyed by `Matchup.id` — no further business logic

### Presentation Layer (`presentation/`)
- One ViewModel per screen holding `StateFlow<ScreenState>` (Loading / Success / Error)
- Shared `Navigator` held at root level
- Root `App.kt` collects navigator state and renders the top screen

### Platform Interop
Three `expect/actual` declarations in `commonMain/Platform.kt`:
- `refreshWowheadTooltips()` — calls `WH.refreshLinks()` on web, no-op on JVM
- `openUrl(url: String)` — opens URL in new tab on web, no-op on JVM
- `pushNavigationState(path: String)` — calls `history.pushState()` on web, no-op on JVM

Wowhead `power.js` injected once at startup in `webMain/main.kt`.

### New Dependency
`kotlinx.serialization` is the only new dependency. Add to project as follows:

**`gradle/libs.versions.toml`:**
```toml
[versions]
kotlinx-serialization = "1.8.0"

[libraries]
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

[plugins]
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

**Root `build.gradle.kts`** — no change needed; the plugin is applied per-module.

**`composeApp/build.gradle.kts`:**
```kotlin
plugins {
    alias(libs.plugins.kotlinSerialization)  // add this line
}
// in commonMain.dependencies:
implementation(libs.kotlinx.serialization.json)
```

---

## Navigation

### Navigator
- Holds `MutableStateFlow<List<Screen>>` as a back stack
- `push(screen)` appends and calls `pushNavigationState("/${screen.path}")`
- `pop()` removes last entry
- Browser back button triggers `pop()` via a `window.addEventListener("popstate", ...)` listener registered in `webMain/main.kt`

### `pushNavigationState` expect/actual
```kotlin
// commonMain
expect fun pushNavigationState(path: String)

// jsMain + wasmJsMain
actual fun pushNavigationState(path: String) {
    js("history.pushState(null, '', path)")
}

// jvmMain
actual fun pushNavigationState(path: String) { /* no-op */ }
```

### Screen Sealed Class

```kotlin
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
}
```

### Screen Flow
```
GameModeSelection
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
    val class1Id: String,   // always alphabetically first by class ID
    val class2Id: String    // always alphabetically second by class ID
) {
    // Canonical ID: class IDs sorted alphabetically and joined with "_"
    // e.g. "mage" + "rogue" → "mage_rogue" (never "rogue_mage")
    val id: String get() = "${class1Id}_${class2Id}"
}
```

**Canonical ordering rule:** In all JSON files and at all call sites, `class1Id` < `class2Id` alphabetically. `CompositionRepository` enforces this on load by sorting the two IDs before constructing the `Composition`. Lookups are always by canonical ID via a `Map<String, Composition>` built at load time. This prevents silent mismatches between `"rogue_mage"` and `"mage_rogue"`.

```kotlin
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
    val id: String,             // e.g. "rogue_mage_vs_warrior_druid" — enemy comp uses canonical order
    val enemyClass1Id: String,
    val enemyClass2Id: String,
    val strategyMarkdown: String  // free-form markdown, all strategy content
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

**Gear file naming convention:** `gear_{classId}_phase{phaseNumber}.json`. `GearRepository.getGearForComposition(compositionId, gameModeId)` derives the two class IDs from `CompositionRepository.getById(compositionId)`, then loads all available phase files for each class by iterating phase numbers 1..N until a file is not found. "Registration" means dropping a correctly named file — no code change required.

**Extensibility:** Adding a new class pool, expansion, or composition set is a file drop — no schema changes required.

---

## Wowhead Tooltip Integration

### `webMain` Source Set
`webMain` is a custom intermediate source set shared by both `jsMain` and `wasmJsMain` browser targets, already present in this project template. It has access to the Kotlin/JS DOM API (`kotlinx.browser`). Script injection and `ComposeViewport` mounting live here.

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

### Platform Interop expect/actuals
```kotlin
// commonMain
expect fun refreshWowheadTooltips()
expect fun openUrl(url: String)
expect fun pushNavigationState(path: String)

// jsMain — js() accepts variable references in Kotlin/JS
actual fun refreshWowheadTooltips() {
    js("if (typeof WH !== 'undefined') WH.refreshLinks()")
}
actual fun openUrl(url: String) {
    js("window.open(url, '_blank')")
}
actual fun pushNavigationState(path: String) {
    js("history.pushState(null, '', path)")
}

// wasmJsMain — js() only accepts string literals in Kotlin/Wasm; use kotlinx.browser DOM API instead
actual fun refreshWowheadTooltips() {
    js("if (typeof WH !== 'undefined') WH.refreshLinks()")  // no variable ref, safe as literal
}
actual fun openUrl(url: String) {
    window.open(url, "_blank")   // kotlinx.browser.window
}
actual fun pushNavigationState(path: String) {
    window.history.pushState(null, "", path)   // kotlinx.browser.window
}

// jvmMain
actual fun refreshWowheadTooltips() { /* no-op */ }
actual fun openUrl(url: String) { /* no-op */ }
actual fun pushNavigationState(path: String) { /* no-op */ }
```

`refreshWowheadTooltips()` called via `LaunchedEffect` after gear lists render.

### Fallback
If `power.js` fails to load, items still render with their local name, slot, enchant, and gems from local data. No broken UI.

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
- **Matchup detail** — renders via `MarkdownText` composable (see below)

### `MarkdownText` Component
A custom `@Composable` in `presentation/screens/components/MarkdownText.kt`. Parses and renders:
- `## Heading` — semibold TextPrimary, larger size
- `**bold**` — semibold inline span
- `*italic*` — italic inline span
- `- item` / `* item` — bullet list with indent
- `---` — horizontal `Divider`
- All other text — regular TextPrimary

**Unsupported syntax** (tables, code blocks, images, links) is rendered as raw text — no errors, no stripping.

This is a custom component; no third-party markdown library is required.

### Spacing & Shape
- Base unit: 8px. Scale: 8 / 12 / 16 / 24 / 32px
- Card corner radius: 8px
- Separation via background contrast, not heavy borders

---

## Screen Inventory

| Screen | ViewModel | Key Data |
|---|---|---|
| `GameModeSelectionScreen` | `GameModeSelectionViewModel` | `List<GameMode>` from `game_modes.json` |
| `CompositionSelectionScreen` | `CompositionSelectionViewModel` | `List<WowClass>` + `List<Composition>` from game mode's pool/set |
| `GearScreen` (tabbed hub) | `GearViewModel` | Derives two class IDs from `compositionId` via `CompositionRepository`; loads all phase files per class |
| `MatchupListScreen` | `MatchupListViewModel` | `List<Matchup>` + `Map<String, WowClass>` for enemy badge rendering |
| `MatchupDetailScreen` | `MatchupDetailViewModel` | Re-fetches single `Matchup` from `MatchupRepository.getById(matchupId)`; renders `strategyMarkdown` via `MarkdownText` |

**`MatchupDetailViewModel` contract:** Re-fetches from the repository by `matchupId`. Since data is static and in-memory after first load, this is effectively instant. No data is passed through the navigation stack.

---

## Source Layout

```
composeApp/src/
├── commonMain/kotlin/net/tautellini/arenatactics/
│   ├── App.kt
│   ├── Platform.kt                          (expect declarations)
│   ├── data/
│   │   ├── model/                           (GameMode, WowClass, Composition, GearItem, GearPhase, Matchup)
│   │   └── repository/                      (GearRepository, MatchupRepository, CompositionRepository)
│   ├── domain/
│   │   └── CompositionGenerator.kt
│   ├── navigation/
│   │   ├── Navigator.kt
│   │   └── Screen.kt
│   └── presentation/
│       ├── GameModeSelectionViewModel.kt
│       ├── CompositionSelectionViewModel.kt
│       ├── GearViewModel.kt
│       ├── MatchupListViewModel.kt
│       ├── MatchupDetailViewModel.kt
│       └── screens/
│           ├── GameModeSelectionScreen.kt
│           ├── CompositionSelectionScreen.kt
│           ├── GearScreen.kt
│           ├── MatchupListScreen.kt
│           ├── MatchupDetailScreen.kt
│           └── components/                  (ItemRow, ClassBadge, CompositionCard, MarkdownText)
├── commonMain/composeResources/files/
│   ├── game_modes.json
│   ├── class_pools/tbc.json
│   ├── composition_sets/tbc_2v2.json
│   ├── gear/gear_{classId}_phase{n}.json
│   └── matchups/matchups_rogue_mage.json
├── jsMain/kotlin/.../Platform.js.kt         (actual implementations)
├── wasmJsMain/kotlin/.../Platform.wasmJs.kt (actual implementations)
├── jvmMain/kotlin/.../
│   ├── main.kt
│   └── Platform.jvm.kt                      (no-op actuals)
└── webMain/kotlin/.../
    └── main.kt   (popstate listener + power.js injection + ComposeViewport mount)
```

---

## Out of Scope
- Backend of any kind
- Blizzard API integration
- User authentication or persistence
- Compositions other than Rogue/Mage (initial release)
- Game modes other than TBC Anniversary 2v2 (initial release; architecture supports adding them)
