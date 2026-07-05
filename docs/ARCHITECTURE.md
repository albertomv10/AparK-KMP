# Apark — Architecture & Changes

## Project Overview

Kotlin Multiplatform (KMP) + Compose Multiplatform app for tracking parked vehicles.
Targets Android + iOS from a single `:composeApp` module.

## Architecture

```
UI (Compose) → ViewModel (MVI) → UseCase → Repository (interface) → DataSource (impl)
```

- **DI**: Koin — `single` for repositories, `factory` for use cases, `viewModel` for ViewModels
- **Navigation**: JetBrains Navigation 3 (alpha06), `sealed interface Destiny` with `@Serializable` data objects
- **Auth flow**: Splash → Login/Register → EmailVerification → Home
- **Platform bridging**: `expect`/`actual` for maps, location, auth buttons, permissions, settings deep-link

## Key Packages (commonMain)

| Package | Role |
|---------|------|
| `data.repository` | Firestore + Auth repository implementations |
| `data.location` | Platform location source interfaces/impls |
| `data.util` | Firestore constants |
| `domain.model` | `User`, `Vehicle`, `LocationModel` |
| `domain.repository` | `AuthRepository`, `VehicleRepository`, `UserRepository`, `LocationRepository` |
| `domain.usecase` | Individual use cases (one `operator fun invoke()` each) |
| `presentation.splash` | Splash screen — checks auth state, routes to Home or Login |
| `presentation.auth.*` | Login, Register, EmailVerification, ResetPassword screens |
| `presentation.home` | Main screen — map + vehicle carousel + logout |
| `presentation.components` | Shared composables: map, buttons, permission handlers |
| `presentation.navigation` | Navigation 3 setup (Destiny sealed interface) |
| `di` | Koin modules |
| `ui.theme` | Theme, colors, typography |
| `utils` | Time utils, validation, snackbar messages |

## Branches

### `main`
Primary branch. Stable, deployable state.

### `fix/logout-data-leak`
Fixes user data leaking between login sessions.
- **Problem**: `HomeViewModel` called `loadVehicles()` once in `init`. When logging out and logging in as a different user, the ViewModel was reused and never re-fetched vehicles.
- **Solution**: Added `authStateChanges: Flow<FirebaseUser?>` to `AuthRepository`. `HomeViewModel` now observes this flow with `flatMapLatest`, automatically cancelling the previous vehicle collection and starting a new one when the user changes.

### `fix/pre-features-foundation`
Preparatory fixes before adding add-vehicle and vehicle-detail screens. See changelog below.

## Changes

### `fix/pre-features-foundation`

| Change | File(s) | Reason |
|--------|---------|--------|
| Fixed `vehicleId` → `userId` | `ShareVehicleWithUserUseCase.kt:9` | Function received `vehicleId` instead of `userId`, making sharing non-functional |
| Split `FirestoreRepository` | `FirestoreVehicleRepository.kt`, `FirestoreUserRepository.kt` | Violated SRP — 13 methods spanning two domains. Split into vehicle and user classes |
| Single Firestore instance in Koin | `SharedModule.kt` | Previously created two separate `FirestoreRepository` instances. Now one `FirebaseFirestore` shared by both repositories |
| Added `$schema` to `opencode.json` | `opencode.json` | Schema annotation for IDE validation |

### OpenCode Configuration (this session)

| Change | File(s) | Reason |
|--------|---------|--------|
| Added Firebase MCP server config | `opencode.json` | `npx firebase-tools@latest mcp` for Firestore/Auth inspection via MCP |
| Created `.firebaserc` | `.firebaserc` | Pins Firebase project `apark-617fd` |
| Investigated MCP not showing up | — | Root cause: OpenCode was launched from `~/` not project root; MCP config in per-project `opencode.json` is invisible when running from a different directory |

### `main` — merged branches (this session)

| Change | File(s) | Reason |
|--------|---------|--------|
| Merged `fix/pre-features-foundation` + `fix/logout-data-leak` into `main` | — | Combined all fixes into main |
| Fixed `SingOutUseCase` typo → `SignOutUseCase` | `SingOutUseCase.kt` → `SignOutUseCase.kt` | All references updated (class, imports, parameters) |
| Version-controlled Security Rules | `firestore.rules`, `firebase.json` | Owner-only writes, shared users can update `lastLocation` with email validation via `auth.token.email` |
| Version-controlled indexes | `firestore.indexes.json` | Empty (no composite indexes needed yet) |
| Deployed rules + indexes to Firebase | `(default)` DB | Rules apply to all databases via `{database}` wildcard |
| Enabled delete protection | Both `(default)` and `apark-at` | Prevents accidental database deletion |

### `fix/logout-data-leak`

| Change | File(s) | Reason |
|--------|---------|--------|
| Added `authStateChanges` Flow | `AuthRepository.kt`, `FirebaseAuthRepository.kt` | Expose reactive auth state for ViewModel observation |
| Reactive vehicle loading | `HomeViewModel.kt` | Old `loadVehicles()` in `init` never ran on re-login. New `observeAuthState()` uses `flatMapLatest` |
| Logout button + flow | `HomeScreen.kt`, `SignOutUseCase.kt`, `SharedModule.kt` | Added DropdownMenu with Logout option that signs out and navigates to Login |
| AGENTS.md | Root | AI configuration file for project context |

## Firestore Collections

| Collection | Document ID | Key Fields |
|------------|-------------|------------|
| `users` | `FirebaseAuth.uid` | `email`, `name`, `userVehicles: List<String>` (vehicle IDs) |
| `cars` | Auto-generated | `name`, `ownerId`, `sharedUsers: List<String>`, `inviteCode`, `lastLocation: LocationModel` |

Debug builds use database name `"apark-at"`; release uses `"(default)"`. Configured in `SharedModule.kt`.

## Known Issues

- Apple Sign-In on Android is a no-op
- Bottom navigation tabs (Map, My Cars, Profile) are cosmetic — no actual navigation
- No add-vehicle or vehicle-detail screens (upcoming)
- Only 1 placeholder test in `commonTest`
- GPS permission denied message uses hardcoded Spanish string in `HomeScreen.kt`
