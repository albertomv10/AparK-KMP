# AparK

App multiplataforma para **recordar dónde has aparcado el coche** y compartir esa ubicación
con otras personas (pareja, familia, compañeros de piso).

Cada vehículo guarda su última ubicación, quién lo aparcó y cuándo. Los vehículos se pueden
compartir, de modo que cualquier miembro puede aparcarlo y todos ven dónde está.

Kotlin Multiplatform + Compose Multiplatform: **Android e iOS desde un único módulo**
(`:composeApp`), interfaz incluida.

---

## Stack

| Capa | Tecnología |
|------|------------|
| UI | Compose Multiplatform 1.9.3 · Material 3 |
| Arquitectura | Clean Architecture + MVI |
| DI | Koin |
| Navegación | JetBrains Navigation 3 |
| Backend | Firebase Auth + Cloud Firestore (`dev.gitlive:firebase`) |
| Mapas | Google Maps (`maps-compose` en Android · `GMSMapView` nativo en iOS) |
| Autenticación | Email/contraseña · Google · Apple |

---

## Requisitos previos

Los ficheros con credenciales **no están en el repositorio** (están en `.gitignore`). Para
compilar en local necesitas colocarlos tú:

| Fichero | Ubicación |
|---------|-----------|
| `google-services.json` | `composeApp/` |
| `GoogleService-Info.plist` | `iosApp/FirebaseConfig/debug/` y `.../release/` |
| `Secrets.xcconfig` | `iosApp/iosApp/` |
| `local.properties` | raíz (SDK de Android y clave de Google Maps) |

Además: JDK 17+, Android Studio y —para iOS— Xcode.

---

## Compilar y ejecutar

**Android**

```sh
./gradlew :composeApp:assembleDebug     # generar APK de debug
./gradlew :composeApp:installDebug      # instalar en el dispositivo/emulador conectado
```

**iOS**

Abre `iosApp/iosApp.xcodeproj` en Xcode y ejecuta desde ahí. Para comprobar rápido que el
código Kotlin compila para iOS sin abrir Xcode:

```sh
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

**Tests**

```sh
./gradlew :composeApp:allTests
```

---

## Bases de datos

El proyecto usa **dos bases de datos Firestore** en el mismo proyecto de Firebase:

- Compilaciones **debug** → `apark-at`
- Compilaciones **release** → `(default)`

La selección es automática según `AppConfig.isDebug` (ver `SharedModule.kt`). Las reglas de
seguridad viven en [`firestore.rules`](firestore.rules) y se despliegan a **ambas**:

```sh
npx firebase-tools@latest deploy --only firestore:rules
```

---

## Estructura

```
composeApp/src/
├── commonMain/     # código y UI compartidos (la mayor parte del proyecto)
│   ├── kotlin/…/data/          # repositorios (Firestore, Auth, ubicación)
│   ├── kotlin/…/domain/        # modelos, interfaces de repositorio y casos de uso
│   ├── kotlin/…/presentation/  # pantallas, ViewModels y componentes
│   ├── kotlin/…/di/            # módulos de Koin
│   └── composeResources/       # textos (es · en · fr) e imágenes
├── androidMain/    # implementaciones `actual` de Android
└── iosMain/        # implementaciones `actual` de iOS

iosApp/             # proyecto Xcode: punto de entrada de la app iOS
docs/               # proceso, specs y decisiones
```

---

## Documentación

- **[docs/PROCESS.md](docs/PROCESS.md)** — cómo se trabaja en este proyecto:
  Spec-Driven Development y el flujo de trabajo con Git.
- **[docs/specs/](docs/specs/)** — una carpeta por funcionalidad, con su spec, su diseño y
  sus tareas.
- **[CHANGELOG.md](CHANGELOG.md)** — qué ha ido entrando en cada versión.
- **[AGENTS.md](AGENTS.md)** — contexto técnico para asistentes de IA.

> Antes de escribir código para una funcionalidad nueva, lee `docs/PROCESS.md`: primero se
> acuerda la spec, después el diseño, y solo entonces se implementa.
