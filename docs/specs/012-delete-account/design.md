# Design: eliminar la cuenta

- **Spec**: [spec.md](spec.md)
- **Estado**: Aprobado

## Enfoque

El borrado lo hace **una callable, `deleteAccount`**, en `europe-west4`. La app reautentica a la
persona y la llama. El servidor comprueba que el login es reciente, limpia todo lo que la cuenta
dejó en Firestore y, **lo último**, borra la cuenta de Auth. Cada paso es idempotente, así que un
reintento termina lo que un fallo dejó a medias.

En la app, una pantalla de *Cuenta* enseña antes de confirmar qué se va a perder, y reautentica
según el proveedor: con la contraseña, o volviendo a pasar por Google.

## Lo que dijo el código antes de decidir nada

1. **Una cuenta deja rastro donde el cliente no llega.** Su nombre y su email están copiados en
   `lastLocation.user` de vehículos ajenos. Las invitaciones que creó no se pueden tocar, porque las
   reglas niegan todo acceso a `invites`. Y los vehículos compartidos de los que es dueña afectan a
   otras personas.
2. **Un trigger de borrado de Auth no puede vivir en Europa sin trampas.** Solo existe en v1, y la
   entrada estrecha `firebase-functions/v1/auth` no admite región. Fijársela obliga a usar la función
   interna `_userWithOptions` o a importar el barril de v1. El barril carga Realtime Database, y eso
   rompe el despliegue (ver «Import narrowly» en [`AGENTS.md`](../../../AGENTS.md)). Sin región, el
   trigger acabaría en `us-central1`, fuera de la UE, procesando datos que viven en `eur3`.
3. **«Iniciar sesión con Apple» hoy no puede funcionar.** El proyecto de iOS no tiene
   *entitlements*, y ese en concreto exige la cuenta de pago de Apple Developer. En producción no
   hay ningún usuario de Apple.
4. **Reautenticar con Google se puede hacer con lo que ya hay.** `GoogleSignInButton` entrega
   `(idToken, accessToken)`. Pero si se elige **otra** cuenta de Google, Firebase responde *user
   mismatch*. En Android eso llega como `InvalidUser`, que hoy se traduce a `SESSION_EXPIRED`, y eso
   cerraría la sesión.
5. **El orden de `memberIds` sirve como orden de entrada.** Crear un vehículo lo deja como
   `[dueño]`, unirse con código hace `arrayUnion` y añade al final, y la migración de la spec 008
   escribió `[dueño, ...sharedUsers]`.

## Archivos / módulos afectados

| Archivo | Cambio |
|---------|--------|
| `functions/src/account.ts` · `accountPlan.ts` · `index.ts` | La callable `deleteAccount`; `planVehicle` e `isRecentLogin`, puras y sin imports, en su propio fichero para que los tests de `tools/` no necesiten las dependencias de `functions/` |
| `tools/test/account.test.ts` | Tests de `planVehicle` e `isRecentLogin` |
| `domain/repository/AccountRepository.kt` · `data/repository/FirebaseAccountRepository.kt` | Llamar a la callable y traducir sus errores |
| `domain/repository/AuthRepository.kt` · `data/repository/FirebaseAuthRepository.kt` | `reauthenticateWithGoogle`, y el *user mismatch* |
| `domain/auth/AuthFailure.kt` | `WRONG_ACCOUNT` |
| `data/repository/NativeAuthErrorCode.kt` (`expect`/`actual`) | El código de error nativo de Android, que no se ve desde `commonMain` |
| `domain/usecase/` | `GetAccountDeletionImpact`, `DeleteAccount` |
| `presentation/account/` | `DeleteAccountScreen` y su ViewModel, y la fila en *Cuenta* |
| `presentation/navigation/Destiny.kt` · `TabShell.kt` | El destino `DeleteAccount`, sin barra |
| `presentation/home/HomeScreen.kt` | «Aparcado por…» solo cuando hay nombre o email |
| `composeResources/values*/strings.xml` | Cadenas nuevas en los tres idiomas |

## Cambios de datos y reglas Firestore

