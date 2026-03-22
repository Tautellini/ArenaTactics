# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

**Desktop (JVM):**
```bash
./gradlew :composeApp:run
```

**Web (Wasm — modern browsers, faster):**
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

**Web (JS — wider browser support):**
```bash
./gradlew :composeApp:jsBrowserDevelopmentRun
```

**Run tests:**
```bash
./gradlew :composeApp:allTests
```

**Run a single test class:**
```bash
./gradlew :composeApp:jvmTest --tests "net.tautellini.arenatactics.ComposeAppCommonTest"
```

**Build distribution packages (DMG/MSI/DEB):**
```bash
./gradlew :composeApp:packageDistributionForCurrentOS
```

On Windows use `gradlew.bat` instead of `./gradlew`.

## Architecture

This is a **Kotlin Multiplatform** app using **Compose Multiplatform** targeting Desktop (JVM), Web (JS), and Web (Wasm). Single module: `composeApp`.

**Source sets under `composeApp/src/`:**
- `commonMain` — shared UI and business logic (all platforms)
- `commonTest` — shared tests
- `jvmMain` — desktop entry point (`main.kt`), JVM-specific platform impl
- `webMain` — web entry point (`main.kt`), shared between JS and Wasm
- `jsMain` / `wasmJsMain` — platform-specific implementations for JS and Wasm targets

**Platform abstraction pattern:** `Platform.kt` in `commonMain` declares `expect fun getPlatform(): Platform`. Each platform source set provides an `actual` implementation in its own `Platform.*.kt` file.

**Main entry points:**
- Desktop: `net.tautellini.arenatactics.MainKt` in `jvmMain/main.kt` — uses Compose `application { Window(...) }`
- Web: `webMain/main.kt` — uses `ComposeViewport(document.body!!)`

**Dependency versions** are managed via the version catalog at `gradle/libs.versions.toml`. Key versions: Kotlin 2.3.0, Compose Multiplatform 1.10.0, Material3 1.10.0-alpha05, Coroutines 1.10.2.

- MVVM
- Repositories for data layer access
- API layers to abstract between Repositories and remote calls
- Compose Multiplatform for a shared UI
- Compose Navigation

## DESIGN GUIDELINES
- modern design with glass-like elements
- **premium feel**
- modern icon first design
- Background color: #042326
- Foreground color: #0A3A40
- Primary color: #1D7373
- Secondary color: #0F5959

## Wowhead / Zamimg Icons
- Icons for specs, classes, items, and game modes are loaded from `wow.zamimg.com/images/wow/icons/{size}/{iconName}.jpg` via the `WowheadIcons` helper.
- Sizes: `medium` (36px) and `large` (56px). Use `WowheadIcons.large(name)` for tile-sized images.
- Icon names follow WoW's internal naming (e.g., `classicon_rogue`, `ability_stealth`, `achievement_arena_2v2_7`).
- To verify an icon exists: `curl -s -o /dev/null -w "%{http_code}" "https://wow.zamimg.com/images/wow/icons/large/{name}.jpg"` — 200 means it exists, 404 means it doesn't.
- Wowhead item tooltips can be integrated on the web target by loading `https://wow.zamimg.com/js/tooltips.js` and using `data-wowhead="item={wowheadId}"` attributes on anchor elements. This is **JS/Wasm web only** — desktop has no tooltip integration.
- Addon tiles use custom "W" emblems with per-addon accent colors instead of Wowhead icons. Accent colors are defined in `addons.json` as hex strings.

## Additional Code Guidance
-  any exception-generating code on the web path (IO, JS interop) needs catch
   (Throwable) and must never be left uncaught in a LaunchedEffect
-  UI must be fully adaptive — use `GridCells.Adaptive`, `FlowRow`, or equivalent so
   layouts reflow naturally across screen widths. Never hardcode column counts or fixed
   widths for list/grid content. Layouts must look correct and visually appealing at both
   narrow (≈800px) and wide (≈1600px) viewports.
- do **not** place all Models in a singular file, separate and extract them

## Multi-Bracket & Multi-Addon Scope
- The app is designed to support **multiple game modes**: 2v2, 3v3, 5v5, and potentially other addons (e.g., Wrath, Retail).
- **Data is currently only curated for TBC Anniversary 2v2** — this is a content scope decision, not an architectural one.
- All core models (`Composition`, `Matchup`) must use **`List<String>` for spec slots**, never hardcoded `spec1Id`/`spec2Id` pairs. This is what allows the same model to represent a 2-spec comp and a 5-spec comp without changes.
- `GameMode` should carry a `teamSize: Int` so repositories and UI can validate and render compositions correctly for any bracket.

## Addon Lifecycle & "Retail"
- **Retail** is always the currently active expansion on WoW's live servers. Right now that is "The War Within — Midnight" (addon ID `midnight`). It is displayed as "RETAIL" in the UI, not by its expansion name.
- When the next retail expansion launches, the `midnight` addon entry keeps its curated data (compositions, matchups, gear, ladder) but gets `hasData: false` — a new addon entry is created for the new expansion and becomes the active "Retail" entry. The old data is preserved because Blizzard may introduce history servers for it later.
- **Anniversary servers** (e.g., TBC Anniversary) are separate Classic-era servers running older expansion states. Blizzard rotates these forward over time — TBC Anniversary will eventually become WotLK Anniversary, etc. When that happens, the old addon entry is deactivated (`hasData: false`) and a new one is created. **Never delete curated data** for a rotated-out addon — it may return as a history server.
- There are **no history servers for modern expansions yet** (e.g., no "The War Within" server after Midnight launches). This may change in the future, which is why we always preserve data.
- An addon is **selectable on the home screen** if it has any data at all (tactics, guides, or ladder). Each section tile (Tactics, Class Guides, Ladder) is individually enabled/disabled based on what data exists for that addon.
## Ladder Data Pipeline
- PvP ladder data is fetched via `scripts/fetch_ladder.py` from the Blizzard Game Data API.
- The script reads credentials from `secrets.properties` (gitignored) or environment variables (`BLIZZARD_CLIENT_ID`, `BLIZZARD_CLIENT_SECRET`).
- Data is organized per-addon: `files/ladder/{addonId}/index.json` + `{region}_{bracket}.json`.
- A GitHub Actions workflow (`.github/workflows/fetch-ladder.yml`) runs daily to refresh data.
- Addon-to-API-namespace mapping is defined in the `ADDONS` list inside the script. Key namespaces: `dynamic-classicann-{region}` for TBC Anniversary, `dynamic-{region}` for retail.

## Spec Ordering
- `specIds` in JSON data files are always **alphabetically sorted** — this is enforced by `Composition.init` and is required for stable IDs and deduplication.
- `RichComposition.specs` (the display layer) are always reordered **DPS first, HEALER last** in `enrichCompositions()`. Never change this order at the UI layer; fix it at the enrichment layer if it is wrong.
- When adding new compositions to JSON, keep `specIds` alphabetical. The display order is handled automatically.