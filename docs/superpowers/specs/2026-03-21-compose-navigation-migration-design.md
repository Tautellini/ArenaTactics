# Compose Navigation Migration Design

## Goal

Migrate from the custom `Navigator` (StateFlow-based back-stack) to `androidx.navigation:navigation-compose`, adding screen transition animations, shared element transitions, a persistent AppShell with breadcrumb navigation, and correct browser forward/back button support.

## Architecture

`SharedTransitionLayout` wraps `AnimatedNavHost` in `App.kt`. The existing `Screen` sealed class becomes the type-safe route set by adding `@Serializable` annotations. `Navigator.kt` is deleted. All screens receive `NavHostController` instead. `SharedTransitionScope` and `AnimatedContentScope` are threaded into screens that participate in shared element transitions.

**Tech Stack:** Compose Multiplatform 1.10.0, `org.jetbrains.androidx.navigation:navigation-compose` (KMP-specific artifact — NOT `androidx.navigation:navigation-compose`), `androidx.compose.animation` shared elements API.

**Dependency artifact:** `org.jetbrains.androidx.navigation:navigation-compose`. The correct version compatible with CMP 1.10.0 must be verified at implementation time against the [JetBrains Compose Multiplatform compatibility table](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-compatibility-and-versioning.html). Add to `gradle/libs.versions.toml` and `composeApp/build.gradle.kts` commonMain dependencies.

---

## Section 1: Routes

`Screen` sealed class gains `@Serializable` on the sealed class itself and each subtype. No structural changes — existing `path`, `fromPath`, and `buildStack` companions are retained for the web URL bridge.

`Navigator.kt` is deleted. `BackButton.kt` is deleted. All call sites updated:
- `navigator.push(Screen.X)` → `navController.navigate(Screen.X)`
- `navigator.pop()` → `navController.popBackStack()`
- `navigator` parameter removed from all screen composables; `navController: NavHostController` added where needed

Type-safe route extraction uses `navBackStackEntry.toRoute<Screen.X>()` (the type-safe API from `navigation-compose`). Each `composable<Screen.X>` receives the typed route directly from the lambda parameter, no manual argument extraction needed.

### GearView and MatchupList routing

`Screen.GearView` and `Screen.MatchupList` are **separate `composable {}` blocks** in the `NavHost`. Both render `CompositionHubScreen` but pass an `initialTab` parameter (enum: `GEAR` or `MATCHUPS`) so the correct tab is active on entry. Deep-linking to `/gear` opens the Gear tab; deep-linking to `/matchups` opens the Matchup tab.

### Initial deep-link stack restoration

On startup, `App.kt` calls `Screen.buildStack(initialScreen)` (already implemented) to get the full logical back-stack, then uses `navController.navigate()` for each screen in the stack in order, so that pressing Back after a deep link navigates correctly through the parent screens rather than jumping straight to root.

```kotlin
val initialScreen = Screen.fromPath(getInitialPath())
val initialStack = Screen.buildStack(initialScreen)
// Navigate each entry in the stack so back-stack is fully populated
LaunchedEffect(Unit) {
    initialStack.drop(1).forEach { screen ->
        navController.navigate(screen) { launchSingleTop = true }
    }
}
```

---

## Section 2: Screen Transitions

Applied globally at `AnimatedNavHost` level — no per-screen override needed:

```kotlin
enterTransition    = { fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.95f) }
exitTransition     = { fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 1.05f) }
popEnterTransition = { fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 1.05f) }
popExitTransition  = { fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.95f) }
```

Push: incoming screen scales up 0.95→1.0 while fading in; outgoing scales up to 1.05 while fading out (receding).
Pop: reverse — incoming scales down 1.05→1.0 (returning), outgoing scales down to 0.95 while fading out.

---

## Section 3: AppShell + Shared Shield

### AppHeader component (`presentation/screens/components/AppHeader.kt`)

Shown on all screens **except** `GameModeSelection`. Contains:

