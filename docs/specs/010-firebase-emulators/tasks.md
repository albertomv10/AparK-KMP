# Tasks: emuladores de Firebase en debug

- **Spec**: [spec.md](spec.md) · **Diseño**: aprobado en modo plan

## Configuración
- [x] `firebase.json`: puertos de `auth` (9099) y `functions` (5001)
- [x] `build.gradle.kts`: `buildConfigField` con `APARK_EMULATOR_HOST`, **solo** en la variante debug
- [x] `Info.plist`: `$(APARK_EMULATOR_HOST)` desde `Secrets.xcconfig`

## Conexión
- [x] `di/EmulatorConfig.kt`: el `var` común y los puertos
- [x] `SharedModule`: `useEmulator` en los tres singletons, antes del primer uso
- [x] `androidDebug/EmulatorHost.kt` lee `BuildConfig`; `androidRelease` devuelve `null`
- [x] `AparkApp` lo fija antes de `initKoin`
- [x] `iOSApp.swift` lo fija bajo `#if DEBUG`, antes de `doInitKoinIos()`

## Siembra
- [x] `tools/seed-emulators.ts`: dos usuarios, tres vehículos, uno compartido
- [x] Comprueba sus propios invariantes, porque el Admin SDK se salta las reglas
- [x] Se niega a correr sin las variables de emulador, para no escribir en `apark-dev`
- [x] Scripts `emulators` y `seed` en `tools/package.json`

## Documentación
- [x] `docs/FIREBASE.md`: levantarlos, sembrarlos, lo que no dan y la trampa del llavero
- [x] `spec.md`: criterio 4 reescrito a lo que de verdad se puede garantizar
- [x] Entrada en `CHANGELOG.md`

## Verificación
- [x] Compila Android (debug **y** release), iOS-Kotlin, tests nativos y Xcode (Debug y Release)
- [x] **(1)** Login con el usuario sembrado hasta el mapa, en el simulador
- [x] **(2)** Los datos son los sembrados: tres vehículos, estados y matrículas correctos
- [x] **(6)** `docs/FIREBASE.md` explica cómo levantarlos desde cero
- [x] **(3)** Parar y rearrancar: cero rastro, y tras sembrar el **mismo hash** (`6c9100a004e49422`)
- [x] **(4)** Debug **sin** la propiedad: `EMULATOR_HOST = ""` → habla con `apark-dev` como siempre
- [x] **(5)** Ni `EMULATOR_HOST` ni el valor `10.0.2.2` aparecen en el APK de release
- [x] Compartir y unirse contra el emulador de Functions: código generado por `createVehicleInvite`,
      usado por la otra cuenta, `memberIds` actualizado y la invitación marcada como usada
- [x] De camino: el plugin de *secrets* metía las contraseñas de firma en el APK de debug
