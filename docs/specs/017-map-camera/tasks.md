# Tasks: la cámara del mapa, y mover un vehículo manteniendo pulsado

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## La regla y la ubicación
- [ ] `MapCamera.kt`: `GeoPoint`, `MapFocus`, `CameraMove`, `CameraRequest`, `cameraMoveFor`; con sus tests
- [ ] `getApproximateLocation()` en lugar de `getUserLocation()`; `LocateUserUseCase` y Koin

## El ViewModel y la pantalla
- [ ] `HomeViewModel`: `lastFocus`, `vehiclesLoaded`, `MapShown`, el canal de órdenes y el `Job` de ubicación
- [ ] Eventos `MarkerTapped`, `MapLongPressed`, `GoToVehicleClicked`; `ManualLocationChosen`; centrar con `ToUser`
- [ ] `HomeScreen`: `MapShown`, la vibración al mantener pulsado y el botón «Ir al vehículo»
- [ ] Textos en `values`, `values-en` y `values-fr`

## El mapa
- [ ] Android: ejecutar órdenes, cámara inicial, `onMapLongClick` y tocar el marcador
- [ ] iOS Kotlin: ejecutar órdenes y los callbacks nuevos
- [ ] Swift: zoom opcional, `pendingCamera`, los delegados nuevos; fuera `centerOnUserLocation`

## Documentación
- [ ] `AGENTS.md` y `CHANGELOG.md`

## Verificación
- [ ] Compila Android, iOS-Kotlin, tests JVM y nativos, y Xcode
- [ ] **(1)** Con otros datos cambiando, la cámara no se mueve
- [ ] **(2)(3)** Pasar de tarjeta vuela con zoom 17; a un vehículo sin aparcar, a tu ubicación
- [ ] **(4)** Al abrir y al volver a la pestaña aparece ya en el vehículo
- [ ] **(5)** Si el elegido cambia de sitio, la cámara lo sigue conservando el zoom
- [ ] **(6)** Centrar, e «Ir al vehículo»
- [ ] **(7)(8)(9)(10)(11)** Mantener pulsado mueve el elegido con «Deshacer»; con la «+» no; arrastrar sigue igual
- [ ] Tocar un marcador elige su vehículo
- [ ] **(12)** Android, la vibración y el aspecto — **Alberto**