- **Modelo**: no cambia.
- **Reglas**: no cambian. La callable usa el Admin SDK.
- **Firestore**: lo que hace la callable, en este orden:
  1. **Vehículos donde es miembro** (`memberIds array-contains uid`). Una transacción por vehículo,
     que lo vuelve a leer antes de decidir:
     - **Era el único miembro**: se borra el vehículo. El trigger `cleanupVehicleReferences` borra
       sus invitaciones y lo quita de `userVehicles`.
     - **Hay más miembros**: sale de `memberIds`. Si era el dueño, `ownerId` pasa al primero que
       queda. Si lo aparcó, `lastLocation.user = null`, conservando la ubicación y la hora. Y
       `updatedAt` se pone al día.
  2. **Vehículos que aparcó y de los que ya no es miembro**: los busca por
     `lastLocation.user.uid == uid`, y por `lastLocation.user.id`, la forma de antes de la spec 008.
     Se anonimizan, comprobando dentro de la transacción que siga siendo quien aparcó.
  3. **Invitaciones** con `createdBy == uid`: se borran.
  4. **`users/{uid}`**: se borra.
  5. **La cuenta de Auth**, con `getAuth().deleteUser(uid)`. Un `user-not-found` cuenta como hecho.
- **Login reciente**: antes de tocar nada, si `request.auth.token.auth_time` tiene más de 5
  minutos, la callable devuelve `{status: "requires-recent-login"}` y no toca nada. El servidor no se
  fía de que el cliente haya reautenticado. Va como estado y no como error, igual que hace
  `joinVehicleWithCode` con sus desenlaces esperados, porque gitlive no deja leer el código de un
  `HttpsError` desde el código común.

## Decisiones y alternativas consideradas

- **Decisión**: un vehículo compartido **pasa al miembro más antiguo**.
  **Alternativas**:
  - *Borrarlo para todos*: los demás perderían el vehículo sin haber hecho nada.
  - *Obligar a transferirlo antes*: necesita la pantalla de miembros (2.4), que no existe, y la spec
    crecería bastante.
- **Decisión**: **anonimizar** las copias de quién aparcó.
  **Alternativa**: borrar la última ubicación entera. Los demás perderían dónde está el coche, aunque
  siga ahí.
- **Decisión**: **una callable**.
  **Alternativa**: además, un trigger de borrado de Auth que limpiara también los borrados desde la
  consola. Solo puede vivir en `us-central1`, por el punto 2 de arriba.
- **Decisión**: **la cuenta de Auth se borra la última.**
  **Alternativa**: borrarla primero, para que nunca quede una cuenta sin datos. Pero si la limpieza
  fallara después, los datos se quedarían huérfanos y la persona ya no podría reintentarlo. El
  criterio 7 se reescribió para reflejarlo.
- **Decisión**: **Apple se aplaza** hasta tener la cuenta de pago.
  **Alternativa**: escribir ya la revocación del token, sin poder probarla. Sería código sin
  verificar en el único flujo que no tiene vuelta atrás.
- **Decisión**: **una pantalla, no un diálogo**. Tiene que enseñar una lista y, para cuentas con
  contraseña, un campo. Es el patrón de cambiar email y contraseña.

## Riesgos

- **Borrar un usuario desde la consola deja sus datos**, porque no hay trigger. Se documenta en
  [`FIREBASE.md`](../../FIREBASE.md).
- **El traspaso de propiedad es silencioso** hasta que existan las notificaciones (fase 4): el nuevo
  dueño se entera al abrir la app.
- **El *user mismatch* en Android** depende del código de error nativo, y no se puede probar sin una
  cuenta real de Google.
- **Publicar en iOS exige lo de Apple**: reautenticar y revocar el token, cuando se active ese inicio
  de sesión.

## Resolución de las preguntas abiertas de la spec

1. **Vehículo compartido del que es dueño** → pasa al miembro más antiguo.
2. **Su nombre y su email en tarjetas ajenas** → se anonimizan.
3. **¿Callable o trigger?** → una callable en `europe-west4`.
4. **Reautenticar con Google o Apple** → Google ahora; Apple con la cuenta de pago.
5. **¿Pantalla o diálogo?** → pantalla completa.
6. **El enlace web de Google Play** → fuera de esta spec, junto a la política de privacidad.
7. **Los ajustes del dispositivo** → se quedan.
