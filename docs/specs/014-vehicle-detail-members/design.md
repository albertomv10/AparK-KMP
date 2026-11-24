# Design: detalle del vehículo, miembros y propiedad

- **Spec**: [spec.md](spec.md)
- **Estado**: Aprobado

## Enfoque

El detalle se rehace por **secciones**: cabecera, dónde está, miembros y, al final, las acciones que
sacan de algo. La fase 3 añadirá las suyas sin tocar las demás.

El vehículo llega **en tiempo real** desde la misma consulta que usa el mapa, y no con un listener
sobre el documento. Así, si te quitan o lo borran, simplemente deja de aparecer, sin errores de
permisos. Los **nombres de los miembros** los da una callable, `getVehicleMembers`, porque nadie
puede leer el `users` de otro. **Quitar** y **pasar la propiedad** se hacen desde el cliente, con
dos condiciones nuevas en las reglas que lo demuestran.

## Lo que dijo el código antes de decidir nada

1. **Nadie puede leer el `users/{uid}` de otro**, y no debe: ese documento lleva `userVehicles`, los
   ids de todos tus vehículos.
2. **El dueño puede escribir cualquier cosa en su vehículo**: un `ownerId` que no es miembro, o uids
   nuevos en `memberIds`, saltándose las invitaciones.
3. **`removeUserFromVehicle` no sirve para echar a alguien.** En el mismo batch quita el vehículo del
   `users` de esa persona, que las reglas no dejan escribir, y falla entero. Para salirse funciona
   porque solo tocas el tuyo.
4. **`ShareVehicleWithUserUseCase` y `TransferVehicleOwnershipUseCase` no están en Koin.** El
   primero, además, lee el `users` de otro en una transacción: no puede funcionar.
5. **Registrarse solo pide email y contraseña**, así que muchas cuentas no tienen nombre.
6. **Home ya sabe centrar un vehículo por id** (`followedVehicleId`), y `TabShell` ya cambia de
   pestaña y maneja sus pilas.
7. **La lista de vehículos ya es tiempo real y segura**: es una consulta por `memberIds` que
   Firestore puede demostrar. Un listener sobre el documento suelto devolvería PERMISSION_DENIED en
   cuanto te quitaran (ver los gotchas de [`AGENTS.md`](../../../AGENTS.md)).

## Archivos / módulos afectados

| Archivo | Cambio |
|---------|--------|
| `functions/src/members.ts` · `membersPlan.ts` · `index.ts` | La callable `getVehicleMembers`, con `memberList` pura y sin imports |
| `functions/src/` (`requireUid`) | Compartido; hoy está duplicado en `invites.ts` y `account.ts` |
| `tools/test/members.test.ts` · `vehicles.rules.test.ts` | Tests de `memberList` y de las reglas nuevas |
| `firestore.rules` | Dos condiciones nuevas en la rama del dueño |
| `domain/model/VehicleMember.kt` · `domain/repository/MemberRepository.kt` · `data/repository/FirebaseMemberRepository.kt` | Los miembros |
| `domain/repository/VehicleRepository.kt` · `data/repository/FirestoreVehicleRepository.kt` | `removeMember`; `updatedAt` al transferir; fuera `shareVehicleWithUser` |
| `domain/usecase/` | `GetVehicleMembersUseCase` y `RemoveVehicleMemberUseCase`; fuera `ShareVehicleWithUserUseCase` |
| `presentation/vehicledetail/` | La pantalla y su ViewModel |
| `presentation/home/HomeScreen.kt` · `presentation/navigation/TabShell.kt` | «Ver en el mapa» |
| `di/SharedModule.kt` · `composeResources/values*/strings.xml` | Koin y textos |

## Cambios de datos y reglas Firestore

- **Modelo**: no cambia. El mapa `members` de [`DATA-MODEL.md`](../../DATA-MODEL.md) sigue
  aplazado.
- **Reglas**: a la rama del dueño en `update` se le añaden dos condiciones:
  - `request.resource.data.ownerId in request.resource.data.memberIds`: solo se pasa la propiedad a
    un miembro.
  - `request.resource.data.memberIds.hasOnly(resource.data.memberIds)`: el dueño puede quitar
    miembros, pero no añadirlos. Se entra solo con un código, por la callable.

  Las ramas de aparcar y de salirse no cambian. **Son compatibles con la app 1.1.0** de producción:
  allí el dueño solo escribe al aparcar, y eso no toca ni `ownerId` ni `memberIds`.
