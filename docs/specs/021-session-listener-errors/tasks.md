# Tasks: que la app no se cierre cuando falla una escucha de Firestore

- **Spec**: [spec.md](spec.md) · **Diseño**: [design.md](design.md), aprobado en modo plan

## Presentation
- [x] `utils/ListenerFailures.kt`: `catchListenerFailure`, y sus tests
- [x] `HomeViewModel` y `HomeScreen`: recogerla dentro del `flatMapLatest`, y el aviso
- [x] `VehiclesViewModel` y `VehiclesScreen`: lo mismo; el texto según `errorKey`, y deja de cargar si falla
- [x] `AccountViewModel`: de `catch { }` a la función común
- [x] `VehicleDetailViewModel` y `EditVehicleViewModel`: a la función común
- [x] Koin: `crashReporter` en `VehiclesViewModel` y `AccountViewModel`
- [x] `vehicles_load_error` en es, en y fr

## Documentación
- [x] `AGENTS.md`: la regla de las escuchas
- [x] `CHANGELOG.md`

## Verificación
- [x] Tests en la JVM; compila Android, iOS-Kotlin, los tests en iOS y Xcode
- [x] **(3)** Simulador, con la lectura de `vehicles` denegada solo en el emulador: sin cierre, y el aviso en *Inicio* y en *Vehículos*. Las dos se quedan con la lista que tenían en caché
- [x] **(1)(4)** Simulador: borrar la cuenta con la app abierta e ir a *Cuenta*: al login, sin cierre ni aviso, y sin informe de cierre nuevo
- [x] **(2)** Simulador: una sesión que ya no vale al arrancar. El SDK no refresca un token todavía válido, y el emulador lo acepta, así que la app arranca; al ir a *Cuenta* lleva al login, sin cierre. Es el mismo camino que el (1)
- [ ] **(5)** Cuenta: un fallo con la sesión viva se registra. Lo cubren el test de `catchListenerFailure` y el código; Crashlytics no se ve desde una debug contra los emuladores
- [x] **(6)** Emulador de Android: los puntos (3) y (1). Para poder entrar contra los emuladores hizo falta permitir HTTP en debug, con un manifiesto temporal que no se commitea; el arreglo de verdad va aparte
- [ ] El aviso en el móvil — **Alberto**
