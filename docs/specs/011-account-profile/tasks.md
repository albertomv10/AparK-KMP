# Tasks: la cuenta, editable

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## Reglas y servidor
- [x] `firestore.rules`: validar `users/{uid}.name` solo cuando cambia (1–40)
- [x] `tools/test/users.rules.test.ts`: lo que se permite y lo que no
- [x] `functions/src/profile.ts`: `propagateUserProfile`, con transacción por vehículo
- [x] El email solo se propaga si coincide con el de Auth

## Dominio y datos
- [x] `AuthFailure` sustituye a `EmailLoginFailure`, con los motivos nuevos
- [x] `toAuthFailure()` para todas las operaciones de Auth, con el 17021 de iOS antes que el tipo
- [x] `AuthRepository`: `reauthenticate`, `requestEmailChange`, `changePassword`, `reloadUser`,
      `signInProvider`
- [x] `UserRepository`: `updateName`, `syncEmail`
- [x] Casos de uso: `UpdateUserName`, `ChangeEmail`, `ChangePassword`, `SyncUserProfile`
- [x] `UpdateVehicleLocationUseCase` firma con el email de Auth
- [x] Registro: el fallo se traduce a un motivo y `RegisterScreen` lo traduce a texto

## Sesión
- [x] `SessionViewModel` en `TabShell`: `null` lleva al login; un usuario sincroniza su perfil
- [x] `AccountScreen` deja de navegar al cerrar sesión
- [x] `AccountViewModel` recarga el usuario al abrirse y cierra sesión si está revocada
- [x] De camino: aparcar firma con el email **del token**, y `reloadUser()` renueva el token —
      `reload()` solo pone al día el perfil, y entre los dos la regla podía denegar

## Interfaz
- [x] `PasswordField` en `presentation/components`
- [x] Cuenta: nombre editable, filas de email y contraseña, o la explicación de Google/Apple
- [x] `EditNameDialog`
- [x] `ChangeEmailScreen`, con la confirmación al enviar
- [x] `ChangePasswordScreen`, con la confirmación al cambiar
- [x] Destinos `ChangeEmail` y `ChangePassword`, sin barra
- [x] Cadenas en `values`, `values-en` y `values-fr`, sin escapar apóstrofos
- [x] Koin: casos de uso y ViewModels nuevos

## Tests
- [x] `commonTest`: `SignInProvider` a partir de los proveedores y la validación del nombre

## Documentación
- [x] `AGENTS.md`: el trigger nuevo, y los gotchas de la sesión y del email al aparcar
- [x] Entrada en `CHANGELOG.md`

## Verificación
- [x] Compila Android, iOS-Kotlin, tests JVM y nativos, Xcode; `functions` compila; tests de reglas
- [x] **(1)** Cambiar el nombre llega a la tarjeta de otro miembro, y al `displayName` de Auth
- [x] **(2)(4)** Contraseña actual mal: error en línea y campos conservados; bien: cambia
- [x] **(3)** Cambiar el email avisa, y el email de acceso no cambia hasta abrir el enlace
- [x] **(8)** Sesión revocada: la app vuelve al login
- [x] **(7)** Tras entrar con el email nuevo, un miembro que no es dueño aparca el compartido
- [x] **(6)** Ningún texto técnico en ninguno de los fallos, registro incluido
- [ ] Desplegar reglas y función en `apark-dev` — **cuando Alberto dé el visto bueno**
- [ ] **(5)** Cuenta de Google o Apple — **Alberto**, en dispositivo: los emuladores no las aceptan
