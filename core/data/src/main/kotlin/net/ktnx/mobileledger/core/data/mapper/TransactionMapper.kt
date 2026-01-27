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

package net.ktnx.mobileledger.core.data.mapper

import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.database.entity.Transaction as DbTransaction
import net.ktnx.mobileledger.core.database.entity.TransactionAccount
import net.ktnx.mobileledger.core.database.entity.TransactionWithAccounts
import net.ktnx.mobileledger.core.domain.model.Transaction
import net.ktnx.mobileledger.core.domain.model.TransactionLine

/**
 * Transaction ドメインモデルとデータベースエンティティ間の変換を担当
 */
object TransactionMapper {

    /**
     * データベースエンティティからドメインモデルへ変換
     *
     * Mapping rules:
     * - id: transaction.id
     * - ledgerId: transaction.ledgerId
     * - date: SimpleDate(transaction.year, transaction.month, transaction.day)
     * - description: transaction.description
     * - comment: transaction.comment
     * - lines: accounts.map { it.toDomain() }
     *
     * Hidden fields (not exposed to domain):
     * - profileId
     * - dataHash
     * - descriptionUpper
     * - generation
     */
    fun toDomain(entity: TransactionWithAccounts): Transaction = Transaction(
        id = entity.transaction.id,
        ledgerId = entity.transaction.ledgerId,
        date = SimpleDate(
            entity.transaction.year,
            entity.transaction.month,
            entity.transaction.day
        ),
        description = entity.transaction.description,
        comment = entity.transaction.comment,
        lines = entity.accounts.map { toDomain(it) }
    )

    /**
     * TransactionAccount のドメインモデルへの変換
     */
    fun toDomain(entity: TransactionAccount): TransactionLine = TransactionLine(
        id = entity.id,
        accountName = entity.accountName,
        amount = entity.amount,
        currency = entity.currency.ifEmpty { "" },
        comment = entity.comment
    )

    /**
     * List of TransactionWithAccounts をドメインモデルに変換
     */
    fun toDomainList(entities: List<TransactionWithAccounts>): List<Transaction> = entities.map { toDomain(it) }

    /**
     * ドメインモデルからデータベースエンティティへ変換
     *
     * Mapping rules:
     * - transaction.id: domain.id ?: 0
     * - transaction.ledgerId: domain.ledgerId
     * - transaction.profileId: profileId (parameter)
     * - transaction.year: domain.date.year
     * - transaction.month: domain.date.month
     * - transaction.day: domain.date.day
     * - transaction.description: domain.description
     * - transaction.comment: domain.comment
     * - accounts: domain.lines.mapIndexed { index, line -> line.toEntity(index + 1) }
     */
    fun toEntity(domain: Transaction, profileId: Long): TransactionWithAccounts {
        val dbTransaction = DbTransaction().apply {
            id = domain.id ?: 0L
            ledgerId = domain.ledgerId
            this.profileId = profileId
            year = domain.date.year
            month = domain.date.month
            day = domain.date.day
            description = domain.description
            comment = domain.comment
        }

        val dbAccounts = domain.lines.mapIndexed { index, line ->
            toEntity(line, index + 1)
        }

        return TransactionWithAccounts().apply {
            transaction = dbTransaction
            accounts = dbAccounts
        }
    }

    /**
     * TransactionLine のエンティティへの変換
     */
    fun toEntity(domain: TransactionLine, orderNo: Int): TransactionAccount = TransactionAccount().apply {
        id = domain.id ?: 0L
        this.orderNo = orderNo
        accountName = domain.accountName
        amount = domain.amount ?: 0f
        currency = domain.currency
        comment = domain.comment
    }
}
