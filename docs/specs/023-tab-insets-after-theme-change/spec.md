# Spec: Que nada se mueva al cambiar de tema en iPhone

- **ID**: 023-tab-insets-after-theme-change
- **Estado**: Implementada — diseño en [design.md](design.md)
- **Fecha**: 2026-11-26

## Problema / Por qué

En iPhone, con la app abierta en el mapa, cambiar la apariencia del sistema a oscuro (o a claro)
**sube el carrusel de tarjetas, los botones flotantes y el logo de Google 34 pt**, lo que mide el
indicador de inicio, y ahí se quedan. La barra de pestañas no se mueve. Se vio el 26-11-2026 en un
iPhone 17 Pro simulado (iOS 26.1), con la debug contra los emuladores.

**Reproducido**, midiendo el desplazamiento en las capturas (3 px por punto):

| Build | Al pasar a oscuro con el mapa abierto |
|---|---|
| `feature/vehicle-card-layout` (spec 022) | la tarjeta y el logo suben 102 px = **34 pt** |
| `main` antes de la 022 (0d77a5f) | lo mismo, **34 pt**: no lo trajo la 022 |

Y además:

- **No hace falta que cambie el tema de la app.** Con la app fijada en «Claro», cambiar el sistema
  también la sube 34 pt.
- **Cambiar el tema desde Cuenta lo provoca en Cuenta**: el contenido gana un hueco de 34 pt al
  final, que se ve al hacer scroll. El mapa no, porque en ese momento no está en pantalla.
- **Se arregla sola al salir de la pantalla**: ir a otra pestaña y volver al mapa lo devuelve a su
  sitio, igual que relanzar la app.
- **El tamaño de letra del sistema no lo provoca**: Compose en iOS no reacciona a ese cambio en
  caliente.

### Por qué pasa

Es la suma de cuatro piezas, tres de ellas de las librerías:

1. **iOS cambia la apariencia y Compose recompone la app entera.** `LocalSystemTheme` es un
   `staticCompositionLocalOf`, y al cambiar un local estático se vuelve a ejecutar todo lo que cuelga
   de él, lo lea o no. El tema de la app (`LocalIsDarkTheme`, también estático, y el `colorScheme`
   de `MaterialTheme`) hace lo mismo cuando se cambia desde Cuenta.
2. **En iOS, `WindowInsets.systemBars` devuelve un objeto nuevo cada vez que se lee**, sin `equals`
   (`PlatformInsets.toWindowInsets()` en `WindowInsets.skiko.kt`). Mismos valores, distinta
   identidad. En Android devuelve siempre el mismo objeto, y por eso allí no pasa.
3. **El `Scaffold` de Material 3 guarda sus márgenes con `remember(contentWindowInsets)`.** Como el
   objeto es nuevo, la clave cambia y crea unos márgenes nuevos **con el inset completo**, sin
   descontar lo que ya consumió quien está por encima. Descontarlo es trabajo de un bloque que le
   pasa a `onConsumedWindowInsetsChanged`.
4. **Ese bloque no se vuelve a ejecutar.** `ConsumedInsetsModifierNode.update()` guarda el bloque
   nuevo y nada más (`foundation-layout` 1.10.1). Solo lo ejecuta al engancharse a la pantalla o
   cuando cambia lo consumido por arriba, y aquí no cambia nada.

Resultado: los `Scaffold` que viven **dentro de `TabShell`** —el único sitio que consume insets,
con `consumeWindowInsets(tabBarPadding)`— vuelven a reservar los 34 pt del indicador de inicio que
el shell ya había descontado. Es justo lo que avisaba AGENTS.md («los márgenes se cuentan dos
veces»): la regla está bien puesta, pero el mecanismo en el que se apoya deja de funcionar tras una
recomposición completa. Los `Scaffold` de fuera del shell (login, registro…) no consumen nada, así
que no les afecta. Los modificadores de padding (`imePadding`, `navigationBarsPadding`) tampoco:
su nodo sí se recalcula cuando recibe insets nuevos.

**Demostrado con un experimento** sobre `main`, sin commitear: pasarle al `Scaffold` del mapa los
mismos insets de siempre, pero guardados con `remember` para que no cambie el objeto, elimina el
salto en dos cambios seguidos de apariencia. Mismo valor, distinta identidad: esa es la causa.

**Arreglado aguas arriba en Compose Multiplatform 1.12.0** (aparece en la 1.12.0-beta01; la última
estable es la 1.12.2): `update()` vuelve a ejecutar el bloque. En 1.10.x y 1.11.x no. La app declara
CMP 1.9.3, pero en iOS enlaza `ui`, `runtime` y `foundation-layout` **1.10.1** —los arrastra alguna
dependencia— y `material3` 1.9.0. No es CMP-9390 («iOS ignora `consumeWindowInsets`»): aquel es un
margen de más permanente desde la 1.10.0-rc01; este solo aparece tras recomponer todo.

