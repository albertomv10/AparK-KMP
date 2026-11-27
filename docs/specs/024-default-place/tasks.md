# Tasks: la ubicación predeterminada de un vehículo

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## Reglas Firestore
- [x] `defaultPlaceOk`, en el dueño y en la creación; la rama del miembro; `atDefaultPlace` booleano al aparcar
- [x] Tests en `tools/test/vehicles.rules.test.ts`
- [x] Desplegar a `apark-dev`, comprobado contra el proyecto activo

## Domain y Data
- [x] `DefaultPlace`, `Vehicle.defaultPlace`, `LocationModel.atDefaultPlace`, `parkedAtPlace`
- [x] `VehicleRepository.setDefaultPlace` y `removeDefaultPlace`
- [x] `SetDefaultPlaceUseCase` y `RemoveDefaultPlaceUseCase`, en Koin
- [x] Tests

## Presentation
- [x] `PlacePickerMap`, `expect`/`actual`; en iOS, su fábrica y su controlador en Swift
- [x] `DefaultPlaceEditor` en `Destiny.kt` (no `DefaultPlace`, que choca con la clase del modelo), `DefaultPlaceViewModel` y `DefaultPlaceScreen`
- [x] El detalle: la fila y la frase larga con el sitio
- [x] La tarjeta: los dos botones, aparcar en la ubicación sin esperar, la frase corta
- [x] Los marcadores: la casa en iOS; `MarkerComposable` en Android
- [x] Textos en es, en y fr
- [x] La siembra

## Documentación
- [x] `DATA-MODEL.md`, `ROADMAP.md`, `NAVIGATION.md` y `AGENTS.md`
- [x] `CHANGELOG.md`

## Verificación
- [x] Tests de reglas y de la app; compila Android, iOS-Kotlin, los tests en iOS y Xcode
- [x] **(1)(2)(3)** Ponerla, cambiarla y quitarla desde el detalle, en iOS: el mapa nace en la que tenía, se mueve, el punto y el nombre llegan al servidor; quitarla deja «Añadir». En la Furgoneta, que nunca se aparcó, nace donde estás tú
- [x] **(4)** Como Edu, que no es el dueño: aparca en la plaza y la quita, y el servidor lo acepta. Las reglas de lo demás, en los tests
- [x] **(5)(6)** La tarjeta con y sin ubicación, del mismo alto
- [x] **(7)** Aparcar en ella sin permiso de ubicación y en modo avión, en Android: aviso con «Undo» al momento, y la escritura llega al servidor al volver la red. «Deshacer» en iOS
- [x] **(8)(9)** «Plaza 23 · hace un momento» en la tarjeta y «Aparcado … en Plaza 23 por ti» en el detalle; el pin con la casa en iOS y en Android
- [x] **(10)** Arrastrar el pin lo devuelve a lo normal, en Android y en iOS. «Aquí» usa el mismo camino —reescribir `lastLocation` sin la marca— y no lo he pulsado — **Alberto**, en el móvil
- [x] **(11)** Una versión anterior que aparca, por REST sin la marca: pin y frase normales, y la ubicación sigue
- [x] **(12)** iOS y Android; inglés en Android y el mapa de elegir en oscuro en iOS
- [x] En el móvil, contra `apark-dev` — **Alberto**: se ve bien

## Cambio después de probarla
- [x] Android: el pin dibujado con la forma y el tamaño del de Google, la cabeza sólida y la casa más grande
- [x] Una pulsación larga encima de un pin no aparca, en iOS (`isOnMarker`) y en Android (`isOnPin`)
- [x] Solo se arrastra el pin del vehículo elegido, en las dos plataformas
- [x] Probado en el simulador y en el emulador de Android, con los pines separados: arrastrar el elegido lo mueve y le quita la casa; mantener pulsado otro no mueve nada; mantener pulsado el mapa vacío sigue aparcando
