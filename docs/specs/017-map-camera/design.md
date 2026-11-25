# Design: la cámara del mapa, y mover un vehículo manteniendo pulsado

- **Spec**: [spec.md](spec.md)
- **Estado**: Aprobado

## Enfoque

**La cámara la decide el `HomeViewModel`, con una función pura en código común.** El mapa de cada
plataforma solo ejecuta órdenes del tipo «ve a este punto, con este zoom o el actual, con animación
o de un salto». Así las dos plataformas hacen lo mismo, la regla tiene tests, y la ubicación del
usuario se resuelve en un solo sitio.

## Lo que dijo el código antes de decidir nada

1. **El fallo de que la cámara se mueva sola** viene de que las dos plataformas la mueven en un
   efecto con `LaunchedEffect(selectedVehicleIndex, vehicles)`. Cualquier cambio en la lista lo
   vuelve a lanzar, y acaba en «ir al vehículo», con zoom 18 en Android y 17 en iOS.
2. **Android centra con `fusedLocationClient.lastLocation`**, que puede ser `null`, y entonces no
   hace nada. iOS centra desde Swift, en `MapLocationController.centerOnUserLocation`, con zoom 15.
3. **El código común ya tiene acceso a la ubicación:**
   - `LocationRepository.getCurrentLocation()` la usa aparcar: pide una fresca durante 5 s, si no la
     consigue toma la última conocida, y si tampoco la hay lanza un error.
   - `getUserLocation()` no la usa nadie.
   - `LocationSource` tiene `getLastKnownLocation()` y `getFreshLocation()` en las dos plataformas.
4. **iOS crea un `GMSMapView` nuevo cada vez que `UIKitView` llama a la fábrica**, también al volver
   a la pestaña. Nace en Madrid con zoom 5, y una orden de cámara que llegue antes de que exista se
   pierde, porque `mapView` es `nil`.
5. **Mover a mano** pasa por `saveManualLocation`, que desde la spec 016 siempre confirma y ofrece
   «Deshacer» si había una ubicación anterior. Mantener pulsado usa el mismo camino.
6. **Mantener pulsado un marcador y mantener pulsado el mapa vacío son eventos distintos.** Lo
   primero empieza a arrastrar el marcador. Lo segundo es `onMapLongClick` en Android y
   `mapView(_:didLongPressAt:)` en iOS. No chocan.

## La regla

Vive en `presentation/components/MapCamera.kt` y tiene tests.

```kotlin
const val STREET_ZOOM = 17f
data class MapFocus(val vehicleId: String?, val location: GeoPoint?) // id null: la «+» o sin vehículos
fun cameraMoveFor(previous: MapFocus?, current: MapFocus): CameraMove?
```

| Caso | Movimiento |
|---|---|
| `previous == null`: el mapa acaba de aparecer | Al vehículo, zoom 17, **de un salto**. Si no tiene ubicación o no hay vehículos, a ti, de un salto |
| Cambia el vehículo elegido | Al vehículo, zoom 17, animado. Si nunca se aparcó, a ti, animado |
| El mismo vehículo, en otro sitio | Al sitio nuevo **conservando el zoom**, animado |
| Se elige la «+», o lo que cambió no es el vehículo ni su sitio | Nada |

## `HomeViewModel`

- **Guarda `lastFocus`.** Tras cada cambio de lista o de selección calcula el movimiento, pero solo
  cuando la lista ya ha llegado (`vehiclesLoaded`). Así, al arrancar no vuela desde tu ubicación
  hasta el vehículo.
- **`MapShown`**, que `HomeScreen` lanza al componerse antes que el mapa, vacía `lastFocus`. Por eso
  volver a la pestaña es un salto.
- **Las órdenes salen por un `Channel` *conflated*.** Si llega una mientras la pestaña está oculta, el
  salto de `MapShown` la sustituye, y nunca se reproduce una vieja.
- **`ToUser` se resuelve con `LocateUserUseCase`**, en un `Job` que se cancela si llega otro
  movimiento.
- **Eventos nuevos:**
  - `MarkerTapped`, que elige ese vehículo;
  - `MapLongPressed`, que hace `saveManualLocation` del vehículo elegido (con la «+» elegida no hace
    nada);
  - `GoToVehicleClicked`.
- **Cambian:** `CenterMapOnUserClicked` pasa a ser `ToUser` animado, y `OnMarkerDragged` pasa a
  llamarse `ManualLocationChosen`.

## Dónde estás, para el mapa

`getUserLocation()` se sustituye por `getApproximateLocation()`:
- primero, la última ubicación conocida, que es instantánea;
- si no la hay, una fresca con un límite de 5 s;
- si tampoco, `null`. Un error, como no tener permiso, también da `null`.

Así Android centra aunque no tenga ninguna ubicación guardada.

## El mapa

**Firma nueva:** `AparKMap(…, cameraRequests: Flow<CameraRequest>, onMarkerDragged, onMarkerTapped,
onMapLongPressed)`.

**Android:**
- La cámara nace en España con zoom 5, igual que en iOS.
- Las órdenes se recogen con `collectLatest`, así que deslizar rápido corta el vuelo anterior.
- `GoogleMap(onMapLongClick = …)`.
- `Marker(onClick = … ; true)`: el marcador se elige y no abre el globo.

**iOS, en Kotlin:** pasa cada orden a `iosMapUpdateCamera(lat, lng, zoom?, animated)`, con dos
callbacks nuevos: `iosOnMapLongPressed` e `iosOnMarkerTapped`.

**iOS, en Swift:**
- Si el mapa todavía no existe, la orden se guarda en `pendingCamera`, y la fábrica la usa como
  cámara inicial. El mapa nace ya en su sitio.
- Los delegados de mantener pulsado y de tocar un marcador avisan a Kotlin.
- Desaparece `centerOnUserLocation`.

## `HomeScreen`

- **Mantener pulsado el mapa vibra**, y solo si la tarjeta elegida es un vehículo.
- **Sobre el botón de centrar va «Ir al vehículo»**, un botón pequeño con el icono del tipo del
  vehículo. Solo aparece si el vehículo elegido está aparcado.

## Riesgos

- **En iOS, una orden puede llegar antes de que exista el mapa.** La cubre `pendingCamera`.
- **Un gesto accidental al mantener pulsado.** Lo cubre «Deshacer», como decidió Alberto.
