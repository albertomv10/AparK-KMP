# Changelog

Todos los cambios notables de AparK se documentan aquí.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).
Este proyecto usa [Spec-Driven Development](docs/PROCESS.md); las specs viven en
[`docs/specs/`](docs/specs/).

## [Unreleased]

### Added
- **Lo pendiente de los vehículos, a la vista** ([spec 020](docs/specs/020-pending-incidents/spec.md)):
  las incidencias ya no se ven solo abriendo cada vehículo.
  - Arriba en *Vehículos*, **«Pendiente»**: las abiertas de todos tus vehículos, la más reciente
    primero, con su icono, el vehículo y cuándo. Tocar una abre su detalle. Sin ninguna, «Todo en
    orden»; si no se han podido cargar, lo dice en lugar de afirmarlo.
  - En la **tarjeta del mapa**, un icono por tipo entre el nombre y la flecha, como mucho tres y
    después «+N», sin cambiar el alto de la tarjeta.
  - En la pestaña *Vehículos*, una **chapita** con las incidencias abiertas, visible desde cualquier
    pestaña. Cuenta lo que está sin resolver, no lo que no has visto: solo baja al resolver o borrar.
  - Todo en tiempo real y sin conexión, como el detalle.
  - «Ordenar» pasa de la barra superior al título «Tus vehículos», que es lo que ordena; «Hecho»
    sale en el mismo sitio.
- **Las incidencias de un vehículo** ([spec 019](docs/specs/019-incidents/spec.md)): en el detalle,
  cualquier miembro puede reportar qué le pasa al vehículo.
  - Se elige qué le pasa de una lista separada por categorías —niveles y energía, mecánica y
    testigos, neumáticos, carrocería e interior—, cada una con su color, y si no encaja nada, una
    «Personalizada» con su propio nombre. La nota es opcional. Lo que se ofrece depende del vehículo: un eléctrico no tiene «Poco
    combustible», y «AdBlue» solo sale en un diésel.
  - Todos los miembros la ven al momento, con quién la reportó y cuándo.
  - Cualquiera la marca como resuelta, con «Deshacer». Las resueltas se ven 30 días en una sección
    plegada y después se borran solas.
  - Quien la reportó, o el dueño, puede borrarla si fue un error.
  - Funciona sin conexión, y borrar el vehículo borra sus incidencias.
- **Mover un vehículo manteniendo pulsado el mapa** ([spec 017](docs/specs/017-map-camera/spec.md)):
  el vehículo elegido pasa al punto que mantienes pulsado, el móvil vibra y sale el aviso con
  «Deshacer», igual que al arrastrar su marcador. Si nunca se había aparcado, queda aparcado ahí.
- **Tocar el marcador de un vehículo lo elige**: el carrusel va a su tarjeta.
- **«Ir al vehículo»**: un botón con el icono de su tipo, encima del de centrar y del mismo tamaño.
  Vuelve al vehículo elegido después de mover el mapa, y de cerca, para ver exactamente dónde está.
- **Ordenar los vehículos desde su lista** ([spec 016](docs/specs/016-vehicle-cards/spec.md)): en
  *Vehículos*, «Ordenar» enseña un asa en cada fila, y se arrastra un vehículo directamente a su
  sitio viéndolos todos, en vez de moverlo de uno en uno en el carrusel. «Hecho» lo guarda una sola
  vez, y **funciona sin conexión**: antes cada movimiento era una transacción, que sin red fallaba.
  Con un lector de pantalla, cada fila tiene «Subir» y «Bajar». El carrusel del mapa sigue en el
  vehículo que enseñaba aunque cambie de posición.
- **Los datos del vehículo, y editarlo** ([spec 015](docs/specs/015-vehicle-data/spec.md)): cada
  vehículo puede llevar su **tipo**, su **marca y modelo**, el **sistema y el combustible**, el
  **color** y la **etiqueta ambiental de la DGT**, todo opcional. Al crearlo se eligen primero la
  marca y el modelo, con un buscador que sugiere mientras se escribe —«skoda» encuentra «Škoda»—, y
  el nombre se rellena solo, aunque sigue siendo editable y nunca se pisa uno puesto a mano. Lo que
  no está en la lista se escribe tal cual. El combustible depende del sistema: un eléctrico no
  pregunta, uno de pila de combustible es de hidrógeno. El **dueño** puede **editar** el vehículo
  desde el lápiz del detalle, que ya enseña todos sus datos; la tarjeta del mapa y la lista de
  *Vehículos* llevan un icono del tipo sobre el color, para distinguirlos de un vistazo. Las reglas
  validan cada código y la longitud de cada texto, y un vehículo con un valor que esta versión no
  conoce lo enseña como «Otro». La lista de marcas viene de json-car-list (MIT), con su aviso dentro
  de la app, y de una lista propia con las que le faltan y las motos.
