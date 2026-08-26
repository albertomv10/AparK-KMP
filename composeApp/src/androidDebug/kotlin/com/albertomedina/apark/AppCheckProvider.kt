package com.albertomedina.apark

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Play Integrity no funciona en un emulador ni en una build sin firmar con la clave de Play, así que
 * en desarrollo se usa el proveedor de depuración. Imprime en el log un token que hay que registrar
 * a mano en la consola: **uno por instalación**.
 *
 * Qué proveedor se usa lo decide **el source set de la variante**, no un `if` en tiempo de ejecución.
 * Tiene que ser así: esta clase entra por `debugImplementation`, de modo que en una build de release
 * no existe, y una referencia a ella dentro de un `if (BuildConfig.DEBUG)` ni siquiera compilaría.
 * Separarlo por variante es además más seguro — no hay despiste en una condición que pueda colar el
 * proveedor de depuración en producción.
 */
fun appCheckProviderFactory(): AppCheckProviderFactory =
    DebugAppCheckProviderFactory.getInstance()
