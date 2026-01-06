# MoLe Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-01-05

## Active Technologies
- AGP 8.7.3 / Gradle 8.9 (002-agp-update)
- Kotlin 1.9.25 / Java 8 互換 (002-agp-update)
- Room Database (SQLite) (002-agp-update)
- Kotlin 1.9.25 → 2.0.21, Java 8 互換 (JVM target 1.8) (003-kotlin-update)

- Kotlin 1.9.x (AGP 8.7.3 互換)、現在: Java 8 互換 + AndroidX Lifecycle 2.4.1, Room 2.4.2, Navigation 2.4.2, Jackson 2.17.1, Material 1.5.0 (001-java-kotlin-migration)

## Project Structure

```text
app/
├── src/
│   ├── main/
│   │   ├── java/net/ktnx/mobileledger/   # 移行前 Java ファイル
│   │   ├── kotlin/net/ktnx/mobileledger/ # 移行後 Kotlin ファイル
│   │   └── res/                          # Android リソース
│   ├── test/                             # ユニットテスト
│   └── androidTest/                      # インストルメンテーションテスト
├── build.gradle                          # アプリビルド設定
└── schemas/                              # Room データベーススキーマ
specs/
└── 001-java-kotlin-migration/            # 現在の移行仕様
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

## Code Style

Kotlin 1.9.x (AGP 8.7.3 互換)、現在: Java 8 互換: Follow standard conventions

### Kotlin コードレビュー基準

- `!!` 演算子は原則禁止（使用時は理由コメント必須）
- `var` より `val` を優先
- data class を適切に使用
- スコープ関数のネストは 2 段階まで
- Java 互換性のため `@JvmStatic` / `@JvmField` を適切に使用

## Recent Changes
- 003-kotlin-update: Added Kotlin 1.9.25 → 2.0.21, Java 8 互換 (JVM target 1.8)
- 002-agp-update: Upgraded AGP 8.0.2 → 8.7.3, Gradle 8.0 → 8.9

- 001-java-kotlin-migration: Added Kotlin 1.9.x (AGP 8.7.3 互換)、現在: Java 8 互換 + AndroidX Lifecycle 2.4.1, Room 2.4.2, Navigation 2.4.2, Jackson 2.17.1, Material 1.5.0

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
