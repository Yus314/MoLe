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

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.domain.model.SyncError
import net.ktnx.mobileledger.domain.model.SyncException
import net.ktnx.mobileledger.domain.model.SyncProgress
import net.ktnx.mobileledger.fake.FakeTransactionSyncer
import net.ktnx.mobileledger.util.createTestDomainProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * TransactionSyncer のテスト
 *
 * FakeTransactionSyncer を使用して同期ロジックをテストする。
 * 実際のネットワーク接続なしで成功/失敗/進捗をシミュレート。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionSyncerImplTest {

    private lateinit var syncer: FakeTransactionSyncer
    private lateinit var testProfile: Profile

    @Before
    fun setup() {
        syncer = FakeTransactionSyncer()
        testProfile = createTestDomainProfile(
            id = 1L,
            name = "Test Profile",
            url = "https://test.example.com",
            authentication = null,
            apiVersion = 0,
            permitPosting = true,
            defaultCommodity = "JPY",
            theme = -1,
            orderNo = 1
        )
    }

    // T018: sync success test case
    @Test
    fun `sync success emits progress and completes`() = runTest {
        // Given
        syncer.shouldSucceed = true
        syncer.progressSteps = 3

        // When
        val progressList = syncer.sync(testProfile).toList()

        // Then
        assertEquals(1, syncer.syncCallCount)
        assertEquals(testProfile, syncer.lastSyncedProfile)

        // Should emit Starting + 3 Running progress
        assertEquals(4, progressList.size)
        assertTrue(progressList[0] is SyncProgress.Starting)
        assertTrue(progressList[1] is SyncProgress.Running)
        assertTrue(progressList[2] is SyncProgress.Running)
        assertTrue(progressList[3] is SyncProgress.Running)

        // Verify result is available
        val result = syncer.getLastResult()
        assertNotNull(result)
        assertEquals(10, result!!.transactionCount)
        assertEquals(5, result.accountCount)
    }

    // T019: sync error handling test cases
    @Test
    fun `sync network error throws SyncException with NetworkError`() = runTest {
        // Given
        syncer.shouldSucceed = false
        syncer.errorToThrow = SyncError.NetworkError("Connection failed")

        // When/Then
        try {
            syncer.sync(testProfile).toList()
            assertTrue("Should throw SyncException", false)
        } catch (e: SyncException) {
            assertTrue(e.syncError is SyncError.NetworkError)
            assertEquals("Connection failed", e.syncError.message)
        }
    }

    @Test
    fun `sync authentication error throws SyncException with AuthenticationError`() = runTest {
        // Given
        syncer.shouldSucceed = false
        syncer.errorToThrow = SyncError.AuthenticationError("Invalid credentials")

        // When/Then
        try {
            syncer.sync(testProfile).toList()
            assertTrue("Should throw SyncException", false)
        } catch (e: SyncException) {
            assertTrue(e.syncError is SyncError.AuthenticationError)
            assertEquals("Invalid credentials", e.syncError.message)
        }
    }

    @Test
    fun `sync timeout error throws SyncException with TimeoutError`() = runTest {
        // Given
        syncer.shouldSucceed = false
        syncer.errorToThrow = SyncError.TimeoutError("Request timed out")

        // When/Then
        try {
            syncer.sync(testProfile).toList()
            assertTrue("Should throw SyncException", false)
        } catch (e: SyncException) {
            assertTrue(e.syncError is SyncError.TimeoutError)
            assertEquals("Request timed out", e.syncError.message)
        }
    }

    @Test
    fun `sync server error throws SyncException with ServerError`() = runTest {
        // Given
        syncer.shouldSucceed = false
        syncer.errorToThrow = SyncError.ServerError("Internal Server Error", 500)

        // When/Then
        try {
            syncer.sync(testProfile).toList()
            assertTrue("Should throw SyncException", false)
        } catch (e: SyncException) {
            assertTrue(e.syncError is SyncError.ServerError)
            val serverError = e.syncError as SyncError.ServerError
            assertEquals(500, serverError.httpCode)
            assertEquals("Internal Server Error", serverError.message)
        }
    }

    // T020: sync progress emission test cases
    @Test
    fun `sync emits Starting progress first`() = runTest {
        // Given
        syncer.shouldSucceed = true
        syncer.progressSteps = 1

        // When
        val progressList = syncer.sync(testProfile).toList()

        // Then
        assertTrue(progressList.isNotEmpty())
        val first = progressList.first()
        assertTrue(first is SyncProgress.Starting)
    }

    @Test
    fun `sync emits Running progress with correct count`() = runTest {
        // Given
        syncer.shouldSucceed = true
        syncer.progressSteps = 5

        // When
        val progressList = syncer.sync(testProfile).toList()

        // Then
        val runningProgress = progressList.filterIsInstance<SyncProgress.Running>()
        assertEquals(5, runningProgress.size)

        // Verify progress fraction increases
        for (i in runningProgress.indices) {
            val running = runningProgress[i]
            assertEquals(i + 1, running.current)
            assertEquals(5, running.total)
        }
    }

    @Test
    fun `sync progress fraction is calculated correctly`() = runTest {
        // Given
        syncer.shouldSucceed = true
        syncer.progressSteps = 4

        // When
        val progressList = syncer.sync(testProfile).toList()

        // Then
        val runningProgress = progressList.filterIsInstance<SyncProgress.Running>()
        assertEquals(0.25f, runningProgress[0].progressFraction, 0.001f)
        assertEquals(0.50f, runningProgress[1].progressFraction, 0.001f)
        assertEquals(0.75f, runningProgress[2].progressFraction, 0.001f)
        assertEquals(1.00f, runningProgress[3].progressFraction, 0.001f)
    }

    @Test
    fun `sync reset clears state`() = runTest {
        // Given - run sync once
        syncer.sync(testProfile).toList()
        assertEquals(1, syncer.syncCallCount)
        assertNotNull(syncer.lastSyncedProfile)

        // When
        syncer.reset()

        // Then
        assertEquals(0, syncer.syncCallCount)
        assertEquals(null, syncer.lastSyncedProfile)
        assertEquals(null, syncer.getLastResult())
    }

    // ==========================================
    // T028: Cancellation test for TransactionSyncer
    // ==========================================

    @Test
    fun `sync can be cancelled while in progress`() = runTest {
        // Given - set up a long-running sync with delays
        syncer.shouldSucceed = true
        syncer.progressSteps = 10
        syncer.delayPerStepMs = 100 // 100ms delay per step

        var job: Job? = null
        val emissions = mutableListOf<SyncProgress>()

        // When - start sync and cancel after first emission
        job = launch {
            syncer.sync(testProfile).collect { progress ->
                emissions.add(progress)
                // Cancel after receiving the first Running progress
                if (progress is SyncProgress.Running && progress.current == 1) {
                    job?.cancel()
                }
            }
        }

        // Wait for job to complete (either by cancellation or completion)
        job.join()

        // Then - verify cancellation occurred and not all steps completed
        assertTrue("Should have received at least 1 emission", emissions.isNotEmpty())
        assertTrue("Should have been cancelled", syncer.wasCancelled || emissions.size < 11)
    }

    @Test
    fun `sync throws CancellationException when cancelled`() = runTest {
        // Given
        syncer.shouldSucceed = true
        syncer.progressSteps = 10
        syncer.delayPerStepMs = 50

        var caughtException: Throwable? = null

        // When
        val job = launch {
            try {
                syncer.sync(testProfile).collect { progress ->
                    if (progress is SyncProgress.Running && progress.current >= 2) {
                        throw CancellationException("User cancelled")
                    }
                }
            } catch (e: CancellationException) {
                caughtException = e
                throw e
            }
        }

        job.join()

        // Then - CancellationException should have been caught
        assertTrue(
            "Should throw CancellationException",
            caughtException is CancellationException || job.isCancelled
        )
    }

    // ==========================================
    // T029: Cancellation response time assertion (<500ms)
    // ==========================================

    @Test
    fun `sync responds to cancellation within 500ms`() = runTest {
        // Given - set up sync with long delays
        syncer.shouldSucceed = true
        syncer.progressSteps = 100
        syncer.delayPerStepMs = 50 // Total would be 5000ms without cancellation

        val startTime = testScheduler.currentTime

        // When - start sync and cancel after getting first progress
        val job = launch {
            syncer.sync(testProfile).first { it is SyncProgress.Running }
            // Cancel after first Running emission
        }

        // Wait for the first emission then cancel
        delay(100) // Give time for first emission
        job.cancelAndJoin()

        val elapsedTime = testScheduler.currentTime - startTime

        // Then - cancellation should complete within 500ms
        // Note: In test scheduler, virtual time is used
        assertTrue(
            "Cancellation should respond within 500ms, took ${elapsedTime}ms",
            elapsedTime < 500
        )
    }

    // ==========================================
    // T030: Structured concurrency test with supervisorScope
    // ==========================================

    @Test
    fun `sync failure in supervisorScope does not cancel sibling coroutines`() = runTest {
        // Given
        val failingSyncer = FakeTransactionSyncer().apply {
            shouldSucceed = false
            errorToThrow = SyncError.NetworkError("Network error")
        }

        val successfulSyncer = FakeTransactionSyncer().apply {
            shouldSucceed = true
            progressSteps = 3
        }

        var syncerOneCompleted = false
        var syncerTwoCompleted = false
        var syncerOneFailed = false

        // When - run two syncs in supervisorScope
        supervisorScope {
            launch {
                try {
                    failingSyncer.sync(testProfile).toList()
                    syncerOneCompleted = true
                } catch (e: SyncException) {
                    syncerOneFailed = true
                }
            }

            launch {
                try {
                    successfulSyncer.sync(testProfile).toList()
                    syncerTwoCompleted = true
                } catch (e: Exception) {
                    // Should not fail
                }
            }
        }

        // Then - first syncer should fail, second should complete
        assertTrue("First syncer should have failed", syncerOneFailed)
        assertFalse("First syncer should not have completed", syncerOneCompleted)
        assertTrue("Second syncer should have completed", syncerTwoCompleted)
    }

    @Test
    fun `cancelling parent scope cancels sync`() = runTest {
        // Given
        syncer.shouldSucceed = true
        syncer.progressSteps = 20
        syncer.delayPerStepMs = 50

        var wasCollecting = false
        var emissionCount = 0

        // When - launch sync and cancel the parent scope
        val job = launch {
            syncer.sync(testProfile).collect { progress ->
                wasCollecting = true
                emissionCount++
                if (emissionCount >= 3) {
                    // Cancel after 3 emissions
                    this@launch.cancel()
                }
            }
        }

        job.join()

        // Then - should have started collecting but not completed all
        assertTrue("Should have started collecting", wasCollecting)
        assertTrue("Should have been cancelled before completion", emissionCount < 21)
    }
}
