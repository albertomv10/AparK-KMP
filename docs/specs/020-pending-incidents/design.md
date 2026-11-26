# Design: lo pendiente de los vehículos, a la vista

- **Spec**: [spec.md](spec.md)
- **Estado**: Aprobado

## Enfoque

**Una escucha de incidencias por vehículo**, abierta desde un caso de uso nuevo,
`ObserveOpenIncidentsUseCase`. Lo usan tres ViewModels: *Inicio* para la tarjeta, *Vehículos* para
«Pendiente» y uno nuevo, `PendingCountViewModel`, para la chapita. Cada uno abre las escuchas según
**el conjunto de ids** de sus vehículos: se reabren cuando alguien entra o sale de un vehículo, y no
cada vez que alguien aparca.

**Si un vehículo falla, los demás siguen**: cada escucha recoge su propio error, así que una que
falla no tumba a las otras dentro del `combine`.

**Lo que se enseña se cruza siempre con la lista de vehículos del momento.** Las incidencias de un
vehículo que ya no está —porque te sacaron, o porque se borró— no se ven, aunque su escucha todavía
no se haya cerrado (criterio 13).

No cambia nada del servidor: ni datos, ni reglas, ni funciones, ni índices.

## Lo que dijo el código antes de decidir nada

1. **`IncidentRepository.observe(vehicleId)`** escucha la subcolección entera, sin índice, y deja
   subir el error (spec 021). Es la misma consulta que abre el detalle.
2. **El SDK de Firestore comparte las escuchas de una misma consulta.** Si el detalle, la lista, el
   carrusel y la chapita escuchan lo mismo, es un solo *target* y las lecturas no se multiplican.
   Pasa ya con la consulta de vehículos, que escuchan *Inicio* y *Vehículos*.
3. **[`DATA-MODEL.md`](../../DATA-MODEL.md) §4:** con diez vehículos como mucho, basta una escucha
   por vehículo. Una consulta de grupo de colección necesitaría `memberIds` en cada incidencia y una
   función que lo mantuviera.
4. **Lo que pinta una incidencia ya existe**, en `IncidentUi.kt`: el icono, el color, el nombre y las
   frases de tiempo. El círculo con el color de la categoría estaba dentro de `IncidentItem`, y
   `SectionTitle` era privado del detalle.
5. **La tarjeta ya dejaba el hueco**: el nombre lleva `weight(1f)`, la flecha va después, y los
   iconos caben entre los dos sin cambiar el alto de la fila.
6. **`VehiclesScreen` pasaba los índices del `LazyColumn` tal cual** al reordenar. Con filas delante
   de los vehículos dejarían de coincidir.
7. **El sembrado ya sirve**: el vehículo compartido tiene cuatro tipos abiertos, así que su tarjeta
   enseña tres y «+1».

## Dominio

`domain/model/PendingIncidents.kt`, puro:

```kotlin
sealed interface OpenIncidents {
    data class Loaded(val incidents: List<Incident>) : OpenIncidents   // la más reciente primero
    data object Failed : OpenIncidents
}
data class PendingIncident(val vehicleId: String, val vehicleName: String, val incident: Incident)
data class Pending(val incidents: List<PendingIncident>, val complete: Boolean, val failed: Boolean)

fun pendingOf(vehicles: List<Vehicle>, open: Map<String, OpenIncidents>): Pending
fun openCount(vehicleIds: Set<String>, open: Map<String, OpenIncidents>): Int
```

- `pendingOf` solo mira los vehículos de la lista y junta sus abiertas, la más reciente primero.
  `complete` es que todos tienen ya respuesta; `failed`, que alguno de la lista falló.
- `List<Incident>.openNewestFirst()`, en `Incident.kt`, lo usan también `incidentsView` y el caso de
  uso, para no repetir el filtro y el orden.

**`ObserveOpenIncidentsUseCase(vehicleIds, isSignedIn, onFailure)`**:
- sin ids, un mapa vacío;
- con ids, el `combine` de una escucha por vehículo;
- cada escucha recoge su error: con la sesión viva llama a `onFailure` y emite `Failed`; sin sesión
  no hace nada, como `catchListenerFailure`.

## ViewModels

- **`VehiclesViewModel` y `HomeViewModel`** guardan `openIncidents` en su estado. Un segundo
  colector toma los ids de sus propios vehículos, sin repetir, y abre las escuchas con
  `flatMapLatest`. El mapa no se vacía al reabrir: mientras llega lo de un vehículo nuevo se sigue
  viendo lo que había, y `pendingOf` ignora lo que sobra.
- **`PendingCountViewModel`**, en `TabShell`: uid → vehículos, con `catchListenerFailure` dentro del
  `flatMapLatest` → ids → incidencias → `openCount`.

Los tres registran los fallos en Crashlytics, cada uno con su contexto.

## Interfaz

**Compartido:**
- `SectionTitle` pasa a `presentation/components`.
- `IncidentTypeIcon`: el círculo con el color de la categoría. Lo usan el detalle, «Pendiente» y la
  tarjeta.
- `TimeAgoSentences`: solo el cuándo, «hace 2 h», sin «Reportada».

**«Pendiente», en *Vehículos***, delante de la lista y solo fuera del modo ordenar:
- el título;
- una fila por incidencia: el icono, el nombre y «Furgoneta · hace 2 h». Al tocarla se abre el
  detalle;
- la línea de estado:
  - con un fallo, «No se han podido cargar todas las incidencias»;
  - completa y sin ninguna, «Todo en orden»;
  - mientras carga, nada. Nunca dice que todo está en orden sin saberlo.
- «Tus vehículos», para separar la lista.

**Reordenar** pasa a calcular las posiciones con la clave de cada fila, no con su índice en la lista.

**La tarjeta** recibe sus incidencias abiertas. `cardIncidents(open, max = 3)` da un representante
por tipo, el más reciente, y cuántos tipos sobran. Entre el nombre y la flecha van los iconos, de
24 dp, y «+N». Para el lector de pantalla son una sola descripción: «Poco combustible, Pinchazo y 2
más». Una personalizada se anuncia con su nombre.

**La chapita**: `AparkBottomNavigationBar` acepta `badges: Map<Tab, Int>` y la pinta con
`BadgedBox`, con «99+» por encima de 99. El lector de pantalla dice «Incidencias abiertas: 3».

## Riesgos

- **Al entrar o salir de un vehículo se reabren todas las escuchas de incidencias.** Con el caché es
  inmediato, y no pasa al aparcar. Llevar cada escucha por separado sería más código para un caso
  raro.
- **La carrera al sacarte de un vehículo.** Si el error de su escucha llegara antes que la lista sin
  él, la línea de error se vería un momento y se registraría un fallo que no lo es. Se comprueba al
  verificar.
- **Tres ViewModels registran el mismo fallo**, cada uno con su contexto, igual que ya pasa con la
  lista de vehículos.
