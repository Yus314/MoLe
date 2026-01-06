# Implementation Plan: Kotlin Version Update

**Branch**: `003-kotlin-update` | **Date**: 2026-01-06 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/003-kotlin-update/spec.md`

## Summary

Kotlin バージョンを 1.9.25 から 2.0.21 へアップデートし、コンパイラ性能向上と言語機能の近代化を実現する。依存ライブラリのバージョン整合性を確保し、deprecation warning をゼロにしつつ、アプリの外部挙動は一切変更しない。

## Technical Context

**Language/Version**: Kotlin 1.9.25 → 2.0.21, Java 8 互換 (JVM target 1.8)
**Primary Dependencies**:
- AGP 8.7.3
- Coroutines 1.7.3
- Room 2.4.2 (kapt)
- Lifecycle 2.4.1
- Navigation 2.4.2
- Jackson 2.17.1
**Storage**: Room Database (SQLite)
**Testing**: JUnit 4.13.2, Espresso, nix flake commands (`nix run .#test`, `nix run .#verify`)
**Target Platform**: Android (minSdk 22, targetSdk 34)
**Project Type**: Mobile (Android single-module app)
**Performance Goals**: 既存パフォーマンス維持（UI操作 100ms以内）
**Constraints**: 動作の不変性、kapt 継続使用（ksp 移行はスコープ外）
**Scale/Scope**: 240 Kotlin ファイル、6 テストファイル

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. コードの可読性とメンテナンス性 | ✅ PASS | 新言語機能で可読性向上を目指す |
| II. 単体テスト | ✅ PASS | 既存テストを維持、リグレッション検出に使用 |
| III. 最小構築・段階的開発 | ✅ PASS | バージョン更新 → ビルド確認 → 段階的リファクタリング |
| IV. パフォーマンス最適化 | ✅ PASS | スコープ外、既存パフォーマンス維持のみ |
| V. アクセシビリティ | ✅ PASS | UI変更なし、影響なし |
| VI. Kotlin移行 | ✅ PASS | Kotlin 最新化で原則に沿う |
| VII. Nix開発環境 | ✅ PASS | nix flake コマンドで検証 |

**Gate Status**: ✅ ALL PASS - Phase 0 に進行可能

## Project Structure

### Documentation (this feature)

```text
specs/003-kotlin-update/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output (N/A for this feature)
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
app/
├── src/
│   ├── main/
│   │   ├── kotlin/net/ktnx/mobileledger/   # 240 Kotlin files
│   │   └── res/                            # Android resources (unchanged)
│   ├── test/kotlin/                        # 6 unit test files
│   └── androidTest/kotlin/                 # Instrumentation tests
├── build.gradle                            # Kotlin plugin configuration
└── schemas/                                # Room database schemas

gradle/
└── libs.versions.toml                      # Version Catalog (primary change target)
```

**Structure Decision**: 既存の Android single-module 構造を維持。変更対象は主に `libs.versions.toml` と `app/build.gradle`、および 240 の Kotlin ソースファイル（deprecation 修正・新機能適用時のみ）。

## Complexity Tracking

> **No Constitution violations - section not required**

N/A - すべての Constitution Check にパスしているため、正当化が必要な違反はない。
