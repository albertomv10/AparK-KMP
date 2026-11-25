# Design: las incidencias de un vehículo

- **Spec**: [spec.md](spec.md)
- **Estado**: Aprobado

## Enfoque

Las incidencias son una **subcolección de cada vehículo**. Las reglas solo dejan entrar a sus
miembros. El móvil escucha la subcolección entera y separa las abiertas de las resueltas. Las
resueltas caducan a los 30 días con la caducidad de Firestore (TTL), así que la subcolección se
queda pequeña y **no hace falta ningún índice**. Las escrituras no esperan al servidor, para que
todo funcione sin conexión, y la hora la pone siempre el servidor.

## Lo que dijo el código antes de decidir nada

1. **No había subcolecciones** y `firestore.indexes.json` estaba vacío. Las reglas de `vehicles`
   comprueban la pertenencia con `uid in memberIds`.
2. **gitlive 2.4 tiene todo lo que hace falta:**
   - `DoubleAsTimestampSerializer` guarda los milisegundos como `Timestamp` y admite la hora del
     servidor (`SERVER_TIMESTAMP`);
   - `ServerTimestampBehavior.ESTIMATE` estima esa hora mientras no hay conexión;
   - `CollectionReference.document` da un id sin ir al servidor.
   
   En cambio, `add()` espera al servidor y sin conexión no vuelve nunca, igual que pasó en la spec
   016.
3. **`cleanupVehicleReferences` ya hace dos limpiezas en paralelo** al borrar un vehículo, y
   `firebase-admin` 13 trae `recursiveDelete`.
4. **La caducidad de `invites` se creó a mano**, porque `firebase-tools` no tiene comando para eso.
5. **El detalle ya carga los miembros** con `getVehicleMembers`: uid y nombre. De ahí sale el nombre
   de quien reportó o resolvió.
6. **`ParkedText`** (spec 016) ya construye «hace 5 min por Edu / por ti». Las incidencias
   necesitan lo mismo, con «Reportada» y «Resuelta».

## Datos

`vehicles/{vehicleId}/incidents/{auto-id}`: `category`, `tag` (`""` si no tiene), `note` (`""`,
hasta 280 caracteres), `status` (`open` o `resolved`), `reportedBy` (uid) y `createdAt` (hora del
servidor). Al resolverla se añaden `resolvedBy`, `resolvedAt` (hora del servidor) y `expiresAt`
(resolución + 30 días), que es el campo de la caducidad.

| Categoría | Código | Etiquetas |
|---|---|---|
| Niveles y energía | `levels` | `low_fuel` · `low_charge` · `oil` · `washer_fluid` · `adblue` |
| Mecánica y testigos | `mechanics` | `warning_light` · `wont_start` · `breakdown` · `strange_noise` · `bulb_out` |
| Neumáticos | `tyres` | `puncture` · `low_pressure` · `tyre_wear` |
| Carrocería e interior | `body` | `damage` · `cleaning` · `items_inside` |
| Otro | `other` | — |

**Qué etiquetas se ofrecen según el vehículo** (`IncidentTag.appliesTo`):
- si el vehículo no tiene datos, todas;
- «Poco combustible» y «Aceite», nunca en un eléctrico;
- «Poca carga», solo en un eléctrico o un híbrido enchufable;
- «AdBlue», solo si es diésel, o si no se sabe el combustible y no es eléctrico ni de pila de
  combustible.

## Reglas

- **Leer, crear, modificar y borrar exigen ser miembro**, y para comprobarlo cada regla lee el
  vehículo padre con `get()`.
- **Crear:**
  - solo con esos campos;
  - la categoría tiene que estar en la lista, y la etiqueta ser de esa categoría;
  - la nota, de 280 caracteres como mucho;
  - `status` tiene que ser `open`, `reportedBy` tiene que ser quien escribe, y `createdAt`, la hora
    del servidor.
- **Modificar** solo puede hacer dos cosas:
  - **resolver:** de `open` a `resolved`, con `resolvedBy` de quien escribe, `resolvedAt` a la hora
    del servidor, y `expiresAt` como mucho a 31 días;
  - **deshacer:** de vuelta a `open`, sin esos tres campos.
- **Borrar:** quien la reportó o el dueño del vehículo.

## Backend

- **`deleteVehicleIncidents`** borra la subcolección con `recursiveDelete`, en paralelo con las
  otras dos limpiezas de `cleanupVehicleReferences`.
- **Borrar una cuenta no cambia:**
  - si el vehículo se borra, sus incidencias se van con él;
  - si no, el uid se queda en sus incidencias y el nombre ya no se encuentra.
- **La caducidad de `incidents` sobre `expiresAt` se crea a mano**, en las dos bases.

## Interfaz

**En el detalle, la sección «Incidencias»**, después de «Dónde está»:
- cada abierta lleva:
  - el icono, que es el de su etiqueta o, si no tiene, el de su categoría;
  - su nombre y la nota;
  - «Reportada hace 2 h por Edu / por ti»;
  - el botón de resolver, y «Borrar» si se puede;
- debajo, «Reportar incidencia»;
- y «Resueltas (n)», plegada.

**Resolver** enseña un aviso con «Deshacer», y **borrar** pide confirmación.

**El formulario es una hoja inferior** con:
- la categoría;
- las etiquetas que tienen sentido para ese vehículo;
- la nota.

Las frases de tiempo se comparten con `ParkedText`.

## Riesgos

- **La caducidad no está en el repositorio.** Si nadie la crea, las resueltas se acumulan, aunque la
  app oculta igualmente las de más de 30 días.
- **`expiresAt` lo calcula el móvil.** Un miembro podría acortarlo, y lo peor que pasaría es que una
  resuelta desapareciera antes de tiempo.
