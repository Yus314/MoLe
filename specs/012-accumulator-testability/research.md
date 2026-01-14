# Research: TransactionAccumulator テスト可能性向上

**Date**: 2026-01-14 | **Branch**: `012-accumulator-testability`

## 1. TransactionAccumulator の現状分析

### 1.1 クラス構造

**ファイル**: `app/src/main/kotlin/net/ktnx/mobileledger/async/TransactionAccumulator.kt`

```kotlin
class TransactionAccumulator(
    private val boldAccountName: String?,
    private val accumulateAccount: String?
)
```

**主要フィールド** (lines 30-35):
- `list: ArrayList<TransactionListItem>` - 累積された取引アイテム
- `runningTotal: HashMap<String, BigDecimal>` - 通貨ごとの累計
- `earliestDate`, `latestDate`, `lastDate: SimpleDate` - 日付追跡
- `transactionCount: Int` - 取引カウンター

**問題箇所** (line 85):
```kotlin
val currencyFormatter = App.currencyFormatter()  // 静的グローバルアクセス
```

### 1.2 使用箇所

| ファイル | 行番号 | コンテキスト |
|---------|-------|-------------|
| MainViewModel.kt | 631 | プロファイル取引読み込み |
| MainViewModel.kt | 797 | アカウントフィルタリング |
| TransactionListViewModel.kt | 143-146 | コメントで依存問題を指摘（現在は使用を回避） |

### 1.3 依存関係チェーン

```
TransactionAccumulator
    └── App.currencyFormatter()
        └── App.instance (Application シングルトン)
            └── Android Application コンテキスト（テストで利用不可）
```

## 2. 既存インフラストラクチャの調査

### 2.1 CurrencyFormatter インターフェース

**ファイル**: `app/src/main/kotlin/net/ktnx/mobileledger/service/CurrencyFormatter.kt`

**使用されるメソッド** (line 101):
```kotlin
fun formatNumber(number: Float): String
```

**その他のメソッド**（TransactionAccumulator では未使用）:
- `formatCurrency(amount: Float, currencySymbol: String?): String`
- `parseNumber(str: String): Float`
- `getDecimalSeparator(): String`
- `getGroupingSeparator(): String`

### 2.2 本番実装

**ファイル**: `app/src/main/kotlin/net/ktnx/mobileledger/service/CurrencyFormatterImpl.kt`

```kotlin
@Singleton
class CurrencyFormatterImpl @Inject constructor() : CurrencyFormatter
```

- Hilt で `@Singleton` スコープとして提供済み
- `ServiceModule.kt` で `@Binds` でバインディング済み

### 2.3 テスト実装

**ファイル**: `app/src/test/kotlin/net/ktnx/mobileledger/fake/FakeCurrencyFormatter.kt`

```kotlin
class FakeCurrencyFormatter : CurrencyFormatter
```

**特徴**:
- `Locale.US` を固定ロケールとして使用
- `DecimalFormat("#,##0.00")` で予測可能なフォーマット
- `setLocale()`, `setCurrencySymbolPosition()`, `setCurrencyGap()` でテスト設定可能

### 2.4 Hilt DI 設定

**ファイル**: `app/src/main/kotlin/net/ktnx/mobileledger/di/ServiceModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {
    @Binds
    @Singleton
    abstract fun bindCurrencyFormatter(impl: CurrencyFormatterImpl): CurrencyFormatter
}
```

**結論**: CurrencyFormatter は既に Hilt で提供されており、ViewModel へのインジェクションが可能

## 3. 設計決定

### 3.1 コンストラクタインジェクション vs ファクトリ

| アプローチ | メリット | デメリット |
|-----------|---------|-----------|
| **コンストラクタインジェクション** | シンプル、明示的な依存関係 | 呼び出し元がフォーマッターを保持する必要 |
| ファクトリパターン | 遅延初期化可能 | 複雑性増加、Hilt との統合が不要 |

**決定**: コンストラクタインジェクション
**理由**:
1. 依存関係が明示的
2. テストでの差し替えが容易
3. Hilt パターンと一貫性あり
4. ファクトリの複雑性は不要

### 3.2 App.currencyFormatter() の扱い

