# Contract: TransactionSender

**Component**: 取引送信 (Transaction Sending)
**Priority**: P4 (中程度の複雑さ)
**Current Status**: ラッパー実装（suspendCancellableCoroutine経由でSendTransactionTaskを使用）

## Interface Definition

```kotlin
package net.ktnx.mobileledger.domain.usecase

import net.ktnx.mobileledger.model.LedgerTransaction
import net.ktnx.mobileledger.model.Profile

/**
 * トランザクションをhledger-webサーバーに送信するUseCase
 *
 * 実装要件:
 * - suspend関数でResult<Unit>を返却
 * - キャンセル対応（CancellationException）
 * - シミュレーションモードをサポート
 */
interface TransactionSender {
    /**
     * トランザクションをサーバーに送信
     *
     * @param profile 送信先プロファイル（URL、認証情報を含む）
     * @param transaction 送信するトランザクション
     * @param simulate true の場合、実際には送信せずシミュレーションを実行
     * @return Result<Unit> 成功時はUnit、失敗時はエラー情報
     *
     * エラーケース:
     * - NetworkException: ネットワーク接続エラー
     * - AuthenticationException: 認証失敗
     * - ServerException: サーバーエラー（4xx, 5xx）
     */
    suspend fun send(
        profile: Profile,
        transaction: LedgerTransaction,
        simulate: Boolean = false
    ): Result<Unit>
}
```

## Implementation Requirements

### Required Dependencies

```kotlin
@Singleton
class TransactionSenderImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TransactionSender
```

### HTTP Request Format

```kotlin
// POST to /add endpoint
// Content-Type: application/x-www-form-urlencoded
// Body format depends on API version:
//   - 1.14+: JSON format via Gateway
//   - Older: Form-encoded format

suspend fun send(...): Result<Unit> = withContext(ioDispatcher) {
    runCatching {
        val connection = NetworkUtil.prepareConnection(
            profile.url + "/add",
            profile.useAuthentication,
            profile.authUser,
            profile.authPassword
        )
        connection.requestMethod = "PUT"
        connection.doOutput = true

        // Detect API version
        val apiVersion = detectApiVersion(profile)

        // Build and send request body
        val body = buildRequestBody(transaction, apiVersion)
        connection.outputStream.write(body.toByteArray())

        // Handle response
        val responseCode = connection.responseCode
        when {
            responseCode in 200..299 -> Unit
            responseCode == 401 -> throw BackgroundTaskException.AuthenticationException("Authentication failed")
            else -> throw BackgroundTaskException.ServerException(responseCode, "Server error: $responseCode")
        }
    }
}
```

### Cancellation Support

```kotlin
override suspend fun send(...): Result<Unit> = withContext(ioDispatcher) {
    runCatching {
        ensureActive() // Check cancellation before network call

        val connection = NetworkUtil.prepareConnection(...)

        ensureActive() // Check cancellation after connection setup

        // Send request...

        ensureActive() // Check cancellation before reading response

        // Read response...
    }
}
```

## Test Scenarios

### Success Path

```kotlin
@Test
fun `send returns success on 200 response`() = runTest {
    val mockServer = MockWebServer()
    mockServer.enqueue(MockResponse().setResponseCode(200))

    val sender = TransactionSenderImpl(
        UnconfinedTestDispatcher(testScheduler)
    )

    val result = sender.send(profileWithMockServerUrl, testTransaction)

    assertTrue(result.isSuccess)
    assertEquals(1, mockServer.requestCount)
}
```

### Error Paths

```kotlin
@Test
fun `send returns failure on network error`() = runTest {
    val sender = TransactionSenderImpl(
        UnconfinedTestDispatcher(testScheduler)
    )

    // Profile with invalid URL
    val result = sender.send(profileWithInvalidUrl, testTransaction)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is BackgroundTaskException.NetworkException)
}

@Test
fun `send returns failure on 401 response`() = runTest {
    val mockServer = MockWebServer()
    mockServer.enqueue(MockResponse().setResponseCode(401))

    val result = sender.send(profileWithMockServerUrl, testTransaction)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is BackgroundTaskException.AuthenticationException)
}

@Test
fun `send returns failure on 500 response`() = runTest {
    val mockServer = MockWebServer()
    mockServer.enqueue(MockResponse().setResponseCode(500))

    val result = sender.send(profileWithMockServerUrl, testTransaction)

    assertTrue(result.isFailure)
    val exception = result.exceptionOrNull() as BackgroundTaskException.ServerException
    assertEquals(500, exception.statusCode)
}
```

### Simulation Mode

```kotlin
@Test
fun `send in simulation mode does not send to server`() = runTest {
    val mockServer = MockWebServer()

    val result = sender.send(profile, transaction, simulate = true)

    assertTrue(result.isSuccess)
    // With simulation, we might still hit the server but with different behavior
    // Or mock internal simulation delay
}
```

### Cancellation Path

```kotlin
@Test
fun `send throws CancellationException when cancelled`() = runTest {
    val slowServer = MockWebServer()
    slowServer.enqueue(MockResponse().setBodyDelay(5, TimeUnit.SECONDS).setResponseCode(200))

    val job = launch {
        sender.send(profileWithSlowServerUrl, testTransaction)
    }

    advanceTimeBy(100)
    job.cancel()

    assertTrue(job.isCancelled)
}
```

## Migration Notes

### Current Implementation (to be replaced)

`TransactionSenderImpl` currently wraps `SendTransactionTask` using `suspendCancellableCoroutine`:

```kotlin
// BEFORE (current - to be replaced)
override suspend fun send(...): Result<Unit> = withContext(Dispatchers.IO) {
    suspendCancellableCoroutine { continuation ->
        val callback = TaskCallback { error ->
            if (error == null) continuation.resume(Result.success(Unit))
            else continuation.resume(Result.failure(error))
        }
        val task = SendTransactionTask(callback, profile, transaction, simulate)
        continuation.invokeOnCancellation { task.interrupt() }
        task.start()
    }
}
```

### Target Implementation

Pure Coroutines without Thread wrapper:

```kotlin
// AFTER (target)
override suspend fun send(...): Result<Unit> = withContext(ioDispatcher) {
    runCatching {
        val connection = NetworkUtil.prepareConnection(...)
        // Direct HTTP request handling
        // Direct response handling
    }
}
```

### Files to Remove After Migration

- `async/SendTransactionTask.kt` (~100 lines)
- `async/TaskCallback.kt` (callback interface)
