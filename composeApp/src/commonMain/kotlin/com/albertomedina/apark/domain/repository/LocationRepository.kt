package com.albertomedina.apark.domain.repository

import com.albertomedina.apark.domain.model.Vehicle

interface LocationRepository {
    /**
     * Dónde está quien mira, para llevar ahí la cámara del mapa (spec 017). Rápido antes que exacto:
     * la última ubicación conocida si la hay, que es instantánea; si no, una fresca con un límite de
     * 5 s. `null` si no hay forma de saberlo, sin permiso incluido: el mapa simplemente no se mueve.
     */
    suspend fun getApproximateLocation(): Vehicle.LocationModel?

    suspend fun getCurrentLocation(): Vehicle.LocationModel

}
