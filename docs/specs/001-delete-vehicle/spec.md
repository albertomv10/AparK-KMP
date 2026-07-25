# Spec: Eliminar vehículo

- **ID**: 001-delete-vehicle
- **Estado**: Aprobada
- **Fecha**: 2026-07-25

## Problema / Por qué

Hoy no existe ninguna forma de eliminar un vehículo desde la app. La lista de un
usuario solo crece, y cuando se borran documentos manualmente (o un vehículo deja de
ser relevante) quedan **IDs colgantes** en el `userVehicles` del usuario, que degradan
el arranque (listeners inútiles) y, sin la reciente resiliencia del stream, podían
crashear la app. Los usuarios necesitan poder quitar vehículos que ya no usan.

## Objetivos

- Que el **dueño** pueda **eliminar** un vehículo por completo.
- Que un **usuario con el que se ha compartido** pueda **quitárselo de su lista** sin
  borrarlo para los demás.
- Que la acción sea **segura** (confirmación) y su efecto **inmediato** en la Home.
- No dejar datos inconsistentes en quien ejecuta la acción.

## No-objetivos

- Deshacer un borrado (undo) ni papelera/soft-delete visible al usuario.
- Rediseñar o completar la pantalla de detalle del vehículo.
- Gestionar permisos avanzados más allá de dueño vs compartido.

## Distinción clave de producto

Hay **dos acciones distintas** que la UI debe diferenciar claramente:

| Actor | Acción | Efecto |
|-------|--------|--------|
| **Dueño** (`ownerId`) | Eliminar vehículo | Borra el documento; el vehículo desaparece para **todos** los miembros. |
| **Usuario compartido** (`sharedUsers`) | Quitármelo | Solo se elimina **su** pertenencia; el vehículo sigue existiendo para el dueño y demás. |

La segunda acción ya tiene soporte en el repositorio (`removeUserFromVehicle`); la
primera no existe todavía.

## Historias de usuario

- Como **dueño**, quiero eliminar un vehículo que ya no tengo, para que desaparezca de
  mi lista y de la de quienes lo compartían conmigo.
- Como **usuario compartido**, quiero quitarme un vehículo que alguien compartió conmigo,
  para dejar de verlo, sin afectar al dueño.
- Como cualquier usuario, quiero **confirmar** antes de eliminar, para no borrar por error.

## Criterios de aceptación

1. **Dado** que soy el dueño de un vehículo, **cuando** pulso eliminar y confirmo,
   **entonces** el vehículo desaparece de mi Home al instante y su documento deja de
   existir en Firestore.
2. **Dado** que un vehículo está compartido conmigo (no soy el dueño), **cuando** pulso
   "quitármelo" y confirmo, **entonces** desaparece de mi Home al instante, pero el
   documento **sigue existiendo** y el dueño lo sigue viendo.
3. **Dado** cualquier flujo de eliminación, **cuando** se me presenta la acción,
   **entonces** aparece un **diálogo de confirmación** antes de ejecutarla.
6. **Dado** que soy el **dueño** de un vehículo, **cuando** se muestra el diálogo de
   confirmación, **entonces** el texto advierte explícitamente de que la acción
   **elimina el vehículo por completo y para todos los miembros**, y de que si desea
   que el vehículo siga existiendo debe **transferir la propiedad** a otro miembro
   (funcionalidad futura; ver *Fuera de alcance*).
7. **Dado** que soy un **usuario compartido**, **cuando** se muestra el diálogo de
   confirmación, **entonces** el texto deja claro que **solo se quita de mi lista** y
   que el vehículo seguirá existiendo para el resto.
4. **Dado** que soy el dueño y elimino el vehículo, **cuando** se completa,
   **entonces** el ID del vehículo ya no está en **mi** `userVehicles`.
5. **Dado** un fallo de red/permiso durante la eliminación, **cuando** ocurre,
   **entonces** se muestra un mensaje de error y la lista no queda en un estado
   inconsistente en mi dispositivo.

## Preguntas abiertas

(A resolver en `design.md`.)

1. **Limpieza de otros miembros**: al eliminar un vehículo compartido, las reglas
   actuales impiden que el dueño escriba en el `userVehicles` de **otros** usuarios
   (solo puede escribir su propio doc). ¿Cómo se limpian esas referencias?
   Opciones a valorar: **limpieza perezosa** aprovechando que el stream ya es resiliente
   a IDs colgantes (`getVehiclesForUser` los descarta) / **Cloud Function** con permisos
   admin que haga el fan-out / **soft-delete** (marcar el doc y filtrar en cliente).
2. **Ubicación del control en la UI**: ¿el borrado va en un menú contextual sobre la
   tarjeta de la Home, o requiere primero una pantalla/hoja de detalle del vehículo?
3. **Reutilización**: confirmar el uso de `removeUserFromVehicle` para el flujo
   "quitármelo" y de la regla `allow delete` (dueño) ya existente para el borrado real.
4. **Componente de confirmación**: ¿`AlertDialog` de Material 3 reutilizable para toda
   la app, o específico de esta feature?

## Fuera de alcance

- Undo / restaurar un vehículo eliminado.
- Historial o soft-delete permanente visible al usuario.
- **Transferir la propiedad** como alternativa al borrado: el repositorio ya expone
  `transferVehicleOwnership`, pero **no hay UI** y no se implementa aquí. Esta feature
  solo la **menciona** en el diálogo del dueño (criterio 6) como camino recomendado
  para conservar el vehículo; la funcionalidad llegará en una spec posterior.
