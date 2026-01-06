# Research: Jetpack Compose UI Rebuild

**Feature**: 006-compose-ui-rebuild
**Date**: 2026-01-06

## 1. Compose BOM Version Selection

### Decision
Compose BOM 2024.12.01 を採用

### Rationale
- Kotlin 2.0.21との互換性が確認済み
- 安定版として十分な実績がある
- Material 3の最新APIが含まれる
- Compose Navigation 2.8.xとの互換性

### Alternatives Considered
| Version | 評価 | 不採用理由 |
|---------|------|-----------|
| BOM 2024.09.00 | 安定だが古い | Material 3の一部機能が欠落 |
| BOM 2025.01.00-alpha | 最新 | アルファ版は本番非推奨 |

### Dependencies to Add
```toml
# gradle/libs.versions.toml に追加
composeBom = "2024.12.01"
composeNavigation = "2.8.5"
composeHilt = "1.2.0"

# libraries に追加
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-ui-graphics = { module = "androidx.compose.ui:ui-graphics" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-navigation = { module = "androidx.navigation:navigation-compose", version.ref = "composeNavigation" }
compose-hilt-navigation = { module = "androidx.hilt:hilt-navigation-compose", version.ref = "composeHilt" }
compose-ui-test-junit4 = { module = "androidx.compose.ui:ui-test-junit4" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-ui-test-manifest = { module = "androidx.compose.ui:ui-test-manifest" }
```

---

## 2. minSdk Compatibility

### Decision
現行のminSdk 22を維持

### Rationale
- Compose BOM 2024.12.01はminSdk 21から対応
- 既存ユーザーへの影響を回避
- Android 5.1 (API 22)以上のデバイスをサポート継続

### Considerations
- Compose Compiler 1.5.0以降はKotlin 2.0に対応
- Compose UIのパフォーマンスはAPI 26+で最適化されるが、22でも動作する

---

## 3. State Management Migration

### Decision
LiveData → StateFlow への段階的移行

### Rationale
- ComposeはFlowとの相性が良い（collectAsState）
- StateFlowは複数のコレクターに対応
- Coroutinesとの統合がシームレス

### Migration Pattern
```kotlin
// 既存: LiveData
class OldViewModel : ViewModel() {
    private val _data = MutableLiveData<String>()
    val data: LiveData<String> = _data
}

// 新規: StateFlow
class NewViewModel : ViewModel() {
    private val _data = MutableStateFlow<String>("")
    val data: StateFlow<String> = _data.asStateFlow()
}

// Composable内での使用
@Composable
fun MyScreen(viewModel: NewViewModel = hiltViewModel()) {
    val data by viewModel.data.collectAsState()
}
```

### Alternatives Considered
| Approach | 評価 | 不採用理由 |
|----------|------|-----------|
| LiveData継続 + observeAsState | 動作する | Composeとの相性が劣る |
| MutableState直接使用 | シンプル | ViewModelとの分離が困難 |

---

## 4. Material 3 Theme with HSL Dynamic Colors

### Decision
Material 3を採用し、既存のHSLベースカラーシステムを統合

### Rationale
- Material 3はdynamic colorをネイティブサポート
- HSL色空間からMaterial 3のColorSchemeへの変換が可能
- 既存のプロファイルごとのテーマカラーを維持

### Implementation Approach
```kotlin
@Composable
fun MoLeTheme(
    profileHue: Float = 0f,  // 0-360
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = remember(profileHue, darkTheme) {
        if (darkTheme) {
            darkColorSchemeFromHue(profileHue)
        } else {
            lightColorSchemeFromHue(profileHue)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MoLeTypography,
        content = content
    )
}

private fun lightColorSchemeFromHue(hue: Float): ColorScheme {
    val primary = Color.hsl(hue, 0.6f, 0.5f)
    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        // ... 他のカラー
    )
}
```

---

## 5. HueRing Custom Composable

### Decision
Canvas APIを使用してHueRingを再実装

### Rationale
- 既存のHueRing.ktはCanvasベースのカスタムView
- Compose Canvasは同等の描画機能を提供
- ジェスチャー検出も Modifier.pointerInput で対応可能

