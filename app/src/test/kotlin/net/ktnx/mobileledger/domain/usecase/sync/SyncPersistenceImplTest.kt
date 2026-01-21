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

package net.ktnx.mobileledger.domain.usecase.sync

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.data.repository.AccountRepository
import net.ktnx.mobileledger.data.repository.OptionRepository
import net.ktnx.mobileledger.data.repository.TransactionRepository
import net.ktnx.mobileledger.domain.model.Account
import net.ktnx.mobileledger.domain.model.AccountAmount
import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.domain.model.TransactionLine
import net.ktnx.mobileledger.util.createTestDomainProfile
import net.ktnx.mobileledger.utils.SimpleDate
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [SyncPersistenceImpl].
 *
 * Tests verify:
 * - Account state preservation during save
 * - Transaction storage
 * - Timestamp updates
 * - Error handling for unsaved profiles
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncPersistenceImplTest {

    private lateinit var mockAccountRepository: AccountRepository
    private lateinit var mockTransactionRepository: TransactionRepository
    private lateinit var mockOptionRepository: OptionRepository
    private lateinit var persistence: SyncPersistenceImpl

    private val testProfile = createTestDomainProfile(
        id = 1L,
        name = "Test Profile",
        url = "https://test.example.com"
    )

    private val testAccounts = listOf(
        Account(
            id = null,
            name = "Assets:Bank",
            level = 1,
            isExpanded = true,
            isVisible = true,
            amounts = listOf(AccountAmount(currency = "USD", amount = 1000.0f))
        ),
        Account(
            id = null,
            name = "Expenses:Food",
            level = 1,
            isExpanded = true,
            isVisible = true,
            amounts = emptyList()
        )
    )

    private val testTransactions = listOf(
        Transaction(
            id = null,
            ledgerId = 1L,
            date = SimpleDate.today(),
            description = "Grocery shopping",
            comment = null,
            lines = listOf(
                TransactionLine(
                    id = null,
                    accountName = "Expenses:Food",
                    amount = 50.0f,
                    currency = "USD",
                    comment = null
                ),
                TransactionLine(
                    id = null,
                    accountName = "Assets:Bank",
                    amount = -50.0f,
                    currency = "USD",
                    comment = null
                )
            )
        )
    )

    @Before
    fun setup() {
        mockAccountRepository = mockk(relaxed = true)
        mockTransactionRepository = mockk(relaxed = true)
        mockOptionRepository = mockk(relaxed = true)

        persistence = SyncPersistenceImpl(
            accountRepository = mockAccountRepository,
            transactionRepository = mockTransactionRepository,
            optionRepository = mockOptionRepository
        )
    }

    @Test
    fun `saveAccountsAndTransactions stores accounts`() = runTest {
        // Given
        coEvery { mockAccountRepository.getByNameWithAmounts(any(), any()) } returns Result.success(null)
        coEvery { mockAccountRepository.storeAccountsAsDomain(any(), any()) } returns Result.success(Unit)
        coEvery { mockTransactionRepository.storeTransactionsAsDomain(any(), any()) } returns Result.success(Unit)
        coEvery { mockOptionRepository.setLastSyncTimestamp(any(), any()) } just Runs

        // When
        persistence.saveAccountsAndTransactions(testProfile, testAccounts, testTransactions)

        // Then
        coVerify { mockAccountRepository.storeAccountsAsDomain(any(), 1L) }
    }

    @Test
    fun `saveAccountsAndTransactions stores transactions`() = runTest {
        // Given
        coEvery { mockAccountRepository.getByNameWithAmounts(any(), any()) } returns Result.success(null)
        coEvery { mockAccountRepository.storeAccountsAsDomain(any(), any()) } returns Result.success(Unit)
        coEvery { mockTransactionRepository.storeTransactionsAsDomain(any(), any()) } returns Result.success(Unit)
        coEvery { mockOptionRepository.setLastSyncTimestamp(any(), any()) } just Runs

        // When
        persistence.saveAccountsAndTransactions(testProfile, testAccounts, testTransactions)

        // Then
        coVerify { mockTransactionRepository.storeTransactionsAsDomain(testTransactions, 1L) }
    }

    @Test
    fun `saveAccountsAndTransactions updates timestamp`() = runTest {
        // Given
        coEvery { mockAccountRepository.getByNameWithAmounts(any(), any()) } returns Result.success(null)
        coEvery { mockAccountRepository.storeAccountsAsDomain(any(), any()) } returns Result.success(Unit)
        coEvery { mockTransactionRepository.storeTransactionsAsDomain(any(), any()) } returns Result.success(Unit)
        coEvery { mockOptionRepository.setLastSyncTimestamp(any(), any()) } just Runs

        // When
        persistence.saveAccountsAndTransactions(testProfile, testAccounts, testTransactions)

        // Then
        coVerify { mockOptionRepository.setLastSyncTimestamp(eq(1L), any()) }
    }

    @Test
    fun `saveAccountsAndTransactions throws for unsaved profile`() = runTest {
        // Given
        val unsavedProfile = testProfile.copy(id = null)

        // When/Then
        assertThrows(IllegalStateException::class.java) {
            runTest {
                persistence.saveAccountsAndTransactions(unsavedProfile, testAccounts, testTransactions)
            }
        }
    }

    @Test
    fun `saveAccountsAndTransactions preserves existing account state`() = runTest {
        // Given
        val existingAccount = Account(
            id = 10L,
            name = "Assets:Bank",
            level = 1,
            isExpanded = false, // Different state
            isVisible = true,
            amounts = emptyList()
        )
        coEvery {
            mockAccountRepository.getByNameWithAmounts(1L, "Assets:Bank")
        } returns Result.success(existingAccount)
        coEvery {
            mockAccountRepository.getByNameWithAmounts(1L, "Expenses:Food")
        } returns Result.success(null)
        coEvery { mockAccountRepository.storeAccountsAsDomain(any(), any()) } returns Result.success(Unit)
        coEvery { mockTransactionRepository.storeTransactionsAsDomain(any(), any()) } returns Result.success(Unit)
        coEvery { mockOptionRepository.setLastSyncTimestamp(any(), any()) } just Runs

        // When
        persistence.saveAccountsAndTransactions(testProfile, testAccounts, testTransactions)

        // Then
        coVerify {
            mockAccountRepository.getByNameWithAmounts(1L, "Assets:Bank")
            mockAccountRepository.getByNameWithAmounts(1L, "Expenses:Food")
        }
    }

    @Test
    fun `saveAccountsAndTransactions propagates repository errors`() = runTest {
        // Given
        coEvery { mockAccountRepository.getByNameWithAmounts(any(), any()) } returns Result.success(null)
        coEvery {
            mockAccountRepository.storeAccountsAsDomain(any(), any())
        } returns Result.failure(RuntimeException("Storage error"))

        // When/Then
        assertThrows(RuntimeException::class.java) {
            runTest {
                persistence.saveAccountsAndTransactions(testProfile, testAccounts, testTransactions)
            }
        }
    }
}
