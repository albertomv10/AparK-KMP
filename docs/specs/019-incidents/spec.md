# Spec: Las incidencias de un vehículo

- **ID**: 019-incidents
- **Estado**: Implementada, con los cambios después de probarla (ver al final) — diseño en [design.md](design.md)
- **Fecha**: 2026-11-25

## Problema / Por qué

AparK sirve hoy para saber **dónde** está un vehículo compartido. Lo que todavía no cuenta es **qué
le pasa**: que alguien lo dejó en reserva, que tiene un golpe nuevo, que hay que lavarlo o que se
encendió un testigo. Hoy eso se cuenta por WhatsApp, o no se cuenta, y quien coge el coche después
se entera al arrancarlo.

El ROADMAP la llama la fase que justifica la app (3.1): su valor no es solo saber dónde está el coche,
sino que todos sepan qué le pasa.

El terreno ya está preparado:

- [`DATA-MODEL.md`](../../DATA-MODEL.md) esboza la subcolección `vehicles/{id}/incidents` y su
  consulta: las abiertas de un vehículo, por fecha, con un índice compuesto.
- [`NAVIGATION.md`](../../NAVIGATION.md#cómo-se-verán-los-pendientes-decidido-para-la-fase-3)
  decidió dónde se verán: arriba en *Vehículos* y como iconos por tipo en la tarjeta del mapa.
- La [spec 016](../016-vehicle-cards/spec.md) dejó sitio en la tarjeta para esos iconos.

Esta spec construye las incidencias en sí: crearlas, verlas y resolverlas desde el detalle del
vehículo, con todo lo que va por detrás: datos, reglas, el índice y la limpieza. **Dónde más se ven**
—la sección «Pendiente», los iconos de la tarjeta y la chapita de la pestaña— **es la spec 020**.

## Objetivos

- **Cualquier miembro puede reportar una incidencia** de un vehículo: qué le pasa, de una lista
  agrupada por categorías, o una personalizada con su propio nombre; y, si quiere, una nota.
- **Todos los miembros la ven al momento**, con quién la reportó y cuándo.
- **Cualquier miembro puede marcarla como resuelta**, y queda constancia de quién y cuándo.
- **Funciona sin conexión**, como aparcar: se ve al momento y se guarda al volver la red.
- **Nadie tiene que limpiar**: una incidencia resuelta se queda 30 días a la vista y después se
  borra sola. Borrar el vehículo borra sus incidencias.

## No-objetivos

- **Fotos.** Necesitan Firebase Storage, el mismo motivo por el que se aplazó la foto de perfil.
- **Comentarios** o una conversación dentro de cada incidencia.
- **Asignar** una incidencia a alguien, o ponerle prioridad.
- **Avisar a los demás.** Las notificaciones son la fase 4, y llegan cuando haya de qué avisar.
- **Mostrarlas fuera del detalle.** Eso es la spec 020.

## Historias de usuario

- Como miembro de un vehículo, quiero avisar de que lo dejo con poca gasolina, para que quien lo
  coja después lo sepa antes de salir.
- Como miembro, quiero ver qué le pasa al vehículo antes de cogerlo.
- Como quien lo arregla, quiero marcarlo como resuelto para que nadie más se preocupe.

## Criterios de aceptación

**Ver**

1. **Dado** que soy miembro de un vehículo, **cuando** abro su detalle, **entonces** veo sus
   incidencias abiertas, la más reciente primero. De cada una veo:
   - el icono de su tipo, en el color de su categoría;
   - su nombre: el del tipo, o el que le puso quien la reportó si es personalizada;
   - la nota, si la tiene;
   - quién la reportó y cuándo.

   El nombre, la nota y quién y cuándo se distinguen a simple vista, y no van pegados.
2. **Dado** que quien la reportó ya no es miembro, o borró su cuenta, **cuando** la veo,
   **entonces** sale solo cuándo, sin nombre y sin ningún texto en su lugar.
3. **Dado** un vehículo sin incidencias abiertas, **cuando** abro su detalle, **entonces** lo dice en
   una línea, y no hay una sección vacía.

**Reportar**

4. **Dado** el detalle, **cuando** reporto una incidencia, **entonces** veo todos los tipos a la vez,
   separados por categorías, y elijo qué le pasa: elegirlo es obligatorio. Si no encaja ninguno,
   elijo «Personalizada» y le pongo un nombre, también obligatorio. La nota es opcional en los dos
   casos. Al guardarla aparece en la lista, y les aparece a los demás miembros que tengan el detalle
   abierto sin que tengan que recargar.
5. **Dado** un vehículo con su sistema y su combustible indicados (spec 015), **cuando** elijo
   qué le pasa, **entonces** solo salen las opciones que tienen sentido: en un eléctrico «Poca carga»
   y no «Poco combustible», y «AdBlue» solo si es diésel. Si el vehículo no tiene esos datos, salen
   todas.
6. **Dado** el formulario, **cuando** escribo la nota o el nombre de una personalizada,
   **entonces** cada uno tiene un límite de longitud, que comprueban también las reglas.

**Resolver**

7. **Dado** una incidencia abierta, **cuando** cualquier miembro la marca como resuelta,
   **entonces** deja de estar entre las abiertas para todos, y queda guardado quién la resolvió y
   cuándo.
8. **Dado** que acabo de resolver una, **cuando** sale el aviso, **entonces** tiene «Deshacer», y
   deshacer la devuelve a abierta.
9. **Dado** un vehículo con incidencias resueltas en los últimos 30 días, **cuando** abro su detalle,
   **entonces** están en una sección «Resueltas», plegada, con quién las resolvió y cuándo.
10. **Dado** una incidencia resuelta hace más de 30 días, **cuando** abro el detalle, **entonces** ya
    no sale, y Firestore la borra sola (TTL), sin que nadie tenga que hacer nada.

**Borrar**

11. **Dado** una incidencia, **cuando** soy quien la reportó o el dueño del vehículo, **entonces**
    puedo borrarla, después de confirmarlo. Sirve para corregir un error, como un aviso repetido o
    equivocado. Los demás no ven la opción, y las reglas se lo deniegan.

**Seguridad y datos**

12. **Dado** que no soy miembro de un vehículo, **cuando** intento leer o escribir sus incidencias,
    **entonces** las reglas me lo deniegan.
13. **Dado** que reporto o resuelvo una incidencia, **cuando** se guarda, **entonces** las reglas
    comprueban:
    - que el tipo es de la lista;
    - que lleva nombre si es personalizada, y solo entonces, y que no pasa del límite;
    - que la nota no pasa del límite;
    - que quien reporta o resuelve soy yo;
    - y que la fecha es la del servidor.
14. **Dado** una incidencia de un tipo que esta versión de la app no conoce, **cuando** se muestra,
    **entonces** sale como una personalizada sin nombre, «Incidencia», con su icono, sin errores (la
    regla de las listas de la spec 015).

**Ciclo de vida**

15. **Dado** un vehículo con incidencias, **cuando** su dueño lo borra, **entonces** sus incidencias
    se borran también.
16. **Dado** que no tengo conexión, **cuando** reporto o resuelvo una incidencia, **entonces** el
    cambio se ve al momento y se guarda al volver la red, sin mensajes de error.

**En todas partes**

17. **Dado** Android y iOS, en español, inglés y francés, **cuando** uso las incidencias,
    **entonces** todo lo anterior se cumple igual.

## Preguntas abiertas — resueltas

1. **¿Qué tipos hay?** → **Tipos concretos, agrupados por categorías, y una personalizada.** Es la
   propuesta de Alberto, con algunos ajustes, y el cambio que hizo después de probarla (ver al
   final).

   | Categoría (solo agrupa) | Tipos |
   |---|---|
   | **Niveles y energía** | Poco combustible · Poca carga · Aceite · Limpiaparabrisas · AdBlue |
   | **Mecánica y testigos** | Testigo encendido · No arranca · Avería · Ruido extraño · Luz fundida |
   | **Neumáticos** | Pinchazo · Presión baja · Desgaste |
   | **Carrocería e interior** | Golpe o arañazo · Limpieza · Objetos dentro |
   | — | **Personalizada**: un nombre de hasta 40, obligatorio |

   - **La incidencia es el tipo, y la categoría solo agrupa** las opciones en el formulario y les da
     su color: no se guarda, porque cada tipo pertenece a una sola.
   - **«Gasolina/Batería» se separa** en «Poco combustible» y «Poca carga», y solo salen las que
     tienen sentido para el vehículo (criterio 5).
   - **«No arranca» va aparte**: es lo más urgente que le puede pasar a un coche compartido, y cubre
     también la batería de 12 V descargada.
   - **«Objetos dentro»** avisa de que hay cosas en el coche, en el maletero o donde sea.
   - **«Personalizada»** es para lo que no encaje: una sola, sin categoría, con el icono de un aviso
     (el octógono con exclamación, distinto del triángulo de «Testigo encendido»). Solo ella guarda
     su nombre; los tipos de la lista no guardan texto, porque se traducen al idioma de quien mira.
   - **Un solo tipo por incidencia**: dos problemas distintos son dos incidencias.
   - **Cada tipo tiene su código y su icono.** En la tarjeta del mapa (spec 020) se verá ese.

2. **¿Quién resuelve y quién borra?** → **Resuelve cualquier miembro. Borra quien la reportó o el
   dueño**, y solo para corregir errores, porque las resueltas se borran solas.
3. **¿Se ven las resueltas?** → **Sí, 30 días, en una sección plegada. Después se borran solas.**
   - Al resolverla se guarda una fecha de caducidad, y Firestore la borra al llegar (TTL), como ya
     hace con las invitaciones.
   - «Deshacer» la quita.
   - Así nadie tiene que limpiar nada, y se puede ver qué ha pasado este mes.
   - Las abiertas no caducan: son problemas reales, y alguien tiene que resolverlos.
4. **¿Cómo se muestra el nombre de quien la reportó?** → **Se guarda solo su uid**, y el nombre sale
   de la lista de miembros que ya carga el detalle (`getVehicleMembers`).
   - Así no hay copias que mantener cuando alguien cambia su nombre o borra su cuenta.
   - Si quien la reportó ya no es miembro o borró su cuenta, ese apartado se queda vacío
     (criterio 2).
   - Si algún día molesta, esa función puede devolver también los nombres de quienes aparecen en
     las incidencias sin tocar los datos.
5. **¿Se puede editar?** → **No.** Se borra y se crea otra.
6. **¿Desde dónde se reporta?** → **Desde el detalle.** Reportar desde la tarjeta del mapa se decide
   en la spec 020.

## Fuera de alcance

- **La spec 020**: la sección «Pendiente» en *Vehículos*, los iconos por tipo en la tarjeta y la
  chapita con el total en la pestaña.
- **La ITV y las revisiones** (3.2) y **el mantenimiento** (3.3).
- **Las notificaciones** (fase 4), incluido avisar al reportar o al resolver.
- **Fotos y comentarios.**

## Cambio después de probarla (2026-11-26)

Alberto probó la primera versión y cambió dos cosas antes de integrarla:

- **Una categoría sola no es una incidencia.** «Niveles y energía» no dice qué le pasa al coche, y
  obligaba a leer la nota. Ahora se elige siempre el tipo concreto, y lo que no encaje es una
  «Personalizada» con su nombre. La categoría «Otro» desaparece. Como no había nada en producción,
  los datos cambian a la vez: `category` y `tag` se sustituyen por `type`, y se añade `title`, solo
  para las personalizadas.
- **En el detalle, el nombre, la nota y quién y cuándo se parecían demasiado**, y el «Reportada por»
  iba pegado a la nota. El criterio 1 pide ahora que se distingan y que no vayan pegados.

Y en una segunda prueba, dos más:

- **No hay que elegir la categoría para ver sus tipos.** El formulario los enseña todos a la vez,
  separados por categorías, y se toca directamente el que es.
- **Cada categoría tiene un color**, que llevan sus iconos en el formulario y en el detalle. Todo se
  veía muy monocromático, y el color ayuda a distinguir de un vistazo una avería de una limpieza.

