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

package net.ktnx.mobileledger.util

import java.util.UUID
import net.ktnx.mobileledger.core.database.entity.Account
import net.ktnx.mobileledger.core.database.entity.AccountValue
import net.ktnx.mobileledger.core.database.entity.AccountWithAmounts
import net.ktnx.mobileledger.core.database.entity.Profile
import net.ktnx.mobileledger.core.database.entity.Transaction
import net.ktnx.mobileledger.core.database.entity.TransactionAccount
import net.ktnx.mobileledger.core.database.entity.TransactionWithAccounts
import net.ktnx.mobileledger.core.domain.model.FutureDates
import net.ktnx.mobileledger.core.domain.model.Profile as DomainProfile
import net.ktnx.mobileledger.core.domain.model.ProfileAuthentication

/**
 * Test utility functions for creating test data.
 */

/**
 * Create a domain model Profile for testing.
 */
fun createTestDomainProfile(
    id: Long? = 1L,
    name: String = "Test Profile",
    url: String = "https://example.com/ledger",
    theme: Int = 0,
    orderNo: Int = 0,
    authentication: ProfileAuthentication? = null,
    permitPosting: Boolean = true,
    defaultCommodity: String? = null,
    apiVersion: Int = 0,
    futureDates: FutureDates = FutureDates.None
): DomainProfile = DomainProfile(
    id = id,
    name = name,
    uuid = UUID.randomUUID().toString(),
    url = url,
    authentication = authentication,
    orderNo = orderNo,
    permitPosting = permitPosting,
    theme = theme,
    preferredAccountsFilter = null,
    futureDates = futureDates,
    apiVersion = apiVersion,
    showCommodityByDefault = false,
    defaultCommodity = defaultCommodity,
    showCommentsByDefault = true,
    serverVersion = null
)

/**
 * Create a db.Profile for testing (legacy, prefer createTestDomainProfile).
 */
fun createTestProfile(
    id: Long = 0L,
    name: String = "Test Profile",
    url: String = "https://example.com/ledger",
    theme: Int = 0,
    orderNo: Int = 0,
    useAuthentication: Boolean = false,
    permitPosting: Boolean = true,
    defaultCommodity: String? = null,
    apiVersion: Int = 0
): Profile = Profile().apply {
    this.id = id
    this.name = name
    this.uuid = UUID.randomUUID().toString()
    this.url = url
    this.theme = theme
    this.orderNo = orderNo
    this.useAuthentication = useAuthentication
    this.permitPosting = permitPosting
    this.setDefaultCommodity(defaultCommodity)
    this.apiVersion = apiVersion
}

fun createTestTransaction(
    id: Long = 0L,
    profileId: Long = 1L,
    ledgerId: Long = 0L,
    description: String = "Test Transaction",
    year: Int = 2024,
    month: Int = 1,
    day: Int = 15,
    comment: String? = null
): Transaction = Transaction().apply {
    this.id = id
    this.profileId = profileId
    this.ledgerId = ledgerId
    this.description = description
    this.year = year
    this.month = month
    this.day = day
    this.comment = comment
}

fun createTestTransactionAccount(
    id: Long = 0L,
    transactionId: Long = 1L,
    accountName: String = "Assets:Checking",
    amount: Float = 100.0f,
    currency: String = "USD",
    orderNo: Int = 0,
    comment: String? = null
): TransactionAccount = TransactionAccount().apply {
    this.id = id
    this.transactionId = transactionId
    this.accountName = accountName
    this.amount = amount
    this.currency = currency
    this.orderNo = orderNo
    this.comment = comment
}

fun createTestTransactionWithAccounts(
    id: Long = 0L,
    profileId: Long = 1L,
    description: String = "Test Transaction",
    year: Int = 2024,
    month: Int = 1,
    day: Int = 15,
    accounts: List<TransactionAccount>? = null
): TransactionWithAccounts {
    val transaction = createTestTransaction(
        id = id,
        profileId = profileId,
        description = description,
        year = year,
        month = month,
        day = day
    )

    val transactionAccounts = accounts ?: listOf(
        createTestTransactionAccount(
            transactionId = id,
            accountName = "Expenses:Groceries",
            amount = 50.0f,
            orderNo = 0
        ),
        createTestTransactionAccount(
            transactionId = id,
            accountName = "Assets:Checking",
            amount = -50.0f,
            orderNo = 1
        )
    )

    return TransactionWithAccounts().apply {
        this.transaction = transaction
        this.accounts = transactionAccounts
    }
}

fun createTestAccount(
    id: Long = 0L,
    profileId: Long = 1L,
    name: String = "Assets:Checking",
    level: Int = 1,
    parentName: String? = null,
    expanded: Boolean = true
): Account = Account().apply {
    this.id = id
    this.profileId = profileId
    this.name = name
    this.level = level
    this.parentName = parentName
    this.expanded = expanded
}

fun createTestAccountValue(
    id: Long = 0L,
    accountId: Long = 1L,
    currency: String = "USD",
    value: Float = 1000.0f
): AccountValue = AccountValue().apply {
    this.id = id
    this.accountId = accountId
    this.currency = currency
    this.value = value
}

fun createTestAccountWithAmounts(
    account: Account = createTestAccount(),
    amounts: List<AccountValue> = listOf(createTestAccountValue(accountId = account.id))
): AccountWithAmounts = AccountWithAmounts().apply {
    this.account = account
    this.amounts = amounts
}
