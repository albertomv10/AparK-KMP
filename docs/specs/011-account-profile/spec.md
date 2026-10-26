# Spec: la cuenta, editable

- **ID**: 011-account-profile
- **Estado**: Aprobado — diseño en [design.md](design.md)
- **Fecha**: 2026-10-26

## Problema / Por qué

La pantalla de *Cuenta* que estrenó la [spec 009](../009-tab-navigation/spec.md) enseña un email y
deja elegir tema. No deja **cambiar nada**: ni el nombre, ni el email, ni la contraseña. Quien se
equivoca al registrarse no tiene arreglo dentro de la app.

Es el punto 1.2 de la [fase 1](../../ROADMAP.md). Va antes que **borrar la cuenta** (1.4, el
bloqueante de tienda) por un motivo concreto y no por tamaño: Firebase exige **reautenticación
reciente** para cambiar email, cambiar contraseña *y* borrar la cuenta. Construirla aquí, en las
operaciones reversibles, es más barato que estrenarla en la única que no tiene vuelta atrás.

## Objetivos

- Cambiar el **nombre** que ven los demás miembros de un vehículo compartido.
- Cambiar el **email** de acceso.
- Cambiar la **contraseña**.
- Una **reautenticación** reutilizable, porque 1.4 la va a necesitar igual.
- Que un fallo se explique: ningún volcado técnico en pantalla, como ya se arregló en el login.

## No-objetivos

- **Foto de perfil** (1.3): estrena Firebase Storage y es una pieza entera aparte.
- **Borrar la cuenta** (1.4): necesita una callable con cascada y decidir qué pasa con los
  vehículos que la persona posee y comparte. Spec propia.
- **Idioma** (1.5): los dos sistemas ya ofrecen idioma por app en sus ajustes; si se hace dentro,
  hay que justificar por qué.
- Cambiar email o contraseña de quien entró con **Google o Apple**: su identidad la gestiona el
  proveedor. Lo que sí hay que decidir es qué se le enseña (ver preguntas abiertas).

## Historias de usuario

- Como **miembro de un vehículo compartido**, quiero corregir mi nombre, para que los demás vean
  quién aparcó y no un nombre a medias o vacío.
- Como **usuario**, quiero cambiar mi contraseña desde la app sin tener que fingir que la olvidé.
- Como **usuario que se equivocó al escribir su email**, quiero corregirlo sin perder la cuenta ni
  los vehículos.

## Criterios de aceptación

1. **Dado** un usuario con sesión iniciada, **cuando** cambia su nombre, **entonces** el nombre
   nuevo aparece en su cuenta y en las tarjetas de los vehículos que haya aparcado.
2. **Dado** un usuario que pide cambiar email o contraseña, **cuando** lo confirma, **entonces** se
   le pide **siempre** su contraseña actual antes de seguir, y **no** se le echa de la app.
   *(Antes decía «cuando lleva demasiado tiempo con la sesión abierta». Firebase no deja saber de
   antemano si el login es lo bastante reciente, y la ventana es de minutos: pedirla siempre evita
   un segundo paso por sorpresa. Ver [design.md](design.md).)*
3. **Dado** que ha pedido cambiar su email, **cuando** confirma, **entonces** recibe un correo de
   verificación **en la dirección nueva**, y su email de acceso **no cambia hasta que lo abre**.
4. **Dado** que introduce mal su contraseña actual al reautenticarse, **cuando** se le avisa,
   **entonces** el mensaje es legible y puede reintentar sin empezar de cero.
5. **Dado** un usuario que entró con Google o Apple, **cuando** abre su cuenta, **entonces** no se
   le ofrece cambiar email ni contraseña, y entiende por qué.
6. **Dado** cualquier fallo de los anteriores, **cuando** se muestra, **entonces** no aparece
   ningún texto técnico de Firebase.
7. **Dado** un miembro que no es dueño y ha cambiado su email, **cuando** vuelve a entrar,
   **entonces** puede aparcar un vehículo compartido. *(Añadido al diseñar: hoy no podría, porque
   al aparcar se firma con el email del documento `users`, que nadie actualiza, y la regla lo
   compara con el del token.)*
8. **Dado** que la sesión termina fuera de la app —el enlace de cambio de email se abrió, o la
   contraseña se cambió en otro dispositivo—, **cuando** la app lo detecta, **entonces** lleva al
   login en lugar de quedarse en unas pestañas que no responden. *(Añadido al diseñar: Firebase
   revoca la sesión al cambiar el email o la contraseña.)*

## Preguntas abiertas — resueltas

Todas se cerraron en la fase de diseño; el razonamiento completo está en [design.md](design.md).

- **¿Dónde vive el nombre, y qué hay que propagar al cambiarlo?** → La fuente de la verdad es
  `users/{uid}.name`, y es lo único que escribe el cliente. Un trigger de servidor,
  `propagateUserProfile`, lo copia a `lastLocation.user` de los vehículos que esa persona aparcó y
  al `displayName` de Auth. Con el Admin SDK, las reglas de vehículos no se tocan.
- **¿Y el email?** → Hoy, quien no es dueño y cambia su email **no puede volver a aparcar**. La
  regla se queda como está, y cambia el cliente: firma con el email de Auth, sincroniza
  `users.email` al entrar en la app, y el mismo trigger propaga el cambio a las tarjetas.
- **La reautenticación, ¿pantalla o diálogo?** → Ninguna de las dos: es un campo «contraseña
  actual» dentro del propio formulario, y se pide siempre. Lo que 1.4 reutilizará es
  `AuthRepository.reauthenticate()` y el componente `PasswordField`.
- **¿Qué se le enseña a quien entró con Google o Apple?** → Su nombre, editable, y una fila que
  explica que su email y su contraseña se gestionan en la cuenta de Google o de Apple.
- **¿Se permite cambiar el nombre a cualquiera?** → Sí, también a quien entró con Google o Apple.
- **El email pendiente de verificar** → Un aviso claro al enviarlo, y nada más: Firebase no expone
  el email pendiente, así que no hay nada fiable que enseñar después.

## Fuera de alcance

- La cascada del borrado de cuenta y qué pasa con los vehículos compartidos.
- Vincular varios proveedores a la misma cuenta.
- Verificación en dos pasos.
