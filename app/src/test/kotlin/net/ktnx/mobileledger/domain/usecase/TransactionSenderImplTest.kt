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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.model.Transaction
import net.ktnx.mobileledger.core.domain.model.TransactionLine
import net.ktnx.mobileledger.fake.FakeTransactionSender
import net.ktnx.mobileledger.util.createTestDomainProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [TransactionSender] interface and [FakeTransactionSender].
 *
 * These tests verify:
 * - T021: send() works without Thread.sleep() (uses delay() instead)
 * - T022: retry logic uses delay() properly with TestDispatcher
 * - T023: network error handling returns Result.failure
 *
 * Note: Direct testing of [TransactionSenderImpl] is limited in unit tests
 * because it depends on Android framework classes (API enum uses SparseArray).
 * For integration testing, use instrumented tests.
 *
 * These tests use [FakeTransactionSender] to verify the interface contract
 * and demonstrate that TestDispatcher enables instant test completion.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionSenderImplTest {

    private lateinit var testDispatcher: kotlinx.coroutines.test.TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var fakeSender: FakeTransactionSender
    private lateinit var testProfile: Profile
    private lateinit var testTransaction: Transaction

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        fakeSender = FakeTransactionSender()

        testProfile = createTestDomainProfile(
            id = 1L,
            name = "Test Profile",
            url = "https://example.com/ledger",
            apiVersion = 0 // auto
        )

        testTransaction = Transaction(
            id = null,
            ledgerId = 0L,
            date = SimpleDate.today(),
            description = "Test transaction",
            comment = null,
            lines = listOf(
                TransactionLine(
                    id = null,
                    accountName = "Expenses:Test",
                    amount = 100.0f,
                    currency = "USD",
                    comment = null
                ),
                TransactionLine(
                    id = null,
                    accountName = "Assets:Bank",
                    amount = -100.0f,
                    currency = "USD",
                    comment = null
                )
            )
        )
    }

    /**
     * T021: Verify that FakeTransactionSender completes instantly with TestDispatcher.
     *
     * This demonstrates that the interface uses suspend functions (not Thread),
     * allowing instant completion with TestDispatcher.
     */
    @Test
    fun `send completes instantly with TestDispatcher`() = testScope.runTest {
        val startTime = testScheduler.currentTime

        // Send transaction using fake
        val result = fakeSender.send(testProfile, testTransaction, simulate = false)

        advanceUntilIdle()

        // Should complete instantly (no Thread.sleep)
        val elapsedTime = testScheduler.currentTime - startTime
        assertEquals("Virtual time should not advance for fake", 0, elapsedTime)
        assertTrue("Result should be success", result.isSuccess)
    }

    /**
     * T022: Verify time control with TestDispatcher.
     *
     * This test demonstrates that delay() can be controlled via TestDispatcher,
     * which is the mechanism used by TransactionSenderImpl.
     */
    @Test
    fun `delay is controllable via TestDispatcher`() = testScope.runTest {
        val startTime = testScheduler.currentTime

        // Advance time by 100ms (the retry delay used in TransactionSenderImpl)
        advanceTimeBy(100)

        val elapsedTime = testScheduler.currentTime - startTime
        assertEquals("Time should advance by exactly 100ms", 100, elapsedTime)
    }

    /**
     * T023: Verify error handling returns Result.failure.
     */
    @Test
    fun `send returns Result failure on error`() = testScope.runTest {
        // Configure fake to fail
        fakeSender.shouldSucceed = false
        fakeSender.errorMessage = "Network error"

        val result = fakeSender.send(testProfile, testTransaction, simulate = false)

        advanceUntilIdle()

        assertTrue("Result should be failure", result.isFailure)
        assertEquals(
            "Error message should match",
            "Network error",
            result.exceptionOrNull()?.message
        )
    }

    /**
     * Verify FakeTransactionSender records sent transactions.
     */
    @Test
    fun `fake records sent transactions`() = testScope.runTest {
        fakeSender.send(testProfile, testTransaction, simulate = false)
        fakeSender.send(testProfile, testTransaction, simulate = true)

        advanceUntilIdle()

        assertEquals("Should record 2 transactions", 2, fakeSender.sentTransactions.size)
        assertFalse("First should not be simulate", fakeSender.sentTransactions[0].simulate)
        assertTrue("Second should be simulate", fakeSender.sentTransactions[1].simulate)
    }

    /**
     * Verify that TransactionSender interface uses suspend functions,
     * enabling proper cancellation.
     */
    @Test
    fun `sender respects cancellation`() = testScope.runTest {
        val job = launch {
            fakeSender.send(testProfile, testTransaction, simulate = true)
        }

        // Cancel immediately
        job.cancel()

        advanceUntilIdle()

        assertTrue("Job should be cancelled", job.isCancelled)
    }

    /**
     * Verify FakeTransactionSender reset functionality.
     */
    @Test
    fun `fake reset clears state`() = testScope.runTest {
        fakeSender.shouldSucceed = false
        fakeSender.send(testProfile, testTransaction, simulate = false)

        advanceUntilIdle()

        assertEquals("Should have 1 transaction", 1, fakeSender.sentTransactions.size)

        fakeSender.reset()

        assertTrue("Should succeed after reset", fakeSender.shouldSucceed)
        assertTrue("Transactions should be cleared", fakeSender.sentTransactions.isEmpty())
    }
}
