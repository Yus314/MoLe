# Research: Java から Kotlin への移行

**Branch**: `001-java-kotlin-migration` | **Date**: 2026-01-05
**Purpose**: Phase 0 - 技術的な不明点の解消と意思決定

## Decision Summary

| 項目 | 決定 | 理由 |
|------|------|------|
| Kotlin バージョン | 1.9.x (最新安定版) | AGP 8.0.2 との互換性。Kotlin 2.x は AGP 8.5+ 必須 |
| アノテーション処理 | KSP 1.9.x | KAPT より 2 倍高速、Kotlin ネイティブ対応 |
| 重複コード抽出 | 委譲パターン + Factory | JSON パーサー 7 バージョンの ~40% コード削減 |
| Null 安全性移行 | 事前アノテーション追加 | Platform 型リスク回避 |
| 非同期処理 | コルーチン (viewModelScope) | ライフサイクル自動管理、メモリリーク防止 |

---

## 1. Kotlin バージョン選定

### 問題
spec では Kotlin 2.1.x を指定しているが、AGP との互換性を確認する必要がある。

### 調査結果

**AGP と Kotlin の互換性マトリクス:**

| Kotlin | 必須 AGP | MoLe の AGP 8.0.2 |
|--------|---------|------------------|
| 1.9.x | 8.0+ | ✅ 互換 |
| 2.0.x | 8.5+ | ❌ 非互換 |
| 2.1.x | 8.6+ | ❌ 非互換 |

**Kotlin 2.x の主要な変更点:**
- K2 コンパイラがデフォルト有効（最大 94% ビルド高速化）
- `compilerOptions` DSL が `kotlinOptions` を置換
- Compose コンパイラプラグインの分離
- Lambda の invokedynamic 生成（バイナリサイズ削減）

### 決定
**Kotlin 1.9.x を使用**

### 根拠
- AGP 8.0.2 との互換性を維持
- AGP アップグレード (8.5+) は別 issue で対応
- 1.9.x でも十分なモダン機能（data class, null 安全, coroutines）が利用可能
- 将来的に AGP + Kotlin 2.x への移行は容易

### 却下した代替案
- **AGP と Kotlin 同時アップグレード**: スコープが大きすぎる。段階的移行の原則に反する
- **Kotlin 2.x 強行**: ビルドエラーで移行が停止するリスク

---

## 2. アノテーション処理: KSP vs KAPT

### 問題
Room のアノテーション処理を KSP に移行すべきか。

### 調査結果

| 観点 | KAPT | KSP |
|------|------|-----|
| ビルド速度 | 基準 | 2 倍高速 |
| Kotlin 理解 | スタブ経由 | ネイティブ |
| Null 安全性 | 部分的 | 完全 |
| Room 対応 | ✅ | ✅ (v2.4.0+) |
| 並行利用 | - | ✅ KAPT と共存可 |

**MoLe 現状:** `annotationProcessor "androidx.room:room-compiler:2.4.2"`

### 決定
**KSP 1.9.x を採用**

### 根拠
- Room 2.4.2 は KSP 完全対応
- ビルド時間短縮（特に 237 ファイル変換後）
- Kotlin の null 安全性をより正確に処理
- KAPT との並行利用で段階的移行可能

### Gradle 設定
```gradle
plugins {
    id 'org.jetbrains.kotlin.android' version '1.9.25'
    id 'com.google.devtools.ksp' version '1.9.25-1.0.20'
}

dependencies {
    ksp "androidx.room:room-compiler:2.4.2"
}
```

---

## 3. JSON パーサー重複コード抽出戦略

### 問題
7 バージョン (v1_14〜v1_50) の JSON パーサーに大量の重複コードが存在。効率的な抽出方法を決定する。

### 調査結果

**重複分析:**

