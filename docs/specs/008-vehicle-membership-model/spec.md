# Spec: La pertenencia como consulta, no como lista de ids

- **ID**: 008-vehicle-membership-model
- **Estado**: Aprobada
- **Fecha**: 2026-08-07

## Problema / Por qué

Tres de los problemas que más código han costado en este proyecto tienen **la misma causa**, y
hasta ahora se han tratado como si fueran tres:

| Síntoma | Dónde está el parche |
|---------|----------------------|
| La app **crasheaba** al crear un vehículo | `retryWhen` + `catch` en `getVehiclesForUser` |
| **Ids colgantes** en el `userVehicles` de los demás miembros | Toda la [spec 002](../002-vehicle-cleanup-function/spec.md) y su Cloud Function |
| **Tarjetas fantasma** de vehículos que ya no existen | [spec 004](../004-stale-cache-cards/spec.md), aparcada sin resolver |

La causa común es cómo se lee la lista de vehículos:

```
users/{uid}.userVehicles → [id1, id2, id3]
   └── por cada id: un listener sobre vehicles/{id}
        └── combine(...)
```

**La lista de vehículos se deriva de un array de ids que puede mentir.** Nada garantiza que un id
de `userVehicles` corresponda a un documento que existe y que puedas leer. Y cuando no existe,
Firestore no responde "no encontrado": la regla `allow read` desreferencia un `resource` nulo y
devuelve **`PERMISSION_DENIED`**, que es indistinguible de un fallo transitorio real. De ahí los
3 reintentos de 1 s, que a su vez alargan la ventana de la tarjeta fantasma.

Además son **N listeners para N vehículos**, y ese número solo va a crecer: las fases siguientes
del [roadmap](../../ROADMAP.md) añaden incidencias, mantenimiento e ITV como subcolecciones, todas
colgando de la pertenencia a un vehículo.

## Objetivos

- Que la lista de vehículos salga de **una consulta sobre la pertenencia**, no de un array de ids
  guardado aparte.
- Que **desaparezca la clase de error** `PERMISSION_DENIED` sobre vehículos de la propia lista, y
  con ella el `retryWhen` que existe para sobrevivirla.
- Que un id colgante deje de ser un fallo y pase a ser ruido inofensivo.
- Que el **orden por usuario** de la [spec 003](../003-reorder-vehicles/spec.md) se conserve
  exactamente como está de cara al usuario.
- Dejar el modelo listo para que las subcolecciones de las fases 2 y 3 puedan comprobar
  pertenencia de una sola forma.

## No-objetivos

- **Cambiar nada de lo que ve el usuario.** Si esta spec se implementa bien, la app se comporta
  igual: mismas tarjetas, mismo orden, mismos permisos. Solo va más rápido y falla menos.
- Rehacer el sistema de invitaciones ([spec 005](../005-share-vehicle/spec.md)), que funciona.
- Retirar la Cloud Function de la spec 002: sigue siendo útil para dejar `userVehicles` limpio.
  Lo que cambia es que **deja de sostener el peso** de que la app funcione.
- Introducir un concepto de grupo, hogar o familia.

## Historias de usuario

- Como usuario, quiero que la app no me enseñe vehículos que ya no tengo, y que no tarde en
  arrancar por culpa de referencias muertas.
- Como desarrollador, quiero una única forma de responder "¿es esta persona miembro de este
  vehículo?", para que incidencias, mantenimiento y ubicaciones no inventen cada una la suya.

## El cambio

Añadir a cada vehículo un array con **todos** sus miembros, dueño incluido:

```
vehicles/{id}.memberIds: [uid_dueño, uid_compartido, ...]
```

Y leer así:

```kotlin
vehicles.where("memberIds", "array-contains", miUid).snapshots   // 1 listener, 1 query
```

Con la regla `allow read: if request.auth.uid in resource.data.memberIds`, Firestore puede
demostrar que la consulta nunca devolverá un documento prohibido, y la acepta. Es el patrón
documentado para este caso.

`ownerId` **se queda**: sigue siendo lo que distingue borrar de salirse, y quién puede compartir.
`memberIds` no sustituye a la propiedad, solo hace consultable la pertenencia.

`userVehicles` **se queda también**, pero cambia de papel: pasa de ser *la fuente* de la lista a
ser **solo la pista de ordenación**. Los ids que sobran se ignoran; los vehículos que aparecen en
la consulta pero no en la pista van al final. Un id colgante deja de tener consecuencias.

### Cambios de modelo que viajan en la misma migración

Se agrupan aquí porque tocan los mismos documentos y las mismas reglas, y separarlos significaría
migrar dos veces:

1. **`createdAt` / `updatedAt`** en `users` y `vehicles`. Hoy no hay una sola marca de tiempo, y
   las van a necesitar las incidencias (ordenar), las notificaciones (saber qué ha cambiado) y
   cualquier depuración futura.
