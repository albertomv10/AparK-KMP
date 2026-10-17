package com.albertomedina.apark.presentation.navigation

/**
 * Cuánto dura el fundido al cambiar de pestaña, y por qué no dura lo mismo en los dos sitios.
 *
 * Android **reconstruye el MapView** al volver a su pestaña, así que el fundido además disimula ese
 * hueco; iOS no lo reconstruye —su estado vive en Swift— y alargarlo allí solo le quita
 * inmediatez, porque durante el cruce la pantalla que sale sigue pintada sobre el mapa.
 *
 * El velo de carga del mapa de Android es lo que arregla el hueco de verdad; esto solo acompaña.
 */
internal expect val tabSwitchFadeMillis: Int
