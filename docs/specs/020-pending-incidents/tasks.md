# Tasks: lo pendiente de los vehículos, a la vista

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## Domain
- [x] `PendingIncidents.kt`: `OpenIncidents`, `pendingOf`, `openCount`; y `openNewestFirst` en `Incident.kt`
- [x] `ObserveOpenIncidentsUseCase`, en Koin
- [x] Tests: `PendingIncidentsTest` y `ObserveOpenIncidentsTest`

## Presentation
- [x] `SectionTitle` a `components`; `IncidentTypeIcon` y `TimeAgoSentences` en `IncidentUi.kt`
- [x] `cardIncidents`, con sus tests
- [x] `VehiclesViewModel` y «Pendiente» en `VehiclesScreen`; reordenar por clave
- [x] `HomeViewModel` y los iconos de la tarjeta
- [x] `PendingCountViewModel` y la chapita en la barra
- [x] Textos en es, en y fr

## Documentación
- [x] `NAVIGATION.md`, `DATA-MODEL.md`, `ROADMAP.md` y `AGENTS.md`
- [x] `CHANGELOG.md`

## Verificación
- [x] Tests en la JVM; compila Android, iOS-Kotlin, los tests en iOS y Xcode
- [x] Los textos nuevos, en el `.cvr` de los tres idiomas
- [x] **(1)(2)(3)(4)** «Pendiente»: orden, vehículo y cuándo, tocar abre el detalle; «Todo en orden»; ordenar sigue funcionando, y la sección vuelve arriba al terminar. Al probarlo salió que la lista se abría con la sección tapada bajo la barra: `LazyColumn` mantenía la fila de arriba al aparecer «Pendiente» encima. Se arregla con `requestScrollToItem(0)`
- [x] **(5)(6)(7)** La tarjeta: tres iconos y «+N», sin cambiar de alto; el lector dice «Flat tire, Low fuel, Silla del bebé desmontada and 2 more». Con avatar e iconos, un nombre largo se queda en «Coche …»
- [x] **(8)(9)** La chapita, desde cualquier pestaña y desde un detalle; baja al resolver. El lector no la anunciaba —`NavigationBarItem` borra la semántica del icono—, así que la descripción pasó a la etiqueta: «Vehicles, Open incidents: 6»
- [x] **(10)** En vivo: Edu reporta y se resuelve por REST, y la sección, la tarjeta y la chapita cambian solas
- [x] **(11)** Sin conexión, en Android en modo avión: «No arranca» sale al momento en la sección y en la chapita, y llega al emulador al volver la red
- [x] **(12)** Un tipo desconocido
- [x] **(13)** Sacarme de un vehículo por REST: sus incidencias desaparecen de la sección, la tarjeta y la chapita, sin cierre. No hubo carrera: ni iOS ni Android registran `Listen … failed`, que sí registran cuando se deniega la lectura
- [x] **(14)** Con la lectura de `incidents` denegada solo en el emulador: «No se han podido cargar todas las incidencias», sin chapita, el resto funciona
- [x] **(15)** iOS y Android; inglés y tema oscuro en el simulador; el francés, en el `.cvr`
- [x] **Cambio de Alberto al probarla:** «Ordenar» pasa de la barra superior al título «Tus vehículos», y «Hecho» sale en el mismo sitio. Comprobado en el simulador: al ordenar, «Pendiente» se oculta y el botón queda arriba; al terminar, la sección vuelve arriba
- [x] El aspecto en el móvil — **Alberto**: bien, salvo que en la tarjeta no cabe todo en una línea. Darle una vuelta a la tarjeta va en una spec aparte
