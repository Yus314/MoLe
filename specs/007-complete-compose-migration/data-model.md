# Data Model: Complete Compose Migration

**Feature Branch**: `007-complete-compose-migration`
**Date**: 2026-01-10

## 概要

この移行はUI層のみを対象とし、データモデル（Room Entity）の変更は行わない（FR-007準拠）。
本ドキュメントでは、Compose UIで使用するUiStateとイベントモデルを定義する。

---

## 既存エンティティ（変更なし）

以下のRoom Entityは006-compose-ui-rebuildで使用されており、本移行でも変更なし:

| Entity | 用途 | 関連画面 |
|--------|------|---------|
| `Profile` | サーバー接続設定 | ProfileList, NavigationDrawer |
| `Currency` | 通貨定義 | CurrencyPickerDialog |
| `db.Currency` | 通貨DBエンティティ | CurrencyDAO |

---

## 新規UiState定義

### 1. SplashUiState

```kotlin
/**
 * スプラッシュ画面の状態
 */
data class SplashUiState(
    /** DB初期化完了フラグ */
    val isInitialized: Boolean = false,
    /** 最小表示時間経過フラグ */
    val minDisplayTimeElapsed: Boolean = false
) {
    /** メイン画面への遷移可能判定 */
    val canNavigate: Boolean get() = isInitialized && minDisplayTimeElapsed
}
```

### 2. BackupsUiState

```kotlin
/**
 * バックアップ画面の状態
 */
data class BackupsUiState(
    /** バックアップ処理中フラグ */
    val isBackingUp: Boolean = false,
    /** リストア処理中フラグ */
    val isRestoring: Boolean = false,
    /** バックアップボタン有効フラグ（プロファイル選択時のみ有効） */
    val backupEnabled: Boolean = false,
    /** ユーザーへのメッセージ（Snackbar表示用） */
    val message: BackupsMessage? = null
)

/**
 * バックアップ画面のメッセージ種別
 */
sealed class BackupsMessage {
    data class Success(val messageResId: Int) : BackupsMessage()
    data class Error(val message: String) : BackupsMessage()
}
```

### 3. CurrencyPickerUiState

```kotlin
/**
 * 通貨選択ダイアログの状態
 */
data class CurrencyPickerUiState(
    /** 利用可能な通貨リスト */
    val currencies: List<String> = emptyList(),
    /** 新規通貨追加モードフラグ */
    val isAddingNew: Boolean = false,
    /** 新規通貨名入力値 */
    val newCurrencyName: String = "",
    /** 通貨位置（金額の前/後） */
    val currencyPosition: CurrencyPosition = CurrencyPosition.BEFORE,
    /** 通貨と金額の間にスペースを入れるか */
    val currencyGap: Boolean = true
)

/**
 * 通貨位置
 */
enum class CurrencyPosition {
    BEFORE,  // $100
    AFTER    // 100$
}
```

### 4. DatePickerUiState

```kotlin
/**
 * 日付選択ダイアログの状態
 * Note: Material3 DatePickerはrememberDatePickerState()で管理するため、
 *       この状態クラスは主に制約条件の保持に使用
 */
data class DatePickerUiState(
    /** 初期選択日付（ミリ秒） */
    val initialDateMillis: Long? = null,
    /** 選択可能な最小日付（ミリ秒） */
    val minDateMillis: Long? = null,
    /** 選択可能な最大日付（ミリ秒） */
    val maxDateMillis: Long? = null,
    /** 将来日付の制限設定 */
    val futureDates: FutureDates = FutureDates.All
)
```

### 5. ProfileListUiState

```kotlin
/**
 * プロファイルリストの状態（NavigationDrawer内）
 */
data class ProfileListUiState(
    /** プロファイルリスト */
    val profiles: List<Profile> = emptyList(),
    /** 選択中のプロファイルID */
    val selectedProfileId: Long? = null,
    /** 編集モードフラグ */
    val isEditing: Boolean = false
)
```

### 6. CrashReportUiState

```kotlin
/**
 * クラッシュレポートダイアログの状態
 */
data class CrashReportUiState(
    /** クラッシュレポートテキスト */
    val crashReportText: String = "",
    /** レポート詳細表示フラグ */
    val isReportVisible: Boolean = false,
    /** 送信処理中フラグ */
    val isSending: Boolean = false
)
```

