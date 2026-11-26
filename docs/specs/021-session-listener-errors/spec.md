# Spec: Que la app no se cierre cuando falla una escucha de Firestore

- **ID**: 021-session-listener-errors
- **Estado**: Implementada — diseño en [design.md](design.md)
- **Fecha**: 2026-11-26

> El número 020 está reservado para lo que la spec 019 deja pendiente de las incidencias. Esta
> se escribió antes, así que toma el siguiente libre.

## Problema / Por qué

La app **se cierra** si una escucha de Firestore de *Inicio* o de *Vehículos* devuelve un error.
Pasó el 25-11-2026 en el simulador de iOS, al arrancar:

```
Uncaught Kotlin exception: dev.gitlive.firebase.firestore.FirebaseFirestoreException:
  Code=7 "Missing or insufficient permissions."
  … NativeDocumentReference.snapshots …
```

**Cómo se llega ahí:**

1. La app arranca con una sesión guardada. `authStateChanges` emite ese usuario, y *Inicio* empieza
   a escuchar sus vehículos: la consulta de `vehicles` y el documento `users/{uid}`, que da el orden.
2. Firebase intenta refrescar el token y el servidor ya no lo acepta. En el simulador fue una sesión
   de otro backend; en la vida real puede ser:
   - que la contraseña o el email se cambiaran desde otro sitio, porque Firebase revoca la sesión;
   - que la cuenta esté deshabilitada;
   - o que se borrara desde la consola.
3. Firebase cierra la sesión, y Firestore vuelve a enviar las escuchas sin credenciales. Las reglas
   las rechazan: PERMISSION_DENIED.
4. El error llega a la corrutina **antes** de que `flatMapLatest` reciba el `null` de
   `authStateChanges` y cancele esas escuchas. Nadie lo recoge, y una excepción sin recoger en
   `viewModelScope` cierra la app, en iOS y en Android.

**No es nuevo, y ya se había visto a medias.** `AccountViewModel` lo dejó escrito: al cerrar sesión,
su escucha «recibe PERMISSION_DENIED antes de que la pantalla se vaya». Por eso lleva un
`.catch { }`. *Detalle* y *Editar* también lo recogen. Los dos que no lo hacen son los que más
tiempo están abiertos:

| ViewModel | Escucha | ¿Recoge el error? |
|---|---|---|
| `HomeViewModel` | la lista de vehículos (consulta + `users/{uid}`) | **No** |
| `VehiclesViewModel` | la lista de vehículos | **No** |
| `VehicleDetailViewModel` · `EditVehicleViewModel` | la lista de vehículos | Sí, y lo enseña |
| `AccountViewModel` | `users/{uid}` | Sí, en silencio, aunque sea un fallo de verdad |

Cerrar sesión con el botón no suele provocarlo, porque ahí casi siempre gana `authStateChanges`.
Es una carrera, y cuando el cierre viene de fuera, la pierde.

**Crashlytics de `apark-dev`** no tiene ningún cierre en los últimos 90 días, ni en Android ni en
iOS. Producción no se ha mirado.

## Objetivos

- **Ninguna escucha de Firestore cierra la app**, termine la sesión como termine.
- **Que la salida siga siendo una**: cuando la sesión muere, `SessionViewModel` lleva al login,
  como desde la spec 011.
- **Separar dos casos.** Un error con la sesión viva es un fallo de verdad: se registra en
  Crashlytics. Un error porque la sesión terminó no lo es, y no se registra.

## No-objetivos

- **Cambiar cómo se detecta que la sesión terminó.** Eso ya lo hace `SessionViewModel`.
- **Reintentar las escuchas.** Firestore ya reintenta por su cuenta los errores pasajeros, como
  quedarse sin red. Solo entrega los que no se arreglan repitiendo, como un permiso denegado.

## Historias de usuario

- Como usuario al que le han cerrado la sesión desde otro sitio, quiero volver al login, no que la
  app se cierre.
- Como desarrollador, quiero enterarme por Crashlytics cuando una escucha falla con la sesión viva,
  porque eso es un fallo de las reglas o de la app.

## Criterios de aceptación

1. **Dado** que la app está abierta en cualquier pestaña, **cuando** la sesión termina desde fuera
   y las escuchas reciben PERMISSION_DENIED, **entonces** la app no se cierra y lleva al login.
2. **Dado** una sesión guardada que el servidor ya no acepta, **cuando** abro la app, **entonces**
   llego al login, sin cierre.
3. **Dado** que una escucha de *Inicio* o de *Vehículos* falla con la sesión todavía viva,
   **cuando** pasa, **entonces** la app no se cierra, el fallo llega a Crashlytics como no fatal y
   sale el aviso «No se han podido cargar tus vehículos». Se queda lo que ya se veía.
4. **Dado** que el error vino de que la sesión terminó, **cuando** se recoge, **entonces** no se
   registra en Crashlytics.
5. **Dado** que la escucha de *Cuenta* falla con la sesión viva, **cuando** pasa, **entonces** se
   registra en Crashlytics, como las demás. Hoy se traga en silencio.
6. **Dado** Android y iOS, **cuando** se da cualquiera de los casos anteriores, **entonces** se
   comporta igual.

## Preguntas abiertas — resueltas

1. **¿Dónde se recoge el error?** → **En cada ViewModel**, con un `catch` dentro del
   `flatMapLatest`, como ya hacían *Detalle* y *Editar*. La regla se apunta en AGENTS.
   - La alternativa era llevarlo al tipo: que los repositorios devolvieran el error como dato
     (`Flow<Result<…>>`) y el compilador obligara a tratarlo. Se descartó por su coste: seis
     usuarios y tres firmas, para dos ViewModels que lo habían olvidado.
2. **¿Qué ve el usuario si falla con la sesión viva?** → **Un aviso**: «No se han podido cargar tus
   vehículos». Sin él, si no había cargado nada, *Inicio* enseña la tarjeta de añadir y parece que
   no tienes vehículos. Solo sale con la sesión viva: si terminó, la app ya va al login.
3. **¿Se alinea *Cuenta*?** → **Sí**: registra en Crashlytics los fallos con la sesión viva, como
   las demás. No enseña aviso, porque lo único que escucha es el nombre.

## Fuera de alcance

- **Mirar Crashlytics de producción.**
- **Las demás llamadas que no son escuchas** (escrituras y funciones). Esas ya devuelven `Result`.
