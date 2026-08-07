# Spec: Caducidad real de las invitaciones

- **Estado**: Propuesto
- **Fecha**: 2026-08-07

## Qué

Que una invitación desaparezca de `invites` cuando deja de tener utilidad, en vez de quedarse
indefinidamente.

## Por qué

La [spec 005](../005-share-vehicle/spec.md) dejó una única vía de limpieza: al crear una invitación
nueva para un vehículo se borran **las anteriores sin usar de ese vehículo**. Cubre el caso normal,
pero deja tres formas de acumular:

1. **Caducada y sin usar**, de un vehículo que no se vuelve a compartir. Nadie dispara la limpieza,
   así que se queda para siempre.
2. **Usada**. El filtro las excluye a propósito (`filter(doc => !doc.data().usedBy)`), porque es lo
   que permite decir *"ya se ha usado"* en vez de *"no es válido"*. Consecuencia: **cada
   compartición exitosa deja un documento permanente**. Esta es la que crece sin límite.
3. **Huérfana**: al borrar un vehículo, `cleanupVehicleReferences` solo limpia `userVehicles`. Sus
   invitaciones se quedan apuntando a un vehículo que ya no existe.

Comprobado en datos el 2026-08-07: en `apark-at` hay 6 invitaciones, 3 de ellas muertas; en
`(default)` hay una, `QRAGPSM7`, sin usar y caducada desde el día 5.

**No es un problema de coste**, y conviene decirlo claro: cada documento son unos 200 bytes, mil
comparticiones no llegan a 200 KB, y el espacio de códigos (31⁸ ≈ 8,5 × 10¹¹) hace que las
colisiones sean irrelevantes. El diseño de la 005 acertaba al decir que el volumen es mínimo.

Lo que sí importa es **la calidad del mensaje**. Mientras el documento existe, quien introduce un
código muerto recibe *"esa invitación ha caducado"* o *"ya se ha usado"*: información accionable,
pide otra. Cuando el documento desaparece recibe *"ese código no es válido"*, que sugiere que se ha
equivocado al teclear. O sea: **borrar demasiado pronto empeora la experiencia**. Ese es el eje de
la decisión, no el almacenamiento.

## Criterios de aceptación

1. Una invitación caducada acaba desapareciendo de `invites` sin que nadie comparta ese vehículo
   otra vez.
2. Una invitación usada acaba desapareciendo también.
3. Durante un **periodo de gracia** posterior a la caducidad, el mensaje sigue siendo el preciso
   (*caducada* / *ya se ha usado*), no *no es válido*.
4. Al borrar un vehículo, sus invitaciones se borran con él, sin esperar a la caducidad.
5. Aplicado a **las dos bases de datos**, `apark-at` y `(default)`.
6. Ninguna regla de seguridad se relaja: `invites` sigue cerrada al cliente.

## Fuera de alcance

- Cambiar las 24 h de validez de una invitación.
- Que el dueño pueda revocar a mano una invitación viva desde la interfaz. Hoy se revoca
  implícitamente al generar otra; una acción explícita es otra spec.
