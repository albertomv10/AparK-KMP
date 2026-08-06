# Spec: ViewModels con ámbito de pantalla

- **Estado**: Aprobado
- **Fecha**: 2026-08-06

## Qué

Que el ViewModel de una pantalla **muera con la pantalla**: al salir de un destino y volver a
entrar, se empieza de cero, sin arrastrar el estado de la visita anterior.

## Por qué

Hoy no ocurre. `NavDisplay` trae un único decorador por defecto, el de `SaveableStateHolder`; **no**
incluye el de `ViewModelStore`. Sin él, `koinViewModel()` resuelve contra el `ViewModelStoreOwner`
raíz, así que **ningún ViewModel de la app se destruye jamás**: viven lo que vive el proceso.

Eso ya ha producido dos fallos reales, encontrados al cerrar la [spec 005](../005-share-vehicle/spec.md):

1. Los formularios de *Añadir vehículo* conservaban lo tecleado en la visita anterior.
2. Peor: el detalle reabría el diálogo con el **código de invitación del vehículo anterior** bajo
   el nombre del nuevo. Alguien habría enviado el código equivocado.

Se taparon pantalla a pantalla, limpiando cada una su propio estado al entrar. Es un parche que
hay que recordar aplicar en **cada pantalla nueva**, y que solo cubre las que ya fallaron: los
formularios de login y registro siguen conservando el correo y la contraseña escritos.

La causa es una sola y tiene arreglo de una sola línea. Merece arreglarse ahí.

## Criterios de aceptación

1. Al salir de *Añadir vehículo* y volver, ambos formularios están vacíos y la pestaña vuelve a
   *Crear* — **sin** que la pantalla se limpie a sí misma.
2. Tras compartir un vehículo, abrir el detalle de otro **no** muestra ningún diálogo.
3. Los formularios de login y registro tampoco conservan lo escrito, sin tocar esas pantallas.
4. Volver a Home desde otro destino **conserva** su estado (Home sigue en la pila; su ViewModel no
   debe recrearse).
5. Cerrar sesión y volver a entrar arranca con un Home limpio.
6. El ciclo Splash → Login → Home sigue funcionando.
7. Rotar el dispositivo **no** pierde lo que se está escribiendo.

## Fuera de alcance

- Migrar el estado de los formularios a `SavedStateHandle` (sobrevivir a que el sistema mate el
  proceso). Es otro problema y otra spec.
