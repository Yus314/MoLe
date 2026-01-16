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

package net.ktnx.mobileledger.data.repository.mapper

import net.ktnx.mobileledger.domain.model.Account
import net.ktnx.mobileledger.domain.model.AccountAmount
import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.domain.model.TransactionLine
import net.ktnx.mobileledger.model.LedgerAccount
import net.ktnx.mobileledger.model.LedgerTransaction
import net.ktnx.mobileledger.model.LedgerTransactionAccount

/**
 * 旧モデル（model.*）からドメインモデル（domain.model.*）への変換を担当
 *
 * JSON/HTMLパーサーが返すLedgerAccount/LedgerTransactionをドメインモデルに変換する。
 * これにより、domain層からdb層への依存を排除できる。
 */
@Suppress("DEPRECATION")
object LegacyModelMapper {

    /**
     * LedgerAccount から ドメインモデル Account へ変換
     *
     * Note: dbId や profileId は保持されない（Repository で設定される）
     */
    fun LedgerAccount.toDomain(): Account = Account(
        id = null, // Will be assigned during storage
        name = name,
        level = level,
        isExpanded = isExpanded,
        isVisible = isVisible,
        amounts = getAmounts()?.map { ledgerAmount ->
            AccountAmount(
                currency = ledgerAmount.currency ?: "",
                amount = ledgerAmount.amount
            )
        } ?: emptyList()
    )

    /**
     * LedgerTransaction から ドメインモデル Transaction へ変換
     *
     * Note: date は必須。null の場合は IllegalStateException をスロー
     */
    fun LedgerTransaction.toDomain(): Transaction {
        val date = requireNotNull(getDateIfAny()) {
            "Transaction date must be set before converting to domain model"
        }
        return Transaction(
            id = null, // Will be assigned during storage
            ledgerId = ledgerId,
            date = date,
            description = description ?: "",
            comment = comment,
            lines = accounts.map { it.toDomain() }
        )
    }

    /**
     * LedgerTransactionAccount から ドメインモデル TransactionLine へ変換
     */
    fun LedgerTransactionAccount.toDomain(): TransactionLine = TransactionLine(
        id = null,
        accountName = accountName,
        amount = if (isAmountSet) amount else null,
        currency = currency ?: "",
        comment = comment
    )

    /**
     * LedgerAccount リストを一括変換
     */
    fun List<LedgerAccount>.toDomainAccounts(): List<Account> = map { it.toDomain() }

    /**
     * LedgerTransaction リストを一括変換
     */
    fun List<LedgerTransaction>.toDomainTransactions(): List<Transaction> = map { it.toDomain() }
}