| クラス | 総行数 | 重複行数 | 重複率 |
|--------|--------|----------|--------|
| ParsedPosting | 1,050 | 800+ | 76% |
| ParsedLedgerTransaction | 1,200+ | 1,000+ | 83% |
| ParsedLedgerAccount | 420 | 200+ | 48% |
| Gateway | 280 | 210+ | 75% |

**バージョン間の主要差異:**

1. **ptransaction_ 型の違い**
   - v1_14〜v1_23: `int`
   - v1_32〜v1_50: `String`

2. **ParsedStyle 設定の違い**
   - v1_14: `setAsdecimalpoint('.')`
   - v1_19_1: `setAsprecision(new ParsedPrecision(2))`
   - v1_32+: `setAsdecimalmark(".")` + `setAsrounding("NoRounding")`

3. **ParsedLedgerAccount 構造の違い**
   - v1_14〜v1_40: `getAibalance()` でバランス取得
   - v1_50: `adata.getFirstPeriodBalance().getBdincludingsubs()` で取得

### 決定
**委譲パターン + Factory パターンの組み合わせ**

### 根拠
- Template Method だと継承階層が深くなりすぎる
- Strategy パターンは粒度が細かすぎる
- 委譲パターンは Kotlin の `by` キーワードと相性が良い

### 実装アプローチ

**Phase 1: 共通フィールドコンテナ**
```kotlin
// 共通フィールドを委譲先クラスに集約
class PostingFields {
    var paccount: String = ""
    var pcomment: String = ""
    var pamount: List<ParsedAmount> = emptyList()
    // ... getter/setter 100+ 行を集約
}

class ParsedPosting(
    private val fields: PostingFields = PostingFields()
) : ParsedPostingBase by fields {
    // バージョン固有: ptransaction_ の型
}
```

**Phase 2: StyleConfigurer インターフェース**
```kotlin
interface StyleConfigurer {
    fun configureStyle(style: ParsedStyle)
}

// v1_14 用
object V1_14StyleConfigurer : StyleConfigurer {
    override fun configureStyle(style: ParsedStyle) {
        style.asprecision = 2
        style.asdecimalpoint = '.'
    }
}

// v1_32+ 用
object V1_32StyleConfigurer : StyleConfigurer {
    override fun configureStyle(style: ParsedStyle) {
        style.asprecision = 2
        style.asdecimalmark = "."
        style.asrounding = "NoRounding"
    }
}
```

**Phase 3: Factory パターン**
```kotlin
sealed interface ApiVersion {
    fun createPosting(): ParsedPosting
    fun createAccount(): ParsedLedgerAccount
    fun createTransaction(): ParsedLedgerTransaction
}

object V1_14Api : ApiVersion { /* ... */ }
object V1_50Api : ApiVersion { /* ... */ }
```

### 期待される削減効果
- **現状**: ~3,000 行（4 クラス × 7 バージョン）
- **削減後**: ~1,800 行（40% 削減）

---

## 4. Null 安全性移行戦略

### 問題
Java コードの nullability アノテーションが不完全（388 箇所のみ）。Platform 型によるランタイムエラーのリスク。

### 調査結果

**現状:**
- `@NonNull`: 約 200 箇所
- `@Nullable`: 約 188 箇所
- **未アノテーション**: 推定 1,000+ 箇所

**Platform 型のリスク:**
Java から来る値で nullability アノテーションがない場合、Kotlin は「Platform 型 (T!)」として扱う。これは null チェックなしでアクセスでき、NPE の原因になる。

### 決定
**事前アノテーション追加 + 段階的変換**

### 根拠
- spec の要件（FR-003: null 安全機能の適切な活用）
- 変換後のランタイムエラー防止
- コードレビュー負担の軽減

### 実装アプローチ

**Step 1: Infer Nullity の実行**
```bash
# Android Studio で
Analyze > Infer Nullity > Whole Project
```

