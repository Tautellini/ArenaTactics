# Composition Selection Grid Layout — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the full-width card list on the Composition Selection screen with an adaptive `LazyVerticalGrid` so horizontal space is used efficiently at any window width.

**Architecture:** Three focused changes: (1) add adaptive UI guidance to `CLAUDE.md`, (2) make `CompositionCard` a compact tile by removing `fillMaxWidth` and adding a minimum height, (3) replace `LazyColumn` with `LazyVerticalGrid(GridCells.Adaptive(280.dp))` in `CompositionSelectionScreen`, moving the OTHERS expand/collapse from a nested `AnimatedVisibility` block to a lazy conditional `if (othersExpanded) { items(...) }` directly inside the grid scope.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.10.0 (`foundation.lazy.grid.*`).

---

## File Map

| Action | Path |
|--------|------|
| Modify | `CLAUDE.md` |
| Modify | `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/components/CompositionCard.kt` |
| Modify | `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/CompositionSelectionScreen.kt` |

> No new files. No repository or ViewModel changes. No test files change — these are layout-only edits. Run `./gradlew :composeApp:jvmTest` after each task to confirm nothing regressed.

---

### Task 1: Add adaptive UI guidance to CLAUDE.md

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: Add the guidance line**

Open `CLAUDE.md`. Find the `## Additional Code Guidance` section (currently has one bullet about catching Throwable on the web path). Add a second bullet **inside that same section**:

```markdown
## Additional Code Guidance
-  any exception-generating code on the web path (IO, JS interop) needs catch
   (Throwable) and must never be left uncaught in a LaunchedEffect
-  UI must be fully adaptive — use `GridCells.Adaptive`, `FlowRow`, or equivalent so
   layouts reflow naturally across screen widths. Never hardcode column counts or fixed
   widths for list/grid content. Layouts must look correct and visually appealing at both
   narrow (≈800px) and wide (≈1600px) viewports.
```

- [ ] **Step 2: Run tests to confirm nothing broke**

```bash
./gradlew :composeApp:jvmTest
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 3: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: add adaptive UI guidance to CLAUDE.md"
```

---

### Task 2: Update CompositionCard — compact tile

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/components/CompositionCard.kt`

The card currently uses `fillMaxWidth()` which forces every card to span the full screen. In a grid the column width drives card width, so `fillMaxWidth` is wrong. We also add `heightIn(min = 72.dp)` to give cards a consistent tile presence.

- [ ] **Step 1: Replace the file**

Replace the full content of `CompositionCard.kt` with:

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
    onClick: (() -> Unit)?,         // null = hasData:false — card is unselectable
    modifier: Modifier = Modifier
) {
    val hasData = richComposition.composition.hasData
    Surface(
        color = CardColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .heightIn(min = 72.dp)
            .alpha(if (hasData) 1f else 0.35f)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpecBadge(richComposition.specs[0], richComposition.classes[0])
            SpecBadge(richComposition.specs[1], richComposition.classes[1])
        }
    }
}
```

Key changes from the current file:
- `fillMaxWidth()` on the `Surface` → removed (grid column drives width)
- `heightIn(min = 72.dp)` → added on the `Surface`
- Inner `Row` arrangement unchanged (`Arrangement.spacedBy(8.dp)`, `Alignment.CenterVertically`)

- [ ] **Step 2: Run tests**

```bash
./gradlew :composeApp:jvmTest
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/components/CompositionCard.kt
git commit -m "feat: make CompositionCard a compact tile for grid layout"
```

---

