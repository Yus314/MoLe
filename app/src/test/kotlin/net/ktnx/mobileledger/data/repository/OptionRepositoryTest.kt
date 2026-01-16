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
import net.ktnx.mobileledger.db.Option
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
    ): Option = Option(profileId, name, value)

    // ========================================
    // getOption tests
    // ========================================

    @Test
    fun `getOption returns null for non-existent option`() = runTest {
        val result = repository.getOption(1L, "non_existent").first()
        assertNull(result)
    }

    @Test
    fun `getOption returns option when exists`() = runTest {
        val option = createTestOption(profileId = 1L, name = "last_scrape", value = "12345")
        repository.insertOption(option)

        val result = repository.getOption(1L, "last_scrape").first()

        assertNotNull(result)
        assertEquals("last_scrape", result?.name)
        assertEquals("12345", result?.value)
    }

    // ========================================
    // getOptionSync tests
    // ========================================

    @Test
    fun `getOptionSync returns null for non-existent option`() = runTest {
        val result = repository.getOptionSync(999L, "non_existent")
        assertNull(result)
    }

    @Test
    fun `getOptionSync returns option when exists`() = runTest {
        val option = createTestOption(profileId = 2L, name = "setting", value = "enabled")
        repository.insertOption(option)

        val result = repository.getOptionSync(2L, "setting")

        assertNotNull(result)
        assertEquals("setting", result?.name)
        assertEquals("enabled", result?.value)
    }

    // ========================================
    // getAllOptionsForProfileSync tests
    // ========================================

    @Test
    fun `getAllOptionsForProfileSync returns empty list when no options`() = runTest {
        val result = repository.getAllOptionsForProfileSync(1L)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAllOptionsForProfileSync returns only options for specified profile`() = runTest {
        repository.insertOption(createTestOption(profileId = 1L, name = "opt1", value = "val1"))
        repository.insertOption(createTestOption(profileId = 1L, name = "opt2", value = "val2"))
        repository.insertOption(createTestOption(profileId = 2L, name = "opt3", value = "val3"))

        val result = repository.getAllOptionsForProfileSync(1L)

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

        val stored = repository.getOptionSync(1L, "new_option")
        assertNotNull(stored)
        assertEquals("new_value", stored?.value)
    }

    @Test
    fun `insertOption replaces existing option with same key`() = runTest {
        val original = createTestOption(profileId = 1L, name = "key", value = "original")
        repository.insertOption(original)

        val updated = createTestOption(profileId = 1L, name = "key", value = "updated")
        repository.insertOption(updated)

        val result = repository.getOptionSync(1L, "key")
        assertEquals("updated", result?.value)
        assertEquals(1, repository.getAllOptionsForProfileSync(1L).size)
    }

    // ========================================
    // deleteOption tests
    // ========================================

    @Test
    fun `deleteOption removes option`() = runTest {
        val option = createTestOption(profileId = 1L, name = "to_delete", value = "value")
        repository.insertOption(option)

        repository.deleteOption(option)

        val result = repository.getOptionSync(1L, "to_delete")
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

        assertTrue(repository.getAllOptionsForProfileSync(1L).isEmpty())
        assertEquals(1, repository.getAllOptionsForProfileSync(2L).size)
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

        assertTrue(repository.getAllOptionsForProfileSync(1L).isEmpty())
        assertTrue(repository.getAllOptionsForProfileSync(2L).isEmpty())
        assertTrue(repository.getAllOptionsForProfileSync(3L).isEmpty())
    }
}

/**
 * Fake implementation of [OptionRepository] for unit testing.
 *
 * This implementation provides an in-memory store that allows testing
 * without a real database or Room infrastructure.
 */
class FakeOptionRepository : OptionRepository {

    // Key: Pair(profileId, name) -> Value: Option
    private val options = mutableMapOf<Pair<Long, String>, Option>()

    override fun getOption(profileId: Long, name: String): Flow<Option?> =
        MutableStateFlow(options[Pair(profileId, name)])

    override suspend fun getOptionSync(profileId: Long, name: String): Option? = options[Pair(profileId, name)]

    override suspend fun getAllOptionsForProfileSync(profileId: Long): List<Option> =
        options.values.filter { it.profileId == profileId }

    override suspend fun insertOption(option: Option): Long {
        options[Pair(option.profileId, option.name)] = option
        return 1L
    }

    override suspend fun deleteOption(option: Option) {
        options.remove(Pair(option.profileId, option.name))
    }

    override suspend fun deleteOptionsForProfile(profileId: Long) {
        val keysToRemove = options.keys.filter { it.first == profileId }
        keysToRemove.forEach { options.remove(it) }
    }

    override suspend fun deleteAllOptions() {
        options.clear()
    }
}
