# Design: que la app no se cierre cuando falla una escucha de Firestore

- **Spec**: [spec.md](spec.md)
- **Estado**: Aprobado (modo plan, 2026-11-26)

## Enfoque

Cada escucha de Firestore que se recoge en un ViewModel pasa por una función común,
`catchListenerFailure`. Esa función distingue dos casos:

- **La sesión terminó:** no hace nada, porque la salida al login ya la lleva `SessionViewModel`.
- **La sesión sigue viva:** es un fallo de verdad. Llama a `onFailure`, que lo registra en
  Crashlytics y, en *Inicio* y *Vehículos*, enseña un aviso.

Va **dentro** del `flatMapLatest`, para que la escucha de la sesión siga viva aunque falle la de los
datos.

**Cómo se sabe si la sesión terminó: con `getCurrentUser()`, no con `authStateChanges`.** Cuando
Firebase cierra la sesión, primero pone el usuario a `null` y después avisa a sus oyentes, entre ellos
Firestore. Así que cuando el PERMISSION_DENIED llega a la corrutina, `getCurrentUser()` ya es `null`.
`authStateChanges` es precisamente la señal que llega tarde y provoca la carrera.

```kotlin
fun <T> Flow<T>.catchListenerFailure(
    isSignedIn: () -> Boolean,
    onFailure: (Throwable) -> Unit
): Flow<T> = catch { error -> if (isSignedIn()) onFailure(error) }
```

Recibe lambdas y no el repositorio, para poder probarla sin un `AuthRepository` falso entero.

## Archivos / módulos afectados

| Archivo | Cambio |
|---------|--------|
| `utils/ListenerFailures.kt` (nuevo) | `catchListenerFailure` |
| `presentation/home/HomeViewModel.kt` · `HomeScreen.kt` | La recoge dentro del `flatMapLatest`: registra y avisa con `error_vehicles_load`. Se queda la lista que hubiera |
| `presentation/vehicles/VehiclesViewModel.kt` · `VehiclesScreen.kt` | Igual, con `errorKey = ERROR_LOAD_KEY`. La pantalla elige el texto según la clave, porque hasta ahora enseñaba siempre el de reordenar |
| `presentation/account/AccountViewModel.kt` | Su `.catch { }` pasa a la función común: registra, sin aviso |
| `presentation/vehicledetail/VehicleDetailViewModel.kt` · `presentation/editvehicle/EditVehicleViewModel.kt` | Su `.catch` pasa a la función común. Hacen lo mismo que antes, pero ya no registran cuando la sesión terminó |
| `di/SharedModule.kt` | `crashReporter` en `VehiclesViewModel` y `AccountViewModel` |
| `composeResources/values{,-en,-fr}/strings.xml` | `vehicles_load_error` |
| `commonTest/.../session/ListenerFailuresTest.kt` (nuevo) | Con sesión y sin sesión |

## Cambios de datos y reglas Firestore

Ninguno.

## Decisiones y alternativas consideradas

- **Decisión:** un `catch` común en cada ViewModel, con la regla apuntada en AGENTS.
  - **Alternativa:** llevar el error al tipo. Los repositorios devolverían `Flow<Result<…>>` y el
    compilador obligaría a tratarlo.
  - Se descartó por su coste: tres firmas y seis usuarios, para dos ViewModels que lo habían
    olvidado.
- **Decisión:** no reintentar.
  - Firestore ya reintenta por su cuenta los errores pasajeros, y solo entrega los permanentes.
  - Ni un reintento automático ni un botón «Reintentar» arreglarían un permiso denegado.
- **Decisión:** el aviso solo sale con la sesión viva.
  - Si la sesión terminó, la app ya va al login, y un aviso en el camino solo confundiría.

## Riesgos

- **Un ViewModel futuro puede olvidarlo**, como estos dos. Por eso se apunta en AGENTS.
- **La interfaz de la spec 019, que está en otra rama,** tiene que adoptar la misma regla al
  actualizarse con `main`:
  - `FirestoreIncidentRepository.observe` deja de tragarse el error con `emptyList()`;
  - `IncidentsViewModel` lo recoge con `catchListenerFailure`.

## Resolución de las preguntas abiertas de la spec

- **¿Dónde se recoge?** → En cada ViewModel, con la función común.
- **¿Qué ve el usuario?** → El aviso «No se han podido cargar tus vehículos», solo con la sesión
  viva.
- **¿Se alinea *Cuenta*?** → Sí: registra en Crashlytics, sin aviso.
