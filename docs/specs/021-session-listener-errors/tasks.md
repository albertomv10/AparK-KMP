# Tasks: que la app no se cierre cuando falla una escucha de Firestore

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## Presentation
- [ ] `utils/ListenerFailures.kt`: `catchListenerFailure`, y sus tests
- [ ] `HomeViewModel` y `HomeScreen`: recogerla dentro del `flatMapLatest`, y el aviso
- [ ] `VehiclesViewModel` y `VehiclesScreen`: lo mismo; el texto según `errorKey`
- [ ] `AccountViewModel`: de `catch { }` a la función común
- [ ] `VehicleDetailViewModel` y `EditVehicleViewModel`: a la función común
- [ ] Koin: `crashReporter` en `VehiclesViewModel` y `AccountViewModel`
- [ ] `vehicles_load_error` en es, en y fr

## Documentación
- [ ] `AGENTS.md`: la regla de las escuchas
- [ ] `CHANGELOG.md`

## Verificación
- [ ] Tests en la JVM; compila Android, iOS-Kotlin, los tests en iOS y Xcode
- [ ] **(3)** Simulador, con la lectura de `vehicles` denegada solo en el emulador: sin cierre, y el aviso en *Inicio* y en *Vehículos*
- [ ] **(1)(4)** Simulador: borrar la cuenta con la app abierta e ir a *Cuenta*: al login, sin cierre ni aviso
- [ ] **(2)** Simulador: una sesión que ya no vale al arrancar
- [ ] **(5)** Cuenta: un fallo con la sesión viva se registra
- [ ] **(6)** Emulador de Android: los puntos (3) y (1)
- [ ] El aviso en el móvil — **Alberto**
