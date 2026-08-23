# Design: la pertenencia como consulta

- **Spec**: [spec.md](spec.md)
- **Estado**: Aprobado

## Enfoque

Cada vehículo pasa a llevar `memberIds`, un array con **todos** sus miembros, dueño incluido, y la
lista de vehículos se obtiene con `where { "memberIds" contains uid }` en vez de abriendo un listener
por cada id de `userVehicles`. Con la regla `allow read: if request.auth.uid in resource.data.memberIds`,
Firestore puede demostrar que la consulta es segura y la acepta, de modo que **nunca devuelve un
documento prohibido**: eso elimina por construcción el `PERMISSION_DENIED` sobre vehículos propios y
el `retryWhen` que existía para sobrevivirlo.

`userVehicles` no desaparece, pero cambia de papel: pasa de ser *la fuente* de la lista a ser **solo
la pista de ordenación**, así que un id colgante deja de ser un fallo y pasa a ser ruido.

Verificado que la API existe en gitlive 2.4.0: el infijo `contains` construye un filtro
`arrayContains` (`firestore.kt:356`).

## Archivos / módulos afectados

Se reparte en **dos PR** para poder revertir por separado el andamiaje y el cambio de comportamiento.

### PR A — herramientas y reglas

| Archivo | Cambio |
|---------|--------|
| `tools/migrate-member-ids.ts` | *(nuevo)* backfill idempotente, `--dry-run`, y la limpieza tras bandera |
| `tools/package.json`, `tools/tsconfig.json` | *(nuevos)* aislados de `functions/`, que sí se despliega |
| `firestore.rules` | `read`, `create`, `update` y la rama de salida pasan a `memberIds` |
| `docs/FIREBASE.md` | la secuencia operativa de cuatro pasos, para que sea reproducible |

### PR B — cliente y funciones

| Archivo | Cambio |
|---------|--------|
| `domain/model/Vehicle.kt` | `memberIds`; fuera `inviteCode` y `sharedUsers`; marcas de tiempo |
| `data/repository/FirestoreVehicleRepository.kt` | **el núcleo**: `getVehiclesForUser` pasa de N listeners a dos; fuera el `retryWhen` |
| `data/util/FirestoreConstants.kt` | constante nueva, fuera la de `sharedUsers` |
| `domain/usecase/UpdateVehicleLocationUseCase.kt` | recortar el `user` incrustado |
| `functions/src/invites.ts` | `joinWithCodeHandler` escribe `memberIds` |
| `functions/src/index.ts` | el trigger de limpieza lee `memberIds` |
| `presentation/home/` | sin cambios: `isOwner` sigue comparando con `ownerId` |

## Cambios de datos y reglas Firestore

**Modelo**: `Vehicle` gana `memberIds: List<String>`, `createdAt` y `updatedAt`; pierde `sharedUsers`
y `inviteCode`. `Vehicle.LocationModel.user` deja de ser un `User` completo y pasa a
`{uid, name, email}`. `User` gana también `createdAt` / `updatedAt`.

**Firestore**: `getVehiclesForUser` pasa de **N+1 listeners a dos** — la consulta sobre `vehicles` y
el documento propio del usuario para el orden—, combinados con `combine`. Crear y salirse siguen
siendo escrituras atómicas en batch sobre dos documentos.

**Reglas**: `read`, `create` y la rama de salida de `update` cambian de `sharedUsers` a `memberIds`.
La rama de salida añade `uid != ownerId`, porque con el dueño dentro del array su salida dejaría un
vehículo sin dueño legible. Se despliegan **primero a `apark-dev` y después a producción**, que ahora
son proyectos distintos.

## Decisiones y alternativas consideradas

- **Decisión**: `sharedUsers` desaparece; `memberIds` incluye al dueño. **Alternativa**: mantener los
  dos. Descartada porque sería duplicar estado derivable, justo lo que este cambio viene a corregir.
- **Decisión**: el mapa `members` de [DATA-MODEL.md](../../DATA-MODEL.md) **no entra**.
  **Alternativa**: añadirlo ya. Descartada: no hay pantalla que muestre miembros, y desnormalizar
  antes de que exista la consulta es lo que ese documento desaconseja.
- **Decisión**: salirse sigue siendo del cliente, con una regla que **demuestra** que solo puedes
  quitarte a ti. **Alternativa**: una callable. Descartada: añade un viaje de red y un despliegue sin
  ganar seguridad. El principio de "los datos de autorización los escribe el servidor" protege contra
  cambiar *quién más* accede, y eso sigue garantizado.
- **Decisión**: migración por **vía corta**, sin reglas transitorias ni doble escribir.
  **Alternativa**: ventana de compatibilidad completa. Descartada por acuerdo: los dos únicos
  usuarios son dispositivos propios que se actualizan a mano.
- **Decisión**: el backfill es un **script versionado en el repo**, no una Cloud Function.
  **Alternativa**: una función. Descartada: se ejecuta un puñado de veces y no debe vivir para
  siempre. Que esté en el repo es la lección de la política TTL, que no está en ningún sitio.

## Riesgos

- **Ventana en la que la app vieja no puede escribir.** Entre desplegar las reglas y actualizar los
  móviles, una app sin actualizar **lee pero no puede crear vehículos ni salirse**. *Mitigación*:
  actualizar los dos dispositivos justo después, y ensayar la secuencia entera en `apark-dev`.
- **Borrar `sharedUsers` demasiado pronto** echaría fuera a los miembros compartidos mientras siga
  habiendo reglas viejas en algún sitio. *Mitigación*: la limpieza es el paso 4, después de actualizar
  las apps, y va tras una bandera explícita del script.
- **El backfill deja vehículos sin `memberIds`** y esos desaparecerían de la lista. *Mitigación*:
  `--dry-run`, idempotencia, y comprobar con el MCP que no queda ninguno sin el campo.

## Resolución de las preguntas abiertas de la spec

1. **¿Sobrevive `sharedUsers`?** → **No.** `memberIds` incluye al dueño; `ownerId` se queda porque
   decide quién borra y quién comparte.
2. **¿Quién mantiene `memberIds` y cómo lo garantizan las reglas?** → Crear: cliente, con la regla
   exigiendo `memberIds == [uid]` y `ownerId == uid`. Unirse: la callable, con Admin SDK. Salirse:
   cliente, con regla de auto-eliminación que además prohíbe al dueño usar esa vía. Transferir: sin
   UI todavía, pero la regla queda lista.
3. **Compatibilidad durante el despliegue** → **La regla nueva de lectura es compatible con el
   cliente viejo**, que lee documento a documento y pasa `uid in memberIds` sin enterarse, siempre
   que el backfill haya corrido antes. Con eso basta la vía corta de cuatro pasos.
4. **El backfill** → script de Node con `firebase-admin` en `tools/`, idempotente, con `--dry-run`,
   ejecutado contra `apark-dev` antes que contra producción.
5. **¿Se cierra la spec 004?** → **No, se mitiga.** Desaparece la amplificación de ~3 s de los
   reintentos, pero Firestore también cachea resultados de consulta y el parpadeo sigue siendo
   posible. Se anotará en la spec 004, que se queda aparcada.
6. **¿Hace falta un índice?** → **No.** `array-contains` sobre un campo único usa el índice
   automático, y no hay `orderBy` en la consulta porque el orden se aplica en cliente.
