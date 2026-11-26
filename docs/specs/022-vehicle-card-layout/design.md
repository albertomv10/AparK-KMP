# Design: la tarjeta del mapa, una fila para cada cosa

- **Spec**: [spec.md](spec.md)
- **Estado**: Aprobado

## Enfoque

Cada fila de la tarjeta tiene un solo trabajo:
1. **Quién es:** el avatar y el nombre.
2. **Cómo está:** la frase corta —«Hace 5 min · Edu»— a la izquierda, y los iconos de las
   incidencias, a la derecha.
3. **Qué haces:** «Aparcar».

La flecha desaparece: toda la tarjeta abre el detalle, y el lector de pantalla ya lo anuncia. No
cambia nada del servidor.

## Lo que dijo el código antes de decidir nada

1. **La tarjeta (`HomeScreen.VehicleCard`)** es un `Column` con `spacedBy(12.dp)`. La primera fila
   llevaba el avatar, el nombre con `weight(1f)`, `IncidentIcons` (spec 020) y la flecha.
2. **`CardMinHeight` (148 dp)** lo usan las tarjetas de vehículo y la de «+», para medir lo mismo.
3. **`ParkedText.kt` ya separa el quién** (`whoParked`, que da `Who`) **del cuándo**
   (`TimeSentences`), así que una frase corta solo necesita sus propias frases de tiempo.
4. **Las frases de «Pendiente» (spec 020)** empiezan en minúscula, y en inglés llevan «on»
   («on 12 Nov»). No sirven para empezar una línea.

## La frase corta

- **`shortParkedLine(whenText, who, you)`**, pura: «Hace 5 min · Edu», «Ayer a las 18:45 · tú», o
  solo el cuándo si no hay a quién nombrar.
- **`CardTimeSentences`**: seis frases nuevas, con mayúscula inicial y sin «Aparcado», en los tres
  idiomas, más `card_by_you` («tú», «you», «toi»).
- **`ShortParkedText`**, en una línea, que se corta antes que los iconos. Sin ubicación dice «Aún no
  aparcado». Para el lector de pantalla, su descripción es la frase larga de siempre: el «·» no se lee
  bien.

**La tarjeta usa siempre la corta**, con incidencias o sin ellas, para que no cambie de forma al
deslizar. La lista de *Vehículos* y el detalle mantienen la larga.

## La tarjeta

- **Primera fila:** el avatar y el nombre, que solo se corta si no cabe en la fila entera.
- **Segunda fila:** `ShortParkedText` con `weight(1f)`; si hay incidencias, `IncidentIcons` a la
  derecha. La fila mide al menos 24 dp, lo que los iconos, así que mide lo mismo con ellos y sin
  ellos.
- **`CardMinHeight` pasa a 160 dp** (16 + 32 + 12 + 24 + 12 + 48 + 16), y la tarjeta de «+» mide como
  las demás. El diseño contaba 152, con el botón a 40; al probarlo, la tarjeta «+» salió más baja. Un
  botón de Material 3 dibuja 40 pero ocupa 48, su zona táctil mínima, y ese desfase de 8 dp venía de
  antes: con 148 ya se quedaba baja.

## Riesgos

- **Sin flecha, puede no quedar claro que la tarjeta se abre.** Es una tarjeta con sombra y se
  puede tocar entera, que es lo habitual. Si alguien no lo descubre, la flecha se puede devolver a la
  segunda fila.
- **La frase corta pierde el verbo.** En una tarjeta de aparcar se entiende: «Hace 5 min · Edu». El
  lector de pantalla sigue diciendo la frase entera.
