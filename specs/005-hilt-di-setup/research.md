# リサーチ結果: Hilt 依存性注入セットアップ

**作成日**: 2026-01-06
**ブランチ**: `005-hilt-di-setup`

## 1. Hilt + Kotlin 2.0 + KSP 互換性

### 決定事項

**Hiltバージョン**: 2.51.1（Kotlin 2.0.xとの互換性確認済み）
**KSPバージョン**: 既存の2.0.21-1.0.26を維持

### 根拠

- [Dagger KSP公式ドキュメント](https://dagger.dev/dev-guide/ksp.html)によると、Hilt 2.48以降でKSPをサポート
- [GitHub Issue #4582](https://github.com/google/dagger/issues/4582)によると、Kotlin 2.1.0では互換性問題があるが、2.0.xでは安定
- Hilt 2.51.1はKSP 2.0.0-1.0.23以降で動作確認されており、プロジェクトのKSP 2.0.21-1.0.26と互換

### 検討した代替案

| 代替案 | 却下理由 |
|--------|----------|
| Hilt 2.56.2（最新） | Kotlin 2.1.0向け、2.0.xとの互換性リスク |
| KAPT使用 | KSPより遅い、プロジェクトは既にKSP移行済み |
| Koin使用 | 実行時DI、コンパイル時検証なし |

### 参考資料

- [Setup of Hilt with KSP in an Android Project 2025](https://medium.com/@mohit2656422/setup-of-hilt-with-ksp-in-an-android-project-2025-e76e42bb261a)
- [AndroidX Hilt Releases](https://developer.android.com/jetpack/androidx/releases/hilt)

---

## 2. Hilt + Room 統合パターン

### 決定事項

**アプローチ**: DatabaseModuleで既存DB.get()シングルトンをラップして提供

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(): DB = DB.get()

    @Provides
    fun provideProfileDAO(db: DB): ProfileDAO = db.getProfileDAO()

    @Provides
    fun provideTransactionDAO(db: DB): TransactionDAO = db.getTransactionDAO()
    // 他のDAO...
}
```

### 根拠

- [Using Room DB with Hilt](https://svvashishtha.medium.com/using-room-with-hilt-cb57a1bc32f)のベストプラクティスに従う
- 既存のDB.get()シングルトンパターンを維持することで移行リスクを最小化
- DAOはインターフェースのため、`@Provides`メソッドで提供が必要
- `@Singleton`でDBインスタンスの単一性を保証

### 検討した代替案

| 代替案 | 却下理由 |
|--------|----------|
| DB.get()を完全排除 | 大規模リファクタリング、移行リスク高 |
| DAOを直接@Inject | DAOはインターフェース、コンストラクタインジェクション不可 |

### 参考資料

- [Hilt and Room Database In Jetpack Compose](https://medium.com/@andyphiri92/hilt-and-room-database-in-jetpack-compose-72852167338b)
- [Dependency injection with Hilt | Android Developers](https://developer.android.com/training/dependency-injection/hilt-android)

---

## 3. Hilt + ViewModel テスト

### 決定事項

**ユニットテスト戦略**: Hiltを使用せず、ViewModelを直接コンストラクタインスタンス化

```kotlin
class MainModelTest {
    private lateinit var mockProfileDAO: ProfileDAO
    private lateinit var mockTransactionDAO: TransactionDAO
    private lateinit var viewModel: MainModel

    @Before
    fun setUp() {
        mockProfileDAO = mockk()
        mockTransactionDAO = mockk()
        viewModel = MainModel(mockProfileDAO, mockTransactionDAO)
    }

    @Test
    fun testSomething() {
        // モックを使用したテスト
    }
}
```

**インストルメンテーションテスト戦略**: Hilt Testing APIを使用

```kotlin
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @BindValue
    @JvmField
    val testDatabase: DB = Room.inMemoryDatabaseBuilder(...)
}
```

### 根拠

- [Hilt testing guide](https://developer.android.com/training/dependency-injection/hilt-testing)の公式推奨:
  > 「Hilt isn't necessary for unit tests, since when testing a class that uses constructor injection, you don't need to use Hilt to instantiate that class.」
- ユニットテストはJVM上で高速実行可能
- インストルメンテーションテストでは`@BindValue`で依存関係を置換

### 検討した代替案

| 代替案 | 却下理由 |
|--------|----------|
| ユニットテストでもHilt使用 | 不必要な複雑性、テスト速度低下 |
| Robolectricのみ | インストルメンテーションテストの価値を失う |

### 参考資料

- [A Complete Guide to MVVM and ViewModel Testing in Android](https://medium.com/@deepak.patidark93/a-complete-guide-to-mvvm-and-viewmodel-testing-in-android-hilt-junit-and-mockito-explained-df54324b8dca)
- [Testing - Hilt (Dagger公式)](https://dagger.dev/hilt/testing.html)

---

## 4. 既存シングルトンラップ戦略

### 決定事項

**アプローチ**: DB/Dataシングルトンを維持し、モジュールから参照

```kotlin
// DatabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(): DB = DB.get()
}

// DataModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideData(): Data = Data  // Kotlinオブジェクト
}
```

### 根拠

- 既存コードの動作を維持
- 段階的移行を可能にする
- テスト時は`@TestInstallIn`でモジュールを置換可能

### 検討した代替案

| 代替案 | 却下理由 |
|--------|----------|
| シングルトン完全排除 | 大規模リファクタリング必要、リスク高 |
| Dataの状態をすべて注入 | 複雑すぎる、段階的移行の原則に反する |

---

## 5. 必要な依存関係

### Gradle設定

```kotlin
// gradle/libs.versions.toml に追加
[versions]
hilt = "2.51.1"
hiltAndroidX = "1.2.0"

[libraries]
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
hilt-android-testing = { module = "com.google.dagger:hilt-android-testing", version.ref = "hilt" }
androidx-hilt-compiler = { module = "androidx.hilt:hilt-compiler", version.ref = "hiltAndroidX" }

[plugins]
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

```kotlin
// app/build.gradle に追加
plugins {
    alias(libs.plugins.hilt)
}

dependencies {
    implementation libs.hilt.android
    ksp libs.hilt.compiler

    testImplementation libs.hilt.android.testing
    kspTest libs.hilt.compiler

    androidTestImplementation libs.hilt.android.testing
    kspAndroidTest libs.hilt.compiler
}
```

---

## 6. 解決済み不明点

| 項目 | 解決内容 |
|------|----------|
| Hilt + Kotlin 2.0 + KSP互換性 | Hilt 2.51.1で互換、KSP 2.0.21-1.0.26維持 |
| Hilt + Room統合パターン | DatabaseModuleでDB.get()をラップして提供 |
| Hilt + ViewModelテスト | ユニットテスト: 直接インスタンス化、インストルメンテーション: @BindValue |
| 既存シングルトンラップ戦略 | モジュールから既存シングルトンを参照して提供 |
