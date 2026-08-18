# AparK — Agent Guide

## Project

Kotlin Multiplatform (KMP) + Compose Multiplatform app for tracking parked vehicles.
Targets Android + iOS from a single `:composeApp` module.

## Way of working — Spec-Driven Development

This project uses **Spec-Driven Development**. Before writing code for a feature,
author the spec, then the design, then the tasks. See [`docs/PROCESS.md`](docs/PROCESS.md)
for the full loop and how each phase maps to plan mode. Specs live in
[`docs/specs/`](docs/specs/) (one folder `NNN-name/` per feature with `spec.md`,
`design.md`, `tasks.md`); shipped changes are logged in [`CHANGELOG.md`](CHANGELOG.md).
Integrate to `main` via Pull Request, never direct pushes.

## OpenCode Setup

- **Launch from project root**: `opencode` must be run from the project root to load the per-project `opencode.json` (which contains MCP server config and other settings).
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

## Code graph — graphify

[graphify](https://github.com/Graphify-Labs/graphify) indexes this repo into a queryable graph.
Use it to *locate* things instead of grepping; it will not save you from reading the code you are
about to change, because it stores signatures and line numbers, not bodies.

```sh
graphify update .                 # rebuild: ~3s, 0 tokens, tree-sitter only
graphify explain "HomeViewModel"  # declarations, methods, what it implements — with file:line
graphify path "A" "B" --undirected
```

- **Prefer the CLI over the `/graphify` skill when cost matters.** `graphify update .` parses code
  with tree-sitter and spends **no tokens**. The skill's full run also does semantic extraction of
  docs and images, and that phase needs an LLM — which is you.
- **Rebuild before trusting it.** A stale graph is worse than none: it is confidently wrong about
  code that has since been deleted.
- **It cannot see the DI wiring.** Koin resolves at runtime, so there is no edge from a ViewModel to
  its UseCase or Repository — `path HomeScreen FirestoreVehicleRepository` detours through the
  `Vehicle` model instead of following the real chain. **A missing edge is not evidence that
  nothing uses something.**
- **Treat the report's "Surprising Connections" as noise.** It has presented an `inherits NSObject`
  edge between a Swift class and an unrelated Kotlin file that merely imports `NSObject`, labelled
  EXTRACTED.
- `graphify-out/` is gitignored (3+ MB, regenerated in seconds). The binary lives in `~/.local/bin`,
  which is not on `PATH` by default — `uv tool update-shell` fixes that.

## Backend — Cloud Functions

`functions/` holds the project's only server-side code (TypeScript, Node 22):

- An `onDocumentDeleted` trigger on `vehicles`, doing two independent cleanups. It strips the
  deleted id from every member's `userVehicles` — which the client cannot do, because rules forbid
  writing to another user's document — and deletes the vehicle's invitations. The two run under
  `Promise.all` and neither sits behind the other's early returns: the member cleanup needs the
  deleted document's data, the invitation cleanup only needs the id from the event parameters.
- Two callables for sharing, `createVehicleInvite` and `joinVehicleWithCode`, which exist because
  rules deny the client any access to `invites` at all.

Everything is exported once per database.

```sh
npm --prefix functions run build     # compile (the deploy predeploy hook runs this too)
npx firebase-tools@latest deploy --only functions
npx firebase-tools@latest functions:log
```

- **Region is not a free choice**: both Firestore databases live in `eur3`, so the triggers must
  be deployed to `europe-west4`. The default `us-central1` is rejected.
- **Adding a Firebase module to KMP has a second step on iOS**: the iOS app resolves Firebase
  through Swift Package Manager, so the matching product (e.g. `FirebaseFunctions`) must also be
  added to the `iosApp` target or the build fails at link time, not at compile time.
- **Callable functions cannot infer the caller's database** the way a Firestore trigger can, so
  each one is exported per database and the app picks by `AppConfig.isDebug`.
- **A TTL policy on `invites` is part of how this works, and it is not in the repo**: both databases
  delete an invitation seven days after its `expiresAt`. There is no file that versions this and no
  deploy that reproduces it — `firebase-tools` has no TTL command, so it is set by hand in the
  console per database. A new database needs it set up again, or invitations pile up forever. See
  `docs/specs/007-invite-cleanup/`
- **Import narrowly**: `firebase-functions/logger`, *not* the `firebase-functions/v2` barrel — the
  barrel loads every provider, including Realtime Database, whose `@firebase/app` peer dependency
  npm does not install, which breaks the deploy-time analysis.

## Architecture

- **Clean Architecture + MVI**: UI (Compose) → ViewModel → UseCase → Repository (interface) → DataSource (impl)
- **DI**: Koin, single instance for repositories, factory for use cases, viewModel for ViewModels (`SharedModule.kt`)
- **Navigation**: JetBrains Navigation 3 (alpha06), `sealed interface Destiny` with `@Serializable` data objects. Uses `backStack.clear()` + `add()` for login/logout routing (`BasicNavigationWrapper.kt`)
- **Auth flow**: Splash → Login/Register → EmailVerification → Home. Splash checks `isUserLoggedIn && isUserEmailVerified`
- **Expect/actual** for: maps (`AparKMap`), location (`LocationSource`), auth buttons (`GoogleSignInButton`, `AppleSignInButton`), permissions (`LocationPermissionHandler`), settings deep-link (`OpenAppSettingsHandler`)

### Packages (commonMain)

| Package | Role |
|---------|------|
| `data.repository` | Firestore + Auth repository implementations |
| `data.location` | Platform location source interfaces/impls |
| `data.util` | Firestore constants |
| `domain.model` | `User`, `Vehicle` (with nested `LocationModel`) |
| `domain.repository` | `AuthRepository`, `VehicleRepository`, `UserRepository`, `LocationRepository` |
| `domain.usecase` | Individual use cases (one `operator fun invoke()` each) |
| `presentation.splash` | Splash — checks auth state, routes to Home or Login |
| `presentation.auth.*` | Login, Register, EmailVerification, ResetPassword |
| `presentation.home` | Main screen — map + vehicle carousel + edit mode |
| `presentation.addvehicle` | Create-vehicle screen |
| `presentation.components` | Shared composables: map, buttons, dialog, permission handlers |
| `presentation.navigation` | Navigation 3 setup (`Destiny` sealed interface) |
| `di` | Koin modules |
| `ui.theme` | Theme, colors, typography |
| `utils` | Time utils, validation, snackbar messages |

### Firestore collections

| Collection | Document ID | Key fields |
|------------|-------------|------------|
| `users` | `FirebaseAuth.uid` | `email`, `name`, `userVehicles: List<String>` (vehicle IDs — **its order is the carousel order**) |
| `vehicles` | Auto-generated | `name`, `licensePlate`, `ownerId`, `sharedUsers: List<String>`, `inviteCode`, `lastLocation` |

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
- **`BackHandler` is not part of `compose.ui`**: the `org.jetbrains.compose.ui:ui-backhandler` artifact must be declared explicitly or the reference will not resolve, and it needs `@OptIn(ExperimentalComposeUiApi::class)`
- **`NavDisplay`'s `entryDecorators` list replaces the default, it does not extend it**. The project
  passes both `rememberSaveableStateHolderNavEntryDecorator()` and
  `rememberViewModelStoreNavEntryDecorator()`; dropping the first one would break every
  `rememberSaveable` in the app, and the second depends on it to hand out `SavedStateHandle`s. This
  is what scopes ViewModels to the navigation entry — without it they resolve against the root owner
  and never get cleared (see `docs/specs/006-scoped-viewmodels/`)
- **The lifecycle version is pinned by Compose**: `lifecycle-viewmodel-navigation3` 2.10.0 targets
  `compose.runtime` 1.9.3, the project's version. 2.11.0 targets 1.10.2 and would drag a Compose
  Multiplatform bump with it
- **A missing R8 keep-rules file is a warning, not an error.** `proguardFiles(..., "proguard-rules.pro")`
  resolves against the *module* directory, so the file has to be `composeApp/proguard-rules.pro`.
  It once lived in `composeApp/src/`, and R8 printed `Supplied proguard configuration does not
  exist:` and **built a release APK anyway**, minified with none of the project's keeps. Nothing
  fails loudly, and iOS has no R8 at all, so testing releases on a real iPhone cannot catch it.
  To check the rules are live, look for the domain models in
  `composeApp/build/outputs/mapping/release/mapping.txt`: they must map to themselves
- **Listening to a Firestore document that does not exist returns PERMISSION_DENIED, not "not found"**, because the rule dereferences a null `resource`. `getVehiclesForUser` therefore wraps each per-vehicle snapshot in `retryWhen` + `catch`; without it, a just-created or deleted vehicle can crash the app

## Known Incomplete / Buggy Areas

> The full picture — architecture review and the phased plan to publication — is in
> [`docs/ROADMAP.md`](docs/ROADMAP.md).

- **`assembleRelease` produces an unsigned APK**: there is no `signingConfig`, and Play needs a
  signed AAB (`bundleRelease`)
- **The vehicle-detail screen is a placeholder**: it renders the name and, for the owner, a share
  button. Nothing else — no vehicle data, no members, no editing
- **No transfer-ownership UI**: `transferVehicleOwnership` exists in the repository but nothing calls it, even though the delete dialog points users at it
- **`UpdateVehicleUseCase` is wired into Koin but nothing calls it**, so `Vehicle.model` and
  `Vehicle.color` are never written. `Vehicle.inviteCode` is dead too — invitations moved to the
  `invites` collection in spec 005
- **`lastLocation.user` stores a whole `User`**, `userVehicles` array included
  (`UpdateVehicleLocationUseCase.kt:34`), so every member of a shared vehicle can read the vehicle
  ids of whoever parked it last. The rule only validates the email on that object
- Only 1 placeholder test (`ComposeAppCommonTest.kt`)
- Apple Sign-In on Android is a no-op
- Hardcoded Spanish strings instead of resources: the GPS-denied snackbar and the "Logout" menu item in `HomeScreen.kt`
- `userEmail` in `HomeUiState` is never written — dead field

> For what has shipped, read [`CHANGELOG.md`](CHANGELOG.md) rather than a list here: it is
> kept up to date on every change.

## Tests

```sh
./gradlew :composeApp:allTests
# commonTest only (placeholder test exists)
```
