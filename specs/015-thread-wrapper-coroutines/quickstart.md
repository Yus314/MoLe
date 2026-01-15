# Quickstart: Thread Wrapper to Coroutines Migration

**Date**: 2026-01-15
**Feature**: 015-thread-wrapper-coroutines

## Overview

このガイドでは、MoLe の Thread ベース非同期処理を pure Coroutines に移行する方法を説明する。

## Prerequisites

- Kotlin 2.0.21
- Kotlin Coroutines 1.9.0
- Hilt 2.51.1
- kotlinx-coroutines-test（テスト用）

## Migration Pattern

### Before: Thread + suspendCancellableCoroutine

```kotlin
// 現在の実装（Thread wrapper）
@Singleton
class TransactionSenderImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TransactionSender {

    override suspend fun send(...): Result<Unit> = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val callback = TaskCallback { error, _ ->
                if (error == null) {
                    continuation.resume(Result.success(Unit))
                } else {
                    continuation.resume(Result.failure(Exception(error)))
                }
            }

            // Thread を起動
            val task = SendTransactionTask(callback, profile, transaction, simulate)
            continuation.invokeOnCancellation { task.interrupt() }
            task.start()
        }
    }
}
```

### After: Pure Coroutines

```kotlin
// 移行後の実装（pure suspend）
@Singleton
class TransactionSenderImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TransactionSender {

    override suspend fun send(
        profile: Profile,
        transaction: LedgerTransaction,
        simulate: Boolean
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            if (simulate) {
                delay(1500) // TestDispatcher で即座にスキップ
            }

            // 直接 HTTP リクエスト
            val response = performHttpRequest(profile, transaction)
            parseResponse(response)

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

## Key Migration Steps

### Step 1: Thread.sleep() → delay()

```kotlin
// Before
Thread.sleep(1500)

// After
delay(1500) // TestDispatcher.advanceTimeBy() でスキップ可能
```

### Step 2: Thread.interrupt() → Job.cancel()

```kotlin
// Before
private var thread: Thread? = null
fun cancel() {
    thread?.interrupt()
}

// After
private var job: Job? = null
fun cancel() {
    job?.cancel()
}
```

### Step 3: runBlocking → suspend

```kotlin
// Before (Thread 内)
runBlocking {
    repository.saveData(data)
}

// After (suspend 関数内)
repository.saveData(data) // 直接呼び出し
```

### Step 4: Callback → Result

```kotlin
// Before
interface TaskCallback {
    fun onDone(error: String?, result: Any?)
}

// After
suspend fun doTask(): Result<Data>
```

### Step 5: isInterrupted → ensureActive()

```kotlin
// Before
for (item in items) {
    if (Thread.currentThread().isInterrupted) return
    process(item)
}

// After
for (item in items) {
    ensureActive() // CancellationException をスロー
    process(item)
}
```

## Testing Pattern

### Before: Real Thread Wait

```kotlin
@Test
fun `send transaction succeeds`() = runBlocking {
    // 実際に 1500ms 待機する
    val result = sender.send(profile, transaction, simulate = true)
    assertEquals(Result.success(Unit), result)
}
```

### After: Instant with TestDispatcher

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
@Test
fun `send transaction succeeds`() = runTest {
    // TestDispatcher を注入
    val sender = TransactionSenderImpl(
        ioDispatcher = StandardTestDispatcher(testScheduler)
    )

    val result = sender.send(profile, transaction, simulate = true)

    // delay(1500) は即座にスキップされる
    advanceUntilIdle()

    assertEquals(Result.success(Unit), result)
}
```

## ViewModel Migration

### Before: Thread in ViewModel

```kotlin
class MyViewModel : ViewModel() {
    private var filterThread: Thread? = null

    private inner class FilterThread : Thread() {
        override fun run() {
            for (item in items) {
                if (isInterrupted) return
                process(item)
            }
        }
    }

    fun startFilter() {
        filterThread?.interrupt()
        filterThread = FilterThread().also { it.start() }
    }
}
```

### After: viewModelScope

```kotlin
class MyViewModel : ViewModel() {
    private var filterJob: Job? = null

    fun startFilter() {
        filterJob?.cancel()
        filterJob = viewModelScope.launch(Dispatchers.Default) {
            for (item in items) {
                ensureActive()
                process(item)
            }
        }
    }

    // onCleared() で自動キャンセル（viewModelScope の利点）
}
```

## DI Setup

### DispatchersModule

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
```

### Test Module

```kotlin
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DispatchersModule::class]
)
@Module
object TestDispatchersModule {
    @Provides
    @IoDispatcher
    fun provideTestIoDispatcher(): CoroutineDispatcher =
        StandardTestDispatcher()
}
```

## Checklist

移行時のチェックリスト:

- [ ] Thread サブクラスを削除
- [ ] runBlocking を排除（テスト以外）
- [ ] Thread.sleep() を delay() に置換
- [ ] Thread.interrupt() を Job.cancel() に置換
- [ ] isInterrupted を ensureActive() に置換
- [ ] Callback を Result または Flow に置換
- [ ] @IoDispatcher を注入可能に
- [ ] TestDispatcher でのテストを追加
- [ ] 既存 Fake 実装との互換性を確認
