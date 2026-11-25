# Tasks: datos del vehículo y editarlo

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## Reglas
- [x] Una validación por lista, con `""` permitido; `make` y `model` hasta 40
- [x] `create` valida los campos nuevos
- [x] La rama del dueño valida cada campo solo si cambia, incluidos el nombre y la matrícula
- [x] Tests: crear válido e inválido, el dueño edita, un miembro no, y el dueño aparca con un valor antiguo inválido

## Dominio, datos y catálogo
- [x] `Vehicle` con los campos nuevos; `VehicleAttributes` con `fromCode` y su caso desconocido
- [x] Combinaciones de sistema y combustible
- [x] `VehicleDetails`; crear y editar en el repositorio y en los casos de uso
- [x] `car-list.json` (commit `607d40047355`), `extra.json` y `NOTICE.txt`
- [x] `CatalogRepository`, `foldForSearch` y `searchNames`
- [x] Tests en `commonTest`

## Interfaz
- [x] `VehicleForm` con su estado y `autoName`; el buscador en línea; la paleta; «Más datos»
- [x] Crear con el formulario
- [x] Destino `EditVehicle` y su pantalla
- [x] El detalle: los datos, el lápiz y «Añade los datos del vehículo»
- [x] La tarjeta del mapa y la lista de *Vehículos*: el icono del tipo y el color
- [x] Textos en `values`, `values-en` y `values-fr`, con nombres de color para los lectores de pantalla

## Documentación
- [x] `AGENTS.md`, DATA-MODEL §9, `ROADMAP.md` (2.1, 2.2, 2.5, y la pantalla de licencias antes de publicar)
- [x] Entrada en `CHANGELOG.md`

## Verificación
- [x] Compila Android, iOS-Kotlin, tests JVM y nativos, Xcode; tests de `tools`
- [x] **(4)** «skoda» encuentra Škoda; el modelo filtra por la marca; el nombre se rellena y no pisa uno a mano
- [x] **(5)** Una marca o un modelo que no están se guardan tal cual
- [x] **(3)** Híbrido ofrece gasolina o diésel; eléctrico no pregunta; pila de combustible es hidrógeno
- [x] **(8)(9)** El detalle, la tarjeta y la lista enseñan los datos; sin datos, nada, y el dueño ve cómo añadirlos
- [x] **(1)(2)** El dueño edita desde el detalle; un miembro no ve el lápiz y las reglas se lo deniegan
- [x] **(7)** Un código desconocido sale como «Otro»; en el color y la etiqueta, no se muestra
- [x] **(10)** La app en inglés
- [ ] El aspecto y Android — **Alberto**
