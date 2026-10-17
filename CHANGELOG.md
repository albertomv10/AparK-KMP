# Changelog

Todos los cambios notables de AparK se documentan aquí.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).
Este proyecto usa [Spec-Driven Development](docs/PROCESS.md); las specs viven en
[`docs/specs/`](docs/specs/).

## [Unreleased]

### Added
- **Pestañas que navegan de verdad** ([spec 009](docs/specs/009-tab-navigation/spec.md)): las tres
  pestañas de abajo dejan de ser decorado. La navegación pasa a **dos capas** —una pila de fuera
  con la autenticación, y un shell con **una pila por pestaña**—, así que cada pestaña recuerda por
  dónde iba y la barra no puede aparecer en el login, porque vive dentro del shell. Estrena dos
  pantallas: *Vehículos*, la lista, y ***Cuenta***, que por fin le da interfaz al **selector de
  tema** que quedó cableado sobre DataStore sin nadie que lo escribiera, y donde se muda el
  **cerrar sesión** que estaba escondido en un menú de tres puntos sobre el mapa.
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
- **El mapa de Android ya no enseña el hueco mientras se reconstruye.** Volver a su pestaña rehace
  el `MapView` nativo, que es una View y se destruye al salir de la composición. Mientras dibuja su
  primer fotograma hay un velo con un indicador de carga, que es lo que hacen las apps que tienen
  el mismo problema. En iOS no ocurre, porque allí el estado del mapa vive en Swift.
- **Cambiar de pestaña ya no desliza una pantalla sobre otra.** Deslizar lateralmente sugiere que
  se avanza, y entre pestañas no se avanza: son tres sitios al mismo nivel. Ahora es un fundido
  corto, y la navegación de dentro de cada pestaña conserva la transición propia de la plataforma.
  Dura distinto en cada sistema: en Android acompaña además a la reconstrucción del mapa, y en iOS,
  que no la tiene, se queda en lo justo para no ser un corte seco.
- **ViewModels con ámbito de pantalla** ([spec 006](docs/specs/006-scoped-viewmodels/spec.md)):
  `NavDisplay` no traía el decorador de `ViewModelStore`, así que **ningún ViewModel se destruía
  nunca** y cada pantalla arrastraba el estado de su visita anterior. Añadirlo exigía subir
  `lifecycle` a **2.10.0**, la primera versión que publica el artefacto para Compose Multiplatform
  y que sigue apuntando a Compose 1.9.3. Con eso desaparecen los parches que cada pantalla tenía
  que ponerse, y los formularios de login y registro dejan de conservar lo escrito sin tocarlos.
- Rediseño de la `AddVehicleCard` acorde a Material 3.
- Regla `create` de la colección `vehicles` endurecida (dueño, longitud de nombre,
  `sharedUsers` vacío, `lastLocation` nulo), con accesos defensivos `get()`. Desplegada
  a las bases de datos `(default)` y `apark-at`.

### Fixed
- **Los apóstrofos salían con una barra invertida delante.** `composeResources` no es `res/` de
  Android: su plugin sí convierte `\n` en salto de línea, pero `\'` lo copia tal cual, así que la
  app mostraba `You don\'t have any vehicles yet`. Afectaba a **catorce** cadenas, trece de ellas
  en francés, donde llevaban meses sin que nadie las leyera.
- **En iPhone, las tarjetas tapaban el logo de Google Maps.** El `-30.0` de `AparKMap.ios.kt`
  compensaba que el mapa se dibujara por detrás de la barra de pestañas, cuando la barra vivía
  dentro de la pantalla del mapa. Al sacarla fuera, el mapa dejó de llegar hasta ahí y esa resta
  pasó a meter el logo debajo de las tarjetas. Las dos plataformas pasan ya el mismo valor.
- **En iOS el mapa salía siempre oscuro, mirase lo que mirase la app.** Desde su versión 9, el SDK
  de Google Maps adapta el mapa al modo oscuro **del sistema** por su cuenta, así que llevaba
  oscuro sin que nadie lo hubiera programado, y quitarle el estilo lo devolvía a ese oscuro en vez
  de aclararlo. Ahora se le fija el `overrideUserInterfaceStyle` según el tema de la app, y el JSON
  del estilo —el mismo que usa Android— pasa a vivir en código común.
- **El mapa no seguía al tema de la app.** Preguntaba por el del **sistema**, cosa que daba igual
  mientras no hubo selector: desde que lo hay, con el móvil en oscuro y la app en claro el mapa se
  quedaba negro. Ahora el tema publica cuál ha resuelto y el mapa lee eso.
- **Los tests comunes no compilaban para iOS, y nadie se enteraba.** Kotlin/Native rechaza nombres
  entre acentos graves que la JVM acepta —basta una coma—, y la CI solo ejecutaba los de JVM, así
  que dos tests del repositorio de ajustes llevaban rotos desde que se escribieron sin que ningún
  job se pusiera rojo. Renombrados, y la CI pasa a **compilarlos también para iOS**.
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
