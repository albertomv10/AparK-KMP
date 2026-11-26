# Tasks: lo pendiente de los vehículos, a la vista

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## Domain
- [ ] `PendingIncidents.kt`: `OpenIncidents`, `pendingOf`, `openCount`; y `openNewestFirst` en `Incident.kt`
- [ ] `ObserveOpenIncidentsUseCase`, en Koin
- [ ] Tests: `PendingIncidentsTest` y `ObserveOpenIncidentsTest`

## Presentation
- [ ] `SectionTitle` a `components`; `IncidentTypeIcon` y `TimeAgoSentences` en `IncidentUi.kt`
- [ ] `cardIncidents`, con sus tests
- [ ] `VehiclesViewModel` y «Pendiente» en `VehiclesScreen`; reordenar por clave
- [ ] `HomeViewModel` y los iconos de la tarjeta
- [ ] `PendingCountViewModel` y la chapita en la barra
- [ ] Textos en es, en y fr

## Documentación
- [ ] `NAVIGATION.md`, `DATA-MODEL.md`, `ROADMAP.md` y `AGENTS.md`
- [ ] `CHANGELOG.md`

## Verificación
- [ ] Tests en la JVM; compila Android, iOS-Kotlin, los tests en iOS y Xcode
- [ ] Los textos nuevos, en el `.cvr` de los tres idiomas
- [ ] **(1)(2)(3)(4)** «Pendiente»: orden, vehículo y cuándo, tocar abre el detalle; «Todo en orden»; ordenar sigue funcionando
- [ ] **(5)(6)(7)** La tarjeta: tres iconos y «+1», sin cambiar de alto; la descripción para el lector
- [ ] **(8)(9)** La chapita, desde cualquier pestaña y desde un detalle
- [ ] **(10)** En vivo: otro miembro reporta y resuelve
- [ ] **(11)** Sin conexión
- [ ] **(12)** Un tipo desconocido
- [ ] **(13)** Sacarme de un vehículo, y la carrera de los riesgos
- [ ] **(14)** Un fallo con la sesión viva
- [ ] **(15)** iOS y Android; inglés y tema oscuro
- [ ] El aspecto en el móvil — **Alberto**
