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
 * テンプレートのドメインモデル
 *
 * 取引テンプレートのビジネス表現。
 * データベースのヘッダー/行分離を隠蔽する。
 */
data class Template(
    /** データベースID。新規テンプレートの場合はnull */
    val id: Long? = null,

    /** UUID（バックアップ/リストア時の重複検出に使用） */
    val uuid: String = java.util.UUID.randomUUID().toString(),

    /** テンプレート名 */
    val name: String,

    /** マッチングパターン（正規表現） */
    val pattern: String,

    /** テスト用テキスト */
    val testText: String? = null,

    /** 取引説明のリテラル値 */
    val transactionDescription: String? = null,

    /** 取引説明のマッチグループ番号 */
    val transactionDescriptionMatchGroup: Int? = null,

    /** 取引コメントのリテラル値 */
    val transactionComment: String? = null,

    /** 取引コメントのマッチグループ番号 */
    val transactionCommentMatchGroup: Int? = null,

    /** 日付の年（リテラル値） */
    val dateYear: Int? = null,

    /** 日付の年グループ番号 */
    val dateYearMatchGroup: Int? = null,

    /** 日付の月（リテラル値） */
    val dateMonth: Int? = null,

    /** 日付の月グループ番号 */
    val dateMonthMatchGroup: Int? = null,

    /** 日付の日（リテラル値） */
    val dateDay: Int? = null,

    /** 日付の日グループ番号 */
    val dateDayMatchGroup: Int? = null,

    /** テンプレート行のリスト */
    val lines: List<TemplateLine> = emptyList(),

    /** フォールバックフラグ */
    val isFallback: Boolean = false
)
