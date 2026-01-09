# Research: Complete Compose Migration

**Feature Branch**: `007-complete-compose-migration`
**Date**: 2026-01-10

## 概要

006-compose-ui-rebuildで残存したView/XMLベースのコンポーネントをJetpack Composeに移行するための調査結果。

---

## 1. DatePickerFragment

### 現状分析

**場所**: `app/src/main/kotlin/net/ktnx/mobileledger/ui/DatePickerFragment.kt`

**主要状態**:
- `presentDate: Calendar` - 現在選択中の日付
- `minDate/maxDate: Long` - 選択可能な日付範囲
- `onDatePickedListener: DatePickedListener?` - 選択コールバック

**UI要素**:
- `CalendarView` ウィジェット
- カスタムダイアログレイアウト (`R.layout.date_picker_view`)

**依存関係**:
- `SimpleDate` クラス（日付範囲設定）
- `FutureDates` enum（将来日付制限）

**日付パース正規表現**:
- YMD: `^\s*(\d+)\d*/\s*(\d+)\s*/\s*(\d+)\s*$`
- MD: `^\s*(\d+)\s*/\s*(\d+)\s*$`
- D: `\s*(\d+)\s*$`

### 移行方針

**Decision**: Material3 `DatePicker` Composableを使用

**Rationale**:
- Material3のDatePickerはアクセシビリティ対応済み
- 状態管理が`rememberDatePickerState()`で簡素化
- 日付範囲制限は`SelectableDates`インターフェースで実装可能

**Alternatives considered**:
- カスタムCalendarView Composable → 工数過大、M3で十分

**実装パターン**:
```kotlin
@Composable
fun DatePickerDialog(
    initialDate: LocalDate,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
)
```

---

## 2. CurrencySelectorFragment

### 現状分析

**場所**: `app/src/main/kotlin/net/ktnx/mobileledger/ui/CurrencySelectorFragment.kt`

**主要状態**:
- `model: CurrencySelectorModel?` - UI状態ViewModel
- `mColumnCount: Int` - グリッド列数（デフォルト2）

**UI要素**:
- `RecyclerView` + `CurrencySelectorRecyclerViewAdapter`
- 新規通貨入力用`TextView`
- 通貨位置選択用`RadioGroup`
- ギャップ設定用`SwitchMaterial`

**依存関係**:
- `DB.get().getCurrencyDAO()` - データベースアクセス
- `Data` シングルトン - グローバル設定

**コールバック**:
- `OnCurrencySelectedListener` - 選択通知
- `OnCurrencyLongClickListener` - 削除通知

### 移行方針

**Decision**: Compose Dialog + LazyVerticalGrid

**Rationale**:
- グリッドレイアウトは`LazyVerticalGrid`で簡単に実装
- 状態管理はViewModel + StateFlowで一貫性を維持
- 新規通貨追加UIはCompose TextField + Buttonで実装

**Alternatives considered**:
- LazyColumn（1列）→ 既存のグリッドUIを維持するためVerticalGridを選択

**実装パターン**:
```kotlin
@Composable
fun CurrencyPickerDialog(
    currencies: List<String>,
    onCurrencySelected: (String) -> Unit,
    onCurrencyAdded: (String) -> Unit,
    onCurrencyDeleted: (String) -> Unit,
    onDismiss: () -> Unit
)
```

---

## 3. CrashReportDialogFragment

### 現状分析

**場所**: `app/src/main/kotlin/net/ktnx/mobileledger/ui/CrashReportDialogFragment.kt`

**主要状態**:
- `mCrashReportText: String?` - クラッシュレポートテキスト

**UI要素**:
- `AlertDialog` + カスタムレイアウト (`R.layout.crash_dialog`)
- `ScrollView` 内にクラッシュレポート表示

**ボタン**:
- Positive: "Send Crash Report" → メール送信Intent
- Negative: "Not Now" → ダイアログ閉じる
- Neutral: "Show Report" → テキスト表示切替

**メール送信**:
```kotlin
Intent(Intent.ACTION_SEND).apply {
    putExtra(Intent.EXTRA_EMAIL, arrayOf(Globals.developerEmail))
    putExtra(Intent.EXTRA_SUBJECT, "MoLe crash report")
    putExtra(Intent.EXTRA_TEXT, mCrashReportText)
    type = "message/rfc822"
}
```

### 移行方針

**Decision**: Material3 `AlertDialog` Composable

**Rationale**:
- シンプルな3ボタンダイアログはM3 AlertDialogで直接対応
- スクロール可能テキストは`Column` + `verticalScroll()`で実装
- メール送信Intentはそのまま流用

