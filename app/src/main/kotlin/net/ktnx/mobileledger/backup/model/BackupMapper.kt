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

package net.ktnx.mobileledger.backup.model

import net.ktnx.mobileledger.core.database.entity.Currency
import net.ktnx.mobileledger.core.database.entity.Profile
import net.ktnx.mobileledger.core.database.entity.TemplateAccount
import net.ktnx.mobileledger.core.database.entity.TemplateHeader
import net.ktnx.mobileledger.core.database.entity.TemplateWithAccounts
import net.ktnx.mobileledger.core.domain.model.API
import net.ktnx.mobileledger.core.domain.model.Currency as DomainCurrency
import net.ktnx.mobileledger.core.domain.model.Profile as DomainProfile

/**
 * Mapper for converting between backup models and DB entities/domain models.
 */
object BackupMapper {

    // Currency mappings

    fun DomainCurrency.toBackupModel(): CurrencyBackupModel = CurrencyBackupModel(
        name = name,
        position = position.toDbString(),
        hasGap = hasGap
    )

    fun CurrencyBackupModel.toDbEntity(): Currency = Currency().apply {
        name = this@toDbEntity.name
        position = this@toDbEntity.position ?: "after"
        hasGap = this@toDbEntity.hasGap ?: true
    }

    // Profile mappings

    fun DomainProfile.toBackupModel(): ProfileBackupModel = ProfileBackupModel(
        uuid = uuid,
        name = name,
        url = url,
        useAuth = isAuthEnabled,
        authUser = if (isAuthEnabled) authentication?.user else null,
        authPass = if (isAuthEnabled) authentication?.password else null,
        apiVersion = if (apiVersion != API.auto.toInt()) apiVersion else null,
        permitPosting = canPost,
        defaultCommodity = if (canPost) defaultCommodityOrEmpty.takeIf { it.isNotEmpty() } else null,
        showCommodityByDefault = if (canPost) showCommodityByDefault else null,
        showCommentsByDefault = if (canPost) showCommentsByDefault else null,
        futureDates = if (canPost) futureDates.toInt() else null,
        preferredAccountsFilter = if (canPost) preferredAccountsFilter else null,
        colour = theme
    )

    fun ProfileBackupModel.toDbEntity(): Profile = Profile().apply {
        uuid = this@toDbEntity.uuid
        name = this@toDbEntity.name
        url = this@toDbEntity.url
        useAuthentication = this@toDbEntity.useAuth
        authUser = this@toDbEntity.authUser
        authPassword = this@toDbEntity.authPass
        apiVersion = this@toDbEntity.apiVersion ?: API.auto.toInt()
        permitPosting = this@toDbEntity.permitPosting
        if (permitPosting) {
            this@toDbEntity.defaultCommodity?.let { setDefaultCommodity(it) }
            this@toDbEntity.showCommodityByDefault?.let { showCommodityByDefault = it }
            this@toDbEntity.showCommentsByDefault?.let { showCommentsByDefault = it }
            this@toDbEntity.futureDates?.let { futureDates = it }
            this@toDbEntity.preferredAccountsFilter?.let { preferredAccountsFilter = it }
        }
        theme = this@toDbEntity.colour
    }

    // Template mappings

    fun TemplateWithAccounts.toBackupModel(): TemplateBackupModel = TemplateBackupModel(
        uuid = header.uuid,
        name = header.name,
        regex = header.regularExpression,
        testText = header.testText,
        dateYear = header.dateYear,
        dateYearMatchGroup = header.dateYearMatchGroup,
        dateMonth = header.dateMonth,
        dateMonthMatchGroup = header.dateMonthMatchGroup,
        dateDay = header.dateDay,
        dateDayMatchGroup = header.dateDayMatchGroup,
        transactionDescription = header.transactionDescription,
        transactionDescriptionMatchGroup = header.transactionDescriptionMatchGroup,
        comment = header.transactionComment,
        commentMatchGroup = header.transactionCommentMatchGroup,
        isFallback = header.isFallback,
        accounts = if (accounts.isNotEmpty()) accounts.map { it.toBackupModel() } else null
    )

    fun TemplateAccount.toBackupModel(): TemplateAccountBackupModel = TemplateAccountBackupModel(
        name = accountName,
        nameMatchGroup = accountNameMatchGroup,
        comment = accountComment,
        commentMatchGroup = accountCommentMatchGroup,
        amount = amount,
        amountGroup = amountMatchGroup,
        negateAmount = negateAmount,
        currency = currency,
        currencyGroup = currencyMatchGroup
    )

    fun TemplateBackupModel.toDbEntity(): TemplateWithAccounts = TemplateWithAccounts().apply {
        header = TemplateHeader(0L, this@toDbEntity.name, this@toDbEntity.regex ?: "").apply {
            uuid = this@toDbEntity.uuid
            testText = this@toDbEntity.testText
            dateYear = this@toDbEntity.dateYear
            dateYearMatchGroup = this@toDbEntity.dateYearMatchGroup
            dateMonth = this@toDbEntity.dateMonth
            dateMonthMatchGroup = this@toDbEntity.dateMonthMatchGroup
            dateDay = this@toDbEntity.dateDay
            dateDayMatchGroup = this@toDbEntity.dateDayMatchGroup
            transactionDescription = this@toDbEntity.transactionDescription
            transactionDescriptionMatchGroup = this@toDbEntity.transactionDescriptionMatchGroup
            transactionComment = this@toDbEntity.comment
            transactionCommentMatchGroup = this@toDbEntity.commentMatchGroup
            isFallback = this@toDbEntity.isFallback
        }
        accounts = this@toDbEntity.accounts?.map { it.toDbEntity() } ?: emptyList()
    }

    fun TemplateAccountBackupModel.toDbEntity(): TemplateAccount = TemplateAccount(0L, 0L, 0L).apply {
        accountName = this@toDbEntity.name
        accountNameMatchGroup = this@toDbEntity.nameMatchGroup
        accountComment = this@toDbEntity.comment
        accountCommentMatchGroup = this@toDbEntity.commentMatchGroup
        amount = this@toDbEntity.amount
        amountMatchGroup = this@toDbEntity.amountGroup
        negateAmount = this@toDbEntity.negateAmount
        currency = this@toDbEntity.currency
        currencyMatchGroup = this@toDbEntity.currencyGroup
    }
}
