# Contract: ConfigBackup

**Component**: バックアップ/リストア (Backup/Restore)
**Priority**: P3 (中程度の複雑さ)
**Current Status**: ラッパー実装（suspendCancellableCoroutine経由でConfigIO/Reader/Writerを使用）

## Interface Definition

```kotlin
package net.ktnx.mobileledger.domain.usecase

import android.net.Uri

/**
 * プロファイル設定のバックアップ/リストアを行うUseCase
 *
 * 実装要件:
 * - suspend関数でResult<Unit>を返却
 * - キャンセル対応（CancellationException）
 * - ファイルI/Oエラーのハンドリング
 */
interface ConfigBackup {
    /**
     * すべてのプロファイル設定をファイルにバックアップ
     *
     * @param uri バックアップ先のUri
     * @return Result<Unit> 成功時はUnit、失敗時はエラー情報
     *
     * エラーケース:
     * - FileException: ファイル書き込みエラー
     */
    suspend fun backup(uri: Uri): Result<Unit>

    /**
     * ファイルからプロファイル設定をリストア
     *
     * @param uri リストア元のUri
     * @return Result<Unit> 成功時はUnit、失敗時はエラー情報
     *
     * エラーケース:
     * - FileException: ファイル読み込みエラー
     * - ParseException: 不正なバックアップファイル形式
     */
    suspend fun restore(uri: Uri): Result<Unit>
}
```

## Implementation Requirements

### Required Dependencies

```kotlin
@Singleton
class ConfigBackupImpl @Inject constructor(
    private val context: Context,
    private val profileRepository: ProfileRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val templateRepository: TemplateRepository,
    private val currencyRepository: CurrencyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ConfigBackup
```

### Backup Implementation

```kotlin
override suspend fun backup(uri: Uri): Result<Unit> = withContext(ioDispatcher) {
    runCatching {
        ensureActive()

        val outputStream = context.contentResolver.openOutputStream(uri)
            ?: throw BackgroundTaskException.FileException("Cannot open output stream")

        outputStream.bufferedWriter().use { writer ->
            ensureActive()

            // Write all profiles
            val profiles = profileRepository.getAllProfilesSync()
            profiles.forEach { profile ->
                ensureActive()
                writeProfile(writer, profile)
            }
        }
    }
}
```

### Restore Implementation

```kotlin
override suspend fun restore(uri: Uri): Result<Unit> = withContext(ioDispatcher) {
    runCatching {
        ensureActive()

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw BackgroundTaskException.FileException("Cannot open input stream")

        inputStream.bufferedReader().use { reader ->
            ensureActive()

            val config = parseBackupFile(reader)

            // Restore profiles (using suspend repository methods)
            config.profiles.forEach { profileData ->
                ensureActive()
                profileRepository.insertProfile(profileData.toProfile())
            }
        }
    }
}
```

### Cancellation Support

```kotlin
// Check cancellation at each significant step
override suspend fun backup(uri: Uri): Result<Unit> = withContext(ioDispatcher) {
    runCatching {
        ensureActive() // Before file open

        outputStream.use { stream ->
            profiles.forEach { profile ->
                ensureActive() // Before each profile write
                writeProfile(stream, profile)
            }
        }
    }
}
```

## Test Scenarios

### Backup Success Path

```kotlin
@Test
fun `backup writes all profiles to file`() = runTest {
    fakeProfileRepository.profiles = listOf(testProfile1, testProfile2)

    val tempFile = File.createTempFile("backup", ".json")
    val uri = Uri.fromFile(tempFile)

    val result = configBackup.backup(uri)

    assertTrue(result.isSuccess)
    assertTrue(tempFile.exists())
    assertTrue(tempFile.readText().contains(testProfile1.name))
    assertTrue(tempFile.readText().contains(testProfile2.name))
}
```

### Restore Success Path

```kotlin
@Test
fun `restore imports profiles from file`() = runTest {
    val backupContent = """{"profiles":[{"name":"TestProfile","url":"http://test"}]}"""
    val tempFile = File.createTempFile("backup", ".json").apply { writeText(backupContent) }
    val uri = Uri.fromFile(tempFile)

    val result = configBackup.restore(uri)

    assertTrue(result.isSuccess)
    assertEquals(1, fakeProfileRepository.insertCallCount)
}
```

### Error Paths

```kotlin
@Test
fun `backup returns failure on file write error`() = runTest {
    val invalidUri = Uri.parse("content://invalid/path")

    val result = configBackup.backup(invalidUri)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is BackgroundTaskException.FileException)
}

@Test
fun `restore returns failure on invalid backup format`() = runTest {
    val invalidContent = "not valid json"
    val tempFile = File.createTempFile("backup", ".json").apply { writeText(invalidContent) }

    val result = configBackup.restore(Uri.fromFile(tempFile))

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is BackgroundTaskException.ParseException)
}
```

### Cancellation Path

```kotlin
@Test
fun `backup stops on cancellation`() = runTest {
    fakeProfileRepository.profiles = List(1000) { createTestProfile(it) } // Many profiles

    val job = launch {
        configBackup.backup(tempUri)
    }

    advanceTimeBy(10)
    job.cancel()

    assertTrue(job.isCancelled)
}
```

## Migration Notes

### Current Implementation (to be replaced)

`ConfigBackupImpl` currently wraps `ConfigWriter`/`ConfigReader` using `suspendCancellableCoroutine`:

```kotlin
// BEFORE (current - to be replaced)
override suspend fun backup(uri: Uri): Result<Unit> = suspendCancellableCoroutine { cont ->
    val writer = ConfigWriter(context, uri, onErrorListener, onDoneListener)
    cont.invokeOnCancellation { writer.interrupt() }
    writer.start()
}
```

### Target Implementation

Pure Coroutines without Thread wrapper:

```kotlin
// AFTER (target)
override suspend fun backup(uri: Uri): Result<Unit> = withContext(ioDispatcher) {
    runCatching {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            // Direct file writing using suspend repository methods
        }
    }
}
```

### Files to Remove After Migration

- `backup/ConfigIO.kt` (abstract Thread base class)
- `backup/ConfigReader.kt` (Thread subclass with runBlocking)
- `backup/ConfigWriter.kt` (Thread subclass)
- `backup/RawConfigReader.kt` (file parsing logic to be integrated into ConfigBackupImpl)
- `backup/RawConfigWriter.kt` (file writing logic to be integrated into ConfigBackupImpl)

### Key Issue: runBlocking in ConfigReader

Current `ConfigReader` uses `runBlocking` to call Repository coroutines:

```kotlin
// PROBLEMATIC (current)
p = runBlocking {
    profileRepository.getProfileByUuidSync(currentProfile)
}
```

This must be removed by making the entire read operation `suspend`:

```kotlin
// FIXED (target)
suspend fun readProfile(): Profile? {
    return profileRepository.getProfileByUuidSync(currentProfile)
}
```
