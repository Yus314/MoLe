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

import android.net.Uri

/**
 * 設定バックアップ/リストアのインターフェース
 *
 * プロファイル設定をファイルにバックアップ・リストアする。
 * 既存の ConfigIO（ConfigReader, ConfigWriter）を置き換える。
 */
interface ConfigBackup {

    /**
     * 設定をファイルにバックアップする
     *
     * @param uri バックアップ先のファイルURI（SAF経由で取得）
     * @return 成功時は Result.success(Unit)、失敗時は Result.failure(Exception)
     */
    suspend fun backup(uri: Uri): Result<Unit>

    /**
     * ファイルから設定をリストアする
     *
     * @param uri リストア元のファイルURI（SAF経由で取得）
     * @return 成功時は Result.success(Unit)、失敗時は Result.failure(Exception)
     */
    suspend fun restore(uri: Uri): Result<Unit>
}
