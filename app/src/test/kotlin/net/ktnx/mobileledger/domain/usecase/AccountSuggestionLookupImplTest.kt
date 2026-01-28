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

import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.core.testing.fake.FakeAccountRepository
import net.ktnx.mobileledger.feature.transaction.usecase.AccountSuggestionLookupImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AccountSuggestionLookupImpl].
 *
 * Tests verify:
 * - Search term validation
 * - Account name search functionality
 * - Case insensitivity
 * - Empty results handling
 */
class AccountSuggestionLookupImplTest {

    private lateinit var fakeAccountRepository: FakeAccountRepository
    private lateinit var lookup: AccountSuggestionLookupImpl

    private val profileId = 1L

    @Before
    fun setup() {
        fakeAccountRepository = FakeAccountRepository()
        lookup = AccountSuggestionLookupImpl(fakeAccountRepository)
    }

    // ========================================
    // isTermValid tests
    // ========================================

    @Test
    fun `isTermValid returns false for empty term`() {
        assertFalse(lookup.isTermValid(""))
    }

    @Test
    fun `isTermValid returns false for single character`() {
        assertFalse(lookup.isTermValid("a"))
    }

    @Test
    fun `isTermValid returns true for two characters`() {
        assertTrue(lookup.isTermValid("ab"))
    }

    @Test
    fun `isTermValid returns true for longer terms`() {
        assertTrue(lookup.isTermValid("assets"))
    }

    // ========================================
    // Search tests
    // ========================================

    @Test
    fun `search returns empty list for short term`() = runTest {
        // Given
        fakeAccountRepository.addAccount(profileId, "Assets:Bank")

        // When
        val result = lookup.search(profileId, "a")

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `search returns matching accounts`() = runTest {
        // Given
        fakeAccountRepository.addAccount(profileId, "Assets:Bank")
        fakeAccountRepository.addAccount(profileId, "Assets:Cash")
        fakeAccountRepository.addAccount(profileId, "Expenses:Food")

        // When
        val result = lookup.search(profileId, "Assets")

        // Then
        assertEquals(2, result.size)
        assertTrue(result.contains("Assets:Bank"))
        assertTrue(result.contains("Assets:Cash"))
    }

    @Test
    fun `search is case insensitive`() = runTest {
        // Given
        fakeAccountRepository.addAccount(profileId, "Assets:Bank")

        // When
        val resultLower = lookup.search(profileId, "assets")
        val resultUpper = lookup.search(profileId, "ASSETS")

        // Then
        assertEquals(1, resultLower.size)
        assertEquals(1, resultUpper.size)
    }

    @Test
    fun `search returns empty list when no matches`() = runTest {
        // Given
        fakeAccountRepository.addAccount(profileId, "Assets:Bank")

        // When
        val result = lookup.search(profileId, "Expenses")

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `search returns empty list for empty repository`() = runTest {
        // Given - no accounts

        // When
        val result = lookup.search(profileId, "Assets")

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `search matches partial account names`() = runTest {
        // Given
        fakeAccountRepository.addAccount(profileId, "Assets:Bank:Checking")

        // When
        val result = lookup.search(profileId, "Bank")

        // Then
        assertEquals(1, result.size)
        assertEquals("Assets:Bank:Checking", result[0])
    }

    @Test
    fun `search only returns accounts for specified profile`() = runTest {
        // Given
        fakeAccountRepository.addAccount(1L, "Assets:Bank")
        fakeAccountRepository.addAccount(2L, "Assets:Cash")

        // When
        val result = lookup.search(1L, "Assets")

        // Then
        assertEquals(1, result.size)
        assertEquals("Assets:Bank", result[0])
    }
}
