# Paper Doll Gear Screen — Design Spec
**Date:** 2026-03-18
**Project:** ArenaTactics — Gear Screen Redesign
**Scope:** Paper doll layout, item icons via Coil 3, Wowhead tooltip DOM integration

---

## Overview

Replace the existing flat gear list with a WoW character screen–style paper doll layout. Two paper dolls sit side by side (one per class in the composition), wrapping to a single column on narrow screens. Phase tabs switch both dolls simultaneously between Phase 1 and Phase 2 gear. Item icons are loaded from Wowhead's CDN via Coil 3. Hovering a slot shows Wowhead's native tooltip via a cursor-following DOM overlay injected over the Compose Canvas.

---

## Architecture

Three independent concerns:

1. **Data model** — `GearItem` gains an `icon: String` field; all 4 gear JSON files are updated with correct Wowhead icon names.
2. **Gear Screen visual layer** — `GearScreen.kt` fully replaced with `PaperDollScreen`, `PaperDoll`, and `GearSlot` composables.
3. **Wowhead tooltip interop** — Two new `expect/actual` functions; `tooltips.js` injected in `webMain/main.kt`.

New dependency: **Coil 3** (`coil-compose` + `coil-network-ktor3`).

---

## Data Model

### `GearItem` change

```kotlin
@Serializable
data class GearItem(
    val wowheadId: Int,
    val name: String,
    val slot: String,
    val icon: String,          // Wowhead icon name e.g. "inv_helmet_09"
    val enchant: String? = null,
    val gems: List<String> = emptyList()
)
```

### Gear JSON files updated

All 4 files (`gear_rogue_phase1.json`, `gear_rogue_phase2.json`, `gear_mage_phase1.json`, `gear_mage_phase2.json`) gain an `"icon"` field per item entry with the correct Wowhead icon name (e.g. `"inv_helmet_09"`, `"inv_sword_27"`, etc.).

### Slot ordering (paper doll grid)

| Left column | Right column | Bottom row |
|---|---|---|
| Head | Hands | Trinket |
| Neck | Waist | Trinket |
| Shoulders | *(class icon center)* | Main Hand |
| Back | Legs | Off Hand |
| Chest | Feet | Ranged / Wand |
| Wrists | Ring | |
| | Ring | |

Icon URLs:
- **Item icons:** `https://wow.zamimg.com/images/wow/icons/medium/{icon}.jpg`
- **Class icons:** `https://wow.zamimg.com/images/wow/icons/large/classicon_{classId}.jpg`
- **Empty slot placeholder:** fallback `Box` with `CardElevated` background + `?` Text

---

## Gear Screen Visual Layer

### File structure

Replace `GearScreen.kt` content. New composables (all in `GearScreen.kt`):

| Composable | Responsibility |
|---|---|
| `GearTabContent(viewModel)` | Phase tab switcher + `FlowRow` of two `PaperDoll`s |
| `PaperDoll(classId, className, phases, selectedPhase)` | Single class paper doll with slot grid |
| `GearSlot(item)` | One equipment slot — icon, name, hover/click behavior |
| `EmptyGearSlot(slotName)` | Placeholder for slots with no item in this phase |

### Phase tab switcher

Single `selectedPhase: Int` state in `GearTabContent`, shared across both `PaperDoll` composables. Renders tabs like the existing `CompositionTab` pattern (Phase 1 / Phase 2).

### Responsive layout

```kotlin
FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(16.dp)
) {
    PaperDoll(modifier = Modifier.weight(1f).widthIn(min = 280.dp), ...)
    PaperDoll(modifier = Modifier.weight(1f).widthIn(min = 280.dp), ...)
}
```
Side by side on wide screens; stacks to single column when width < ~560dp.

### `PaperDoll` layout

Uses a `Row` of three zones:
1. **Left column** — `Column` of 6 `GearSlot`/`EmptyGearSlot`
2. **Center** — `Column` containing class icon (72×72dp with class-color border) + class name
3. **Right column + bottom** — `Column` of 6 slots, then a `Row` of 5 bottom slots

### `GearSlot`

```
┌──────────┐
│  [icon]  │  48×48dp AsyncImage, corner radius 8dp
│  name    │  11sp, Accent color, maxLines = 1, overflow = Ellipsis
│ ✦ enchant│  10sp, TextSecondary (if present)
└──────────┘
```

- Fixed 80dp wide, auto height
- `CardColor` background, 8dp corner radius
- `pointerInput` for hover → `showWowheadTooltip` / `hideWowheadTooltip`
- `clickable` → `openUrl("https://www.wowhead.com/tbc/item=${item.wowheadId}")`

