# Spec: Tarjetas fantasma por caché de Firestore

- **ID**: 004-stale-cache-cards
- **Estado**: Borrador (aparcada — valorar más adelante)
- **Fecha**: 2026-08-04

## Problema / Por qué

Firestore mantiene una **caché local en disco**. Al abrir la app, los listeners devuelven primero
lo último que había en esa caché y **después** la versión del servidor. Si un vehículo dejó de
existir mientras la app estaba cerrada, durante unos segundos se pinta una **tarjeta fantasma**
con su nombre, y luego desaparece sola.

No es teórico: ocurrió en la versión de release, en un iPhone real. Aparecía un vehículo llamado
"OpenCode" durante unos segundos aunque **ese documento no existía en la base de datos** — el
nombre venía del disco del teléfono. Fue desconcertante incluso sabiendo cómo funciona el
sistema por dentro; para un usuario cualquiera lo es más.

Dos factores lo agravaban:

1. **IDs colgantes** en `userVehicles` (ya limpiados a mano, y la
   [spec 002](../002-vehicle-cleanup-function/spec.md) evita que se generen nuevos).
2. La **resiliencia del cliente**: escuchar un documento inexistente devuelve
   `PERMISSION_DENIED` —la regla desreferencia un `resource` nulo—, así que `getVehiclesForUser`
   reintenta **3 veces con 1 s** antes de descartarlo. Eso alarga el fantasma a ~3 segundos.

Con la spec 002 desplegada el caso es mucho más raro, pero **no imposible**: basta que otro
miembro borre un vehículo compartido mientras tu app está cerrada y que la función tarde en
propagar, o que el documento desaparezca por cualquier otra vía.

## Objetivos

- Que no se muestren vehículos que ya no existen.
- Sin perder el arranque rápido: la caché es lo que permite ver tus coches al instante y offline.

## No-objetivos

- Desactivar la persistencia offline de Firestore (perderíamos el uso sin conexión).
- Volver a tocar los datos: los IDs colgantes ya están limpios.

## Historias de usuario

- Como usuario, no quiero ver durante unos segundos un vehículo que ya no tengo, porque me hace
  dudar de si la app funciona bien.

## Criterios de aceptación (borrador)

1. **Dado** un ID en mi lista cuyo vehículo ya no existe, **cuando** abro la app, **entonces**
   no se pinta ninguna tarjeta para él en ningún momento.
2. **Dado** que abro la app sin conexión, **cuando** tengo vehículos válidos en caché,
   **entonces** siguen apareciendo (no se sacrifica el modo offline).

## Preguntas abiertas

1. **El compromiso de fondo.** Esperar confirmación del servidor antes de pintar elimina el
   fantasma pero retrasa el primer pintado y **rompe el arranque offline**, que es justo la
   ventaja de la caché. ¿Merece la pena? Alternativas a valorar:
   - **Esperar solo la primera emisión** de cada vehículo si hay conexión, y usar caché si no.
   - **Distinguir el origen del dato**: `snapshots` expone metadatos (`isFromCache`) que
     permitirían pintar la caché pero marcar/ocultar lo no confirmado.
   - **No hacer nada** y aceptarlo como comportamiento normal de una app offline-first,
     apoyándose en que la spec 002 ya hace el caso muy improbable.
2. **Reducir la ventana**: ¿bajar los 3 reintentos de 1 s cuando el error es `PERMISSION_DENIED`
   sobre un documento que probablemente no existe? Distinguirlo de un fallo transitorio real es
   el problema — y ese `retryWhen` existe porque sin él la app **crasheaba**.
3. **¿Es siquiera un bug?** Mostrar caché y corregir después es el comportamiento esperado de
   Firestore. Quizá la respuesta correcta sea de producto (un indicador de carga) y no técnica.

## Fuera de alcance

- Cambiar el modelo de datos o las reglas.
- Limpieza de datos históricos (ya hecha).

## Notas

Antes de implementar nada conviene **medir cuánto ocurre de verdad** ahora que la spec 002 está
desplegada. Puede que el arreglo correcto sea ninguno.
