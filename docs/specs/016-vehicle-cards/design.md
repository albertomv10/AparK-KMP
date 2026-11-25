# Design: las tarjetas del mapa, y ordenar los vehículos desde su lista

- **Spec**: [spec.md](spec.md)
- **Estado**: Aprobado

## Enfoque

**La tarjeta solo hace dos cosas.**
- Dice dónde está el vehículo y quién lo dejó, en una línea.
- Deja aparcarlo.

**Borrar se queda en el detalle**, que ya lo tiene. **Ordenar se va a la lista de *Vehículos***:
- se entra con un botón «Ordenar»;
- se arrastra cada fila por un asa;
- se guarda una sola vez, con una escritura normal que funciona sin conexión.

El modo edición del carrusel desaparece entero.

## Lo que dijo el código antes de decidir nada

1. **Reordenar es `moveUserVehicle`, una transacción por flecha.** Una transacción necesita el
   servidor, así que sin conexión falla.
2. **La lista se ordena con `userVehicles` como pista** (`FirestoreVehicleRepository`:
   `sortedBy { position[id] ?: Int.MAX_VALUE }`).
   - Un vehículo que no está en ella sale al final.
   - Por eso basta una escritura normal: perder una carrera solo cambia de sitio un vehículo.
3. **La regla de `users` deja escribir `userVehicles` al dueño del documento**, y solo valida
   `name`. No cambian las reglas ni hay nada que desplegar.
4. **Una escritura de gitlive no termina hasta que el servidor la confirma, y sin conexión no
   termina nunca.** El caché local, en cambio, la refleja al momento, así que la interfaz no puede
   quedarse esperándola.
5. **`DeleteVehicleUseCase` y `RemoveUserFromVehicleUseCase` también los usa el detalle**, así que
   se quedan. `MoveVehicleUseCase` y `moveUserVehicle` solo los usa el carrusel.
6. **`DynamicTimeText` solo lo usan tres sitios**: la tarjeta, la lista y el detalle. Los tres
   escriben «Última vez: %s» y, debajo, «Aparcado por %s». Esa segunda línea, en francés, decía
   «Pour %s».
7. **Reorderable 3.1.0** (`sh.calvin.reorderable:reorderable`):
   - es Apache-2.0 y se publicó el 20 de abril de 2026;
   - tiene versión para Android, iOS arm64 e iOS simulator arm64;
   - pide `foundation` 1.7.0 como mínimo, y el proyecto usa 1.9.3.

## Ordenar desde *Vehículos*

**Datos**

- **Repositorio:** `UserRepository.moveUserVehicle` se sustituye por
  `setVehicleOrder(userId, vehicleIds)`, que hace un `update` de `userVehicles` y `updatedAt`.
  - Escribe los ids de la lista tal como se ve, así que las pistas viejas de vehículos que ya no
    están desaparecen solas.
  - Un vehículo que me añaden mientras ordeno no está en lo escrito y sale al final (criterio 8).
- **Caso de uso:** `SetVehicleOrderUseCase` sustituye a `MoveVehicleUseCase`.
- **Funciones puras**, en `presentation/vehicles/VehicleOrder.kt`:
  - `List<Vehicle>.inOrder(ids)` pone los vehículos en el orden de `ids`. Los que no están en `ids`
    van al final, en el orden en que llegan, y los ids sin vehículo se ignoran.
  - `List<T>.moved(from, to)` mueve un elemento.

**`VehiclesViewModel`**

- **Estado nuevo:** `currentUserId`, `isReordering` y `orderOverride: List<String>?`.
- **Lo que se muestra** es la lista en el orden del override, si lo hay. Si mientras se ordena llega
  un cambio —alguien aparca, entra un vehículo—, se ve sin perder lo movido.
- **Eventos:**
  - `ReorderStarted` entra en el modo.
  - `Moved(from, to)` guarda los ids movidos como override.
  - `ReorderFinished` sale del modo. Si el orden cambió respecto al que llega de Firestore, lanza
    `setVehicleOrder` sin esperarla; si no cambió, quita el override.
- **El override dura hasta que la lista llega ya en ese orden, o hasta que la escritura termina.**
  Así no se ve el orden viejo ni un instante entre «Hecho» y la respuesta del caché.
- **Si la escritura falla**, se quita el override y sale `reorder_error` en un snackbar, que la
  pantalla gana para esto.

**`VehiclesScreen`**

- **La barra:**
  - «Ordenar» aparece con dos vehículos o más, y se cambia por «Hecho» mientras se ordena.
  - El botón de añadir se oculta durante el modo.
  - «Atrás» termina el modo, igual que «Hecho».
- **La lista** usa `rememberReorderableLazyListState`, con cada fila dentro de
  `ReorderableItem(key = id)`.
  - Mientras se ordena, las filas no se pueden tocar, y la flecha se cambia por un asa con
    `Modifier.draggableHandle()`.
  - La fila que se arrastra se eleva.
  - Vibra al cogerla y al pasar por cada posición.
- **Para el lector de pantalla:** cada fila tiene las acciones «Subir» y «Bajar» mientras se ordena
  (criterio 6). El asa es decorativa para él.

## El carrusel

