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

import net.ktnx.mobileledger.db.Profile

/**
 * hledger-web バージョン検出のインターフェース
 *
 * プロファイルのサーバーに接続し、hledger-webのバージョンを検出する。
 */
interface VersionDetector {

    /**
     * hledger-web のバージョンを検出する
     *
     * @param url サーバーのベースURL
     * @param useAuth 認証を使用するかどうか
     * @param user 認証ユーザー名（useAuth=true の場合に使用）
     * @param password 認証パスワード（useAuth=true の場合に使用）
     * @return 成功時はバージョン文字列（例: "1.32"）、失敗時は Result.failure()
     */
    suspend fun detect(url: String, useAuth: Boolean, user: String?, password: String?): Result<String>

    /**
     * プロファイルからバージョンを検出する（便利メソッド）
     *
     * @param profile 検出対象のプロファイル
     * @return 成功時はバージョン文字列、失敗時は Result.failure()
     */
    suspend fun detect(profile: Profile): Result<String> = detect(
        url = profile.url,
        useAuth = profile.useAuthentication,
        user = profile.authUser,
        password = profile.authPassword
    )
}
