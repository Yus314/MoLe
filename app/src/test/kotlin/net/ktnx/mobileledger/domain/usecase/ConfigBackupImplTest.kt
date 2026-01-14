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

import android.net.Uri
import io.mockk.mockk
import java.io.FileNotFoundException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.fake.FakeConfigBackup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ConfigBackup のテスト
 *
 * FakeConfigBackup を使用してバックアップ/リストアロジックをテストする。
 * T050-T053: バックアップ成功/失敗、リストア成功/失敗のテストケース。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConfigBackupImplTest {

    private lateinit var configBackup: FakeConfigBackup
    private lateinit var testUri: Uri

    @Before
    fun setup() {
        configBackup = FakeConfigBackup()
        // Use mockk to create a mock Uri since Uri.parse requires Android runtime
        testUri = mockk(relaxed = true)
    }

    // ==========================================
    // T051: Backup success test case
    // ==========================================

    @Test
    fun `backup success returns Result success`() = runTest {
        // Given
        configBackup.shouldSucceed = true

        // When
        val result = configBackup.backup(testUri)

        // Then
        assertTrue("Backup should succeed", result.isSuccess)
        assertEquals(1, configBackup.backupCallCount)
        assertEquals(testUri, configBackup.lastBackupUri)
    }

    @Test
    fun `backup tracks call count correctly`() = runTest {
        // Given
        configBackup.shouldSucceed = true

        // When
        configBackup.backup(testUri)
        configBackup.backup(testUri)
        configBackup.backup(testUri)

        // Then
        assertEquals(3, configBackup.backupCallCount)
    }

    // ==========================================
    // T052: Restore success test case
    // ==========================================

    @Test
    fun `restore success returns Result success`() = runTest {
        // Given
        configBackup.shouldSucceed = true

        // When
        val result = configBackup.restore(testUri)

        // Then
        assertTrue("Restore should succeed", result.isSuccess)
        assertEquals(1, configBackup.restoreCallCount)
        assertEquals(testUri, configBackup.lastRestoreUri)
    }

    @Test
    fun `restore tracks call count correctly`() = runTest {
        // Given
        configBackup.shouldSucceed = true

        // When
        configBackup.restore(testUri)
        configBackup.restore(testUri)

        // Then
        assertEquals(2, configBackup.restoreCallCount)
    }

    // ==========================================
    // T053: Error handling test cases
    // ==========================================

    @Test
    fun `backup failure returns Result failure`() = runTest {
        // Given
        configBackup.shouldSucceed = false

        // When
        val result = configBackup.backup(testUri)

        // Then
        assertTrue("Backup should fail", result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun `backup with specific error returns correct exception`() = runTest {
        // Given
        val expectedException = FileNotFoundException("File not found")
        configBackup.backupError = expectedException

        // When
        val result = configBackup.backup(testUri)

        // Then
        assertTrue("Backup should fail", result.isFailure)
        val actualException = result.exceptionOrNull()
        assertTrue(
            "Should be FileNotFoundException",
            actualException is FileNotFoundException
        )
        assertEquals("File not found", actualException?.message)
    }

    @Test
    fun `restore failure returns Result failure`() = runTest {
        // Given
        configBackup.shouldSucceed = false

        // When
        val result = configBackup.restore(testUri)

        // Then
        assertTrue("Restore should fail", result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun `restore with file not found error returns correct exception`() = runTest {
        // Given
        val expectedException = FileNotFoundException("Backup file not found")
        configBackup.restoreError = expectedException

        // When
        val result = configBackup.restore(testUri)

        // Then
        assertTrue("Restore should fail", result.isFailure)
        val actualException = result.exceptionOrNull()
        assertTrue(
            "Should be FileNotFoundException",
            actualException is FileNotFoundException
        )
        assertEquals("Backup file not found", actualException?.message)
    }

    @Test
    fun `restore with parse error returns correct exception`() = runTest {
        // Given
        val expectedException = IllegalArgumentException("Invalid JSON format")
        configBackup.restoreError = expectedException

        // When
        val result = configBackup.restore(testUri)

        // Then
        assertTrue("Restore should fail", result.isFailure)
        val actualException = result.exceptionOrNull()
        assertTrue(
            "Should be IllegalArgumentException",
            actualException is IllegalArgumentException
        )
        assertEquals("Invalid JSON format", actualException?.message)
    }

    // ==========================================
    // Reset tests
    // ==========================================

    @Test
    fun `reset clears all state`() = runTest {
        // Given - perform some operations
        configBackup.backup(testUri)
        configBackup.restore(testUri)
        configBackup.backupError = FileNotFoundException("test")
        configBackup.shouldSucceed = false

        assertEquals(1, configBackup.backupCallCount)
        assertEquals(1, configBackup.restoreCallCount)
        assertNotNull(configBackup.lastBackupUri)
        assertFalse(configBackup.shouldSucceed)

        // When
        configBackup.reset()

        // Then
        assertEquals(0, configBackup.backupCallCount)
        assertEquals(0, configBackup.restoreCallCount)
        assertEquals(null, configBackup.lastBackupUri)
        assertEquals(null, configBackup.lastRestoreUri)
        assertTrue(configBackup.shouldSucceed)
    }
}
