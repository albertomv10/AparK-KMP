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

### Firmar la versión de release (Android)

Para generar un artefacto **firmado** hay que añadir cuatro propiedades a `local.properties`. Están
fuera del repositorio a propósito, y el fichero ya está en `.gitignore`:

```properties
RELEASE_STORE_FILE=/ruta/absoluta/a/tu/keystore.jks
RELEASE_STORE_PASSWORD=…
RELEASE_KEY_ALIAS=…
RELEASE_KEY_PASSWORD=…
```

También se aceptan como **variables de entorno** con esos mismos nombres, que es como las recibe CI.

Si faltan, la build de release **no se firma en vez de fallar**: compilar el proyecto sigue
funcionando en cualquier máquina, y la salida se llama `composeApp-release-unsigned.apk`, que avisa
sola. Lo que sí produce un aviso ruidoso es dejarlas **a medias**, porque eso casi siempre es un
descuido.

---

## Compilar y ejecutar

**Android**

```sh
./gradlew :composeApp:assembleDebug     # generar APK de debug
./gradlew :composeApp:installDebug      # instalar en el dispositivo/emulador conectado
./gradlew :composeApp:bundleRelease     # generar el AAB firmado que pide Google Play
```

Google Play **solo acepta AAB**, no APK. Con ese formato la clave de firma de la app la custodia
Google y tu keystore pasa a ser la *clave de subida*: si la pierdes, Google puede reemplazarla y
sigues publicando. Para repartir builds por Firebase App Distribution el APK de
`assembleRelease` sigue valiendo.

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
- **[docs/ROADMAP.md](docs/ROADMAP.md)** — revisión de la arquitectura y plan por fases hasta
  publicar en las tiendas.
- **[docs/specs/](docs/specs/)** — una carpeta por funcionalidad, con su spec, su diseño y
  sus tareas.
- **[CHANGELOG.md](CHANGELOG.md)** — qué ha ido entrando en cada versión.
- **[AGENTS.md](AGENTS.md)** — contexto técnico para asistentes de IA.

> Antes de escribir código para una funcionalidad nueva, lee `docs/PROCESS.md`: primero se
> acuerda la spec, después el diseño, y solo entonces se implementa.
