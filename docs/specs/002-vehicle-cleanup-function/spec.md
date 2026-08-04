# Spec: Limpieza de referencias al borrar un vehículo (Cloud Function)

- **ID**: 002-vehicle-cleanup-function
- **Estado**: Aprobada
- **Fecha**: 2026-07-25 (desbloqueada el 2026-08-03 al activarse el plan Blaze)

## Problema / Por qué

Cuando el dueño elimina un vehículo ([spec 001](../001-delete-vehicle/spec.md)), el cliente
solo puede limpiar **su propia** lista: las reglas de Firestore impiden que un usuario escriba
en el documento de otro (`match /users/{userId}` exige `request.auth.uid == userId`). Por eso
el ID del vehículo eliminado **queda colgante** en el `userVehicles` de los demás miembros.

Hoy eso **no rompe la app**: `getVehiclesForUser` reintenta y descarta los IDs ilegibles, así
que la tarjeta desaparece igualmente. Pero deja dos costes:

1. **Arranque más lento** para esos usuarios: cada ID colgante consume ~3 reintentos de 1 s
   antes de descartarse.
2. **Basura de datos** que crece con el tiempo.

## Objetivos

- Que al eliminarse un vehículo, su ID desaparezca del `userVehicles` de **todos** los miembros.
- Que la limpieza sea automática y no dependa del cliente que ejecutó el borrado.

## No-objetivos

- Cambiar el comportamiento de cara al usuario (la feature 001 ya funciona).
- Sustituir la resiliencia del cliente: seguirá siendo la red de seguridad.

## Historias de usuario

- Como miembro de un vehículo que ha sido eliminado por su dueño, quiero que mi lista quede
  limpia automáticamente, para que la app arranque rápido y sin referencias muertas.

## Criterios de aceptación

1. **Dado** un vehículo con varios miembros, **cuando** el dueño lo elimina, **entonces** el ID
   deja de estar en el `userVehicles` de **todos** los miembros (dueño y compartidos).
2. **Dado** el borrado de un vehículo, **cuando** la función se ejecuta, **entonces** funciona
   igual en la base de datos de producción `(default)` y en la de debug `apark-at`.
3. **Dado** un fallo de la función, **cuando** ocurre, **entonces** la app sigue comportándose
   correctamente (la resiliencia del cliente lo cubre).

## Preguntas abiertas (resueltas)

- **Requisito de facturación** → **plan Blaze activado**, confirmado por el propietario.
- **¿Un trigger por base de datos o dos despliegues?** → **dos exports** del mismo manejador, cada
  uno con su opción `database`; un único despliegue publica los dos.
- **¿Limitar el fan-out o paginar?** → **no**: los miembros por vehículo están muy lejos del
  límite de 500 escrituras de un batch. Queda documentado por si cambiara.
- **¿Función de mantenimiento puntual?** → **no se implementó**, pero la razón que se dio era
  incorrecta. Se comprobó que no había IDs colgantes **solo en la base de datos de debug** y se
  generalizó a las dos. La de release **sí los tenía**, y salieron a la luz al aparecer una
  tarjeta fantasma en un iPhone real (ver [spec 004](../004-stale-cache-cards/spec.md)). Se
  limpiaron **a mano** el 2026-08-04, con precondición de `updateTime` para no repetir la
  sobrescritura ciega. Conclusión correcta: la función evita los IDs colgantes **nuevos**, pero
  los antiguos requerían una limpieza puntual.

## Fuera de alcance

- Cualquier otra lógica de servidor (notificaciones, validaciones adicionales).
- Migrar lógica de cliente existente a funciones.

## Notas de implementación (preliminares)

- Trigger `onDocumentDeleted('vehicles/{vehicleId}')`; leer `before.data()` para obtener
  `ownerId` y `sharedUsers`, y hacer `arrayRemove` del ID en cada `users/{uid}`.
- Requiere crear el directorio `functions/` (Node) y añadir el bloque `functions` a
  `firebase.json`, que hoy solo contiene `firestore`.
