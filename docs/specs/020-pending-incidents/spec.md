# Spec: Lo pendiente de los vehículos, a la vista

- **ID**: 020-pending-incidents
- **Estado**: Implementada — diseño en [design.md](design.md)
- **Fecha**: 2026-11-26

## Problema / Por qué

La [spec 019](../019-incidents/spec.md) construyó las incidencias, pero **solo se ven en el detalle
de cada vehículo**. Para saber si a alguno le pasa algo hay que abrirlos uno por uno, y eso no lo hace
nadie: el problema se descubre al coger el coche, que es justo lo que las incidencias querían evitar.

[`NAVIGATION.md`](../../NAVIGATION.md#cómo-se-verán-los-pendientes-decidido-para-la-fase-3) ya
decidió dónde se verán, y la spec 019 lo dejó para esta:

- **arriba en *Vehículos*, lo pendiente de todos**;
- **en la tarjeta del mapa, iconos por tipo** en la esquina que la
  [spec 016](../016-vehicle-cards/spec.md) dejó libre;
- **en la pestaña, una chapita con el total**.

Es también lo que justifica la pestaña *Vehículos*: hasta ahora lo único que solo se hacía ahí era
ordenar. La regla que la separa del mapa —*el carrusel muestra dónde está, la lista muestra cómo
está*— empieza a cumplirse con esto.

## Objetivos

- **Ver de un vistazo qué les pasa a todos tus vehículos**, sin abrir ninguno.
- **Saber antes de coger un coche si tiene algo**, desde la tarjeta del mapa, sin tocar nada.
- **Que se note desde cualquier pestaña** que hay algo pendiente.
- **En tiempo real y sin conexión**, como el resto de la app.

## No-objetivos

- **Reportar o resolver desde la lista o desde la tarjeta.** Se hace en el detalle, como en la
  spec 019. La tarjeta se queda con lo del día a día: dónde está y aparcar.
- **Quién la reportó**, en la lista de pendientes. Se ve al abrir el detalle.
- **Cambiar las filas de la lista de vehículos.** Lo pendiente ya está arriba; las filas se quedan
  como hoy.
- **Cambiar el nombre de la pestaña.** Sigue siendo *Vehículos*: es la lista de vehículos, con lo
  pendiente encima.
- **La ITV y los vencimientos** (3.2). Cuando existan, entrarán en la misma sección y en la misma
  esquina de la tarjeta, pero no son parte de esta spec.
- **Avisar.** Las notificaciones son la fase 4.
- **Cambiar datos, reglas o funciones.** Todo se lee con lo que la spec 019 ya permite.

## Historias de usuario

- Como miembro de varios vehículos, quiero ver en un sitio todo lo que tienen pendiente, para no
  tener que abrirlos uno a uno.
- Como quien va a coger el coche, quiero ver en la tarjeta del mapa si le pasa algo, antes de bajar
  al garaje.
- Como miembro, quiero notar que hay algo pendiente aunque esté en otra pestaña.

## Criterios de aceptación

**La sección «Pendiente», en *Vehículos***

1. **Dado** que mis vehículos tienen incidencias abiertas, **cuando** abro *Vehículos*, **entonces**
   arriba, antes de la lista, hay una sección «Pendiente» con **todas las abiertas de todos**, la
   más reciente primero. Cada fila lleva:
   - el icono de su tipo, en el color de su categoría, como en el detalle;
   - su nombre: el del tipo, o el de la personalizada;
   - el vehículo y cuándo se reportó: «Furgoneta · hace 2 h».

   No lleva quién la reportó.
2. **Dado** que ninguno tiene incidencias abiertas, **cuando** abro *Vehículos*, **entonces** la
   sección es una sola línea, «Todo en orden», y la lista sigue debajo.
3. **Dado** una fila de «Pendiente», **cuando** la toco, **entonces** se abre el detalle de su
   vehículo.
4. **Dado** la lista de vehículos, **cuando** la veo, **entonces** sus filas son como hoy, y ordenar
   funciona igual que antes. «Ordenar» está junto al título «Tus vehículos», y no en la barra de
   arriba (cambio de Alberto al probarla).

**La tarjeta del mapa**

5. **Dado** un vehículo con incidencias abiertas, **cuando** veo su tarjeta en el carrusel,
   **entonces** en la esquina de arriba a la derecha, entre el nombre y la flecha, sale **un icono
   por tipo**, en el color de su categoría:
   - dos incidencias del mismo tipo son un solo icono;
   - como mucho tres, los de las más recientes, y después «+N» con los tipos que no caben;
   - el nombre se corta antes que los iconos;
   - la tarjeta mide lo mismo con iconos y sin ellos.
6. **Dado** esos iconos, **cuando** llega el lector de pantalla, **entonces** los anuncia por su
   nombre: «Poco combustible, Pinchazo y 2 más».
7. **Dado** un vehículo sin incidencias abiertas, **cuando** veo su tarjeta, **entonces** es como
   hoy.

**La chapita**

8. **Dado** que mis vehículos tienen incidencias abiertas, **cuando** veo la barra de pestañas,
   **entonces** *Vehículos* lleva una chapita con **el número de incidencias abiertas** de todos, y
   sin ninguna no hay chapita. Se ve desde cualquier pestaña.
9. **Dado** la chapita, **cuando** entro en *Vehículos* o miro lo pendiente, **entonces** no
   desaparece: no cuenta lo que no he visto, sino lo que está sin resolver. Solo baja al resolver o
   borrar.

**En tiempo real, sin conexión y sin errores**

10. **Dado** que otro miembro reporta, resuelve o borra una incidencia, **cuando** tengo la app
    abierta, **entonces** la sección, los iconos y la chapita cambian sin recargar. Lo mismo cuando
    lo hago yo en el detalle y vuelvo.
11. **Dado** que no tengo conexión, **cuando** reporto o resuelvo una incidencia, **entonces** la
    sección, los iconos y la chapita cambian al momento, igual que el detalle.
12. **Dado** una incidencia de un tipo que esta versión no conoce, **cuando** sale en la sección o
    en la tarjeta, **entonces** es una personalizada sin nombre, «Incidencia», con su icono (spec
    019, criterio 14).
13. **Dado** que me sacan de un vehículo, o su dueño lo borra, **cuando** tengo la app abierta,
    **entonces** sus incidencias desaparecen de la sección, de las tarjetas y de la chapita, sin
    avisos de error y sin que la app se cierre (spec 021).
14. **Dado** que las incidencias no se pueden cargar con la sesión viva, **cuando** abro
    *Vehículos*, **entonces** la sección lo dice en lugar de «Todo en orden», el fallo se registra
    en Crashlytics, y el resto de la app funciona.

**En todas partes**

15. **Dado** Android y iOS, en español, inglés y francés, en tema claro y oscuro, **cuando** uso
    todo lo anterior, **entonces** se cumple igual.

## Preguntas abiertas — resueltas con Alberto

1. **¿En qué orden va «Pendiente»?** → **La más reciente primero**, como en el detalle. Los tipos no
   tienen gravedad, y no se inventa una.
2. **¿Se ve quién la reportó?** → **No.** El nombre exigiría llamar a `getVehicleMembers` una vez por
   vehículo cada vez que se entra en la pestaña, y llegaría con retraso. La opción de fondo, el mapa
   `members` en el vehículo (`DATA-MODEL.md`), pide una migración y mantenerlo desde las funciones;
   no compensa por una línea. Quién la reportó se ve en el detalle.
3. **¿Se reporta desde la tarjeta del mapa?** (la pregunta 6 de la spec 019) → **No**, solo desde el
   detalle, que se abre tocando la tarjeta.
4. **¿Qué enseñan las filas de la lista de vehículos?** → **Nada nuevo.** Lo pendiente ya está
   arriba.

## Preguntas para el diseño

- **Cómo se leen.** `DATA-MODEL.md` dice que con diez vehículos como mucho basta una escucha por
  vehículo, y no una consulta de grupo de colección, que necesitaría `memberIds` en cada incidencia.
  Hay tres sitios que las necesitan a la vez —la lista, el carrusel y la chapita—, así que hay que
  decidir si comparten la escucha o si cada uno abre la suya.
- **Dónde vive la chapita**, que está en la barra del contenedor de pestañas y no en una pantalla.
- **Qué pasa con «Pendiente» mientras se ordena la lista.**

## Fuera de alcance

- **La ITV, las revisiones y el mantenimiento** (3.2 y 3.3).
- **Las notificaciones** (fase 4).
- **El mapa `members`** en el vehículo.
