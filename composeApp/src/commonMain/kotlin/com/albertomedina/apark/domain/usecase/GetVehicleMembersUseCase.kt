package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.model.VehicleMember
import com.albertomedina.apark.domain.repository.MemberRepository

class GetVehicleMembersUseCase(
    private val memberRepository: MemberRepository
) {
    suspend operator fun invoke(vehicleId: String): Result<List<VehicleMember>> =
        memberRepository.getMembers(vehicleId)
}
