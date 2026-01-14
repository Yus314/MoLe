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

package net.ktnx.mobileledger.fake

import net.ktnx.mobileledger.domain.usecase.DatabaseInitializer

class FakeDatabaseInitializer : DatabaseInitializer {
    var shouldSucceed: Boolean = true
    var hasProfiles: Boolean = false
    var errorToThrow: Exception? = null
    var initializeCallCount = 0
        private set
    private var _isInitialized: Boolean = false

    override val isInitialized: Boolean
        get() = _isInitialized

    override suspend fun initialize(): Result<Boolean> {
        initializeCallCount++
        return if (shouldSucceed && errorToThrow == null) {
            _isInitialized = true
            Result.success(hasProfiles)
        } else {
            Result.failure(errorToThrow ?: Exception("Database initialization failed"))
        }
    }

    fun reset() {
        shouldSucceed = true
        hasProfiles = false
        errorToThrow = null
        initializeCallCount = 0
        _isInitialized = false
    }
}
