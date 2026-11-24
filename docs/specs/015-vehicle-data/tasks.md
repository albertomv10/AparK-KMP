# Tasks: datos del vehículo y editarlo

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## Reglas
- [ ] Una validación por lista, con `""` permitido; `make` y `model` hasta 40
- [ ] `create` valida los campos nuevos
- [ ] La rama del dueño valida cada campo solo si cambia, incluidos el nombre y la matrícula
- [ ] Tests: crear válido e inválido, el dueño edita, un miembro no, y el dueño aparca con un valor antiguo inválido

## Dominio, datos y catálogo
- [ ] `Vehicle` con los campos nuevos; `VehicleAttributes` con `fromCode` y su caso desconocido
- [ ] Combinaciones de sistema y combustible
- [ ] `VehicleDetails`; crear y editar en el repositorio y en los casos de uso
- [ ] `car-list.json` (commit `607d40047355`), `extra.json` y `NOTICE.txt`
- [ ] `CatalogRepository`, `foldForSearch` y `searchNames`
- [ ] Tests en `commonTest`

## Interfaz
- [ ] `VehicleForm` con su estado y `autoName`; el buscador en línea; la paleta; «Más datos»
- [ ] Crear con el formulario
- [ ] Destino `EditVehicle` y su pantalla
- [ ] El detalle: los datos, el lápiz y «Añade los datos del vehículo»
- [ ] La tarjeta del mapa y la lista de *Vehículos*: el icono del tipo y el color
- [ ] Textos en `values`, `values-en` y `values-fr`, con nombres de color para los lectores de pantalla

## Documentación
- [ ] `AGENTS.md`, DATA-MODEL §9, `ROADMAP.md` (2.1, 2.2, 2.5, y la pantalla de licencias antes de publicar)
- [ ] Entrada en `CHANGELOG.md`

## Verificación
- [ ] Compila Android, iOS-Kotlin, tests JVM y nativos, Xcode; tests de `tools`
- [ ] **(4)** «skoda» encuentra Škoda; el modelo filtra por la marca; el nombre se rellena y no pisa uno a mano
- [ ] **(5)** Una marca o un modelo que no están se guardan tal cual
- [ ] **(3)** Híbrido ofrece gasolina o diésel; eléctrico no pregunta; pila de combustible es hidrógeno
- [ ] **(8)(9)** El detalle, la tarjeta y la lista enseñan los datos; sin datos, nada, y el dueño ve cómo añadirlos
- [ ] **(1)(2)** El dueño edita desde el detalle; un miembro no ve el lápiz y las reglas se lo deniegan
- [ ] **(7)** Un código desconocido sale como «Otro»
- [ ] **(10)** La app en inglés
- [ ] El aspecto y Android — **Alberto**
