# Tasks: las tarjetas del mapa, y ordenar los vehículos desde su lista

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## Ordenar desde *Vehículos*
- [x] Reorderable 3.1.0 en el catálogo de versiones y en `commonMain`
- [x] `setVehicleOrder` en el repositorio, en lugar de `moveUserVehicle`; `SetVehicleOrderUseCase` y Koin
- [x] `inOrder` y `moved`, con sus tests
- [x] `VehiclesViewModel`: el modo, el override y guardar sin esperar
- [x] `VehiclesScreen`: «Ordenar» y «Hecho», las asas, «Atrás», el snackbar, y «Subir» y «Bajar» para el lector de pantalla

## El carrusel y la tarjeta
- [x] Fuera el modo edición: la pantalla, el ViewModel, sus eventos y sus claves
- [x] El carrusel sigue en el mismo vehículo cuando cambia el orden
- [x] Aparcar por primera vez confirma, sin «Deshacer»
- [x] La tarjeta nueva: misma estructura, flecha, «Ver detalles» y el spinner dentro del botón; la tarjeta «+» a juego
- [x] `ParkedText` y `whoParked`, con sus tests, en la tarjeta, la lista y el detalle; fuera `DynamicTimeText`
- [x] Textos en `values`, `values-en` y `values-fr`; fuera los que ya no se usan

## Documentación
- [x] `AGENTS.md`, menciones a reordenar en `ROADMAP.md` y `DATA-MODEL.md`
- [x] Entrada en `CHANGELOG.md`

## Verificación
- [x] Compila Android, iOS-Kotlin, tests JVM y nativos
- [x] **(1)(2)** La pulsación larga ya no abre ningún modo: cuenta como un toque; el detalle sigue eliminando y quitando
- [x] **(3)(4)** Ordenar arrastrando; el carrusel tiene el mismo orden, sigue en su vehículo y se mantiene al relanzar
- [x] **(5)** Sin conexión, el orden se ve y no sale ningún error
- [x] **(7)(8)** El orden de otro miembro no cambia; un vehículo nuevo sale al final
- [x] **(9)(10)(11)(14)** Una línea con el cuándo y el quién; sin huecos; misma altura; el nombre largo se corta antes de la flecha
- [x] **(13)** El primer aparcamiento confirma
- [x] **(15)** En inglés y en francés
- [x] El aspecto y Android — **Alberto**
- [ ] **(6)(12)** TalkBack y VoiceOver: «Ver detalles» en la tarjeta, y «Subir» y «Bajar» al ordenar — **Alberto**
