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
- [ ] **Criterio 3** (código ya usado → error): no confirmado en la interfaz. La propiedad de un
      solo uso **sí** está respaldada por los datos (la invitación quedó marcada y la función
      comprueba `usedBy` antes que nada), pero falta ver el mensaje
- [ ] **Criterio 4** (caducada): sin probar
- [ ] **Criterio 6** (sin botón si no eres dueño): **sin verificar**. Un intento resultó inválido:
      la sesión del simulador era la del **dueño**, así que ver el botón era lo correcto. Requiere
      abrir un vehículo compartido desde la cuenta que no lo posee

### Cómo cerrar lo que falta
Con la sesión de `albertomedinavaquero@gmail.com` (que ahora tiene Tesla compartido):
abrir el detalle de **Tesla** → no debe aparecer "Compartir vehículo" (criterio 6), y meter
`PG5CWYQ9` en *Unirme* → debe decir que ya se ha usado (criterio 3).