**Step 2: パッケージごとに確認**
優先順位の高い順:
1. `model/` - ドメインモデル（公開 API）
2. `db/` - Room エンティティ（永続化境界）
3. `dao/` - DAO インターフェース（クエリ結果）
4. `json/` - JSON パーサー（外部データ）

**Step 3: 変換時のパターン**
```kotlin
// Java: @Nullable String getName()
// Kotlin: val name: String?

// Java: @NonNull String getName()
// Kotlin: val name: String

// Java: String getName() (未アノテーション)
// → 先にアノテーション追加してから変換
```

**Step 4: 厳格モードの有効化**
```gradle
kotlinOptions {
    freeCompilerArgs += ["-Xjsr305=strict"]
}
```

---

## 5. Room エンティティの data class 変換

### 問題
16 の Room エンティティを data class に変換する際のベストプラクティス。

### 調査結果

**現状のパターン (Account.java):**
```java
@Entity(tableName = "accounts")
public class Account {
    @PrimaryKey(autoGenerate = true)
    long id;
    @ColumnInfo(name = "profile_id")
    long profileId;
    @NonNull
    private String name;
    // ... 8+ フィールド
    // ... 20+ getter/setter
}
```

**Kotlin data class 変換:**
```kotlin
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "profile_id") val profileId: Long,
    val name: String,
    // ... 8+ フィールド（getter/setter 自動生成）
)
```

### 決定
**Primary constructor パラメータとして定義**

### 根拠
- Room は data class の primary constructor を認識
- 行数 70% 削減（getter/setter 不要）
- `equals()`, `hashCode()`, `copy()` 自動生成
- 不変性 (immutability) の促進

### 注意事項
1. **ForeignKey 制約**: `@Entity` アノテーション内で維持
2. **TypeConverter**: 変更不要（Kotlin でも同様に動作）
3. **スキーマ**: 変更なし（バイナリ互換）
4. **@NonNull の扱い**: Kotlin の非 null 型に置換

---

## 6. 非同期処理: コルーチン移行

### 問題
`GeneralBackgroundTasks` と `Executor` ベースの非同期処理をコルーチンに移行すべきか。

### 調査結果

**現状のパターン:**
```java
GeneralBackgroundTasks.run(
    () -> { /* background work */ },
    result -> { /* onSuccess */ },
    error -> { /* onError */ },
    () -> { /* onDone */ }
);
```

**コルーチン移行後:**
```kotlin
viewModelScope.launch {
    try {
        val result = withContext(Dispatchers.IO) {
            // background work
        }
        // onSuccess (Main thread)
    } catch (e: Exception) {
        // onError
    }
}
```

### 決定
**Phase 6 でコルーチン化（async パッケージ移行時）**

### 根拠
- 憲章 VI: 「非同期操作にはKotlinコルーチンを使用する」
- ライフサイクル自動管理（メモリリーク防止）
- 構造化並行性（Structured Concurrency）
- 既存の `lifecycle-viewmodel-ktx` 依存関係で利用可能

### 段階的移行
1. **Phase 6**: `async/` パッケージをコルーチン化
2. **Phase 7**: UI 層から `GeneralBackgroundTasks` 呼び出しを置換
3. **最終**: `GeneralBackgroundTasks` クラスを削除

---

## 7. Jackson + Kotlin の統合

### 問題
Jackson JSON パーサーを Kotlin で使用する際の注意点。

### 調査結果

**現状:**
```java
ObjectMapper mapper = new ObjectMapper();
mapper.readValue(input, ParsedLedgerAccount.class);
```

**Kotlin 最適化:**
```kotlin
// KotlinModule 登録（必須）
val mapper = ObjectMapper()
    .registerModule(KotlinModule.Builder().build())
    .registerModule(JavaTimeModule())

// Singleton として再利用
object JsonMapper {
    val instance: ObjectMapper = ObjectMapper()
        .registerModule(KotlinModule.Builder()
            .withReflectionCacheSize(512)
            .build())
}
```

