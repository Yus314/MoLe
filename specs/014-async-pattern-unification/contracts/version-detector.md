# Contract: VersionDetector

**Component**: バージョン検出 (Version Detection)
**Priority**: P1 (最もシンプル)
**Current Status**: 移行完了（純粋なCoroutines実装）

## Interface Definition

```kotlin
package net.ktnx.mobileledger.domain.usecase

/**
 * hledger-webサーバーのAPIバージョンを検出するUseCase
 *
 * 実装要件:
 * - suspend関数でResult<String>を返却
 * - HTTPヘッダーからバージョン情報を抽出
 * - 認証オプションをサポート
 */
interface VersionDetector {
    /**
     * サーバーのAPIバージョンを検出
     *
     * @param url サーバーのベースURL
     * @param useAuth 認証を使用するかどうか
     * @param authUser 認証ユーザー名（useAuthがtrueの場合）
     * @param authPassword 認証パスワード（useAuthがtrueの場合）
     * @return Result<String> 成功時はバージョン文字列、失敗時はエラー情報
     *
     * エラーケース:
     * - NetworkException: ネットワーク接続エラー
     * - AuthenticationException: 認証失敗
     * - ServerException: サーバーエラー
     */
    suspend fun detect(
        url: String,
        useAuth: Boolean = false,
        authUser: String? = null,
        authPassword: String? = null
    ): Result<String>
}
```

## Current Implementation (Reference)

既に純粋なCoroutines実装が完了している。他のコンポーネントの参考にできる。

```kotlin
@Singleton
class VersionDetectorImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : VersionDetector {

    override suspend fun detect(
        url: String,
        useAuth: Boolean,
        authUser: String?,
        authPassword: String?
    ): Result<String> = withContext(ioDispatcher) {
        runCatching {
            ensureActive()

            val connection = NetworkUtil.prepareConnection(
                url,
                useAuth,
                authUser,
                authPassword
            )

            ensureActive()

            connection.connect()

            val responseCode = connection.responseCode
            when {
                responseCode == 401 ->
                    throw BackgroundTaskException.AuthenticationException("Authentication failed")
                responseCode !in 200..299 ->
                    throw BackgroundTaskException.ServerException(responseCode, "Server error")
            }

            // Extract version from headers or response
            val version = connection.getHeaderField("X-Api-Version")
                ?: extractVersionFromResponse(connection.inputStream)
                ?: "unknown"

            connection.disconnect()
            version
        }
    }

    private fun extractVersionFromResponse(inputStream: InputStream): String? {
        // Parse response to find version info
        return inputStream.bufferedReader().use { reader ->
            // Implementation details...
            null
        }
    }
}
```

## Test Scenarios

### Success Path

```kotlin
@Test
fun `detect returns version from header`() = runTest {
    val mockServer = MockWebServer()
    mockServer.enqueue(
        MockResponse()
            .setResponseCode(200)
            .setHeader("X-Api-Version", "1.14")
    )

    val detector = VersionDetectorImpl(UnconfinedTestDispatcher(testScheduler))
    val result = detector.detect(mockServer.url("/").toString())

    assertTrue(result.isSuccess)
    assertEquals("1.14", result.getOrNull())
}
```

### Error Paths

```kotlin
@Test
fun `detect returns failure on network error`() = runTest {
    val detector = VersionDetectorImpl(UnconfinedTestDispatcher(testScheduler))

    val result = detector.detect("http://invalid.url.that.does.not.exist")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is BackgroundTaskException.NetworkException)
}

@Test
fun `detect returns failure on 401 response`() = runTest {
    val mockServer = MockWebServer()
    mockServer.enqueue(MockResponse().setResponseCode(401))

    val result = detector.detect(mockServer.url("/").toString())

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is BackgroundTaskException.AuthenticationException)
}

@Test
fun `detect returns failure on 500 response`() = runTest {
    val mockServer = MockWebServer()
    mockServer.enqueue(MockResponse().setResponseCode(500))

    val result = detector.detect(mockServer.url("/").toString())

    assertTrue(result.isFailure)
    val exception = result.exceptionOrNull() as BackgroundTaskException.ServerException
    assertEquals(500, exception.statusCode)
}
```

### With Authentication

```kotlin
@Test
fun `detect sends auth header when useAuth is true`() = runTest {
    val mockServer = MockWebServer()
    mockServer.enqueue(MockResponse().setResponseCode(200).setHeader("X-Api-Version", "1.14"))

    val result = detector.detect(
        mockServer.url("/").toString(),
        useAuth = true,
        authUser = "user",
        authPassword = "pass"
    )

    assertTrue(result.isSuccess)

    val request = mockServer.takeRequest()
    assertNotNull(request.getHeader("Authorization"))
}
```

### Cancellation Path

```kotlin
@Test
fun `detect is cancellable`() = runTest {
    val slowServer = MockWebServer()
    slowServer.enqueue(
        MockResponse()
            .setBodyDelay(5, TimeUnit.SECONDS)
            .setResponseCode(200)
    )

    val job = launch {
        detector.detect(slowServer.url("/").toString())
    }

    advanceTimeBy(100)
    job.cancel()

    assertTrue(job.isCancelled)
}
```

## Usage in ProfileDetailViewModel

```kotlin
@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    private val versionDetector: VersionDetector,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    fun testConnection(url: String, useAuth: Boolean, user: String?, password: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true) }

            val result = versionDetector.detect(url, useAuth, user, password)

            result.fold(
                onSuccess = { version ->
                    _uiState.update {
                        it.copy(
                            isTestingConnection = false,
                            connectionTestResult = ConnectionTestResult.Success(version)
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isTestingConnection = false,
                            connectionTestResult = ConnectionTestResult.Failure(error.message ?: "Unknown error")
                        )
                    }
                }
            )
        }
    }
}
```

## Migration Status

**This component is COMPLETE and serves as a reference implementation.**

### Implementation Pattern to Follow

1. **Inject Dispatcher** for testability
2. **Use `withContext(ioDispatcher)`** for I/O operations
3. **Use `runCatching`** for Result wrapping
4. **Call `ensureActive()`** at cancellation points
5. **Proper resource cleanup** (connection.disconnect())

### No Files to Remove

This component was created fresh without legacy Thread implementation.

### Reference Value

This is the **simplest completed example** and should be used as the template for migrating other components. The pattern demonstrates:

- Proper Dispatcher injection
- Cancellation support
- Error handling with typed exceptions
- Result wrapping
- HTTP connection handling
