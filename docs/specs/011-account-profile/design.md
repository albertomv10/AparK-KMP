# Design: la cuenta, editable

- **Spec**: [spec.md](spec.md)
- **Estado**: Aprobado

## Enfoque

El nombre tiene **una fuente de la verdad**, `users/{uid}.name`, y es lo único que escribe el
cliente. Las copias —`lastLocation.user` en los vehículos que esa persona aparcó, y el
`displayName` de Auth— las mantiene un **trigger de servidor**, igual que el modelo objetivo de
[`DATA-MODEL.md`](../../DATA-MODEL.md) quiere que se mantengan todas las copias desnormalizadas.

El email y la contraseña se cambian con una **reautenticación que forma parte del formulario**: la
contraseña actual se pide siempre, como un campo más. Y la app aprende dos cosas que hoy no sabe:
salir de las pestañas cuando la sesión muere por su cuenta, y firmar al aparcar con el email del
token en lugar del email del documento.

## Lo que dijo el código antes de decidir nada

1. **Nadie escribe hoy un nombre propio.** El registro con email no lo pide, y
   `EmailVerificationViewModel` crea `users/{uid}` con `name = ""`. Solo Google y Apple traen uno,
   que se copia una vez en el primer login. El `displayName` de Auth no lo escribe la app nunca. Por
   eso las tarjetas de quien se registró con email enseñan su email a los demás: es el *fallback*
   de `HomeScreen`.
2. **Quien no es dueño y cambia su email no puede volver a aparcar.** `UpdateVehicleLocationUseCase`
   firmaba `ParkedBy.email` con el email **del documento `users`**, que nada actualiza, mientras la
   regla lo compara con `request.auth.token.email`. El dueño no lo nota, porque su rama de la regla
   le deja cambiar lo que quiera; un miembro compartido recibe PERMISSION_DENIED para siempre.
3. **Cambiar el email cierra la sesión.** Firebase revoca los refresh tokens ante «un cambio
   importante de la cuenta», y el cambio de email o de contraseña cuenta como tal (*Manage user
   sessions*). Al abrir el enlace, el dispositivo pierde la sesión en la siguiente renovación, como
   mucho una hora después. `RootNavigation` **no reaccionaba** a un cierre de sesión que no saliera
   del botón, así que la app se habría quedado en las pestañas con todo devolviendo error.
4. **gitlive 2.4.0 trae todo lo necesario**: `reauthenticate`, `verifyBeforeUpdateEmail`,
   `updatePassword`, `updateProfile`, `providerData`, `EmailAuthProvider.credential`. En iOS tipa
   bien el 17014 (*requires recent login*), el 17026 (contraseña débil) y el 17007 (email en uso).
   El 17009 sigue sin tipar, y el 17021 (*token expirado*) **llega como `InvalidCredentials`**, igual
   que una contraseña incorrecta.

## Archivos / módulos afectados

| Archivo | Cambio |
|---------|--------|
| `functions/src/profile.ts` · `index.ts` | Trigger nuevo `propagateUserProfile` |
| `firestore.rules` | Validación del nombre en `users/{uid}` |
| `tools/test/users.rules.test.ts` | Tests nuevos de las reglas de `users` |
| `domain/auth/AuthFailure.kt` | Sustituye a `EmailLoginFailure`, con más motivos |
| `domain/auth/SignInProvider.kt` | `PASSWORD` · `GOOGLE` · `APPLE`, sacado de los proveedores |
| `domain/repository/AuthRepository.kt` · `UserRepository.kt` | Reautenticar, cambiar email y contraseña, recargar, proveedor · nombre y email |
| `data/repository/FirebaseAuthRepository.kt` | `toAuthFailure()` para todas las operaciones, `register` incluido |
| `data/repository/FirestoreUserRepository.kt` | `updateName`, `syncEmail` |
| `domain/usecase/` | `UpdateUserName`, `ChangeEmail`, `ChangePassword`, `SyncUserProfile` |
| `domain/usecase/UpdateVehicleLocationUseCase.kt` | Firma con el email de Auth |
| `presentation/navigation/SessionViewModel.kt` · `TabShell.kt` | La sesión tiene una sola salida |
| `presentation/account/` | La pantalla de Cuenta, el diálogo del nombre y las dos pantallas nuevas |
| `presentation/components/PasswordField.kt` | Campo de contraseña reutilizable, también para 1.4 |
| `presentation/navigation/Destiny.kt` | `ChangeEmail`, `ChangePassword`, sin barra |
| `presentation/auth/register/` | El error de registro deja de volcarse crudo |
| `composeResources/values*/strings.xml` | Cadenas nuevas en los tres idiomas |

