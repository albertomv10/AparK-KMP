# Design: Reordenar vehículos

- **Spec**: [spec.md](spec.md)
- **Estado**: Aprobado
- **Fecha**: 2026-07-25

## Enfoque

Botones **◀ ▶** en cada tarjeta durante el modo edición que mueven el vehículo una posición.
El movimiento se persiste reescribiendo el array `userVehicles` del documento del usuario
**dentro de una transacción**, y el carrusel se desplaza para seguir a la tarjeta movida.

### Decisiones tomadas
- **Interacción**: flechas, no arrastrar (chocaría con el gesto del `HorizontalPager`).
- **Ubicación**: las flechas **sustituyen al botón "Aparcar"**, que ya se deshabilita en modo
  edición, así que el hueco está libre.
- **Sin pantalla nueva**: se descarta la pestaña "Mis Coches"; la Home ya cubre recorrer y
  eliminar, y editar será cosa de la futura pantalla de detalle.
- **Persistir en cada pulsación**, no al salir del modo edición.

## Hallazgos que condicionan el diseño

- **`UserRepository.updateUserCars` existe, nadie lo llama, y sobrescribe el array a ciegas.**
  Es el patrón exacto que dejó dos vehículos huérfanos en este proyecto al partir de una copia
  obsoleta de la lista.
- Ya hay un `runTransaction` en `shareVehicleWithUser` (`FirestoreVehicleRepository.kt`): hay
  patrón interno que seguir.
- `VehicleCard` ya recibe `isEditMode` y deshabilita "Aparcar" en ese estado.

## Archivos / módulos afectados

| Archivo | Cambio |
|---------|--------|
| `domain/repository/UserRepository.kt` | Nuevo `moveUserVehicle`; **eliminar** `updateUserCars` |
| `data/repository/FirestoreUserRepository.kt` | Implementación con `runTransaction` |
| `domain/usecase/MoveVehicleUseCase.kt` | **Nuevo** |
| `di/SharedModule.kt` | Registrar el use case e inyectarlo en `HomeViewModel` |
| `presentation/home/HomeViewModel.kt` | Evento `MoveVehicleClicked`, `pendingScrollToIndex`, `ScrollHandled` |
| `presentation/home/HomeScreen.kt` | Flechas en la tarjeta y desplazamiento del pager |
| `composeResources/values{,-en,-fr}/strings.xml` | `reorder_move_left`, `reorder_move_right`, `reorder_error` |

## Cambios de datos y reglas Firestore

- **Modelo**: sin cambios.
- **Firestore**: se reescribe `users/{uid}.userVehicles` con el nuevo orden, dentro de una
  transacción.
- **Reglas**: **ninguna modificación** — el usuario ya puede escribir su propio documento. No
  hay despliegue.

## Cómo se aborda el riesgo de sobrescritura

La transacción:

1. Lee el documento del usuario **dentro** de la transacción.
2. Localiza el vehículo **por su id**, nunca por el índice que venga de la UI.
3. Calcula el destino; si el id no está o el destino se sale de rango, no hace nada y devuelve
   éxito (un movimiento imposible no es un error).
4. Escribe la lista reordenada.

Así se parte siempre del array **más reciente en servidor**, y Firestore reintenta si hubo
conflicto: un vehículo creado entre medias desde otro dispositivo no se pierde. Mover por id
evita además mover el vehículo equivocado si la lista local estuviera desincronizada.

Por coherencia se **elimina `updateUserCars`**: está muerto y es justo la trampa que esta
feature debe evitar.

## El carrusel sigue a la tarjeta movida

Es lo que hace la feature usable. Si al pulsar ◀ la tarjeta se va y el pager se queda, el
usuario acaba mirando otro vehículo, con las flechas de otra tarjeta bajo el dedo. Tras un
movimiento con éxito el ViewModel publica el índice destino en `pendingScrollToIndex` y la
pantalla desplaza el pager, de modo que se puede pulsar ◀ repetidamente para seguir moviendo el
mismo vehículo. Se usa el patrón de un solo disparo (`...Handled`) ya presente en el proyecto.

## Riesgos

- **Ráfaga de pulsaciones**: varias transacciones seguidas. Son atómicas, así que el resultado
  es correcto; si aparece parpadeo, deshabilitar la flecha mientras haya una escritura en vuelo.
- **Salto del pager**: el desplazamiento programático dispara `OnVehicleSwiped`. Es benigno,
  pero hay que comprobar que no produce un tirón visual.

## Resolución de las preguntas abiertas de la spec

1. **Cuándo persistir** → **en cada pulsación**. Firestore aplica en local al instante, así que
   no hay latencia perceptible; acumular obligaría a mantener estado intermedio y a decidir qué
   hacer si el guardado final falla tras varios movimientos.
2. **Feedback visual** → basta el modo edición existente. En los extremos la flecha se
   **deshabilita** (no se oculta), para que la tarjeta no cambie de forma.
3. **Alcance del movimiento** → **una posición por pulsación**. Mantener pulsado para repetir
   queda fuera de alcance.
