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

package net.ktnx.mobileledger.domain.exception

/**
 * バックグラウンド処理のエラー基底クラス
 *
 * すべてのバックグラウンドタスクで発生する例外をカテゴリ分けするためのsealed class。
 * ViewModelで適切なエラーハンドリングを行うために使用される。
 */
sealed class BackgroundTaskException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * ネットワーク関連エラー
     *
     * 接続タイムアウト、DNS解決失敗、接続拒否など
     */
    class NetworkException(
        message: String,
        cause: Throwable? = null
    ) : BackgroundTaskException(message, cause)

    /**
     * 認証エラー
     *
     * HTTPステータス401またはその他の認証失敗
     */
    class AuthenticationException(
        message: String
    ) : BackgroundTaskException(message)

    /**
     * サーバーエラー
     *
     * HTTPステータス4xx（クライアントエラー）または5xx（サーバーエラー）
     */
    class ServerException(
        val statusCode: Int,
        message: String
    ) : BackgroundTaskException(message)

    /**
     * パースエラー
     *
     * JSONパース失敗、不正なフォーマットなど
     */
    class ParseException(
        message: String,
        cause: Throwable? = null
    ) : BackgroundTaskException(message, cause)

    /**
     * ファイルI/Oエラー
     *
     * ファイル読み書き失敗、ストリームエラーなど
     */
    class FileException(
        message: String,
        cause: Throwable? = null
    ) : BackgroundTaskException(message, cause)
}
