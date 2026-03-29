# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

**Desktop (JVM):**
```bash
./gradlew :arenatactics:run
```

**Web (Wasm — modern browsers, faster):**
```bash
./gradlew :arenatactics:wasmJsBrowserDevelopmentRun
```

**Web (JS — wider browser support):**
```bash
./gradlew :arenatactics:jsBrowserDevelopmentRun
```

**Run tests:**
```bash
./gradlew :arenatactics:allTests
```

**Run a single test class:**
```bash
./gradlew :arenatactics:jvmTest --tests "net.tautellini.arenatactics.ComposeAppCommonTest"
```

**Build distribution packages (DMG/MSI/DEB):**
```bash
./gradlew :arenatactics:packageDistributionForCurrentOS
```

On Windows use `gradlew.bat` instead of `./gradlew`.

## Architecture

This is a **Kotlin Multiplatform** app using **Compose Multiplatform** targeting Desktop (JVM), Web (JS), and Web (Wasm). Modules: `arenatactics` (frontend), `backend` (Ktor API), `models` (shared data classes).

**Source sets under `arenatactics/src/`:**
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

## Backend & Data Layer

**Infrastructure:**
- **Cloud Run** backend (Ktor + Netty) at `https://backend-532845578761.europe-west3.run.app`
- **Firestore** (named database `tautellini`, region `europe-west3`) as the single source of truth for all data
- **Artifact Registry** (`tautellini`, `europe-west3`) for Docker images
- GCP project: `tautellini`

**Firestore namespace:** All data lives under `projects/arenatactics/` as a top-level document scope. This allows future projects to coexist in the same database (e.g., `projects/project2/`).

**Module structure:**
- `:models` — shared KMP module (`net.tautellini.models.arenatactics`) with all `@Serializable` data classes. Used by both frontend and backend. **Never duplicate model classes** — always add them here.
- `:backend` — Ktor server. Plugins (CORS, auth, compression, rate limiting) are in `backend/.../plugins/`. ArenaTactics-specific routes and services are under `backend/.../arenatactics/`.
- `:arenatactics` — KMP frontend. Repositories call the backend API via `ArenaApi`, not local files. Only `TalentTreeRepository` still uses local resources.

**Data flow:**
```
Frontend Repositories → ArenaApi (ktor-client) → Backend Routes → FirestoreService → Firestore
```

**API endpoints:** All read endpoints are under `GET /api/v1/arena-tactics/` with rate limiting (60 req/min per IP). Key endpoints:
- `/addons`, `/game-modes`, `/spec-pools/{id}`, `/class-pools/{id}`
- `/composition-sets/{id}`, `/matchups/{compositionId}`, `/gear/{classId}`
- `/ladder/{addonId}/index`, `/ladder/{addonId}/snapshots/{region}/{bracket}`
- `/ladder/{addonId}/players/{region}/{characterId}`, `/ladder/{addonId}/items/{itemId}`
- `/spec-meta/{specId}` — precomputed aggregation, not computed at runtime

**Admin write endpoints:** All under `PUT /api/v1/admin/arena-tactics/` with bearer token auth (`ADMIN_API_KEY` env var) and stricter rate limiting (10 req/min). Used to upsert data during development:
```bash
curl -X PUT .../api/v1/admin/arena-tactics/addons \
  -H "Authorization: Bearer $ADMIN_API_KEY" \
  -H "Content-Type: application/json" \
  -d @addons.json
```
The admin key is stored in `secrets.properties` (gitignored) as `ADMIN_API_KEY`.

**Updating data during development:**
- **Firestore is the source of truth.** Do not create local JSON data files.
- Use the admin PUT endpoints to upsert data. All endpoints accept the same JSON format as the model classes.
- For bulk updates, POST the full list/map. For individual documents, use the specific endpoint.
- `SpecMeta` is precomputed from player profiles — push it via `PUT /admin/arena-tactics/spec-meta/{specId}`.
- Talent trees are the **only** data still embedded as local resources (`arenatactics/src/.../files/talent_trees/`).

**Deployment:**
- Backend deploys automatically via GitHub Actions when `backend/**` changes are pushed to main.
- Frontend deploys to GitHub Pages when `arenatactics/**` changes are pushed to main.
- Both workflows use path filters — changes to one don't trigger the other.
- Docker base image must be `eclipse-temurin:21-jre-noble` (NOT Alpine — gRPC native SSL crashes on musl).

