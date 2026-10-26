# Tasks: la cuenta, editable

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## Reglas y servidor
- [ ] `firestore.rules`: validar `users/{uid}.name` solo cuando cambia (1–40)
- [ ] `tools/test/users.rules.test.ts`: lo que se permite y lo que no
- [ ] `functions/src/profile.ts`: `propagateUserProfile`, con transacción por vehículo
- [ ] El email solo se propaga si coincide con el de Auth

## Dominio y datos
- [ ] `AuthFailure` sustituye a `EmailLoginFailure`, con los motivos nuevos
- [ ] `toAuthFailure()` para todas las operaciones de Auth, con el 17021 de iOS antes que el tipo
- [ ] `AuthRepository`: `reauthenticate`, `requestEmailChange`, `changePassword`, `reloadUser`,
      `signInProvider`
- [ ] `UserRepository`: `updateName`, `syncEmail`
- [ ] Casos de uso: `UpdateUserName`, `ChangeEmail`, `ChangePassword`, `SyncUserProfile`
- [ ] `UpdateVehicleLocationUseCase` firma con el email de Auth
- [ ] Registro: el fallo se traduce a un motivo y `RegisterScreen` lo traduce a texto

## Sesión
- [ ] `SessionViewModel` en `TabShell`: `null` lleva al login; un usuario sincroniza su perfil
- [ ] `AccountScreen` deja de navegar al cerrar sesión
- [ ] `AccountViewModel` recarga el usuario al abrirse y cierra sesión si está revocada

## Interfaz
- [ ] `PasswordField` en `presentation/components`
- [ ] Cuenta: nombre editable, filas de email y contraseña, o la explicación de Google/Apple
- [ ] `EditNameDialog`
- [ ] `ChangeEmailScreen`, con la confirmación al enviar
- [ ] `ChangePasswordScreen`, con la confirmación al cambiar
- [ ] Destinos `ChangeEmail` y `ChangePassword`, sin barra
- [ ] Cadenas en `values`, `values-en` y `values-fr`, sin escapar apóstrofos
- [ ] Koin: casos de uso y ViewModels nuevos

## Tests
- [ ] `commonTest`: `SignInProvider` a partir de los proveedores y la validación del nombre

## Documentación
- [ ] `AGENTS.md`: el trigger nuevo, y los gotchas de la sesión y del email al aparcar
- [ ] Entrada en `CHANGELOG.md`

## Verificación
- [ ] Compila Android, iOS-Kotlin, tests JVM y nativos, Xcode; `functions` compila; tests de reglas
- [ ] **(1)** Cambiar el nombre llega a la tarjeta de otro miembro, y al `displayName` de Auth
- [ ] **(2)(4)** Contraseña actual mal: error en línea y campos conservados; bien: cambia
- [ ] **(3)** Cambiar el email avisa, y el email de acceso no cambia hasta abrir el enlace
- [ ] **(8)** Sesión revocada: la app vuelve al login
- [ ] **(7)** Tras entrar con el email nuevo, un miembro que no es dueño aparca el compartido
- [ ] **(6)** Ningún texto técnico en ninguno de los fallos, registro incluido
- [ ] **(5)** Cuenta de Google o Apple — **Alberto**, en dispositivo: los emuladores no las aceptan