| アプローチ | メリット | デメリット |
|-----------|---------|-----------|
| **静的メソッドを残す** | 後方互換性維持 | 古いパターンが残る |
| 静的メソッドを削除 | クリーンなコード | 他の呼び出し箇所の調査が必要 |

**決定**: 静的メソッドを残す
**理由**:
1. 本機能のスコープ外での使用箇所がある可能性
2. 段階的移行を許容
3. 本機能の目標は TransactionAccumulator のテスト可能性のみ

### 3.3 テストファイルの配置

| アプローチ | パス |
|-----------|------|
| **async パッケージにテストを配置** | `app/src/test/.../async/TransactionAccumulatorTest.kt` |
| 専用テストパッケージ | `app/src/test/.../accumulator/TransactionAccumulatorTest.kt` |

**決定**: async パッケージ
**理由**: ソースコードの構造をミラーリングする既存のプロジェクトパターンに従う

## 4. 代替案の評価

### 4.1 アプローチ A: 直接コンストラクタインジェクション（採用）

```kotlin
class TransactionAccumulator(
    private val boldAccountName: String?,
    private val accumulateAccount: String?,
    private val currencyFormatter: CurrencyFormatter  // 新規パラメータ
)
```

**評価**:
- ✅ シンプル
- ✅ テスト可能
- ✅ 既存パターンと一貫
- ⚠️ 呼び出し元の更新が必要

### 4.2 アプローチ B: デフォルトパラメータ付きコンストラクタ（不採用）

```kotlin
class TransactionAccumulator(
    private val boldAccountName: String?,
    private val accumulateAccount: String?,
    private val currencyFormatter: CurrencyFormatter = App.currencyFormatter()
)
```

**評価**:
- ✅ 後方互換性が高い
- ❌ App への依存が残る
- ❌ テスト時に Application コンテキストが必要になる可能性

**不採用理由**: デフォルト値が App.currencyFormatter() を参照するため、クラスロード時に Application が必要

### 4.3 アプローチ C: インターフェース抽出（不採用）

新しい `CurrencyFormattingCapable` インターフェースを作成し、TransactionAccumulator に実装させる。

**評価**:
- ❌ 過剰な設計
- ❌ CurrencyFormatter インターフェースで十分
- ❌ 新しい抽象化が必要

**不採用理由**: 既存の CurrencyFormatter インターフェースで目的は達成可能

## 5. テスト戦略

### 5.1 テストケース

| カテゴリ | テストケース | 優先度 |
|---------|-------------|-------|
| 基本 | 単一取引の累計計算 | P1 |
| 基本 | 複数取引の累計計算 | P1 |
| 通貨 | 単一通貨の累計フォーマット | P1 |
| 通貨 | 複数通貨の累計フォーマット | P1 |
| エッジ | 金額ゼロの取引 | P2 |
| エッジ | 負の金額の取引 | P2 |
| フィルタ | アカウントフィルタリング | P2 |

### 5.2 Fake 使用パターン

```kotlin
@Test
fun `summarizeRunningTotal formats using injected formatter`() {
    val fakeFormatter = FakeCurrencyFormatter()
    val accumulator = TransactionAccumulator(null, null, fakeFormatter)

    val transaction = createTestTransaction(amount = 1234.56f)
    accumulator.put(transaction)

    val items = accumulator.getItems()
    // FakeCurrencyFormatter は "1,234.56" 形式を返す
    assertThat(items[0].runningTotal).contains("1,234.56")
}
```

## 6. リスク評価

| リスク | 影響 | 確率 | 軽減策 |
|-------|------|------|-------|
| フォーマット出力の差異 | 高 | 低 | 実機テストで検証（SC-005） |
| 他の App.currencyFormatter() 呼び出し | 低 | 低 | スコープ外、静的メソッドを残す |
| ViewModel のテスト失敗 | 中 | 低 | 既存テストを実行して確認 |

## 7. 結論

**決定事項**:
1. TransactionAccumulator のコンストラクタに `currencyFormatter: CurrencyFormatter` パラメータを追加
2. `summarizeRunningTotal()` で注入されたフォーマッターを使用
3. MainViewModel の両方のインスタンス化箇所を更新
4. FakeCurrencyFormatter を使用したユニットテストを作成
5. App.currencyFormatter() 静的メソッドは後方互換性のため残す

**NEEDS CLARIFICATION の解決**: すべて解決済み。Phase 1 に進行可能。
