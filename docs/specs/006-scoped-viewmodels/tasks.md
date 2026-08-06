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
- [ ] **Criterio 3** (login y registro no conservan lo escrito) y **criterio 5** (cerrar sesión y
      volver a entrar da un Home limpio): **sin verificar**. Requieren cerrar sesión, y volver a
      entrar exige teclear una contraseña, cosa que el asistente no hace. Es además el camino que
      más cambia con esta spec (`backStack.clear()` ahora destruye el `HomeViewModel`), así que
      conviene ejecutarlo
- [ ] **Criterio 7** (rotar no pierde lo escrito): sin probar. Solo aplica a Android; el decorador
      consulta `isChangingConfigurations` antes de limpiar

### Cómo cerrar lo que falta
Cerrar sesión desde el menú de Home. En la pantalla de login, comprobar que **no** aparece el
correo de la sesión anterior. Volver a entrar y comprobar que Home carga limpio y con los
vehículos de la cuenta.
