# AparK — Agent Guide

## Project

Kotlin Multiplatform (KMP) + Compose Multiplatform app for tracking parked vehicles.
Targets Android + iOS from a single `:composeApp` module.

## OpenCode Setup

- **Launch from project root**: `opencode` must be run from `/Users/albertomedina/AndroidStudioProjects/Apark` to load per-project `opencode.json` (which contains MCP server config and other settings).
- **MCP**: Firebase CLI MCP (`npx firebase-tools@latest mcp`) is configured in `opencode.json` for direct Firestore and Auth inspection.
- **Context file**: `~/.config/opencode/AGENTS.md` contains general-purpose context7 instructions.

## Build & Run

```sh
# Android debug APK
./gradlew :composeApp:assembleDebug

# Verify iOS Kotlin compilation (fast feedback, no Xcode needed)
./gradlew :composeApp:compileKotlinIosSimulatorArm64

# Run iOS: open iosApp/iosApp.xcodeproj in Xcode, build from there
```

## Architecture

- **Clean Architecture + MVI**: UI (Compose) → ViewModel → UseCase → Repository (interface) → DataSource (impl)
- **DI**: Koin, single instance for repositories, factory for use cases, viewModel for ViewModels (`SharedModule.kt`)
- **Navigation**: JetBrains Navigation 3 (alpha06), `sealed interface Destiny` with `@Serializable` data objects. Uses `backStack.clear()` + `add()` for login/logout routing (`BasicNavigationWrapper.kt`)
- **Auth flow**: Splash → Login/Register → EmailVerification → Home. Splash checks `isUserLoggedIn && isUserEmailVerified`
- **Expect/actual** for: maps (`AparKMap`), location (`LocationSource`), auth buttons (`GoogleSignInButton`, `AppleSignInButton`), permissions (`LocationPermissionHandler`), settings deep-link (`OpenAppSettingsHandler`)

## Tech Stack

| Layer | Choice |
|-------|--------|
| UI | Compose Multiplatform 1.9.3, Material3 |
| DI | Koin 4.1.1, koin-compose-viewmodel |
| Navigation | JetBrains Navigation 3 `navigation3-ui:1.0.0-alpha06` |
| Backend | Firebase Auth + Firestore (dev.gitlive:firebase 2.4.0) |
| Maps | Google Maps (`maps-compose` on Android, native `GMSMapView` on iOS) |
| Auth | Email/password, Google (Credential Manager / GIDSignIn), Apple |
| Animations | Compottie (Lottie for Compose Multiplatform) |
| Serialization | kotlinx.serialization |

## Key Gotchas

- **Auth data leak bug (fixed)**: `HomeViewModel` uses `authStateChanged` + `flatMapLatest` to reactively switch vehicle data when the user changes. Never call `loadVehicles()` once in `init` — observe the auth flow instead. See `observeAuthState()` in `HomeViewModel.kt`.
- **Firestore DB name differs by build**: Debug uses `"apark-at"`, release uses `"(default)"`. Controlled by `AppConfig.isDebug` in `SharedModule.kt`.
- **Secrets are gitignored**: `**/Secrets.xcconfig`, `**/GoogleService-Info.plist`, `**/google-services.json`, `*.jks`, `local.properties`. Must be provided manually for local builds.
- **iOS map padding**: `-30.0` magic number in `AparKMap.ios.kt:114` for bottom sheet offset. Can break with layout changes.
- **Bottom nav tabs are cosmetic**: 3 tabs (Map, My Cars, Profile) switch local state only — no actual navigation implemented.
- **Kotlin 2.3.0** — Compose Compiler plugin is bundled (`org.jetbrains.kotlin.plugin.compose`), no separate version.
- **`flatMapLatest` requires `@OptIn(ExperimentalCoroutinesApi::class)`** — used in `HomeViewModel.observeAuthState()`

## Known Incomplete / Buggy Areas

- `SignOutUseCase` — use case for logout flow
- No add-vehicle screen or vehicle-detail screen — navigation callbacks are no-ops
- Only 1 placeholder test (`ComposeAppCommonTest.kt`)
- Apple Sign-In on Android is a no-op
- GPS permission denied message uses hardcoded Spanish string instead of resource key
- Firebase Security Rules and indexes not version-controlled (planned)

## Recent Changes

- Auth data leak fixed via `flatMapLatest` on `authStateChanges` Flow (`fix/logout-data-leak` branch)
- `ShareVehicleWithUserUseCase` bug fixed: `vehicleId` → `userId`
- Monolithic `FirestoreRepository` split into `FirestoreVehicleRepository` + `FirestoreUserRepository`
- Silent try-catch fallback in Firestore init removed (fail fast on DB misconfiguration)
- Firebase MCP configured in `opencode.json` with `.firebaserc`

## Git Branches

- `fix/pre-features-foundation` — current branch, contains foundation fixes before vehicle CRUD screens
- `fix/logout-data-leak` — auth data leak fix (not merged to main)
- `main` — primary branch

## Tests

```sh
./gradlew :composeApp:allTests
# commonTest only (placeholder test exists)
```
