# Tasks: eliminar la cuenta

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## Servidor
- [x] `functions/src/account.ts`: `deleteAccount`; `planVehicle` e `isRecentLogin` en `accountPlan.ts`, sin imports
- [x] Exportada desde `index.ts`, en `europe-west4`
- [x] `tools/test/account.test.ts`: `planVehicle` e `isRecentLogin`

## Dominio y datos
- [x] `AccountRepository` + `FirebaseAccountRepository`: la callable y la traducción de sus errores
- [x] `AuthRepository.reauthenticateWithGoogle`
- [x] `AuthFailureReason.WRONG_ACCOUNT`, con el código nativo de Android vía `expect`/`actual`
- [x] `GetAccountDeletionImpactUseCase` y `DeleteAccountUseCase`
- [x] Koin

## Interfaz
- [x] Fila «Eliminar cuenta» en *Cuenta*
- [x] `DeleteAccountScreen` y su ViewModel: impacto, reautenticación por proveedor y confirmación
- [x] Destino `DeleteAccount`, sin barra
- [x] `HomeScreen`: «Aparcado por…» solo con nombre o email
- [x] Cadenas en `values`, `values-en` y `values-fr`

## Tests
- [x] `commonTest`: el impacto del borrado

## Documentación
- [x] `AGENTS.md`, `FIREBASE.md`, `DATA-MODEL.md`, `ROADMAP.md`
- [x] Entrada en `CHANGELOG.md`

## Verificación

Contra los emuladores. Los dos borrados del servidor se probaron llamando a la callable; el de Edu,
otra vez desde la app en el simulador.
- [x] Compila Android, iOS-Kotlin, tests JVM y nativos, Xcode; `functions` compila; tests de `tools`
- [x] **(3)(5)** Edu, miembro, borra su cuenta: sale del compartido, que queda anonimizado
- [x] **(3)(4)** Alberto, dueño, borra su cuenta: sus vehículos en solitario se borran y el compartido
      pasa a Edu
- [x] **(7)** Llamar dos veces no falla ni deja restos
- [x] **(2)(9)** Simulador de iOS: contraseña mal da un error en línea; bien, «cuenta eliminada» y
      vuelta al login
- [x] **(1)** La pantalla enseña qué se borra, qué pasa a otro y de qué se sale
- [x] **(8)** Registrarse otra vez con el mismo email empieza de cero
- [x] Google en dispositivo, *user mismatch* y Android — **Alberto**, en iPhone y Android contra `apark-dev`
- [ ] **(6)** Apple — aplazado hasta la cuenta de pago
