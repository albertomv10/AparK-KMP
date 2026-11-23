# Spec: eliminar la cuenta

- **ID**: 012-delete-account
- **Estado**: Aprobado — diseño en [design.md](design.md)
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
   Apple, como exige la App Store. *(Aplazado: hoy nadie puede entrar con Apple, porque falta el
   entitlement, que exige la cuenta de pago de Apple Developer, y no hay ningún usuario de Apple. Se
   cumplirá al activar ese inicio de sesión. Es requisito para publicar en iOS. Ver
   [design.md](design.md).)*
7. **Dado** un fallo a mitad del borrado, **cuando** se vuelve a intentar, **entonces** termina el
   trabajo sin duplicar nada, y nunca quedan datos suyos sin una cuenta desde la que reclamarlos.
   *(Antes decía «ninguna cuenta con la que entrar sin sus datos». Al borrar la cuenta de Auth la
   última, un fallo intermedio deja justo eso hasta que se reintenta. El orden contrario sería peor:
   datos huérfanos que la persona ya no puede pedir que se borren.)*
8. **Dado** que se borró la cuenta, **cuando** alguien se registra después con el mismo email,
   **entonces** empieza de cero, sin heredar nada.
9. **Dado** cualquier fallo, **cuando** se muestra, **entonces** el mensaje es legible y no aparece
   ningún texto técnico.

## Preguntas abiertas — resueltas

Todas se cerraron en el diseño. El razonamiento completo está en [design.md](design.md).

1. **Vehículo compartido del que es dueño** → **pasa al miembro más antiguo**: el primero de
   `memberIds` que queda tras quitarle a él.
2. **Su nombre y su email en tarjetas ajenas** → **se anonimizan**: se borra quién aparcó y se
   conservan la ubicación y la hora.
3. **¿Callable o trigger?** → **Callable `deleteAccount` en `europe-west4`.** Un trigger de borrado
   de Auth solo podría vivir en `us-central1`, fuera de la UE. Borrar desde la consola deja los datos,
   y queda documentado.
4. **Reautenticar con Google o Apple** → **Google, ahora; Apple, cuando haya cuenta de pago.**
5. **¿Pantalla o diálogo?** → **Pantalla completa**, con el patrón de cambiar email y contraseña.
6. **El enlace web de Google Play** → **fuera de esta spec**: va con la política de privacidad.
7. **Los ajustes del dispositivo** → **se quedan**: el tema es del móvil, no de la persona.

## Fuera de alcance

- La política de privacidad y los formularios de privacidad de las tiendas. También bloquean la
  publicación, pero no son código.
- La pantalla de miembros y transferencia de propiedad (2.4), salvo que la pregunta 1 la meta aquí.
- Borrar la cuenta de otro miembro, o expulsarlo de un vehículo.
