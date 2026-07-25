# Changelog

Todos los cambios notables de AparK se documentan aquí.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).
Este proyecto usa [Spec-Driven Development](docs/PROCESS.md); las specs viven en
[`docs/specs/`](docs/specs/).

## [Unreleased]

### Added
- **Eliminar vehículo** ([spec 001](docs/specs/001-delete-vehicle/spec.md)): manteniendo
  pulsada una tarjeta se activa un **modo edición** desde el que el **dueño puede eliminar**
  el vehículo (desaparece para todos) y un **miembro compartido puede quitárselo** de su
  lista (el vehículo sobrevive). Ambas acciones piden confirmación, con un texto que avisa
  al dueño de que el borrado es total e irreversible y le sugiere transferir la propiedad.
- Nuevo componente reutilizable `AparKConfirmDialog` (primer diálogo del proyecto).
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
- Un usuario con el que se comparte un vehículo no podía quitárselo de su lista: las reglas
  solo le permitían modificar `lastLocation`, así que la operación se rechazaba con
  `PERMISSION_DENIED`. Se añadió una rama a la regla `allow update` que permite a un miembro
  salirse a sí mismo (y solo a sí mismo) de `sharedUsers`.
- Posible `IndexOutOfBoundsException` en el carrusel de la Home: el pager accedía a la lista
  de vehículos por índice sin protección y no acotaba la página seleccionada, por lo que al
  encoger la lista (p. ej. al eliminar) podía apuntar fuera de rango.
- Los vehículos sin ubicación mostraban el literal **"kotlin.Unit"** en la tarjeta
  (rama `else` vacía en `DynamicTimeText`).
- Crash / vehículo que no aparecía hasta reiniciar al crear uno nuevo: el listener del
  snapshot se enganchaba antes de que el batch confirmara en el servidor, provocando
  `PERMISSION_DENIED`. Se añadió `retryWhen` + `catch` en `getVehiclesForUser`, que
  además hace el stream resiliente a IDs de vehículo colgantes.
