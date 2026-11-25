# Tasks: la cámara del mapa, y mover un vehículo manteniendo pulsado

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## La regla y la ubicación
- [x] `MapCamera.kt`: `GeoPoint`, `MapFocus`, `CameraMove`, `CameraRequest`, `cameraMoveFor`; con sus tests
- [x] `getApproximateLocation()` en lugar de `getUserLocation()`; `LocateUserUseCase` y Koin

## El ViewModel y la pantalla
- [x] `HomeViewModel`: `lastFocus`, `vehiclesLoaded`, `MapShown`, el canal de órdenes y el `Job` de ubicación
- [x] Eventos `MarkerTapped`, `MapLongPressed`, `GoToVehicleClicked`; `ManualLocationChosen`; centrar con `ToUser`
- [x] `HomeScreen`: `MapShown`, la vibración al mantener pulsado y el botón «Ir al vehículo»
- [x] Textos en `values`, `values-en` y `values-fr`

## El mapa
- [x] Android: ejecutar órdenes, cámara inicial, `onMapLongClick` y tocar el marcador
- [x] iOS Kotlin: ejecutar órdenes y los callbacks nuevos
- [x] Swift: zoom opcional, `pendingCamera`, los delegados nuevos; fuera `centerOnUserLocation`

## Documentación
- [x] `AGENTS.md` y `CHANGELOG.md`

## Verificación
- [x] Compila Android, iOS-Kotlin, tests JVM y nativos, y Xcode
- [x] **(1)** Con otros datos cambiando, la cámara no se mueve
- [x] **(2)(3)** Pasar de tarjeta vuela con zoom 17; a un vehículo sin aparcar, a tu ubicación
- [x] **(4)** Al abrir y al volver a la pestaña aparece ya en el vehículo
- [x] **(5)** Si el elegido cambia de sitio, la cámara lo sigue de cerca, con zoom 20
- [x] **(6)** Centrar, e «Ir al vehículo»
- [x] **(7)(8)(9)(10)(11)** Mantener pulsado mueve el elegido con «Deshacer»; con la «+» no; arrastrar sigue igual
- [x] Tocar un marcador elige su vehículo
- [x] **(12)** Android, la vibración y el aspecto — **Alberto**