2. **Quitar `Vehicle.inviteCode`**: resto de antes de la spec 005. Nada lo escribe.
3. **Arreglar `lastLocation.user`**: hoy se guarda un `User` completo, con su `userVehicles`
   dentro ([`UpdateVehicleLocationUseCase.kt:34`](../../composeApp/src/commonMain/kotlin/com/albertomedina/apark/domain/usecase/UpdateVehicleLocationUseCase.kt#L34)),
   así que cualquier miembro de un vehículo compartido puede leer los ids de todos los vehículos
   de quien lo aparcó. Debe guardarse `{uid, name, email}` y nada más.

## Criterios de aceptación

1. **Dado** un usuario con varios vehículos, **cuando** abre la app, **entonces** los ve todos, en
   el mismo orden que antes de este cambio, y la app abre **un solo listener** sobre `vehicles`.
2. **Dado** un id en mi `userVehicles` cuyo vehículo ya no existe, **cuando** abro la app,
   **entonces** no se produce ningún `PERMISSION_DENIED` ni ningún reintento, y no aparece ninguna
   tarjeta para él.
3. **Dado** que reordeno mis vehículos, **cuando** lo hago, **entonces** el orden se conserva y
   **no afecta** al orden de los demás miembros (la spec 003 sigue cumpliéndose entera).
4. **Dado** un vehículo compartido, **cuando** el dueño lo elimina, **entonces** desaparece de la
   lista de todos los miembros sin que ninguno vea un error.
5. **Dado** que me uno a un vehículo con un código, **cuando** la operación termina, **entonces**
   aparece en mi lista, y mi uid está en su `memberIds`.
6. **Dado** que me salgo de un vehículo compartido, **cuando** la operación termina, **entonces**
   mi uid **ya no está** en su `memberIds` y dejo de poder leer el documento.
7. **Dado** un intento de añadirse a sí mismo al `memberIds` de un vehículo ajeno, **cuando** se
   ejecuta contra las reglas, **entonces** se rechaza.
8. **Dado** que aparco un vehículo compartido, **cuando** otro miembro lo mira, **entonces** ve mi
   nombre y **no** puede leer los ids de mis otros vehículos.
9. **Dado** el `retryWhen` de `getVehiclesForUser`, **cuando** termine esta spec, **entonces** ya
   no existe (o queda justificado por escrito por qué sigue).

## Preguntas abiertas — **todas resueltas en [design.md](design.md)**

1. **¿Sobrevive `sharedUsers`, o se deriva de `memberIds`?** Mantener los dos es duplicar estado y
   arriesgarse a que se desincronicen; quitarlo obliga a reescribir la regla de "salirse" y la
   Cloud Function de limpieza, que hoy lo leen.
2. **¿Quién mantiene `memberIds` y cómo lo garantizan las reglas?** Cambia en cuatro sitios:
   crear (cliente), unirse (función), salirse (cliente), transferir propiedad (todavía sin UI).
   Las reglas tienen que impedir que un cliente se meta donde no debe, y eso es lo más delicado
   del diseño.
3. **La compatibilidad durante el despliegue.** Hay una build de release instalada en un iPhone
   real, y esa versión lee por ids y no sabe nada de `memberIds`. Si las reglas cambian de golpe,
   esa app deja de funcionar. ¿Hace falta una ventana en la que las reglas acepten las dos formas?
   ¿Y en qué orden se despliegan reglas, backfill y app?
4. **El backfill.** Los vehículos existentes no tienen `memberIds`, y sin él no salen en ninguna
   consulta — o sea, **desaparecen**. Hay que decidir si es un script puntual con `firebase-admin`
   o una función, y **en qué momento del despliegue** corre. Hay que hacerlo en las dos bases de
   datos.
5. **¿Se cierra con esto la [spec 004](../004-stale-cache-cards/spec.md)?** Mi lectura es que
   **no del todo**: Firestore también cachea resultados de consulta, así que un vehículo borrado
   con la app cerrada todavía puede parpadear. Lo que sí desaparece es la amplificación de ~3 s de
   los reintentos. Hay que decidir si eso basta para cerrar la 004 o si se queda aparcada.
6. **¿Hace falta un índice?** `array-contains` sobre un solo campo usa el índice automático. Si el
   diseño acaba necesitando ordenar en el servidor, ya no. Confirmar antes de implementar.

## Fuera de alcance

- Denormalizar `memberIds` en los documentos hijos de las subcolecciones futuras. Cuando existan,
  sus reglas leerán el vehículo padre con `get()`; se optimizará si duele.
- La UI de transferir propiedad (fase 2 del roadmap), aunque esta spec deba dejar la regla lista.
- Cambiar `users/{uid}` por algo que no sea un documento con arrays.

## Notas

Este cambio es barato **hoy** y caro más adelante. En cuanto existan incidencias, mantenimiento y
ubicaciones como subcolecciones, cambiar el modelo de pertenencia significa migrar también sus
reglas y sus datos. Los usuarios reales de la base de release se cuentan con los dedos de una mano:
no va a haber un momento mejor.