**Alternatives considered**:
- カスタムDialog → 工数過大、標準AlertDialogで十分

**実装パターン**:
```kotlin
@Composable
fun CrashReportDialog(
    crashReportText: String,
    onSendReport: () -> Unit,
    onDismiss: () -> Unit
)
```

---

## 4. SplashActivity

### 現状分析

**場所**: `app/src/main/kotlin/net/ktnx/mobileledger/ui/activity/SplashActivity.kt`

**主要ロジック**:
1. `onCreate()` → レイアウト設定
2. `onStart()` → DatabaseInitThread起動
3. DB初期化完了 → 400ms最小表示後MainActivityへ遷移

**タイミング定数**:
```kotlin
private const val KEEP_ACTIVE_FOR_MS = 400L
```

**遷移フラグ**:
- `CLEAR_TASK`, `NO_USER_ACTION`, `NEW_TASK`
- フェードイン/アウトアニメーション適用

**依存関係**:
- `DB.get()` - データベースシングルトン
- `DB.initComplete: MutableLiveData<Boolean>` - 初期化フラグ

### 移行方針

**Decision**: ActivityをCompose化し、SplashScreenを追加

**Rationale**:
- Activityとしての役割（DB初期化、画面遷移）は維持
- UIをCompose化してXMLレイアウトを削除
- 将来的にAndroid 12+ Splash Screen APIとの統合も可能

**Alternatives considered**:
- MainActivityComposeに統合 → 初期化ロジック分離のため別Activity維持

**実装パターン**:
```kotlin
@AndroidEntryPoint
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MoLeTheme {
                SplashScreen()
            }
        }
        // 既存の初期化ロジック
    }
}

@Composable
fun SplashScreen() {
    // ロゴ + ローディング表示
}
```

---

## 5. BackupsActivity

### 現状分析

**場所**: `app/src/main/kotlin/net/ktnx/mobileledger/BackupsActivity.kt`

**主要機能**:
- バックアップ（JSONエクスポート）
- リストア（JSONインポート）

**UI要素**:
- ViewBinding (`FragmentBackupsBinding`)
- Toolbar + 2ボタン + 説明テキスト
- Snackbarでステータス表示

**ファイル操作**:
- `ActivityResultContracts.CreateDocument` - バックアップ保存
- `ActivityResultContracts.OpenDocument` - リストア読込

**依存関係**:
- `ConfigWriter` - バックアップ書き込み（バックグラウンドスレッド）
- `ConfigReader` - リストア読み込み（バックグラウンドスレッド）

**バックアップファイル形式**: JSON
```json
{
  "profiles": [...],
  "accounts": [...],
  "commodities": [...],
  "templates": [...],
  "currentProfile": "uuid-string"
}
```

### 移行方針

**Decision**: Compose Activity + Material3 UI

**Rationale**:
- UIはシンプルなボタン2つ + テキストなのでCompose化容易
- ファイル操作ロジック（ConfigReader/Writer）はそのまま流用
- SnackbarはCompose SnackbarHostで置換

**Alternatives considered**:
- ロジック全面書き換え → 不要、既存ロジックは安定

**実装パターン**:
```kotlin
@Composable
fun BackupsScreen(
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    snackbarHostState: SnackbarHostState
)
```

---

## 6. ProfilesRecyclerViewAdapter

### 現状分析

**場所**: `app/src/main/kotlin/net/ktnx/mobileledger/ui/profiles/ProfilesRecyclerViewAdapter.kt`

**主要機能**:
- プロファイルリスト表示
- ドラッグ&ドロップ並べ替え
- 編集モード切替

**主要状態**:
- `editingProfiles: MutableLiveData<Boolean>` - 編集モードフラグ
- `listDiffer: AsyncListDiffer<Profile>` - リスト差分管理
- `rearrangeHelper: ItemTouchHelper` - ドラッグ&ドロップ

**ユーザー操作**:
- 通常モード: タップで選択
- 編集モード: ドラッグで並べ替え、編集ボタンで詳細画面へ

**アニメーション**:
- フェードイン/アウト（編集モード切替時）

**依存関係**:
- `ProfileDAO.updateOrder()` - 並び順永続化
- `Data.setCurrentProfile()` - 選択プロファイル更新
- `ProfileDetailActivity.start()` - 編集画面起動

### 移行方針

**Decision**: Compose LazyColumn + カスタムドラッグジェスチャー

**Rationale**:
- LazyColumnはRecyclerViewの直接的な置換
- ドラッグ&ドロップは`pointerInput()` + `detectDragGesturesAfterLongPress()`で実装
- または`org.burnoutcrew.reorderable`ライブラリ使用

