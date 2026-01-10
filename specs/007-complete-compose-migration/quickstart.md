# Quickstart: Complete Compose Migration

**Feature Branch**: `007-complete-compose-migration`
**Date**: 2026-01-10

## 概要

006-compose-ui-rebuildで残存したView/XMLベースのコンポーネントをJetpack Composeに移行し、Viewベースのコードを完全に削除する。

---

## 前提条件

- [x] 006-compose-ui-rebuildが完了していること
- [x] Compose BOM 2024.12.01が設定済み
- [x] MoLeTheme、共通コンポーネントが利用可能
- [x] Hilt DIが設定済み

---

## 開発環境

```bash
# 開発シェルに入る
nix develop

# ビルド確認
nix run .#build

# テスト実行
nix run .#test

# フルワークフロー（テスト → ビルド → インストール）
nix run .#verify
```

---

## 移行対象コンポーネント

### ダイアログ（US1 - P1）

| 現行 | 新規 | 優先度 |
|------|------|--------|
| DatePickerFragment | DatePickerDialog.kt | 高 |
| CurrencySelectorFragment | CurrencyPickerDialog.kt | 高 |

### 画面（US2, US3）

| 現行 | 新規 | 優先度 |
|------|------|--------|
| SplashActivity + XML | SplashActivity + SplashScreen.kt | 中 |
| BackupsActivity + ViewBinding | BackupsActivity + BackupsScreen.kt | 中 |

### アダプター（US4）

| 現行 | 新規 | 優先度 |
|------|------|--------|
| ProfilesRecyclerViewAdapter | ProfileList Composable | 中 |
| AccountAutocompleteAdapter | 削除（未使用） | 低 |
| AccountWithAmountsAutocompleteAdapter | 削除（未使用） | 低 |
| TransactionDescriptionAutocompleteAdapter | 削除（未使用） | 低 |
| CurrencySelectorRecyclerViewAdapter | 削除（Fragment削除時） | 低 |

### その他（US5 - P5）

| 現行 | 新規 | 優先度 |
|------|------|--------|
| CrashReportDialogFragment | CrashReportDialog.kt | 低 |
| QRScanCapableFragment | 削除 | 低 |

---

## 実装パターン

### Compose Dialog パターン

```kotlin
@Composable
fun MyDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = { Text("Title") },
        text = { Text("Content") }
    )
}
```

### Material3 DatePicker パターン

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    initialDateMillis: Long?,
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
            }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
```

### Activity + Compose パターン

```kotlin
@AndroidEntryPoint
class MyActivity : ComponentActivity() {
    private val viewModel: MyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MoLeTheme {
                MyScreen(viewModel = viewModel)
            }
        }
    }
}
```

### ViewModel パターン

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val someDAO: SomeDAO
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyUiState())
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()

    private val _effects = Channel<MyEffect>()
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: MyEvent) {
        when (event) {
            is MyEvent.SomeAction -> handleSomeAction()
        }
    }
}
```

---

## ファイル配置

### 新規作成

```text
app/src/main/kotlin/net/ktnx/mobileledger/ui/
├── components/
│   ├── DatePickerDialog.kt      # 新規
│   ├── CurrencyPickerDialog.kt  # 新規
│   └── CrashReportDialog.kt     # 新規
├── splash/
│   ├── SplashScreen.kt          # 新規
│   ├── SplashViewModel.kt       # 新規（オプション）
│   └── SplashUiState.kt         # 新規
├── backups/
│   ├── BackupsScreen.kt         # 新規
│   ├── BackupsViewModel.kt      # 新規
│   └── BackupsUiState.kt        # 新規
└── main/
    └── ProfileListComposable.kt # 新規（既存NavigationDrawer拡張）
```

### 削除対象

```text
# Fragment
app/src/main/kotlin/net/ktnx/mobileledger/ui/
├── DatePickerFragment.kt
├── CurrencySelectorFragment.kt
├── CurrencySelectorRecyclerViewAdapter.kt
├── QRScanCapableFragment.kt
├── CrashReportDialogFragment.kt
├── OnCurrencySelectedListener.kt
├── OnCurrencyLongClickListener.kt
└── profiles/
    └── ProfilesRecyclerViewAdapter.kt

# Adapter
app/src/main/kotlin/net/ktnx/mobileledger/db/
├── AccountAutocompleteAdapter.kt
├── AccountWithAmountsAutocompleteAdapter.kt
└── TransactionDescriptionAutocompleteAdapter.kt

# XML Layout（全て削除 - SC-001）
app/src/main/res/layout/
├── splash_activity_layout.xml
├── fragment_backups.xml
├── crash_dialog.xml
├── date_picker_view.xml
├── fragment_currency_selector_list.xml
├── fragment_currency_selector.xml
├── profile_list_content.xml
├── account_autocomplete_row.xml
└── hue_dialog.xml
```

---

## 検証コマンド

### 各Phase完了後

```bash
# テスト実行
nix run .#test

# ビルド確認
nix run .#build

# 実機検証
nix run .#verify
```

### 最終検証（SC確認）

```bash
# SC-001: XMLレイアウトファイル数確認
ls app/src/main/res/layout/ | wc -l  # 期待値: 0

# SC-002: Fragment数確認
grep -r "extends.*Fragment" app/src/main/kotlin/ | wc -l  # 期待値: 0
grep -r ": Fragment(" app/src/main/kotlin/ | wc -l  # 期待値: 0

# SC-003: ViewBinding使用確認
grep -r "ViewBinding" app/src/main/kotlin/ | wc -l  # 期待値: 0

# SC-005: APKサイズ確認
ls -la app/build/outputs/apk/debug/*.apk  # 期待値: ~27MB ±5%

# SC-006: 起動時間測定
adb shell am start-activity -W -n net.ktnx.mobileledger.debug/.ui.activity.SplashActivity
# 期待値: TotalTime ~526ms ±20%
```

---

## トラブルシューティング

### ビルドエラー: 未使用インポート

削除したFragment/Adapterを参照しているファイルがある場合:
```bash
# 参照箇所を検索
grep -r "DatePickerFragment" app/src/main/kotlin/
grep -r "CurrencySelectorFragment" app/src/main/kotlin/
```

### 実行時エラー: Fragment not found

AndroidManifest.xmlやNavigation XMLに残存参照がないか確認:
```bash
grep -r "DatePickerFragment" app/src/main/
grep -r "CurrencySelectorFragment" app/src/main/
```

### Compose Preview表示されない

`@Preview`アノテーションが正しいか確認:
```kotlin
@Preview(showBackground = true)
@Composable
fun MyScreenPreview() {
    MoLeTheme {
        MyScreen()
    }
}
```

---

## 参考資料

- [006-compose-ui-rebuild plan.md](../006-compose-ui-rebuild/plan.md)
- [Material3 DatePicker](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#DatePicker)
- [Compose Dialog](https://developer.android.com/develop/ui/compose/components/dialog)
