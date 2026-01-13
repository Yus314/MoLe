# ProfileSelectionViewModel Contract

**Component**: ProfileSelectionViewModel
**Purpose**: Manages profile selection and ordering in the navigation drawer
**Target Size**: ~120 lines

## State

### Exposed StateFlows

| Name | Type | Description |
|------|------|-------------|
| `uiState` | `StateFlow<ProfileSelectionUiState>` | Current UI state for profile selection |
| `currentProfile` | `StateFlow<Profile?>` | Delegate to ProfileRepository.currentProfile |
| `allProfiles` | `StateFlow<List<Profile>>` | All available profiles |

### ProfileSelectionUiState

```kotlin
data class ProfileSelectionUiState(
    val currentProfileId: Long? = null,
    val currentProfileName: String = "",
    val currentProfileTheme: Int = -1,
    val currentProfileCanPost: Boolean = false,
    val profiles: List<ProfileListItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

## Events

### ProfileSelectionEvent

| Event | Parameters | Description |
|-------|------------|-------------|
| `SelectProfile` | `profileId: Long` | Select a profile by ID |
| `ReorderProfiles` | `orderedProfiles: List<ProfileListItem>` | Reorder profiles |

### Event Handler

```kotlin
fun onEvent(event: ProfileSelectionEvent)
```

## Effects

None. Profile changes propagate via ProfileRepository.currentProfile StateFlow.

## Dependencies

| Dependency | Type | Purpose |
|------------|------|---------|
| `ProfileRepository` | Constructor injection | Profile data access and current profile management |

## Public Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| `onEvent` | `fun onEvent(event: ProfileSelectionEvent)` | Handle user events |

## Internal Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| `selectProfile` | `private fun selectProfile(profileId: Long)` | Select profile and notify repository |
| `reorderProfiles` | `private fun reorderProfiles(orderedProfiles: List<ProfileListItem>)` | Update profile order |
| `observeProfiles` | `private fun observeProfiles()` | Observe ProfileRepository for changes |

## Behavior

### Init

1. Observe ProfileRepository.currentProfile
2. Observe ProfileRepository.getAllProfiles()
3. Update uiState when profile changes

### selectProfile(profileId)

1. Get profile from repository by ID
2. If found, call ProfileRepository.setCurrentProfile(profile)
3. uiState updates automatically via observation

### reorderProfiles(orderedProfiles)

1. Map ProfileListItem list to Profile list
2. Call ProfileRepository.updateProfileOrder(profiles)

## Test Cases

### Unit Tests (ProfileSelectionViewModelTest.kt)

| Test | Description |
|------|-------------|
| `init_loadsProfilesFromRepository` | Verify profiles are loaded on init |
| `selectProfile_updatesCurrentProfile` | Verify profile selection updates repository |
| `selectProfile_invalidId_doesNothing` | Verify invalid profile ID is ignored |
| `reorderProfiles_updatesOrder` | Verify profile reordering works |
| `observesProfileChanges_updatesUiState` | Verify external profile changes are reflected |

### Test Setup

```kotlin
@HiltViewModelTest
class ProfileSelectionViewModelTest {
    private val fakeProfileRepository = FakeProfileRepositoryForViewModel()
    private lateinit var viewModel: ProfileSelectionViewModel

    @Before
    fun setup() {
        viewModel = ProfileSelectionViewModel(fakeProfileRepository)
    }
}
```
