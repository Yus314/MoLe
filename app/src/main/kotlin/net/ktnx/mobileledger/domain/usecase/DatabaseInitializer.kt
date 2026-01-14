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

/**
 * データベース初期化のインターフェース
 *
 * アプリ起動時のデータベース初期化処理を管理する。
 */
interface DatabaseInitializer {

    /**
     * データベースを初期化する
     *
     * @return 成功時は Result.success(hasProfiles)、失敗時は Result.failure()
     *         hasProfiles: プロファイルが1つ以上存在する場合 true
     */
    suspend fun initialize(): Result<Boolean>

    /**
     * 初期化が完了しているかどうか
     */
    val isInitialized: Boolean
}
