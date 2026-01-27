/*
 * Use cases for startup preferences (theme/profile id).
 */
package net.ktnx.mobileledger.domain.usecase

import javax.inject.Inject
import net.ktnx.mobileledger.core.domain.repository.PreferencesRepository

interface GetStartupThemeUseCase {
    operator fun invoke(): Int
}

interface GetStartupProfileIdUseCase {
    operator fun invoke(): Long
}

interface SetStartupThemeUseCase {
    operator fun invoke(theme: Int)
}

interface SetStartupProfileIdUseCase {
    operator fun invoke(profileId: Long)
}

class GetStartupThemeUseCaseImpl @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : GetStartupThemeUseCase {
    override fun invoke(): Int = preferencesRepository.getStartupTheme()
}

class GetStartupProfileIdUseCaseImpl @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : GetStartupProfileIdUseCase {
    override fun invoke(): Long = preferencesRepository.getStartupProfileId()
}

class SetStartupThemeUseCaseImpl @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : SetStartupThemeUseCase {
    override fun invoke(theme: Int) = preferencesRepository.setStartupTheme(theme)
}

class SetStartupProfileIdUseCaseImpl @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : SetStartupProfileIdUseCase {
    override fun invoke(profileId: Long) = preferencesRepository.setStartupProfileId(profileId)
}
