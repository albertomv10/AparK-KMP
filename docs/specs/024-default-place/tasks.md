# Tasks: la ubicación predeterminada de un vehículo

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## Reglas Firestore
- [ ] `defaultPlaceOk`, en el dueño y en la creación; la rama del miembro; `atDefaultPlace` booleano al aparcar
- [ ] Tests en `tools/test/vehicles.rules.test.ts`
- [ ] Desplegar a `apark-dev`

## Domain y Data
- [ ] `DefaultPlace`, `Vehicle.defaultPlace`, `LocationModel.atDefaultPlace`, `parkedAtPlace`
- [ ] `VehicleRepository.setDefaultPlace` y `removeDefaultPlace`
- [ ] `SetDefaultPlaceUseCase` y `RemoveDefaultPlaceUseCase`, en Koin
- [ ] Tests

## Presentation
- [ ] `PlacePickerMap`, `expect`/`actual`; en iOS, su fábrica y su controlador en Swift
- [ ] `DefaultPlace` en `Destiny.kt`, `DefaultPlaceViewModel` y `DefaultPlaceScreen`
- [ ] El detalle: la fila y la frase larga con el sitio
- [ ] La tarjeta: los dos botones, aparcar en la ubicación sin esperar, la frase corta
- [ ] Los marcadores: la casa en iOS; `MarkerComposable` en Android
- [ ] Textos en es, en y fr
- [ ] La siembra

## Documentación
- [ ] `DATA-MODEL.md`, `ROADMAP.md`, `NAVIGATION.md` y `AGENTS.md`
- [ ] `CHANGELOG.md`

## Verificación
- [ ] Tests de reglas y de la app; compila Android, iOS-Kotlin, los tests en iOS y Xcode
- [ ] **(1)(2)(3)** Ponerla, cambiarla y quitarla desde el detalle
- [ ] **(4)** Como Edu, que no es el dueño
- [ ] **(5)(6)** La tarjeta con y sin ubicación, del mismo alto
- [ ] **(7)** Aparcar en ella sin permiso de ubicación y sin conexión, con «Deshacer»
- [ ] **(8)(9)** La frase y el pin con la casa, en las dos plataformas
- [ ] **(10)** Aparcar con «Aquí» o arrastrar el pin lo devuelve a lo normal
- [ ] **(11)** Una versión anterior que aparca
- [ ] **(12)** iOS y Android; inglés y tema oscuro
- [ ] En el móvil, contra `apark-dev` — **Alberto**
