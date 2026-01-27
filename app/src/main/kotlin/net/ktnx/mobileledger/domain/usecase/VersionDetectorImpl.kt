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

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import net.ktnx.mobileledger.core.common.di.IoDispatcher
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.model.TemporaryAuthData
import net.ktnx.mobileledger.core.network.HledgerClient
import net.ktnx.mobileledger.core.network.NetworkNotFoundException

/**
 * VersionDetector の実装
 *
 * Ktor HttpClient を使用して hledger-web のバージョンを検出する。
 */
@Singleton
class VersionDetectorImpl @Inject constructor(
    private val hledgerClient: HledgerClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : VersionDetector {

    override suspend fun detect(url: String, useAuth: Boolean, user: String?, password: String?): Result<String> =
        withContext(ioDispatcher) {
            try {
                logcat { "Detecting version for URL: $url" }

                // Create temporary auth data if authentication is required
                val temporaryAuth = if (useAuth && !user.isNullOrEmpty()) {
                    TemporaryAuthData(
                        url = url,
                        useAuthentication = true,
                        authUser = user,
                        authPassword = password ?: ""
                    )
                } else {
                    null
                }

                // Create a minimal profile for the request
                val tempProfile = createTempProfile(url)

                val result = hledgerClient.get(tempProfile, "version", temporaryAuth)

                result.fold(
                    onSuccess = { inputStream ->
                        val version = parseVersionFromStream(inputStream)
                        if (version != null) {
                            logcat { "Detected version: $version" }
                            Result.success(version)
                        } else {
                            logcat(LogPriority.WARN) { "Could not parse version from response" }
                            Result.failure(Exception("Could not parse version from response"))
                        }
                    },
                    onFailure = { error ->
                        when (error) {
                            is NetworkNotFoundException -> {
                                // 404 means old hledger-web version (pre-1.19)
                                logcat { "Version endpoint not found, assuming pre-1.19" }
                                Result.success("pre-1.19")
                            }

                            else -> {
                                logcat(LogPriority.WARN) { "Version detection failed: ${error.asLog()}" }
                                Result.failure(error)
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                logcat(LogPriority.WARN) { "Version detection failed: ${e.asLog()}" }
                Result.failure(e)
            }
        }

    private fun createTempProfile(url: String): Profile = Profile(
        id = null,
        name = "Temp",
        uuid = UUID.randomUUID().toString(),
        url = url,
        authentication = null
    )

    private fun parseVersionFromStream(inputStream: InputStream): String? {
        return try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val versionLine = reader.readLine() ?: return null

            // Parse version string like "1.32" or "\"1.32\""
            val matcher = VERSION_PATTERN.matcher(versionLine)
            if (matcher.find()) {
                val major = matcher.group(1)
                val minor = matcher.group(2)
                "$major.$minor"
            } else {
                // Try unquoted format
                val unquotedMatcher = UNQUOTED_VERSION_PATTERN.matcher(versionLine)
                if (unquotedMatcher.find()) {
                    val major = unquotedMatcher.group(1)
                    val minor = unquotedMatcher.group(2)
                    "$major.$minor"
                } else {
                    logcat(LogPriority.WARN) { "Version string format not recognized: $versionLine" }
                    null
                }
            }
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "Error parsing version: ${e.asLog()}" }
            null
        }
    }

    companion object {
        // Pattern for quoted version: "1.32" or "1.32.1"
        private val VERSION_PATTERN = Pattern.compile("^\"(\\d+)\\.(\\d+)(?:\\.(\\d+))?\"$")

        // Pattern for unquoted version: 1.32 or 1.32.1
        private val UNQUOTED_VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?$")
    }
}
