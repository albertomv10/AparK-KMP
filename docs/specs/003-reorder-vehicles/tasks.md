# Tasks: Reordenar vehículos

- **Spec**: [spec.md](spec.md) · **Design**: [design.md](design.md)

## Domain
- [x] `UserRepository`: añadir `moveUserVehicle(userId, vehicleId, offset): Result<Unit>`
- [x] `UserRepository`: **eliminar** `updateUserCars` (muerto y con sobrescritura ciega)
- [x] Crear `MoveVehicleUseCase`
- [x] `SharedModule`: registrar el use case e inyectarlo en `HomeViewModel`

## Data
- [x] `FirestoreUserRepository.moveUserVehicle` con `runTransaction`: leer dentro de la
      transacción, localizar **por id**, no hacer nada si el destino está fuera de rango,
      escribir la lista reordenada
- [x] Eliminar la implementación de `updateUserCars`

## Presentation
- [x] `HomeEvent.MoveVehicleClicked(vehicleId, offset)` y `HomeEvent.ScrollHandled`
- [x] `HomeUiState.pendingScrollToIndex: Int?`
- [x] Lógica del movimiento siguiendo el patrón existente (launch + fold + clave de error en
      `companion object`), publicando el índice destino al tener éxito
- [x] `VehicleCard`: parámetros `canMoveLeft`, `canMoveRight`, `onMoveLeft`, `onMoveRight`
- [x] En modo edición, sustituir el botón "Aparcar" por la fila de flechas
- [x] Deshabilitar la flecha en los extremos (no ocultarla)
- [x] `LaunchedEffect(pendingScrollToIndex)`: desplazar el pager y despachar `ScrollHandled`
- [x] Añadir `reorder_error` al `when` de traducción

## Reglas Firestore
- [x] Sin cambios: el usuario ya puede escribir su propio documento (no hay despliegue)

## i18n
- [x] `reorder_move_left`, `reorder_move_right`, `reorder_error` en `values/`, `values-en/`,
      `values-fr/`

## Documentación
- [x] Entrada en `CHANGELOG.md`

## Verificación
- [x] Compila Android (`./gradlew :composeApp:compileDebugKotlinAndroid`)
- [x] Compila iOS-Kotlin (`./gradlew :composeApp:compileKotlinIosSimulatorArm64`)
- [x] Criterio 1: mover una tarjeta reordena el carrusel y **este sigue** al vehículo movido
- [x] Criterios 2 y 3: el array `userVehicles` refleja el orden (MCP) y persiste al reabrir
- [x] Criterio 4: el `userVehicles` del otro miembro de un vehículo compartido no cambia
- [~] Criterio 5: no forzado. El camino de error es el mismo patrón ya verificado en la
      spec 001, donde un `PERMISSION_DENIED` real mostró el snackbar y dejó la lista intacta
- [x] Criterio 6: el número de vehículos no cambia tras varios movimientos
- [x] Extremos: ◀ deshabilitada en el primero, ▶ en el último
