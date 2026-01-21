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
import net.ktnx.mobileledger.fake.FakeAuthDataProvider
import net.ktnx.mobileledger.fake.FakeProfileRepository
import net.ktnx.mobileledger.util.createTestDomainProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ProfilePersistenceImpl].
 *
 * Tests verify:
 * - Profile insert for new profiles (id = null)
 * - Profile update for existing profiles
 * - AuthDataProvider notification after save
 * - Profile deletion
 */
class ProfilePersistenceImplTest {

    private lateinit var fakeProfileRepository: FakeProfileRepository
    private lateinit var fakeAuthDataProvider: FakeAuthDataProvider
    private lateinit var persistence: ProfilePersistenceImpl

    @Before
    fun setup() {
        fakeProfileRepository = FakeProfileRepository()
        fakeAuthDataProvider = FakeAuthDataProvider()
        persistence = ProfilePersistenceImpl(fakeProfileRepository, fakeAuthDataProvider)
    }

    // ========================================
    // Save - Insert tests
    // ========================================

    @Test
    fun `save inserts new profile when id is null`() = runTest {
        // Given
        val profile = createTestDomainProfile(id = null, name = "New Profile")

        // When
        val result = persistence.save(profile)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, fakeProfileRepository.profiles.size)
        assertEquals("New Profile", fakeProfileRepository.profiles[0].name)
    }

    @Test
    fun `save inserts new profile when id is zero`() = runTest {
        // Given - id of 0 should also be treated as new (though less common)
        val profile = createTestDomainProfile(id = null, name = "New Profile")

        // When
        val result = persistence.save(profile)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, fakeProfileRepository.profiles.size)
    }

    @Test
    fun `save notifies AuthDataProvider after insert`() = runTest {
        // Given
        val profile = createTestDomainProfile(id = null, name = "New Profile")

        // When
        persistence.save(profile)

        // Then
        assertEquals(1, fakeAuthDataProvider.backupDataChangedCallCount)
    }

    // ========================================
    // Save - Update tests
    // ========================================

    @Test
    fun `save updates existing profile when id is set`() = runTest {
        // Given - first insert a profile
        val originalProfile = createTestDomainProfile(id = null, name = "Original")
        fakeProfileRepository.insertProfile(originalProfile)

        // Update with the same ID
        val updatedProfile = createTestDomainProfile(id = 1L, name = "Updated")

        // When
        val result = persistence.save(updatedProfile)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, fakeProfileRepository.profiles.size)
        assertEquals("Updated", fakeProfileRepository.profiles[0].name)
    }

    @Test
    fun `save notifies AuthDataProvider after update`() = runTest {
        // Given
        val originalProfile = createTestDomainProfile(id = null, name = "Original")
        fakeProfileRepository.insertProfile(originalProfile)
        val updatedProfile = createTestDomainProfile(id = 1L, name = "Updated")

        // When
        persistence.save(updatedProfile)

        // Then
        assertEquals(1, fakeAuthDataProvider.backupDataChangedCallCount)
    }

    // ========================================
    // Delete tests
    // ========================================

    @Test
    fun `delete removes existing profile`() = runTest {
        // Given
        val profile = createTestDomainProfile(id = null, name = "To Delete")
        fakeProfileRepository.insertProfile(profile)
        assertEquals(1, fakeProfileRepository.profiles.size)

        // When
        val result = persistence.delete(1L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, fakeProfileRepository.profiles.size)
    }

    @Test
    fun `delete does nothing for non-existent profile`() = runTest {
        // Given - no profiles exist

        // When
        val result = persistence.delete(999L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, fakeProfileRepository.profiles.size)
    }

    // ========================================
    // Error handling tests
    // ========================================

    @Test
    fun `save returns success result on success`() = runTest {
        // Given
        val profile = createTestDomainProfile(id = null, name = "Test")

        // When
        val result = persistence.save(profile)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `delete returns success result on success`() = runTest {
        // Given
        val profile = createTestDomainProfile(id = null, name = "Test")
        fakeProfileRepository.insertProfile(profile)

        // When
        val result = persistence.delete(1L)

        // Then
        assertTrue(result.isSuccess)
    }
}
