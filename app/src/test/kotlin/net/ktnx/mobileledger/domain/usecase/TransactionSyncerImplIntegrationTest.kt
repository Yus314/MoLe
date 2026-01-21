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

package net.ktnx.mobileledger.domain.usecase

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.domain.model.Account
import net.ktnx.mobileledger.domain.model.SyncError
import net.ktnx.mobileledger.domain.model.SyncException
import net.ktnx.mobileledger.domain.model.SyncProgress
import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.domain.model.TransactionLine
import net.ktnx.mobileledger.domain.usecase.sync.AccountFetchResult
import net.ktnx.mobileledger.domain.usecase.sync.AccountListFetcher
import net.ktnx.mobileledger.domain.usecase.sync.LegacyHtmlParser
import net.ktnx.mobileledger.domain.usecase.sync.LegacyParseResult
import net.ktnx.mobileledger.domain.usecase.sync.SyncExceptionMapper
import net.ktnx.mobileledger.domain.usecase.sync.SyncPersistence
import net.ktnx.mobileledger.domain.usecase.sync.TransactionListFetcher
import net.ktnx.mobileledger.service.AppStateService
import net.ktnx.mobileledger.util.createTestDomainProfile
import net.ktnx.mobileledger.utils.SimpleDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for [TransactionSyncerImpl] using MockK.
 *
 * Tests verify:
 * - JSON API success path
 * - HTML fallback when JSON not available
 * - Progress emissions
 * - Error handling and exception mapping
 * - Persistence calls
 * - AppStateService updates
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionSyncerImplIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockAccountListFetcher: AccountListFetcher
    private lateinit var mockTransactionListFetcher: TransactionListFetcher
    private lateinit var mockLegacyHtmlParser: LegacyHtmlParser
    private lateinit var mockSyncPersistence: SyncPersistence
    private lateinit var mockAppStateService: AppStateService
    private lateinit var syncExceptionMapper: SyncExceptionMapper
    private lateinit var syncer: TransactionSyncerImpl

    private val testProfile = createTestDomainProfile(
        id = 1L,
        name = "Test Profile",
        url = "https://test.example.com"
    )

    private val testAccounts = listOf(
        Account(
            name = "Assets:Bank",
            level = 1,
            isExpanded = true,
            amounts = emptyList()
        ),
        Account(
            name = "Expenses:Food",
            level = 1,
            isExpanded = true,
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
        mockAccountListFetcher = mockk(relaxed = true)
        mockTransactionListFetcher = mockk(relaxed = true)
        mockLegacyHtmlParser = mockk(relaxed = true)
        mockSyncPersistence = mockk(relaxed = true)
        mockAppStateService = mockk(relaxed = true)
        syncExceptionMapper = SyncExceptionMapper()

        syncer = TransactionSyncerImpl(
            accountListFetcher = mockAccountListFetcher,
            transactionListFetcher = mockTransactionListFetcher,
            legacyHtmlParser = mockLegacyHtmlParser,
            syncPersistence = mockSyncPersistence,
            syncExceptionMapper = syncExceptionMapper,
            appStateService = mockAppStateService,
            ioDispatcher = testDispatcher
        )
    }

    // ========================================
    // JSON API success tests
    // ========================================

    @Test
    fun `sync via JSON API emits progress and completes successfully`() = runTest(testDispatcher) {
        // Given
        coEvery { mockAccountListFetcher.fetch(testProfile) } returns AccountFetchResult(
            accounts = testAccounts,
            expectedPostingsCount = 10
        )
        coEvery {
            mockTransactionListFetcher.fetch(testProfile, 10, any())
        } returns testTransactions
        coEvery { mockSyncPersistence.saveAccountsAndTransactions(any(), any(), any()) } just Runs
        every { mockAppStateService.updateSyncInfo(any()) } just Runs

        // When
        val progressList = syncer.sync(testProfile).toList()

        // Then
        assertTrue("Should emit Starting progress", progressList.any { it is SyncProgress.Starting })
        assertTrue("Should emit Indeterminate progress", progressList.any { it is SyncProgress.Indeterminate })

        // Verify persistence was called
        coVerify { mockSyncPersistence.saveAccountsAndTransactions(testProfile, testAccounts, testTransactions) }

        // Verify AppStateService was updated
        verify { mockAppStateService.updateSyncInfo(any()) }

        // Verify result
        val result = syncer.getLastResult()
        assertNotNull("Should have result", result)
        assertEquals("Should have 1 transaction", 1, result!!.transactionCount)
        assertEquals("Should have 2 accounts", 2, result.accountCount)
    }

    @Test
    fun `sync uses transaction progress callback for Running emissions`() = runTest(testDispatcher) {
        // Given
        coEvery { mockAccountListFetcher.fetch(testProfile) } returns AccountFetchResult(
            accounts = testAccounts,
            expectedPostingsCount = 5
        )
        coEvery {
            mockTransactionListFetcher.fetch(testProfile, 5, any())
        } coAnswers {
            val onProgress = thirdArg<suspend (Int, Int) -> Unit>()
            // Simulate progress emissions
            onProgress(1, 5)
            onProgress(2, 5)
            onProgress(3, 5)
            onProgress(4, 5)
            onProgress(5, 5)
            testTransactions
        }
        coEvery { mockSyncPersistence.saveAccountsAndTransactions(any(), any(), any()) } just Runs
        every { mockAppStateService.updateSyncInfo(any()) } just Runs

        // When
        val progressList = syncer.sync(testProfile).toList()

        // Then
        val runningProgress = progressList.filterIsInstance<SyncProgress.Running>()
        assertEquals("Should have 5 Running progress items", 5, runningProgress.size)

        // Verify progress fractions
        assertEquals(0.2f, runningProgress[0].progressFraction, 0.01f)
        assertEquals(0.4f, runningProgress[1].progressFraction, 0.01f)
        assertEquals(1.0f, runningProgress[4].progressFraction, 0.01f)
    }

    // ========================================
    // HTML fallback tests
    // ========================================

    @Test
    fun `sync falls back to HTML when JSON account fetch returns null`() = runTest(testDispatcher) {
        // Given
        coEvery { mockAccountListFetcher.fetch(testProfile) } returns null
        coEvery {
            mockLegacyHtmlParser.parse(testProfile, -1, any())
        } returns LegacyParseResult(
            accounts = testAccounts,
            transactions = testTransactions
        )
        coEvery { mockSyncPersistence.saveAccountsAndTransactions(any(), any(), any()) } just Runs
        every { mockAppStateService.updateSyncInfo(any()) } just Runs

        // When
        val progressList = syncer.sync(testProfile).toList()

        // Then
        // Should NOT call transactionListFetcher
        coVerify(exactly = 0) { mockTransactionListFetcher.fetch(any(), any(), any()) }

        // Should call legacyHtmlParser
        coVerify { mockLegacyHtmlParser.parse(testProfile, -1, any()) }

        // Verify persistence was called with HTML data
        coVerify { mockSyncPersistence.saveAccountsAndTransactions(testProfile, testAccounts, testTransactions) }

        // Should emit HTML mode progress
        val indeterminateProgress = progressList.filterIsInstance<SyncProgress.Indeterminate>()
        assertTrue(
            "Should have HTML mode progress",
            indeterminateProgress.any { it.message.contains("HTML") }
        )
    }

    @Test
    fun `sync falls back to HTML when JSON transaction fetch returns null`() = runTest(testDispatcher) {
        // Given
        coEvery { mockAccountListFetcher.fetch(testProfile) } returns AccountFetchResult(
            accounts = testAccounts,
            expectedPostingsCount = 10
        )
        coEvery { mockTransactionListFetcher.fetch(testProfile, 10, any()) } returns null
        coEvery {
            mockLegacyHtmlParser.parse(testProfile, -1, any())
        } returns LegacyParseResult(
            accounts = testAccounts,
            transactions = testTransactions
        )
        coEvery { mockSyncPersistence.saveAccountsAndTransactions(any(), any(), any()) } just Runs
        every { mockAppStateService.updateSyncInfo(any()) } just Runs

        // When
        syncer.sync(testProfile).toList()

        // Then
        coVerify { mockLegacyHtmlParser.parse(testProfile, -1, any()) }
    }

    // ========================================
    // Error handling tests
    // ========================================

    @Test
    fun `sync throws SyncException with NetworkError on IOException`() = runTest(testDispatcher) {
        // Given
        coEvery { mockAccountListFetcher.fetch(testProfile) } throws IOException("Network unreachable")

        // When/Then
        try {
            syncer.sync(testProfile).toList()
            assertTrue("Should throw SyncException", false)
        } catch (e: SyncException) {
            assertTrue("Should be NetworkError", e.syncError is SyncError.NetworkError)
        }
    }

    @Test
    fun `sync throws SyncException with TimeoutError on SocketTimeoutException`() = runTest(testDispatcher) {
        // Given
        coEvery {
            mockAccountListFetcher.fetch(testProfile)
        } throws java.net.SocketTimeoutException("Read timed out")

        // When/Then
        try {
            syncer.sync(testProfile).toList()
            assertTrue("Should throw SyncException", false)
        } catch (e: SyncException) {
            assertTrue("Should be TimeoutError", e.syncError is SyncError.TimeoutError)
        }
    }

    @Test
    fun `sync exception during persistence is mapped correctly`() = runTest(testDispatcher) {
        // Given
        coEvery { mockAccountListFetcher.fetch(testProfile) } returns AccountFetchResult(
            accounts = testAccounts,
            expectedPostingsCount = 10
        )
        coEvery { mockTransactionListFetcher.fetch(testProfile, 10, any()) } returns testTransactions
        coEvery {
            mockSyncPersistence.saveAccountsAndTransactions(any(), any(), any())
        } throws IOException("Database write failed")

        // When/Then
        try {
            syncer.sync(testProfile).toList()
            assertTrue("Should throw SyncException", false)
        } catch (e: SyncException) {
            assertTrue("Should be NetworkError for IOException", e.syncError is SyncError.NetworkError)
        }
    }

    // ========================================
    // Result tracking tests
    // ========================================

    @Test
    fun `getLastResult returns null before first sync`() {
        assertNull("Should be null initially", syncer.getLastResult())
    }

    @Test
    fun `getLastResult contains correct counts after successful sync`() = runTest(testDispatcher) {
        // Given
        val moreAccounts = testAccounts + Account(
            name = "Liabilities:CreditCard",
            level = 1,
            isExpanded = true,
            amounts = emptyList()
        )
        val moreTransactions = testTransactions + testTransactions.first().copy(ledgerId = 2L)

        coEvery { mockAccountListFetcher.fetch(testProfile) } returns AccountFetchResult(
            accounts = moreAccounts,
            expectedPostingsCount = 10
        )
        coEvery { mockTransactionListFetcher.fetch(testProfile, 10, any()) } returns moreTransactions
        coEvery { mockSyncPersistence.saveAccountsAndTransactions(any(), any(), any()) } just Runs
        every { mockAppStateService.updateSyncInfo(any()) } just Runs

        // When
        syncer.sync(testProfile).toList()

        // Then
        val result = syncer.getLastResult()
        assertNotNull(result)
        assertEquals(2, result!!.transactionCount)
        assertEquals(3, result.accountCount)
        assertTrue("Duration should be non-negative", result.duration >= 0)
    }

    // ========================================
    // Progress order tests
    // ========================================

    @Test
    fun `sync progress emissions are in correct order`() = runTest(testDispatcher) {
        // Given
        coEvery { mockAccountListFetcher.fetch(testProfile) } returns AccountFetchResult(
            accounts = testAccounts,
            expectedPostingsCount = 3
        )
        coEvery {
            mockTransactionListFetcher.fetch(testProfile, 3, any())
        } coAnswers {
            val onProgress = thirdArg<suspend (Int, Int) -> Unit>()
            onProgress(1, 3)
            onProgress(2, 3)
            onProgress(3, 3)
            testTransactions
        }
        coEvery { mockSyncPersistence.saveAccountsAndTransactions(any(), any(), any()) } just Runs
        every { mockAppStateService.updateSyncInfo(any()) } just Runs

        // When
        val progressList = syncer.sync(testProfile).toList()

        // Then - verify order
        assertTrue("First should be Starting", progressList[0] is SyncProgress.Starting)

        // Account fetch progress (Indeterminate)
        val accountFetchIndex = progressList.indexOfFirst {
            it is SyncProgress.Indeterminate && it.message.contains("アカウント")
        }
        assertTrue("Should have account fetch progress", accountFetchIndex > 0)

        // Transaction fetch progress (Indeterminate then Running)
        val txFetchIndex = progressList.indexOfFirst {
            it is SyncProgress.Indeterminate && it.message.contains("取引")
        }
        assertTrue("Transaction fetch should be after account fetch", txFetchIndex > accountFetchIndex)

        // Running progress for transaction processing
        val runningIndices = progressList.mapIndexedNotNull { idx, p ->
            if (p is SyncProgress.Running) idx else null
        }
        assertTrue("Running should be after transaction fetch start", runningIndices.all { it > txFetchIndex })

        // Save progress (Indeterminate)
        val saveIndex = progressList.indexOfFirst {
            it is SyncProgress.Indeterminate && it.message.contains("保存")
        }
        assertTrue("Save should be after Running", saveIndex > runningIndices.last())
    }
}
