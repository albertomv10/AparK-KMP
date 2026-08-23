# AparK — Agent Guide

## Project

Kotlin Multiplatform + Compose Multiplatform app for **remembering where a shared vehicle is
parked**. A vehicle can have several members; any of them can park it and everyone sees where it
is. Android and iOS ship from one `:composeApp` module, UI included.

Not a commercial project, but it is built to be publishable: the target is the App Store and Google
Play, so store requirements are treated as real constraints rather than someday-problems.

## Read these first

| Document | What it answers |
|----------|-----------------|
| [`docs/PROCESS.md`](docs/PROCESS.md) | How work happens here: Spec-Driven Development and the Git flow |
| [`docs/ROADMAP.md`](docs/ROADMAP.md) | Architecture review and the phased plan to publication |
| [`docs/DATA-MODEL.md`](docs/DATA-MODEL.md) | Where the schema is **going**, and the method behind it |
| [`docs/FIREBASE.md`](docs/FIREBASE.md) | The two projects, and how to stand one up from zero |
| [`docs/specs/NNN-*/`](docs/specs/) | One folder per feature: `spec.md`, `design.md`, `tasks.md` |
| [`CHANGELOG.md`](CHANGELOG.md) | What has actually shipped |

**Before writing code for a feature, write the spec.** Then the design, then the tasks. Plan mode
*is* the design phase, and approving the plan equals approving `design.md`. Integrate to `main` via
Pull Request — never push to `main` directly.

## Tooling

- **Claude Code**, with the Firebase CLI MCP server configured in `.mcp.json`
  (`npx -y firebase-tools@latest mcp`). It gives direct read access to Firestore, Auth, Functions
  logs and project config, which beats guessing from the client side.
- `opencode.json` is left over from an earlier setup and duplicates the same MCP config. Harmless,
  but it is no longer what drives this project.

## Build & Run

```sh
./gradlew :composeApp:assembleDebug                      # Android debug APK
./gradlew :composeApp:installDebug                       # install on a connected device
./gradlew :composeApp:compileKotlinIosSimulatorArm64     # fast iOS check, no Xcode needed
./gradlew :composeApp:allTests                           # commonTest (one placeholder test)
```

iOS runs from `iosApp/iosApp.xcodeproj` in Xcode. To check the Swift side compiles without opening
the IDE:

```sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO
```

**Compiling Kotlin for iOS does not compile the Swift.** Changes to `iosApp/` — bridges, Info.plist,
build phases — only fail in Xcode, so run the command above before claiming an iOS change works.

## Environments

Two **separate Firebase projects**. Which one a build talks to is decided by the config file baked
into the binary, and by nothing at runtime:

| Build | Project | Config file |
|-------|---------|-------------|
| debug | `apark-dev` | `composeApp/src/debug/google-services.json` · `iosApp/FirebaseConfig/debug/GoogleService-Info.plist` |
| release | `apark-617fd` | `composeApp/google-services.json` · `iosApp/FirebaseConfig/release/GoogleService-Info.plist` |

All of those are **gitignored**, along with `Secrets.xcconfig`, `local.properties` and `*.jks`, so a
fresh clone has to regenerate them — see [`docs/FIREBASE.md`](docs/FIREBASE.md).

```sh
npx firebase-tools@latest deploy --only firestore:rules             # dev, via the default alias
npx firebase-tools@latest deploy --only firestore:rules -P prod     # production, explicitly named
```

`.firebaserc` points `default` at **development on purpose**: touching production requires naming it.

## Architecture

**Clean Architecture + MVI**: UI (Compose) → ViewModel → UseCase → Repository (interface) →
DataSource (impl). Koin for DI — `single` for repositories, `factory` for use cases, `viewModel` for
ViewModels, all in `SharedModule.kt`.

**Navigation 3** (`navigation3-ui`, alpha): a `sealed interface Destiny` of `@Serializable` objects
in `BasicNavigationWrapper.kt`. Login/logout routing uses `backStack.clear()` + `add()`.

**Auth flow**: Splash → Login/Register → EmailVerification → Home.

**expect/actual** for maps (`AparKMap`), location (`LocationSource`), the social sign-in buttons,
permissions (`LocationPermissionHandler`) and the settings deep link (`OpenAppSettingsHandler`).

### Packages (commonMain)

| Package | Role |
|---------|------|
| `data.repository` | Firestore, Auth, invite and location implementations |
| `data.location` | Platform location source |
| `data.util` | Firestore constants |
| `domain.model` | `User`, `Vehicle` (with nested `LocationModel`), `VehicleInvite`, `JoinResult` |
| `domain.repository` | Repository interfaces |
| `domain.usecase` | One `operator fun invoke()` each |
| `presentation.splash` · `presentation.auth.*` | Splash, login, register, verification, reset |
| `presentation.home` | Map + vehicle carousel + edit mode |
| `presentation.addvehicle` · `presentation.vehicledetail` | Create/join, and the detail placeholder |
| `presentation.components` | Shared composables: map, buttons, dialog, permission handlers |
| `presentation.navigation` | Navigation 3 setup |
| `di` · `ui.theme` · `utils` | Koin modules, Material 3 theme, helpers |

