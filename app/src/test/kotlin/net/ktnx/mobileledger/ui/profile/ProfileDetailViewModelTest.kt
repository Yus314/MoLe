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

package net.ktnx.mobileledger.ui.profile

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.ktnx.mobileledger.TemporaryAuthData
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.domain.model.FutureDates
import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.domain.usecase.ProfilePersistence
import net.ktnx.mobileledger.domain.usecase.ProfilePersistenceImpl
import net.ktnx.mobileledger.domain.usecase.ProfileValidator
import net.ktnx.mobileledger.domain.usecase.ProfileValidatorImpl
import net.ktnx.mobileledger.domain.usecase.VersionDetector
import net.ktnx.mobileledger.fake.FakeVersionDetector
import net.ktnx.mobileledger.json.API
import net.ktnx.mobileledger.service.AuthDataProvider
import net.ktnx.mobileledger.util.createTestDomainProfile
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for ProfileDetailViewModel.
 *
 * Tests cover:
 * - Form field updates
 * - Validation logic
 * - Save/delete operations
 * - UI state (dialogs, loading states)
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class ProfileDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var profileRepository: FakeProfileRepositoryForProfileDetail
    private lateinit var authDataProvider: FakeAuthDataProvider
    private lateinit var versionDetector: FakeVersionDetector
    private lateinit var profileValidator: ProfileValidator
    private lateinit var profilePersistence: ProfilePersistence
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: ProfileDetailViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = FakeProfileRepositoryForProfileDetail()
        authDataProvider = FakeAuthDataProvider()
        versionDetector = FakeVersionDetector()
        profileValidator = ProfileValidatorImpl()
        profilePersistence = ProfilePersistenceImpl(profileRepository, authDataProvider)
        savedStateHandle = SavedStateHandle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ProfileDetailViewModel = ProfileDetailViewModel(
        profileRepository = profileRepository,
        authDataProvider = authDataProvider,
        versionDetector = versionDetector,
        profileValidator = profileValidator,
        profilePersistence = profilePersistence,
        ioDispatcher = testDispatcher,
        savedStateHandle = savedStateHandle
    )

    private fun createTestProfile(
        id: Long? = null,
        name: String = "Test Profile",
        url: String = "https://example.com/ledger",
        theme: Int = 180
    ): Profile = createTestDomainProfile(
        id = id,
        name = name,
        url = url,
        theme = theme,
        orderNo = 1,
        authentication = null,
        permitPosting = true,
        futureDates = FutureDates.None,
        apiVersion = API.auto.toInt()
    )

    // ========================================
    // Initialization tests
    // ========================================

    @Test
    fun `initialize with new profile sets default values`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 120)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(120, state.themeHue)
        assertTrue(state.isNewProfile)
        assertEquals("https://", state.url)
    }

    @Test
    fun `initialize with existing profile loads profile data`() = runTest {
        val profile = createTestProfile(name = "Existing Profile", url = "https://test.com")
        val id = profileRepository.insertProfile(profile).getOrThrow()

        viewModel = createViewModel()
        viewModel.initialize(profileId = id, initialThemeHue = -1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Existing Profile", state.name)
        assertEquals("https://test.com", state.url)
        assertFalse(state.isNewProfile)
    }

    @Test
    fun `initialize only runs once`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 100)
        advanceUntilIdle()

        // Try to initialize again with different values
        viewModel.initialize(profileId = 0L, initialThemeHue = 200)
        advanceUntilIdle()

        // Should still have original value
        assertEquals(100, viewModel.uiState.value.themeHue)
    }

    // ========================================
    // Form field update tests
    // ========================================

    @Test
    fun `updateName updates state and marks unsaved changes`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateName("New Name"))

        val state = viewModel.uiState.value
        assertEquals("New Name", state.name)
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun `updateUrl updates state and marks unsaved changes`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateUrl("https://new-url.com"))

        val state = viewModel.uiState.value
        assertEquals("https://new-url.com", state.url)
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun `updateUseAuthentication updates state`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateUseAuthentication(true))

        assertTrue(viewModel.uiState.value.useAuthentication)
    }

    @Test
    fun `updateAuthUser updates state`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateAuthUser("testuser"))

        assertEquals("testuser", viewModel.uiState.value.authUser)
    }

    @Test
    fun `updateAuthPassword updates state`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateAuthPassword("secret123"))

        assertEquals("secret123", viewModel.uiState.value.authPassword)
    }

    @Test
    fun `updateThemeHue updates state`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateThemeHue(240))

        assertEquals(240, viewModel.uiState.value.themeHue)
    }

    @Test
    fun `updateFutureDates updates state`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateFutureDates(FutureDates.All))

        assertEquals(FutureDates.All, viewModel.uiState.value.futureDates)
    }

    @Test
    fun `updateApiVersion updates state`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateApiVersion(API.v1_23))

        assertEquals(API.v1_23, viewModel.uiState.value.apiVersion)
    }

    @Test
    fun `updatePermitPosting updates state`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdatePermitPosting(false))

        assertFalse(viewModel.uiState.value.permitPosting)
    }

    @Test
    fun `updateShowCommentsByDefault updates state`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateShowCommentsByDefault(false))

        assertFalse(viewModel.uiState.value.showCommentsByDefault)
    }

    @Test
    fun `updateShowCommodityByDefault updates state`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateShowCommodityByDefault(true))

        assertTrue(viewModel.uiState.value.showCommodityByDefault)
    }

    @Test
    fun `updateDefaultCommodity updates state`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateDefaultCommodity("USD"))

        assertEquals("USD", viewModel.uiState.value.defaultCommodity)
    }

    @Test
    fun `updatePreferredAccountsFilter updates state`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdatePreferredAccountsFilter("Assets:"))

        assertEquals("Assets:", viewModel.uiState.value.preferredAccountsFilter)
    }

    // ========================================
    // Validation tests
    // ========================================

    @Test
    fun `save with empty name shows validation error`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateUrl("https://valid.com"))
        viewModel.onEvent(ProfileDetailEvent.Save)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.validationErrors.containsKey(ProfileField.NAME))
    }

    @Test
    fun `save with empty url shows validation error`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateName("Test"))
        viewModel.onEvent(ProfileDetailEvent.UpdateUrl(""))
        viewModel.onEvent(ProfileDetailEvent.Save)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.validationErrors.containsKey(ProfileField.URL))
    }

    @Test
    fun `save with invalid url shows validation error`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateName("Test"))
        viewModel.onEvent(ProfileDetailEvent.UpdateUrl("not-a-valid-url"))
        viewModel.onEvent(ProfileDetailEvent.Save)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.validationErrors.containsKey(ProfileField.URL))
    }

    @Test
    fun `save with auth enabled but empty username shows validation error`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateName("Test"))
        viewModel.onEvent(ProfileDetailEvent.UpdateUrl("https://valid.com"))
        viewModel.onEvent(ProfileDetailEvent.UpdateUseAuthentication(true))
        viewModel.onEvent(ProfileDetailEvent.UpdateAuthPassword("password"))
        viewModel.onEvent(ProfileDetailEvent.Save)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.validationErrors.containsKey(ProfileField.AUTH_USER))
    }

    @Test
    fun `save with auth enabled but empty password shows validation error`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateName("Test"))
        viewModel.onEvent(ProfileDetailEvent.UpdateUrl("https://valid.com"))
        viewModel.onEvent(ProfileDetailEvent.UpdateUseAuthentication(true))
        viewModel.onEvent(ProfileDetailEvent.UpdateAuthUser("user"))
        viewModel.onEvent(ProfileDetailEvent.Save)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.validationErrors.containsKey(ProfileField.AUTH_PASSWORD))
    }

    @Test
    fun `updating field clears its validation error`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        // Trigger validation error
        viewModel.onEvent(ProfileDetailEvent.Save)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.validationErrors.containsKey(ProfileField.NAME))

        // Update the field
        viewModel.onEvent(ProfileDetailEvent.UpdateName("Valid Name"))

        // Error should be cleared
        assertFalse(viewModel.uiState.value.validationErrors.containsKey(ProfileField.NAME))
    }

    @Test
    fun `clearValidationError clears all errors`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.Save)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.validationErrors.isNotEmpty())

        viewModel.onEvent(ProfileDetailEvent.ClearValidationError)

        assertTrue(viewModel.uiState.value.validationErrors.isEmpty())
    }

    // ========================================
    // Save profile tests
    // ========================================

    @Test
    fun `save valid new profile inserts into repository`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateName("New Profile"))
        viewModel.onEvent(ProfileDetailEvent.UpdateUrl("https://valid.com"))
        viewModel.onEvent(ProfileDetailEvent.Save)
        advanceUntilIdle()

        assertEquals(1, profileRepository.getProfileCount().getOrThrow())
        val saved = profileRepository.getAllProfiles().getOrThrow().first()
        assertEquals("New Profile", saved.name)
        assertEquals("https://valid.com", saved.url)
    }

    @Test
    fun `save valid existing profile updates repository`() = runTest {
        val profile = createTestProfile(name = "Original", url = "https://original.com")
        val id = profileRepository.insertProfile(profile).getOrThrow()

        viewModel = createViewModel()
        viewModel.initialize(profileId = id, initialThemeHue = -1)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateName("Updated"))
        viewModel.onEvent(ProfileDetailEvent.UpdateUrl("https://updated.com"))
        viewModel.onEvent(ProfileDetailEvent.Save)
        advanceUntilIdle()

        assertEquals(1, profileRepository.getProfileCount().getOrThrow())
        val saved = profileRepository.getProfileById(id).getOrNull()
        assertEquals("Updated", saved?.name)
        assertEquals("https://updated.com", saved?.url)
    }

    @Test
    fun `save clears unsaved changes flag`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateName("Test"))
        viewModel.onEvent(ProfileDetailEvent.UpdateUrl("https://test.com"))
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)

        viewModel.onEvent(ProfileDetailEvent.Save)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasUnsavedChanges)
    }

    // ========================================
    // Delete profile tests
    // ========================================

    @Test
    fun `showDeleteConfirmDialog shows dialog`() = runTest {
        val profile = createTestProfile()
        val id = profileRepository.insertProfile(profile).getOrThrow()

        viewModel = createViewModel()
        viewModel.initialize(profileId = id, initialThemeHue = -1)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.ShowDeleteConfirmDialog)

        assertTrue(viewModel.uiState.value.showDeleteConfirmDialog)
    }

    @Test
    fun `dismissDeleteConfirmDialog hides dialog`() = runTest {
        val profile = createTestProfile()
        val id = profileRepository.insertProfile(profile).getOrThrow()

        viewModel = createViewModel()
        viewModel.initialize(profileId = id, initialThemeHue = -1)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.ShowDeleteConfirmDialog)
        viewModel.onEvent(ProfileDetailEvent.DismissDeleteConfirmDialog)

        assertFalse(viewModel.uiState.value.showDeleteConfirmDialog)
    }

    @Test
    fun `confirmDelete removes profile from repository`() = runTest {
        val profile = createTestProfile()
        val id = profileRepository.insertProfile(profile).getOrThrow()
        assertEquals(1, profileRepository.getProfileCount().getOrThrow())

        viewModel = createViewModel()
        viewModel.initialize(profileId = id, initialThemeHue = -1)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.ConfirmDelete)
        advanceUntilIdle()

        assertEquals(0, profileRepository.getProfileCount().getOrThrow())
    }

    // ========================================
    // Unsaved changes dialog tests
    // ========================================

    @Test
    fun `navigateBack with unsaved changes shows dialog`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateName("Changed"))
        viewModel.onEvent(ProfileDetailEvent.NavigateBack)

        assertTrue(viewModel.uiState.value.showUnsavedChangesDialog)
    }

    @Test
    fun `navigateBack without unsaved changes does not show dialog`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.NavigateBack)

        assertFalse(viewModel.uiState.value.showUnsavedChangesDialog)
    }

    @Test
    fun `dismissUnsavedChangesDialog hides dialog`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateName("Changed"))
        viewModel.onEvent(ProfileDetailEvent.NavigateBack)
        viewModel.onEvent(ProfileDetailEvent.DismissUnsavedChangesDialog)

        assertFalse(viewModel.uiState.value.showUnsavedChangesDialog)
    }

    @Test
    fun `confirmDiscardChanges hides dialog`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateName("Changed"))
        viewModel.onEvent(ProfileDetailEvent.NavigateBack)
        viewModel.onEvent(ProfileDetailEvent.ConfirmDiscardChanges)

        assertFalse(viewModel.uiState.value.showUnsavedChangesDialog)
    }

    // ========================================
    // Hue ring dialog tests
    // ========================================

    @Test
    fun `showHueRingDialog shows dialog`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.ShowHueRingDialog)

        assertTrue(viewModel.uiState.value.showHueRingDialog)
    }

    @Test
    fun `dismissHueRingDialog hides dialog`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.ShowHueRingDialog)
        viewModel.onEvent(ProfileDetailEvent.DismissHueRingDialog)

        assertFalse(viewModel.uiState.value.showHueRingDialog)
    }

    // ========================================
    // UiState computed properties tests
    // ========================================

    @Test
    fun `isNewProfile returns true for new profile`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isNewProfile)
    }

    @Test
    fun `isNewProfile returns false for existing profile`() = runTest {
        val profile = createTestProfile()
        val id = profileRepository.insertProfile(profile).getOrThrow()

        viewModel = createViewModel()
        viewModel.initialize(profileId = id, initialThemeHue = -1)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isNewProfile)
    }

    @Test
    fun `canDelete returns false for new profile`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canDelete)
    }

    @Test
    fun `canDelete returns true for existing profile`() = runTest {
        val profile = createTestProfile()
        val id = profileRepository.insertProfile(profile).getOrThrow()

        viewModel = createViewModel()
        viewModel.initialize(profileId = id, initialThemeHue = -1)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.canDelete)
    }

    @Test
    fun `isFormValid returns true for valid form without auth`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateName("Valid Name"))
        viewModel.onEvent(ProfileDetailEvent.UpdateUrl("https://valid.com"))

        assertTrue(viewModel.uiState.value.isFormValid)
    }

    @Test
    fun `isFormValid returns true for valid form with auth`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateName("Valid Name"))
        viewModel.onEvent(ProfileDetailEvent.UpdateUrl("https://valid.com"))
        viewModel.onEvent(ProfileDetailEvent.UpdateUseAuthentication(true))
        viewModel.onEvent(ProfileDetailEvent.UpdateAuthUser("user"))
        viewModel.onEvent(ProfileDetailEvent.UpdateAuthPassword("pass"))

        assertTrue(viewModel.uiState.value.isFormValid)
    }

    @Test
    fun `isFormValid returns false when auth enabled but credentials missing`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateName("Valid Name"))
        viewModel.onEvent(ProfileDetailEvent.UpdateUrl("https://valid.com"))
        viewModel.onEvent(ProfileDetailEvent.UpdateUseAuthentication(true))

        assertFalse(viewModel.uiState.value.isFormValid)
    }

    @Test
    fun `showInsecureWarning returns true for http with auth`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateUseAuthentication(true))
        viewModel.onEvent(ProfileDetailEvent.UpdateUrl("http://insecure.com"))

        assertTrue(viewModel.uiState.value.showInsecureWarning)
    }

    @Test
    fun `showInsecureWarning returns false for https with auth`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateUseAuthentication(true))
        viewModel.onEvent(ProfileDetailEvent.UpdateUrl("https://secure.com"))

        assertFalse(viewModel.uiState.value.showInsecureWarning)
    }

    // ========================================
    // Connection test result tests
    // ========================================

    @Test
    fun `clearConnectionTestResult clears result`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        // Set a mock result (normally set by testConnection)
        viewModel.onEvent(ProfileDetailEvent.ClearConnectionTestResult)

        assertNull(viewModel.uiState.value.connectionTestResult)
    }

    // ========================================
    // Error handling tests
    // ========================================

    @Test
    fun `save new profile handles repository insert failure`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateName("Test Profile"))
        viewModel.onEvent(ProfileDetailEvent.UpdateUrl("https://test.com"))
        profileRepository.shouldFailInsert = true

        viewModel.onEvent(ProfileDetailEvent.Save)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        // Profile should not be in repository
        assertEquals(0, profileRepository.getProfileCount().getOrThrow())
    }

    @Test
    fun `save existing profile handles repository update failure`() = runTest {
        val profile = createTestProfile(name = "Original", url = "https://original.com")
        val id = profileRepository.insertProfile(profile).getOrThrow()

        viewModel = createViewModel()
        viewModel.initialize(profileId = id, initialThemeHue = -1)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateName("Updated"))
        profileRepository.shouldFailUpdate = true

        viewModel.onEvent(ProfileDetailEvent.Save)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        // Original profile should remain unchanged
        val saved = profileRepository.getProfileById(id).getOrNull()
        assertEquals("Original", saved?.name)
    }

    @Test
    fun `confirmDelete handles repository delete failure`() = runTest {
        val profile = createTestProfile()
        val id = profileRepository.insertProfile(profile).getOrThrow()
        assertEquals(1, profileRepository.getProfileCount().getOrThrow())

        viewModel = createViewModel()
        viewModel.initialize(profileId = id, initialThemeHue = -1)
        advanceUntilIdle()

        profileRepository.shouldFailDelete = true
        viewModel.onEvent(ProfileDetailEvent.ConfirmDelete)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        // Profile should still exist in repository
        assertEquals(1, profileRepository.getProfileCount().getOrThrow())
    }

    // ========================================
    // Connection test tests
    // ========================================

    @Test
    fun `testConnection success updates state with detected version`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateUrl("https://test.com"))
        versionDetector.shouldSucceed = true
        versionDetector.versionToReturn = "1.32"

        viewModel.onEvent(ProfileDetailEvent.TestConnection)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isTestingConnection)
        assertNotNull(state.detectedVersion)
        assertTrue(state.connectionTestResult is ConnectionTestResult.Success)
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun `testConnection failure updates state with error`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateUrl("https://test.com"))
        versionDetector.shouldSucceed = false
        versionDetector.errorToThrow = RuntimeException("Connection failed")

        viewModel.onEvent(ProfileDetailEvent.TestConnection)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isTestingConnection)
        assertNull(state.detectedVersion)
        assertTrue(state.connectionTestResult is ConnectionTestResult.Error)
    }

    @Test
    fun `testConnection with unrecognized version returns null but marks as error`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateUrl("https://test.com"))
        versionDetector.shouldSucceed = true
        versionDetector.versionToReturn = "unknown-version-string"

        viewModel.onEvent(ProfileDetailEvent.TestConnection)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isTestingConnection)
        assertNull(state.detectedVersion)
        assertTrue(state.connectionTestResult is ConnectionTestResult.Error)
    }

    @Test
    fun `testConnection sets loading state to false when complete`() = runTest {
        viewModel = createViewModel()
        viewModel.initialize(profileId = 0L, initialThemeHue = 0)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateUrl("https://test.com"))
        viewModel.onEvent(ProfileDetailEvent.TestConnection)
        advanceUntilIdle()

        // After completion, loading state should be false
        assertFalse(viewModel.uiState.value.isTestingConnection)
        assertNotNull(viewModel.uiState.value.connectionTestResult)
    }
}