## Cambios de datos y reglas Firestore

- **Modelo**: no cambia. `User.name` y `Vehicle.ParkedBy` ya tienen la forma que hace falta.
- **Firestore**: el cliente escribe `users/{uid}.name` y `users/{uid}.email` (este último solo para
  sincronizarlo con Auth). El trigger escribe `vehicles/*/lastLocation.user.{name,email}` en una
  transacción por vehículo.
- **Reglas**: `users/{uid}` valida el nombre **solo cuando cambia**: cadena de 1 a 40 caracteres.
  Si se validara en toda escritura, un documento antiguo con un `displayName` largo de Google no
  podría ni reordenar. Las reglas de vehículos no cambian. Se despliegan primero a `apark-dev`.

### El trigger `propagateUserProfile`

`onDocumentUpdated("users/{uid}")` en `europe-west4`, el mismo patrón que
`cleanupVehicleReferences`.

- Si ni `name` ni `email` cambian, sale. Así filtra las escrituras de reordenar, que son la mayoría.
- **El email solo se propaga si coincide con el de Auth.** El documento `users` lo escribe entero su
  dueño, así que el valor que trae no se da por bueno sin comprobarlo.
- Si cambió el nombre, lo copia al `displayName` de Auth. Es lo que saludan los correos de Firebase
  (`%DISPLAY_NAME%`), y es de donde sale el nombre de quien entra con Google.
- Busca los vehículos con `memberIds array-contains uid`. En cada uno abre una **transacción** que
  lo vuelve a leer y solo escribe si `lastLocation.user` sigue siendo esa persona: si otro miembro
  aparca en ese instante, sin la comprobación se le pondría a él el nombre de otro. Reconoce también
  el `id` de la forma anterior a la spec 008.
- No toca `timestamp` ni `updatedAt`, porque la tarjeta pinta «hace X min» con el primero.
- Es idempotente y no entra en bucle: escribe en `vehicles` y en Auth, nunca en `users`.

## Decisiones y alternativas consideradas

- **Decisión**: la cascada la hace **un trigger de servidor**.
  **Alternativas**:
  - *Hacerla desde el cliente*: las reglas ya lo permiten, porque la rama de aparcar deja reescribir
    `lastLocation` a quien firma con su email. No hacía falta backend, pero la cascada se quedaba a
    medias si la app moría o no había red. Y dejaba la lógica de las copias en la app justo cuando
    el modelo objetivo quiere sacarla de ahí; el mapa `members` de `DATA-MODEL.md` irá a este mismo
    trigger.
  - *Sin cascada, como foto fija*: `ParkedBy` querría decir «quién aparcó, con el nombre que tenía
    entonces». Era lo más barato, pero obligaba a rebajar el criterio 1.
- **Decisión**: la regla de aparcar **no se toca**, y el cliente firma con el email de Auth.
  **Alternativa**: comparar por `uid` en la regla. `uid` no cambia nunca y sería lo más limpio,
  pero las apps de antes de la spec 008 firman con `id`, y todavía puede haber alguna instalada; es
  la misma espera que tiene pendiente la limpieza de `sharedUsers`. Cuando esa limpieza llegue,
  vale la pena hacerlo.
