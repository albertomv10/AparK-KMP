# Spec: detalle del vehículo, miembros y propiedad

- **ID**: 014-vehicle-detail-members
- **Estado**: Aprobado — diseño en [design.md](design.md)
- **Fecha**: 2026-11-24

## Problema / Por qué

La pantalla de detalle de un vehículo es un **marcador de posición**: el nombre y, si eres el dueño,
un botón para compartir. Es lo único que se ve al tocar una tarjeta. Con ella pasan tres cosas:

1. **Nadie sabe con quién comparte un vehículo.** Los miembros existen en `memberIds`, pero la app
   no los enseña en ningún sitio. La única pista es «Aparcado por…», y solo del último que aparcó.
2. **El dueño no puede gestionar a los miembros.** Puede invitar, pero no quitar a nadie. Quien
   vende el coche o deja de compartirlo no tiene forma de echar a los demás, salvo borrar el
   vehículo.
3. **La app pide algo que no deja hacer.** Al borrar un vehículo, el diálogo dice «si quieres que
   el vehículo siga existiendo, transfiere antes la propiedad a otro miembro». No hay dónde.
   `transferVehicleOwnership` existe en el repositorio desde hace meses y nada lo llama.

Son los puntos 2.3 y 2.4 de la [fase 2](../../ROADMAP.md). Y hay una razón para hacerlos ahora:
**el detalle es donde vivirá la fase 3** (incidencias, ITV, mantenimiento), así que necesita una
estructura que aguante crecer.

Lo difícil no es la pantalla, son los datos:

| Qué hace falta | Dónde está hoy | ¿Lo puede leer un miembro? |
|---|---|---|
| Quiénes son los miembros | `vehicles/{id}.memberIds`, solo uids | Sí |
| Cómo se llaman | `users/{uid}.name` | **No**: las reglas solo dejan leer tu propio `users` |
| Quién es el dueño | `vehicles/{id}.ownerId` | Sí |
| Quién aparcó y cuándo | `vehicles/{id}.lastLocation` | Sí |

Y uno de seguridad: hoy **el dueño puede escribir cualquier cosa** en su vehículo, `ownerId`
incluido. `transferVehicleOwnership` funcionaría, pero nada impide pasar la propiedad a alguien que
no es miembro, que dejaría el vehículo con un dueño que no puede ni verlo.

## Objetivos

- Que el detalle enseñe **lo que importa del vehículo**: nombre, matrícula, dónde está aparcado,
  quién lo aparcó y cuándo, y **sus miembros con su nombre**, marcando quién es el dueño.
- Que el **dueño** pueda **quitar** a un miembro y **pasarle la propiedad** a otro.
- Que un **miembro** pueda **salirse** desde el detalle, no solo desde el modo edición del mapa.
- Que las reglas **demuestren** que solo se pasa la propiedad a un miembro y que solo el dueño quita
  a otros, con tests que prueben también lo que se deniega.
- Que la pantalla **reaccione** si algo cambia mientras se mira: te quitan, te pasan la propiedad,
  o el vehículo se borra.
- Una estructura que la fase 3 pueda ampliar con secciones, sin rehacerla.

## No-objetivos

- **Roles** más allá de dueño y miembro, como administradores o miembros de solo lectura.
- **Avisar** a quien quitan o a quien recibe la propiedad. Sin notificaciones (fase 4), se entera
  al abrir la app.
- **Datos ricos** del vehículo: marca, modelo, tipo, año (2.1 y 2.5).
- **Invitar de otra forma** que con el código de un solo uso que ya existe.
- Un **historial** de quién aparcó y cuándo.

## Historias de usuario

- Como **miembro**, quiero ver con quién comparto el vehículo y quién es el dueño.
- Como **dueño**, quiero quitar a alguien que ya no debería usar el vehículo.
- Como **dueño**, quiero pasar la propiedad a otro miembro para que el vehículo siga existiendo si
  me voy o borro mi cuenta.
- Como **miembro**, quiero salirme de un vehículo desde su ficha.
- Como **cualquiera**, quiero ver en la ficha dónde está aparcado, desde cuándo y quién lo dejó.

