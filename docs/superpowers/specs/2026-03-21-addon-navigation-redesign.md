# Addon Navigation Redesign

**Date:** 2026-03-21
**Status:** Approved

## Overview

Restructure the app's entry flow to introduce addons as the top-level concept, then split content into two sections — **Tactics** (composition + matchup guides) and **Class Guides** (per-spec gear). Gear is removed from the Tactics path entirely and becomes standalone, addon-scoped content.

---

## Navigation Flow

```
/ ──────────────────── AddonSelection         (home)
/{addonId} ─────────── AddonHub               (Tactics | Class Guides)

  Tactics path:
  /{addonId}/tactics ─────────────────────────────────── GameModeSelection
  /{addonId}/tactics/{modeId} ────────────────────────── CompositionSelection
  /{addonId}/tactics/{modeId}/{compId}/matchups ───────── MatchupList
  /{addonId}/tactics/{modeId}/{compId}/matchups/{matchupId} ── MatchupDetail

  Class Guides path:
  /{addonId}/guides ───────────────────────── ClassGuideList   (class + spec picker)
  /{addonId}/guides/{classId}/{specId} ─────── SpecGuide        (gear by phase)
```

**Key decisions:**
- Class Guides branch off at the addon level — game mode is irrelevant to gear.
- After selecting a composition, the user goes straight to MatchupList (no tabs).
- `GearView` and `CompositionHubScreen` (tabbed hub) are removed.

### URL Parsing Strategy (`fromPath`)

Segment layout (0-indexed after stripping leading `/`):
- `[0]` = `addonId` (never `"tactics"` or `"guides"` — these are reserved words)
- `[1]` = section literal: `"tactics"` | `"guides"` (absent → `AddonHub`)
- Tactics: `[2]` = `modeId`, `[3]` = `compId`, `[4]` = `"matchups"`, `[5]` = `matchupId`
- Guides: `[2]` = `classId`, `[3]` = `specId`

`addonId` values in `addons.json` must never be `"tactics"` or `"guides"`. `fromPath` disambiguates purely by segment position — no anchor word search needed.

### `buildStack` Definitions

Complete back-stacks for deep-link initialization:

| Screen | Stack |
|---|---|
| `AddonSelection` | `[AddonSelection]` |
| `AddonHub(a)` | `[AddonSelection, AddonHub(a)]` |
| `GameModeSelection(a)` | `[AddonSelection, AddonHub(a), GameModeSelection(a)]` |
| `CompositionSelection(a, m)` | `[AddonSelection, AddonHub(a), GameModeSelection(a), CompositionSelection(a,m)]` |
| `MatchupList(a, m, c)` | `[AddonSelection, AddonHub(a), GameModeSelection(a), CompositionSelection(a,m), MatchupList(a,m,c)]` |
| `MatchupDetail(a, m, c, id)` | `[AddonSelection, AddonHub(a), GameModeSelection(a), CompositionSelection(a,m), MatchupList(a,m,c), MatchupDetail(a,m,c,id)]` |
| `ClassGuideList(a)` | `[AddonSelection, AddonHub(a), ClassGuideList(a)]` |
| `SpecGuide(a, cl, sp)` | `[AddonSelection, AddonHub(a), ClassGuideList(a), SpecGuide(a,cl,sp)]` |

### `toScreen()` Extension

`NavBackStackEntry.toScreen()` in `Screen.kt` contains a `when` block matching destination route strings. Every branch must be updated for the new variants. New branches required:
- `"AddonHub"` → `toRoute<Screen.AddonHub>()`
- `"GameModeSelection"` with addonId → `toRoute<Screen.GameModeSelection>()`
- `"ClassGuideList"` → `toRoute<Screen.ClassGuideList>()`
- `"SpecGuide"` → `toRoute<Screen.SpecGuide>()`
- Remove the `"GearView"` branch

---

## Data Model Changes

