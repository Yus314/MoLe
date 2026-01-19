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
 * テンプレート行のドメインモデル
 *
 * テンプレート内の1行を表す。各フィールドはリテラル値またはマッチグループ番号のいずれか。
 * データベースのテンプレートID、position等の実装詳細を隠蔽する。
 */
data class TemplateLine(
    /** データベースID。新規行の場合はnull */
    val id: Long? = null,

    /** 勘定科目名（リテラル値） */
    val accountName: String? = null,

    /** 勘定科目名マッチグループ番号 */
    val accountNameGroup: Int? = null,

    /** 金額（リテラル値） */
    val amount: Float? = null,

    /** 金額マッチグループ番号 */
    val amountGroup: Int? = null,

    /** 通貨ID（リテラル値） */
    val currencyId: Long? = null,

    /** 通貨名（解決済み） */
    val currencyName: String? = null,

    /** 通貨マッチグループ番号 */
    val currencyGroup: Int? = null,

    /** コメント（リテラル値） */
    val comment: String? = null,

    /** コメントマッチグループ番号 */
    val commentGroup: Int? = null,

    /** 金額の符号を反転するか */
    val negateAmount: Boolean = false
)