### Firestore collections (as they are today)

| Collection | Document ID | Key fields |
|------------|-------------|------------|
| `users` | `FirebaseAuth.uid` | `email`, `name`, `userVehicles: List<String>` — **its order is the carousel order** |
| `vehicles` | Auto-generated | `name`, `licensePlate`, `ownerId`, `sharedUsers: List<String>`, `lastLocation` |
| `invites` | The 8-character code | `vehicleId`, `createdBy`, `expiresAt`, `usedBy` — clients are denied all access |

> This is the current shape, not the intended one. [`docs/DATA-MODEL.md`](docs/DATA-MODEL.md)
> describes where it is going, and [spec 008](docs/specs/008-vehicle-membership-model/spec.md) is
> the next step.

## Backend — Cloud Functions

`functions/` is the only server-side code (TypeScript, Node 22): one `onDocumentDeleted` trigger on
`vehicles` doing two independent cleanups under `Promise.all`, plus two callables for sharing that
exist because the rules deny clients any access to `invites` at all.

```sh
npm --prefix functions run build
npx firebase-tools@latest deploy --only functions            # dev
npx firebase-tools@latest functions:log
```

- **Region is not a free choice**: Firestore is in `eur3` in both projects, so functions go to
  `europe-west4`. The Eventarc trigger itself is created in `eur3`, next to the database.
- **The first 2nd-gen deploy to a new project fails, and that is expected.** Firebase enables Cloud
  Build, Artifact Registry, Eventarc, Run and Pub/Sub in the same run, then grants IAM roles to
  service agents Google is still creating. Wait a couple of minutes and retry rather than touching
  permissions. Pass `--force` the first time so Artifact Registry gets a cleanup policy.
- **A callable created without its public invoker binding answers 401 to everything**, before your
  code runs. It shows up in the logs as `The request was not authorized to invoke this service`.
  The binding is applied on *create*, so a function that failed to build and was later *updated* can
  end up without it — delete it and redeploy.
- **A TTL policy on `invites` is part of how this works and is not in the repo.** Each project
  deletes invitations seven days after `expiresAt`; `firebase-tools` has no TTL command, so it is
  set by hand per database. See [spec 007](docs/specs/007-invite-cleanup/spec.md).
- **Import narrowly**: `firebase-functions/logger`, *not* the `firebase-functions/v2` barrel, which
  pulls in every provider and breaks deploy-time analysis.

## Code graph — graphify

