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
import io.mockk.verify
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.dao.ProfileDAO
import net.ktnx.mobileledger.db.Profile as DbProfile
import net.ktnx.mobileledger.domain.model.AppException
import net.ktnx.mobileledger.domain.model.FutureDates
import net.ktnx.mobileledger.domain.model.Profile as DomainProfile
import net.ktnx.mobileledger.domain.usecase.AppExceptionMapper
import net.ktnx.mobileledger.domain.usecase.sync.SyncExceptionMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ProfileRepositoryImpl].
 *
 * Tests verify:
 * - Current profile state management
 * - Query operations (observe and get)
 * - Mutation operations (insert, update, delete)
 * - Error handling with Result<T>
 * - Domain model mapping
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockProfileDAO: ProfileDAO
    private lateinit var appExceptionMapper: AppExceptionMapper
    private lateinit var repository: ProfileRepositoryImpl

    @Before
    fun setup() {
        mockProfileDAO = mockk(relaxed = true)
        appExceptionMapper = AppExceptionMapper(SyncExceptionMapper())

        repository = ProfileRepositoryImpl(
            profileDAO = mockProfileDAO,
            appExceptionMapper = appExceptionMapper,
            ioDispatcher = testDispatcher
        )
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createDbProfile(
        id: Long = 1L,
        name: String = "Test Profile",
        url: String = "https://test.example.com",
        uuid: String = UUID.randomUUID().toString()
    ): DbProfile = DbProfile().apply {
        this.id = id
        this.name = name
        this.url = url
        this.uuid = uuid
        this.orderNo = 0
        this.permitPosting = true
        this.apiVersion = 0
    }

    private fun createDomainProfile(
        id: Long? = 1L,
        name: String = "Test Profile",
        url: String = "https://test.example.com"
    ): DomainProfile = DomainProfile(
        id = id,
        name = name,
        uuid = UUID.randomUUID().toString(),
        url = url,
        authentication = null,
        orderNo = 0,
        permitPosting = true,
        theme = -1,
        preferredAccountsFilter = null,
        futureDates = FutureDates.None,
        apiVersion = 0,
        showCommodityByDefault = false,
        defaultCommodity = null,
        showCommentsByDefault = false,
        serverVersion = null
    )

    // ========================================
    // Current Profile State tests
    // ========================================

    @Test
    fun `currentProfile initially returns null`() = runTest(testDispatcher) {
        // When
        val current = repository.currentProfile.value

        // Then
        assertNull(current)
    }

    @Test
    fun `setCurrentProfile updates currentProfile state`() = runTest(testDispatcher) {
        // Given
        val profile = createDomainProfile()

        // When
        repository.setCurrentProfile(profile)

        // Then
        assertEquals(profile, repository.currentProfile.value)
    }

    @Test
    fun `setCurrentProfile to null clears current profile`() = runTest(testDispatcher) {
        // Given
        repository.setCurrentProfile(createDomainProfile())
        assertNotNull(repository.currentProfile.value)

        // When
        repository.setCurrentProfile(null)

        // Then
        assertNull(repository.currentProfile.value)
    }

    // ========================================
    // Query Operations - Flow tests
    // ========================================

    @Test
    fun `observeAllProfiles returns mapped domain models`() = runTest(testDispatcher) {
        // Given
        val dbProfiles = listOf(createDbProfile(), createDbProfile(id = 2L, name = "Profile 2"))
        every { mockProfileDAO.getAllOrdered() } returns flowOf(dbProfiles)

        // When
        val result = repository.observeAllProfiles().first()

        // Then
        assertEquals(2, result.size)
        assertEquals("Test Profile", result[0].name)
        assertEquals("Profile 2", result[1].name)
        verify { mockProfileDAO.getAllOrdered() }
    }

    @Test
    fun `observeAllProfiles returns empty list when no profiles`() = runTest(testDispatcher) {
        // Given
        every { mockProfileDAO.getAllOrdered() } returns flowOf(emptyList())

        // When
        val result = repository.observeAllProfiles().first()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `observeProfileById returns mapped domain model`() = runTest(testDispatcher) {
        // Given
        val dbProfile = createDbProfile()
        every { mockProfileDAO.getById(1L) } returns flowOf(dbProfile)

        // When
        val result = repository.observeProfileById(1L).first()

        // Then
        assertNotNull(result)
        assertEquals("Test Profile", result?.name)
    }

    // Note: Flow-based "not found" test removed because DAO returns Flow<Profile>
    // (non-nullable). The sync version getByIdSync returns nullable and is tested below.

    @Test
    fun `observeProfileByUuid returns mapped domain model`() = runTest(testDispatcher) {
        // Given
        val uuid = UUID.randomUUID().toString()
        val dbProfile = createDbProfile(uuid = uuid)
        every { mockProfileDAO.getByUuid(uuid) } returns flowOf(dbProfile)

        // When
        val result = repository.observeProfileByUuid(uuid).first()

        // Then
        assertNotNull(result)
        assertEquals(uuid, result?.uuid)
    }

    // ========================================
    // Query Operations - Suspend tests
    // ========================================

    @Test
    fun `getAllProfiles returns Result success with mapped domain models`() = runTest(testDispatcher) {
        // Given
        val dbProfiles = listOf(createDbProfile())
        coEvery { mockProfileDAO.getAllOrderedSync() } returns dbProfiles

        // When
        val result = repository.getAllProfiles()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `getAllProfiles returns Result failure on exception`() = runTest(testDispatcher) {
        // Given
        coEvery { mockProfileDAO.getAllOrderedSync() } throws RuntimeException("DB error")

        // When
        val result = repository.getAllProfiles()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppException)
    }

    @Test
    fun `getProfileById returns profile when found`() = runTest(testDispatcher) {
        // Given
        val dbProfile = createDbProfile()
        coEvery { mockProfileDAO.getByIdSync(1L) } returns dbProfile

        // When
        val result = repository.getProfileById(1L)

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun `getProfileById returns null when not found`() = runTest(testDispatcher) {
        // Given
        coEvery { mockProfileDAO.getByIdSync(999L) } returns null

        // When
        val result = repository.getProfileById(999L)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `getProfileByUuid returns profile when found`() = runTest(testDispatcher) {
        // Given
        val uuid = UUID.randomUUID().toString()
        val dbProfile = createDbProfile(uuid = uuid)
        coEvery { mockProfileDAO.getByUuidSync(uuid) } returns dbProfile

        // When
        val result = repository.getProfileByUuid(uuid)

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun `getAnyProfile returns first available profile`() = runTest(testDispatcher) {
        // Given
        val dbProfile = createDbProfile()
        coEvery { mockProfileDAO.getAnySync() } returns dbProfile

        // When
        val result = repository.getAnyProfile()

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun `getProfileCount returns profile count`() = runTest(testDispatcher) {
        // Given
        coEvery { mockProfileDAO.getProfileCountSync() } returns 5

        // When
        val result = repository.getProfileCount()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(5, result.getOrNull())
    }

    // ========================================
    // Mutation Operations tests
    // ========================================

    @Test
    fun `insertProfile calls DAO and returns generated id`() = runTest(testDispatcher) {
        // Given
        val domainProfile = createDomainProfile(id = null)
        coEvery { mockProfileDAO.insertLastSync(any()) } returns 1L

        // When
        val result = repository.insertProfile(domainProfile)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1L, result.getOrNull())
        coVerify { mockProfileDAO.insertLastSync(any()) }
    }

    @Test
    fun `updateProfile calls DAO and updates current profile if matching`() = runTest(testDispatcher) {
        // Given
        val profile = createDomainProfile(id = 1L, name = "Original")
        repository.setCurrentProfile(profile)
        val updatedProfile = profile.copy(name = "Updated")
        coEvery { mockProfileDAO.updateSync(any()) } just Runs

        // When
        val result = repository.updateProfile(updatedProfile)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("Updated", repository.currentProfile.value?.name)
        coVerify { mockProfileDAO.updateSync(any()) }
    }

    @Test
    fun `updateProfile does not change current profile if not matching`() = runTest(testDispatcher) {
        // Given
        val currentProfile = createDomainProfile(id = 1L, name = "Current")
        repository.setCurrentProfile(currentProfile)
        val otherProfile = createDomainProfile(id = 2L, name = "Other")
        coEvery { mockProfileDAO.updateSync(any()) } just Runs

        // When
        val result = repository.updateProfile(otherProfile)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("Current", repository.currentProfile.value?.name)
    }

    @Test
    fun `deleteProfile selects fallback profile when current is deleted`() = runTest(testDispatcher) {
        // Given
        val profileToDelete = createDomainProfile(id = 1L, name = "To Delete")
        val fallbackProfile = createDbProfile(id = 2L, name = "Fallback")
        repository.setCurrentProfile(profileToDelete)
        coEvery { mockProfileDAO.deleteSync(any()) } just Runs
        coEvery { mockProfileDAO.getAnySync() } returns fallbackProfile

        // When
        val result = repository.deleteProfile(profileToDelete)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("Fallback", repository.currentProfile.value?.name)
    }

    @Test
    fun `deleteProfile clears current when no fallback available`() = runTest(testDispatcher) {
        // Given
        val profileToDelete = createDomainProfile(id = 1L)
        repository.setCurrentProfile(profileToDelete)
        coEvery { mockProfileDAO.deleteSync(any()) } just Runs
        coEvery { mockProfileDAO.getAnySync() } returns null

        // When
        val result = repository.deleteProfile(profileToDelete)

        // Then
        assertTrue(result.isSuccess)
        assertNull(repository.currentProfile.value)
    }

    @Test
    fun `updateProfileOrder calls DAO with all profiles`() = runTest(testDispatcher) {
        // Given
        val profiles = listOf(
            createDomainProfile(id = 1L),
            createDomainProfile(id = 2L)
        )
        coEvery { mockProfileDAO.updateOrderSync(any()) } just Runs

        // When
        val result = repository.updateProfileOrder(profiles)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockProfileDAO.updateOrderSync(any()) }
    }

    @Test
    fun `deleteAllProfiles clears current profile`() = runTest(testDispatcher) {
        // Given
        repository.setCurrentProfile(createDomainProfile())
        coEvery { mockProfileDAO.deleteAllSync() } just Runs

        // When
        val result = repository.deleteAllProfiles()

        // Then
        assertTrue(result.isSuccess)
        assertNull(repository.currentProfile.value)
        coVerify { mockProfileDAO.deleteAllSync() }
    }
}
