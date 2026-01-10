# Research: Data Layer Repository Migration

**Feature**: 008-data-layer-repository
**Date**: 2026-01-10

## Research Summary

本フィーチャーの技術的な不明点を調査し、設計決定の根拠を記録する。

---

## 1. Repository Pattern Best Practices in Android

### Decision
GoogleのGuide to app architectureに準拠したRepositoryパターンを採用する。

### Rationale
- Googleが公式に推奨するアーキテクチャパターン
- Now in Android (Google公式サンプル) で実装例あり
- データソースの抽象化とテスト容易性向上

### Alternatives Considered
1. **DAO直接使用継続**: テスト困難、Data.kt依存が解消されない
2. **UseCase のみ導入**: データアクセス層の抽象化が不十分

### Implementation Pattern
```kotlin
// Interface
interface ProfileRepository {
    fun getAllProfiles(): Flow<List<Profile>>
    suspend fun getProfileById(id: Long): Profile?
    suspend fun insertProfile(profile: Profile): Long
    suspend fun updateProfile(profile: Profile)
    suspend fun deleteProfile(profile: Profile)
}

// Implementation
class ProfileRepositoryImpl @Inject constructor(
    private val profileDAO: ProfileDAO
) : ProfileRepository {
    override fun getAllProfiles(): Flow<List<Profile>> =
        profileDAO.getAllOrdered().asFlow()
    // ...
}
```

---

## 2. LiveData to Flow Migration Strategy

### Decision
新規Repositoryメソッドは`Flow`を返す。既存のLiveData返却DAOメソッドは`asFlow()`で変換。

### Rationale
- Kotlin Coroutines Flow はGoogleが推奨する新しいリアクティブパターン
- Compose と相性が良い (`collectAsState()`)
- テストが容易 (`turbine` ライブラリ使用可)
- 既存DAOを変更せずにRepository層で変換可能

### Alternatives Considered
1. **LiveData継続**: Composeとの相性が悪く、テスト複雑
2. **DAO層でFlow化**: 既存コードへの影響大

### Conversion Pattern
```kotlin
// DAO returns LiveData
@Query("SELECT * FROM profiles ORDER BY orderNo")
fun getAllOrdered(): LiveData<List<Profile>>

// Repository converts to Flow
override fun getAllProfiles(): Flow<List<Profile>> =
    profileDAO.getAllOrdered().asFlow()
```

---

## 3. Current Profile State Management

### Decision
`ProfileRepository`が現在のプロファイル選択状態を管理する。`StateFlow<Profile?>`として公開。

### Rationale
- Data.ktの`profile: MutableLiveData<Profile?>`の責務をRepositoryに移行
- 複数ViewModelで共有される状態のため、Repositoryスコープ(@Singleton)が適切
- StateFlowはread-only公開で型安全

### Implementation Pattern
```kotlin
@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileDAO: ProfileDAO
) : ProfileRepository {
    private val _currentProfile = MutableStateFlow<Profile?>(null)
    override val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    override fun setCurrentProfile(profile: Profile?) {
        _currentProfile.value = profile
    }
}
```

---

## 4. Thread Safety for Concurrent Access

### Decision
Repositoryはスレッドセーフに設計。StateFlowは内部でスレッドセーフ。suspend関数はコルーチンディスパッチャで管理。

### Rationale
- MutableStateFlowはスレッドセーフ
- Room DAOのsuspend関数は内部でDispatchersを管理
- 明示的なsynchronized不要

### Pattern
```kotlin
// StateFlow is thread-safe
private val _currentProfile = MutableStateFlow<Profile?>(null)

// Suspend functions use Dispatchers.IO via Room
suspend fun getProfileById(id: Long): Profile? =
    withContext(Dispatchers.IO) {
        profileDAO.getByIdSync(id)
    }
```

---

## 5. Hilt DI Module Structure

### Decision
新規`RepositoryModule`を作成し、Repository Interface -> Impl のバインディングを定義。

