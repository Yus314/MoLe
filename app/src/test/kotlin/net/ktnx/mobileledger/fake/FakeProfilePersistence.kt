/*
 * Copyright © 2024 Damyan Ivanov.
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

package net.ktnx.mobileledger.fake

import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.domain.usecase.ProfilePersistence

/**
 * Fake implementation of ProfilePersistence for testing.
 */
class FakeProfilePersistence : ProfilePersistence {

    var shouldSucceed: Boolean = true
    var errorMessage: String = "Test error"

    private var _saveCallCount = 0
    val saveCallCount: Int get() = _saveCallCount

    private var _deleteCallCount = 0
    val deleteCallCount: Int get() = _deleteCallCount

    private var _lastSavedProfile: Profile? = null
    val lastSavedProfile: Profile? get() = _lastSavedProfile

    private var _lastDeletedProfileId: Long? = null
    val lastDeletedProfileId: Long? get() = _lastDeletedProfileId

    override suspend fun save(profile: Profile): Result<Unit> {
        _saveCallCount++
        _lastSavedProfile = profile

        return if (shouldSucceed) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(errorMessage))
        }
    }

    override suspend fun delete(profileId: Long): Result<Unit> {
        _deleteCallCount++
        _lastDeletedProfileId = profileId

        return if (shouldSucceed) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(errorMessage))
        }
    }

    fun reset() {
        shouldSucceed = true
        errorMessage = "Test error"
        _saveCallCount = 0
        _deleteCallCount = 0
        _lastSavedProfile = null
        _lastDeletedProfileId = null
    }
}
