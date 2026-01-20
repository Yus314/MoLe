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

package net.ktnx.mobileledger.domain.model

/**
 * ファイルI/O操作に関するエラー
 *
 * バックアップ/リストア、設定ファイル読み書きなどで発生するエラーを表す。
 * 基本的にリトライ不可。
 */
sealed class FileError : AppError() {
    override val isRetryable: Boolean = false

    /**
     * ファイル読み込みエラー
     *
     * - ファイルオープン失敗
     * - 読み込み中のI/Oエラー
     */
    data class ReadFailed(
        override val message: String = "ファイルの読み込みに失敗しました",
        override val cause: Throwable? = null,
        val path: String? = null
    ) : FileError()

    /**
     * ファイル書き込みエラー
     *
     * - ファイル作成失敗
     * - 書き込み中のI/Oエラー
     * - ディスク容量不足
     */
    data class WriteFailed(
        override val message: String = "ファイルの書き込みに失敗しました",
        override val cause: Throwable? = null,
        val path: String? = null
    ) : FileError()

    /**
     * ファイル未検出エラー
     *
     * - 指定されたパスにファイルが存在しない
     */
    data class NotFound(
        override val message: String = "ファイルが見つかりません",
        override val cause: Throwable? = null,
        val path: String? = null
    ) : FileError()

    /**
     * アクセス権限エラー
     *
     * - ファイルへの読み取り/書き込み権限がない
     * - Androidの権限が未付与
     */
    data class PermissionDenied(
        override val message: String = "ファイルへのアクセス権限がありません",
        override val cause: Throwable? = null,
        val path: String? = null
    ) : FileError()

    /**
     * ファイル形式エラー
     *
     * - 期待される形式と異なるファイル
     * - 破損したファイル
     * - パース失敗
     */
    data class InvalidFormat(
        override val message: String = "ファイル形式が不正です",
        override val cause: Throwable? = null,
        val expectedFormat: String? = null
    ) : FileError()
}
