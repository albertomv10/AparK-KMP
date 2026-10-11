# Spec: pestañas que navegan de verdad

- **ID**: 009-tab-navigation
- **Estado**: Borrador
- **Fecha**: 2026-10-11

## Problema / Por qué

La barra inferior tiene tres pestañas —*Mapa*, *Mis Coches*, *Perfil*— y **ninguna navega a ningún
sitio**. Al tocarlas cambia un `remember` local y se mueve el subrayado; nada más.

Y hay un problema estructural debajo: **la barra vive dentro del `Scaffold` de `HomeScreen`**
(`HomeScreen.kt:200`). No es que las pestañas no funcionen, es que la barra solo existe en una
pantalla. Mientras siga ahí, no hay **dónde colgar** las otras dos.

Eso bloquea toda la fase 1 del [roadmap](../../ROADMAP.md): perfil, ajustes y eliminar cuenta no
tienen sitio al que ir. Y bloquea más de lo que parece — el ajuste de tema que ya quedó cableado
sobre DataStore **no tiene pantalla que lo escriba**, así que hoy es infraestructura sin interfaz.

## Objetivos

- Que las tres pestañas **lleven a pantallas distintas**.
- Que la barra viva **por encima** de las pantallas y no dentro de una de ellas.
- Que cada pestaña **recuerde dónde estabas**: entrar al detalle de un vehículo, cambiar de pestaña
  y volver debería devolverte al detalle, no al principio.
- Dejar **sitio preparado** para lo que viene: el perfil y los ajustes de la fase 1, y la gestión de
  vehículos que necesitarán las fases 2 y 3.
- Que los textos de las pestañas salgan de recursos y no estén incrustados en castellano.

## No-objetivos

- **Construir el contenido del perfil o de los ajustes.** Eso es la fase 1.2 y 1.5, con sus propias
  specs. Aquí solo se crea el hueco.
- Rediseñar la Home, que funciona.
- Tocar el flujo de autenticación.

## Historias de usuario

- Como usuario, quiero moverme entre el mapa, mis vehículos y mi cuenta sin que la app me devuelva
  siempre al mismo sitio.
- Como usuario, quiero que al volver a una pestaña siga donde la dejé, porque perder el sitio al
  cambiar de pestaña se siente como un fallo.
- Como desarrollador, quiero que una pantalla nueva tenga un lugar evidente donde vivir.

## Criterios de aceptación

1. **Dado** que estoy en el mapa, **cuando** toco *Mis Coches*, **entonces** veo una pantalla
   distinta, y la barra sigue visible con esa pestaña marcada.
2. **Dado** que entro al detalle de un vehículo desde *Mis Coches*, **cuando** cambio a *Perfil* y
   vuelvo, **entonces** sigo en el detalle.
3. **Dado** que estoy en una pantalla sin pestañas —login, splash, añadir vehículo—, **cuando** se
   muestra, **entonces** la barra **no** aparece.
4. **Dado** que cierro sesión desde donde sea, **cuando** ocurre, **entonces** acabo en el login sin
   barra y sin que quede nada de la sesión anterior en ninguna pestaña.
5. **Dado** que giro el dispositivo o la app se recrea, **cuando** vuelve, **entonces** sigo en la
   misma pestaña y en la misma pantalla dentro de ella.
6. **Dado** cualquier idioma de los tres soportados, **cuando** miro la barra, **entonces** los
   nombres están traducidos.

## Preguntas abiertas

Para cerrar en `design.md`.

1. **¿Una pila por pestaña, o una sola compartida?** El criterio 2 pide que cada pestaña recuerde su
   sitio, y eso normalmente significa **una pila de navegación por pestaña**. Navigation 3 está en
   alpha y hay que ver qué soporta de serie y qué habría que montar a mano — es la decisión técnica
   gorda de esta spec.
2. **¿Dónde vive la barra?** Lo natural es un *shell* que envuelva al `NavDisplay`, pero hay que
   decidir cómo sabe ese shell si la pantalla actual lleva pestañas o no (criterio 3): ¿lo declara
   cada destino, o lo decide el shell por una lista?
3. **¿Qué hace el botón atrás en una pestaña que no es la primera?** ¿Vuelve a *Mapa* o cierra la
   app? Android y iOS tienen expectativas distintas aquí.
4. **¿Qué es *Mis Coches*, exactamente?** Hoy el carrusel de la Home ya lista los vehículos, así que
   la pestaña corre el riesgo de duplicarla. La lectura que da sentido a las tres es que *Mapa*
   responde a **dónde está** y *Mis Coches* a **cómo está** — y que sea por tanto la puerta de
   entrada a lo que traen las fases 2 y 3: datos del vehículo, miembros, incidencias, ITV. Conviene
   decidirlo ahora, porque cambia si la pestaña es una lista o algo más.
5. **¿Qué se pone de momento en *Perfil*?** Un marcador de posición, o adelantar el cierre de sesión
   —que hoy vive en un menú de tres puntos sobre el mapa— para que la pestaña no nazca vacía.
6. **¿Sobrevive el menú de tres puntos de la Home?** Si *Perfil* existe, tener ahí el cierre de
   sesión pasa a ser redundante.

## Fuera de alcance

- Las pantallas de perfil y ajustes con su contenido real (fases 1.2 y 1.5).
- Eliminar cuenta (fase 1.4), aunque viva en *Perfil* cuando llegue.
- Cualquier cambio en el modelo de datos o en las reglas.

## Notas

Esta es la primera spec que toca **solo presentación**: ni Firestore, ni reglas, ni funciones. A
cambio, es la que más condiciona lo que venga después, porque define dónde aterriza cada pantalla
nueva de aquí en adelante.