---

## Image Loading (Coil 3)

### Dependencies

**`gradle/libs.versions.toml`:**
```toml
[versions]
coil3 = "3.1.0"

[libraries]
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil3" }
coil-network-ktor = { module = "io.coil-kt.coil3:coil-network-ktor3", version.ref = "coil3" }
```

Both added to `commonMain.dependencies` in `composeApp/build.gradle.kts`. Coil 3's `ktor3` engine bundles its own Ktor; no separate Ktor entry needed unless the build reports a missing dependency.

### Usage

**Item icons:**
```kotlin
AsyncImage(
    model = "https://wow.zamimg.com/images/wow/icons/medium/${item.icon}.jpg",
    contentDescription = item.name,
    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)),
    error = { EmptyIconPlaceholder() }
)
```

**Class icons:**
```kotlin
AsyncImage(
    model = "https://wow.zamimg.com/images/wow/icons/large/classicon_${classId}.jpg",
    contentDescription = className,
    modifier = Modifier.size(72.dp)
        .clip(RoundedCornerShape(8.dp))
        .border(2.dp, classColor(classId), RoundedCornerShape(8.dp))
)
```

**Error fallback:** `EmptyIconPlaceholder` — a `Box(CardElevated bg)` with centered `?` Text in `TextSecondary`.

---

## Wowhead Tooltip DOM Integration (Approach B — Cursor Following)

### New `expect/actual` declarations

**`commonMain/Platform.kt`** — two new functions added:
```kotlin
expect fun showWowheadTooltip(itemId: Int, cursorX: Float, cursorY: Float)
expect fun hideWowheadTooltip()
```

### Web actuals (`jsMain` + `wasmJsMain`)

Lazy-create one shared `<a id="wh-tt">` element appended to `document.body` on first call.

**`showWowheadTooltip`:**
1. Set `href="https://www.wowhead.com/tbc/item={itemId}"`, `data-wowhead="item={itemId}&domain=tbc"`
2. Position: `style="position:fixed; left:{x}px; top:{y}px; width:1px; height:1px; opacity:0; pointer-events:none;"`
3. Call `WH?.refreshLinks()` to process the element
4. Dispatch synthetic `mouseover` event on the element

**`hideWowheadTooltip`:**
1. Dispatch `mouseout` on the element

**JVM actuals:** both no-ops.

### `webMain/main.kt` script injection

Replaces the previously removed `power.js` with the correct current Wowhead script:

```kotlin
// Config — disable auto-scanning; we only want programmatic tooltip display
val cfg = document.createElement("script")
cfg.textContent = """const whTooltips = {colorLinks:false, iconizeLinks:false, renameLinks:false};"""
document.head!!.appendChild(cfg)

// tooltips.js
val script = document.createElement("script")
script.setAttribute("src", "https://wow.zamimg.com/js/tooltips.js")
document.head!!.appendChild(script)
```

### `GearSlot` pointer input

```kotlin
Modifier.pointerInput(item.wowheadId) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            val pos = event.changes.firstOrNull()?.position
            when (event.type) {
                PointerEventType.Enter ->
                    pos?.let { showWowheadTooltip(item.wowheadId, it.x, it.y) }
                PointerEventType.Exit ->
                    hideWowheadTooltip()
            }
        }
    }
}
```

---

## Files Changed

| File | Change |
|---|---|
| `gradle/libs.versions.toml` | Add coil3 version + 2 library entries |
| `composeApp/build.gradle.kts` | Add coil dependencies to commonMain |
| `data/model/Models.kt` | Add `icon: String` to `GearItem` |
| `data/repository/GearRepository.kt` | No change (parses new field automatically) |
| `composeResources/files/gear/*.json` | Add `"icon"` field to all 68 item entries |
| `Platform.kt` | Add 2 new expect declarations |
| `Platform.jvm.kt` | Add 2 no-op actuals |
| `Platform.js.kt` | Add 2 web actuals (shared DOM element) |
| `Platform.wasmJs.kt` | Add 2 web actuals (shared DOM element) |
| `presentation/screens/GearScreen.kt` | Full rewrite — PaperDollScreen, PaperDoll, GearSlot |
| `webMain/main.kt` | Inject whTooltips config + tooltips.js |

---

## Out of Scope

- Enchantment icons (enchant is shown as text only)
- Empty slot placeholder art from WoW (using simple Box fallback)
- Approach A (per-slot `onGloballyPositioned` overlay) — deferred
- Matchup screen changes