1. **Small shield** (44×50dp Canvas, same path as home shield) — participates in `sharedElement("shield", animatedVisibilityScope)`
2. **Breadcrumb row** — chips built by calling `Screen.buildStack(currentScreen)` on the current destination, separated by `Icons.Rounded.ChevronRight`
   - Non-last chips: clickable, call `navController.popBackStack(destination, inclusive = false)` using the appropriate `Screen` type as destination
   - Last chip: current screen, non-clickable, `TextPrimary` color
   - Chip labels (see below)

**Why `Screen.buildStack()` for breadcrumbs:** `NavController.backQueue` is not a stable public API. Instead, reconstruct the logical breadcrumb list from `Screen.buildStack(currentScreen)` — this is deterministic since navigation is always hierarchical and `buildStack` already encodes the full parent chain.

**Breadcrumb labels per screen:**
| Screen | Label |
|---|---|
| `GameModeSelection` | "Select Mode" |
| `CompositionSelection` | "Select Composition" |
| `GearView` / `MatchupList` | composition display name (e.g. "Frost Mage / Sub Rogue") |
| `MatchupDetail` | enemy comp name |

`GearView`/`MatchupList` and `MatchupDetail` labels require the composition display name. `AppHeader` accepts an optional `label: String?` parameter for the current screen's dynamic label. This label is resolved by the screen's own ViewModel and passed up to `AppHeader` — no separate `AppHeaderViewModel` needed. `CompositionHubScreen` passes `richComposition.displayName`; `MatchupDetailScreen` passes the enemy comp label.

### Shield shared element

`ShieldLogo` is **moved from `GameModeSelectionScreen.kt` into a new file `components/ShieldLogo.kt`** so both `GameModeSelectionScreen` and `AppHeader` can use it. Visibility becomes `internal`. It gains a `modifier: Modifier = Modifier` parameter:

```kotlin
// components/ShieldLogo.kt
@Composable
internal fun ShieldLogo(modifier: Modifier = Modifier) {
    // Canvas(modifier = modifier.size(220.dp, 250.dp)) { ... }
}
```

In the `composable<Screen.GameModeSelection>` block, thread in the shared element modifier:
```kotlin
GameModeSelectionScreen(
    vm, navController,
    shieldModifier = Modifier.sharedElement(
        rememberSharedContentState("shield"), animatedVisibilityScope
    )
)
```

`AppHeader` calls `ShieldLogo(modifier = Modifier.size(44.dp, 50.dp).sharedElement(...))` with the same key. Compose interpolates position, size, and shape during navigation transitions.

### Removal of back buttons

`BackButton.kt` deleted. All explicit back button + surrounding header rows removed from:
- `CompositionSelectionScreen`
- `CompositionHubScreen` (GearScreen)
- `MatchupDetailScreen`

---

## Section 4: Shared Elements Inventory

| Element | From | To | Key |
|---|---|---|---|
| Shield logo | `GameModeSelectionScreen` (220×250dp centered) | `AppHeader` (44×50dp top-left) | `"shield"` |
| Spec badge icons | `CompositionCard` (each badge) | `CompositionHubScreen` header | `"spec_badge_{specId}"` |

**Note on spec badge indices:** `CompositionCard` currently accesses `richComposition.specs[0]` and `[1]` with hardcoded indices. Shared element key generation must iterate all specs dynamically (e.g. `specs.forEachIndexed`) rather than using fixed indices, both for correctness and forward-compatibility with 3v3/5v5 comps.

The tier chip shared element (CompositionCard → hub header) is **deferred** — only add during implementation if it looks natural.

---

## Section 5: Web URL Bridge

Add a new platform expect function `getCurrentPath(): String` (alongside existing `getInitialPath()`). JS/Wasm `actual` implementation: `return window.location.pathname`. Desktop `actual`: `return "/"`.

```kotlin
// In App.kt

// NavController → browser history (skip initial navigation to avoid duplicate entry)
var isRestoringFromBrowser = false

LaunchedEffect(navController) {
    navController.currentBackStackEntryFlow
        .drop(initialStack.size - 1)  // skip entries we pushed during stack restoration
        .collect { entry ->
            if (!isRestoringFromBrowser) {
                pushNavigationState(entry.toScreen().path)  // see toScreen() below
            }
            isRestoringFromBrowser = false
        }
}

// Browser back/forward → NavController
DisposableEffect(navController) {
    registerPopCallback {
        isRestoringFromBrowser = true
        val screen = Screen.fromPath(getCurrentPath())
        val stack = Screen.buildStack(screen)
        // Pop to root then re-push the full stack to match browser state
        navController.navigate(Screen.GameModeSelection) {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
        }
        stack.drop(1).forEach { s ->
            navController.navigate(s) { launchSingleTop = true }
        }
    }
    onDispose { registerPopCallback {} }
}
```

