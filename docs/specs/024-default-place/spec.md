# Spec: La ubicación predeterminada de un vehículo

- **ID**: 024-default-place
- **Estado**: Implementada — diseño en [design.md](design.md)
- **Fecha**: 2026-11-27

## Problema / Por qué

Hoy aparcar siempre usa el GPS. Pero muchos coches duermen casi siempre en el mismo sitio —una plaza
de garaje— y ahí el GPS es justo lo que peor funciona: en un garaje subterráneo no hay señal, o la
posición sale a varios metros, en la calle de al lado. Quien aparca tiene que arrastrar el marcador
a mano o dejarlo mal.

El [`ROADMAP.md`](../../ROADMAP.md) lo tenía como 3.4, con una duda abierta: a quién pertenece el
garaje. Recomendaba que fuera **del vehículo**: cada vehículo define el suyo, y lo ven todos sus
miembros. La [spec 022](../022-vehicle-card-layout/spec.md) ya dejó la tarjeta preparada para un
segundo botón.

## Objetivos

- **Cada vehículo puede tener una ubicación predeterminada**, con su nombre: «Garaje», «Plaza 23».
- **Aparcar ahí con un toque, sin GPS**, desde la tarjeta del mapa, también sin conexión.
- **Que se vea que está ahí**: en la frase de la tarjeta y con un marcador propio en el mapa.

## No-objetivos

- **Varias ubicaciones por vehículo.** Una basta para el garaje. Si algún día hacen falta más, se
  pasa a una subcolección (`places`, en `DATA-MODEL.md`).
- **Compartir un garaje entre vehículos.** Cada vehículo define el suyo; dos coches en la misma plaza
  la definen dos veces.
- **Detectar sola que estás en el garaje**, por cercanía. El GPS falla, y más bajo tierra: la
  ubicación predeterminada se usa cuando alguien la elige.
- **Avisos** al llegar o al salir.

## Historias de usuario

- Como miembro de un vehículo que duerme en una plaza de garaje, quiero aparcarlo ahí con un toque,
  aunque no tenga señal.
- Como miembro, quiero ver de un vistazo en el mapa qué coches están en su garaje.
- Como miembro, quiero poner, cambiar o quitar el garaje de un vehículo.

## Criterios de aceptación

**Ponerla, cambiarla y quitarla**

1. **Dado** el detalle de un vehículo, **cuando** miro «Dónde está», **entonces** hay una fila
   «Ubicación predeterminada»: su nombre si la tiene, o una invitación a añadirla si no.
2. **Dado** esa fila, **cuando** la toco, **entonces** se abre un mapa para elegir el punto:
   - empieza donde está aparcado el vehículo, o donde estoy yo si nunca se ha aparcado;
   - el punto se elige moviendo el mapa bajo un marcador fijo en el centro;
   - el nombre es libre, hasta 40 caracteres, y por defecto «Garaje»;
   - al guardar, vuelvo al detalle y se ve el nombre.
3. **Dado** un vehículo con ubicación predeterminada, **cuando** la abro, **entonces** puedo cambiar
   el punto o el nombre, o quitarla.
4. **Dado** que soy miembro, sea o no el dueño, **cuando** pongo, cambio o quito la ubicación
   predeterminada, **entonces** se guarda, y las reglas no me dejan cambiar nada más del vehículo.

**Aparcar en ella**

5. **Dado** un vehículo sin ubicación predeterminada, **cuando** veo su tarjeta, **entonces**
   «Aparcar» es como hoy.
6. **Dado** un vehículo con ubicación predeterminada, **cuando** veo su tarjeta, **entonces** hay dos
   botones del mismo tamaño:
   - «Aquí», el principal, que aparca con el GPS, como siempre;
   - el de la ubicación, con su nombre, que aparca en ella sin pedir la ubicación.

   La tarjeta mide lo mismo que sin ella.
7. **Dado** el botón de la ubicación, **cuando** lo toco, **entonces** el vehículo queda aparcado en
   ella, a mi nombre y con la hora del momento, con el mismo aviso y el mismo «Deshacer» de siempre.
   Funciona sin permiso de ubicación y sin conexión.

**Que se vea que está ahí**

8. **Dado** un vehículo aparcado con el botón de su ubicación, **cuando** veo su tarjeta, **entonces**
   la frase lo dice («Garaje · hace 5 min · Edu», o como se decida en el diseño), y el detalle
   también, con la frase larga.
