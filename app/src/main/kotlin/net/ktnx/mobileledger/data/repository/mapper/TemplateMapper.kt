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

import net.ktnx.mobileledger.core.database.entity.TemplateAccount
import net.ktnx.mobileledger.core.database.entity.TemplateHeader
import net.ktnx.mobileledger.core.database.entity.TemplateWithAccounts
import net.ktnx.mobileledger.core.domain.model.Template
import net.ktnx.mobileledger.core.domain.model.TemplateLine

/**
 * Template ドメインモデルとデータベースエンティティ間の変換を担当
 */
object TemplateMapper {

    /**
     * データベースエンティティからドメインモデルへ変換
     *
     * @receiver TemplateWithAccounts (Room Relation)
     * @param currencyMap 通貨IDから通貨名へのマップ（Repository層で構築）
     * @return Template ドメインモデル
     */
    fun TemplateWithAccounts.toDomain(currencyMap: Map<Long, String> = emptyMap()): Template = Template(
        id = header.id,
        name = header.name,
        pattern = header.regularExpression,
        testText = header.testText,
        transactionDescription = header.transactionDescription,
        transactionDescriptionMatchGroup = header.transactionDescriptionMatchGroup,
        transactionComment = header.transactionComment,
        transactionCommentMatchGroup = header.transactionCommentMatchGroup,
        dateYear = header.dateYear,
        dateYearMatchGroup = header.dateYearMatchGroup,
        dateMonth = header.dateMonth,
        dateMonthMatchGroup = header.dateMonthMatchGroup,
        dateDay = header.dateDay,
        dateDayMatchGroup = header.dateDayMatchGroup,
        isFallback = header.isFallback,
        lines = accounts.sortedBy { it.position }.map { account ->
            val currencyName = account.currency?.let { currencyMap[it] }
            account.toDomain(currencyName)
        }
    )

    /**
     * TemplateAccount のドメインモデルへの変換
     *
     * @param currencyName 解決済み通貨名（nullの場合は未解決）
     */
    fun TemplateAccount.toDomain(currencyName: String? = null): TemplateLine = TemplateLine(
        id = id,
        accountName = accountName,
        accountNameGroup = accountNameMatchGroup,
        amount = amount,
        amountGroup = amountMatchGroup,
        currencyId = currency,
        currencyName = currencyName,
        currencyGroup = currencyMatchGroup,
        comment = accountComment,
        commentGroup = accountCommentMatchGroup,
        negateAmount = negateAmount == true
    )

    /**
     * ドメインモデルからデータベースエンティティへ変換
     *
     * @receiver Template ドメインモデル
     * @return TemplateWithAccounts (Room Relation)
     */
    fun Template.toEntity(): TemplateWithAccounts {
        val templateId = id ?: 0L

        val header = TemplateHeader(templateId, name, pattern).apply {
            testText = this@toEntity.testText
            transactionDescription = this@toEntity.transactionDescription
            transactionDescriptionMatchGroup = this@toEntity.transactionDescriptionMatchGroup
            transactionComment = this@toEntity.transactionComment
            transactionCommentMatchGroup = this@toEntity.transactionCommentMatchGroup
            dateYear = this@toEntity.dateYear
            dateYearMatchGroup = this@toEntity.dateYearMatchGroup
            dateMonth = this@toEntity.dateMonth
            dateMonthMatchGroup = this@toEntity.dateMonthMatchGroup
            dateDay = this@toEntity.dateDay
            dateDayMatchGroup = this@toEntity.dateDayMatchGroup
            isFallback = this@toEntity.isFallback
        }

        val accounts = lines.mapIndexed { index, line ->
            line.toEntity(templateId, index.toLong())
        }

        return TemplateWithAccounts().apply {
            this.header = header
            this.accounts = accounts
        }
    }

    /**
     * TemplateLine のエンティティへの変換
     */
    fun TemplateLine.toEntity(templateId: Long, position: Long): TemplateAccount =
        TemplateAccount(id ?: 0L, templateId, position).apply {
            accountName = this@toEntity.accountName
            accountNameMatchGroup = this@toEntity.accountNameGroup
            amount = this@toEntity.amount
            amountMatchGroup = this@toEntity.amountGroup
            currency = this@toEntity.currencyId
            currencyMatchGroup = this@toEntity.currencyGroup
            accountComment = this@toEntity.comment
            accountCommentMatchGroup = this@toEntity.commentGroup
            negateAmount = if (this@toEntity.negateAmount) true else null
        }
}
