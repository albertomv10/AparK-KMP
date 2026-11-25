# Spec: Las incidencias de un vehículo

- **ID**: 019-incidents
- **Estado**: Aprobada — diseño en [design.md](design.md)
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

- **Cualquier miembro puede reportar una incidencia** de un vehículo: una categoría y, si quiere,
  una etiqueta más concreta y una nota.
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
   - su icono: el de la etiqueta si tiene una, o el de la categoría;
   - el nombre de la categoría y de la etiqueta;
   - la nota, si la tiene;
   - quién la reportó y cuándo.
2. **Dado** que quien la reportó ya no es miembro, o borró su cuenta, **cuando** la veo,
   **entonces** sale solo cuándo, sin nombre y sin ningún texto en su lugar.
3. **Dado** un vehículo sin incidencias abiertas, **cuando** abro su detalle, **entonces** lo dice en
   una línea, y no hay una sección vacía.

**Reportar**

4. **Dado** el detalle, **cuando** reporto una incidencia, **entonces** elijo una categoría, que es
   obligatoria, y si quiero una etiqueta de esa categoría y una nota. Al guardarla aparece en la
   lista, y les aparece a los demás miembros que tengan el detalle abierto sin que tengan que
   recargar.
5. **Dado** un vehículo con su sistema y su combustible indicados (spec 015), **cuando** elijo
   etiqueta, **entonces** solo salen las que tienen sentido: en un eléctrico «Poca carga» y no «Poco
   combustible», y «AdBlue» solo si es diésel. Si el vehículo no tiene esos datos, salen todas.
6. **Dado** el formulario, **cuando** escribo la nota, **entonces** tiene un límite de longitud, que
   comprueban también las reglas.

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
    - que la categoría es de la lista;
    - que la etiqueta es de esa categoría;
    - que la nota no pasa del límite;
    - que quien reporta o resuelve soy yo;
    - y que la fecha es la del servidor.
14. **Dado** una incidencia con una categoría que esta versión de la app no conoce, **cuando** se
    muestra, **entonces** sale como «Otro», sin errores (la regla de las listas de la spec 015). Una
    etiqueta desconocida no se muestra.

**Ciclo de vida**

15. **Dado** un vehículo con incidencias, **cuando** su dueño lo borra, **entonces** sus incidencias
    se borran también.
16. **Dado** que no tengo conexión, **cuando** reporto o resuelvo una incidencia, **entonces** el
    cambio se ve al momento y se guarda al volver la red, sin mensajes de error.

**En todas partes**

17. **Dado** Android y iOS, en español, inglés y francés, **cuando** uso las incidencias,
    **entonces** todo lo anterior se cumple igual.

## Preguntas abiertas — resueltas

1. **¿Qué tipos hay?** → **Categorías, con etiquetas opcionales.** Es la propuesta de Alberto, con
   algunos ajustes.

   | Categoría | Etiquetas (opcionales, como mucho una) |
   |---|---|
   | **Niveles y energía** | Poco combustible · Poca carga · Aceite · Limpiaparabrisas · AdBlue |
   | **Mecánica y testigos** | Testigo encendido · No arranca · Avería · Ruido extraño · Luz fundida |
   | **Neumáticos** | Pinchazo · Presión baja · Desgaste |
   | **Carrocería e interior** | Golpe o arañazo · Limpieza · Objetos dentro |
   | **Otro** | Ninguna: lo que no encaje se explica en la nota |

   - **«Gasolina/Batería» se separa** en «Poco combustible» y «Poca carga», y solo salen las que
     tienen sentido para el vehículo (criterio 5).
   - **«No arranca» va aparte**: es lo más urgente que le puede pasar a un coche compartido, y cubre
     también la batería de 12 V descargada.
   - **«Objetos dentro»** avisa de que hay cosas en el coche, en el maletero o donde sea.
   - **«Otro» hace falta** para lo que no encaje y para leer un código desconocido.
   - **Una sola etiqueta**: dos problemas distintos son dos incidencias.
   - **Cada categoría y cada etiqueta tienen su código y su icono.** En la tarjeta del mapa
     (spec 020) se verá el de la etiqueta si la hay, y si no el de la categoría.

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
