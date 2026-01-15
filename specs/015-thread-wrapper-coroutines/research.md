# Research: Thread Wrapper to Coroutines Migration

**Date**: 2026-01-15
**Feature**: 015-thread-wrapper-coroutines

## 1. Current Thread Implementation Analysis

### 1.1 RetrieveTransactionsTask (COMPLEX)

**Location**: `app/src/main/kotlin/net/ktnx/mobileledger/async/RetrieveTransactionsTask.kt`

**Current Implementation**:
- Thread サブクラス（554行）
- 内部に `AccountAndTransactionListSaver` という nested Thread（line 715）
- nested Thread 内で `runBlocking` を使用して Repository 呼び出し
- HTML/JSON パーサーが混在（API バージョンによる分岐）
- `TransactionSyncerImpl` が `callbackFlow` + `suspendCancellableCoroutine` でラップ

**Migration Decision**: Thread を完全に排除し、`TransactionSyncerImpl` に直接 suspend 関数として実装
**Rationale**: runBlocking の排除により TestDispatcher で即座にテスト可能
**Alternatives Rejected**:
- Thread をそのまま維持 → テスト時に実際のスレッド待機が発生

### 1.2 SendTransactionTask (MEDIUM)

**Location**: `app/src/main/kotlin/net/ktnx/mobileledger/async/SendTransactionTask.kt`

**Current Implementation**:
- Thread サブクラス（deprecated マーク済み）
- `Thread.sleep(1500)` でシミュレーションモード待機
- `Thread.sleep(100)` でリトライ待機
- `TaskCallback` でコールバック通知
- `TransactionSenderImpl` が `suspendCancellableCoroutine` でラップ

**Migration Decision**: `delay()` に置換、suspend 関数として直接実装
**Rationale**: Thread.sleep を delay に置換することで TestDispatcher の時間操作が可能
**Alternatives Rejected**:
- sleep を維持 → テスト時間が実際にかかる

### 1.3 ConfigIO/ConfigWriter/ConfigReader (MEDIUM)

**Location**: `app/src/main/kotlin/net/ktnx/mobileledger/backup/`

**Current Implementation**:
- `ConfigIO` が Thread の抽象基底クラス
- `ConfigWriter`/`ConfigReader` が継承
- `RawConfigWriter` で `runBlocking` を使用（lines 79, 82, 84）
- `RawConfigReader` で `runBlocking` を使用（lines 166, 176, 186, 227）
- `ConfigBackupImpl` が `suspendCancellableCoroutine` でラップ

**Migration Decision**: Thread 基底クラスを廃止し、suspend 関数として再実装
**Rationale**: runBlocking 排除により全体が suspend チェーンになる
**Alternatives Rejected**:
- 部分的な移行 → runBlocking が残り、テストが困難

### 1.4 TransactionsDisplayedFilter (SIMPLE)

**Location**: `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/TransactionListViewModel.kt:405`

**Current Implementation**:
- Thread サブクラス
- `isInterrupted` チェックでキャンセル対応
- 同期的なフィルタリング処理

**Migration Decision**: `viewModelScope.launch` + `ensureActive()` に置換
**Rationale**: ViewModel スコープと連動したキャンセル処理が自動化される
**Alternatives Rejected**: なし

### 1.5 VersionDetectionThread (SIMPLE)

**Location**: `app/src/main/kotlin/net/ktnx/mobileledger/ui/profiles/ProfileDetailModel.kt:331`

**Current Implementation**:
- Thread サブクラス
- `Thread.sleep()` でスロットリング
- `LiveData.postValue` で結果通知

**Migration Decision**: `VersionDetectorImpl` を使用し、ViewModel で `viewModelScope.launch`
**Rationale**: 既存の `VersionDetector` インターフェースと `VersionDetectorImpl` が存在
**Alternatives Rejected**: なし

### 1.6 BaseDAO.asyncRunner (SIMPLE)

**Location**: `app/src/main/kotlin/net/ktnx/mobileledger/dao/BaseDAO.kt:69`

**Current Implementation**:
- `Executors.newSingleThreadExecutor()`
- `insert()`, `update()`, `delete()` で使用
- `Misc.onMainThread` でコールバック通知

**Migration Decision**: 既存の suspend DAO メソッドを直接使用
**Rationale**: Room DAO は既に suspend 関数をサポート
**Alternatives Rejected**: なし