/**
 * Fake ProfileRepository for ProfileDetailViewModel testing.
 */
class FakeProfileRepositoryForProfileDetail : ProfileRepository {
    private val profiles = mutableMapOf<Long, Profile>()
    private var nextId = 1L
    private val _currentProfile = MutableStateFlow<Profile?>(null)

    // Error simulation properties
    var shouldFailInsert: Boolean = false
    var shouldFailUpdate: Boolean = false
    var shouldFailDelete: Boolean = false
    var errorToThrow: Exception = RuntimeException("Fake error for testing")

    override val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    override fun setCurrentProfile(profile: Profile?) {
        _currentProfile.value = profile
    }

    override fun observeAllProfiles(): Flow<List<Profile>> = MutableStateFlow(
        profiles.values.sortedBy { it.orderNo }
    )

    override suspend fun getAllProfiles(): Result<List<Profile>> = Result.success(
        profiles.values.sortedBy {
            it.orderNo
        }
    )

    override fun observeProfileById(profileId: Long): Flow<Profile?> = MutableStateFlow(profiles[profileId])

    override suspend fun getProfileById(profileId: Long): Result<Profile?> = Result.success(profiles[profileId])

    override fun observeProfileByUuid(uuid: String): Flow<Profile?> = MutableStateFlow(
        profiles.values.find { it.uuid == uuid }
    )

