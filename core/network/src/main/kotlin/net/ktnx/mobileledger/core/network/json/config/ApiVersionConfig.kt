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

package net.ktnx.mobileledger.core.network.json.config

import net.ktnx.mobileledger.core.network.json.API
import net.ktnx.mobileledger.core.network.json.ApiNotSupportedException

/**
 * API バージョンごとの設定を定義する sealed class 階層
 *
 * 各 API バージョン間の JSON 構造の違いを吸収するための設定を提供する。
 *
 * サポート対象: v1_32, v1_40, v1_50 のみ
 * (v1_14, v1_15, v1_19_1, v1_23 は削除済み)
 *
 * 主な差分:
 * - Account 残高構造 (direct vs period-based)
 * - tsourcepos フィールド (単一オブジェクト vs 配列)
 */
sealed class ApiVersionConfig {
    /**
     * アカウント残高の取得方法
     */
    abstract val accountBalanceExtractor: AccountBalanceExtractor

    /**
     * v1_32, v1_40: JSON API
     * - ptransaction_: String
     * - asdecimalmark: String
     * - asrounding: String
     * - Account: aibalance から直接取得
     */
    data object V1_32_40 : ApiVersionConfig() {
        override val accountBalanceExtractor = AccountBalanceExtractor.DirectBalance
    }

    /**
     * v1_50: アカウント構造が変更
     * - ptransaction_: String
     * - asdecimalmark: String
     * - Account: adata.pdperiods[0][1].bdincludingsubs (period-based構造)
     * - tsourcepos: MutableList<ParsedSourcePos> (配列化)
     */
    data object V1_50 : ApiVersionConfig() {
        override val accountBalanceExtractor = AccountBalanceExtractor.PeriodBasedBalance
    }

    companion object {
        /**
         * API バージョンから対応する設定を取得
         *
         * @param api API バージョン
         * @return 対応する ApiVersionConfig
         * @throws ApiNotSupportedException auto の場合
         */
        fun forApi(api: API): ApiVersionConfig = when (api) {
            API.v1_32, API.v1_40 -> V1_32_40
            API.v1_50 -> V1_50
            API.auto -> throw ApiNotSupportedException("API $api is not supported")
        }
    }
}

/**
 * アカウント残高の取得方法を表す
 */
enum class AccountBalanceExtractor {
    /** v1_32, v1_40: aibalance フィールドから直接取得 */
    DirectBalance,

    /** v1_50: adata.pdperiods[0][1].bdincludingsubs から取得 */
    PeriodBasedBalance
}
