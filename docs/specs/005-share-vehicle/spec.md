# Spec: Compartir vehículo

- **ID**: 005-share-vehicle
- **Estado**: En revisión
- **Fecha**: 2026-08-04

## Problema / Por qué

Compartir un vehículo es **la idea central del producto**: que tu pareja o tu familia vean dónde
has dejado el coche. Todo el modelo está construido para ello —`sharedUsers`, las reglas que
distinguen dueño de miembro, e incluso un `inviteCode` de 6 caracteres que se genera en **cada**
vehículo— y sin embargo **no hay forma de invitar a nadie desde la app**: ese código nunca se le
muestra al usuario.

Los vehículos compartidos que existen hoy en producción se crearon a mano en la consola de
Firestore. La funcionalidad está entera por dentro y es inalcanzable por fuera.

### Hallazgo crítico: el código heredado no puede funcionar

El proyecto arrastra un `joinVehicleByCodeOrId` de la versión anterior (solo Android) que **hoy
fallaría por dos motivos independientes**, ambos de reglas:

1. **La búsqueda se deniega**: consultar `where inviteCode == código` exige leer vehículos de los
   que aún no eres miembro, y `allow read` solo permite dueño o `sharedUsers`. Firestore rechaza
   toda consulta que no pueda demostrar que devuelve documentos legibles.
2. **La escritura se deniega**: añadirte a `sharedUsers` es una actualización sobre un vehículo
   donde no eres ni dueño ni miembro, y ninguna rama de `allow update` lo contempla.

En la app anterior funcionaba porque las reglas eran permisivas. Las actuales son estrictas, y
eso es correcto: **relajar la lectura para permitir la búsqueda por código dejaría enumerar
códigos por fuerza bruta y colarse en vehículos ajenos**.

Por tanto, unirse a un vehículo **requiere backend** — que el proyecto ya tiene desde la
[spec 002](../002-vehicle-cleanup-function/spec.md).

## Objetivos

- Que el **dueño** pueda generar una invitación y enviarla por los medios que ya usa (WhatsApp,
  etc.).
- Que otra persona pueda **unirse** con ese código y ver el vehículo en su pantalla de inicio.
- Que una invitación **caduque** y no sirva indefinidamente.
- Conseguirlo **sin debilitar ninguna regla** de Firestore.

## No-objetivos

- Un sistema de invitaciones por email con bandeja de pendientes.
- Que el dueño pueda expulsar a un miembro (feature aparte).
- La pantalla de detalle completa: aquí se crea una **provisional** solo con compartir.

## Decisiones ya tomadas

| Decisión | Elección |
|---|---|
| Ciclo de vida | Invitación con **caducidad de 24 h** y **un solo uso** |
| Ejecución del "unirse" | **Cloud Function** invocable (única vía sin relajar reglas) |
| Envío | **Hoja de compartir nativa** *y* **diálogo con el código copiable** |
| Entrada para unirse | **Pestañas** en la pantalla de añadir vehículo: *Crear* / *Unirme con código* |
| Botón de compartir | Pantalla de **detalle provisional**, visible **solo al dueño** |

Sobre el doble envío: la hoja de compartir resuelve el **envío**, pero a quien lo recibe le llega
el código dentro de un texto y no puede copiarlo de un toque. Por eso se mantiene también el
diálogo copiable, y el mensaje compartido debe poner **el código en su propia línea**.

## Historias de usuario

- Como **dueño**, quiero generar un código y mandárselo a mi pareja por WhatsApp, para que vea
  dónde está aparcado el coche.
- Como **invitado**, quiero introducir ese código en la app y que el vehículo aparezca en mi
  inicio.
- Como **dueño**, quiero que ese código **deje de servir** pronto, para que reenviar el mensaje
  no dé acceso indefinido a mi coche.

## Criterios de aceptación

1. **Dado** que soy el dueño de un vehículo, **cuando** pulso compartir, **entonces** obtengo un
   código de invitación y puedo tanto **copiarlo** como **enviarlo** con la hoja de compartir.
2. **Dado** un código válido, **cuando** otro usuario lo introduce en la pestaña *Unirme*,
   **entonces** el vehículo aparece en su inicio y él pasa a estar en `sharedUsers`.
3. **Dado** un código **ya usado**, **cuando** alguien intenta usarlo de nuevo, **entonces** se
   rechaza con un mensaje claro y no se une a nadie.
4. **Dado** un código con **más de 24 h**, **cuando** se intenta usar, **entonces** se rechaza
   por caducado.
5. **Dado** un código **inexistente o mal escrito**, **cuando** se intenta usar, **entonces** se
   muestra un error claro, sin revelar si el código existe para otro vehículo.
6. **Dado** que **no soy el dueño** de un vehículo, **cuando** abro su detalle, **entonces** no
   veo la opción de compartir.
7. **Dado** que ya soy miembro de un vehículo, **cuando** uso un código suyo, **entonces** se me
   informa de que ya lo tengo y no se duplica nada.
8. **Dado** el sistema completo, **cuando** se revisan las reglas, **entonces** **ninguna** se ha
   relajado: sigue sin poderse leer un vehículo del que no eres miembro.

## Preguntas abiertas

(A resolver en `design.md`.)

1. **Formato del código**: se teclea a mano, así que conviene un alfabeto **sin caracteres
   ambiguos** (0/O, 1/I/L) y una longitud que resista fuerza bruta. ¿6, 8 caracteres?
2. **Modelo de datos**: ¿colección `invites/{código}` usando el propio código como ID del
   documento (búsqueda directa, sin consulta)? ¿Qué campos: `vehicleId`, `createdBy`,
   `expiresAt`, `usedBy`?
3. **Quién genera la invitación**: ¿la crea el cliente (permitido por reglas si es el dueño) o
   también la genera una función? ¿Se reutiliza una invitación vigente o se crea una nueva cada
   vez que se pulsa compartir?
4. **Limpieza de invitaciones caducadas**: ¿política TTL de Firestore, borrado por la propia
   función al detectarlas, o se dejan acumular?
5. **Qué se le devuelve al que se une**: ¿el nombre del vehículo, para poder decir "te has unido
   a *Tiguan*"?
6. **El `inviteCode` heredado** del documento de vehículo queda sin uso: ¿se elimina del modelo,
   o se deja por compatibilidad?
7. **Fuerza bruta**: la función es el único punto de entrada. ¿Basta con la caducidad y el uso
   único, o conviene alguna limitación de intentos?

## Fuera de alcance

- **Códigos QR** y **deep links**: descartados *por ahora*, no por malos. El QR encaja muy bien
  para compartir en persona y es candidato natural a una segunda fase. Los deep links exigen
  dominio propio, Universal/App Links y web de respaldo, porque **Firebase Dynamic Links se apagó
  en agosto de 2025**: demasiada infraestructura para el alcance actual.
- Pantalla de detalle completa (editar nombre, matrícula, ver miembros).
- Revocar manualmente una invitación ya emitida.
- Que el dueño expulse a un miembro.