### New: `Addon` — file: `data/model/Addon.kt`
```kotlin
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

New file: `files/addons.json`

### Modified: `GameMode` — extract to `data/model/GameMode.kt`
- **Remove:** `specPoolId`, `classPoolId` (moved to `Addon`)
- **Add:** `addonId: String`

### New: `AddonRepository`
Loads `addons.json`. Provides:
- `getAll(): List<Addon>`
- `getById(id: String): Addon?`

### Modified: `GameModeRepository`
Add:
- `getByAddon(addonId: String): List<GameMode>` — delegates to `getAll().filter { it.addonId == addonId }`

### Modified: `GearRepository`
- Add: `suspend fun getGearForSpec(classId: String): List<GearPhase>` — delegates to the existing `loadPhasesForClass(classId)`.
- Remove: `getGearForComposition` and its `CompositionRepository` constructor parameter (no longer needed).

Gear data is **class-scoped** (one gear list per class, shared across all specs of that class — e.g., all Druid specs share `gear_druid_phaseN.json`). The `specId` in the `SpecGuide` URL is navigation context (spec name, icon, role display) — it does not filter gear items.

### Repository migration: pool ID sourcing

`SpecRepository`, `CompositionRepository`, `MatchupListViewModel`, and `MatchupDetailViewModel` currently read `specPoolId`/`classPoolId` from `GameMode`. Lookup recipe after migration:
```
val addon = addonRepository.getById(addonId) // → specPoolId, classPoolId
val mode  = gameModeRepository.getAll().first { it.id == gameModeId } // → compositionSetId, teamSize
```
All ViewModels that need pool IDs must accept `addonId` as a constructor parameter and call `AddonRepository.getById(addonId)`.

---

## Screen Inventory

| Screen | Status | Notes |
|---|---|---|
| `AddonSelection` | Renamed (was `GameModeSelection`) | Home screen — lists addons |
| `AddonHub` | New | Section picker: Tactics / Class Guides |
| `GameModeSelection` | New | Tactics sub-path — lists game modes via `getByAddon(addonId)` |
| `CompositionSelection` | Updated | Takes `addonId` + `gameModeId`; pool IDs from `Addon`, compositionSetId/teamSize from `GameMode` |
| `MatchupList` | Updated | Direct list, no tabs; takes `addonId` for pool ID resolution |
| `MatchupDetail` | Updated | Takes `addonId` for pool ID resolution |
| `GearView` | Removed | Replaced by `SpecGuide` |
| `CompositionHubScreen` | Removed | Tabs eliminated |
| `ClassGuideList` | New | Adaptive grid of specs from addon's spec pool |
| `SpecGuide` | New | Gear phases for a spec (class-scoped data, spec context for display) |

---

## ViewModel Changes

| ViewModel | Change |
|---|---|
| `GameModeSelectionViewModel` → `AddonSelectionViewModel` | Loads addons instead of game modes |
| `AddonHubViewModel` | New — loads `Addon` by id; no heavy data fetching |
| `GameModeSelectionViewModel` (new) | Loads game modes via `getByAddon(addonId)` |
| `CompositionSelectionViewModel` | Takes `addonId` + `gameModeId`; loads `Addon` for pool IDs, `GameMode` for `compositionSetId`/`teamSize` |
| `MatchupListViewModel` | Updated: takes `addonId`; uses lookup recipe above |
| `MatchupDetailViewModel` | Updated: takes `addonId`; uses lookup recipe above |
| `GearViewModel` → `SpecGuideViewModel` | Renamed + updated: takes `addonId`, `classId`, `specId`; calls `getGearForSpec(classId)` |
| `ClassGuideListViewModel` | New — loads all `WowSpec` from addon's `specPoolId`; sorts DPS-first, healer-last (sort on `WowSpec.role` in ViewModel, not in the enrichment layer) |

---

## `App.kt` Wiring Changes

`App.kt` is the composition root and requires these updates:
- **Repository instantiation:** `GearRepository` constructor no longer takes `CompositionRepository`. Add `AddonRepository`.
- **NavHost destinations:** All composable destinations updated for the new `Screen` variants. Remove `GearView` and `MatchupList`-with-tabs destinations. Add `AddonHub`, `GameModeSelection(addonId)`, `ClassGuideList`, `SpecGuide`.
- **Deep-link init block:** References `Screen.GameModeSelection` (data object) → update to `Screen.AddonSelection`.
- **AppHeader visibility guard:** `currentScreen !is Screen.GameModeSelection` → `currentScreen !is Screen.AddonSelection`.
- **Shield home tap in AppHeader:** `Screen.GameModeSelection` reference → `Screen.AddonSelection`.

---

## `AppHeader` Breadcrumb Labels

`AppHeader.kt` contains a `Screen.breadcrumbLabel()` extension. New/changed labels:

| Screen | Label |
|---|---|
| `AddonSelection` | `"Home"` |
| `AddonHub` | `addonId.formatId()` (e.g., `"TBC Anniversary"`) |
| `GameModeSelection(addonId)` | `"Tactics"` |
| `CompositionSelection` | `gameModeId.formatId()` (unchanged behaviour) |
| `MatchupList` | `"Matchups"` (unchanged) |
| `MatchupDetail` | `"Detail"` (unchanged) |
| `ClassGuideList` | `"Class Guides"` |
| `SpecGuide` | `specId.formatId()` (e.g., `"Druid Restoration"`) |
| `GearView` | Remove |

The `Box(clickable { onNavigate(Screen.GameModeSelection) })` reference in `AppHeader.kt` must be updated to `Screen.AddonSelection`.

---

## Scope & Constraints

- **Class Guides content:** Gear only at launch (phases 1–2). No talent builds, consumables, or stat priorities in scope.
- **Gear scope:** Gear files are class-scoped (`gear_{classId}_phaseN.json`). All specs of a class share the same gear list. `specId` in the URL is for display context only.
- **Data scope:** TBC Anniversary 2v2 remains the only fully populated addon/mode. Other modes stay greyed out (`hasData: false`).
- **Model file separation:** `Addon` goes in `data/model/Addon.kt`. `GameMode` extracted to `data/model/GameMode.kt`. Per CLAUDE.md, models must not all live in one file.
- **Adaptive layout:** All new screens must use `GridCells.Adaptive` / `FlowRow` — no hardcoded column counts.
- **Web safety:** All coroutine paths in new screens must catch `Throwable` (not just `Exception`) per project guidelines.
- **Spec ordering:** `ClassGuideList` displays specs DPS-first, healers last — applied via sort on `WowSpec.role` in `ClassGuideListViewModel`.
- **addonId reserved words:** Values in `addons.json` must never be `"tactics"` or `"guides"` (URL parsing relies on these as section discriminators).
