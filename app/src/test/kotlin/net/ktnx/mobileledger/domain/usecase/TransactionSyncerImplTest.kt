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
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.domain.model.SyncError
import net.ktnx.mobileledger.domain.model.SyncException
import net.ktnx.mobileledger.domain.model.SyncProgress
import net.ktnx.mobileledger.fake.FakeTransactionSyncer
import org.junit.Assert.assertEquals
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
        testProfile = Profile().apply {
            id = 1L
            name = "Test Profile"
            uuid = "test-uuid"
            url = "https://test.example.com"
            useAuthentication = false
            authUser = null
            authPassword = null
            apiVersion = 0
            permitPosting = true
            showCommentsByDefault = true
            setDefaultCommodity("JPY")
            theme = -1
            orderNo = 1
            showCommodityByDefault = true
            futureDates = 0
            preferredAccountsFilter = null
        }
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
}
