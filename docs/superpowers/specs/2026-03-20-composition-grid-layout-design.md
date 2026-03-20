# Composition Selection Grid Layout — Design Spec

## Goal

Replace the full-width card list in `CompositionSelectionScreen` with an adaptive grid so horizontal space is used efficiently at any window width.

## Context

`CompositionCard` currently uses `fillMaxWidth()`, rendering each comp as a wide row. On a desktop browser at 1400px this wastes the majority of the screen. The fix is a `LazyVerticalGrid` with adaptive column sizing — Compose calculates column count from available width automatically.

---

## Layout Changes

### CompositionSelectionScreen

- Replace `LazyColumn` with `LazyVerticalGrid(GridCells.Adaptive(minSize = 280.dp))`.
  - 280 dp is chosen so a two-badge card has comfortable padding; at 800px this yields ~2 columns, at 1400px ~4 columns.
  - Grid spacing: `horizontalArrangement = Arrangement.spacedBy(12.dp)`, `verticalArrangement = Arrangement.spacedBy(12.dp)`.
  - The grid must have `Modifier.weight(1f)` so it fills the remaining height inside the parent `Column` (which holds the back-button header row above it).
- **Imports:** Replace `foundation.lazy.LazyColumn` and `foundation.lazy.items` with `foundation.lazy.grid.LazyVerticalGrid`, `foundation.lazy.grid.GridCells`, `foundation.lazy.grid.GridItemSpan`, and `foundation.lazy.grid.items`. These are in a different package (`lazy.grid.*`) and the old `lazy.*` imports must be removed.
- **Tier headers** use `span = { GridItemSpan(maxLineSpan) }` so they always occupy the full row regardless of column count.
- **DOMINANT / STRONG / PLAYABLE** cards: standard `items(comps)` directly inside the `LazyVerticalGrid { }` DSL block — the grid handles layout automatically.
- **OTHERS section**: replace `item { AnimatedVisibility { Column { ... } } }` with a conditional block directly at `LazyGridScope` level:
  ```kotlin
  // Inside LazyVerticalGrid { }
  item(span = { GridItemSpan(maxLineSpan) }) { TierHeader(...) }
  if (othersExpanded) {
      items(comps) { rich -> CompositionCard(...) }
  }
  ```
  The `if` must sit at the `LazyGridScope` level — **not** inside an `item { }` wrapper — so that Others cards are composed lazily only when expanded. The `AnimatedVisibility` and nested `Column` workaround are removed entirely.

### CompositionCard

- Remove `fillMaxWidth()`. The grid column width drives card width.
- Add `heightIn(min = 72.dp)` so cards have consistent tile presence.
- Badge arrangement inside the card: keep `Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically)` — **no change to badge spacing**.
- `alpha` shimmer and conditional `clickable` behaviour unchanged.

**Note:** `CompositionCard` currently accesses `richComposition.specs[0]` and `richComposition.specs[1]` as hardcoded indices. This is a pre-existing issue (it does not support 3v3/5v5 brackets). It is **out of scope** for this layout change — do not touch the badge rendering logic here.

---

## CLAUDE.md Guidance Addition

Add to the "Additional Code Guidance" section:

> UI must be fully adaptive — use `GridCells.Adaptive`, `FlowRow`, or equivalent so layouts reflow naturally across screen widths. Never hardcode column counts or fixed widths for list/grid content. Layouts must look correct and visually appealing at both narrow (≈800px) and wide (≈1600px) viewports.

---

## Files Changed

| Action | File |
|--------|------|
| Modify | `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/CompositionSelectionScreen.kt` |
| Modify | `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/components/CompositionCard.kt` |
| Modify | `CLAUDE.md` |

---

## What Is Not Changing

- `SpecBadge` — unchanged.
- `TierHeader` — unchanged (given full-span treatment via `GridItemSpan(maxLineSpan)`).
- Badge arrangement inside `CompositionCard` (`Arrangement.spacedBy(8.dp)`) — unchanged.
- ViewModel and state — unchanged.
- Navigation behaviour — unchanged.
- `hasData` shimmer and click guard — unchanged.
