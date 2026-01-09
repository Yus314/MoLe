# Quickstart: Jetpack Compose UI Rebuild

**Feature**: 006-compose-ui-rebuild
**Date**: 2026-01-06

## Prerequisites

- Nix環境がセットアップ済み
- Android Studio または IntelliJ IDEA（Compose Preview用）
- 実機またはエミュレータ（API 22以上）

## Setup

### 1. 開発環境に入る

```bash
cd /home/kaki/MoLe
nix develop .#fhs
```

### 2. 依存関係の追加（Phase 1で実施）

`gradle/libs.versions.toml` に以下を追加:

```toml
[versions]
# ... 既存 ...
composeBom = "2024.12.01"
composeNavigation = "2.8.5"
composeHilt = "1.2.0"

[libraries]
# ... 既存 ...
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

[bundles]
# ... 既存 ...
compose = ["compose-ui", "compose-ui-graphics", "compose-ui-tooling-preview", "compose-material3"]
compose-testing = ["compose-ui-test-junit4"]
```

`app/build.gradle` に以下を追加:

```gradle
android {
    // ... 既存 ...
    buildFeatures {
        viewBinding = true
        compose = true  // 追加
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"  // Kotlin 2.0.21対応
    }
}

dependencies {
    // ... 既存 ...

    // Compose
    implementation platform(libs.compose.bom)
    implementation libs.bundles.compose
    implementation libs.compose.navigation
    implementation libs.compose.hilt.navigation

    // Compose Testing
    androidTestImplementation platform(libs.compose.bom)
    androidTestImplementation libs.bundles.compose.testing
    debugImplementation libs.compose.ui.tooling
    debugImplementation libs.compose.ui.test.manifest
}
```

### 3. Kotlin Compiler Plugin設定

`build.gradle` (project level) に追加が必要な場合:

```gradle
plugins {
    // ... 既存 ...
    alias(libs.plugins.kotlin.compose) apply false  // Kotlin 2.0+の場合
}
```

`gradle/libs.versions.toml` に追加:

```toml
[plugins]
# ... 既存 ...
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

`app/build.gradle` に追加:

```gradle
plugins {
    // ... 既存 ...
    alias(libs.plugins.kotlin.compose)
}
```

## Build & Run

### テスト実行

```bash
nix run .#test
```

### ビルド

```bash
nix run .#build
```

### 実機インストール

```bash
nix run .#install
```

### フルワークフロー（推奨）

```bash
nix run .#verify
```

## Development Workflow

### 新しいComposable関数の作成

1. `app/src/main/kotlin/net/ktnx/mobileledger/ui/` 配下に配置
2. TDDに従い、テストを先に書く
3. `@Preview` アノテーションでプレビュー確認
4. ktlint/detektチェック通過を確認

### Compose Preview の使用

```kotlin
@Preview(showBackground = true)
@Composable
fun ProfileDetailScreenPreview() {
    MoLeTheme {
        ProfileDetailScreen(
            uiState = ProfileDetailUiState(name = "Test Profile"),
            onEvent = {}
        )
    }
}
```

Android Studio で Preview ペインを開いて確認。

### テストの書き方

```kotlin
@HiltAndroidTest
class ProfileDetailScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun nameField_displaysValue() {
        composeTestRule.setContent {
            MoLeTheme {
                ProfileDetailScreen(
                    uiState = ProfileDetailUiState(name = "Test"),
                    onEvent = {}
                )
            }
        }

        composeTestRule
            .onNodeWithTag("profileNameField")
            .assertTextContains("Test")
    }
}
```

## File Structure

新規ファイルの配置先:

```
app/src/main/kotlin/net/ktnx/mobileledger/
├── ui/
│   ├── theme/
│   │   ├── Theme.kt           # MoLeTheme
│   │   ├── Color.kt           # カラーパレット
│   │   └── Type.kt            # Typography
│   ├── components/
│   │   └── *.kt               # 共通コンポーネント
│   ├── profile/
│   │   ├── ProfileDetailScreen.kt
│   │   └── ProfileDetailViewModel.kt
│   └── ... (各画面ディレクトリ)
```

テストファイルの配置先:

```
app/src/test/kotlin/net/ktnx/mobileledger/
└── ui/
    └── ... (UIテスト)

app/src/androidTest/kotlin/net/ktnx/mobileledger/
└── ui/
    └── ... (Instrumentation UIテスト)
```

## Troubleshooting

### Compose Compiler バージョンエラー

```
Compose Compiler requires Kotlin 1.x.x but found 2.x.x
```

**解決策**: Kotlin 2.0.x では `kotlin-compose` plugin を使用

### Preview が表示されない

**解決策**:
1. Build → Rebuild Project
2. `@Preview` が正しくインポートされているか確認
3. 関数が `@Composable` でマークされているか確認

### Hilt injection が失敗

```
Expected @HiltAndroidApp on class...
```

**解決策**: テストクラスに `@HiltAndroidTest` を追加し、`HiltAndroidRule` を設定

## References

- [Compose BOM Documentation](https://developer.android.com/jetpack/compose/bom)
- [Material 3 for Compose](https://developer.android.com/jetpack/compose/designsystems/material3)
- [Compose Navigation](https://developer.android.com/jetpack/compose/navigation)
- [Compose Testing](https://developer.android.com/jetpack/compose/testing)
- [Hilt with Compose](https://developer.android.com/jetpack/compose/libraries#hilt)