## DESIGN GUIDELINES
- dark, premium design with glass-like translucent elements
- **teal/aqua palette** — deep ocean darks with cyan accents
- modern icon first design
- **Always use theme color tokens** (`Primary`, `TextPrimary`, `TextSecondary`, `CardColor`,
  `CardBorder`, `Background`, etc.) from `Theme.kt`. Never hardcode `Color.White`, `Color.Black`, or hex
  literals for UI elements that should follow the theme. The only exceptions are WoW-standard
  colors (class colors, item quality colors, faction colors) which are universal constants.

## Wowhead / Zamimg Icons
- Icons for specs, classes, items, and game modes are loaded from `wow.zamimg.com/images/wow/icons/{size}/{iconName}.jpg` via the `WowheadIcons` helper.
- Sizes: `medium` (36px) and `large` (56px). Use `WowheadIcons.large(name)` for tile-sized images.
- Icon names follow WoW's internal naming (e.g., `classicon_rogue`, `ability_stealth`, `achievement_arena_2v2_7`).
- To verify an icon exists: `curl -s -o /dev/null -w "%{http_code}" "https://wow.zamimg.com/images/wow/icons/large/{name}.jpg"` — 200 means it exists, 404 means it doesn't.
- Wowhead item tooltips can be integrated on the web target by loading `https://wow.zamimg.com/js/tooltips.js` and using `data-wowhead="item={wowheadId}"` attributes on anchor elements. This is **JS/Wasm web only** — desktop has no tooltip integration.
- Addon tiles use custom "W" emblems with per-addon accent colors instead of Wowhead icons. Accent colors are defined in `addons.json` as hex strings.

## Additional Code Guidance
-  **Never use Unicode text characters as icons** (e.g., `"▼"`, `"▲"`, `"✕"`). They render
   inconsistently across platforms. Always use Material Icons (`Icons.Rounded.*`) from
   `androidx.compose.material.icons` or Wowhead icon images instead.
-  any exception-generating code on the web path (IO, JS interop) needs catch
   (Throwable) and must never be left uncaught in a LaunchedEffect
-  UI must be fully adaptive — use `GridCells.Adaptive`, `FlowRow`, or equivalent so
   layouts reflow naturally across screen widths. Never hardcode column counts or fixed
   widths for list/grid content. Layouts must look correct and visually appealing at both
   narrow (≈800px) and wide (≈1600px) viewports.
-  **Cards and widgets must never default to full width** unless they are genuinely
   full-width content (e.g., player tables). Use `widthIn(min, max)`, `IntrinsicSize`,
   or `FlowRow` to let cards size to their content and wrap side-by-side on wide screens.
   A 200px card stretching to 1600px is wasted space. Always ask: "does this card need
   all this width, or would a compact layout be more useful?"
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
- PvP ladder data is fetched from the Blizzard Game Data API. **Each addon has its own fetch script** in `scripts/` (e.g., `fetch_tbc_anniversary.py`, `fetch_midnight.py`). Shared helpers live in `scripts/blizzard_api.py`.
- Scripts read credentials from `secrets.properties` (gitignored) or environment variables (`BLIZZARD_CLIENT_ID`, `BLIZZARD_CLIENT_SECRET`).
- Fetched data is pushed to Firestore via the admin API endpoints. Scripts should use the `ADMIN_API_KEY` from `secrets.properties` to authenticate.
- A GitHub Actions workflow (`.github/workflows/fetch-ladder.yml`) runs daily to refresh data. Addon scripts are enabled/disabled independently in the workflow.
- **Brutal efficiency is required for fetch scripts.** The Blizzard API has a hard rate limit of 36,000 requests/hour. Every call counts:
  - **Deduplicate across brackets**: the same player in 2v2, 3v3, and 5v5 is resolved once. Character profile data (gear, talents, race, guild) is identical across brackets.
  - **Never fetch redundant endpoints**: `pvp-summary` only returns links — skip it and call `pvp-bracket/{bracket}` directly.
  - **No data available inline on the profile**: equipment, specializations, and pvp-bracket ratings are all separate endpoints. There is no combined endpoint.
  - **Minimum calls per unique character**: profile (1) + equipment (1) + specializations (1, TBC only — retail has `active_spec` inline) + pvp-bracket per bracket (2–3). That's 5–6 calls per unique character.
- Key namespaces: `dynamic-classicann-{region}` / `profile-classicann-{region}` for TBC Anniversary, `dynamic-{region}` / `profile-{region}` for retail.

## Spec Ordering
- `specIds` in JSON data files are always **alphabetically sorted** — this is enforced by `Composition.init` and is required for stable IDs and deduplication.
- `RichComposition.specs` (the display layer) are always reordered **DPS first, HEALER last** in `enrichCompositions()`. Never change this order at the UI layer; fix it at the enrichment layer if it is wrong.
- When adding new compositions, keep `specIds` alphabetical. The display order is handled automatically.