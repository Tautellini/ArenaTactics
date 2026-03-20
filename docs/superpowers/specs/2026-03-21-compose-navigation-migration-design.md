# Compose Navigation Migration Design

## Goal

Migrate from the custom `Navigator` (StateFlow-based back-stack) to `androidx.navigation:navigation-compose`, adding screen transition animations, shared element transitions, a persistent AppShell with breadcrumb navigation, and correct browser forward/back button support.

## Architecture

`SharedTransitionLayout` wraps `AnimatedNavHost` in `App.kt`. The existing `Screen` sealed class becomes the type-safe route set by adding `@Serializable` annotations. `Navigator.kt` is deleted. All screens receive `NavHostController` instead. `SharedTransitionScope` and `AnimatedContentScope` are threaded into screens that participate in shared element transitions.

**Tech Stack:** Compose Multiplatform 1.10.0, `androidx.navigation:navigation-compose` (KMP), `androidx.compose.animation` shared elements API.

---

## Section 1: Routes

`Screen` sealed class gains `@Serializable` on each subtype. No structural changes — existing `path`, `fromPath`, and `buildStack` companions are retained for the web URL bridge.

`Navigator.kt` is deleted. `BackButton.kt` is deleted. All call sites updated:
- `navigator.push(Screen.X)` → `navController.navigate(Screen.X)`
- `navigator.pop()` → `navController.popBackStack()`
- `navigator` parameter removed from all screen composables; `navController: NavHostController` added where needed

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
2. **Breadcrumb row** — chips built from the NavController back-stack, separated by `Icons.Rounded.ChevronRight`
   - Non-last chips: clickable, call `navController.popBackStack(destination, inclusive = false)`
   - Last chip: current screen, non-clickable, `TextPrimary` color
   - Chip labels (see below)

**Breadcrumb labels per screen:**
| Screen | Label |
|---|---|
| `GameModeSelection` | "Select Mode" |
| `CompositionSelection` | "Select Composition" |
| `GearView` / `MatchupList` | composition display name (e.g. "Frost Mage / Sub Rogue") |
| `MatchupDetail` | enemy comp name |

`GearView`/`MatchupList` and `MatchupDetail` labels require loading the composition name from the repository. These are loaded via a lightweight `AppHeaderViewModel` that takes `gameModeId` + `compositionId` + optional `matchupId` and exposes a single `StateFlow<String>` label.

### Shield shared element

In `GameModeSelectionScreen`, the `ShieldLogo` Canvas gets:
```kotlin
Modifier.sharedElement(
    state = rememberSharedContentState("shield"),
    animatedVisibilityScope = animatedVisibilityScope
)
```

In `AppHeader`, the small shield Canvas gets the same key. Compose interpolates position, size, and shape during navigation transitions.

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

The tier chip shared element (CompositionCard → hub header) is **deferred** — only add during implementation if it looks natural.

---

## Section 5: Web URL Bridge

```kotlin
// In App.kt

// NavController → browser history
LaunchedEffect(navController) {
    navController.currentBackStackEntryFlow.collect { entry ->
        val screen = entry.toScreen()
        pushNavigationState(screen.path)
    }
}

// Browser back/forward → NavController
DisposableEffect(navController) {
    registerPopCallback {
        val screen = Screen.fromPath(getCurrentPath())
        navController.navigate(screen) {
            popUpTo(navController.graph.startDestinationId)
            launchSingleTop = true
        }
    }
    onDispose { registerPopCallback {} }
}
```

`entry.toScreen()` is a small extension on `NavBackStackEntry` that reads typed route args and constructs the `Screen` instance. Forward button works because `popstate` fires for both directions and we reconstruct from `window.location.pathname` rather than blindly popping.

---

## Files Changed

| Action | File |
|---|---|
| Modify | `composeApp/build.gradle.kts` — add `navigation-compose` dependency |
| Modify | `Screen.kt` — add `@Serializable` to each subtype |
| **Delete** | `Navigator.kt` |
| **Delete** | `components/BackButton.kt` |
| Modify | `App.kt` — `SharedTransitionLayout` + `AnimatedNavHost` + URL bridge |
| **Create** | `components/AppHeader.kt` — small shield + breadcrumbs |
| **Create** | `AppHeaderViewModel.kt` — label resolution |
| Modify | `GameModeSelectionScreen.kt` — shield gets `sharedElement` modifier, remove `Navigator` param |
| Modify | `CompositionSelectionScreen.kt` — remove back button row, `Navigator` → `NavHostController`, spec badges get `sharedElement` |
| Modify | `GearScreen.kt` — remove back button row, `Navigator` → `NavHostController`, spec badges in header get `sharedElement` |
| Modify | `MatchupDetailScreen.kt` — remove back button row, `Navigator` → `NavHostController` |
| Modify | `MatchupListScreen.kt` — `Navigator` → `NavHostController` |

---

## Design Guidelines Note

All new screens and components that introduce a navigable element should consider shared element transitions. Specifically:
- Any icon or visual that appears on both a list screen and a detail screen is a candidate for `sharedElement()`
- Use the key convention `"{elementType}_{uniqueId}"` (e.g. `"spec_badge_rogue_subtlety"`)
- `SharedTransitionScope` and `AnimatedContentScope` must be threaded in from the `composable {}` block in `NavHost`
