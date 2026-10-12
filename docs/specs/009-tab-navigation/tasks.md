# Tasks: pestañas que navegan de verdad

- **Spec**: [spec.md](spec.md) · **Design**: [design.md](design.md)

## Navegación
- [ ] `Destiny.kt`: sacar los destinos del wrapper y añadir `showsTabBar`
- [ ] `AddVehicle` lo pone a `false`; el resto hereda `true`
- [ ] Nuevos destinos: `TabShell`, `Vehicles`, `Account`
- [ ] `Tab.kt`: enum con icono y clave de texto
- [ ] `RootNavigation.kt`: solo autenticación y el shell; login y logout mueven **esta** pila
- [ ] `TabShell.kt`: tres `rememberNavBackStack` **como sentencias separadas** (no en bucle)
- [ ] El `NavDisplay` recibe la pila de la pestaña activa
- [ ] La pestaña activa se guarda con `rememberSaveable` (criterio 5)
- [ ] `BackHandler`: si la pestaña no es *Mapa* y está en su raíz, volver a *Mapa*
- [ ] La barra se oculta cuando el destino de arriba dice `showsTabBar = false`

## Presentation
- [ ] `AparkBottomNavigationBar`: recibe pestaña y callback, textos desde recursos
- [ ] `HomeScreen`: fuera el `bottomBar` del Scaffold
- [ ] `HomeScreen`: fuera el menú de tres puntos y su logout
- [ ] `VehiclesScreen` + ViewModel: lista con `GetVehicleListUseCase`, acceso al detalle
- [ ] `AccountScreen` + ViewModel: email, selector de tema, cerrar sesión
- [ ] Registrar los dos ViewModels nuevos en `SharedModule`

## Recursos
- [ ] Nombres de las tres pestañas, en es/en/fr
- [ ] Textos de *Vehículos* y *Cuenta*, en es/en/fr

## Documentación
- [ ] `AGENTS.md`: la navegación ya no es «tres pestañas decorativas»
- [ ] Entrada en `CHANGELOG.md`

## Verificación
- [ ] Compila Android, iOS-Kotlin y el proyecto de Xcode
- [ ] Los tests que ya hay siguen pasando; CI en verde
- [ ] **(1)** Las tres pestañas llevan a pantallas distintas, con la barra marcada
- [ ] **(2)** Detalle desde *Vehículos* → *Cuenta* → volver: **sigue en el detalle**
- [ ] **(3)** Sin barra en login ni en añadir vehículo; **con** barra en el detalle
- [ ] **(4)** Cerrar sesión desde *Cuenta*: login limpio y sin rastro de la sesión anterior
- [ ] **(5)** Girar el dispositivo: misma pestaña y misma pantalla
- [ ] **(6)** Cambiar el idioma del sistema: pestañas traducidas
- [ ] **Mirar con atención**: volver a *Mapa* desde otra pestaña. Si parpadea o tarda, izar el
      estado de cámara al `TabShell` — y **no** rehacer la navegación