---

## イベント定義

### BackupsEvent

```kotlin
sealed class BackupsEvent {
    object BackupClicked : BackupsEvent()
    object RestoreClicked : BackupsEvent()
    data class BackupUriSelected(val uri: Uri) : BackupsEvent()
    data class RestoreUriSelected(val uri: Uri) : BackupsEvent()
    object MessageShown : BackupsEvent()
}
```

### CurrencyPickerEvent

```kotlin
sealed class CurrencyPickerEvent {
    data class CurrencySelected(val currency: String) : CurrencyPickerEvent()
    data class CurrencyDeleted(val currency: String) : CurrencyPickerEvent()
    object AddNewClicked : CurrencyPickerEvent()
    data class NewCurrencyNameChanged(val name: String) : CurrencyPickerEvent()
    object AddCurrencyConfirmed : CurrencyPickerEvent()
    object AddCurrencyCancelled : CurrencyPickerEvent()
    data class PositionChanged(val position: CurrencyPosition) : CurrencyPickerEvent()
    data class GapChanged(val gap: Boolean) : CurrencyPickerEvent()
    object NoCurrencySelected : CurrencyPickerEvent()
}
```

### ProfileListEvent

```kotlin
sealed class ProfileListEvent {
    data class ProfileSelected(val profile: Profile) : ProfileListEvent()
    data class ProfileEditClicked(val profile: Profile) : ProfileListEvent()
    data class ProfilesReordered(val profiles: List<Profile>) : ProfileListEvent()
    object EditModeToggled : ProfileListEvent()
}
```

---

## エフェクト定義

### BackupsEffect

```kotlin
sealed class BackupsEffect {
    object LaunchBackupFilePicker : BackupsEffect()
    object LaunchRestoreFilePicker : BackupsEffect()
    data class ShowSnackbar(val message: BackupsMessage) : BackupsEffect()
}
```

### SplashEffect

```kotlin
sealed class SplashEffect {
    object NavigateToMain : SplashEffect()
}
```

---

## 状態遷移図

### SplashActivity 状態遷移

```
┌─────────────┐
│   Initial   │
│ initialized │
│   = false   │
└──────┬──────┘
       │ DB.initComplete = true
       ▼
┌─────────────┐
│ Initialized │
│ initialized │
│   = true    │
└──────┬──────┘
       │ 400ms elapsed
       ▼
┌─────────────┐
│  Ready to   │
│  Navigate   │
│ canNavigate │
│   = true    │
└─────────────┘
```

### BackupsActivity 状態遷移

```
┌────────────┐
│   Idle     │
│ backupEnabled │
│ = hasProfile │
└──────┬─────┘
       │ Backup clicked
       ▼
┌────────────┐   URI selected   ┌────────────┐
│  Picking   │ ───────────────► │  Backing   │
│   File     │                  │    Up      │
└────────────┘                  └──────┬─────┘
                                       │ Complete/Error
                                       ▼
                                ┌────────────┐
                                │   Idle +   │
                                │  Message   │
                                └────────────┘
```

---

## ファイル配置

```text
app/src/main/kotlin/net/ktnx/mobileledger/ui/
├── splash/
│   └── SplashUiState.kt        # SplashUiState, SplashEffect
├── backups/
│   └── BackupsUiState.kt       # BackupsUiState, BackupsMessage, BackupsEvent, BackupsEffect
├── components/
│   ├── DatePickerUiState.kt    # DatePickerUiState (optional, may inline)
│   └── CurrencyPickerUiState.kt # CurrencyPickerUiState, CurrencyPosition, CurrencyPickerEvent
└── main/
    └── ProfileListUiState.kt   # ProfileListUiState, ProfileListEvent (extend existing)
```

---

## バリデーションルール

### CurrencyPickerDialog

| フィールド | ルール |
|-----------|--------|
| newCurrencyName | 空でないこと、既存通貨と重複しないこと |

### DatePickerDialog

| フィールド | ルール |
|-----------|--------|
| selectedDate | minDate〜maxDate範囲内であること |
| selectedDate | futureDates設定に従うこと |

---

## 備考

- 全てのUiStateは`data class`で定義し、不変性を維持
- イベントは`sealed class`で定義し、型安全性を確保
- 既存のRoom Entity（Profile, Currency等）は変更なし（FR-007準拠）
