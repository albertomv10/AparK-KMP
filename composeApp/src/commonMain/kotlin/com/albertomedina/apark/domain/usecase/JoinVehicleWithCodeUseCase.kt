package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.model.JoinResult
import com.albertomedina.apark.domain.repository.InviteRepository

class JoinVehicleWithCodeUseCase(
    private val repository: InviteRepository
) {
    suspend operator fun invoke(code: String): Result<JoinResult> {
        return repository.joinWithCode(code)
    }
}
