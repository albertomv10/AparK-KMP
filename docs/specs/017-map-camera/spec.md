# Spec: La cámara del mapa, y mover un vehículo manteniendo pulsado

- **ID**: 017-map-camera
- **Estado**: Implementada — diseño en [design.md](design.md)
- **Fecha**: 2026-11-25

## Problema / Por qué

La cámara del mapa la decide cada plataforma por su cuenta: Android en `AparKMap.android.kt`, iOS a
medias entre `AparKMap.ios.kt` y Swift. Leyendo las dos, esto es lo que hace hoy:

| Momento | Android | iOS |
|---|---|---|
| Abrir el mapa, con el vehículo elegido aparcado | Salta a él, zoom 18 | Salta a él, zoom 17 |
| … si el elegido nunca se aparcó | Se queda en el mundo entero | Se queda en España, zoom 5 |
| … sin ningún vehículo | Se queda en el mundo entero | Va a tu ubicación, zoom 15 |
| Pasar a otra tarjeta | Vuela a ese vehículo, zoom 18, en 1 s | Vuela, zoom 17, en 0,8 s |
| … a una que nunca se aparcó | No se mueve: sigue enseñando la anterior | Igual |
| Aparcar o arrastrar el elegido | Vuela al sitio nuevo, zoom 20 | Igual |
| **Cambia cualquier otro dato** | **Vuelve al vehículo elegido, zoom 18** | **Igual, zoom 17** |
| Volver a la pestaña del mapa | Salta al vehículo, zoom 18 | Salta al vehículo, zoom 17 |
| Botón de centrar | Tu última ubicación guardada, zoom 16. Si no hay ninguna guardada, no hace nada | Tu ubicación, zoom 15. Si no la tiene, la pide |
| Tocar un marcador | Lo centra y abre un globo con nombre y modelo. La tarjeta no cambia | Lo centra y abre un globo con el nombre |

Hay tres problemas:

- **La cámara se mueve sola.** El efecto que la mueve depende de la lista entera de vehículos. Así
  que cualquier cambio vuelve a llevar la cámara al vehículo elegido: que otro miembro aparque otro
  coche, que cambie un nombre o que se ordene la lista. Si estabas mirando los alrededores, te
  devuelve de golpe y pierdes tu zoom. Es el fallo más molesto, y nadie lo ha pedido.
- **Las dos plataformas no coinciden:** el zoom de la tarjeta es 18 en una y 17 en la otra, el de
  centrar 16 y 15, y sin vehículos una va a tu ubicación y la otra enseña el mundo.
- **Hay casos sin respuesta:**
  - Un vehículo que nunca se aparcó deja la cámara donde estaba, enseñando otro vehículo.
  - En Android, centrar no hace nada si el sistema no tiene una ubicación guardada.
  - Tocar el marcador de un vehículo no lo elige.

Además, **mover un vehículo a mano** hoy solo se puede arrastrando su marcador: hay que mantenerlo
pulsado, arrastrarlo y soltarlo. Si el sitio está lejos, hay que arrastrar a lo largo de la
pantalla. Mantener pulsado el punto exacto del mapa donde está el vehículo es más rápido y más
preciso, y es el gesto que todo el mundo conoce de otras apps de mapas.

## Objetivos

- **Una sola regla de cámara para las dos plataformas**, decidida en código común y probada.
- **La cámara solo se mueve por algo que importa:**
  - abrir el mapa;
  - elegir otro vehículo;
  - que el elegido cambie de sitio;
  - pedir centrar.

  Nunca por cambios en otros datos.
- **Ningún caso sin respuesta:**
  - un vehículo sin aparcar lleva la cámara a tu ubicación, que es donde lo dejaría «Aparcar»;
  - centrar siempre funciona.
- **Mover el vehículo elegido manteniendo pulsado un punto del mapa**, con la misma red de seguridad
  que arrastrar: el aviso con «Deshacer».

## No-objetivos

- **Distinguir corregir la ubicación de aparcar.** Mover a mano, sea arrastrando o manteniendo
  pulsado, sigue contando como aparcar ahora y por ti, como hoy al arrastrar.
