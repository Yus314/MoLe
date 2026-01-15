# Quickstart: 非同期処理パターンの統一

**Feature Branch**: `014-async-pattern-unification`
**Date**: 2026-01-15

このドキュメントは、開発者がバックグラウンド処理を理解し、新しいCoroutinesパターンに移行するためのガイドです。

---

## 1. 現在の状態

### パターン比較

| パターン | ファイル例 | 状態 | 推奨 |
|---------|-----------|------|------|
| Thread継承 | `RetrieveTransactionsTask.kt` | 非推奨 | ❌ |
| ExecutorService | `GeneralBackgroundTasks.kt` | 非推奨 | ❌ |
| コールバック | `TaskCallback.kt` | 非推奨 | ❌ |
| **Coroutines** | `VersionDetectorImpl.kt` | **推奨** | ✅ |

### 移行状況

| コンポーネント | 状態 | インターフェース | 実装 |
|---------------|------|-----------------|------|
| VersionDetector | ✅ 完了 | Coroutines | 純粋Coroutines |
| DatabaseInitializer | ✅ 完了 | Coroutines | 純粋Coroutines |
| ConfigBackup | 🟡 移行中 | Coroutines | ラッパー（Thread） |
| TransactionSender | 🟡 移行中 | Coroutines | ラッパー（Thread） |
| TransactionSyncer | 🟡 移行中 | Coroutines | ラッパー（Thread） |

---

## 2. 推奨パターン

### 2.1 単純な非同期処理

結果のみを返す処理には `suspend fun` + `Result<T>` を使用。

```kotlin
// インターフェース定義
interface MyUseCase {
    suspend fun execute(param: String): Result<Data>
}

// 実装
@Singleton
class MyUseCaseImpl @Inject constructor(
    private val repository: MyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MyUseCase {

    override suspend fun execute(param: String): Result<Data> = withContext(ioDispatcher) {
        runCatching {
            ensureActive() // キャンセルチェック
            repository.getData(param)
        }
    }
}

// ViewModel での使用
@HiltViewModel
class MyViewModel @Inject constructor(
    private val myUseCase: MyUseCase
) : ViewModel() {

    fun doSomething(param: String) {
        viewModelScope.launch {
            val result = myUseCase.execute(param)
            result.fold(
                onSuccess = { data -> _uiState.update { it.copy(data = data) } },
                onFailure = { error -> _uiState.update { it.copy(error = error.message) } }
            )
        }
    }
}
```

### 2.2 進捗付き非同期処理

進捗レポートが必要な処理には `Flow<Progress>` を使用。

```kotlin
// インターフェース定義
interface MySyncUseCase {
    fun sync(profile: Profile): Flow<SyncProgress>
}

// 実装
@Singleton
class MySyncUseCaseImpl @Inject constructor(
    private val repository: MyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MySyncUseCase {

    override fun sync(profile: Profile): Flow<SyncProgress> = flow {
        emit(SyncProgress.Starting)

        val items = withContext(ioDispatcher) {
            repository.fetchItems(profile)
        }

        items.forEachIndexed { index, item ->
            ensureActive()
            emit(SyncProgress.Processing(Progress(current = index, total = items.size)))
            processItem(item)
        }

        emit(SyncProgress.Completed)
    }.flowOn(ioDispatcher)
}

// ViewModel での使用
@HiltViewModel
class MyViewModel @Inject constructor(
    private val syncUseCase: MySyncUseCase
) : ViewModel() {

    private var syncJob: Job? = null

    fun startSync(profile: Profile) {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            syncUseCase.sync(profile).collect { progress ->
                _uiState.update { it.copy(syncProgress = progress) }
            }
        }
    }

    fun cancelSync() {
        syncJob?.cancel()
        syncJob = null
    }
}
```

---

## 3. エラーハンドリング

### 3.1 エラー型

```kotlin
sealed class BackgroundTaskException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    class NetworkException(message: String, cause: Throwable? = null)
        : BackgroundTaskException(message, cause)

    class AuthenticationException(message: String)
        : BackgroundTaskException(message)

    class ServerException(val statusCode: Int, message: String)
        : BackgroundTaskException(message)

    class ParseException(message: String, cause: Throwable? = null)
        : BackgroundTaskException(message, cause)

    class FileException(message: String, cause: Throwable? = null)
        : BackgroundTaskException(message, cause)
}
```

### 3.2 エラーハンドリングの例

```kotlin
// UseCase内
override suspend fun execute(): Result<Data> = withContext(ioDispatcher) {
    runCatching {
        try {
            val response = httpClient.get(url)
            when (response.code) {
                401 -> throw BackgroundTaskException.AuthenticationException("Auth failed")
                in 400..499 -> throw BackgroundTaskException.ServerException(response.code, "Client error")
                in 500..599 -> throw BackgroundTaskException.ServerException(response.code, "Server error")
            }
            parseResponse(response.body)
        } catch (e: IOException) {
            throw BackgroundTaskException.NetworkException("Network error", e)
        }
    }
}

// ViewModel内
viewModelScope.launch {
    val result = useCase.execute()
    result.fold(
        onSuccess = { /* handle success */ },
        onFailure = { error ->
            when (error) {
                is BackgroundTaskException.NetworkException ->
                    showError("ネットワーク接続を確認してください")
                is BackgroundTaskException.AuthenticationException ->
                    navigateToLogin()
                is BackgroundTaskException.ServerException ->
                    showError("サーバーエラー: ${error.statusCode}")
                else ->
                    showError(error.message ?: "Unknown error")
            }
        }
    )
}
```