9. **Dado** ese vehículo, **cuando** veo el mapa, **entonces** su marcador es el pin de siempre, con
   el color de siempre —el elegido y los demás—, y el icono de la ubicación dentro.
10. **Dado** que después alguien lo aparca con «Aquí» o arrastra su marcador, **cuando** veo la
    tarjeta y el mapa, **entonces** vuelven a ser los de siempre.

**El pin y arrastrarlo** (cambios después de probarla)

13. **Dado** el mapa, **cuando** veo los pines en Android, **entonces** tienen la forma y el tamaño
    del pin de antes, y la casa de uno en su ubicación se distingue bien.
14. **Dado** un vehículo elegido, **cuando** mantengo pulsado su pin, **entonces** empiezo a
    arrastrarlo, también en iOS, y no se aparca en el punto del dedo. Mantener pulsado el pin de
    otro vehículo tampoco aparca el elegido encima de él.
15. **Dado** el mapa, **cuando** intento arrastrar el pin de un vehículo que no es el elegido,
    **entonces** no se mueve. Para moverlo, lo elijo antes, en el carrusel o tocando su pin.

**En todas partes**

11. **Dado** una versión anterior de la app, **cuando** aparca un vehículo que tiene ubicación
    predeterminada, **entonces** aparca como siempre y nada se rompe; la ubicación predeterminada
    sigue ahí para las versiones nuevas.
12. **Dado** Android y iOS, en español, inglés y francés, en claro y en oscuro, **cuando** uso todo
    lo anterior, **entonces** se cumple igual.

## Preguntas abiertas — resueltas con Alberto

1. **¿Cuántas por vehículo?** → **Una**, guardada en el propio vehículo. Sin colección nueva y sin
   escuchas extra: el vehículo ya se escucha.
2. **¿Desde dónde se pone?** → **Desde el detalle, en «Dónde está»**, con un mapa que empieza donde
   está aparcado.
3. **¿Quién puede ponerla, cambiarla o quitarla?** → **Cualquier miembro**, no solo el dueño. Es una
   excepción a que solo el dueño cambia los datos del vehículo, y las reglas la limitan a ese campo.
4. **¿Cómo se nombra?** → **Nombre libre, «Garaje» por defecto**, con un solo icono para todas.

## Preguntas para el diseño

- **Cómo se sabe que el vehículo está en su ubicación**: copiar el nombre dentro de `lastLocation`
  al aparcar (lo que proponía el `ROADMAP.md`), o guardar solo una marca y leer el nombre actual de
  la ubicación. Cambia qué pasa si alguien la renombra o la mueve con el coche ya aparcado ahí.
- **La frase de la tarjeta** cuando está en su ubicación: con el nombre, la frase corta de la spec
  022 puede no caber junto a los iconos.
- **El mapa para elegir el punto**: pantalla completa o dentro de las pestañas, y cómo se comparte
  entre Android (maps-compose) e iOS (`GMSMapView`).

## Fuera de alcance

- **Varias ubicaciones por vehículo** y **compartirlas entre vehículos**.
- **Detectar la ubicación por cercanía.**

## Cambio después de probarla (2026-11-27)

Alberto lo probó en su móvil y pidió tres cosas (criterios 13, 14 y 15):

- **Los pines de Android eran más pequeños y la casa no se distinguía.** Eran el icono `Place` de
  Material, más pequeño que el pin de Google y con un agujero en la cabeza: la casa blanca quedaba
  encima del mapa claro. Ahora se dibujan con la forma y el tamaño del de Google, con la cabeza
  sólida. En iOS ya se veían bien, porque son el pin del SDK con la casa encima.
- **En iOS, mantener pulsado un pin aparcaba el vehículo en el punto del dedo** en vez de empezar a
  arrastrarlo, y el arrastre se cortaba. Venía de la spec 017: allí el SDK avisa de la pulsación larga
  también encima de un marcador. En Android pasaba lo mismo con los pines que no se pueden arrastrar:
  al probar que el de otro vehículo no se movía, el elegido acabó encima de él. Ahora, en las dos
  plataformas, una pulsación larga encima de cualquier pin no aparca.
- **Solo se arrastra el pin del vehículo elegido**, para no mover otro sin querer.