- **Callable `getVehicleMembers({vehicleId})`**, en `europe-west4`:
  - Si el vehículo no existe o quien llama no es miembro, devuelve `{status: "not-member"}`. Va como
    estado y no como error, porque gitlive no deja leer el código de un `HttpsError` desde el código
    común.
  - Si es miembro, lee los `users` de `memberIds` con el Admin SDK y devuelve
    `{status: "ok", members: [{uid, displayName, isOwner}]}`.
  - `memberList`, la parte pura, pone primero al dueño y luego al resto en orden de entrada.
    `displayName` es el nombre, o si no el email, o si no `""`. Un perfil que falte, como el de una
    cuenta borrada desde la consola, sale con `""`.

## Cliente

- **El ViewModel sigue el patrón de `HomeViewModel`**: `authStateChanges` → `distinctUntilChanged` →
  `flatMapLatest` → `getVehicleListUseCase(uid)` → el vehículo con ese id.
  - **Si el vehículo desaparece después de haberse visto**, te han quitado o lo han borrado. La
    pantalla dice «Ya no tienes acceso a este vehículo» y ofrece volver. Si lo borras o te sales tú
    desde aquí, se cierra sin ese aviso.
  - **Cuando cambian `ownerId` o `memberIds`**, se vuelven a pedir los miembros, cancelando la
    petición anterior. Por eso las acciones de dueño aparecen solas al recibir la propiedad.
  - **Hay como mucho una acción pendiente de confirmar**: quitar, pasar la propiedad, borrar o salir.
    Borrar y salir reutilizan `DeleteVehicleUseCase` y `RemoveUserFromVehicleUseCase`.
- **Quitar a alguien** es `removeMember`: solo `arrayRemove` en `memberIds`, más `updatedAt`. No toca
  el `users` del otro, así que su pista de orden queda colgando, y eso es inocuo desde la spec 008.
- **La pantalla**, de arriba abajo:
  - Nombre y matrícula.
  - **Dónde está**: «Última vez…», «Aparcado por…» si hay a quién nombrar, y **Ver en el mapa**.
  - **Miembros**: «Dueño» y «Tú» como marcas. Para el dueño, un menú en cada uno de los demás con
    **Hacer dueño** y **Quitar**. Si los nombres no cargan, **Reintentar**. Debajo, **Compartir**.
  - **En rojo, al final**: **Eliminar vehículo** o **Quitar de mi lista**, con los diálogos de Home.
- **«Ver en el mapa»**: `TabShell` guarda el id del vehículo a enfocar, recorta la pila del mapa a
  su raíz y cambia a esa pestaña. `HomeScreen` mueve el carrusel al vehículo, igual que con
  `followedVehicleId`.

## Decisiones y alternativas consideradas

- **Decisión**: los nombres, con **una callable**.
  **Alternativa**: el mapa `members` en el vehículo. Sería tiempo real, pero obliga a migrar los
  vehículos existentes y a mantenerlo al crear, unirse, salir, quitar, cambiar el nombre y borrar la
  cuenta. La lectura de miembros es poco frecuente y no necesita tiempo real.
- **Decisión**: **el nombre, o el email si no hay nombre**.
  **Alternativa**: solo el nombre. Con tantas cuentas sin nombre, muchos miembros serían
  irreconocibles. Además, la tarjeta ya enseña ese email al aparcar.
- **Decisión**: quitar y transferir **desde el cliente**.
  **Alternativa**: callables. Añaden un viaje de red y un despliegue sin ganar seguridad: las reglas
  demuestran que se transfiere solo a un miembro y que nadie entra por esta vía.
- **Decisión**: el vehículo, **desde la consulta de la lista**.
  **Alternativa**: un listener sobre el documento. Daría PERMISSION_DENIED en cuanto te quitaran, y
  ese es justo el caso que hay que contar bien.
- **Decisión**: **«Ver en el mapa»**.
  **Alternativa**: un mapa pequeño en el detalle. En iOS el mapa vive en una sola instancia de Swift
  compartida, y una segunda es arriesgada.

## Riesgos

- **Los nombres no son en tiempo real**: si alguien cambia el suyo mientras miras, se ve la próxima
  vez.
- **Quien es expulsado no recibe aviso** hasta la fase 4: lo descubre porque el vehículo desaparece.
- **Una llamada a la función por cada apertura**, y otra cada vez que cambian los miembros.
- **Orden de despliegue en producción**: primero la función y las reglas, después la app que llama a
  la función.

## Resolución de las preguntas abiertas de la spec

1. **Nombres de los miembros** → callable `getVehicleMembers`.
2. **¿Email visible?** → solo si no hay nombre.
3. **¿Cliente o callable para quitar y transferir?** → cliente, con reglas.
4. **¿Borrar y salir siguen en el mapa?** → sí, y además en el detalle.
5. **¿Editar el vehículo?** → spec aparte.
6. **¿Cómo se enseña la ubicación?** → texto y «Ver en el mapa».
7. **¿Se anonimiza a quien quitan?** → no.