- **Cambiar el aspecto de los marcadores.** Siguen rojo para el elegido y azul para los demás.
- **Que el mapa se reconstruya o no al volver a la pestaña.** Es una diferencia de plataforma, ya
  explicada en AGENTS.md.

## Historias de usuario

- Como usuario, quiero mirar por el mapa sin que la cámara me devuelva sola al coche cada vez que
  alguien hace algo.
- Como usuario que corrige dónde dejó el coche, quiero mantener pulsado el sitio y que el coche se
  mueva ahí, sin arrastrarlo por media pantalla.
- Como usuario, quiero que la app se comporte igual en Android y en iPhone.

## Criterios de aceptación

**La cámara**

1. **Dado** que miro el mapa, **cuando** otro miembro aparca otro vehículo, cambia un nombre o
   reordeno mis vehículos, **entonces** la cámara no se mueve.
2. **Dado** el carrusel, **cuando** paso a un vehículo aparcado, **entonces** la cámara vuela a él con
   el zoom de vehículo, igual en las dos plataformas.
3. **Dado** el carrusel, **cuando** paso a un vehículo que nunca se aparcó, **entonces** la cámara va
   a mi ubicación.
4. **Dado** que abro el mapa, **cuando** el vehículo elegido está aparcado, **entonces** aparece ya en
   él, sin un vuelo desde otro sitio. Si no está aparcado, o no tengo vehículos, aparece en mi
   ubicación.
5. **Dado** el vehículo elegido, **cuando** cambia de sitio —lo aparco, lo muevo o lo aparca otro—,
   **entonces** la cámara lo sigue.
6. **Dado** el botón de centrar, **cuando** lo pulso, **entonces** la cámara va a mi ubicación aunque
   el sistema no tuviera ninguna guardada.

**Mover manteniendo pulsado**

7. **Dado** un vehículo elegido en el carrusel, **cuando** mantengo pulsado un punto del mapa,
   **entonces** el vehículo pasa a ese punto, el móvil vibra y sale el aviso con «Deshacer».
8. **Dado** que lo he movido así, **cuando** pulso «Deshacer», **entonces** vuelve donde estaba.
9. **Dado** un vehículo que nunca se aparcó, **cuando** mantengo pulsado el mapa, **entonces** queda
   aparcado ahí, con el aviso sin «Deshacer», como en la spec 016.
10. **Dado** que la tarjeta elegida es la de «+», **cuando** mantengo pulsado el mapa, **entonces** no
    pasa nada.
11. **Dado** cualquier vehículo, **cuando** arrastro su marcador, **entonces** sigue funcionando como
    hoy.

**En todas partes**

12. **Dado** Android y iOS, **cuando** hago cada una de estas cosas, **entonces** la cámara se
    comporta igual en las dos.

## Preguntas abiertas — resueltas

Todas se cerraron en el diseño. El razonamiento completo está en [design.md](design.md).

1. **¿Qué zoom después de aparcar o mover?** → **De cerca, zoom 20, como antes.** Se probó
   conservar el zoom y Alberto lo descartó. El GPS se equivoca a veces varios metros, y de lejos el
   error no se ve; de cerca sí, y se corrige a mano.
2. **¿Mantener pulsado mueve o pregunta?** → **Mueve directamente**, con vibración y «Deshacer», como
   arrastrar.
3. **¿Tocar un marcador elige el vehículo?** → **Sí.** El mapa y el carrusel hablan siempre del mismo
   vehículo.
4. **¿Qué zoom al elegir un vehículo?** → **Siempre 17**, y también para tu ubicación. Es el mismo en
   las dos plataformas.
5. **¿Cómo se vuelve al vehículo?** → **Con un botón «Ir al vehículo»**, encima del de centrar y del
   mismo tamaño. Lleva el icono de su tipo y solo aparece si el vehículo está aparcado. Además
   **acerca hasta el zoom 20**, el que antes ponía aparcar, para ver exactamente dónde está. Así no
   repite lo que ya hace elegir el vehículo, que lo enseña a zoom 17. Esto se decidió al probarlo.

## Fuera de alcance

- Las ubicaciones guardadas, como «el garaje» (ROADMAP 3.4).
- La dirección del sitio donde está aparcado.
- Ver todos los vehículos a la vez, con un zoom que los abarque.
