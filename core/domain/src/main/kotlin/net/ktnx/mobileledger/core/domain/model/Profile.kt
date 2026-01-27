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
 * プロファイルのドメインモデル
 *
 * サーバー接続設定のビジネス表現。
 * データベースの検出バージョンフラグ等の実装詳細を隠蔽する。
 */
data class Profile(
    /** データベースID。新規プロファイルの場合はnull */
    val id: Long? = null,

    /** プロファイル名 */
    val name: String,

    /** UUID（同期用識別子） */
    val uuid: String,

    /** サーバーURL */
    val url: String,

    /** 認証設定 */
    val authentication: ProfileAuthentication? = null,

    /** 表示順序 */
    val orderNo: Int = 0,

    /** 投稿許可 */
    val permitPosting: Boolean = false,

    /** テーマID（-1はデフォルト） */
    val theme: Int = -1,

    /** 推奨アカウントフィルタ */
    val preferredAccountsFilter: String? = null,

    /** 将来日付設定 */
    val futureDates: FutureDates = FutureDates.None,

    /** APIバージョン */
    val apiVersion: Int = 0,

    /** デフォルトで通貨を表示 */
    val showCommodityByDefault: Boolean = false,

    /** デフォルト通貨 */
    val defaultCommodity: String? = null,

    /** デフォルトでコメントを表示 */
    val showCommentsByDefault: Boolean = true,

    /** 検出されたサーバーバージョン */
    val serverVersion: ServerVersion? = null
) {
    /**
     * 認証が有効かどうか
     */
    val isAuthEnabled: Boolean get() = authentication != null

    /**
     * 投稿可能かどうか
     */
    val canPost: Boolean get() = permitPosting

    /**
     * デフォルト通貨（空文字の場合は空文字を返す）
     */
    val defaultCommodityOrEmpty: String get() = defaultCommodity ?: ""

    /**
     * 1.20.1以前のバージョンかどうか
     */
    val isVersionPre_1_20_1: Boolean get() = serverVersion?.isPre_1_20_1 ?: false

    /**
     * 検出されたメジャーバージョン
     */
    val detectedVersionMajor: Int get() = serverVersion?.major ?: 0

    /**
     * 検出されたマイナーバージョン
     */
    val detectedVersionMinor: Int get() = serverVersion?.minor ?: 0

    companion object {
        const val NO_PROFILE_ID: Long = 0
    }
}
