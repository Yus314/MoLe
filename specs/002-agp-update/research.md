# 調査報告: AGP 8.7.3 アップグレード

**日付**: 2026-01-06
**機能ブランチ**: `002-agp-update`

## サマリー

AGP 8.0.2 から 8.7.3 へのアップグレードに必要な互換性要件と潜在的な問題を調査。すべての要件は現在のプロジェクト設定と互換性があることを確認。

## 調査項目

### 1. AGP 8.7.x の Gradle バージョン要件

**決定**: Gradle 8.9 を使用

**根拠**:
- AGP 8.7.0 の公式ドキュメントによると、**Gradle 8.9 が最小要件かつデフォルトバージョン**
- 参照: [Android Gradle Plugin 8.7.0 Release Notes](https://developer.android.com/build/releases/past-releases/agp-8-7-0-release-notes)

**検討した代替案**:
| バージョン | 評価 |
|-----------|------|
| Gradle 8.9 | ✅ 採用 - 最小要件を満たし、安定性が高い |
| Gradle 8.10+ | ❌ 不採用 - 追加機能は不要、リスク最小化を優先 |

### 2. JDK バージョン要件

**決定**: JDK 17 を使用（Nix Flake環境で提供）

**根拠**:
- AGP 8.7.0 は **JDK 17 が必須**
- 現在の Nix Flake 環境で JDK 17 が提供されている

**影響**:
- `compileOptions` の `sourceCompatibility` / `targetCompatibility` は Java 8 互換のまま維持可能
- ビルドツールとしては JDK 17、ランタイム互換性は Java 8

### 3. Kotlin 1.9.25 との互換性

**決定**: Kotlin 1.9.25 をそのまま維持

**根拠**:
- [AGP、D8、R8 と Kotlin バージョンの互換性](https://developer.android.com/build/kotlin-support)によると、Kotlin 1.9.x は AGP 8.0 以上が必要
- AGP 8.7.x は Kotlin 1.9.x と完全に互換性がある
- 参照: [Kotlin Compatibility Guide for 1.9](https://kotlinlang.org/docs/compatibility-guide-19.html)

**検討した代替案**:
| バージョン | 評価 |
|-----------|------|
| Kotlin 1.9.25 | ✅ 採用 - 現行バージョン、AGP 8.7と互換 |
| Kotlin 2.0.x | ❌ 不採用 - スコープ外、別タスクで検討 |

### 4. Room 2.4.2 との互換性

**決定**: Room 2.4.2 をそのまま維持（kapt使用）

**根拠**:
- Room 2.4.2 は kapt を使用しており、AGP 8.7.x と互換性がある
- kapt は Kotlin 1.9.x で引き続きサポートされている
- プロジェクトは Java→Kotlin 移行中であり、Room の KSP 移行は移行完了後に検討

**リスク**:
- kapt は KSP より遅いが、機能的には問題なし
- 将来的に KSP への移行を推奨（別タスク）

**検討した代替案**:
| 構成 | 評価 |
|------|------|
| Room 2.4.2 + kapt | ✅ 採用 - 現行構成、互換性確認済み |
| Room 2.6.x + KSP | ❌ 不採用 - スコープ外、Java→Kotlin移行後に検討 |

### 5. AndroidX ライブラリの互換性

**決定**: 既存のバージョンを維持

**根拠**:
- Lifecycle 2.4.1、Navigation 2.4.2、Material 1.5.0 は AGP 8.7.x と互換性がある
- これらのライブラリは targetSdk 34 で動作確認済み

**影響なし**:
- compileSdk / targetSdk 34 は AGP 8.7.x でサポート（最大 API 35）
- minSdk 22 も引き続きサポート

### 6. AGP 8.7.x の重要な変更点

**Lint 動作の変更（注意必要）**:
- AGP 8.7.0-alpha08 以降、`LintError` が発生した場合、lint 解析タスクが例外をスローする
- 既存の lint baseline ファイルに問題がある場合、ビルドが失敗する可能性あり

**対応策**:
1. アップグレード後に `nix run .#build` でビルドを確認
2. Lint エラーが発生した場合:
   - 問題のあるライブラリ依存関係を更新
   - 必要に応じて問題の lint チェックを無効化

**SDK Build Tools**:
- 最小 / デフォルト: 34.0.0（現在のプロジェクトと一致）

**NDK**:
- デフォルトが 27.x に更新されるが、プロジェクトで NDK を使用していないため影響なし

### 7. 更新が必要なファイル

| ファイル | 変更内容 |
|---------|---------|
| `gradle/libs.versions.toml` | `agp = "8.0.2"` → `agp = "8.7.3"` |
| `gradle/wrapper/gradle-wrapper.properties` | `distributionUrl` を gradle-8.9-bin.zip に更新 |

**ソースコード変更**: 不要

## 互換性マトリックス

| コンポーネント | 現在 | 更新後 | 互換性 |
|---------------|------|--------|--------|
| AGP | 8.0.2 | 8.7.3 | ✅ |
| Gradle | 8.0 | 8.9 | ✅ |
| Kotlin | 1.9.25 | 1.9.25 | ✅ 変更なし |
| JDK | 17 | 17 | ✅ 変更なし |
| Room | 2.4.2 | 2.4.2 | ✅ 変更なし |
| compileSdk | 34 | 34 | ✅ 変更なし |
| targetSdk | 34 | 34 | ✅ 変更なし |
| minSdk | 22 | 22 | ✅ 変更なし |
| Build Tools | 34.0.0 | 34.0.0 | ✅ 変更なし |

## リスク評価

| リスク | 影響度 | 発生確率 | 緩和策 |
|--------|--------|----------|--------|
| Lint エラーによるビルド失敗 | 中 | 低 | lint baseline を確認、必要に応じてチェック無効化 |
| 依存関係の解決エラー | 低 | 低 | Version Catalog の整合性を確認 |
| ビルド時間の増加 | 低 | 低 | Gradle デーモンのキャッシュを活用 |

## 結論

AGP 8.0.2 → 8.7.3、Gradle 8.0 → 8.9 へのアップグレードは、既存のプロジェクト設定と完全に互換性があります。ソースコードの変更は不要で、ビルド設定ファイル（libs.versions.toml、gradle-wrapper.properties）のみの更新で完了します。

## 参考資料

- [Android Gradle Plugin 8.7.0 Release Notes](https://developer.android.com/build/releases/past-releases/agp-8-7-0-release-notes)
- [AGP Release Notes](https://developer.android.com/build/releases/gradle-plugin)
- [AGP、D8、R8 と Kotlin バージョンの互換性](https://developer.android.com/build/kotlin-support)
- [Gradle Compatibility Matrix](https://docs.gradle.org/current/userguide/compatibility.html)
- [Kotlin Compatibility Guide for 1.9](https://kotlinlang.org/docs/compatibility-guide-19.html)
