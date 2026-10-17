# Design: pestañas que navegan de verdad

- **Spec**: [spec.md](spec.md)
- **Estado**: Aprobado

## Enfoque

La navegación se parte en **dos capas**. Fuera, una pila con el flujo de autenticación y un único
destino más: el shell de pestañas. Dentro, el shell lleva **una pila de navegación por pestaña** y
le pasa al `NavDisplay` la de la pestaña activa.

Separarlas resuelve media spec **por construcción**: la barra pertenece al shell, y las pantallas de
autenticación están fuera de él, así que no pueden mostrarla aunque se quiera. También limpia el
`backStack.clear()` + `add()` de hoy — iniciar y cerrar sesión mueven la pila de fuera y no tocan las
de las pestañas.

Lo que hace esto viable es que en Navigation 3 **la pila es tuya**: `NavBackStack` es una
`MutableList` sobre un `SnapshotStateList` (`NavBackStack.kt:49`). Tres pestañas son tres listas, sin
necesidad de soporte especial; la propia librería lo contempla, con un comentario que habla de
«*if backStacks were swapped*» (`DecoratedNavEntries.kt:201`).

## Archivos / módulos afectados

| Archivo | Cambio |
|---------|--------|
| `presentation/navigation/Destiny.kt` | *(nuevo)* los destinos salen del wrapper y ganan `showsTabBar` |
| `presentation/navigation/RootNavigation.kt` | renombra `BasicNavigationWrapper`: solo autenticación y el shell |
| `presentation/navigation/TabShell.kt` | *(nuevo)* tres pilas, la barra y el `NavDisplay` |
| `presentation/navigation/Tab.kt` | *(nuevo)* el enum, con icono y clave de texto |
| `presentation/components/AparkBottomNavigationBar.kt` | recibe pestaña y callback; textos desde recursos |
| `presentation/home/HomeScreen.kt` | fuera el `bottomBar` del Scaffold y fuera el menú de tres puntos |
| `presentation/vehicles/VehiclesScreen.kt` | *(nuevo)* lista mínima con acceso al detalle |
| `presentation/account/AccountScreen.kt` | *(nuevo)* email, selector de tema y cerrar sesión |
| `composeResources/values*/strings.xml` | textos nuevos en es/en/fr |

Se reutiliza lo que ya existe: `GetVehicleListUseCase`, `SignOutUseCase` y
`SettingsRepository.setThemeMode`, que es lo que por fin da interfaz a lo cableado en el punto 0.4
del roadmap.

## Cambios de datos y reglas Firestore

**Ninguno.** Es la primera spec que toca solo presentación.

## Decisiones y alternativas consideradas

- **Decisión**: una pila por pestaña, intercambiando cuál recibe el `NavDisplay`.
  **Alternativa**: un `NavDisplay` por pestaña, todos compuestos a la vez. Descartada: conservaría
  también los ViewModels, pero mantiene tres pantallas vivas —incluido un mapa— y complica el atrás.
- **Decisión**: aceptar que al cambiar de pestaña se limpien los ViewModels de la que dejas.
  **Alternativa**: `removeViewModelStoreOnPop = { false }`. Descartada porque **reintroduce el bug
  de la [spec 006](../006-scoped-viewmodels/spec.md)**: dejaría de limpiarlos también en los *pops*
  de verdad, y cada pantalla volvería a encontrarse el estado de su visita anterior. La lambda no
  recibe la clave, así que no hay forma de distinguir un caso del otro.
- **Decisión**: cada destino declara si quiere barra, con `Destiny.showsTabBar`.
  **Alternativa**: que el shell mantenga una lista de destinos sin barra. Descartada: es una lista
  aparte que se olvida de actualizar al añadir una pantalla.
- **Decisión**: atrás en una pestaña que no es *Mapa* vuelve a *Mapa*.
  **Alternativa**: cerrar la app. Descartada por ser lo contrario de lo que espera quien usa
  Android. En iOS no aplica: no hay atrás de sistema.

## Riesgos

- **Recrear el mapa al volver a su pestaña.** Es la consecuencia aceptada de intercambiar pilas, y la
  única pantalla cara de reconstruir. *Mitigación si se nota*: izar el estado de cámara al
  `TabShell`, por encima del `NavDisplay`. Es un arreglo localizado — **no** hay que rehacer la
  navegación.
- **`rememberNavBackStack` no acepta clave** y se apoya en la posición en la composición
  (`RememberNavBackStack.kt:60`). Llamarla en un bucle haría colisionar las tres al restaurar.
  *Mitigación*: tres llamadas como sentencias separadas, con un comentario que diga por qué.
- **Navigation 3 está en alpha.** Nada de esto usa API privada, pero el comportamiento del *pop*
  podría cambiar entre versiones. *Mitigación*: el criterio de aceptación 2 lo cubre en dispositivo.

## Resolución de las preguntas abiertas de la spec

1. **¿Una pila por pestaña?** → **Sí**, tres `NavBackStack` y se intercambia cuál se muestra. El
   *pop* se detecta comparando contra la lista mostrada, así que al cambiar de pestaña las entradas
   de la anterior parecen popeadas y sus ViewModels se limpian. El criterio 2 se cumple igual
   —vuelves al detalle— y el estado perdido se reconstruye solo, porque todo deriva de flujos de
   Firestore que se resuscriben desde la caché local.
2. **¿Dónde vive la barra y cómo sabe ocultarse?** → En el `TabShell`. La autenticación queda fuera
   por construcción; dentro, lo declara cada destino con `showsTabBar`. `AddVehicle` la oculta;
   `VehicleDetail` no, porque quitarla al entrar en un detalle da sensación de salirse de la app.
3. **¿Atrás en una pestaña que no es la primera?** → Vuelve a *Mapa*; en *Mapa* no se intercepta.
