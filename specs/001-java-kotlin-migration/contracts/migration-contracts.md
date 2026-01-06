# Migration Contracts: Java から Kotlin への移行

**Branch**: `001-java-kotlin-migration` | **Date**: 2026-01-05
**Purpose**: 移行中に維持すべきインターフェース契約

## Overview

本移行プロジェクトでは新規 API を作成しないが、既存の Java コードとの相互運用性を維持するため、以下の契約を定義する。

---

## 1. DAO Contracts

Room DAO インターフェースは移行中も完全な後方互換性を維持する。

### AccountDAO Contract

```kotlin
@Dao
interface AccountDAO {
    // 必須メソッド（シグネチャ維持）
    @Query("SELECT * FROM accounts WHERE profile_id = :profileId")
    fun getAll(profileId: Long): LiveData<List<Account>>

    @Query("SELECT * FROM accounts WHERE profile_id = :profileId")
    suspend fun getAllSync(profileId: Long): List<Account>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account): Long

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)

    // Java 互換性のための @JvmName アノテーション
    @JvmName("getAllBlocking")
    @Query("SELECT * FROM accounts WHERE profile_id = :profileId")
    fun getAllBlocking(profileId: Long): List<Account>
}
```

### TransactionDAO Contract

```kotlin
@Dao
interface TransactionDAO {
    @Query("SELECT * FROM transactions WHERE profile_id = :profileId ORDER BY year DESC, month DESC, day DESC")
    fun getAll(profileId: Long): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Transaction
    @Query("SELECT * FROM transactions WHERE profile_id = :profileId")
    fun getAllWithAccounts(profileId: Long): LiveData<List<TransactionWithAccounts>>
}
```

---

## 2. JSON Parser Contracts

### Gateway Interface Contract

各 API バージョンの Gateway は以下のインターフェースを実装する。

```kotlin
abstract class Gateway {
    abstract val apiVersion: API

    // 必須メソッド
    abstract fun transactionSaveRequest(transaction: LedgerTransaction): String
    abstract fun createAccountListParser(input: InputStream): AccountListParser
    abstract fun createTransactionListParser(input: InputStream): TransactionListParser

    companion object {
        @JvmStatic
        fun forVersion(version: API): Gateway
    }
}
```

### AccountListParser Contract

```kotlin
abstract class AccountListParser {
    abstract fun hasNext(): Boolean
    abstract fun getNext(): ParsedLedgerAccount?
}
```

### TransactionListParser Contract

```kotlin
abstract class TransactionListParser {
    abstract fun hasNext(): Boolean
    abstract fun getNext(): ParsedLedgerTransaction?
}
```

---

## 3. Model Conversion Contracts

ドメインモデルと DB エンティティ間の変換契約。

### LedgerAccount Conversion

```kotlin
interface AccountConverter {
    fun fromDBO(dbo: AccountWithAmounts): LedgerAccount
    fun toDBO(model: LedgerAccount, profileId: Long): AccountWithAmounts
}

// 実装: LedgerAccount.Companion
data class LedgerAccount(...) {
    companion object : AccountConverter {
        override fun fromDBO(dbo: AccountWithAmounts): LedgerAccount
        override fun toDBO(model: LedgerAccount, profileId: Long): AccountWithAmounts
    }
}
```

### LedgerTransaction Conversion

```kotlin
interface TransactionConverter {
    fun fromDBO(dbo: TransactionWithAccounts): LedgerTransaction
    fun toDBO(model: LedgerTransaction, profileId: Long): TransactionWithAccounts
}
```

---

## 4. Static Method Contracts

Java からの呼び出しを維持するため、`@JvmStatic` で公開する。

### App Class

```kotlin
class App : Application() {
    companion object {
        // 必須 static メソッド
        @JvmStatic fun prepareMonthNames()
        @JvmStatic fun setAuthenticationDataFromProfileModel(model: ProfileDetailModel)
        @JvmStatic fun isRecoverable(t: Throwable): Boolean

        // 必須 static フィールド
        @JvmField val PREF_NAME: String = "MoLe"
        @JvmField val PREF_THEME_HUE: String = "theme-hue"

        // Compile-time constants
        const val DB_VERSION: Int = 68
    }
}
```

### Globals Class

