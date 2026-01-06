# 実装計画: AGP 最新安定版へのアップデート

**ブランチ**: `002-agp-update` | **日付**: 2026-01-06 | **仕様書**: [spec.md](./spec.md)
**入力**: `/specs/002-agp-update/spec.md` からの機能仕様

## サマリー

Android Gradle Plugin (AGP) を 8.0.2 から 8.7.3 にアップグレードし、それに伴い Gradle Wrapper を 8.0 から 8.9 に更新する。Version Catalog (libs.versions.toml) を更新し、すべての依存関係の互換性を確認する。ビルド、テスト、実機での動作確認を行い、リグレッションがないことを検証する。

## Technical Context

**Language/Version**: Kotlin 1.9.25 / Java 8 互換
**Primary Dependencies**:
- AGP: 8.0.2 → **8.7.3** (アップグレード対象)
- Gradle: 8.0 → **8.9** (アップグレード対象)
- AndroidX Lifecycle: 2.4.1
- Room: 2.4.2 (kapt使用)
- Navigation: 2.4.2
- Material: 1.5.0
- Jackson: 2.17.1

**Storage**: Room Database (SQLite)
**Testing**: JUnit 4.13.2, AndroidX Test Runner, Espresso
**Target Platform**: Android (minSdk 22, targetSdk 34, compileSdk 34)
**Project Type**: Mobile (Android)
**Performance Goals**: ビルド時間が50%以上増加しないこと
**Constraints**: 既存コードの変更なし（ビルド設定のみ）、Nix Flake環境でのビルド
**Scale/Scope**: 単一Androidアプリケーション

## Constitution Check

*GATE: Phase 0 調査前に確認必須。Phase 1 設計後に再確認。*

| 原則 | ステータス | 備考 |
|------|----------|------|
| I. コードの可読性とメンテナンス性 | ✅ 該当なし | ビルド設定変更のみ、ソースコード変更なし |
| II. 単体テスト | ✅ 適合 | 既存テストがすべて成功することを検証 |
| III. 最小構築・段階的開発 | ✅ 適合 | バージョン更新→ビルド確認→テスト→実機確認の段階的アプローチ |
| IV. パフォーマンス最適化 | ✅ 適合 | ビルド時間の50%以上増加を許容しない |
| V. アクセシビリティ | ✅ 該当なし | UI変更なし |
| VI. Kotlin移行 | ✅ 適合 | Kotlin 1.9.25との互換性維持 |
| VII. Nix開発環境 | ✅ 適合 | Nix Flakeコマンドでビルド・テスト・インストール |

**ゲート結果**: ✅ すべてパス - Phase 0 に進行可能

## Project Structure

### Documentation (this feature)

```text
specs/002-agp-update/
├── plan.md              # このファイル (/speckit.plan コマンド出力)
├── research.md          # Phase 0 出力
├── data-model.md        # Phase 1 出力（該当なし - ビルド設定変更のみ）
├── quickstart.md        # Phase 1 出力
└── tasks.md             # Phase 2 出力 (/speckit.tasks コマンド)
```

### Source Code (repository root)

```text
app/
├── build.gradle                    # AGPプラグイン設定（更新対象）
├── src/
│   ├── main/
│   │   ├── java/net/ktnx/mobileledger/
│   │   └── kotlin/net/ktnx/mobileledger/
│   ├── test/                       # ユニットテスト
│   └── androidTest/                # インストルメンテーションテスト
└── schemas/                        # Room DBスキーマ

gradle/
├── wrapper/
│   └── gradle-wrapper.properties   # Gradleバージョン（更新対象）
└── libs.versions.toml              # Version Catalog（更新対象）

build.gradle                        # ルートビルド設定
flake.nix                           # Nix開発環境設定
```

**Structure Decision**: 既存のAndroidプロジェクト構造を維持。ビルド設定ファイル（gradle-wrapper.properties、libs.versions.toml、build.gradle）のみ更新。

## Complexity Tracking

> **Constitution Checkに違反がある場合のみ記入**

違反なし - このセクションは空です。
