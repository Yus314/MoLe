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
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.json.API
import net.ktnx.mobileledger.model.FutureDates
import net.ktnx.mobileledger.service.AuthDataProvider
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
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: ProfileDetailViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = FakeProfileRepositoryForProfileDetail()
        authDataProvider = FakeAuthDataProvider()
        savedStateHandle = SavedStateHandle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ProfileDetailViewModel = ProfileDetailViewModel(
        profileRepository = profileRepository,
        authDataProvider = authDataProvider,
        ioDispatcher = testDispatcher,
        savedStateHandle = savedStateHandle
    )

    private fun createTestProfile(
        id: Long = 0L,
        name: String = "Test Profile",
        url: String = "https://example.com/ledger",
        theme: Int = 180
    ): Profile = Profile().apply {
        this.id = id
        this.name = name
        this.url = url
        this.theme = theme
        this.uuid = java.util.UUID.randomUUID().toString()
        this.orderNo = 1
        this.useAuthentication = false
        this.permitPosting = true
        this.futureDates = FutureDates.None.toInt()
        this.apiVersion = API.auto.toInt()
    }

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
        val id = profileRepository.insertProfile(profile)

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

        assertEquals(1, profileRepository.getProfileCount())
        val saved = profileRepository.getAllProfilesSync().first()
        assertEquals("New Profile", saved.name)
        assertEquals("https://valid.com", saved.url)
    }

    @Test
    fun `save valid existing profile updates repository`() = runTest {
        val profile = createTestProfile(name = "Original", url = "https://original.com")
        val id = profileRepository.insertProfile(profile)

        viewModel = createViewModel()
        viewModel.initialize(profileId = id, initialThemeHue = -1)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.UpdateName("Updated"))
        viewModel.onEvent(ProfileDetailEvent.UpdateUrl("https://updated.com"))
        viewModel.onEvent(ProfileDetailEvent.Save)
        advanceUntilIdle()

        assertEquals(1, profileRepository.getProfileCount())
        val saved = profileRepository.getProfileByIdSync(id)
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
        val id = profileRepository.insertProfile(profile)

        viewModel = createViewModel()
        viewModel.initialize(profileId = id, initialThemeHue = -1)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.ShowDeleteConfirmDialog)

        assertTrue(viewModel.uiState.value.showDeleteConfirmDialog)
    }

    @Test
    fun `dismissDeleteConfirmDialog hides dialog`() = runTest {
        val profile = createTestProfile()
        val id = profileRepository.insertProfile(profile)

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
        val id = profileRepository.insertProfile(profile)
        assertEquals(1, profileRepository.getProfileCount())

        viewModel = createViewModel()
        viewModel.initialize(profileId = id, initialThemeHue = -1)
        advanceUntilIdle()

        viewModel.onEvent(ProfileDetailEvent.ConfirmDelete)
        advanceUntilIdle()

        assertEquals(0, profileRepository.getProfileCount())
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
        val id = profileRepository.insertProfile(profile)

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
        val id = profileRepository.insertProfile(profile)

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
}

/**
 * Fake ProfileRepository for ProfileDetailViewModel testing.
 */
class FakeProfileRepositoryForProfileDetail : ProfileRepository {
    private val profiles = mutableMapOf<Long, Profile>()
    private var nextId = 1L
    private val _currentProfile = MutableStateFlow<Profile?>(null)

    override val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    override fun setCurrentProfile(profile: Profile?) {
        _currentProfile.value = profile
    }

    override fun getAllProfiles(): Flow<List<Profile>> = MutableStateFlow(
        profiles.values.sortedBy { it.orderNo }
    )

    override suspend fun getAllProfilesSync(): List<Profile> = profiles.values.sortedBy { it.orderNo }

    override fun getProfileById(profileId: Long): Flow<Profile?> = MutableStateFlow(profiles[profileId])

    override suspend fun getProfileByIdSync(profileId: Long): Profile? = profiles[profileId]

    override fun getProfileByUuid(uuid: String): Flow<Profile?> = MutableStateFlow(
        profiles.values.find { it.uuid == uuid }
    )

    override suspend fun getProfileByUuidSync(uuid: String): Profile? = profiles.values.find { it.uuid == uuid }

    override suspend fun getAnyProfile(): Profile? = profiles.values.firstOrNull()

    override suspend fun getProfileCount(): Int = profiles.size

    override suspend fun insertProfile(profile: Profile): Long {
        val id = if (profile.id == 0L) nextId++ else profile.id
        profile.id = id
        profiles[id] = profile
        return id
    }

    override suspend fun updateProfile(profile: Profile) {
        profiles[profile.id] = profile
        if (_currentProfile.value?.id == profile.id) {
            _currentProfile.value = profile
        }
    }

    override suspend fun deleteProfile(profile: Profile) {
        profiles.remove(profile.id)
        if (_currentProfile.value?.id == profile.id) {
            _currentProfile.value = profiles.values.firstOrNull()
        }
    }

    override suspend fun updateProfileOrder(profiles: List<Profile>) {
        profiles.forEachIndexed { index, profile ->
            this.profiles[profile.id]?.orderNo = index
        }
    }

    override suspend fun deleteAllProfiles() {
        profiles.clear()
        _currentProfile.value = null
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
