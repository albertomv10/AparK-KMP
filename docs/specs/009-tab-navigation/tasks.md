# Tasks: pestañas que navegan de verdad

- **Spec**: [spec.md](spec.md) · **Design**: [design.md](design.md)

## Navegación
- [x] `Destiny.kt`: sacar los destinos del wrapper y añadir `showsTabBar`
- [x] `AddVehicle` lo pone a `false`; el resto hereda `true`
- [x] Nuevos destinos: `Tabs` (el shell), `Vehicles`, `Account`
- [x] `Tab.kt`: enum con icono y clave de texto
- [x] `RootNavigation.kt`: solo autenticación y el shell; login y logout mueven **esta** pila
- [x] `TabShell.kt`: tres `rememberNavBackStack` **como sentencias separadas** (no en bucle)
- [x] El `NavDisplay` recibe la pila de la pestaña activa
- [x] La pestaña activa se guarda con `rememberSaveable` (criterio 5)
- [x] `BackHandler`: si la pestaña no es *Mapa* y está en su raíz, volver a *Mapa*
- [x] La barra se oculta cuando el destino de arriba dice `showsTabBar = false`

## Presentation
- [x] `AparkBottomNavigationBar`: recibe pestaña y callback, textos desde recursos
- [x] `HomeScreen`: fuera el `bottomBar` del Scaffold
- [x] `HomeScreen`: fuera el menú de tres puntos y su logout
- [x] `VehiclesScreen` + ViewModel: lista con `GetVehicleListUseCase`, acceso al detalle
- [x] `AccountScreen` + ViewModel: email, selector de tema, cerrar sesión
- [x] Registrar los dos ViewModels nuevos en `SharedModule`

## Recursos
- [x] Nombres de las tres pestañas, en es/en/fr
- [x] Textos de *Vehículos* y *Cuenta*, en es/en/fr

## Documentación
- [x] `AGENTS.md`: la navegación ya no es «tres pestañas decorativas»
- [ ] Entrada en `CHANGELOG.md`

## Verificación
- [x] Compila Android, iOS-Kotlin y el proyecto de Xcode
- [x] Los tests que ya hay siguen pasando (6/6 en JVM) y además **compilan para iOS**, que no lo
      hacían: dos nombres con coma que Kotlin/Native rechaza. La CI solo corría los de JVM, así que
      ahora compila también los nativos
- [ ] CI en verde
- [ ] **(1)** Las tres pestañas llevan a pantallas distintas, con la barra marcada
- [ ] **(2)** Detalle desde *Vehículos* → *Cuenta* → volver: **sigue en el detalle**
- [ ] **(3)** Sin barra en login ni en añadir vehículo; **con** barra en el detalle
      *(la mitad de login comprobada en el simulador: arranca, Splash → Login y sin barra. El
      resto vive detrás del inicio de sesión, así que toca dispositivo.)*
- [ ] **(4)** Cerrar sesión desde *Cuenta*: login limpio y sin rastro de la sesión anterior
- [ ] **(5)** Girar el dispositivo: misma pestaña y misma pantalla
- [ ] **(6)** Cambiar el idioma del sistema: pestañas traducidas
- [ ] **Mirar con atención**: volver a *Mapa* desde otra pestaña. Si parpadea o tarda, izar el
      estado de cámara al `TabShell` — y **no** rehacer la navegación