### 1.7 GeneralBackgroundTasks (SIMPLE)

**Location**: `app/src/main/kotlin/net/ktnx/mobileledger/async/GeneralBackgroundTasks.kt`

**Current Implementation**:
- `Executors.newFixedThreadPool()`
- 汎用バックグラウンドタスク実行

**Migration Decision**: 削除し、呼び出し元で `viewModelScope.launch` または `withContext(Dispatchers.IO)` を使用
**Rationale**: Coroutines で同等の機能が提供される
**Alternatives Rejected**: なし

## 2. Coroutines Best Practices

### 2.1 Structured Concurrency

**Decision**: 全ての Coroutines は適切なスコープ（`viewModelScope`, `CoroutineScope`）で起動
**Rationale**: GlobalScope 禁止（Constitution VI）、キャンセル伝播の保証

### 2.2 Dispatcher Injection

**Decision**: `@IoDispatcher` アノテーションで `CoroutineDispatcher` を注入
**Rationale**: テスト時に `TestDispatcher` を注入可能

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
```

### 2.3 Cancellation Handling

**Decision**: `ensureActive()` または `isActive` チェックで cooperative cancellation
**Rationale**: Coroutines のキャンセルは cooperative、明示的なチェックが必要

```kotlin
suspend fun longRunningTask() {
    for (item in items) {
        ensureActive() // CancellationException をスロー
        process(item)
    }
}
```

### 2.4 Flow vs suspend function

**Decision**:
- 進捗報告が必要 → `Flow<Progress>`
- 単一結果 → `suspend fun`: `Result<T>`

**Rationale**: TransactionSyncer は進捗報告が必要なので Flow、TransactionSender は結果のみなので suspend

## 3. Testing Patterns

### 3.1 TestDispatcher Usage

**Decision**: `kotlinx-coroutines-test` の `StandardTestDispatcher` を使用

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionSyncerImplTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun `sync completes without real delay`() = runTest {
        val syncer = TransactionSyncerImpl(
            dispatcher = testDispatcher,
            // ... other dependencies
        )

        syncer.sync(profile).collect { progress ->
            // progress は即座に受信される
        }

        advanceUntilIdle() // 全ての pending coroutines を実行
    }
}
```

### 3.2 Fake Implementation Compatibility

**Decision**: 既存の Fake 実装のインターフェースを維持

```kotlin
// 既存の FakeTransactionSyncer はそのまま使用可能
class FakeTransactionSyncer : TransactionSyncer {
    override fun sync(profile: Profile): Flow<SyncProgress> = flow {
        emit(SyncProgress.Starting("..."))
        // TestDispatcher で即座に完了
    }
}
```

## 4. Migration Risks and Mitigations

### 4.1 Network I/O Thread Safety

**Risk**: Network I/O が Main スレッドで実行される可能性
**Mitigation**: `withContext(Dispatchers.IO)` で明示的に IO スレッドに切り替え

### 4.2 Progress Reporting Timing

**Risk**: Flow emission のタイミングが Thread と異なる可能性
**Mitigation**: 既存のテストで動作確認、必要に応じて `yield()` で制御

### 4.3 Cancellation Propagation

**Risk**: 長時間処理中のキャンセルが適切に伝播されない
**Mitigation**: 処理ループ内で `ensureActive()` を呼び出し

### 4.4 Exception Handling

**Risk**: Coroutine 内の例外が適切に伝播されない
**Mitigation**: `supervisorScope` の使用は避け、通常のスコープで例外を伝播

## 5. Summary of Decisions

| Component | Current | Target | Migration Complexity |
|-----------|---------|--------|---------------------|
| RetrieveTransactionsTask | Thread + runBlocking | suspend function | HIGH |
| SendTransactionTask | Thread + callback | suspend function | MEDIUM |
| ConfigWriter/Reader | Thread + runBlocking | suspend function | MEDIUM |
| TransactionsDisplayedFilter | Thread | viewModelScope.launch | LOW |
| VersionDetectionThread | Thread | VersionDetectorImpl | LOW |
| BaseDAO.asyncRunner | Executor | Remove (use suspend DAO) | LOW |
| GeneralBackgroundTasks | Executor | Remove | LOW |
