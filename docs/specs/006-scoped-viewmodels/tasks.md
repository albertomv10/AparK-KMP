# Tasks: ViewModels con ámbito de pantalla

- **Spec**: [spec.md](spec.md) · **Design**: [design.md](design.md)

## Dependencias
- [x] `androidx-lifecycle` 2.9.6 → **2.10.0** (la primera con el artefacto, y apunta al mismo
      `compose.runtime` 1.9.3 que ya usa el proyecto)
- [x] Alias y dependencia `lifecycle-viewmodel-navigation3` en `commonMain`
- [x] Comprobado que el artefacto publica los targets de iOS que usa el proyecto

## Navegación
- [x] Los **dos** decoradores en `NavDisplay`, en orden: `SaveableStateHolder` primero
- [x] Comprobado que ninguna pantalla comparte ViewModel con otra: cada destino tiene el suyo, así
      que acotarlos no rompe ningún flujo

## Retirada de los apaños
- [x] Fuera `AddVehicleEvent.ScreenOpened` y la bandera `formCleared` de la pantalla
- [x] En el detalle se conserva el estado limpio en `load`, pero ya no como parche: cargar otro
      vehículo debe empezar de cero por su propio mérito

## Documentación
- [x] `CHANGELOG.md`
- [x] Sustituido el aviso de `AGENTS.md` que decía que ningún ViewModel se destruye
- [x] Nota en las tareas de la [spec 005](../005-share-vehicle/tasks.md), que dejó esto anotado
      como pendiente

## Verificación
- [x] Compilan Android e iOS-Kotlin; enlaza y arranca en el simulador
- [x] **Criterio 1**: escrito `SinDecorador` en *Añadir vehículo*, salida al mapa y vuelta → campo
      vacío y pestaña en *Crear*. Ahora **sin** que la pantalla se limpie sola: lo hace el decorador
- [x] **Criterio 2**: abiertos dos detalles seguidos → cada uno carga su vehículo y ninguno abre
      diálogo. Además es ya imposible por construcción: cada entrada estrena ViewModel
- [x] **Criterio 4**: el carrusel de Home mantuvo su posición al volver de *Añadir vehículo* —
      Home sigue en la pila y su ViewModel no se recrea
- [x] **Criterio 6**: Splash → Home al arrancar, sin incidencias
- [x] **Criterios 3 y 5** (login y registro no conservan lo escrito; cerrar sesión y volver a
      entrar da un Home limpio): comprobados por Alberto **compilando esta rama** en su iPhone.
      Era el camino que más cambia, porque `backStack.clear()` ahora destruye el `HomeViewModel`
      en lugar de dejarlo vivo entre cuentas
- [x] **Criterio 7** (rotar no pierde lo escrito): comprobado por Alberto en Android. Es lo
      esperado: el decorador consulta `isChangingConfigurations`, así que una rotación no cuenta
      como salir de la pantalla y el ViewModel no se limpia

**Los siete criterios quedan verificados.**

## Relación con la fuga de datos entre cuentas
El proyecto ya arregló ese fallo en julio (`f229131` y `74d61b9`) sustituyendo un
`init { loadVehicles() }` de una sola pasada por `observeAuthState()` con `flatMapLatest`.

Con el decorador puesto, aquel síntoma concreto **habría desaparecido igualmente**: el
`HomeViewModel` moría al cerrar sesión y el nuevo volvía a ejecutar su `init`. Pero aquel arreglo
**no era un parche de esta carencia** y sigue siendo necesario, porque cubre lo que el decorador no
puede ver:

- Cambios de sesión que **no pasan por la navegación**: token caducado, sesión revocada, cuenta
  borrada desde otro dispositivo. No hay entrada nueva que recrear.
- El `return` silencioso del código antiguo si el uid no estaba listo, sin reintento.
- `currentUserId`, que hoy alimenta ese flujo y es lo que distingue al dueño del miembro
  compartido en el diálogo de eliminar.

El decorador es una garantía **de navegación**; `observeAuthState` una garantía **de datos**. Hacer
que la corrección de los datos dependa de la forma de la pila devolvería la fuga en silencio el día
que alguien cambie el cierre de sesión.