---

## 4. テストの書き方

### 4.1 Fake実装

```kotlin
// FakeMyUseCase.kt
class FakeMyUseCase : MyUseCase {
    var shouldSucceed = true
    var result: Data = Data()
    var callCount = 0

    override suspend fun execute(param: String): Result<Data> {
        callCount++
        return if (shouldSucceed) {
            Result.success(result)
        } else {
            Result.failure(BackgroundTaskException.NetworkException("Fake error"))
        }
    }
}
```

### 4.2 ユニットテスト

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeUseCase: FakeMyUseCase
    private lateinit var viewModel: MyViewModel

    @Before
    fun setup() {
        fakeUseCase = FakeMyUseCase()
        viewModel = MyViewModel(fakeUseCase)
    }

    @Test
    fun `doSomething updates state on success`() = runTest {
        // Given
        fakeUseCase.shouldSucceed = true
        fakeUseCase.result = Data("test")

        // When
        viewModel.doSomething("param")
        advanceUntilIdle()

        // Then
        assertEquals(Data("test"), viewModel.uiState.value.data)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `doSomething shows error on failure`() = runTest {
        // Given
        fakeUseCase.shouldSucceed = false

        // When
        viewModel.doSomething("param")
        advanceUntilIdle()

        // Then
        assertNotNull(viewModel.uiState.value.error)
    }
}
```

### 4.3 Flow のテスト

```kotlin
@Test
fun `sync emits correct progress sequence`() = runTest {
    // Given
    val fakeRepository = FakeRepository()
    val useCase = MySyncUseCaseImpl(fakeRepository, UnconfinedTestDispatcher(testScheduler))

    // When & Then
    useCase.sync(testProfile).test {
        assertEquals(SyncProgress.Starting, awaitItem())
        assertTrue(awaitItem() is SyncProgress.Processing)
        assertEquals(SyncProgress.Completed, awaitItem())
        awaitComplete()
    }
}
```

---

## 5. 移行チェックリスト

新しいバックグラウンド処理を実装する際のチェックリスト：

### 設計

- [ ] インターフェースを定義（`suspend fun` または `Flow` を返す）
- [ ] 適切なエラー型を選択
- [ ] キャンセルポイントを特定

### 実装

- [ ] `@Inject constructor` でDI設定
- [ ] `@IoDispatcher` で Dispatcher を注入
- [ ] `withContext(ioDispatcher)` でI/O操作をラップ
- [ ] `runCatching` で Result をラップ
- [ ] `ensureActive()` でキャンセルチェック

### テスト

- [ ] Fake実装を作成
- [ ] 成功パスのテストを作成
- [ ] エラーパスのテストを作成
- [ ] キャンセルのテストを作成
- [ ] テストが1秒以内に完了することを確認

### DI設定

- [ ] `UseCaseModule.kt` に `@Binds` または `@Provides` を追加

---

## 6. 参考実装

### 完成した実装（テンプレートとして使用）

- `domain/usecase/VersionDetectorImpl.kt` - シンプルなHTTP処理
- `domain/usecase/DatabaseInitializerImpl.kt` - Repository呼び出し

### 現在のラッパー実装（移行対象）

- `domain/usecase/TransactionSyncerImpl.kt` - 複雑な進捗レポート
- `domain/usecase/TransactionSenderImpl.kt` - HTTP POST
- `domain/usecase/ConfigBackupImpl.kt` - ファイルI/O

### Fake実装（テスト用）

- `fake/FakeVersionDetector.kt`
- `fake/FakeDatabaseInitializer.kt`
- `fake/FakeTransactionSyncer.kt`
- `fake/FakeTransactionSender.kt`
- `fake/FakeConfigBackup.kt`

---

## 7. よくある質問

### Q: 既存のThread継承コードはいつ削除されますか？

A: すべてのコンポーネントの移行が完了した後に一括削除します。移行期間中は新旧のコードが共存しますが、新しいコードは常にCoroutinesインターフェースを使用してください。

### Q: `GlobalScope` を使用してもいいですか？

A: いいえ。`GlobalScope` は禁止です。ViewModelでは `viewModelScope` を、その他の場所では適切にスコープされた `CoroutineScope` を使用してください。

### Q: `runBlocking` を使用してもいいですか？

A: テストコード以外では禁止です。`runBlocking` は Thread をブロックするため、Coroutines のメリットを失います。代わりに `suspend fun` を使用してください。

### Q: 進捗レポートが必要な処理はどうすればいいですか？

A: `Flow<Progress>` を使用してください。ViewModel で `collect` して UI に反映します。詳細は「2.2 進捗付き非同期処理」を参照してください。

### Q: キャンセルに対応する必要がありますか？

A: はい。すべてのバックグラウンド処理はキャンセルに対応する必要があります。長い処理の前に `ensureActive()` を呼び出してください。