## Objetivos

- **Que cambiar la apariencia, del sistema o de la app, no mueva nada** en ninguna pantalla de las
  pestañas.
- **Que no dependa de la versión de Compose.** El contrato tiene que aguantar en 1.10, y seguir
  siendo correcto si se sube a 1.12.
- **Que la regla quede donde la vea quien escriba la próxima pantalla.**

## No-objetivos

- **Cambiar dónde terminan las pantallas.** Al abrir la app, todo queda donde está hoy, con barra
  de pestañas y sin ella.
- **Subir Compose Multiplatform.** Es el arreglo de fondo y tiene su propio coste; ver las
  preguntas abiertas.
- **Dibujar por detrás del indicador de inicio** (edge-to-edge en las pantallas de dentro).

## Historias de usuario

- Como usuario de iPhone con el modo oscuro automático, quiero que el carrusel siga en su sitio
  cuando el móvil cambia de tema al anochecer, sin tener que salir del mapa.
- Como usuario que cambia el tema desde Cuenta, quiero que la pantalla no gane un hueco al final.

## Criterios de aceptación

1. **Dado** el mapa abierto en iPhone, **cuando** cambio la apariencia del sistema de claro a oscuro
   y de vuelta, **entonces** el carrusel, los botones flotantes y el logo de Google no se mueven.
   Con el tema de la app en Automático, Claro y Oscuro.
2. **Dado** Cuenta abierta en iPhone, **cuando** cambio el tema desde la propia pantalla,
   **entonces** el contenido no gana un hueco al final.
3. **Dado** cualquier otra pantalla de las pestañas abierta en iPhone —*Vehículos* con su botón
   flotante, el detalle, añadir, editar, cambiar email, cambiar contraseña, borrar cuenta—,
   **cuando** cambia la apariencia, **entonces** nada se desplaza.
4. **Dado** que abro la app, **cuando** recorro esas mismas pantallas, **entonces** todo está donde
   está hoy: nada tapado por la barra de pestañas ni por el indicador de inicio, y sin huecos.
5. **Dado** un formulario con el teclado abierto, **cuando** escribo, **entonces** el contenido sigue
   subiendo con el teclado como hoy.
6. **Dado** Android, **cuando** se da cualquiera de los casos anteriores, **entonces** se comporta
   igual que hoy.

## Preguntas abiertas — resueltas

1. **¿Arreglo propio o subir Compose?** → **Propio ahora; subir Compose, en una spec aparte.**
   - **Que el shell sea el dueño del margen inferior**, y decirlo explícitamente. Los `Scaffold` de
     dentro de `TabShell` declaran `contentWindowInsets` **sin la parte de abajo**
     (`ScaffoldDefaults.contentWindowInsets.only(Top + Horizontal)`). Así ya no hay nada que
     descontar: aunque el `Scaffold` rehaga sus márgenes, salen bien. No depende de la versión, y
     sigue siendo correcto después de subir. Coste: tocar los siete `Scaffold` del shell (nueve
     pantallas), y que una pantalla nueva que lo olvide vuelve a fallar, solo en iPhone y solo tras
     cambiar el tema.
   - **El de fondo, subir Compose Multiplatform a 1.12.x**, arregla la causa en la librería, para
     estas pantallas y para cualquier otra. Coste: dos versiones menores de golpe (1.9.3 declarada,
     1.10.1 real), y AGENTS.md ya avisa de que el ciclo de vida está atado a Compose
     (`lifecycle-viewmodel-navigation3` 2.10.0 ↔ `compose.runtime` 1.9.3), y Navigation 3 sigue en
     alpha06. Más Compottie, maps-compose y los recursos. Es una spec entera, con su verificación en
     las dos plataformas, y no está probado que 1.12 arregle este síntoma de punta a punta: solo que
     la pieza 4 cambió.
2. **¿Constante o envoltorio?** → **Una constante**, `TabShellDefaults.contentWindowInsets`, que cada
   pantalla pasa a su `Scaffold`. Es una línea por pantalla y deja a la vista la API de Material; un
   `TabScaffold` se heredaría al copiar una pantalla, pero esconde un parámetro. La regla se apunta en
   AGENTS.md, en el lugar de la que hay ahora, y en NAVIGATION.md.
3. **¿Se avisa a JetBrains?** → **No**: ya está arreglado en 1.12.

## Fuera de alcance

- **Subir Compose Multiplatform**, salvo que se decida en la pregunta 1.
- **Averiguar qué dependencia sube `ui` y `runtime` a 1.10.1.** Interesa para la spec de subir
  Compose, no para esta.
