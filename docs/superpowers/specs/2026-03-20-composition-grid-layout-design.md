# Composition Selection Grid Layout — Design Spec

## Goal

Replace the full-width card list in `CompositionSelectionScreen` with an adaptive grid so horizontal space is used efficiently at any window width.

## Context

`CompositionCard` currently uses `fillMaxWidth()`, rendering each comp as a wide row. On a desktop browser at 1400px this wastes the majority of the screen. The fix is a `LazyVerticalGrid` with adaptive column sizing — Compose calculates column count from available width automatically.

---

## Layout Changes

### CompositionSelectionScreen

- Replace `LazyColumn` with `LazyVerticalGrid(GridCells.Adaptive(minSize = 200.dp))`.
- Grid spacing: `horizontalArrangement = Arrangement.spacedBy(12.dp)`, `verticalArrangement = Arrangement.spacedBy(12.dp)`.
- **Tier headers** use `span = { GridItemSpan(maxLineSpan) }` so they always occupy the full row regardless of column count.
- **DOMINANT / STRONG / PLAYABLE** cards: standard `items(comps)` — grid handles layout automatically.
- **OTHERS section**: replace `AnimatedVisibility` wrapping a nested `Column` with a conditional `if (othersExpanded) { items(comps) { ... } }`. This makes Others cards lazy (not composed until expanded) and eliminates the nested-layout workaround. The toggle header and arrow remain unchanged.

### CompositionCard

- Remove `fillMaxWidth()`. The grid column width drives card width — no manual sizing needed.
- Add `heightIn(min = 72.dp)` so cards have consistent tile presence even when spec badge text wraps.
- Spec badges remain centered horizontally and vertically (`Arrangement.Center`, `Alignment.CenterVertically`).
- `alpha` shimmer and conditional `clickable` behaviour unchanged.

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
- `TierHeader` — unchanged (just given full-span treatment in the grid).
- ViewModel and state — unchanged.
- Navigation behaviour — unchanged.
- `hasData` shimmer and click guard — unchanged.