### 決定
**KotlinModule を登録した ObjectMapper Singleton を使用**

### 根拠
- KotlinModule なしでは primary constructor が認識されない
- Singleton 化でパフォーマンス改善（毎回生成のオーバーヘッド排除）
- `@JsonIgnoreProperties(ignoreUnknown = true)` は維持

### 注意事項
- Null 安全性: JSON の `null` と Kotlin の非 null 型の不一致に注意
- Default 値: data class のデフォルト値は Jackson が尊重

---

## 8. 静的メソッドと Companion Object

### 問題
Java の `static` メソッドを Kotlin でどう扱うか。

### 調査結果

**現状 (App.java):**
```java
public class App extends Application {
    public static void prepareMonthNames() { }
    public static final String PREF_NAME = "MoLe";
}
```

**Kotlin 変換:**
```kotlin
class App : Application() {
    companion object {
        @JvmStatic
        fun prepareMonthNames() { }

        @JvmField
        val PREF_NAME = "MoLe"

        const val DB_VERSION = 68  // compile-time constant
    }
}
```

### 決定
**@JvmStatic / @JvmField で Java 互換性を維持**

### 根拠
- 段階的移行中、Java コードから呼び出される可能性
- Android フレームワークからの呼び出し対応
- 移行完了後に `@JvmStatic` を削除可能

---

## 9. ソースディレクトリ構成

### 問題
`src/main/java` と `src/main/kotlin` の使い分け。

### 調査結果

**オプション A: 別ディレクトリ**
```
src/main/java/    # 移行前 Java
src/main/kotlin/  # 移行後 Kotlin
```

**オプション B: 同一ディレクトリ**
```
src/main/java/    # Java + Kotlin 混在
```

### 決定
**オプション A: 別ディレクトリ**

### 根拠
- 移行進捗の可視化が容易
- IDE の言語別カラーリングと併用
- 移行完了後に `src/main/java` を削除可能
- Gradle 設定で両方を sourceSets に含める

### Gradle 設定
```gradle
android {
    sourceSets {
        main {
            java.srcDirs += 'src/main/kotlin'
        }
    }
}
```

---

## 10. テストコード移行

### 問題
6 unit + 1 instrumented テストの移行タイミング。

### 調査結果

**現状:**
- `DigestUnitTest.java`
- `LegacyParserTest.java`
- `ParsedQuantityTest.java`
- `LedgerAccountTest.java`
- `AmountStyleTest.java`
- `SimpleDateTest.java`
- `ExampleInstrumentedTest.java`

### 決定
**各パッケージ移行時に対応するテストも同時移行**

### 根拠
- テスト対象クラスと同じタイミングで変換
- Kotlin のテスト記述の簡潔さを活用
- 既存テストケースは維持（カバレッジ保証）

### テストフレームワーク
JUnit 4 を維持（JUnit 5 移行は別 issue）

---

## References

1. [Kotlin Best Practices for Room Database in Android Projects](https://www.slingacademy.com/article/kotlin-best-practices-for-room-database-in-android-projects/)
2. [Migrate from kapt to KSP - Android Developers](https://developer.android.com/build/migrate-to-ksp)
3. [Jackson Support for Kotlin - Baeldung](https://www.baeldung.com/kotlin/jackson-kotlin)
4. [Easy Coroutines in Android: viewModelScope - Android Developers](https://medium.com/androiddevelopers/easy-coroutines-in-android-viewmodelscope-25bffb605471)
5. [Nullability in Java and Kotlin - Kotlin Documentation](https://kotlinlang.org/docs/java-to-kotlin-nullability-guide.html)
6. [AGP, D8, and R8 Versions for Kotlin - Android Developers](https://developer.android.com/build/kotlin-support)
7. [Compatibility Guide for Kotlin 2.0.x - Kotlin Documentation](https://kotlinlang.org/docs/compatibility-guide-20.html)
