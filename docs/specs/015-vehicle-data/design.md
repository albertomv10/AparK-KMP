# Design: datos del vehículo y editarlo

- **Spec**: [spec.md](spec.md)
- **Estado**: Aprobado

## Enfoque

Un **formulario compartido**, `VehicleForm`, sirve para crear y para editar. Primero se eligen la marca
y el modelo, con un buscador que filtra mientras se escribe, y el nombre se rellena solo. Los datos se
guardan como **códigos** en campos `String` del vehículo, según el
[catálogo de DATA-MODEL.md](../../DATA-MODEL.md) (§9). Las reglas validan cada campo **solo cuando
cambia**. La lista de marcas y modelos viaja **dentro de la app**: un JSON abierto, más uno nuestro con
lo que le falta.

## Lo que dijo el código antes de decidir nada

1. **Crear escribe el `Vehicle` entero con `set()`**, así que los campos nuevos viajan con su valor
   por defecto, `""`. La regla de `create` tiene que aceptar `""` como «sin dato».
2. **El dueño aparca por la rama del dueño** en las reglas. Si esa rama validara todos los campos, un
   valor antiguo inválido le impediría aparcar. Por eso se valida solo lo que cambia, como ya hace la
   regla de `users` con el nombre.
3. **El dueño puede cambiar hoy el nombre sin validación**: la rama del dueño no lo mira. Se añade,
   también solo cuando cambia.
4. **Leer ficheros empaquetados ya tiene patrón**: `Res.readBytes("files/email_animation.json")`.
5. **El marcador del mapa es nativo** en las dos plataformas: se queda como está.
6. **`commonMain` no tiene `java.text.Normalizer`**: la búsqueda sin tildes («skoda» → «Škoda»)
   necesita su propia tabla de equivalencias.

## Archivos / módulos afectados

| Archivo | Cambio |
|---------|--------|
| `domain/model/Vehicle.kt` · `VehicleAttributes.kt` · `VehicleDetails.kt` | Campos nuevos, las listas como tipos con `fromCode`, y las combinaciones de sistema y combustible |
| `domain/repository/VehicleRepository.kt` · `data/repository/FirestoreVehicleRepository.kt` | Crear y editar con `VehicleDetails` |
| `domain/usecase/CreateVehicleUseCase.kt` · `UpdateVehicleUseCase.kt` | Con `VehicleDetails` |
| `domain/catalog/` · `data/catalog/CatalogRepository.kt` | Carga, fusión y búsqueda del catálogo |
| `composeResources/files/catalog/` | `car-list.json`, `extra.json` y `NOTICE.txt` |
| `presentation/vehicleform/` | `VehicleForm`, su estado, `autoName`, el buscador, las muestras de color |
| `presentation/addvehicle/` · `presentation/editvehicle/` | Crear con el formulario, y la pantalla de editar |
| `presentation/vehicledetail/` | Los datos, el lápiz y la tarjeta de añadirlos |
| `presentation/home/HomeScreen.kt` · `presentation/vehicles/VehiclesScreen.kt` | El icono del tipo y el color |
| `presentation/navigation/` · `di/SharedModule.kt` | El destino `EditVehicle` y Koin |
| `firestore.rules` · `tools/test/vehicles.rules.test.ts` | La validación y sus tests |

## Cambios de datos y reglas Firestore

- **Modelo**: `vehicles/{id}` gana `type`, `powertrain`, `fuel`, `make` y `emissionsLabel`, todos de
  texto. `model` y `color` ya existían. `""` significa «sin dato».
- **Reglas**:
  - **en `create`**, cada lista valida su campo contra sus códigos, con `""` permitido, y `make` y
    `model` son texto de hasta 40;
  - **en la rama del dueño de `update`**, cada campo **se valida solo si cambia**, y entran el nombre
    (1–60) y la matrícula (≤ 15), con el mismo criterio;
  - **los miembros** siguen sin poder editar: su rama solo deja aparcar o salirse.
- **Compatibilidad**: la app 1.1.0 no escribe estos campos, y si faltan cuentan como `""`.

## El nombre que se rellena solo

`autoName(nombreActual, marcaAnterior, modeloAnterior, marcaNueva, modeloNueva)`, una función pura: **si
el nombre está vacío, o coincide con lo que se generó a partir de la marca y el modelo anteriores, se
regenera**. Si no, es tuyo y no se toca. Así vale igual al crear y al editar: un nombre puesto a mano
nunca se pisa, y uno automático sigue a la marca y al modelo.

## El catálogo

- **`car-list.json`**: copia de [json-car-list](https://github.com/getfrontend/json-car-list) en el
  commit `607d40047355`, del 2025-04-27. Son 135 marcas y 1.420 modelos.
- **`extra.json`**: lo nuestro.
  - Los coches que faltan, con sus modelos: Cupra, Omoda, Jaecoo y KGM/SsangYong.
  - Unas veinticinco marcas de moto, sin modelos.
- **`NOTICE.txt`**: la atribución, el commit y el texto de la licencia MIT. El README del repositorio
  declara la licencia, aunque no tiene fichero `LICENSE`.
- **Dónde se busca, según el tipo**: coche, furgoneta y autocaravana buscan en las marcas de coche;
  moto, en las de moto; sin tipo u «otro», en todas.
- **La búsqueda**: `foldForSearch()` quita mayúsculas y tildes; `searchNames()` pone primero lo que
  empieza por lo escrito y después lo que lo contiene, con un máximo de seis.
- **Las sugerencias van en línea, debajo del campo**, y no en un menú emergente: funcionan igual en
  las dos plataformas y con el teclado abierto.

## Decisiones y alternativas consideradas

- **Decisión**: **un formulario compartido** para crear y editar.
  **Alternativa**: dos pantallas distintas, que divergirían en cuanto una cambie.
- **Decisión**: **marca y modelo primero**, con el nombre generado y editable (a petición de Alberto).
  **Alternativa**: el nombre primero, como ahora. Obliga a escribir dos veces lo mismo.
- **Decisión**: **validar en las reglas solo los campos que cambian**.
  **Alternativa**: validarlos todos en cada escritura. Un valor antiguo inválido bloquearía hasta
  aparcar.
- **Decisión**: **sugerencias en línea**.
  **Alternativa**: el `ExposedDropdownMenu` de Material. Es un menú emergente, y con el teclado
  abierto en iOS se coloca mal.
- **Decisión**: **«Más datos» plegado al crear**, y abierto al editar. El alta sigue siendo rápida.
- **Decisión**: **el marcador del mapa no cambia**. Dibujar un icono por tipo es trabajo nativo en las
  dos plataformas, y queda para otra spec si se echa de menos.

## Riesgos

- **La lista no estará completa**, y por eso siempre se puede escribir a mano.
- **Las sugerencias en línea empujan el formulario hacia abajo** mientras se escribe.
- **La lista se actualiza solo al publicar** una versión. Llevarla a Firestore queda para cuando haga
  falta.

## Resolución de las preguntas abiertas de la spec

1. **¿Pantalla o diálogo?** → pantalla completa.
2. **¿Datos al crear?** → sí, opcionales, con la marca y el modelo primero y el nombre automático.
3. **¿El tipo cambia el marcador?** → no.
4. **¿Dónde se ven el tipo y el color?** → tarjeta, lista y detalle.
5. **¿Motos con modelos?** → solo marcas.
6. **¿Licencia?** → `NOTICE` empaquetado, y la pantalla de licencias al ROADMAP.