- **El detalle del vehículo, de verdad** ([spec 014](docs/specs/014-vehicle-detail-members/spec.md)):
  dónde está aparcado, quién lo dejó y cuándo, con un botón para verlo en el mapa, y **con quién se
  comparte**, marcando al dueño. El dueño puede **quitar** a un miembro y **pasarle la propiedad**,
  que era lo que el diálogo de borrar pedía sin que existiera dónde. El resto puede **salirse**
  desde aquí también. Si te quitan o lo borran mientras lo miras, la pantalla lo dice en vez de
  fallar, y si te pasan la propiedad, las acciones de dueño aparecen solas. Los nombres los da una
  función nueva, `getVehicleMembers`, porque nadie puede leer el perfil de otro; quien no tiene
  nombre sale con su email, como en «Aparcado por». Las reglas ahora solo dejan pasar la propiedad a
  un miembro, y el dueño ya no puede añadir a nadie: se entra solo con un código.
- **Eliminar la cuenta** ([spec 012](docs/specs/012-delete-account/spec.md)), el último bloqueante
  de tienda de la fase 1: desde *Cuenta*, debajo de cerrar sesión. Antes de pedir nada, la pantalla
  enseña qué vehículos se borrarán, cuáles pasarán al miembro que lleva más tiempo y de cuáles
  saldrás. Después pide la contraseña, o volver a pasar por Google. El trabajo lo hace una callable
  nueva, `deleteAccount`, que comprueba que el login es reciente, limpia Firestore —vehículos,
  invitaciones, el documento del usuario— y borra la cuenta de Auth lo último, para que un fallo a
  medias se pueda reintentar. En las tarjetas de los demás se conserva dónde quedó el vehículo, pero
  no quién lo aparcó, y la línea «Aparcado por» desaparece en vez de quedarse vacía. Las cuentas de
  Apple esperan a la cuenta de pago de Apple Developer.
- **La cuenta, editable** ([spec 011](docs/specs/011-account-profile/spec.md)): desde *Cuenta* se
  cambia el **nombre** —el que ven los demás miembros en las tarjetas—, el **email** y la
  **contraseña**. Los dos últimos piden siempre la contraseña actual, porque Firebase exige un login
  reciente y no deja saber de antemano si lo es. Cambiar el email manda un enlace a la dirección
  nueva y no cambia nada hasta que se abre; entonces Firebase cierra la sesión, y la app vuelve al
  login. Quien entró con Google o Apple puede cambiar su nombre, y se le explica por qué no ve lo
  demás. Las copias del nombre que llevan los vehículos las mantiene una función nueva,
  `propagateUserProfile`: el cliente solo escribe `users/{uid}.name`.
- **Emuladores de Firebase en local** ([spec 010](docs/specs/010-firebase-emulators/spec.md)): una
  build de debug puede hablar con los emuladores de Auth, Firestore y Functions de tu máquina en
  lugar de con `apark-dev`, poniendo su dirección en `local.properties` o en `Secrets.xcconfig`. Un
  script siembra dos cuentas y tres vehículos —uno compartido entre ambas—, iguales en cada
  arranque. Sirve para probar casos incómodos sin ensuciar nada, y sobre todo para que el asistente
  pueda **entrar en la app y mirar por sí mismo**, que hasta ahora no podía: todo lo que importa
  está detrás del inicio de sesión. Una build de release no puede encenderlos.
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
- **La tarjeta del mapa, una fila para cada cosa** ([spec 022](docs/specs/022-vehicle-card-layout/spec.md)):
  arriba el vehículo, con su nombre entero; en medio, cuándo y quién lo aparcó —«Hace 5 min · Edu»—
  y los iconos de lo que tiene pendiente; abajo, «Aparcar». Antes los iconos compartían fila con el
  nombre y lo cortaban. Sin la flecha: toda la tarjeta abre el detalle. La tarjeta «+» ya mide lo
  mismo que las demás; se quedaba 8 dp más baja.
- **El splash de Android, como el de iOS** ([spec 018](docs/specs/018-splash/spec.md)): fondo blanco
  y el logo nítido en el centro, también en modo oscuro (salvo en Xiaomi con HyperOS, que oscurece el
  fondo por su cuenta). Ahora usa la librería de splash de Android,
  así que sale igual de Android 7 a 15; antes, de la 7 a la 11, no salía el logo. En iOS, el logo
  de la pantalla de arranque está centrado con restricciones, para cualquier tamaño de iPhone.
