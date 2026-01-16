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
 * 勘定科目のドメインモデル
 *
 * 勘定科目と残高のビジネス表現。
 * データベースのテーブル結合やリレーション構造を隠蔽する。
 */
data class Account(
    /** データベースID。新規勘定科目の場合はnull */
    val id: Long? = null,

    /** 勘定科目名（フルパス） */
    val name: String,

    /** 階層レベル（0-based） */
    val level: Int = 0,

    /** 展開状態（UI用） */
    val isExpanded: Boolean = false,

    /** 可視状態（UI用） */
    val isVisible: Boolean = true,

    /** 勘定科目ごとの残高リスト */
    val amounts: List<AccountAmount> = emptyList()
) {
    /**
     * 親勘定科目名を取得
     */
    val parentName: String?
        get() = name.lastIndexOf(':').let { idx ->
            if (idx > 0) name.substring(0, idx) else null
        }

    /**
     * 短い名前（最後のセグメント）を取得
     */
    val shortName: String
        get() = name.substringAfterLast(':')

    /**
     * 残高を持っているかどうか
     */
    val hasAmounts: Boolean get() = amounts.isNotEmpty()
}
