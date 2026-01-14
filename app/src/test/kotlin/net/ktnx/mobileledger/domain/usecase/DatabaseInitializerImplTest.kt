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
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.fake.FakeDatabaseInitializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * DatabaseInitializer のテスト
 *
 * FakeDatabaseInitializer を使用して初期化ロジックをテストする。
 * T060-T063: 初期化成功/失敗のテストケース。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DatabaseInitializerImplTest {

    private lateinit var databaseInitializer: FakeDatabaseInitializer

    @Before
    fun setup() {
        databaseInitializer = FakeDatabaseInitializer()
    }

    // ==========================================
    // T061: Initialization success test case (hasProfiles=true)
    // ==========================================

    @Test
    fun `initialize success with profiles returns Result success with true`() = runTest {
        // Given
        databaseInitializer.shouldSucceed = true
        databaseInitializer.hasProfiles = true

        // When
        val result = databaseInitializer.initialize()

        // Then
        assertTrue("Initialize should succeed", result.isSuccess)
        assertTrue("hasProfiles should be true", result.getOrNull() == true)
        assertEquals(1, databaseInitializer.initializeCallCount)
        assertTrue("isInitialized should be true", databaseInitializer.isInitialized)
    }

    // ==========================================
    // T062: Initialization success test case (hasProfiles=false)
    // ==========================================

    @Test
    fun `initialize success without profiles returns Result success with false`() = runTest {
        // Given
        databaseInitializer.shouldSucceed = true
        databaseInitializer.hasProfiles = false

        // When
        val result = databaseInitializer.initialize()

        // Then
        assertTrue("Initialize should succeed", result.isSuccess)
        assertFalse("hasProfiles should be false", result.getOrNull() == true)
        assertEquals(1, databaseInitializer.initializeCallCount)
        assertTrue("isInitialized should be true", databaseInitializer.isInitialized)
    }

    // ==========================================
    // T063: Initialization failure test case
    // ==========================================

    @Test
    fun `initialize failure returns Result failure`() = runTest {
        // Given
        databaseInitializer.shouldSucceed = false

        // When
        val result = databaseInitializer.initialize()

        // Then
        assertTrue("Initialize should fail", result.isFailure)
        assertNotNull(result.exceptionOrNull())
        assertEquals(1, databaseInitializer.initializeCallCount)
        assertFalse("isInitialized should be false", databaseInitializer.isInitialized)
    }

    @Test
    fun `initialize with specific error returns correct exception`() = runTest {
        // Given
        val expectedException = RuntimeException("Database migration failed")
        databaseInitializer.errorToThrow = expectedException

        // When
        val result = databaseInitializer.initialize()

        // Then
        assertTrue("Initialize should fail", result.isFailure)
        val actualException = result.exceptionOrNull()
        assertTrue(
            "Should be RuntimeException",
            actualException is RuntimeException
        )
        assertEquals("Database migration failed", actualException?.message)
    }

    // ==========================================
    // isInitialized tests
    // ==========================================

    @Test
    fun `isInitialized is false before initialize is called`() = runTest {
        // Given - no initialize call

        // Then
        assertFalse("isInitialized should be false", databaseInitializer.isInitialized)
    }

    @Test
    fun `isInitialized is true after successful initialize`() = runTest {
        // Given
        databaseInitializer.shouldSucceed = true

        // When
        databaseInitializer.initialize()

        // Then
        assertTrue("isInitialized should be true", databaseInitializer.isInitialized)
    }

    @Test
    fun `isInitialized remains false after failed initialize`() = runTest {
        // Given
        databaseInitializer.shouldSucceed = false

        // When
        databaseInitializer.initialize()

        // Then
        assertFalse("isInitialized should remain false", databaseInitializer.isInitialized)
    }

    // ==========================================
    // Multiple call tests
    // ==========================================

    @Test
    fun `initialize can be called multiple times`() = runTest {
        // Given
        databaseInitializer.shouldSucceed = true
        databaseInitializer.hasProfiles = false

        // When
        databaseInitializer.initialize()
        databaseInitializer.hasProfiles = true
        val secondResult = databaseInitializer.initialize()

        // Then
        assertEquals(2, databaseInitializer.initializeCallCount)
        assertTrue("Second result should reflect new hasProfiles value", secondResult.getOrNull() == true)
    }

    // ==========================================
    // Reset tests
    // ==========================================

    @Test
    fun `reset clears all state`() = runTest {
        // Given - perform initialization
        databaseInitializer.shouldSucceed = true
        databaseInitializer.hasProfiles = true
        databaseInitializer.initialize()

        assertEquals(1, databaseInitializer.initializeCallCount)
        assertTrue(databaseInitializer.isInitialized)

        // When
        databaseInitializer.reset()

        // Then
        assertEquals(0, databaseInitializer.initializeCallCount)
        assertFalse(databaseInitializer.isInitialized)
        assertTrue(databaseInitializer.shouldSucceed)
        assertFalse(databaseInitializer.hasProfiles)
    }
}