- **Una sola regla de cámara para Android e iOS**, decidida en código común
  ([spec 017](docs/specs/017-map-camera/spec.md)). La cámara solo se mueve al abrir el mapa, al
  elegir otro vehículo, cuando el elegido cambia de sitio o al pedirlo. El zoom de calle es 17 en las
  dos plataformas (antes 18 en una y 17 en la otra, y 16 o 15 al centrar). Al aparcar o mover el
  vehículo elegido, la cámara lo sigue de cerca, con zoom 20, para ver si el GPS acertó. Al abrir el
  mapa con un vehículo que nunca se aparcó, o sin vehículos, aparece en tu ubicación.
- **La tarjeta del mapa, rehecha** ([spec 016](docs/specs/016-vehicle-cards/spec.md)): una sola
  línea dice cuándo se aparcó y quién —«Aparcado hace 5 min por Edu», o «por ti»—, igual que la lista
  de *Vehículos* y el detalle; todas las tarjetas miden lo mismo, más bajas que antes y sin el hueco
  de cuando no está aparcado; una flecha dice que abre el detalle, y el lector de pantalla lo
  anuncia. Deja sitio para los iconos de las incidencias que vendrán.
- **Aparcar siempre confirma.** El aviso solo salía cuando había algo que deshacer, así que aparcar
  un vehículo por primera vez no daba ninguna señal.
- **La CI de iOS, de 14–35 minutos a la mitad, y a cero cuando no toca la app.** Casi todo el tiempo
  era enlazar el framework de Kotlin, una vez para Debug y otra para Release, en la misma máquina y
  una detrás de otra. Ahora van en paralelo en dos máquinas, y validan lo mismo que antes. Un job
  previo mira qué cambia el PR y salta iOS si todo es documentación, `functions/`, `tools/` o
  configuración de Firebase; ante cualquier fichero que no esté en esa lista, compila. Y los
  paquetes de Swift (~1,3 GB) se guardan en caché en vez de bajarse en cada ejecución. La variación
  entre 14 y 35 minutos no era del código: en las ejecuciones lentas todo iba 2,5 veces más
  despacio, también lo que no tiene nada que ver con Kotlin, así que es la máquina que toca.
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

### Removed
- **Cinco imágenes que no usaba nada o que ya no hacen falta**, entre ellas el PNG del splash de
  2000 px: unos 530 KB menos dentro de la app de Android.
- **El modo edición del carrusel**: la pulsación larga sobre una tarjeta ya no enseña la papelera
  ni las flechas. Eliminar un vehículo o quitarlo de tu lista se hace desde su detalle, lejos del
  botón de aparcar, y ordenar desde *Vehículos*. Una pulsación larga cuenta ahora como un toque.
- **La base de datos `apark-at` de producción**, de cuando desarrollo y producción compartían
  proyecto. Solo tenía datos de prueba.
- **El campo `sharedUsers` de los vehículos**, sustituido por `memberIds` en la
  [spec 008](docs/specs/008-vehicle-membership-model/spec.md). Era el último paso de esa migración,
  y con él se va la vía de respaldo que todavía lo leía en la función de limpieza de vehículos.

### Fixed
- **En iPhone, cambiar de tema ya no mueve las pantallas**
  ([spec 023](docs/specs/023-tab-insets-after-theme-change/spec.md)).
  - Al pasar el móvil a oscuro o a claro con el mapa abierto, el carrusel y el logo de Google subían
    34 pt, lo que mide el indicador de inicio, y se quedaban ahí hasta salir de la pantalla. Cambiar
    el tema desde *Cuenta* dejaba un hueco al final de *Cuenta*.
  - Las pantallas de dentro de las pestañas ya no reservan el indicador de inicio: lo hace solo la
    barra de pestañas, que es quien lo tiene debajo.
- **La debug de Android ya puede entrar contra los emuladores de Firebase.**
  - Android bloquea el HTTP sin cifrar desde la 9, los emuladores hablan HTTP y el proyecto nunca lo
    había permitido. Así que el inicio de sesión fallaba con «Ha ocurrido un error inesperado».
  - Ahora se permite solo en la debug apuntada a los emuladores (`APARK_EMULATOR_HOST`). La debug
    normal y la release siguen sin HTTP.
- **La app ya no se cierra cuando una escucha de Firestore falla**
  ([spec 021](docs/specs/021-session-listener-errors/spec.md)).
  - Si la sesión terminaba desde fuera —contraseña o email cambiados en otro sitio, cuenta
    deshabilitada o borrada—, las escuchas de *Inicio* y *Vehículos* podían recibir el rechazo antes
    de enterarse de que ya no había sesión. Nadie recogía ese error, y la app se cerraba, incluso al
    arrancar.
  - Ahora lleva al login, como debía.
  - Si una escucha falla con la sesión todavía viva, la app avisa con «No se han podido cargar tus
    vehículos», se queda con lo que ya enseñaba, y el fallo llega a Crashlytics.
  - Cuando la sesión termina, ya no se registra como fallo, tampoco desde el detalle ni desde editar.
