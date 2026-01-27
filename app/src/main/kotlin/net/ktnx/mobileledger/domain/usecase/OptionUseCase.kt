/*
 * Use cases for option access (currently last sync timestamp).
 */
package net.ktnx.mobileledger.domain.usecase

import javax.inject.Inject
import net.ktnx.mobileledger.core.domain.repository.OptionRepository

interface GetLastSyncTimestampUseCase {
    suspend operator fun invoke(profileId: Long): Result<Long?>
}

interface SetLastSyncTimestampUseCase {
    suspend operator fun invoke(profileId: Long, timestamp: Long): Result<Unit>
}

class GetLastSyncTimestampUseCaseImpl @Inject constructor(
    private val optionRepository: OptionRepository
) : GetLastSyncTimestampUseCase {
    override suspend fun invoke(profileId: Long): Result<Long?> = runCatching {
        optionRepository.getLastSyncTimestamp(profileId)
    }
}

class SetLastSyncTimestampUseCaseImpl @Inject constructor(
    private val optionRepository: OptionRepository
) : SetLastSyncTimestampUseCase {
    override suspend fun invoke(profileId: Long, timestamp: Long): Result<Unit> = runCatching {
        optionRepository.setLastSyncTimestamp(profileId, timestamp)
    }
}