[graphify](https://github.com/Graphify-Labs/graphify) indexes the repo into a queryable graph. Use
it to *locate* things instead of grepping; it stores signatures and line numbers, not bodies, so it
will not save you from reading the code you are about to change.

```sh
graphify update .                 # rebuild: ~3s, 0 tokens, tree-sitter only
graphify explain "HomeViewModel"
graphify path "A" "B" --undirected
```

- **Prefer the CLI over the `/graphify` skill when cost matters** — `graphify update .` spends no
  tokens; the skill's semantic phase needs an LLM, which is you.
- **Rebuild before trusting it.** A stale graph is confidently wrong about deleted code.
- **It cannot see the DI wiring.** Koin resolves at runtime, so there is no edge from a ViewModel to
  its UseCase. **A missing edge is not evidence that nothing uses something.**
- **Treat "Surprising Connections" as noise.**
- `graphify-out/` is gitignored. The binary lives in `~/.local/bin`, not on `PATH` by default.

## Tech Stack

| Layer | Choice |
|-------|--------|
| UI | Compose Multiplatform 1.9.3, Material 3 |
| Language | Kotlin 2.3.0 — the Compose Compiler plugin is bundled, no separate version |
| DI | Koin 4.1.1, koin-compose-viewmodel |
| Navigation | JetBrains Navigation 3 `navigation3-ui:1.0.0-alpha06` |
| Backend | Firebase Auth + Firestore + Functions (`dev.gitlive:firebase` 2.4.0) |
| Maps | Google Maps — `maps-compose` on Android, native `GMSMapView` on iOS |
| Auth | Email/password, Google (Credential Manager / GIDSignIn), Apple |
| Animations | Compottie |
| Serialization | kotlinx.serialization |

## Key Gotchas

### Build and release

- **A missing R8 keep-rules file is a warning, not an error.** `proguardFiles(..., "…")` resolves
  against the *module* directory, so it must be `composeApp/proguard-rules.pro`. It once lived in
  `composeApp/src/`, and R8 printed `Supplied proguard configuration does not exist:` and **built a
  release APK anyway**, with none of the project's keeps. Nothing fails loudly, and iOS has no R8,
  so testing releases on a real iPhone cannot catch it. Verify via
  `composeApp/build/outputs/mapping/release/mapping.txt`: the domain models must map to themselves.
- **`assembleRelease` produces an *unsigned* APK** — there is no `signingConfig`, and Play needs a
  signed AAB (`bundleRelease`).
- **Kotlin `Boolean` crosses to Swift boxed** as `KotlinBoolean`: read it with `.boolValue`, and
  construct it with `KotlinBoolean(bool:)`. When in doubt, check the generated
  `ComposeApp.framework/Headers/ComposeApp.h`.

### Firebase and OAuth

- **An Android OAuth client is globally unique on (package name + SHA-1)** across *all* Google
  projects. Registering the same debug fingerprint in a second project silently fails, and Google
  Sign-In there dies with `[28444] Developer console is not set up correctly`, which says nothing
  about the real cause. Removing the SHA in Firebase **unlinks but does not delete** the underlying
  OAuth client — it has to be deleted in Google Cloud → APIs & Services → Credentials.
- **`firebase apps:sdkconfig --out` does not create the parent directory.** It fails, writes
  nothing, and the next symptom is a debug build quietly talking to production, because the plugin
  falls back to the module-root `google-services.json`.
- **`apps:android:sha:create` reports `Failed to create SHA certificate hash` and registers it
  anyway.** Verify with `sha:list`; do not trust the exit code.
- **Listening to a Firestore document that does not exist returns PERMISSION_DENIED**, not "not
  found", because the rule dereferences a null `resource`. `getVehiclesForUser` wraps each snapshot
  in `retryWhen` + `catch`; without it a just-created vehicle can crash the app. Spec 008 removes
  the need for this.

### App code

- **Auth data leak (fixed)**: `HomeViewModel` observes `authStateChanges` + `flatMapLatest` so
  vehicle data switches with the user. Never load once in `init`. `flatMapLatest` needs
  `@OptIn(ExperimentalCoroutinesApi::class)`.
- **`NavDisplay`'s `entryDecorators` list replaces the default, it does not extend it.** Both
  `rememberSaveableStateHolderNavEntryDecorator()` and `rememberViewModelStoreNavEntryDecorator()`
  must be passed: dropping the first breaks every `rememberSaveable`, and the second depends on it.
  This is what scopes ViewModels to the navigation entry (see spec 006).
- **The lifecycle version is pinned by Compose**: `lifecycle-viewmodel-navigation3` 2.10.0 targets
  `compose.runtime` 1.9.3. 2.11.0 would drag a Compose Multiplatform bump with it.
- **`BackHandler` is not part of `compose.ui`** — `org.jetbrains.compose.ui:ui-backhandler` must be
  declared explicitly, with `@OptIn(ExperimentalComposeUiApi::class)`.
- **Social sign-in errors carry a reason, not a string.** `SocialLoginFailure` has a
  `SocialLoginReason`: `CANCELLED` shows nothing, `NO_ACCOUNTS` is actionable, everything else gets
  one readable message. Its `detail` field is for diagnostics and **must never be displayed**.
- **iOS map padding**: the `-30.0` magic number in `AparKMap.ios.kt` for the bottom sheet offset can
  break with layout changes.

### Testing on devices

- **MIUI/Xiaomi suppresses third-party app logs.** `println` and `Log` from the app do not reach
  `logcat`, and the buffer only holds a few minutes. Do not debug by log on those devices — make the
  app show its own error instead. This is why Crashlytics is a priority in the roadmap.
- The release build is tested on a personal iPhone, so **the Android release path gets the least
  attention** — that is where R8 problems hide.

## Known Incomplete

> The full picture and the order to tackle it: [`docs/ROADMAP.md`](docs/ROADMAP.md).

- **Bottom nav tabs are cosmetic**: three tabs switching local state, no navigation behind them.
- **The vehicle-detail screen is a placeholder**: name plus a share button for the owner.
- **No transfer-ownership UI**: `transferVehicleOwnership` exists but nothing calls it, even though
  the delete dialog points users at it.
- **`UpdateVehicleUseCase` is wired into Koin but nothing calls it**, so `Vehicle.model` and
  `Vehicle.color` are never written. `Vehicle.inviteCode` is dead too.
- **`lastLocation.user` stores a whole `User`**, `userVehicles` included
  (`UpdateVehicleLocationUseCase.kt:34`), so any member can read the vehicle ids of whoever parked
  it last. Fixed by spec 008.
- **One placeholder test**, no CI, no Crashlytics, no App Check, no local persistence.
- Hardcoded Spanish strings: the GPS-denied snackbar and the "Logout" item in `HomeScreen.kt`.
- `userEmail` in `HomeUiState` is never written — dead field.

**Not a bug**: `AppleSignInButton`'s Android `actual` has an empty body, so the button is not
composed at all outside iOS. That is deliberate — the platform decides whether the button exists.
