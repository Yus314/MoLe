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
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.NetworkUtil

/**
 * VersionDetector の実装
 *
 * HttpURLConnection を使用して hledger-web のバージョンを検出する。
 * 既存の VersionDetectionThread のロジックを suspend 関数として提供。
 */
@Singleton
class VersionDetectorImpl @Inject constructor() : VersionDetector {

    override suspend fun detect(url: String, useAuth: Boolean, user: String?, password: String?): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Detecting version for URL: $url")

                ensureActive()

                val http = NetworkUtil.prepareConnection(url, "version", useAuth)
                try {
                    ensureActive()

                    when (val responseCode = http.responseCode) {
                        HttpURLConnection.HTTP_OK -> {
                            ensureActive()
                            val version = parseVersionFromStream(http)
                            if (version != null) {
                                Logger.debug(TAG, "Detected version: $version")
                                Result.success(version)
                            } else {
                                Logger.warn(TAG, "Could not parse version from response")
                                Result.failure(Exception("Could not parse version from response"))
                            }
                        }

                        HttpURLConnection.HTTP_NOT_FOUND -> {
                            // 404 means old hledger-web version (pre-1.19)
                            Logger.debug(TAG, "Version endpoint not found, assuming pre-1.19")
                            Result.success("pre-1.19")
                        }

                        else -> {
                            Logger.warn(TAG, "HTTP error: [$responseCode] ${http.responseMessage}")
                            Result.failure(Exception("HTTP error: $responseCode ${http.responseMessage}"))
                        }
                    }
                } finally {
                    http.disconnect()
                }
            } catch (e: Exception) {
                Logger.warn(TAG, "Version detection failed", e)
                Result.failure(e)
            }
        }

    private fun parseVersionFromStream(http: HttpURLConnection): String? {
        return try {
            val reader = BufferedReader(InputStreamReader(http.inputStream))
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
                    Logger.warn(TAG, "Version string format not recognized: $versionLine")
                    null
                }
            }
        } catch (e: Exception) {
            Logger.warn(TAG, "Error parsing version", e)
            null
        }
    }

    companion object {
        private const val TAG = "VersionDetectorImpl"

        // Pattern for quoted version: "1.32" or "1.32.1"
        private val VERSION_PATTERN = Pattern.compile("^\"(\\d+)\\.(\\d+)(?:\\.(\\d+))?\"$")

        // Pattern for unquoted version: 1.32 or 1.32.1
        private val UNQUOTED_VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?$")
    }
}
