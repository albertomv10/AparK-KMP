# Spec: El splash de Android, como el de iOS

- **ID**: 018-splash
- **Estado**: Implementada — diseño en [design.md](design.md)
- **Fecha**: 2026-11-25

## Problema / Por qué

En iOS el splash se ve bien: una pantalla de arranque con fondo blanco fijo y el logo a 180 puntos,
sacado de un PNG de 1024 px. En Android se ve peor, y en el Xiaomi de Alberto, con el modo oscuro, el
fondo sale oscuro y el logo no destaca.

**La calidad del logo en Android**

- El icono es `splash.png`, de 2000×2000 px, en `drawable/` sin densidad. Android lo trata como
  pensado para las pantallas de menor densidad, lo amplía según la del móvil (×3 en uno actual, unos
  6000 px) y después lo reduce al hueco del icono, de unos 240 dp. Una reducción tan grande de un PNG
  deja los bordes dentados.
- El logo ocupa solo el 40 % central de esa imagen, y Android 12 además recorta el icono con un
  círculo, así que sale pequeño.
- **La versión vectorial ya existe**: `logosinfondosvg.xml`, con los mismos tres colores. Se ve nítida
  en cualquier pantalla.

**El fondo oscuro**

- El tema dice fondo blanco, y no hay versión para el modo oscuro. Que salga oscuro en el Xiaomi es
  el **modo oscuro forzado de MIUI/HyperOS**: oscurece por su cuenta las apps de tema claro. Eso
  mismo puede estar tocando los colores del resto de la app, que ya tiene su propio modo oscuro,
  elegido en *Cuenta*.
- El parabrisas, los faros y la P del logo son huecos transparentes, así que sobre fondo oscuro el
  azul apenas destaca.

**La librería de splash está incluida pero no se usa**

- `androidx.core:core-splashscreen` está en las dependencias, pero el tema usa los atributos de
  Android 12 directamente.
- No hay `installSplashScreen()`, y `postSplashScreenTheme` apunta al mismo tema que lo define.
- **En Android 7 a 11** (la app admite desde la 7.0) no sale el logo: solo el fondo del tema.
- `windowSplashScreenAnimationDuration` no hace nada con una imagen estática.

**En iOS, un detalle**

El logo está colocado en una posición fija, calculada para la pantalla de 440 puntos de ancho, y no
centrado con restricciones. En un iPhone de otro tamaño podría quedar algo descentrado.

## Objetivos

- **El mismo splash en Android que en iOS**: fondo blanco y el logo nítido, en el centro.
- **Blanco siempre**, también con el modo oscuro y con el oscuro forzado de Xiaomi.
- **En todas las versiones de Android que admite la app**, de la 7 a la 15.
- **Que el modo oscuro forzado de Xiaomi no toque la app**: el tema lo decide la propia app.

## No-objetivos

- **Un splash oscuro.** Decidido: blanco siempre, como en iOS.
- **Animar el logo.**
- **Cambiar el logo.**

## Historias de usuario

- Como usuario de Android, quiero que la app arranque con su logo nítido sobre blanco, como en un
  iPhone.
- Como usuario con el modo oscuro, quiero que el splash se vea igual de claro y que el logo destaque.

## Criterios de aceptación

**Android**

1. **Dado** un móvil con Android 12 o posterior en modo claro, **cuando** abro la app, **entonces**
   el splash tiene fondo blanco y el logo se ve nítido, sin bordes dentados, y de un tamaño parecido
   al de iOS.
2. **Dado** un móvil en modo oscuro, incluido un Xiaomi con el modo oscuro forzado para otras apps,
   **cuando** abro la app, **entonces** el splash sigue teniendo fondo blanco y el logo con sus
   colores.
3. **Dado** un móvil con Android 7 a 11, **cuando** abro la app, **entonces** sale el mismo splash:
   fondo blanco y el logo.
4. **Dado** un Xiaomi con el modo oscuro forzado, **cuando** uso la app, **entonces** se ve con el
   tema que elegí en *Cuenta*, igual que en cualquier otro móvil.

**iOS**

5. **Dado** un iPhone de cualquier tamaño, **cuando** abro la app, **entonces** el logo está
   centrado.
6. **Dado** que todo lo anterior se cumple, **cuando** abro la app en iOS, **entonces** el splash se
   ve igual que hoy.

## Resultado

Se cumple todo en Android estándar y en iOS, menos una cosa en el Xiaomi de Alberto (HyperOS). Allí
el logo ya se ve nítido, pero en modo oscuro **el fondo del splash sigue saliendo oscuro**
(criterio 2):

- la app pide que no se le aplique el oscuro forzado (`forceDarkAllowed=false`);
- no está en la lista de apps a las que HyperOS aplica el modo oscuro;
- y en un emulador con Android estándar, en modo oscuro, sale blanco.

Así que es HyperOS pintando la ventana de arranque por su cuenta. Alberto lo da por bueno así: no
queda mal. Si algún día molesta, lo que queda por probar es que el logo destaque también sobre
oscuro (un círculo blanco detrás, `windowSplashScreenIconBackgroundColor`), no pelear con el color.

## Preguntas abiertas — resueltas

Se cerraron en el diseño. El razonamiento completo está en [design.md](design.md).

1. **¿Qué se hace con las imágenes que dejan de usarse?** → **Se borran**:
   - `splash.png`;
   - `splash_image_sin_fondo.png`, `playstore.png` e `ic_launcher_playstore.png`, que no usaba
     nadie;
   - el vector `logosinfondosvg.xml`, que sustituye un vector cuadrado nuevo con el mismo dibujo.

   Si alguna hace falta para la ficha de Google Play, se guarda fuera del código de la app.

## Fuera de alcance

- **Tapar el instante vacío entre el splash y la primera pantalla.** Después del splash, la pantalla
  `SplashScreen` de Compose no dibuja nada mientras decide entre el login y el mapa. Dura unas
  centésimas y no se nota:
  - en modo claro es blanca, como el splash;
  - en modo oscuro es oscura, como la app, y el salto de blanco a oscuro existe de todos modos.
  
  Solo merecería la pena si un día decidir la primera pantalla tuviera que esperar, por ejemplo a la
  red. Con la librería de splash, mantenerlo en pantalla es entonces una línea
  (`setKeepOnScreenCondition`).

- El icono de la app en el escritorio del móvil.
- La pantalla de «Licencias» del ROADMAP.
