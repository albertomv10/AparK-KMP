package com.albertomedina.apark.di

/**
 * A qué máquina apuntan los emuladores de Firebase, o `null` para hablar con el proyecto de verdad.
 *
 * Es un `var` que fija cada plataforma **antes** de `initKoin`, igual que `androidSettingsContext`:
 * no hace falta `expect/actual` porque los tres SDK de gitlive exponen `useEmulator` en común, así
 * que lo único que cambia de una plataforma a otra es de dónde sale esta cadena.
 *
 * El valor **es la dirección**, no un interruptor. Así «activarlo es explícito» se cumple solo
 * —sin dirección no hay emulador— y de paso cada entorno pone la suya sin que nadie tenga que
 * adivinarla: `10.0.2.2` desde el emulador de Android, `127.0.0.1` desde el simulador de iOS, y la
 * IP de la máquina desde un móvil físico, que así también puede usarlos.
 *
 * Quién lo rellena, y por qué una build de release no puede:
 * - Android: una función que existe **por variante**, como el proveedor de App Check. La de debug
 *   lee un `BuildConfig` que solo se genera para esa variante; la de release no hace nada.
 * - iOS: Swift, bajo `#if DEBUG`, que el compilador ni mira en release.
 */
var emulatorHost: String? = null

/**
 * Los puertos son los de `firebase.json`. Están aquí y no en la configuración de cada máquina
 * porque no son un gusto de nadie: si no coinciden con los del fichero, no conecta.
 */
internal object EmulatorPorts {
    const val AUTH = 9099
    const val FIRESTORE = 8080
    const val FUNCTIONS = 5001
}
