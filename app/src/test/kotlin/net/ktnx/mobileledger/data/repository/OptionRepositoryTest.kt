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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.domain.model.AppOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [OptionRepository] using a fake repository implementation.
 *
 * These tests verify:
 * - CRUD operations work correctly
 * - Profile-specific option retrieval
 * - Batch delete operations
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OptionRepositoryTest {

    private lateinit var repository: FakeOptionRepository

    @Before
    fun setup() {
        repository = FakeOptionRepository()
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createTestOption(
        profileId: Long = 1L,
        name: String = "test_option",
        value: String? = "test_value"
    ): AppOption = AppOption(profileId, name, value)

    // ========================================
    // getOption tests
    // ========================================

    @Test
    fun `observeOption returns null for non-existent option`() = runTest {
        val result = repository.observeOption(1L, "non_existent").first()
        assertNull(result)
    }

    @Test
    fun `observeOption returns option when exists`() = runTest {
        val option = createTestOption(profileId = 1L, name = "last_scrape", value = "12345")
        repository.insertOption(option)

        val result = repository.observeOption(1L, "last_scrape").first()

        assertNotNull(result)
        assertEquals("last_scrape", result?.name)
        assertEquals("12345", result?.value)
    }

    // ========================================
    // getOption tests
    // ========================================

    @Test
    fun `getOption returns null for non-existent option`() = runTest {
        val result = repository.getOption(999L, "non_existent")
        assertNull(result)
    }

    @Test
    fun `getOption returns option when exists`() = runTest {
        val option = createTestOption(profileId = 2L, name = "setting", value = "enabled")
        repository.insertOption(option)

        val result = repository.getOption(2L, "setting")

        assertNotNull(result)
        assertEquals("setting", result?.name)
        assertEquals("enabled", result?.value)
    }

    // ========================================
    // getAllOptionsForProfile tests
    // ========================================

    @Test
    fun `getAllOptionsForProfile returns empty list when no options`() = runTest {
        val result = repository.getAllOptionsForProfile(1L)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAllOptionsForProfile returns only options for specified profile`() = runTest {
        repository.insertOption(createTestOption(profileId = 1L, name = "opt1", value = "val1"))
        repository.insertOption(createTestOption(profileId = 1L, name = "opt2", value = "val2"))
        repository.insertOption(createTestOption(profileId = 2L, name = "opt3", value = "val3"))

        val result = repository.getAllOptionsForProfile(1L)

        assertEquals(2, result.size)
        assertTrue(result.all { it.profileId == 1L })
    }

    // ========================================
    // insertOption tests
    // ========================================

    @Test
    fun `insertOption stores new option`() = runTest {
        val option = createTestOption(profileId = 1L, name = "new_option", value = "new_value")

        repository.insertOption(option)

        val stored = repository.getOption(1L, "new_option")
        assertNotNull(stored)
        assertEquals("new_value", stored?.value)
    }

    @Test
    fun `insertOption replaces existing option with same key`() = runTest {
        val original = createTestOption(profileId = 1L, name = "key", value = "original")
        repository.insertOption(original)

        val updated = createTestOption(profileId = 1L, name = "key", value = "updated")
        repository.insertOption(updated)

        val result = repository.getOption(1L, "key")
        assertEquals("updated", result?.value)
        assertEquals(1, repository.getAllOptionsForProfile(1L).size)
    }

    // ========================================
    // deleteOption tests
    // ========================================

    @Test
    fun `deleteOption removes option`() = runTest {
        val option = createTestOption(profileId = 1L, name = "to_delete", value = "value")
        repository.insertOption(option)

        repository.deleteOption(option)

        val result = repository.getOption(1L, "to_delete")
        assertNull(result)
    }

    // ========================================
    // deleteOptionsForProfile tests
    // ========================================

    @Test
    fun `deleteOptionsForProfile removes all options for profile`() = runTest {
        repository.insertOption(createTestOption(profileId = 1L, name = "opt1"))
        repository.insertOption(createTestOption(profileId = 1L, name = "opt2"))
        repository.insertOption(createTestOption(profileId = 2L, name = "opt3"))

        repository.deleteOptionsForProfile(1L)

        assertTrue(repository.getAllOptionsForProfile(1L).isEmpty())
        assertEquals(1, repository.getAllOptionsForProfile(2L).size)
    }

    // ========================================
    // deleteAllOptions tests
    // ========================================

    @Test
    fun `deleteAllOptions removes all options`() = runTest {
        repository.insertOption(createTestOption(profileId = 1L, name = "opt1"))
        repository.insertOption(createTestOption(profileId = 2L, name = "opt2"))
        repository.insertOption(createTestOption(profileId = 3L, name = "opt3"))

        repository.deleteAllOptions()

        assertTrue(repository.getAllOptionsForProfile(1L).isEmpty())
        assertTrue(repository.getAllOptionsForProfile(2L).isEmpty())
        assertTrue(repository.getAllOptionsForProfile(3L).isEmpty())
    }

    // ========================================
    // setLastSyncTimestamp / getLastSyncTimestamp tests
    // ========================================

    @Test
    fun `setLastSyncTimestamp stores timestamp as option`() = runTest {
        repository.setLastSyncTimestamp(1L, 1234567890L)

        val option = repository.getOption(1L, AppOption.OPT_LAST_SCRAPE)
        assertNotNull(option)
        assertEquals("1234567890", option?.value)
    }

    @Test
    fun `getLastSyncTimestamp returns null when not set`() = runTest {
        val result = repository.getLastSyncTimestamp(1L)
        assertNull(result)
    }

    @Test
    fun `getLastSyncTimestamp returns timestamp when set`() = runTest {
        repository.setLastSyncTimestamp(1L, 1234567890L)

        val result = repository.getLastSyncTimestamp(1L)
        assertEquals(1234567890L, result)
    }
}

/**
 * Fake implementation of [OptionRepository] for unit testing.
 *
 * This implementation provides an in-memory store that allows testing
 * without a real database or Room infrastructure.
 */
class FakeOptionRepository : OptionRepository {

    // Key: Pair(profileId, name) -> Value: AppOption
    private val options = mutableMapOf<Pair<Long, String>, AppOption>()

    override fun observeOption(profileId: Long, name: String): Flow<AppOption?> =
        MutableStateFlow(options[Pair(profileId, name)])

    override suspend fun getOption(profileId: Long, name: String): AppOption? = options[Pair(profileId, name)]

    override suspend fun getAllOptionsForProfile(profileId: Long): List<AppOption> =
        options.values.filter { it.profileId == profileId }

    override suspend fun insertOption(option: AppOption): Long {
        options[Pair(option.profileId, option.name)] = option
        return 1L
    }

    override suspend fun deleteOption(option: AppOption) {
        options.remove(Pair(option.profileId, option.name))
    }

    override suspend fun deleteOptionsForProfile(profileId: Long) {
        val keysToRemove = options.keys.filter { it.first == profileId }
        keysToRemove.forEach { options.remove(it) }
    }

    override suspend fun deleteAllOptions() {
        options.clear()
    }

    override suspend fun setLastSyncTimestamp(profileId: Long, timestamp: Long) {
        insertOption(AppOption(profileId, AppOption.OPT_LAST_SCRAPE, timestamp.toString()))
    }

    override suspend fun getLastSyncTimestamp(profileId: Long): Long? =
        options[Pair(profileId, AppOption.OPT_LAST_SCRAPE)]?.valueAsLong()
}
