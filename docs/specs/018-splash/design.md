# Design: el splash de Android, como el de iOS

- **Spec**: [spec.md](spec.md)
- **Estado**: Aprobado

## Enfoque

En Android, el splash se monta con **la librería `core-splashscreen`**, que ya estaba en las
dependencias. Así es el mismo de Android 7 a 15. Lleva el fondo blanco, sin versión oscura, y el logo
como un vector cuadrado con el margen que pide Android. **La app se excluye del modo oscuro
forzado** de MIUI/HyperOS, porque ya tiene el suyo. En iOS, el logo se centra con restricciones.

## Lo que dijo el código antes de decidir nada

1. **El tema actual mezclaba dos cosas.** `Theme.AparK`:
   - usaba directamente los atributos `android:windowSplashScreen*`, que solo existen desde Android
     12;
   - usaba también `postSplashScreenTheme`, que es de la librería, apuntando a sí mismo;
   - y `MainActivity` no llamaba a `installSplashScreen()`.
2. **El icono era un PNG de 2000×2000** con el logo en el 40 % central. Android lo trataba como
   pensado para las pantallas de menor densidad, lo ampliaba y luego lo reducía mucho, y eso dejaba
   los bordes dentados. El vector `logosinfondosvg.xml`, que no usaba nadie, tiene el mismo dibujo
   en tres trazados, pero mide 545×597: no es cuadrado.
3. **Tres imágenes más no las referenciaba nadie.** El icono de la app usa `ic_launcher_foreground`
   e `ic_launcher_background`, que no se tocan.
4. **En iOS el logo tenía una posición fija** en (130, 388), que solo es el centro en una pantalla de
   440×956, y no tenía ninguna restricción.

## Android

**Dos temas:**
- **`Theme.AparK.Starting`** hereda de `Theme.SplashScreen`. Lleva el fondo blanco (`@color/splash_background`,
  sin `values-night`), el icono `@drawable/splash_logo`, y `postSplashScreenTheme` apuntando a
  `Theme.AparK`.
- **`Theme.AparK`** es el tema de la app.

Los dos llevan `android:forceDarkAllowed=false`: el modo oscuro lo decide la app, desde *Cuenta*, y
no MIUI. La actividad arranca con `Theme.AparK.Starting`, y la aplicación usa `Theme.AparK`.

**En `MainActivity`**, `installSplashScreen()` va antes de `super.onCreate()`, que es lo que pide la
librería.

**`splash_logo.xml`** es un vector de 288×288 dp, el lienzo del icono sin fondo:
- su viewport cuadrado mide unas 1148 unidades, y los tres trazados del logo van centrados dentro
  de un `<group>`;
- el logo ocupa así unos 150 dp de alto, dentro del círculo central de 192 dp que Android no
  recorta, y parecido a los ~160 pt de iOS.

Un `<inset>` sobre el vector original no sirve: no es cuadrado y se deformaría.

## iOS

En `Launch Screen.storyboard`, el logo pasa de una posición fija a cuatro restricciones:
- ancho y alto de 180;
- centrado horizontal y vertical en la vista.

El fondo y las imágenes no cambian.

## Riesgos

- **Que MIUI no haga caso a `forceDarkAllowed`** en la ventana del splash. Lo dirá la prueba en el
  Xiaomi.
- **El tamaño del logo** frente al de iOS. Se ajusta con el viewport.
