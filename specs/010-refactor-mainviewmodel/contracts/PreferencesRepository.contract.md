# PreferencesRepository Contract

**Component**: PreferencesRepository
**Purpose**: Abstract preferences storage, replacing App static methods
**Target Size**: ~40 lines

## Interface

```kotlin
interface PreferencesRepository {
    fun getShowZeroBalanceAccounts(): Boolean
    fun setShowZeroBalanceAccounts(value: Boolean)
}
```

## Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getShowZeroBalanceAccounts()` | `Boolean` | Get current zero balance accounts visibility preference |
| `setShowZeroBalanceAccounts(value: Boolean)` | `Unit` | Store zero balance accounts visibility preference |

## Implementation

### PreferencesRepositoryImpl

```kotlin
class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    override fun getShowZeroBalanceAccounts(): Boolean {
        return sharedPreferences.getBoolean(KEY_SHOW_ZERO_BALANCE, true)
    }

    override fun setShowZeroBalanceAccounts(value: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_SHOW_BALANCE, value)
            .apply()
    }

    companion object {
        private const val PREF_NAME = "mobileledger"
        private const val KEY_SHOW_ZERO_BALANCE = "showZeroBalanceAccounts"
    }
}
```

## Hilt Binding

### RepositoryModule

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    // ... existing bindings ...

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        impl: PreferencesRepositoryImpl
    ): PreferencesRepository
}
```

## Usage

### Migration from App Static Methods

**Before (App.kt)**:
```kotlin
// In App.kt
fun getShowZeroBalanceAccounts(): Boolean =
    prefs.getBoolean(KEY_SHOW_ZERO_BALANCE_ACCOUNTS, true)

fun storeShowZeroBalanceAccounts(value: Boolean) =
    prefs.edit().putBoolean(KEY_SHOW_ZERO_BALANCE_ACCOUNTS, value).apply()
```

**After (AccountSummaryViewModel)**:
```kotlin
@HiltViewModel
class AccountSummaryViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    // ...
) : ViewModel() {

    private fun loadInitialData() {
        _uiState.update {
            it.copy(showZeroBalanceAccounts = preferencesRepository.getShowZeroBalanceAccounts())
        }
    }

    private fun toggleZeroBalanceAccounts() {
        val newValue = !_uiState.value.showZeroBalanceAccounts
        _uiState.update { it.copy(showZeroBalanceAccounts = newValue) }
        preferencesRepository.setShowZeroBalanceAccounts(newValue)
        reloadAccounts()
    }
}
```

## Test Cases

### Unit Tests (PreferencesRepositoryTest.kt)

| Test | Description |
|------|-------------|
| `getShowZeroBalanceAccounts_defaultsToTrue` | Verify default value is true |
| `setShowZeroBalanceAccounts_persists` | Verify value is persisted |
| `getShowZeroBalanceAccounts_returnsStoredValue` | Verify stored value is returned |

### Test Setup

```kotlin
class PreferencesRepositoryTest {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var repository: PreferencesRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sharedPreferences = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().commit()
        repository = PreferencesRepositoryImpl(context)
    }

    @Test
    fun `getShowZeroBalanceAccounts defaults to true`() {
        assertEquals(true, repository.getShowZeroBalanceAccounts())
    }

    @Test
    fun `setShowZeroBalanceAccounts persists value`() {
        repository.setShowZeroBalanceAccounts(false)
        assertEquals(false, repository.getShowZeroBalanceAccounts())
    }
}
```

### Fake for Testing

```kotlin
class FakePreferencesRepository : PreferencesRepository {
    private var showZeroBalanceAccounts = true

    override fun getShowZeroBalanceAccounts(): Boolean = showZeroBalanceAccounts

    override fun setShowZeroBalanceAccounts(value: Boolean) {
        showZeroBalanceAccounts = value
    }

    fun reset() {
        showZeroBalanceAccounts = true
    }
}
```