- **El logo del splash de Android se veía con los bordes dentados.** Era un PNG de 2000 px que Android
  ampliaba y luego reducía mucho; ahora es vectorial.
- **La cámara del mapa volvía sola al vehículo elegido** cada vez que cambiaba cualquier dato: que
  otro miembro aparcara otro coche, un nombre o el orden. Si estabas mirando los alrededores, te
  devolvía de golpe y perdías el zoom.
- **Elegir un vehículo que nunca se aparcó dejaba la cámara enseñando el anterior.** Ahora va a tu
  ubicación, que es donde lo dejaría «Aparcar».
- **En Android, centrar no hacía nada** si el sistema no tenía una ubicación guardada. Ahora la
  pide.
- **En francés, la tarjeta decía «Pour Edu»** en vez de quién había aparcado. Lo sustituye la frase
  nueva, «Garé il y a 5 min par Edu».
- **No queda ningún texto en castellano escrito a fuego.** El aviso de permiso de ubicación
  denegado y su botón «Ajustes», y el texto del botón «Continuar con Apple», salían en castellano
  con la app en inglés o en francés; ahora van en los tres idiomas. El aviso de permiso, además,
  tiene prioridad sobre el de «deshacer»: si llegaba con este todavía en pantalla, salía con el
  botón «Deshacer», que en realidad abría los ajustes.
- **Los lectores de pantalla ya no leen dos veces los botones de Google y Apple.** Sus iconos son
  decorativos, porque el texto del botón ya dice qué es, y la animación de verificar el email no
  cuenta nada. Los cuatro tenían una descripción escrita a fuego, en castellano o en inglés; ahora
  TalkBack y VoiceOver los saltan.
- **El CI de iOS ya no falla a ratos con `OutOfMemoryError` al enlazar Release.** El compilador de
  Kotlin/Native corre dentro del daemon de Gradle, así que el techo que cuenta es el de ese daemon,
  y estaba en 4 GB, justo en el límite de lo que pide el LTO. La propiedad que se añadió en octubre
  para «darle memoria propia», `kotlin.native.jvmArgs`, no se aplica en ese modo: no hizo nada
  nunca. Sale, y el daemon pasa a 6 GB, que es lo que mostró una medición del recolector para que
  el pico no necesite compactaciones de emergencia.
- **Los correos de Firebase llegan en el idioma de la app**, y no en inglés. Verificar la cuenta,
  recuperar la contraseña y confirmar un email nuevo salen en el idioma en que se está viendo la
  app: la app nunca se lo decía al SDK, y sin eso Firebase usa el inglés.
- **Los usuarios nuevos nacen con `createdAt` y `updatedAt`.** Se guardaban a 0, porque quien crea el
  documento le pasaba un `User` recién construido. Reordenar los vehículos ahora también pone al
  día `updatedAt`.
- **Un miembro que cambiaba de email ya no se quedaba sin poder aparcar.** Al aparcar, la app
  firmaba con el email del documento `users`, que nadie actualizaba, mientras la regla lo compara
  con el del token; el dueño no lo notaba, pero un miembro compartido recibía PERMISSION_DENIED
  para siempre. Ahora firma con el email del token, y el documento se pone al día al entrar.
- **La app ya no se queda colgada cuando la sesión termina por su cuenta.** Solo salía al login el
  botón de cerrar sesión; una sesión revocada —por cambiar el email o la contraseña en otro
  sitio— dejaba unas pestañas en las que todo fallaba. Ahora hay una única salida, que escucha el
  estado de la sesión.
- **El registro ya no enseña el error crudo de Firebase**, el mismo arreglo que el login: «Ese email
  ya tiene una cuenta» aparece en su campo.
- **El login ya no enseña el error crudo de Firebase.** Cuando fallaba el inicio de sesión con
  email, la pantalla volcaba el `NSError` entero —medio pantallazo de `FIRAuthErrorDomain
  Code=17995 ... SecItemAdd (-34018)`—, y no por descuido: la traducción de mensajes decía
  explícitamente que lo que no reconociera se enseñara tal cual. Ahora el fallo llega como motivo y
  no como texto, igual que ya hacía el login social, y el detalle técnico va a Crashlytics.
- **Los listeners de Firestore ya no se reinician cada hora.** La Home observa el estado de auth
  para que los datos cambien con el usuario, pero `authStateChanges` emite también al refrescarse
  el token —cada hora aproximadamente—, no solo al entrar o salir. Como el `flatMapLatest` no
  filtraba el uid, cada refresco cancelaba las dos suscripciones de `getVehiclesForUser` y las
  volvía a abrir **para el mismo usuario**: una relectura completa a cambio de nada. Un
  `distinctUntilChanged()` sobre el uid deja que se reinicie solo cuando cambia de verdad. La
  pestaña de vehículos, que observa el estado de auth igual, lleva el mismo filtro.
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
