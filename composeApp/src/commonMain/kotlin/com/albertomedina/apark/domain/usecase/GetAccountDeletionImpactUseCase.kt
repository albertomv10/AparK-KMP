package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.repository.AuthRepository
import kotlinx.coroutines.flow.first

/** Lo que pasará con cada vehículo si la persona borra su cuenta, por nombre. */
data class AccountDeletionImpact(
    /** Es la dueña y no hay nadie más: desaparecen. */
    val deleted: List<String> = emptyList(),
    /** Es la dueña y hay más miembros: pasan al que lleva más tiempo. */
    val handedOver: List<String> = emptyList(),
    /** Son de otra persona: deja de ser miembro. */
    val left: List<String> = emptyList()
)

/**
 * El mismo reparto que hace la callable `deleteAccount` (`planVehicle`), para enseñarlo **antes** de
 * confirmar. Si alguna vez divergen, manda el servidor; esto solo describe.
 */
fun accountDeletionImpact(vehicles: List<Vehicle>, uid: String): AccountDeletionImpact {
    val deleted = mutableListOf<String>()
    val handedOver = mutableListOf<String>()
    val left = mutableListOf<String>()
    for (vehicle in vehicles) {
        val others = vehicle.memberIds.filter { it != uid }
        when {
            vehicle.ownerId != uid -> left += vehicle.name
            others.isEmpty() -> deleted += vehicle.name
            else -> handedOver += vehicle.name
        }
    }
    return AccountDeletionImpact(deleted, handedOver, left)
}

class GetAccountDeletionImpactUseCase(
    private val authRepository: AuthRepository,
    private val getVehicleListUseCase: GetVehicleListUseCase
) {
    suspend operator fun invoke(): AccountDeletionImpact {
        val uid = authRepository.getCurrentUser()?.uid ?: return AccountDeletionImpact()
        return accountDeletionImpact(getVehicleListUseCase(uid).first(), uid)
    }
}
