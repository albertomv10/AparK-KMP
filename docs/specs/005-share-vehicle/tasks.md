# Tasks: Compartir vehículo

- **Spec**: [spec.md](spec.md) · **Design**: [design.md](design.md)

## Backend
- [x] `functions/src/invites.ts`: `createInviteHandler` y `joinWithCodeHandler`
- [x] Código de 8 caracteres, alfabeto sin ambigüedades, entrada normalizada
- [x] Revocar las invitaciones anteriores sin usar al crear una nueva
- [x] Comprobar el uso **dentro** de la transacción (dos personas con el mismo código)
- [x] Exportar cada función por base de datos (las invocables no infieren cuál usa el cliente)
- [x] Desplegado en `europe-west4`

## Reglas
- [x] Añadir `match /invites/{code} { allow read, write: if false; }`
- [x] Ninguna regla existente relajada (criterio 8)
- [x] Desplegadas a ambas bases de datos

## Cliente — datos y dominio
- [x] Dependencia `dev.gitlive:firebase-functions`
- [x] `VehicleInvite`, `InviteRepository`, `FirebaseInviteRepository`
- [x] Región `europe-west4` fijada en el DI
- [x] `CreateVehicleInviteUseCase`, `JoinVehicleWithCodeUseCase` + Koin

## Cliente — UI
- [x] Pantalla de detalle provisional + ViewModel, con compartir **solo para el dueño**
- [x] Primer destino de navegación con parámetro; `onNavigateToDetails` cableado
- [x] Diálogo con código, copiar y compartir
- [x] `expect/actual ShareTextHandler` (Android `ACTION_SEND`, iOS `UIActivityViewController`)
- [x] Pestañas *Crear* / *Unirme* en añadir vehículo
- [x] Confirmación de copiado **en el propio botón**: un snackbar queda detrás del velo del diálogo
- [x] Strings en ES/EN/FR

## iOS
- [x] Producto SPM `FirebaseFunctions` añadido al target (sin él, el enlazado falla)

## Limpieza
- [x] Eliminar `joinVehicleByCodeOrId` (no puede funcionar con estas reglas)
- [x] Dejar de generar `inviteCode`; el campo permanece por compatibilidad

## Documentación
- [x] Entrada en `CHANGELOG.md`
- [x] Nota en `AGENTS.md` sobre el paso extra de iOS al añadir módulos de Firebase

## Verificación
- [x] Compila Android e iOS-Kotlin; enlaza y arranca en el simulador
- [x] **Criterio 1**: el dueño genera código (`PG5CWYQ9`); el documento tiene `expiresAt` a
      exactamente 24 h y `usedBy` nulo
- [x] **Criterio 2**: otra cuenta se unió con el código. Verificado en datos: la invitación quedó
      con `usedBy` y `usedAt`, el vehículo sumó al usuario en `sharedUsers`, y su `userVehicles`
      incluye el vehículo — los tres con el **mismo `updateTime`**, o sea, en una sola transacción
- [x] **Criterio 5**: código inexistente → "Ese código no es válido"
- [x] **Criterio 7**: código propio → "Ya tienes este vehículo" y la invitación **no** se consume
      (`usedBy` seguía nulo)
- [x] Los estados del servidor se traducen: al usuario ya no le llega el volcado de la excepción
- [x] **Criterio 3** (código ya usado → error): comprobado por Alberto desde la cuenta que ya se
      había unido. Concuerda con los datos: la invitación quedó marcada y la función comprueba
      `usedBy` antes que nada
- [x] **Criterio 6** (sin botón si no eres dueño): comprobado por Alberto abriendo el detalle de
      un vehículo compartido desde la cuenta que **no** lo posee. Un intento anterior mío no
      valía: la sesión del simulador era la del **dueño**, así que ver el botón era lo correcto
- [ ] **Criterio 4** (caducada): sin probar. Es el único que queda, y para probarlo hay que
      adelantar `expiresAt` a mano desde Firestore

## Corrección posterior — los formularios no se vaciaban
Al salir de *Añadir vehículo* y volver a entrar seguía el texto de la visita anterior.

**Causa**: `NavDisplay` solo trae por defecto el decorador de `SaveableStateHolder`; **no** el de
`ViewModelStore`. Sin él, `koinViewModel()` resuelve contra el `ViewModelStoreOwner` raíz, así que
**ningún ViewModel de la app se destruye nunca** al navegar hacia atrás.

El arreglo de raíz era añadir `lifecycle-viewmodel-navigation3`, que no existía para la 2.9.6 que
usaba el proyecto. Se hizo justo después, subiendo lifecycle a 2.10.0:
[spec 006](../006-scoped-viewmodels/spec.md). Los parches de aquí quedaron retirados allí.

- [x] `AddVehicleEvent.ScreenOpened` vacía el estado al entrar, con una bandera `rememberSaveable`
      para hacerlo **una vez por visita** y no perder lo escrito al rotar
- [x] Mismo fallo, más grave, en el detalle: `load` no limpiaba `invite`, así que tras compartir un
      vehículo y abrir otro se reabría el diálogo con el **código del anterior** bajo el nombre del
      nuevo. Ahora `load` parte de un estado limpio
- [ ] Los formularios de login y registro tienen el mismo comportamiento (conservan el correo y la
      contraseña escritos). Fuera del alcance de esta spec
