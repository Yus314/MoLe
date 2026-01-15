# Contract: DatabaseInitializer

**Component**: データベース初期化 (Database Initialization)
**Priority**: P2 (シンプル)
**Current Status**: 移行完了（純粋なCoroutines実装）

## Interface Definition

```kotlin
package net.ktnx.mobileledger.domain.usecase

/**
 * アプリ起動時のデータベース初期化を行うUseCase
 *
 * 実装要件:
 * - suspend関数でResult<Boolean>を返却
 * - プロファイルの存在確認
 * - 初期化完了状態の管理
 */
interface DatabaseInitializer {
    /**
     * データベースを初期化し、プロファイルの存在を確認
     *
     * @return Result<Boolean> 成功時はプロファイルが存在するかどうか、失敗時はエラー情報
     *
     * 戻り値:
     * - true: 1つ以上のプロファイルが存在
     * - false: プロファイルが存在しない（初回起動）
     */
    suspend fun initialize(): Result<Boolean>

    /**
     * 初期化が完了したかどうか
     */
    val isInitialized: Boolean
}
```

## Current Implementation (Reference)

既に純粋なCoroutines実装が完了している。他のコンポーネントの参考にできる。

```kotlin
@Singleton
class DatabaseInitializerImpl @Inject constructor(
    private val profileRepository: ProfileRepository
) : DatabaseInitializer {

    private var _isInitialized = false
    override val isInitialized: Boolean
        get() = _isInitialized

    override suspend fun initialize(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val profileCount = profileRepository.getProfileCount()
            _isInitialized = true
            profileCount > 0
        }
    }
}
```

## Test Scenarios

### Success Paths

```kotlin
@Test
fun `initialize returns true when profiles exist`() = runTest {
    fakeProfileRepository.profileCount = 2

    val result = databaseInitializer.initialize()

    assertTrue(result.isSuccess)
    assertTrue(result.getOrNull() == true)
    assertTrue(databaseInitializer.isInitialized)
}

@Test
fun `initialize returns false when no profiles exist`() = runTest {
    fakeProfileRepository.profileCount = 0

    val result = databaseInitializer.initialize()

    assertTrue(result.isSuccess)
    assertTrue(result.getOrNull() == false)
    assertTrue(databaseInitializer.isInitialized)
}
```

### Error Path

```kotlin
@Test
fun `initialize returns failure on database error`() = runTest {
    fakeProfileRepository.shouldThrowError = true

    val result = databaseInitializer.initialize()

    assertTrue(result.isFailure)
    assertFalse(databaseInitializer.isInitialized)
}
```

## Usage in SplashViewModel

```kotlin
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val databaseInitializer: DatabaseInitializer
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val result = databaseInitializer.initialize()
            result.fold(
                onSuccess = { hasProfiles ->
                    _uiState.value = SplashUiState(
                        isLoading = false,
                        navigateTo = if (hasProfiles) Screen.Main else Screen.ProfileSetup
                    )
                },
                onFailure = { error ->
                    _uiState.value = SplashUiState(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
}
```

## Migration Status

**This component is COMPLETE and serves as a reference implementation.**

### Implementation Pattern to Follow

1. **Inject Repository** (not DAO directly)
2. **Use `withContext(Dispatchers.IO)`** for I/O operations
3. **Use `runCatching`** for Result wrapping
4. **Simple, single-responsibility** suspend function

### No Files to Remove

This component was created fresh without legacy Thread implementation.
