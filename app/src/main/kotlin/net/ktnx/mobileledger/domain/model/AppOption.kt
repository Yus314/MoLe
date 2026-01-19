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
 * アプリケーションオプションのドメインモデル
 *
 * プロファイルごとのキーバリュー設定を表現。
 * データベーススキーマの詳細を隠蔽する。
 */
data class AppOption(
    val profileId: Long,
    val name: String,
    val value: String?
) {
    companion object {
        /** 最終同期タイムスタンプのオプション名 */
        const val OPT_LAST_SCRAPE = "last_scrape"
    }

    /**
     * 値を Long として取得（タイムスタンプ等）
     * パース失敗時は null を返す
     */
    fun valueAsLong(): Long? = value?.toLongOrNull()
}