    override suspend fun getProfileByUuid(uuid: String): Result<Profile?> = Result.success(
        profiles.values.find {
            it.uuid ==
                uuid
        }
    )

    override suspend fun getAnyProfile(): Result<Profile?> = Result.success(profiles.values.firstOrNull())

    override suspend fun getProfileCount(): Result<Int> = Result.success(profiles.size)

    override suspend fun insertProfile(profile: Profile): Result<Long> {
        if (shouldFailInsert) return Result.failure(errorToThrow)
        val id = if (profile.id == null || profile.id == 0L) nextId++ else profile.id
        val profileWithId = profile.copy(id = id)
        profiles[id] = profileWithId
        return Result.success(id)
    }

    override suspend fun updateProfile(profile: Profile): Result<Unit> {
        if (shouldFailUpdate) return Result.failure(errorToThrow)
        val id = profile.id ?: return Result.success(Unit)
        profiles[id] = profile
        if (_currentProfile.value?.id == id) {
            _currentProfile.value = profile
        }
        return Result.success(Unit)
    }

    override suspend fun deleteProfile(profile: Profile): Result<Unit> {
        if (shouldFailDelete) return Result.failure(errorToThrow)
        val id = profile.id ?: return Result.success(Unit)
        profiles.remove(id)
        if (_currentProfile.value?.id == id) {
            _currentProfile.value = profiles.values.firstOrNull()
        }
        return Result.success(Unit)
    }

