# Design: Eliminar vehículo

- **Spec**: [spec.md](spec.md)
- **Estado**: Aprobado
- **Fecha**: 2026-07-25

## Enfoque

Se añade el borrado real de un vehículo (inexistente hoy) y se reutiliza el ya implementado
`removeUserFromVehicle` para el flujo "quitármelo". La entrada en la UI es un **modo edición**
del carrusel que se activa manteniendo pulsada una tarjeta, pensado para albergar en el futuro
el **reordenado**. Toda eliminación pasa por un diálogo de confirmación —el primero del
proyecto— con copy distinto según seas dueño o miembro compartido.

### Decisiones de producto tomadas
- **UI**: pulsación larga → modo edición (estilo "jiggle mode"), no menú contextual ni pantalla
  de detalle (que no existe).
- **Limpieza de otros miembros**: la Cloud Function se **pospone a la spec 002** (requiere plan
  Blaze). Esta feature funciona sin ella.

## Hallazgos que condicionan el diseño

- **No existe ningún método que borre un documento** de vehículo. Hay que crearlo.
- La regla `allow delete` (solo dueño) **ya existe**: **no se tocan las reglas**.
- `removeUserFromVehicle` ya está implementado y registrado en DI.
- **No hay ningún `AlertDialog` en el proyecto**: creamos el primero, reutilizable.
- El **uid actual no está en `HomeUiState`**. Existe `userEmail`, pero **nunca se escribe**
  (campo muerto).
- ⚠️ El pager hace `state.vehicles[page]` **sin protección** y no acota `selectedVehicleIndex`
  → riesgo real de `IndexOutOfBoundsException` al encoger la lista.

## Archivos / módulos afectados

| Archivo | Cambio |
|---------|--------|
| `domain/repository/VehicleRepository.kt` | Nuevo `deleteVehicle(vehicleId, userId)` |
| `data/repository/FirestoreVehicleRepository.kt` | Implementación con `batch.delete` + `arrayRemove` |
| `domain/usecase/DeleteVehicleUseCase.kt` | **Nuevo** |
| `di/SharedModule.kt` | Registrar use case; inyectar los dos use cases en `HomeViewModel` |
| `presentation/home/HomeViewModel.kt` | Estado (`currentUserId`, `isEditMode`, `pendingDeletion`) + eventos + lógica |
| `presentation/home/HomeScreen.kt` | Modo edición, badge de borrado, diálogo, **guards del pager**, ramas de traducción |
| `presentation/components/AparKConfirmDialog.kt` | **Nuevo** componente reutilizable |
| `composeResources/values{,-en,-fr}/strings.xml` | Claves nuevas en los tres idiomas |

## Cambios de datos y reglas Firestore

- **Modelo**: sin cambios.
- **Firestore**: un único `batch` atómico → `delete(vehicles/{id})` + `arrayRemove` del id en
  `users/{uid}.userVehicles`. Ambas escrituras están permitidas por las reglas actuales.
- **Reglas**: el borrado del dueño **no** requiere cambios (`allow delete` ya existía), pero el
  flujo "quitármelo" **sí**. Ver la corrección más abajo.

### Corrección durante la implementación: el flujo del miembro compartido estaba bloqueado

La suposición inicial de que bastaba con reutilizar `removeUserFromVehicle` **era incorrecta**.
Ese método modifica `sharedUsers`, pero la regla `allow update` solo permitía a un usuario
compartido tocar `lastLocation`, así que Firestore denegaba la operación con
`PERMISSION_DENIED`. Era código muerto —nunca había estado conectado a ninguna UI—, por lo que
el fallo nunca se había manifestado. Se detectó al **ejecutar** el criterio 2 en el simulador.

Solución: una tercera rama en `allow update` que permite a un usuario **salirse a sí mismo**,
y solo a sí mismo:

```
|| (
  request.auth.uid in resource.data.sharedUsers
  && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['sharedUsers'])
  && !(request.auth.uid in request.resource.data.sharedUsers)
  && resource.data.sharedUsers.removeAll([request.auth.uid]) == request.resource.data.sharedUsers
)
```

Las dos últimas condiciones impiden que alguien use esta vía para expulsar a otros miembros.
Desplegada a `(default)` y `apark-at`.

### Corrección durante la verificación: salir del modo edición no se encontraba

La primera versión salía del modo solo con un botón de texto "Listo" sobre el carrusel. Al
probarlo, resultó **invisible en la práctica**: texto azul sin contenedor sobre un mapa muy
ruidoso. Además el mapa seguía activo, con el riesgo de arrastrar un marcador sin querer
mientras se editaba.

Solución: al entrar en modo edición se superpone un **velo sobre el mapa** (negro al 25%) que
cumple tres funciones a la vez — bloquea los gestos del mapa (no se puede desplazar ni arrastrar
marcadores), lo atenúa para señalar visualmente el modo, y **al tocarlo se sale**. Las tarjetas,
los FAB y el botón "Listo" se dibujan por encima, así que siguen operativos y, de paso, el
"Listo" gana contraste. El `clickable` del velo usa `indication = null` para no dibujar un
ripple a pantalla completa.

## Decisiones y alternativas consideradas

- **Modo edición por pulsación larga** en lugar de menú ⋮ por tarjeta o pantalla de detalle:
  es la entrada natural del futuro reordenado y evita crear una pantalla fuera de alcance.
  El swipe para borrar se descartó por colisionar con el `HorizontalPager`.
- **Estado de modo edición global** (`isEditMode: Boolean`) en vez de por tarjeta: el
  reordenado futuro es una operación sobre el conjunto.
- **Limpieza perezosa temporal** frente a Cloud Function inmediata: la Function es la solución
  correcta, pero exige Blaze; la resiliencia existente hace que la feature funcione mientras
  tanto. Queda registrada como spec 002.

## Riesgos

- **`batch.delete` en gitlive**: verificar la firma exacta al implementar (el compilador lo dirá).
- **`combinedClickable` dentro del pager**: validar que la pulsación larga no interfiere con el
  swipe horizontal en ambas plataformas.
- **IDs colgantes en otros miembros**: aceptado conscientemente hasta la spec 002; el stream ya
  los descarta, con coste de ~3 reintentos de 1s por ID en el arranque de ese usuario.

## Resolución de las preguntas abiertas de la spec

1. **Limpieza del `userVehicles` de otros miembros** → **Cloud Function**, pospuesta a la
   **spec 002**. Mientras tanto, limpieza perezosa vía el stream resiliente.
2. **Ubicación del control en la UI** → **modo edición activado con pulsación larga** sobre la
   tarjeta; no se crea pantalla de detalle.
3. **Reutilización** → sí: `removeUserFromVehicle` para "quitármelo" y la regla `allow delete`
   existente para el borrado del dueño.
4. **Componente de confirmación** → **no existe ninguno**; se crea `AparKConfirmDialog`
   reutilizable en `presentation/components/`.

## Nota para el futuro reordenado (no se implementa aquí)

`getVehiclesForUser` construye la lista **en el orden del array `userVehicles`**, y las reglas
permiten al usuario escribir su propio documento. Reordenar en el futuro es, por tanto,
**reescribir ese array**: sin cambios de modelo ni de reglas. El modo edición creado aquí es su
punto de entrada natural.
