# Spec: La tarjeta del mapa, una fila para cada cosa

- **ID**: 022-vehicle-card-layout
- **Estado**: Implementada — diseño en [design.md](design.md)
- **Fecha**: 2026-11-26

## Problema / Por qué

La [spec 020](../020-pending-incidents/spec.md) puso en la primera fila de la tarjeta del carrusel
un icono por tipo de incidencia abierta, entre el nombre y la flecha. Alberto lo probó en su móvil, y
funciona, pero **no cabe todo en una línea**: el avatar, el nombre, hasta tres iconos, «+N» y la
flecha. Lo que se corta es el nombre, y «Coche compartido» se queda en «Coche …».

Es justo lo que menos debería cortarse: el nombre es lo que te dice qué coche es. La primera fila hace
tres cosas a la vez —**quién es** (el avatar y el nombre), **cómo está** (los iconos) y **que se puede
abrir** (la flecha)—, y compiten por el mismo sitio.

Además, la spec 023 traerá una ubicación predeterminada por vehículo —la plaza de garaje, el 3.4 de
[`ROADMAP.md`](../../ROADMAP.md)— y, con ella, un segundo botón al lado de «Aparcar». La tarjeta
tiene que estar preparada.

## Objetivos

- **Una fila para cada cosa**: quién es, cómo está y qué haces con él.
- **El nombre se ve entero** siempre que quepa en la fila.
- **Cómo está, junto**: dónde y cuándo se aparcó, y qué le pasa, en la misma fila.
- **La tarjeta no cambia de alto**, ni entre vehículos ni con incidencias o sin ellas.

## No-objetivos

- **La ubicación predeterminada.** Es la spec 023, con su botón, su nombre en la tarjeta y su
  marcador en el mapa.
- **Una banda de estado con texto** (la opción C que se valoró). Daría más información, pero quita
  unos 30 dp de mapa a todas las tarjetas.
- **Cambiar qué iconos se enseñan.** Siguen las reglas de la spec 020: uno por tipo, los de las más
  recientes primero, y «+N».
- **Cambiar qué hace «Aparcar»**, la lista de *Vehículos* o el detalle.

## Historias de usuario

- Como miembro de varios vehículos, quiero reconocer cada tarjeta por su nombre completo, aunque
  tenga incidencias.
- Como quien va a coger el coche, quiero ver de un vistazo dónde está, cuándo se dejó y si le pasa
  algo.

## Criterios de aceptación

1. **Dado** un vehículo con incidencias abiertas, **cuando** veo su tarjeta, **entonces** la primera
   fila lleva solo el avatar y el nombre, y el nombre se ve entero si cabe en la fila. Solo se
   corta si es más largo que la fila entera.
2. **Dado** esa tarjeta, **cuando** la veo, **entonces** la segunda fila lleva cuándo se aparcó y
   quién, a la izquierda, y los iconos de las incidencias, a la derecha. Si no cabe todo, lo que se
   corta es la frase, nunca los iconos.
3. **Dado** cualquier tarjeta, **cuando** la veo, **entonces** no lleva flecha. Toda la tarjeta
   sigue abriendo el detalle, y el lector de pantalla lo sigue anunciando.
4. **Dado** el carrusel, **cuando** paso de un vehículo con incidencias a uno sin ellas, **entonces**
   las tarjetas miden lo mismo y la frase dice lo mismo de la misma forma.
5. **Dado** un vehículo que nunca se ha aparcado, **cuando** veo su tarjeta, **entonces** dice «Aún
   no aparcado», como ahora.
6. **Dado** el lector de pantalla, **cuando** llega a la tarjeta, **entonces** anuncia el nombre,
   cuándo y quién, y lo que tiene pendiente, como ahora.
7. **Dado** Android y iOS, en español, inglés y francés, en claro y en oscuro, y con la letra
   grande, **cuando** veo las tarjetas, **entonces** todo lo anterior se cumple.

## Preguntas abiertas — resueltas con Alberto

1. **¿Cuántos iconos caben en la segunda fila?** → **Tres y «+N»**, como en la spec 020.
2. **¿La tarjeta usa una frase más corta?** Con tres iconos, «Aparcado hace 5 min por Edu» no cabe en
   la fila. → **En la tarjeta, siempre la corta**, con o sin incidencias, para que no cambie de
   forma al pasar de un vehículo a otro (criterio 4):
   - «Hace 5 min · Edu»;
   - «Ayer a las 18:45 · tú»;
   - «El 12 nov · Edu».

   En la lista de *Vehículos* y en el detalle se queda la larga, que tiene sitio.
3. **¿Se echa de menos la flecha?** Sin ella, la tarjeta sigue pareciendo tocable porque es una
   tarjeta con sombra, y el lector de pantalla anuncia «abrir el detalle». → **Se quita.**

## Fuera de alcance

- **La spec 023**: la ubicación predeterminada, su botón al lado de «Aparcar», «En el garaje» en la
  frase y un marcador propio en el mapa.
- **La banda de estado con texto** (opción C).
