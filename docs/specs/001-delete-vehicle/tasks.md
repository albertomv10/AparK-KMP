# Tasks: Eliminar vehículo

- **Spec**: [spec.md](spec.md) · **Design**: [design.md](design.md)

## Domain
- [x] `VehicleRepository`: añadir `deleteVehicle(vehicleId: String, userId: String): Result<Unit>`
- [x] Crear `DeleteVehicleUseCase`
- [x] `SharedModule`: registrar `DeleteVehicleUseCase` e inyectarlo (junto a
      `RemoveUserFromVehicleUseCase`, ya registrado) en `HomeViewModel`

## Data
- [x] `FirestoreVehicleRepository.deleteVehicle`: batch atómico con `batch.delete(carRef)` +
      `batch.update(userRef, CARS_FIELD to FieldValue.arrayRemove(vehicleId))`
      *(riesgo resuelto: `batch.delete` existe en la API de gitlive)*

## Presentation
- [x] `HomeUiState`: añadir `currentUserId`, `isEditMode`, `pendingDeletion`
- [x] `PendingDeletion(vehicleId, vehicleName, isOwner)`
- [x] `HomeEvent`: `VehicleLongPressed`, `EditModeExited`, `DeleteVehicleClicked`,
      `DeleteConfirmed`, `DeleteDismissed`
- [x] `observeAuthState()`: poblar `currentUserId` con el uid ya disponible
- [x] Lógica de borrado siguiendo el patrón de `updateVehicleLocation` (loading + fold +
      claves de snackbar en `companion object`), eligiendo rama según `isOwner`
- [x] Crear componente reutilizable `components/AparKConfirmDialog.kt` (Material 3, variante
      destructiva)
- [x] `HomeScreen`: modo edición (long-press con `combinedClickable`, badge de borrado,
      botón "Listo", botón Aparcar deshabilitado)
- [x] `HomeScreen`: mostrar el diálogo con el copy correcto según rol (criterios 6 y 7)
- [x] `HomeScreen`: añadir las nuevas claves al `when` de traducción

## Robustez (regresión que dispara esta feature)
- [x] Sustituir `state.vehicles[page]` por `getOrNull(page)`
- [x] Acotar la página/`selectedVehicleIndex` cuando la lista encoge
- [x] Proteger el `selectedVehicleIndex` pasado a `AparKMap`

## Reglas Firestore
- [x] Borrado del dueño: sin cambios (el `allow delete` ya existía)
- [x] **Corrección**: nueva rama en `allow update` para que un usuario compartido pueda salirse
      a sí mismo (`removeUserFromVehicle` estaba bloqueado por las reglas). Desplegada a ambas BDs

## Documentación
- [x] Entrada en `CHANGELOG.md`
- [x] Crear `docs/specs/002-vehicle-cleanup-function/spec.md` (trabajo pospuesto)

## Verificación
- [x] Compila Android (`./gradlew :composeApp:compileDebugKotlinAndroid`)
- [x] Compila iOS-Kotlin (`./gradlew :composeApp:compileKotlinIosSimulatorArm64`)
- [x] Criterio 1: dueño elimina → desaparece al instante y el doc no existe (MCP)
- [x] Criterio 2: compartido se lo quita → desaparece de su Home, el doc sigue existiendo (MCP)
- [x] Criterios 3, 6, 7: diálogo siempre presente y copy correcto según rol
- [x] Criterio 4: tras eliminar, el id ya no está en mi `userVehicles` (MCP)
- [x] Criterio 5: fallo → snackbar de error y lista consistente *(verificado con el
      `PERMISSION_DENIED` real que provocó el bug de reglas, antes de corregirlo)*
- [x] Regresión pager: borrado de la última tarjeta estando en ella → sin crash, el carrusel
      aterriza en la tarjeta de añadir
- [x] Verificado en simulador iOS (iPhone 17) sobre la BD `apark-at`
