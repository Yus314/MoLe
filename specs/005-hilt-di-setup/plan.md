# 実装計画: Hilt 依存性注入セットアップ

**ブランチ**: `005-hilt-di-setup` | **日付**: 2026-01-06 | **仕様書**: [spec.md](./spec.md)
**入力**: `/specs/005-hilt-di-setup/spec.md` からの機能仕様書

## 概要

Hiltを使用した依存性注入（DI）をMoLeアプリケーションに導入し、テスト駆動開発（TDD）を可能にする。MainModelをパイロットとして段階的に移行し、ViewModelのユニットテストでモック依存関係を使用できるようにする。既存のシングルトン（DB、Data）はラップして移行リスクを最小化する。

## 技術コンテキスト

**言語/バージョン**: Kotlin 2.0.21 / Java 8互換 (JVM target 1.8)
**主要依存関係**: AndroidX Lifecycle 2.4.1, Room 2.4.2, Navigation 2.4.2, KSP 2.0.21-1.0.26
**ストレージ**: Room Database (SQLite) - `DB.kt` シングルトン経由
**テスト**: JUnit 4.13.2, AndroidX Test Runner, Espresso
**ターゲットプラットフォーム**: Android (minSdk 22, targetSdk 34)
**プロジェクトタイプ**: モバイル (Android単一モジュール)
**パフォーマンス目標**: UI操作 100ms以内、アプリ起動 2秒以下（憲章基準）
**制約**: 既存機能の動作維持、段階的移行、グローバルシングルトンのラップ
**スケール/スコープ**: 5つのViewModel移行対象（パイロット: MainModel）

## 憲章チェック

*ゲート: Phase 0リサーチ前に合格必須。Phase 1設計後に再チェック。*

| 原則 | ステータス | 備考 |
|------|-----------|------|
| I. コードの可読性とメンテナンス性 | ✅ 合格 | DI導入により依存関係が明示化され可読性向上 |
| II. 単体テスト | ✅ 合格 | 本機能の主目的がテスト容易性向上 |
| III. 最小構築・段階的開発 | ✅ 合格 | MainModelパイロット → 残りVM段階移行 |
| IV. パフォーマンス最適化 | ✅ 合格 | Hiltはコンパイル時DI、実行時オーバーヘッド最小 |
| V. アクセシビリティ | N/A | UI変更なし |
| VI. Kotlin移行 | ✅ 合格 | すべてのDIコードはKotlinで記述 |
| VII. Nix開発環境 | ✅ 合格 | 既存Nix環境で動作確認必須 |

**ゲート結果**: ✅ すべて合格 - Phase 0に進行可能

## プロジェクト構造

### ドキュメント（本機能）

```text
specs/005-hilt-di-setup/
├── plan.md              # 本ファイル
├── spec.md              # 機能仕様書
├── research.md          # Phase 0出力
├── data-model.md        # Phase 1出力
├── quickstart.md        # Phase 1出力
├── contracts/           # Phase 1出力（本機能ではN/A）
└── tasks.md             # Phase 2出力（/speckit.tasksで生成）
```

### ソースコード（リポジトリルート）

```text
app/
├── src/
│   ├── main/
│   │   ├── java/net/ktnx/mobileledger/     # 既存Javaファイル
│   │   └── kotlin/net/ktnx/mobileledger/   # Kotlinファイル
│   │       ├── App.kt                       # @HiltAndroidApp追加
│   │       ├── di/                          # 新規: DIモジュール
│   │       │   ├── DatabaseModule.kt        # DB提供モジュール
│   │       │   └── DataModule.kt            # Data提供モジュール
│   │       ├── db/
│   │       │   └── DB.kt                    # 既存（シングルトン維持、モジュールから参照）
│   │       ├── model/
│   │       │   └── Data.kt                  # 既存（シングルトン維持、モジュールから参照）
│   │       └── ui/
│   │           ├── MainModel.kt             # @HiltViewModel追加
│   │           └── activity/
│   │               └── MainActivity.kt      # @AndroidEntryPoint追加
│   ├── test/
│   │   └── kotlin/net/ktnx/mobileledger/
│   │       └── ui/
│   │           └── MainModelTest.kt         # 新規: サンプルユニットテスト
│   └── androidTest/
│       └── kotlin/net/ktnx/mobileledger/
│           └── HiltTestRunner.kt            # 新規: Hiltテストランナー
└── build.gradle                             # Hilt依存関係追加
```

**構造決定**: 既存のAndroid単一モジュール構造を維持。新規`di/`ディレクトリにHiltモジュールを配置。

## 複雑性トラッキング

> 憲章チェックに違反がある場合のみ記入

該当なし - すべての憲章原則に準拠

## Phase 0: リサーチタスク

### 調査項目

1. **Hilt + Kotlin 2.0 + KSP互換性**: Hiltの最新バージョンとKotlin 2.0.21/KSP 2.0.21-1.0.26との互換性確認
2. **Hilt + Room統合パターン**: Room DAOをHilt経由で提供するベストプラクティス
3. **Hilt + ViewModelテスト**: HiltでのViewModelユニットテストセットアップ
4. **既存シングルトンラップ戦略**: DB/Dataシングルトンをモジュールから提供する方法

## Phase 1: 設計成果物

- `research.md`: 調査結果と決定事項
- `data-model.md`: DIコンポーネント/モジュール構造
- `quickstart.md`: 開発者向けクイックスタートガイド
- `contracts/`: N/A（API契約なし - 内部DIセットアップ）