### Implementation Approach
```kotlin
@Composable
fun HueRing(
    selectedHue: Float,
    onHueSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .size(200.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val angle = calculateAngle(change.position, size)
                    onHueSelected(angle)
                }
            }
    ) {
        // HSLカラーリングの描画
        for (i in 0 until 360) {
            drawArc(
                color = Color.hsl(i.toFloat(), 1f, 0.5f),
                startAngle = i.toFloat() - 90f,
                sweepAngle = 1.5f,
                useCenter = false,
                style = Stroke(width = ringWidth.toPx())
            )
        }
        // 選択インジケーターの描画
        drawCircle(/* ... */)
    }
}
```

---

## 6. Navigation Strategy

### Decision
Compose Navigation を使用し、Activity間遷移は維持

### Rationale
- 各Activityは独立したNavGraphを持つ
- 段階的移行が容易（Activity単位で移行）
- Deep linkサポートが組み込み

### Navigation Structure
```
MainActivity (Phase 3)
└── MainNavGraph
    ├── AccountSummaryTab
    └── TransactionListTab

TemplatesActivity (Phase 2)
└── TemplatesNavGraph
    ├── TemplateListScreen
    └── TemplateDetailScreen

NewTransactionActivity (Phase 4)
└── TransactionNavGraph
    ├── NewTransactionScreen
    └── TransactionSavingScreen

ProfileDetailActivity (Phase 1)
└── 単一画面（NavGraph不要）
```

---

## 7. Compose-XML Interoperability

### Decision
ComposeViewを使用した段階的移行

### Rationale
- 既存のActivityをホストとして維持
- setContent{}でCompose画面を表示
- Phase完了後にActivity全体をCompose化

### Interop Pattern
```kotlin
// Phase 1: Activity内でCompose画面をホスト
class ProfileDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MoLeTheme(profileHue = viewModel.profileHue) {
                ProfileDetailScreen(
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
```

---

## 8. Testing Strategy

### Decision
既存テスト維持 + Compose UIテスト追加

### Rationale
- 既存のユニットテストはViewModel層をテスト（変更不要）
- Compose UIテストはComposeTestRuleを使用
- インストルメンテーションテストは段階的に移行

### Test Structure
```kotlin
@HiltAndroidTest
class ProfileDetailScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ProfileDetailActivity>()

    @Test
    fun profileNameField_displaysCorrectly() {
        composeTestRule.onNodeWithText("プロファイル名").assertIsDisplayed()
    }

    @Test
    fun saveButton_enabledWhenFormValid() {
        // フォーム入力
        composeTestRule.onNodeWithTag("profileName").performTextInput("Test")
        composeTestRule.onNodeWithTag("serverUrl").performTextInput("https://example.com")

        // 保存ボタンが有効化されることを確認
        composeTestRule.onNodeWithText("保存").assertIsEnabled()
    }
}
```

---

## 9. Performance Optimization

### Decision
LazyColumnのkey指定とderivadStateOfの活用

### Rationale
- LazyColumnのkey指定でリコンポジション最適化
- derivedStateOfで不要な再計算を防止
- remember + mutableStateOfで状態を効率的に管理

### Best Practices
```kotlin
// LazyColumnでのkey指定
LazyColumn {
    items(
        items = transactions,
        key = { it.id }  // 重要: 一意のキーを指定
    ) { transaction ->
        TransactionItem(transaction)
    }
}

// derivedStateOfの使用
val showFab by remember {
    derivedStateOf { listState.firstVisibleItemIndex == 0 }
}
```

---

## 10. Accessibility Considerations

### Decision
Composeの組み込みアクセシビリティを活用

### Rationale
- Composeは自動的にcontentDescriptionを生成
- Modifier.semanticsで追加情報を提供可能
- 既存実装と同等のアクセシビリティを維持

### Implementation Notes
```kotlin
// 明示的なcontentDescription
Icon(
    imageVector = Icons.Default.Add,
    contentDescription = "新規追加",  // TalkBackで読み上げられる
    modifier = Modifier.clickable { /* ... */ }
)

// カスタムセマンティクス
Box(
    modifier = Modifier.semantics {
        contentDescription = "アカウント残高: $balance"
        stateDescription = if (isNegative) "マイナス残高" else "プラス残高"
    }
)
```

---

## Summary

| 項目 | 決定 |
|------|------|
| Compose BOM | 2024.12.01 |
| minSdk | 22（維持） |
| 状態管理 | StateFlow |
| テーマ | Material 3 + HSL動的カラー |
| ナビゲーション | Compose Navigation（Activity単位） |
| テスト | Compose Testing + 既存テスト維持 |
| パフォーマンス | LazyColumn + key + derivedStateOf |
