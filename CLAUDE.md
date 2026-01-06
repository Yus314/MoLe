# MoLe Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-01-06

## Active Technologies
- AGP 8.7.3 / Gradle 8.9 (002-agp-update)
- Kotlin 2.0.21 / Coroutines 1.9.0 / Java 8 互換 (JVM target 1.8) (003-kotlin-update)
- Room Database (SQLite) with KSP 2.0.21-1.0.26 (004-kapt-ksp-migration)
- Hilt 2.51.1 for Dependency Injection (005-hilt-di-setup)
- AndroidX Lifecycle 2.4.1, Room 2.4.2, Navigation 2.4.2, Jackson 2.17.1, Material 1.5.0 (001-java-kotlin-migration)
- Kotlin 2.0.21 / Java 8互換 (JVM target 1.8) (006-compose-ui-rebuild)
- Room Database（既存、変更なし） (006-compose-ui-rebuild)

## Project Structure

```text
app/
├── src/
│   ├── main/
│   │   ├── kotlin/net/ktnx/mobileledger/ # Kotlin ソースファイル
│   │   │   └── di/                       # Hilt DI モジュール
│   │   └── res/                          # Android リソース
│   ├── test/                             # ユニットテスト
│   └── androidTest/                      # インストルメンテーションテスト
├── build.gradle                          # アプリビルド設定
└── schemas/                              # Room データベーススキーマ
specs/
└── 005-hilt-di-setup/                    # 現在の機能仕様
```

## Commands

### Nix Flake コマンド（推奨）

| コマンド | 説明 |
|----------|------|
| `nix run .#build` | デバッグ APK ビルド |
| `nix run .#test` | ユニットテスト実行 |
| `nix run .#clean` | ビルド成果物のクリーン |
| `nix run .#install` | ビルド → 実機インストール |
| `nix run .#verify` | **フルワークフロー（テスト → ビルド → インストール）** |
| `nix run .#buildRelease` | リリース APK ビルド（署名必要）|
| `nix run .#lint` | Android Lint チェック（CI 用）|

### 開発シェル

```bash
nix develop .#fhs    # FHS 互換環境（ビルド可能）
nix develop          # 通常の開発シェル
```

### 注意事項

`nix run .#test` や `nix run .#build` などの出力を確認したい場合、パイプで `tail` や `head` を使用しないでください。出力が確認できなくなります。

```bash
# NG: 出力が表示されない
nix run .#test | tail -100

# OK: そのまま実行
nix run .#test
```

### Gradle コマンド（FHS 環境内）

```bash
./gradlew assembleDebug              # デバッグビルド
./gradlew test                       # ユニットテスト
./gradlew test --tests "*.TestName"  # 特定テスト実行
./gradlew lintDebug                  # Lint チェック
```

## Development Workflow

### 機能実装後の検証フロー

**重要**: 全ての機能変更は実機で検証する

```bash
# 推奨: フルワークフロー
nix run .#verify
```

このコマンドで以下が自動実行されます:
1. ユニットテスト
2. デバッグ APK ビルド
3. 実機へのインストール

その後、手動で以下を確認:
- アプリが正常に起動する
- データ更新が動作する
- プロファイル作成/編集ができる
- 取引登録ができる

### ログ確認

```bash
adb logcat | grep -E "(MoLe|mobileledger)"
```

## Pre-commit Hooks

開発環境（`nix develop`）に入ると、pre-commit フックが自動的にインストールされます。

### 有効なフック

- **ktlint**: Kotlin コードスタイルチェック
- **detekt**: Kotlin 静的解析

### 手動実行

```bash
pre-commit run --all-files    # 全ファイルをチェック
pre-commit run ktlint         # ktlint のみ実行
pre-commit run detekt         # detekt のみ実行
```

### 設定ファイル

- `.editorconfig`: ktlint のルール設定
- `detekt.yml`: detekt の設定（maxIssues: -1 で警告のみ、エラーにしない）

### ktlint ルール状況

ほとんどのルールが有効化されています。以下のルールのみ永久無効化:

| ルール | 無効化理由 |
|--------|-----------|
| `filename` | Abstract* プレフィックスは意図的 |
| `property-naming` | JSON フィールド名との互換性（ptransaction_ 等）|
| `backing-property-naming` | 特殊なパターンの許容 |
| `function-naming` | 特殊な関数名の許容 |
| `no-wildcard-imports` | Android Studio デフォルト動作 |
| `package-name` | API バージョンサフィックス（v1_14 等）|
| `enum-entry-name-case` | JSON シリアライズ互換性 |
| `kdoc` | KDoc スタイル強制なし |

## Code Style

Kotlin 2.0.21 / Java 8 互換 (JVM target 1.8): Follow standard conventions

### Kotlin コードレビュー基準

- `!!` 演算子は原則禁止（使用時は理由コメント必須）
- `var` より `val` を優先
- data class を適切に使用
- スコープ関数のネストは 2 段階まで

## Hilt Dependency Injection

### 新しい ViewModel の作成方法

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val profileDAO: ProfileDAO,
    private val data: Data
) : ViewModel() {
    // ビジネスロジック
}
```

### Activity での使用

```kotlin
@AndroidEntryPoint
class MyActivity : AppCompatActivity() {
    private val viewModel: MyViewModel by viewModels()
}
```

### 利用可能な依存関係

- **DatabaseModule**: DB, ProfileDAO, TransactionDAO, AccountDAO, AccountValueDAO, TemplateHeaderDAO, TemplateAccountDAO, CurrencyDAO, OptionDAO
- **DataModule**: Data (グローバル状態)

### DI ベストプラクティス

- 新しい ViewModel は `@HiltViewModel` と `@Inject constructor` を使用
- Activity は `@AndroidEntryPoint` でマーク
- `by viewModels()` デリゲートで ViewModel 取得
- 既存の `DB.get()` / `Data` 直接アクセスは動作するが、新規コードでは DI を使用

## Recent Changes
- 006-compose-ui-rebuild: Added Kotlin 2.0.21 / Java 8互換 (JVM target 1.8)
- ktlint-enforcement: Enabled most ktlint rules, auto-fixed 200+ files across 4 phases, permanently disabled 8 rules for JSON/API compatibility
- 005-hilt-di-setup: Added Hilt 2.51.1 DI framework, migrated MainModel to constructor injection, created DatabaseModule and DataModule, added instrumentation test infrastructure with HiltTestRunner and TestDatabaseModule

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
