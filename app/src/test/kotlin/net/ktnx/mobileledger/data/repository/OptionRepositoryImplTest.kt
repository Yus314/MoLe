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

package net.ktnx.mobileledger.data.repository

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.core.data.repository.impl.OptionRepositoryImpl
import net.ktnx.mobileledger.core.database.dao.OptionDAO
import net.ktnx.mobileledger.core.database.entity.Option
import net.ktnx.mobileledger.core.domain.model.AppOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [OptionRepositoryImpl].
 *
 * Tests verify:
 * - Query operations (observe and get)
 * - Mutation operations (insert, delete)
 * - Convenience operations (sync timestamp)
 * - Domain model mapping
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OptionRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockOptionDAO: OptionDAO
    private lateinit var repository: OptionRepositoryImpl

    private val testProfileId = 1L

    @Before
    fun setup() {
        mockOptionDAO = mockk(relaxed = true)

        repository = OptionRepositoryImpl(
            optionDAO = mockOptionDAO,
            ioDispatcher = testDispatcher
        )
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createDbOption(
        profileId: Long = testProfileId,
        name: String = "test_option",
        value: String = "test_value"
    ): Option = Option(profileId, name, value)

    // ========================================
    // Query Operations - Flow tests
    // ========================================

    @Test
    fun `observeOption returns mapped domain model`() = runTest(testDispatcher) {
        // Given
        val dbOption = createDbOption()
        every { mockOptionDAO.load(testProfileId, "test_option") } returns flowOf(dbOption)

        // When
        val result = repository.observeOption(testProfileId, "test_option").first()

        // Then
        assertNotNull(result)
        assertEquals("test_option", result?.name)
        assertEquals("test_value", result?.value)
    }

    // Note: Flow-based "not found" test removed because DAO returns Flow<Option>
    // (non-nullable). The sync version loadSync returns nullable and is tested below.

    // ========================================
    // Query Operations - Suspend tests
    // ========================================

    @Test
    fun `getOption returns mapped domain model`() = runTest(testDispatcher) {
        // Given
        val dbOption = createDbOption()
        coEvery { mockOptionDAO.loadSync(testProfileId, "test_option") } returns dbOption

        // When
        val result = repository.getOption(testProfileId, "test_option")

        // Then
        assertNotNull(result)
        assertEquals("test_option", result?.name)
    }

    @Test
    fun `getOption returns null when not found`() = runTest(testDispatcher) {
        // Given
        coEvery { mockOptionDAO.loadSync(testProfileId, "unknown") } returns null

        // When
        val result = repository.getOption(testProfileId, "unknown")

        // Then
        assertNull(result)
    }

    @Test
    fun `getAllOptionsForProfile returns all options`() = runTest(testDispatcher) {
        // Given
        val options = listOf(
            createDbOption(name = "opt1", value = "val1"),
            createDbOption(name = "opt2", value = "val2")
        )
        coEvery { mockOptionDAO.allForProfileSync(testProfileId) } returns options

        // When
        val result = repository.getAllOptionsForProfile(testProfileId)

        // Then
        assertEquals(2, result.size)
        assertEquals("opt1", result[0].name)
        assertEquals("opt2", result[1].name)
    }

    // ========================================
    // Mutation Operations tests
    // ========================================

    @Test
    fun `insertOption calls DAO and returns id`() = runTest(testDispatcher) {
        // Given
        val appOption = AppOption(profileId = testProfileId, name = "new_opt", value = "new_val")
        coEvery { mockOptionDAO.insertSync(any()) } returns 1L

        // When
        val result = repository.insertOption(appOption)

        // Then
        assertEquals(1L, result)
        coVerify { mockOptionDAO.insertSync(any()) }
    }

    @Test
    fun `deleteOption calls DAO`() = runTest(testDispatcher) {
        // Given
        val appOption = AppOption(profileId = testProfileId, name = "del_opt", value = "del_val")
        coEvery { mockOptionDAO.deleteSync(any<Option>()) } just Runs

        // When
        repository.deleteOption(appOption)

        // Then
        coVerify { mockOptionDAO.deleteSync(any<Option>()) }
    }

    @Test
    fun `deleteOptionsForProfile removes all profile options`() = runTest(testDispatcher) {
        // Given
        val options = listOf(
            createDbOption(name = "opt1"),
            createDbOption(name = "opt2")
        )
        coEvery { mockOptionDAO.allForProfileSync(testProfileId) } returns options
        coEvery { mockOptionDAO.deleteSync(any<List<Option>>()) } just Runs

        // When
        repository.deleteOptionsForProfile(testProfileId)

        // Then
        coVerify { mockOptionDAO.allForProfileSync(testProfileId) }
        coVerify { mockOptionDAO.deleteSync(options) }
    }

    @Test
    fun `deleteAllOptions calls DAO`() = runTest(testDispatcher) {
        // Given
        coEvery { mockOptionDAO.deleteAllSync() } just Runs

        // When
        repository.deleteAllOptions()

        // Then
        coVerify { mockOptionDAO.deleteAllSync() }
    }

    // ========================================
    // Convenience Operations tests
    // ========================================

    @Test
    fun `setLastSyncTimestamp inserts option`() = runTest(testDispatcher) {
        // Given
        val timestamp = 1234567890L
        coEvery { mockOptionDAO.insertSync(any()) } returns 1L

        // When
        repository.setLastSyncTimestamp(testProfileId, timestamp)

        // Then
        coVerify {
            mockOptionDAO.insertSync(
                match {
                    it.profileId == testProfileId &&
                        it.name == Option.OPT_LAST_SCRAPE &&
                        it.value == timestamp.toString()
                }
            )
        }
    }

    @Test
    fun `getLastSyncTimestamp returns parsed timestamp`() = runTest(testDispatcher) {
        // Given
        val dbOption = createDbOption(name = AppOption.OPT_LAST_SCRAPE, value = "1234567890")
        coEvery { mockOptionDAO.loadSync(testProfileId, AppOption.OPT_LAST_SCRAPE) } returns dbOption

        // When
        val result = repository.getLastSyncTimestamp(testProfileId)

        // Then
        assertEquals(1234567890L, result)
    }

    @Test
    fun `getLastSyncTimestamp returns null when not found`() = runTest(testDispatcher) {
        // Given
        coEvery { mockOptionDAO.loadSync(testProfileId, AppOption.OPT_LAST_SCRAPE) } returns null

        // When
        val result = repository.getLastSyncTimestamp(testProfileId)

        // Then
        assertNull(result)
    }

    @Test
    fun `getLastSyncTimestamp returns null for invalid value`() = runTest(testDispatcher) {
        // Given
        val dbOption = createDbOption(name = AppOption.OPT_LAST_SCRAPE, value = "invalid")
        coEvery { mockOptionDAO.loadSync(testProfileId, AppOption.OPT_LAST_SCRAPE) } returns dbOption

        // When
        val result = repository.getLastSyncTimestamp(testProfileId)

        // Then
        assertNull(result)
    }
}
