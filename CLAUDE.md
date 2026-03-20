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

## Additional Code Guidance
-  any exception-generating code on the web path (IO, JS interop) needs catch
   (Throwable) and must never be left uncaught in a LaunchedEffect

## Multi-Bracket & Multi-Addon Scope
- The app is designed to support **multiple game modes**: 2v2, 3v3, 5v5, and potentially other addons (e.g., Wrath, Retail).
- **Data is currently only curated for TBC Anniversary 2v2** — this is a content scope decision, not an architectural one.
- All core models (`Composition`, `Matchup`) must use **`List<String>` for spec slots**, never hardcoded `spec1Id`/`spec2Id` pairs. This is what allows the same model to represent a 2-spec comp and a 5-spec comp without changes.
- `GameMode` should carry a `teamSize: Int` so repositories and UI can validate and render compositions correctly for any bracket.