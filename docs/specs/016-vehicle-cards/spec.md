# Spec: Las tarjetas del mapa, y ordenar los vehículos desde su lista

- **ID**: 016-vehicle-cards
- **Estado**: Aprobada — diseño en [design.md](design.md)
- **Fecha**: 2026-11-25

## Problema / Por qué

Las tarjetas del carrusel se hicieron en la primera fase, cuando eran la única pantalla de la app:
tenían que servir para verlo todo, aparcar, borrar y ordenar. Desde entonces han llegado la pestaña
*Vehículos* (spec 009) y el detalle (spec 014), y la tarjeta sigue cargando con trabajo que ya tiene
un sitio mejor.

**Lo destructivo está en dos sitios.** El modo edición del carrusel —pulsación larga— enseña una
papelera en cada tarjeta, y el detalle tiene «Eliminar vehículo» y «Quitar de mi lista» desde la
spec 014. Un borrado tan cerca del botón de aparcar, en la pantalla que se usa a diario con prisa,
es justo lo que [`NAVIGATION.md`](../../NAVIGATION.md) dice que no debe estar ahí.

**Ordenar cuesta más de lo que debería.**

- **No se descubre.** Nada indica que una pulsación larga haga algo, y un lector de pantalla no la
  ofrece.
- **Se ordena a ciegas.** Se ve una tarjeta y un trozo de sus vecinas, nunca el orden entero.
- **Se mueve de uno en uno.** Llevar el último de trece al principio son doce toques.
- **Cada flecha es una transacción de Firestore.** Las transacciones necesitan el servidor, así que
  **sin conexión ordenar falla**, y con conexión son doce viajes de ida y vuelta para un solo cambio.

La spec 003 ya descartó arrastrar en el carrusel, porque choca con el gesto de deslizar entre
vehículos, y descartó la pestaña «Mis Coches» porque entonces era decorativa. **Lo segundo ya no es
verdad**: *Vehículos* es una lista de verdad, y una lista vertical es donde arrastrar para ordenar es
el gesto que todo el mundo conoce. Además, ordenar es gestión, no uso diario, y la gestión vive en
*Vehículos*.

**La propia tarjeta**, vista en el simulador:

- **Tiene una altura fija de 180 dp.** Un vehículo sin aparcar enseña su nombre, «Aún no aparcado» y
  un hueco vacío encima del botón.
- **«Última vez: 01 ene» no dice de qué.** La línea de debajo, «Aparcado por Alberto», completa la
  frase, pero son dos líneas para una idea. Y si quien aparcó eres tú, la tarjeta te nombra como a un
  tercero.
- **Nada indica que tocarla abre el detalle**, ni a la vista ni al lector de pantalla.
- **El primer aparcamiento no confirma nada.** El aviso «Ubicación actualizada correctamente» solo sale si hay una
  ubicación anterior a la que volver con «Deshacer». Si no la hay, aparcar no da ninguna señal.

## Objetivos

- **Lo destructivo, solo en el detalle**: la tarjeta no borra ni quita.
- **Ordenar desde la lista de *Vehículos***:
  - viendo el orden entero;
  - llevando un vehículo a cualquier posición de una vez;
  - con una alternativa sin arrastrar para el lector de pantalla;
  - y que funcione sin conexión.
- **Una tarjeta que se lea de un vistazo**: cuándo se aparcó y quién, sin huecos, y que se entienda
  que se puede tocar.
