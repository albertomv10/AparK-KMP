# Tasks: eliminar la cuenta

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## Servidor
- [ ] `functions/src/account.ts`: `deleteAccount`, con `planVehicle` e `isRecentLogin`
- [ ] Exportada desde `index.ts`, en `europe-west4`
- [ ] `tools/test/account.test.ts`: `planVehicle` e `isRecentLogin`

## Dominio y datos
- [ ] `AccountRepository` + `FirebaseAccountRepository`: la callable y la traducción de sus errores
- [ ] `AuthRepository.reauthenticateWithGoogle`
- [ ] `AuthFailureReason.WRONG_ACCOUNT`, con el código nativo de Android vía `expect`/`actual`
- [ ] `GetAccountDeletionImpactUseCase` y `DeleteAccountUseCase`
- [ ] Koin

## Interfaz
- [ ] Fila «Eliminar cuenta» en *Cuenta*
- [ ] `DeleteAccountScreen` y su ViewModel: impacto, reautenticación por proveedor y confirmación
- [ ] Destino `DeleteAccount`, sin barra
- [ ] `HomeScreen`: «Aparcado por…» solo con nombre o email
- [ ] Cadenas en `values`, `values-en` y `values-fr`

## Tests
- [ ] `commonTest`: el impacto del borrado

## Documentación
- [ ] `AGENTS.md`, `FIREBASE.md`, `DATA-MODEL.md`, `ROADMAP.md`
- [ ] Entrada en `CHANGELOG.md`

## Verificación
- [ ] Compila Android, iOS-Kotlin, tests JVM y nativos, Xcode; `functions` compila; tests de `tools`
- [ ] **(3)(5)** Edu, miembro, borra su cuenta: sale del compartido, que queda anonimizado
- [ ] **(3)(4)** Alberto, dueño, borra su cuenta: sus vehículos en solitario se borran y el compartido
      pasa a Edu
- [ ] **(7)** Llamar dos veces no falla ni deja restos
- [ ] **(2)(9)** Simulador de iOS: contraseña mal da un error en línea; bien, «cuenta eliminada» y
      vuelta al login
- [ ] **(1)** La pantalla enseña qué se borra, qué pasa a otro y de qué se sale
- [ ] **(8)** Registrarse otra vez con el mismo email empieza de cero
- [ ] Google en dispositivo y *user mismatch* — **Alberto**
- [ ] **(6)** Apple — aplazado hasta la cuenta de pago
