# Changelog

Todos los cambios notables de AparK se documentan aquí.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).
Este proyecto usa [Spec-Driven Development](docs/PROCESS.md); las specs viven en
[`docs/specs/`](docs/specs/).

## [Unreleased]

### Added
- **Crear vehículo**: nueva pantalla completa (destino Navigation 3) para añadir un
  vehículo con nombre (obligatorio) y matrícula (opcional). Incluye `CreateVehicleUseCase`,
  `AddVehicleViewModel` (MVI) y `AddVehicleScreen`. Los vehículos se crean sin ubicación.
- Estado vacío **"Aún no aparcado"** en la tarjeta de vehículo cuando no tiene ubicación.
- Proceso de trabajo **Spec-Driven Development**: `docs/PROCESS.md`, plantillas en
  `docs/specs/_template/` y este `CHANGELOG.md`.

### Changed
- Rediseño de la `AddVehicleCard` acorde a Material 3.
- Regla `create` de la colección `vehicles` endurecida (dueño, longitud de nombre,
  `sharedUsers` vacío, `lastLocation` nulo), con accesos defensivos `get()`. Desplegada
  a las bases de datos `(default)` y `apark-at`.

### Fixed
- Los vehículos sin ubicación mostraban el literal **"kotlin.Unit"** en la tarjeta
  (rama `else` vacía en `DynamicTimeText`).
- Crash / vehículo que no aparecía hasta reiniciar al crear uno nuevo: el listener del
  snapshot se enganchaba antes de que el batch confirmara en el servidor, provocando
  `PERMISSION_DENIED`. Se añadió `retryWhen` + `catch` en `getVehiclesForUser`, que
  además hace el stream resiliente a IDs de vehículo colgantes.
