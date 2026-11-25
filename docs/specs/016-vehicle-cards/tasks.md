# Tasks: las tarjetas del mapa, y ordenar los vehículos desde su lista

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## Ordenar desde *Vehículos*
- [ ] Reorderable 3.1.0 en el catálogo de versiones y en `commonMain`
- [ ] `setVehicleOrder` en el repositorio, en lugar de `moveUserVehicle`; `SetVehicleOrderUseCase` y Koin
- [ ] `inOrder` y `moved`, con sus tests
- [ ] `VehiclesViewModel`: el modo, el override y guardar sin esperar
- [ ] `VehiclesScreen`: «Ordenar» y «Hecho», las asas, «Atrás», el snackbar, y «Subir» y «Bajar» para el lector de pantalla

## El carrusel y la tarjeta
- [ ] Fuera el modo edición: la pantalla, el ViewModel, sus eventos y sus claves
- [ ] El carrusel sigue en el mismo vehículo cuando cambia el orden
- [ ] Aparcar por primera vez confirma, sin «Deshacer»
- [ ] La tarjeta nueva: misma estructura, flecha, «Ver detalles» y el spinner dentro del botón; la tarjeta «+» a juego
- [ ] `ParkedText` y `parkedBy`, con sus tests, en la tarjeta, la lista y el detalle; fuera `DynamicTimeText`
- [ ] Textos en `values`, `values-en` y `values-fr`; fuera los que ya no se usan

## Documentación
- [ ] `AGENTS.md`, menciones a reordenar en `ROADMAP.md` y `DATA-MODEL.md`
- [ ] Entrada en `CHANGELOG.md`

## Verificación
- [ ] Compila Android, iOS-Kotlin, tests JVM y nativos
- [ ] **(1)(2)** La pulsación larga no hace nada; el detalle sigue eliminando y quitando
- [ ] **(3)(4)** Ordenar arrastrando; el carrusel tiene el mismo orden, sigue en su vehículo y se mantiene al relanzar
- [ ] **(5)** Sin conexión, el orden se ve y no sale ningún error
- [ ] **(7)(8)** El orden de otro miembro no cambia; un vehículo nuevo sale al final
- [ ] **(9)(10)(11)(14)** Una línea con el cuándo y el quién; sin huecos; misma altura; el nombre largo se corta antes de la flecha
- [ ] **(13)** El primer aparcamiento confirma
- [ ] **(15)** En inglés y en francés
- [ ] **(6)(12)** TalkBack y VoiceOver, el aspecto y Android — **Alberto**