Forward button works because `popstate` fires for both back and forward, and we reconstruct from `window.location.pathname` rather than blindly popping.

### `NavBackStackEntry.toScreen()` extension

Add to `Screen.kt` (or a new `NavExt.kt` alongside it):

```kotlin
fun NavBackStackEntry.toScreen(): Screen = when {
    destination.hasRoute<Screen.GameModeSelection>() -> Screen.GameModeSelection
    destination.hasRoute<Screen.CompositionSelection>() ->
        toRoute<Screen.CompositionSelection>().let { Screen.CompositionSelection(it.gameModeId) }
    destination.hasRoute<Screen.GearView>() ->
        toRoute<Screen.GearView>().let { Screen.GearView(it.gameModeId, it.compositionId) }
    destination.hasRoute<Screen.MatchupList>() ->
        toRoute<Screen.MatchupList>().let { Screen.MatchupList(it.gameModeId, it.compositionId) }
    destination.hasRoute<Screen.MatchupDetail>() ->
        toRoute<Screen.MatchupDetail>().let { Screen.MatchupDetail(it.gameModeId, it.compositionId, it.matchupId) }
    else -> Screen.GameModeSelection
}
```

This extension lives in `Screen.kt` and is the only place that reads route args from a `NavBackStackEntry`.

---

## Files Changed

| Action | File |
|---|---|
| Modify | `gradle/libs.versions.toml` — add `navigation-compose` version alias |
| Modify | `composeApp/build.gradle.kts` — add `navigation-compose` to commonMain |
| Modify | `Screen.kt` — add `@Serializable` to sealed class and all subtypes |
| **Delete** | `Navigator.kt` |
| **Delete** | `components/BackButton.kt` |
| **Delete** | `NavigatorTest.kt` (or migrate `Screen.fromPath`/`buildStack` tests to `RepositoryParsingTest`) |
| Modify | `Platform.kt` + all platform actuals — add `getCurrentPath(): String` expect/actual |
| Modify | `App.kt` — `SharedTransitionLayout` + `AnimatedNavHost` + URL bridge + stack restoration |
| **Create** | `components/AppHeader.kt` — small shield + breadcrumbs |
| **Create** | `components/ShieldLogo.kt` — extracted from `GameModeSelectionScreen`, visibility `internal`, gains `modifier: Modifier` param |
| Modify | `GameModeSelectionScreen.kt` — remove `ShieldLogo` (moved to `ShieldLogo.kt`), receives `shieldModifier`, `Navigator` → `NavHostController` |
| Modify | `CompositionSelectionScreen.kt` — remove back button row, `Navigator` → `NavHostController`, spec badges get `sharedElement` |
| Modify | `GearScreen.kt` — remove back button row, `Navigator` → `NavHostController`, spec badges in header get `sharedElement`, add `initialTab` param, passes dynamic label to `AppHeader` |
| Modify | `MatchupDetailScreen.kt` — remove back button row, `Navigator` → `NavHostController`, passes dynamic label to `AppHeader` |
| Modify | `MatchupListScreen.kt` — `Navigator` → `NavHostController` |

---

## Design Guidelines Note

All new screens and components that introduce a navigable element should consider shared element transitions. Specifically:
- Any icon or visual that appears on both a list screen and a detail screen is a candidate for `sharedElement()`
- Use the key convention `"{elementType}_{uniqueId}"` (e.g. `"spec_badge_rogue_subtlety"`)
- `SharedTransitionScope` and `AnimatedContentScope` must be threaded in from the `composable {}` block in `NavHost`
- Shared element modifiers must be applied via a `modifier: Modifier = Modifier` parameter — never hardcode inside a `private` composable