```kotlin
object Globals {
    @JvmStatic fun formatIsoDate(date: SimpleDate): String
    @JvmStatic fun parseIsoDate(dateString: String): SimpleDate

    @JvmField val DEFAULT_LOCALE: Locale = Locale.US
}
```

---

## 5. Callback Contracts

非同期処理のコールバックインターフェース。移行後はコルーチンに置換されるが、移行中は維持。

### TaskCallback

```kotlin
interface TaskCallback<T> {
    fun onDone(result: T)
    fun onError(error: Exception)
}

// Java 互換の SAM 変換対応
fun interface TaskCallback<T> {
    fun onDone(result: T)

    // デフォルト実装
    fun onError(error: Exception) {
        throw error
    }
}
```

### AsyncResultCallback

```kotlin
interface AsyncResultCallback<T> {
    fun onSuccess(result: T)
    fun onFailure(error: Throwable)
    fun onComplete()
}
```

---

## 6. UI Binding Contracts

データバインディングとの互換性を維持。

### LiveData Exposure

```kotlin
// ViewModel での LiveData 公開パターン
class AccountViewModel : ViewModel() {
    // Java 互換: getter 自動生成
    val accounts: LiveData<List<Account>> = accountDAO.getAll(profileId)

    // 内部 MutableLiveData
    private val _selectedAccount = MutableLiveData<Account?>()
    val selectedAccount: LiveData<Account?> = _selectedAccount
}
```

### Observable Pattern

```kotlin
// ObservableField から StateFlow への移行準備
class ProfileDetailModel {
    // 移行前: Java Observable
    val name: ObservableField<String> = ObservableField("")

    // 移行後: Kotlin StateFlow
    private val _name = MutableStateFlow("")
    val nameFlow: StateFlow<String> = _name.asStateFlow()

    // Java 互換 getter
    @JvmName("getName")
    fun getName(): String = _name.value
}
```

---

## 7. Exception Contracts

例外クラスの継承関係を維持。

```kotlin
// 基底例外クラス
open class MoLeException(message: String, cause: Throwable? = null)
    : Exception(message, cause)

// 具体的な例外
class NetworkException(message: String, cause: Throwable? = null)
    : MoLeException(message, cause)

class ParseException(message: String, cause: Throwable? = null)
    : MoLeException(message, cause)

class AuthenticationException(message: String)
    : MoLeException(message)
```

---

## 8. Annotation Processor Contracts

Room と KSP の互換性を維持。

### Entity Annotations

```kotlin
// 必須アノテーション維持
@Entity(
    tableName = "...",
    foreignKeys = [...],
    indices = [...]
)
data class EntityName(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "column_name") val fieldName: Type,
    @Ignore val ignoredField: Type
)
```

### DAO Annotations

```kotlin
@Dao
interface DaoName {
    @Query("...")
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @Update
    @Delete
    @Transaction
}
```

---

## 9. Interop Annotations Summary

移行中に必要な Java 互換性アノテーション:

| Annotation | Purpose | Usage |
|------------|---------|-------|
| `@JvmStatic` | static メソッド公開 | companion object メソッド |
| `@JvmField` | static フィールド公開 | companion object プロパティ |
| `@JvmName` | メソッド名変更 | プロパティ getter 名変更 |
| `@JvmOverloads` | オーバーロード生成 | デフォルト引数付き関数 |
| `@Throws` | 例外宣言 | Java からの呼び出し対応 |

---

## 10. Testing Contracts

テストの互換性を維持。

### JUnit 4 Pattern

```kotlin
class AccountTest {
    @Before
    fun setup() { }

    @After
    fun teardown() { }

    @Test
    fun testAccountCreation() { }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidInput() { }
}
```

### Instrumented Test Pattern

```kotlin
@RunWith(AndroidJUnit4::class)
class AccountInstrumentedTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testAccountDisplay() { }
}
```

---

## Contract Verification

各 Phase の移行完了時に以下を検証:

1. **コンパイル**: Java と Kotlin の相互呼び出しが成功
2. **テスト**: 既存の全テストがパス
3. **バイナリ互換**: APK のメソッドシグネチャが維持
4. **ランタイム**: 実機でのクラッシュなし

### 検証コマンド

```bash
# ビルド確認
./gradlew assembleDebug

# テスト実行
./gradlew test
./gradlew connectedAndroidTest

# バイナリ互換チェック (optional)
./gradlew apiCheck
```
