# Spec: eliminar la cuenta

- **ID**: 012-delete-account
- **Estado**: Borrador
- **Fecha**: 2026-11-23

## Problema / Por qué

Hoy no hay forma de borrar la cuenta desde la app. Es el punto 1.4 de la
[fase 1](../../ROADMAP.md), y no es opcional: **App Store y Google Play rechazan** cualquier app que
deje crear una cuenta y no deje borrarla desde dentro.

La [spec 011](../011-account-profile/spec.md) fue antes por este motivo: Firebase exige un login
reciente para borrar la cuenta, y la reautenticación ya está construida y probada en operaciones que
sí tienen vuelta atrás. Esta es la que no la tiene.

Lo difícil no es la pantalla. Una cuenta de AparK deja rastro en varios sitios, y casi ninguno lo
puede borrar el cliente:

| Dónde | Qué | ¿Lo puede borrar el cliente? |
|---|---|---|
| Auth | La cuenta | Sí, tras reautenticarse |
| `users/{uid}` | Perfil y orden de vehículos | Sí |
| `vehicles` de los que es dueño | El vehículo entero, y con él el acceso de los demás miembros | Sí, pero solo los suyos |
| `vehicles` de otros | Su uid en `memberIds` | Sí: es la vía de «salirse» |
| `lastLocation.user` de cualquier vehículo que aparcó | **Copia de su nombre y su email** | **No** en los que no es dueño |
| `invites` | Las invitaciones que creó | **No**: las reglas niegan todo acceso |
| El dispositivo | Ajustes locales (tema) | Sí |

## Objetivos

- Borrar la cuenta desde *Cuenta*, con **confirmación explícita** y sabiendo antes qué se va a perder.
- Que no quede **ningún dato personal** de la persona: ni su perfil, ni su nombre o su email en las
  tarjetas de otros.
- Que los **demás miembros** de un vehículo compartido no se queden sin él por sorpresa.
- Que un fallo a mitad **no deje la cuenta a medias**.
- Cumplir lo que piden las dos tiendas para aprobar la app.

## No-objetivos

- **Periodo de gracia** o «deshacer» durante unos días. El borrado es inmediato.
- **Exportar los datos** antes de borrar.
- Herramientas para borrar cuentas desde fuera de la app, más allá de lo que pidan las tiendas (ver
  preguntas abiertas).

## Historias de usuario

- Como **usuario**, quiero borrar mi cuenta y mis datos desde la app, sin tener que escribir a nadie.
- Como **dueño de un vehículo compartido**, quiero saber qué les pasa a los demás antes de borrar mi
  cuenta.
- Como **miembro de un vehículo de otra persona**, no quiero perder ese vehículo porque otro miembro
  haya borrado su cuenta.

## Criterios de aceptación

1. **Dado** un usuario con sesión iniciada, **cuando** elige eliminar la cuenta, **entonces** ve
   antes de confirmar qué vehículos se borrarán y de cuáles saldrá.
2. **Dado** que confirma, **cuando** no ha demostrado hace poco que es él, **entonces** se le pide
   reautenticarse: la contraseña si entró con email, o volver a pasar por Google o Apple si entró
   con ellos. Sin eso no se borra nada.
3. **Dado** que se completa el borrado, **entonces** desaparecen su cuenta de Auth, su documento
   `users`, los vehículos de los que era dueño, salvo lo que se decida en la pregunta 1, y las
   invitaciones que creó. Además sale de los vehículos de otros, y la app vuelve al login.
4. **Dado** un vehículo **compartido** del que era dueño, **cuando** borra la cuenta, **entonces**
   los demás miembros ven el resultado que se acuerde en el diseño (pregunta 1) sin hacer nada.
5. **Dado** cualquier vehículo que haya aparcado, **cuando** borra la cuenta, **entonces** ningún
   otro miembro sigue viendo su nombre ni su email en la tarjeta.
6. **Dado** que entró con Apple, **cuando** borra la cuenta, **entonces** se revoca su acceso con
   Apple, como exige la App Store.
7. **Dado** un fallo a mitad del borrado, **cuando** se vuelve a intentar, **entonces** termina el
   trabajo sin duplicar nada. En ningún caso queda una cuenta con la que se pueda entrar sin sus
   datos.
8. **Dado** que se borró la cuenta, **cuando** alguien se registra después con el mismo email,
   **entonces** empieza de cero, sin heredar nada.
9. **Dado** cualquier fallo, **cuando** se muestra, **entonces** el mensaje es legible y no aparece
   ningún texto técnico.

## Preguntas abiertas

1. **¿Qué pasa con un vehículo compartido del que es dueño?**
   - **Borrarlo para todos**: es lo más simple, pero los demás pierden el vehículo sin haber hecho
     nada.
   - **Pasar la propiedad automáticamente** a otro miembro. Habría que decidir a cuál. El orden de
     `memberIds` refleja más o menos quién entró antes, porque unirse añade al final, pero ese campo
     no se pensó para eso.
   - **Obligar a transferirla antes** de poder borrar la cuenta. Necesita la pantalla de miembros y
     transferencia (punto 2.4 del roadmap), que no existe. El diálogo de borrar vehículo ya le dice a
     la gente que la transfiera, y no hay dónde hacerlo.
2. **Las copias de su nombre y su email en `lastLocation.user`**: ¿se anonimizan, dejando la
   ubicación con un «cuenta eliminada», o se borra la última ubicación entera? Ninguna de las dos
   la puede hacer el cliente en vehículos ajenos.
3. **¿Callable o trigger de borrado de Auth?**
   - Una **callable** hace todo el borrado cuando la app la llama: es lo que dice
     [`DATA-MODEL.md`](../../DATA-MODEL.md).
   - Un **trigger** que salte al borrarse el usuario de Auth haría la misma limpieza **también
     cuando se borra desde la consola**. Hace unos días se borraron 11 usuarios de producción desde
     la consola, y sus datos se habrían limpiado solos.
4. **Reautenticar a quien entró con Google o Apple**: hay que volver a lanzar su inicio de sesión
   desde la pantalla de borrado. Con Apple, además, ese inicio de sesión es el que da el código para
   revocar el token (criterio 6).
5. **¿Pantalla o diálogo?** La confirmación tiene que enseñar una lista de consecuencias y, para
   cuentas con contraseña, un campo. Las pantallas de cambiar email y contraseña ya siguen ese
   patrón.
6. **El enlace web que pide Google Play**: además del borrado dentro de la app, Play pide un enlace
   desde el que solicitar el borrado sin tener la app instalada. ¿Una página en Firebase Hosting, un
   formulario, un email?
7. **Los ajustes del dispositivo** (el tema): ¿se borran con la cuenta, o se quedan, como
   preferencia del móvil y no de la persona?

## Fuera de alcance

- La política de privacidad y los formularios de privacidad de las tiendas. También bloquean la
  publicación, pero no son código.
- La pantalla de miembros y transferencia de propiedad (2.4), salvo que la pregunta 1 la meta aquí.
- Borrar la cuenta de otro miembro, o expulsarlo de un vehículo.
