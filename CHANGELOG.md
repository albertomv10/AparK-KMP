# Changelog

Todos los cambios notables de AparK se documentan aquí.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).
Este proyecto usa [Spec-Driven Development](docs/PROCESS.md); las specs viven en
[`docs/specs/`](docs/specs/).

## [Unreleased]

### Added
- **Compartir vehículo** ([spec 005](docs/specs/005-share-vehicle/spec.md)): el dueño genera una
  invitación de **un solo uso que caduca en 24 h**, y puede copiarla o enviarla con la hoja de
  compartir del sistema; otra persona se une desde la nueva pestaña *Unirme* de añadir vehículo.
  Volver a compartir **revoca** el código anterior. Incluye una pantalla de detalle provisional
  (solo con compartir) que además estrena la navegación a detalle, hasta ahora sin cablear.
- **Limpieza automática de referencias** ([spec 002](docs/specs/002-vehicle-cleanup-function/spec.md)):
  primera Cloud Function del proyecto. Al borrarse un vehículo, quita su id del `userVehicles` de
  **todos** sus miembros —algo que el cliente no puede hacer, porque las reglas le impiden
  escribir en el documento de otro usuario—, evitando así los ids colgantes que penalizaban el
  arranque de los demás. Desplegada para las dos bases de datos. Con esto el proyecto estrena
  backend: `functions/` en TypeScript.
- **Reordenar vehículos** ([spec 003](docs/specs/003-reorder-vehicles/spec.md)): en modo
  edición, cada tarjeta muestra flechas ◀ ▶ para moverla una posición. El carrusel sigue al
  vehículo movido, de modo que se puede pulsar la misma flecha repetidamente. El orden es
  **por usuario** (vive en su propio `userVehicles`), así que no afecta a los demás miembros
  de un vehículo compartido, y se guarda dentro de una **transacción** para no perder
  vehículos añadidos entre medias desde otro dispositivo.
- **Eliminar vehículo** ([spec 001](docs/specs/001-delete-vehicle/spec.md)): manteniendo
  pulsada una tarjeta se activa un **modo edición** desde el que el **dueño puede eliminar**
  el vehículo (desaparece para todos) y un **miembro compartido puede quitárselo** de su
  lista (el vehículo sobrevive). Ambas acciones piden confirmación, con un texto que avisa
  al dueño de que el borrado es total e irreversible y le sugiere transferir la propiedad.
- Nuevo componente reutilizable `AparKConfirmDialog` (primer diálogo del proyecto).
- En modo edición el mapa queda **bloqueado y atenuado**: no se puede desplazar ni arrastrar
  marcadores por accidente, y basta con tocarlo para salir del modo. El **gesto/botón atrás**
  también sale del modo edición (sin afectar a la navegación normal).
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
- **Las invitaciones ya no se acumulan para siempre**
  ([spec 007](docs/specs/007-invite-cleanup/spec.md)). La limpieza que había solo actuaba al volver
  a compartir el mismo vehículo, así que una invitación de un vehículo que no se comparte dos veces
  no se borraba nunca, **las usadas no se borraban jamás** —una por cada compartición con éxito— y
  al eliminar un vehículo las suyas quedaban huérfanas. Ahora una **política TTL** las retira siete
  días después de caducar, y el trigger de borrado de vehículo se lleva las suyas en el acto. La
  semana de gracia es deliberada: mientras el documento existe se dice *"ha caducado"*, que invita
  a pedir otra; cuando desaparece se dice *"no es válido"*, que hace pensar en un error al teclear.
- Los formularios de *Añadir vehículo* conservaban lo escrito en la visita anterior, y la
  pantalla de detalle podía reabrir el diálogo con el **código de invitación del vehículo
  anterior** bajo el nombre del nuevo. La causa es común: `NavDisplay` no trae el decorador de
  `ViewModelStore`, así que **ningún ViewModel se destruye** al navegar hacia atrás. Cada una de
  las dos pantallas parte ahora de un estado limpio al entrar.
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