    override suspend fun updateProfileOrder(profiles: List<Profile>): Result<Unit> {
        profiles.forEachIndexed { index, profile ->
            val id = profile.id ?: return@forEachIndexed
            this.profiles[id]?.let { existing ->
                this.profiles[id] = existing.copy(orderNo = index)
            }
        }
        return Result.success(Unit)
    }

    override suspend fun deleteAllProfiles(): Result<Unit> {
        profiles.clear()
        _currentProfile.value = null
        return Result.success(Unit)
    }
}

/**
 * Fake AuthDataProvider for testing.
 */
class FakeAuthDataProvider : AuthDataProvider {
    var currentAuthData: TemporaryAuthData? = null
        private set
    var backupNotifiedCount: Int = 0
        private set
    var themeHueDefault: Int = 261 // Default matching Colors.DEFAULT_HUE_DEG

    override fun setTemporaryAuthData(authData: TemporaryAuthData?) {
        currentAuthData = authData
    }

    override fun getTemporaryAuthData(): TemporaryAuthData? = currentAuthData

    override fun resetAuthenticationData() {
        currentAuthData = null
    }

    override fun notifyBackupDataChanged() {
        backupNotifiedCount++
    }

    override fun getDefaultThemeHue(): Int = themeHueDefault

    override fun getNewProfileThemeHue(existingProfiles: List<Profile>?): Int {
        // Simple implementation: return a hue that differs from existing ones
        // For testing, just return a calculated value
        if (existingProfiles.isNullOrEmpty()) return themeHueDefault
        val themes = existingProfiles.map { it.theme }
        val maxHue = themes.maxOrNull() ?: themeHueDefault
        return (maxHue + 60) % 360
    }
}
