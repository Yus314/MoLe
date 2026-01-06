/*
 * Copyright © 2020 Damyan Ivanov.
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

package net.ktnx.mobileledger.utils

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import net.ktnx.mobileledger.db.Profile

object NetworkUtil {
    private const val THIRTY_SECONDS = 30000

    @JvmStatic
    @Throws(IOException::class)
    fun prepareConnection(profile: Profile, path: String): HttpURLConnection = prepareConnection(profile.url, path, profile.isAuthEnabled())

    @JvmStatic
    @Throws(IOException::class)
    fun prepareConnection(url: String, path: String, authEnabled: Boolean): HttpURLConnection {
        var connectURL = url
        if (!connectURL.endsWith("/")) {
            connectURL += "/"
        }
        connectURL += path
        Logger.debug("network", "Connecting to $connectURL")
        val http = URL(connectURL).openConnection() as HttpURLConnection
        http.allowUserInteraction = true
        http.setRequestProperty("Accept-Charset", "UTF-8")
        http.instanceFollowRedirects = false
        http.useCaches = false
        http.readTimeout = THIRTY_SECONDS
        http.connectTimeout = THIRTY_SECONDS

        return http
    }
}
