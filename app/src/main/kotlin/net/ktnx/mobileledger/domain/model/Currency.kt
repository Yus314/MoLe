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
 * 通貨のドメインモデル
 *
 * 通貨設定のビジネス表現。
 * データベースのprofileId等の実装詳細を隠蔽する。
 */
data class Currency(
    /** データベースID。新規通貨の場合はnull */
    val id: Long? = null,

    /** 通貨名/シンボル */
    val name: String,

    /** 表示位置 */
    val position: CurrencyPosition = CurrencyPosition.AFTER,

    /** 金額との間にスペースを入れる */
    val hasGap: Boolean = true
)
