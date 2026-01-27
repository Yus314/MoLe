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

package net.ktnx.mobileledger.core.domain.model

/**
 * バックグラウンド処理で発生するエラーの共通型
 *
 * すべてのエラーは以下の情報を持つ:
 * - message: ユーザーに表示するメッセージ
 * - isRetryable: リトライ可能かどうか
 */
sealed class SyncError {
    abstract val message: String
    abstract val isRetryable: Boolean

    /**
     * ネットワーク接続エラー
     * - WiFi/モバイルデータ未接続
     * - DNS解決失敗
     * - サーバー到達不可
     */
    data class NetworkError(
        override val message: String = "ネットワークに接続できません",
        val cause: Throwable? = null
    ) : SyncError() {
        override val isRetryable = true
    }

    /**
     * タイムアウトエラー
     * - 接続タイムアウト（30秒）
     * - 読み取りタイムアウト（30秒）
     */
    data class TimeoutError(
        override val message: String = "サーバーが応答しません",
        val timeoutMs: Long = 30_000
    ) : SyncError() {
        override val isRetryable = true
    }

    /**
     * 認証エラー
     * - HTTP 401 Unauthorized
     * - 無効なユーザー名/パスワード
     */
    data class AuthenticationError(
        override val message: String = "認証に失敗しました。ユーザー名とパスワードを確認してください",
        val httpCode: Int = 401
    ) : SyncError() {
        override val isRetryable = false
    }

    /**
     * サーバーエラー
     * - HTTP 4xx クライアントエラー（401以外）
     * - HTTP 5xx サーバーエラー
     */
    data class ServerError(
        override val message: String,
        val httpCode: Int,
        val serverMessage: String? = null
    ) : SyncError() {
        override val isRetryable = httpCode >= 500
    }

    /**
     * バリデーションエラー
     * - 取引送信時のサーバー側バリデーション失敗
     * - 勘定科目不存在、金額エラーなど
     */
    data class ValidationError(
        override val message: String,
        val field: String? = null,
        val details: List<String> = emptyList()
    ) : SyncError() {
        override val isRetryable = false
    }

    /**
     * パースエラー
     * - JSONパース失敗
     * - 予期しないレスポンス形式
     */
    data class ParseError(
        override val message: String = "データの解析に失敗しました",
        val cause: Throwable? = null
    ) : SyncError() {
        override val isRetryable = false
    }

    /**
     * APIバージョンエラー
     * - サポートされていないAPIバージョン
     */
    data class ApiVersionError(
        override val message: String = "サポートされていないAPIバージョンです",
        val detectedVersion: String? = null,
        val supportedVersions: List<String> = emptyList()
    ) : SyncError() {
        override val isRetryable = false
    }

    /**
     * キャンセル
     * - ユーザーによる明示的キャンセル
     * - バックグラウンド30秒超過による自動キャンセル
     */
    data object Cancelled : SyncError() {
        override val message = "処理がキャンセルされました"
        override val isRetryable = false
    }

    /**
     * 不明なエラー
     * - 予期しない例外
     */
    data class UnknownError(
        override val message: String = "予期しないエラーが発生しました",
        val cause: Throwable? = null
    ) : SyncError() {
        override val isRetryable = false
    }
}