- **Aparcar siempre confirma.**
- **Dejar sitio para lo que viene.** Con las incidencias, la tarjeta llevará iconos en la esquina de
  arriba a la derecha con lo que tiene pendiente: ⛽, 🧽, o la ITV si está cerca. Está decidido en
  [`NAVIGATION.md`](../../NAVIGATION.md#cómo-se-verán-los-pendientes-decidido-para-la-fase-3). Esta
  spec no los construye, pero la tarjeta nueva no puede obligar a rehacerla cuando lleguen.

## No-objetivos

- Cambiar el marcador del mapa o el gesto de arrastrarlo.
- La distancia hasta el vehículo o la dirección donde está: la dirección llega con las ubicaciones
  guardadas del ROADMAP.
- Ordenar automáticamente, por uso o por cercanía.
- Que mi orden cambie el de los demás miembros: sigue siendo por persona, como en la spec 003.

## Historias de usuario

- Como usuario, quiero que la tarjeta me diga de un vistazo cuándo se aparcó y quién lo hizo, para no
  tener que leer dos líneas.
- Como usuario con varios vehículos, quiero ver todos a la vez mientras los ordeno, y mover uno
  directamente a su sitio.
- Como usuario, quiero que borrar un vehículo esté en un solo sitio, lejos del botón de aparcar.
- Como usuario de lector de pantalla, quiero poder ordenar sin arrastrar.

## Criterios de aceptación

**Borrar, fuera de la tarjeta**

1. **Dado** el carrusel, **cuando** mantengo pulsada una tarjeta, **entonces** no aparece ninguna
   opción de eliminar ni de quitar.
2. **Dado** el detalle, **cuando** soy el dueño o un miembro, **entonces** eliminar y quitar de mi
   lista siguen funcionando como en la spec 014.

**Ordenar**

3. **Dado** que tengo varios vehículos, **cuando** ordeno desde *Vehículos*, **entonces** los veo
   todos y puedo llevar uno a cualquier posición con un solo gesto.
4. **Dado** que he cambiado el orden, **cuando** vuelvo al mapa, **entonces** el carrusel tiene el
   mismo orden, y lo sigue teniendo tras cerrar y abrir la app.
5. **Dado** que no tengo conexión, **cuando** ordeno, **entonces** el nuevo orden se ve al momento y
   se guarda al volver la conexión, sin mensajes de error.
6. **Dado** un lector de pantalla, **cuando** estoy en un vehículo de la lista mientras ordeno,
   **entonces** puedo subirlo o bajarlo sin arrastrar.
7. **Dado** un vehículo compartido, **cuando** cambio su posición, **entonces** el orden de los demás
   miembros no cambia.
8. **Dado** que alguien me añade a un vehículo mientras ordeno, **cuando** se guarda mi orden,
   **entonces** ese vehículo no desaparece: sale al final.

**La tarjeta**

9. **Dado** un vehículo aparcado, **cuando** veo su tarjeta, **entonces** una sola línea dice cuándo y
   quién: «Aparcado hace 5 min por Edu», o «por ti» si fui yo. Si quien aparcó ya no tiene cuenta,
   solo el cuándo.
10. **Dado** un vehículo sin aparcar, **cuando** veo su tarjeta, **entonces** no hay ningún hueco
    vacío.
11. **Dado** el carrusel, **cuando** paso de un vehículo a otro, **entonces** las tarjetas no cambian
    de alto, y ni el botón de centrar ni el mapa dan saltos.
12. **Dado** cualquier tarjeta, **cuando** la miro, **entonces** se entiende que abre el detalle; y un
    lector de pantalla la anuncia con esa acción.
13. **Dado** un vehículo que nunca se aparcó, **cuando** lo aparco, **entonces** sale la confirmación,
    sin «Deshacer», porque no hay ubicación anterior a la que volver.
14. **Dado** una tarjeta con un nombre largo, **cuando** la miro, **entonces** el nombre se corta antes
    del final de su fila, donde están la flecha y, con la fase 3, irán los iconos de pendientes.

**En todas partes**

15. **Dado** el mapa y *Vehículos*, en español, inglés y francés, en Android y en iOS, **cuando** los
    uso, **entonces** todo lo anterior se cumple igual.

## Preguntas abiertas — resueltas

Todas se cerraron en el diseño. El razonamiento completo está en [design.md](design.md).

1. **¿Queda algo del modo edición en el carrusel?** → **No.** La pulsación larga deja de hacer nada,
   y ordenar vive en un solo sitio.
2. **¿Cómo se entra a ordenar?** → **Con «Ordenar» en la barra de *Vehículos***, que abre un modo con
   asas de arrastre y «Hecho». Fuera de ese modo, tocar una fila abre el detalle.
3. **¿A mano o con una librería?** → **[Reorderable](https://github.com/Calvin-LL/Reorderable)
   3.1.0**: multiplataforma, Apache-2.0 y compatible con Compose 1.9.3.
4. **¿Cuándo se guarda?** → **Una vez, al terminar, con una escritura normal** que se encola sin
   conexión, no con una transacción.
5. **¿Matrícula o indicador de posición?** → **Ninguno de los dos.** El icono con el color ya
   distingue el vehículo, y las tarjetas vecinas que asoman ya dicen que hay más.
6. **¿Qué altura?** → **La misma para todas, más baja que ahora**, porque todas tienen la misma
   estructura: el nombre, una línea y el botón.

## Fuera de alcance

- **Las incidencias y los vencimientos**: la sección «Pendiente» de *Vehículos*, los iconos de la
  tarjeta y la chapita de la pestaña. Llegan con la fase 3, y cómo se verán ya está en
  [`NAVIGATION.md`](../../NAVIGATION.md#cómo-se-verán-los-pendientes-decidido-para-la-fase-3).
- La dirección del sitio donde está aparcado.
- Cambiar cómo se aparca o se deshace, más allá de confirmar el primer aparcamiento.
