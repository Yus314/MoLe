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

@file:Suppress("ktlint:standard:class-naming")

package net.ktnx.mobileledger.json.config

import net.ktnx.mobileledger.json.API
import net.ktnx.mobileledger.json.ApiNotSupportedException

/**
 * API バージョンごとの設定を定義する sealed class 階層
 *
 * 各 API バージョン間の JSON 構造の違いを吸収するための設定を提供する。
 * 主な差分:
 * - ptransaction_ フィールドの型 (Int vs String)
 * - decimal point/mark フィールド名と型
 * - Account 残高構造 (direct vs period-based)
 */
sealed class ApiVersionConfig {
    /**
     * トランザクションIDの型
     */
    abstract val transactionIdType: TransactionIdType

    /**
     * スタイル設定の構造
     */
    abstract val styleConfig: StyleFieldConfig

    /**
     * アカウント残高の取得方法
     */
    abstract val accountBalanceExtractor: AccountBalanceExtractor

    /**
     * v1_14, v1_15: 最初期のJSON API
     * - ptransaction_: Int
     * - asdecimalpoint: Char
     * - asprecision: Int
     */
    data object V1_14_15 : ApiVersionConfig() {
        override val transactionIdType = TransactionIdType.IntType
        override val styleConfig = StyleFieldConfig.DecimalPointChar
        override val accountBalanceExtractor = AccountBalanceExtractor.DirectBalance
    }

    /**
     * v1_19_1: asprecision が ParsedPrecision オブジェクトに変更
     * - ptransaction_: Int
     * - asdecimalpoint: Char
     * - asprecision: ParsedPrecision (オブジェクト)
     */
    data object V1_19_1 : ApiVersionConfig() {
        override val transactionIdType = TransactionIdType.IntType
        override val styleConfig = StyleFieldConfig.DecimalPointCharWithParsedPrecision
        override val accountBalanceExtractor = AccountBalanceExtractor.DirectBalance
    }

    /**
     * v1_23: asprecision が Int に戻る
     * - ptransaction_: Int
     * - asdecimalpoint: Char
     * - asprecision: Int
     */
    data object V1_23 : ApiVersionConfig() {
        override val transactionIdType = TransactionIdType.IntType
        override val styleConfig = StyleFieldConfig.DecimalPointCharIntPrecision
        override val accountBalanceExtractor = AccountBalanceExtractor.DirectBalance
    }

    /**
     * v1_32, v1_40: 大きな構造変更
     * - ptransaction_: String (重要な型変更!)
     * - asdecimalmark: String (フィールド名変更)
     * - asrounding: String (新規追加)
     */
    data object V1_32_40 : ApiVersionConfig() {
        override val transactionIdType = TransactionIdType.StringType
        override val styleConfig = StyleFieldConfig.DecimalMarkString
        override val accountBalanceExtractor = AccountBalanceExtractor.DirectBalance
    }

    /**
     * v1_50: アカウント構造が完全に変更
     * - ptransaction_: String
     * - asdecimalmark: String
     * - Account: adata.pdperiods[0][1].bdincludingsubs (period-based構造)
     * - tsourcepos: MutableList<ParsedSourcePos> (配列化)
     */
    data object V1_50 : ApiVersionConfig() {
        override val transactionIdType = TransactionIdType.StringType
        override val styleConfig = StyleFieldConfig.DecimalMarkString
        override val accountBalanceExtractor = AccountBalanceExtractor.PeriodBasedBalance
    }

    companion object {
        /**
         * API バージョンから対応する設定を取得
         *
         * @param api API バージョン
         * @return 対応する ApiVersionConfig
         * @throws ApiNotSupportedException auto または html の場合
         */
        fun forApi(api: API): ApiVersionConfig = when (api) {
            API.v1_14, API.v1_15 -> V1_14_15
            API.v1_19_1 -> V1_19_1
            API.v1_23 -> V1_23
            API.v1_32, API.v1_40 -> V1_32_40
            API.v1_50 -> V1_50
            API.auto, API.html -> throw ApiNotSupportedException("API $api is not supported")
        }
    }
}

/**
 * トランザクションIDの型を表す
 */
enum class TransactionIdType {
    /** v1_14〜v1_23: ptransaction_ は Int */
    IntType,

    /** v1_32+: ptransaction_ は String */
    StringType
}

/**
 * スタイルフィールドの構造を表す
 */
enum class StyleFieldConfig {
    /** v1_14, v1_15: asdecimalpoint (Char), asprecision (Int) */
    DecimalPointChar,

    /** v1_19_1: asdecimalpoint (Char), asprecision (ParsedPrecision オブジェクト) */
    DecimalPointCharWithParsedPrecision,

    /** v1_23: asdecimalpoint (Char), asprecision (Int) に戻る */
    DecimalPointCharIntPrecision,

    /** v1_32+: asdecimalmark (String), asprecision (Int), asrounding (String) */
    DecimalMarkString
}

/**
 * アカウント残高の取得方法を表す
 */
enum class AccountBalanceExtractor {
    /** v1_14〜v1_40: aibalance フィールドから直接取得 */
    DirectBalance,

    /** v1_50: adata.pdperiods[0][1].bdincludingsubs から取得 */
    PeriodBasedBalance
}