### Task 3: Replace LazyColumn with LazyVerticalGrid in CompositionSelectionScreen

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/CompositionSelectionScreen.kt`

This is the main layout change. The `LazyColumn` becomes a `LazyVerticalGrid`. Tier headers get `span = { GridItemSpan(maxLineSpan) }` so they always span full width. The OTHERS section drops `AnimatedVisibility` + nested `Column` in favour of a lazy `if (othersExpanded) { items(comps) }` directly in the grid scope.

**Imports that change:**
- Remove: `import androidx.compose.animation.AnimatedVisibility`
- Remove: `import androidx.compose.foundation.lazy.LazyColumn`
- Remove: `import androidx.compose.foundation.lazy.items`
- Remove: `import net.tautellini.arenatactics.domain.RichComposition` (no longer referenced directly)
- Add: `import androidx.compose.foundation.lazy.grid.GridCells`
- Add: `import androidx.compose.foundation.lazy.grid.GridItemSpan`
- Add: `import androidx.compose.foundation.lazy.grid.LazyVerticalGrid`
- Add: `import androidx.compose.foundation.lazy.grid.items`

- [ ] **Step 1: Replace the file**

Replace the full content of `CompositionSelectionScreen.kt` with:

```kotlin
package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.data.model.CompositionTier
import net.tautellini.arenatactics.navigation.Navigator
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.CompositionSelectionState
import net.tautellini.arenatactics.presentation.CompositionSelectionViewModel
import net.tautellini.arenatactics.presentation.screens.components.BackButton
import net.tautellini.arenatactics.presentation.screens.components.CompositionCard
import net.tautellini.arenatactics.presentation.theme.*

private fun CompositionTier.label() = when (this) {
    CompositionTier.DOMINANT -> "Dominant"
    CompositionTier.STRONG   -> "Strong"
    CompositionTier.PLAYABLE -> "Playable"
    CompositionTier.OTHERS   -> "Others"
}

@Composable
fun CompositionSelectionScreen(
    gameModeId: String,
    viewModel: CompositionSelectionViewModel,
    navigator: Navigator
) {
    val state by viewModel.state.collectAsState()
    var othersExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(Background).padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton { navigator.pop() }
            Spacer(Modifier.width(12.dp))
            Text(
                "Select Composition",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(24.dp))
        when (val s = state) {
            is CompositionSelectionState.Loading ->
                CircularProgressIndicator(color = Accent)
            is CompositionSelectionState.Error ->
                Text(s.message, color = TextSecondary)
            is CompositionSelectionState.Success -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 280.dp),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CompositionTier.entries.forEach { tier ->
                        val comps = s.grouped[tier] ?: return@forEach
                        if (tier == CompositionTier.OTHERS) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                TierHeader(
                                    label = tier.label(),
                                    expandable = true,
                                    expanded = othersExpanded,
                                    onToggle = { othersExpanded = !othersExpanded }
                                )
                            }
                            if (othersExpanded) {
                                items(comps) { rich ->
                                    CompositionCard(
                                        richComposition = rich,
                                        onClick = if (rich.composition.hasData) {
                                            { navigator.push(Screen.GearView(gameModeId, rich.composition.id)) }
                                        } else null
                                    )
                                }
                            }
                        } else {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                TierHeader(label = tier.label())
                            }
                            items(comps) { rich ->
                                CompositionCard(
                                    richComposition = rich,
                                    onClick = if (rich.composition.hasData) {
                                        { navigator.push(Screen.GearView(gameModeId, rich.composition.id)) }
                                    } else null
                                )
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
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (expandable) {
            Text(
                text = if (expanded) "▲" else "▼",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
```

- [ ] **Step 2: Run tests**

```bash
./gradlew :composeApp:jvmTest
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 3: Visual smoke-test**

```bash
./gradlew :composeApp:jsBrowserDevelopmentRun
```

Navigate to the composition selection screen. Verify:
1. Dominant comps appear in a multi-column grid (not one per row)
2. Tier headers (Dominant, Strong, Playable, Others) span the full width
3. Others section is collapsed by default; clicking the header expands it lazily
4. Resizing the browser window reflows columns automatically
5. Disabled comps (Strong/Playable/Others) show at 35% opacity and are not clickable

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/net/tautellini/arenatactics/presentation/screens/CompositionSelectionScreen.kt
git commit -m "feat: adaptive grid layout for composition selection screen"
```
