/*
 * Copyright © 2026 Damyan Ivanov.
 * This file is part of MoLe.
 * MoLe is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * MoLe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoLe. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.di.IoDispatcher
import net.ktnx.mobileledger.utils.Logger

/**
 * DatabaseInitializer の実装
 *
 * ProfileRepository を使用してデータベース初期化を行う。
 * Room データベースは ProfileRepository を通じてアクセスされるため、
 * 最初のクエリでデータベースが初期化される。
 */
@Singleton
class DatabaseInitializerImpl @Inject constructor(
    private val profileRepository: ProfileRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : DatabaseInitializer {

    private var _isInitialized: Boolean = false

    override val isInitialized: Boolean
        get() = _isInitialized

    override suspend fun initialize(): Result<Boolean> = withContext(ioDispatcher) {
        try {
            Logger.debug(TAG, "Starting database initialization")

            // ProfileRepository を通じてデータベースにアクセス
            // これにより Room データベースが初期化される
            val profileCount = profileRepository.getProfileCount()
            val hasProfiles = profileCount > 0

            Logger.debug(TAG, "Database initialization complete. Profile count: $profileCount")

            _isInitialized = true
            Result.success(hasProfiles)
        } catch (e: Exception) {
            Logger.warn(TAG, "Database initialization failed", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "DatabaseInitializerImpl"
    }
}
