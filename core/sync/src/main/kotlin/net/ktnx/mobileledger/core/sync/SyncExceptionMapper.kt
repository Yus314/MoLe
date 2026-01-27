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

package net.ktnx.mobileledger.core.sync

import android.os.OperationCanceledException
import java.io.IOException
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.text.ParseException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import net.ktnx.mobileledger.core.domain.model.SyncError
import net.ktnx.mobileledger.core.domain.model.SyncException
import net.ktnx.mobileledger.core.network.NetworkAuthenticationException
import net.ktnx.mobileledger.core.network.NetworkHttpException
import net.ktnx.mobileledger.core.network.json.ApiNotSupportedException

/**
 * Maps various exceptions to SyncException with appropriate SyncError.
 */
@Singleton
class SyncExceptionMapper @Inject constructor() {

    /**
     * Maps a throwable to a SyncException with appropriate error type.
     *
     * @param e The throwable to map
     * @return SyncException containing the mapped error
     */
    fun mapToSyncException(e: Throwable): SyncException {
        val syncError = when (e) {
            is SyncException -> return e

            is SocketTimeoutException -> SyncError.TimeoutError(message = "サーバーが応答しません")

            is MalformedURLException -> SyncError.NetworkError(message = "無効なサーバーURLです", cause = e)

            is NetworkAuthenticationException ->
                SyncError.AuthenticationError(message = "認証に失敗しました", httpCode = 401)

            is NetworkHttpException ->
                SyncError.ServerError(message = e.message ?: "サーバーエラー", httpCode = e.statusCode)

            is IOException -> SyncError.NetworkError(message = e.localizedMessage ?: "ネットワークエラー", cause = e)

            is SerializationException -> SyncError.ParseError(message = "JSONパースエラー", cause = e)

            is ParseException -> SyncError.ParseError(message = "データ解析エラー", cause = e)

            is OperationCanceledException -> SyncError.Cancelled

            is ApiNotSupportedException -> SyncError.ApiVersionError(message = "サポートされていないAPIバージョンです")

            is kotlinx.coroutines.CancellationException -> SyncError.Cancelled

            else -> SyncError.UnknownError(
                message = e.localizedMessage ?: "予期しないエラーが発生しました",
                cause = e
            )
        }
        return SyncException(syncError)
    }
}
