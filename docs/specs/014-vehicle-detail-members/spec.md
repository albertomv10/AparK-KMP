# Spec: detalle del vehículo, miembros y propiedad

- **ID**: 014-vehicle-detail-members
- **Estado**: Borrador
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

## Preguntas abiertas

1. **¿De dónde salen los nombres de los miembros?** Es la decisión de fondo.
   - **(a) El mapa `members`** que propone [`DATA-MODEL.md`](../../DATA-MODEL.md): `{uid: {name}}`
     dentro del vehículo, mantenido solo por el servidor. Se pinta con el documento que ya se ha
     leído, y en tiempo real. A cambio, hay que migrar los vehículos existentes, y que unirse con
     código, `propagateUserProfile` y `deleteAccount` lo mantengan al día. La
     [spec 008](../008-vehicle-membership-model/design.md) lo aplazó porque «no hay pantalla que
     muestre miembros»: esta es esa pantalla.
   - **(b) Una callable `getVehicleMembers`** que lee los `users` con el Admin SDK. No hay migración
     ni nada que sincronizar, y los nombres salen siempre frescos. Cuesta una llamada cada vez que se
     abre el detalle, y no es en tiempo real. DATA-MODEL marca la lectura de miembros (R7) como de
     frecuencia baja y sin tiempo real.
   - **(c) Abrir las reglas de `users`** a quien comparte un vehículo: descartable, porque ese
     documento lleva `userVehicles`, los ids de todos tus vehículos.
2. **¿Ven los demás miembros tu email?** La tarjeta ya enseña el email de quien aparcó cuando no
   tiene nombre. ¿La lista también, o solo el nombre, y «Miembro sin nombre» si no lo tiene?
3. **¿Quitar y pasar la propiedad desde el cliente, con reglas más estrictas, o con callables?**
   DATA-MODEL dice que los datos de autorización los escribe el servidor. La spec 008 dejó
   «salirse» en el cliente porque la regla demuestra que solo te quitas a ti, y aquí se podría hacer
   lo mismo.
4. **¿Borrar el vehículo y salirse se quedan también en el modo edición del mapa**, o pasan a vivir
   solo en el detalle?
5. **¿Entra editar el vehículo** (2.2: nombre y matrícula)? `UpdateVehicleUseCase` existe y nadie
   lo llama, y el detalle es el sitio natural para ello.
6. **¿La ubicación del detalle lleva un mapa pequeño**, o texto con «Ver en el mapa», que lleve a la
   pestaña del mapa centrada en ese vehículo?
7. **Si quitan a alguien que fue el último en aparcar**, su nombre sigue en la tarjeta. ¿Se deja,
   porque es un dato de lo que pasó, o se anonimiza como hace `deleteAccount`?

## Fuera de alcance

- Notificar al quitado y al nuevo dueño: fase 4.
- Marca, modelo y demás datos del vehículo: 2.1 y 2.5.
- Incidencias, ITV y mantenimiento: fase 3, aunque la pantalla debe dejarles sitio.
- Invitar por email o por nombre de usuario.