## Criterios de aceptación

1. **Dado** un vehículo del que soy miembro, **cuando** abro su detalle, **entonces** veo su nombre,
   su matrícula si la tiene, dónde está aparcado, quién lo aparcó y cuándo, o «aún no aparcado».
   Veo también la lista de miembros, cada uno con su nombre, y marcados el dueño y yo.
2. **Dado** que soy el dueño, **cuando** miro la lista, **entonces** cada miembro que no soy yo
   tiene dos acciones: **quitar** y **hacer dueño**. Y sigo viendo **compartir**.
3. **Dado** que soy miembro y no dueño, **cuando** abro el detalle, **entonces** no veo ninguna
   acción sobre los demás, y sí **salir del vehículo**.
4. **Dado** que soy el dueño, **cuando** quito a un miembro y lo confirmo, **entonces** desaparece de
   la lista y deja de ver el vehículo: en su mapa ya no está.
5. **Dado** que soy el dueño, **cuando** paso la propiedad a otro miembro y lo confirmo, **entonces**
   esa persona es la nueva dueña y yo sigo como miembro. Antes de confirmar se me explica qué
   pierdo: compartir, quitar y borrar. Después, mi pantalla pasa a la de un miembro sin salir de
   ella.
6. **Dado** cualquier cliente, **cuando** intenta pasar la propiedad a alguien que no es miembro, o
   un miembro intenta quitar a otro o pasar la propiedad, **entonces** Firestore lo deniega. Hay
   tests de reglas que lo prueban, y también los casos permitidos.
7. **Dado** que estoy mirando el detalle, **cuando** me quitan del vehículo o se borra, **entonces**
   la pantalla lo dice y vuelve atrás. No se cuelga ni repite errores de permisos.
8. **Dado** que estoy mirando el detalle, **cuando** me pasan la propiedad, **entonces** aparecen las
   acciones de dueño sin tener que salir y volver.
9. **Dado** que un miembro cambia su nombre, entra con un código o borra su cuenta, **cuando** otro
   miembro abre el detalle, **entonces** la lista lo refleja.
10. **Dado** cualquier idioma de los tres, **cuando** se usa la pantalla, **entonces** todo está
    traducido, y las acciones tienen descripción para los lectores de pantalla.

## Preguntas abiertas — resueltas

Todas se cerraron en el diseño. El razonamiento completo está en [design.md](design.md).

1. **¿De dónde salen los nombres?** → **Una callable, `getVehicleMembers`.** El mapa `members` sigue
   aplazado: no hay migración ni nada que mantener sincronizado, a cambio de que los nombres no sean
   en tiempo real.
2. **¿Ven los demás tu email?** → **Solo si no tienes nombre**, como hace ya «Aparcado por». Registrarse
   solo pide email y contraseña, así que hay muchas cuentas sin nombre. Lo resuelve el servidor, que
   devuelve una sola etiqueta y no los dos datos.
3. **¿Quitar y pasar la propiedad desde el cliente o con callables?** → **Desde el cliente**, con
   reglas que lo demuestran, como «salirse» en la spec 008.
4. **¿Borrar y salirse siguen en el modo edición del mapa?** → **Sí**, y además en el detalle.
5. **¿Entra editar el vehículo (2.2)?** → **No: va en una spec aparte.**
6. **¿Mapa pequeño o «Ver en el mapa»?** → **Texto y «Ver en el mapa»**, que lleva a la pestaña del
   mapa con ese vehículo seleccionado.
7. **¿Se anonimiza a quien quitan si fue el último en aparcar?** → **No**: es lo que pasó, y esa
   persona sigue existiendo.

## Fuera de alcance

- Editar el nombre y la matrícula (2.2): spec aparte.
- Notificar al quitado y al nuevo dueño: fase 4.
- Marca, modelo y demás datos del vehículo: 2.1 y 2.5.
- Incidencias, ITV y mantenimiento: fase 3, aunque la pantalla debe dejarles sitio.
- Invitar por email o por nombre de usuario.
