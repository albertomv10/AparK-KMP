# Design: Caducidad real de las invitaciones

- **Spec**: [spec.md](spec.md)
- **Estado**: Propuesto
- **Fecha**: 2026-08-07

## Enfoque: dos piezas, cada una para lo suyo

**Una política TTL de Firestore** para el paso del tiempo, y **una línea en el trigger que ya
existe** para el borrado de vehículos. No hace falta nada más.

### Por qué TTL y no una función programada

Firestore trae este mecanismo incorporado: se le señala un campo de tipo timestamp y borra solo los
documentos cuya fecha ya pasó. Encaja aquí porque **las tres formas de acumular tienen `expiresAt`**
—caducadas, usadas y huérfanas—, así que un único mecanismo las cubre todas.

La alternativa, una `onSchedule` que barra la colección cada día, sería más código, otro despliegue
que mantener y un Cloud Scheduler que pagar, para hacer peor lo que la plataforma ya hace.

Se descartó también **borrar al leer** (que `joinVehicleWithCode` borre la invitación cuando la
encuentra caducada): solo limpiaría las que alguien intenta usar, que son justo las que menos
molestan.

### La configuración, y por qué el desfase no es cero

| Campo | Valor |
|-------|-------|
| Grupo de colección | `invites` |
| Campo de marca de tiempo | `expiresAt` |
| Desfase de vencimiento | **7 días** |
| Bases de datos | `apark-at` **y** `(default)`, por separado |

El desfase se **suma** al valor del campo: con 7 días, el documento se borra una semana después de
caducar la invitación.

Poner 0 es lo inmediato y funcionaría, pero empeora el mensaje. Con 0, en cuanto pasa la fecha el
documento es candidato a borrarse, y a partir de ahí quien introduzca ese código recibe *"no es
válido"* — que le hace pensar que ha tecleado mal— en lugar de *"ha caducado"*, que le diría que
pida otra. La semana de gracia mantiene el mensaje preciso justo durante los días en que alguien
podría intentar usar un código que le llegó tarde, y después limpia. El coste de esa semana es
irrelevante, que es precisamente por qué se puede pagar.

Firestore no borra en el instante exacto: lo habitual es **dentro de las 24 h siguientes** a la
fecha de vencimiento. Da igual, porque nada depende del momento preciso.

### Sobre el aviso de los índices automáticos

La consola advierte de que el campo con TTL recibe un índice automático y que eso puede dar
problemas. El riesgo real es el de siempre con un índice sobre un timestamp que solo crece: a
volúmenes altos de escritura, todas las escrituras caen en el mismo extremo del índice y se crea un
punto caliente. El umbral está en el orden de cientos de escrituras por segundo sobre esa
colección; AparK escribe en `invites` una vez por compartición. No aplica.

## El caso que el TTL cubre tarde: borrar un vehículo

Al eliminar un vehículo, sus invitaciones acabarían cayendo por caducidad, pero mientras tanto
quedan códigos vivos apuntando a algo que ya no existe. Eso se arregla donde ya se arregla el resto:
`cleanupVehicleReferences`, el trigger `onDocumentDeleted` de la [spec 002](../002-vehicle-cleanup-function/spec.md),
que hoy solo toca `userVehicles`.

Se le añade una consulta por `vehicleId` sobre `invites` y un borrado en lote, en el mismo trigger y
sin desplegar nada nuevo. El Admin SDK salta las reglas, así que la colección sigue cerrada al
cliente.

## Archivos afectados

| Archivo | Cambio |
|---------|--------|
| `functions/src/index.ts` | El trigger borra también las invitaciones del vehículo |
| Consola de Firebase | Política TTL en las dos bases de datos (**no es código**) |
| `docs/specs/005-share-vehicle/design.md` | Se corrige la decisión 4, que decía que no haría falta TTL |
| `AGENTS.md` | La política TTL es infraestructura invisible en el repo: hay que dejarla escrita |

## Riesgos

- **Activarlo borra datos de producción.** Los documentos ya caducados desaparecerán. Son invitaciones
  muertas, pero es un borrado real y lo aprueba Alberto, no el asistente.
- **La política vive fuera del repositorio.** No hay `firestore.rules` que la versione ni despliegue
  que la reproduzca: si mañana se crea otra base de datos, hay que acordarse. De ahí la nota en
  `AGENTS.md`.
- **`firebase-tools` no configura TTL** y `gcloud` no está instalado en la máquina, así que el paso
  es manual en la consola.