- **El modo edición se quita entero:**
  - en la pantalla, `combinedClickable`, el velo sobre el mapa, el `BackHandler`, la papelera, el
    diálogo, las flechas y `followedVehicleId`;
  - en `HomeViewModel`, los casos de uso de borrar, quitar y mover, y `PendingDeletion` con sus
    eventos y claves.
- **El carrusel sigue en el mismo vehículo aunque cambie el orden.**
  - `HomeUiState` gana `selectedVehicleId`, que se actualiza al deslizar.
  - Cuando llega la lista con ese vehículo en otra posición, `selectedVehicleIndex` pasa a ella y la
    pantalla mueve el pager con `scrollToPage`.
  - Si el vehículo ya no está, el efecto de ahora devuelve el pager a una posición válida.
- **Aparcar siempre confirma** (criterio 13). Si no había ubicación anterior, o la había con
  `timestamp == 0`, sale el aviso de éxito sin datos para deshacer, y la pantalla lo pinta sin
  «Deshacer».

## La tarjeta nueva

```
┌──────────────────────────────────────────┐
│ (🚐) Furgoneta de casa…              ›   │   avatar · nombre (weight 1, …) · [iconos fase 3] · flecha
│ Aparcado hace 5 min por Edu              │   una línea, o «Aún no aparcado»
│ [              Aparcar              ]    │   al aparcar, el spinner va dentro del botón
└──────────────────────────────────────────┘
```

- **Todas las tarjetas tienen la misma estructura**, así que todas miden lo mismo:
  - la fila del nombre mide al menos 32 dp, haya avatar o no;
  - siempre hay una línea de texto;
  - siempre hay un botón.

  Miden unos 148 dp, frente a los 180 de ahora. No se fija la altura: con la letra grande crecen
  todas igual. La tarjeta «+» usa ese alto como mínimo.
- **La flecha** dice que la tarjeta abre el detalle. La tarjeta lleva `clickable(onClickLabel =
  «Ver detalles»)`, que es lo que anuncia el lector de pantalla (criterio 12).
- **Los iconos de pendientes de la fase 3** irán entre el nombre y la flecha. El nombre ya se corta
  antes de ella (criterio 14).

## «Aparcado hace 5 min por Edu», en tres sitios

- **`ParkedText(location, currentUserId, …)`**, en `presentation/components`, sustituye a
  `DynamicTimeText` y a la línea de quién aparcó.
  - Se usa en la tarjeta, en la fila de *Vehículos* (que conserva la matrícula) y en el detalle.
  - Se refresca cada minuto.
- **`parkedBy(location, currentUserId)`** es pura y tiene tests. Devuelve `You`, `Named(nombre o
  email)` o `Nobody`; `Nobody` es una cuenta borrada, que se queda solo con el cuándo.
- **Hay una frase por cada tipo de tiempo**, porque la preposición cambia («hace 5 min», «hoy a las
  18:45», «el 01 ene»). El quién va en una plantilla aparte:

| Clave | es | en | fr |
|---|---|---|---|
| `parked_just_now` | Aparcado hace un momento | Parked just now | Garé à l'instant |
| `parked_minutes_ago` | Aparcado hace %1$d min | Parked %1$d min ago | Garé il y a %1$d min |
| `parked_hours_ago` | Aparcado hace %1$d h | Parked %1$d h ago | Garé il y a %1$d h |
| `parked_today_at` | Aparcado hoy a las %1$s | Parked today at %1$s | Garé aujourd'hui à %1$s |
| `parked_yesterday_at` | Aparcado ayer a las %1$s | Parked yesterday at %1$s | Garé hier à %1$s |
| `parked_on_date` | Aparcado el %1$s %2$s | Parked on %1$s %2$s | Garé le %1$s %2$s |
| `parked_by` | %1$s por %2$s | %1$s by %2$s | %1$s par %2$s |
| `parked_by_you` | %1$s por ti | %1$s by you | %1$s par vous |

## Archivos principales

| Archivo | Cambio |
|---|---|
| `gradle/libs.versions.toml` · `composeApp/build.gradle.kts` | Reorderable 3.1.0 en `commonMain` |
| `domain/repository/UserRepository.kt` · `data/repository/FirestoreUserRepository.kt` | `setVehicleOrder` en vez de `moveUserVehicle` |
| `domain/usecase/SetVehicleOrderUseCase.kt` · `di/SharedModule.kt` | Caso de uso y Koin |
| `presentation/vehicles/` | Ordenar, y `VehicleOrder.kt` |
| `presentation/home/` | Sin modo edición; la tarjeta nueva; seguir al vehículo; confirmar siempre |
| `presentation/components/ParkedText.kt` | Sustituye a `DynamicTimeText.kt` |
| `presentation/vehicledetail/VehicleDetailScreen.kt` | `ParkedText` en «Dónde está» |
| `composeResources/values*/strings.xml` | Textos |

## Riesgos

- **Una dependencia nueva de terceros.** Entra en la pantalla de «Licencias» que el ROADMAP pide
  antes de publicar.
- **Las acciones de accesibilidad en iOS** dependen de cómo Compose Multiplatform las traslade a
  VoiceOver. Si no llegan, la alternativa son botones ↑↓ visibles en el modo ordenar.
- **Que el carrusel no salte de alto** depende de que todas las tarjetas tengan de verdad la misma
  estructura. Por eso cada línea lleva `maxLines = 1`.
