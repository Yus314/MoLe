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

import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.domain.usecase.VersionDetector

class FakeVersionDetector : VersionDetector {
    var shouldSucceed: Boolean = true
    var versionToReturn: String = "1.32"
    var errorToThrow: Exception? = null
    var detectCallCount = 0
        private set
    var lastDetectedUrl: String? = null
        private set
    var lastDetectedProfile: Profile? = null
        private set

    override suspend fun detect(url: String, useAuth: Boolean, user: String?, password: String?): Result<String> {
        detectCallCount++
        lastDetectedUrl = url
        return if (shouldSucceed && errorToThrow == null) {
            Result.success(versionToReturn)
        } else {
            Result.failure(errorToThrow ?: Exception("Version detection failed"))
        }
    }

    override suspend fun detect(profile: Profile): Result<String> {
        lastDetectedProfile = profile
        return detect(
            url = profile.url,
            useAuth = profile.isAuthEnabled,
            user = profile.authentication?.user,
            password = profile.authentication?.password
        )
    }

    fun reset() {
        shouldSucceed = true
        versionToReturn = "1.32"
        errorToThrow = null
        detectCallCount = 0
        lastDetectedUrl = null
        lastDetectedProfile = null
    }
}