### Rationale
- DatabaseModuleとの責務分離
- テスト時のモジュール差し替えが容易
- `@Binds`アノテーションでボイラープレート削減

### Implementation Pattern
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: ProfileRepositoryImpl
    ): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository

    // ...
}
```

---

## 6. AppStateManager Design

### Decision
Data.ktを`AppStateManager`にリネームし、データアクセス以外の状態のみ保持。

### Rationale
- UI状態(drawerOpen, locale)はデータ層の責務外
- バックグラウンドタスク状態はアプリ全体で共有されるUI状態
- 責務の明確化によりテスト容易性向上

### State Categories

**AppStateManager に残す (UI/App状態)**:
- `backgroundTasksRunning: MutableLiveData<Boolean>`
- `backgroundTaskProgress: MutableLiveData<Progress>`
- `drawerOpen: MutableLiveData<Boolean>`
- `currencySymbolPosition: MutableLiveData<Currency.Position>`
- `currencyGap: MutableLiveData<Boolean>`
- `locale: MutableLiveData<Locale>`
- `lastUpdateDate`, `lastUpdateTransactionCount`, etc. (UI表示用)

**ProfileRepository に移行**:
- `profiles: LiveData<List<Profile>>` → `getAllProfiles(): Flow<List<Profile>>`
- `profile: MutableLiveData<Profile?>` → `currentProfile: StateFlow<Profile?>`

---

## 7. ViewModel Migration Order

### Decision
P1 TransactionRepository → P2 ProfileRepository → P3 ViewModel移行 の順で実施。

### Rationale
- 取引機能が最も頻繁に修正される
- ProfileRepositoryはcurrentProfile状態管理を含むため慎重に移行
- ViewModel単位で完全移行（混在禁止）

### Migration Steps per ViewModel

1. **MainViewModel**:
   - Inject: ProfileRepository, TransactionRepository, AccountRepository
   - Remove: profileDAO, transactionDAO, accountDAO direct access
   - Keep: AppStateManager for drawer/task state

2. **NewTransactionViewModel**:
   - Inject: TransactionRepository, AccountRepository, TemplateRepository
   - Remove: DAO direct access
   - Keep: AppStateManager for number formatting (via locale)

3. **ProfileDetailViewModel**:
   - Inject: ProfileRepository
   - Remove: profileDAO direct access

4. **BackupsViewModel**:
   - Inject: ProfileRepository (for currentProfile check only)
   - Minimal changes

---

## 8. Testing Strategy

### Decision
Repository単体テストはFake DAOを注入してテスト。MockKは使用しない方針。

### Rationale
- Fakeは振る舞いを明示的に定義できる
- MockKはKotlin/Android互換性問題が発生することがある
- Google公式サンプル(Now in Android)もFake使用

### Test Pattern
```kotlin
class ProfileRepositoryTest {
    private lateinit var fakeProfileDAO: FakeProfileDAO
    private lateinit var repository: ProfileRepositoryImpl

    @Before
    fun setup() {
        fakeProfileDAO = FakeProfileDAO()
        repository = ProfileRepositoryImpl(fakeProfileDAO)
    }

    @Test
    fun getAllProfiles_returnsOrderedList() = runTest {
        fakeProfileDAO.insertProfile(Profile(name = "Test"))

        repository.getAllProfiles().first().let { profiles ->
            assertEquals(1, profiles.size)
            assertEquals("Test", profiles[0].name)
        }
    }
}
```

---

## References

1. [Guide to app architecture - Android Developers](https://developer.android.com/topic/architecture)
2. [Now in Android - GitHub](https://github.com/android/nowinandroid)
3. [Testing Kotlin coroutines - Android Developers](https://developer.android.com/kotlin/coroutines/test)
4. [StateFlow and SharedFlow - Kotlin Docs](https://kotlinlang.org/docs/flow.html#stateflow-and-sharedflow)