- **Decisión**: la contraseña actual se pide **siempre**, como un campo del formulario.
  **Alternativa**: pedirla solo cuando Firebase devuelva *requires recent login*, en un diálogo de
  reautenticación con reintento. Eran dos caminos que probar para ahorrarse el campo en los pocos
  minutos que siguen a un login, y el emulador ni siquiera hace cumplir esa ventana. Pedirla siempre
  es lo que hacen Google y Apple.
- **Decisión**: el nombre se edita en un diálogo; el email y la contraseña, cada uno en su pantalla
  completa sin barra, como *Añadir vehículo*.
  **Alternativas**: todo en diálogos, donde tres campos con el teclado abierto no caben bien en un
  iPhone pequeño; o una única pantalla «Editar perfil», que mezcla lo que pide contraseña con lo que
  no la pide.
- **Decisión**: el email pendiente se anuncia al enviarlo, y nada más.
  **Alternativa**: guardarlo en el dispositivo y enseñarlo en Cuenta. Podía mentir, porque el enlace
  caduca y se puede abrir desde otro dispositivo, y Firebase no da forma de comprobarlo.
- **Decisión**: la sesión tiene **una sola salida**. Un `SessionViewModel` en `TabShell` observa
  `authStateChanges` y navega al login cuando llega `null`, venga de donde venga: del botón, de una
  revocación o de una cuenta deshabilitada. El botón de cerrar sesión deja de navegar por su cuenta.
  **Alternativa**: mantener la navegación del botón y añadir el observador como red. Habría dos
  caminos para lo mismo, y el login se crearía dos veces.
- **Decisión**: `EmailLoginFailure` pasa a llamarse `AuthFailure`. Ya no describe solo el login,
  sino también el registro, la reautenticación y los cambios de email y contraseña, y el traductor
  de excepciones del repositorio vale para todas esas operaciones.

## Riesgos

- **Protección contra la enumeración de emails**: si el proyecto la tiene activa,
  `verifyBeforeUpdateEmail` hacia un email que ya existe puede responder con éxito sin enviar nada,
  en vez de *email en uso*. El mensaje de éxito no promete más de lo que se sabe.
- **Apps antiguas sin actualizar**: siguen firmando con `users.email` y no sincronizan al entrar.
  Quien cambie el email desde una app nueva y aparque desde una vieja seguirá fallando como hoy. Es
  la misma limitación con la que ya vive la migración de la spec 008.
- **El trigger salta en cada escritura de `users`**: las de reordenar salen en la primera
  comprobación, y el coste es una invocación.
- **La cascada puede perderse** si la función falla, porque no hay reintentos, igual que en
  `cleanupVehicleReferences`. Se corrige sola en el siguiente aparcamiento.
- **El emulador de Auth puede no revocar la sesión** al aplicar el enlace. Para probar la reacción
  de la app, se revoca a mano con `revokeRefreshTokens`; el comportamiento real se confirma en
  `apark-dev`.

## Resolución de las preguntas abiertas de la spec

- **¿Dónde vive el nombre, y qué hay que propagar?** → En `users/{uid}.name`. Lo demás lo propaga
  el trigger.
- **¿Y el email?** → Hoy, quien no es dueño y cambia su email no puede aparcar (hallazgo 2). El
  cliente firma con el email de Auth y sincroniza `users.email` al entrar, y el trigger propaga el
  cambio a las tarjetas.
- **La reautenticación, ¿pantalla o diálogo?** → Un campo del formulario que se pide siempre. Lo
  reutilizable es `AuthRepository.reauthenticate()` junto con `PasswordField`.
- **¿Qué se le enseña a quien entró con Google o Apple?** → Una fila que explica de dónde viene su
  identidad, en lugar de las opciones de email y contraseña.
- **¿Nombre para todos?** → Sí.
- **El email pendiente** → Se anuncia al enviarlo, y nada más.
