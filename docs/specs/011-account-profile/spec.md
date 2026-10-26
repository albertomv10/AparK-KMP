# Spec: la cuenta, editable

- **ID**: 011-account-profile
- **Estado**: Borrador
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
2. **Dado** un usuario que pide cambiar email o contraseña, **cuando** lleva demasiado tiempo con
   la sesión abierta, **entonces** se le pide su contraseña actual antes de seguir, y **no** se le
   echa de la app.
3. **Dado** que ha pedido cambiar su email, **cuando** confirma, **entonces** recibe un correo de
   verificación **en la dirección nueva**, y su email de acceso **no cambia hasta que lo abre**.
4. **Dado** que introduce mal su contraseña actual al reautenticarse, **cuando** se le avisa,
   **entonces** el mensaje es legible y puede reintentar sin empezar de cero.
5. **Dado** un usuario que entró con Google o Apple, **cuando** abre su cuenta, **entonces** no se
   le ofrece cambiar email ni contraseña, y entiende por qué.
6. **Dado** cualquier fallo de los anteriores, **cuando** se muestra, **entonces** no aparece
   ningún texto técnico de Firebase.

## Preguntas abiertas

- **¿Dónde vive el nombre, y qué hay que propagar al cambiarlo?** Hoy está en tres sitios: el
  `displayName` de Firebase Auth, el campo `name` del documento `users`, y **copiado dentro de cada
  `lastLocation.user`** de los vehículos que esa persona aparcó (`ParkedBy`, spec 008). Lo tercero
  es una desnormalización deliberada, así que cambiar el nombre no es una escritura: es una
  cascada. ¿Se reescriben los vehículos —y con qué reglas, si solo el dueño puede escribirlos—, se
  deja el histórico como estaba, o lo hace una Cloud Function?
- **¿Y el email?** Está en `users.email` además de en Auth, y la regla de aparcar compara
  `request.resource.data.lastLocation.user.email == request.auth.token.email`. Si el email de Auth
  cambia y el documento no, ¿sigue pudiendo aparcar?
- **La reautenticación, ¿pantalla o diálogo?** Un diálogo mantiene el contexto y es lo que hacen la
  mayoría de apps; una pantalla se presta mejor a reutilizarse desde el borrado de cuenta.
- **¿Qué se le enseña a quien entró con Google o Apple?** Ocultar las opciones sin más deja la
  pantalla distinta sin explicación. ¿Se enseñan deshabilitadas con el motivo, o se indica de dónde
  viene su identidad?
- **¿Se permite cambiar el nombre a cualquiera?** Es el único dato de los tres que sí tiene sentido
  para todos, incluidos los de Google y Apple.
- **El email pendiente de verificar** (criterio 3) es un estado que la app no tiene hoy. ¿Se enseña
  en algún sitio, o se asume que el correo basta?

## Fuera de alcance

- La cascada del borrado de cuenta y qué pasa con los vehículos compartidos.
- Vincular varios proveedores a la misma cuenta.
- Verificación en dos pasos.