**Alternatives considered**:
- 外部ライブラリ（reorderable）→ 依存関係増加を避けるためカスタム実装を優先
- LazyColumn以外 → リスト表示にはLazyColumnが最適

**実装パターン**:
```kotlin
@Composable
fun ProfileList(
    profiles: List<Profile>,
    selectedProfileId: Long,
    isEditing: Boolean,
    onProfileSelected: (Profile) -> Unit,
    onProfileEdit: (Profile) -> Unit,
    onProfilesReordered: (List<Profile>) -> Unit,
    onEditModeToggle: () -> Unit
)
```

---

## 7. 削除対象アダプター

### AccountAutocompleteAdapter
**場所**: `app/src/main/kotlin/net/ktnx/mobileledger/db/AccountAutocompleteAdapter.kt`
**状態**: 未使用（Compose AccountAutocompleteで置換済み）
**アクション**: 削除

### AccountWithAmountsAutocompleteAdapter
**場所**: `app/src/main/kotlin/net/ktnx/mobileledger/db/AccountWithAmountsAutocompleteAdapter.kt`
**状態**: 未使用
**アクション**: 削除

### TransactionDescriptionAutocompleteAdapter
**場所**: `app/src/main/kotlin/net/ktnx/mobileledger/db/TransactionDescriptionAutocompleteAdapter.kt`
**状態**: 未使用
**アクション**: 削除

### CurrencySelectorRecyclerViewAdapter
**場所**: `app/src/main/kotlin/net/ktnx/mobileledger/ui/CurrencySelectorRecyclerViewAdapter.kt`
**状態**: CurrencySelectorFragmentでのみ使用
**アクション**: CurrencySelectorFragment削除時に同時削除

---

## 8. 削除対象Fragment/その他

### QRScanCapableFragment
**場所**: `app/src/main/kotlin/net/ktnx/mobileledger/ui/QRScanCapableFragment.kt`
**状態**: 抽象基底クラス、具象Fragmentなし
**アクション**: 削除

### OnCurrencySelectedListener / OnCurrencyLongClickListener
**場所**: `app/src/main/kotlin/net/ktnx/mobileledger/ui/`
**状態**: CurrencySelectorFragmentでのみ使用
**アクション**: CurrencySelectorFragment削除時に同時削除

---

## 移行優先度と複雑度

| コンポーネント | 複雑度 | 依存関係 | アニメーション | データバインディング |
|--------------|--------|---------|--------------|-------------------|
| CrashReportDialogFragment | 低 | Intent | なし | 一方向テキスト |
| SplashActivity | 低 | DB初期化 | フェード | LiveData |
| DatePickerFragment | 中 | SimpleDate | なし | コールバック |
| BackupsActivity | 中 | ConfigIO | Snackbar | ViewBinding |
| CurrencySelectorFragment | 高 | DAO, ViewModel | キーボード | RecyclerView + LiveData |
| ProfilesRecyclerViewAdapter | 高 | ItemTouchHelper | フェードイン/アウト | AsyncListDiffer + LiveData |

---

## 推奨実装順序

spec.mdのUser Story優先度（P1→P5）に従いつつ、技術的依存関係を考慮:

1. **Phase 1: US1 - ダイアログ (P1)**
   - DatePickerDialog（中複雑度）
   - CurrencyPickerDialog（高複雑度）

2. **Phase 2: US2 - スプラッシュ (P2)**
   - SplashScreen（低複雑度）

3. **Phase 3: US3 - バックアップ (P3)**
   - BackupsScreen（中複雑度）

4. **Phase 4: US4 - アダプター削除 (P4)**
   - ProfileList Composable（高複雑度）
   - 未使用アダプター削除（削除のみ）

5. **Phase 5: US5 - クラッシュレポート (P5)**
   - CrashReportDialog（低複雑度）
   - QRScanCapableFragment削除
   - 最終クリーンアップ

---

## Composeパターン参照

006-compose-ui-rebuildで確立されたパターンを再利用:

### UiStateパターン
```kotlin
data class BackupsUiState(
    val isLoading: Boolean = false,
    val backupEnabled: Boolean = false,
    val message: String? = null
)
```

### ViewModelパターン
```kotlin
@HiltViewModel
class BackupsViewModel @Inject constructor(
    private val profileDAO: ProfileDAO
) : ViewModel() {
    private val _uiState = MutableStateFlow(BackupsUiState())
    val uiState: StateFlow<BackupsUiState> = _uiState.asStateFlow()
}
```

### Dialogパターン
```kotlin
@Composable
fun MyDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onConfirm) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Title") },
        text = { Text("Content") }
    )
}
```
