# Tasks: las incidencias de un vehículo

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## Reglas y backend
- [ ] Las reglas de `incidents`, con sus tests en `tools`
- [ ] `deleteVehicleIncidents` en `cleanupVehicleReferences`

## Dominio y datos
- [ ] `IncidentCategory`, `IncidentTag`, `fromCode` y `appliesTo`
- [ ] `Incident` e `incidentsView`
- [ ] `IncidentRepository`, `FirestoreIncidentRepository`, los casos de uso y Koin
- [ ] Tests en `commonTest`

## Interfaz
- [ ] `ParkedText` generalizado para las frases de tiempo
- [ ] `IncidentsViewModel`
- [ ] La sección del detalle, con resolver, Deshacer, Resueltas y borrar
- [ ] El formulario de reportar
- [ ] Textos en `values`, `values-en` y `values-fr`
- [ ] La siembra con incidencias

## Documentación
- [ ] AGENTS, DATA-MODEL, FIREBASE (la caducidad), ROADMAP y CHANGELOG

## Verificación
- [ ] Tests de `tools` y `commonTest`; compila Android, iOS-Kotlin y Xcode
- [ ] **(1)(2)(3)** Ver: las abiertas, quién, sin nombre, sin incidencias
- [ ] **(4)(5)(6)** Reportar, con las etiquetas según el vehículo
- [ ] **(7)(8)(9)(10)** Resolver, Deshacer, Resueltas, y la de más de 30 días que no sale
- [ ] **(11)** Borrar: quien la reportó o el dueño
- [ ] **(12)(13)(14)** Reglas, y el tipo desconocido
- [ ] **(15)** Borrar el vehículo borra sus incidencias
- [ ] **(16)** Sin conexión
- [ ] **(17)** En inglés y en francés
- [ ] El aspecto, Android y la caducidad real — **Alberto**
