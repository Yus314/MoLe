# Data Model: TransactionAccumulator テスト可能性向上

**Date**: 2026-01-14 | **Branch**: `012-accumulator-testability`

## 概要

本機能はデータモデルの変更を伴わない純粋なリファクタリング。TransactionAccumulator クラスのコンストラクタシグネチャのみを変更し、内部ロジックと出力は維持する。

## エンティティ（変更なし）

### TransactionAccumulator

内部で使用するデータ構造は変更なし:

```kotlin
// 累計を通貨ごとに管理
private val runningTotal: HashMap<String, BigDecimal>

// 累積された取引アイテムのリスト
private val list: ArrayList<TransactionListItem>

// 日付追跡
private var earliestDate: SimpleDate
private var latestDate: SimpleDate
private var lastDate: SimpleDate

// 取引カウント
private var transactionCount: Int
```

### CurrencyFormatter（変更なし）

既存のインターフェースをそのまま使用:

```kotlin
interface CurrencyFormatter {
    fun formatNumber(number: Float): String
    // 他のメソッドは TransactionAccumulator では未使用
}
```

### TransactionListItem（変更なし）

累計結果を保持する既存のデータクラス:

```kotlin
// 既存の TransactionListItem に runningTotal フィールドが含まれる
data class TransactionListItem(
    // ... 既存フィールド
    val runningTotal: String,  // フォーマット済み累計
    // ...
)
```

## シグネチャ変更

### Before

```kotlin
class TransactionAccumulator(
    private val boldAccountName: String?,
    private val accumulateAccount: String?
)
```

### After

```kotlin
class TransactionAccumulator(
    private val boldAccountName: String?,
    private val accumulateAccount: String?,
    private val currencyFormatter: CurrencyFormatter
)
```

## 依存関係図

### Before

```
┌─────────────────────────────┐
│   TransactionAccumulator    │
└──────────────┬──────────────┘
               │ App.currencyFormatter()
               ▼
┌─────────────────────────────┐
│         App.instance        │
│   (Application Singleton)   │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│  CurrencyFormatterImpl      │
└─────────────────────────────┘
```

### After

```
┌─────────────────────────────┐
│      MainViewModel          │
│  @Inject currencyFormatter  │
└──────────────┬──────────────┘
               │ コンストラクタ引数
               ▼
┌─────────────────────────────┐
│   TransactionAccumulator    │
│  (currencyFormatter)        │
└──────────────┬──────────────┘
               │ 直接使用
               ▼
┌─────────────────────────────┐
│     CurrencyFormatter       │
│      (インターフェース)      │
└─────────────────────────────┘
         ▲           ▲
    本番  │           │ テスト
         │           │
┌────────┴───┐  ┌────┴────────────┐
│Formatter   │  │FakeCurrency     │
│Impl        │  │Formatter        │
└────────────┘  └─────────────────┘
```

## バリデーション

| ルール | 適用箇所 | 実施方法 |
|-------|---------|---------|
| currencyFormatter は非 null | コンストラクタ | Kotlin の型システム（non-null 型） |
| formatNumber の出力形式 | 本番/テスト | 各実装の責務 |

## 状態遷移

TransactionAccumulator は状態を持つが、本機能では変更なし:

```
初期状態 → [put()] → 累積中 → [getItems()] → 完了
```

## データベース変更

なし。本機能は Room データベースに影響しない。

## マイグレーション

なし。コード変更のみ。
