# Quickstart: TransactionAccumulator テスト可能性向上

**Date**: 2026-01-14 | **Branch**: `012-accumulator-testability`

## 前提条件

- Nix 環境がインストール済み
- Git リポジトリがクローン済み
- `012-accumulator-testability` ブランチにチェックアウト済み

## 開発環境セットアップ

```bash
# 開発シェルに入る
nix develop .#fhs

# または通常の開発シェル
nix develop
```

## 変更対象ファイル

### 1. TransactionAccumulator.kt

**パス**: `app/src/main/kotlin/net/ktnx/mobileledger/async/TransactionAccumulator.kt`

**変更内容**:
1. コンストラクタに `currencyFormatter: CurrencyFormatter` パラメータを追加
2. `summarizeRunningTotal()` で `App.currencyFormatter()` の代わりに注入されたフォーマッターを使用

### 2. MainViewModel.kt

**パス**: `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainViewModel.kt`

**変更内容**:
1. `@Inject` で `currencyFormatter: CurrencyFormatter` を受け取る
2. TransactionAccumulator インスタンス化時にフォーマッターを渡す（2 箇所）

### 3. TransactionAccumulatorTest.kt（新規）

**パス**: `app/src/test/kotlin/net/ktnx/mobileledger/async/TransactionAccumulatorTest.kt`

**内容**: FakeCurrencyFormatter を使用したユニットテスト

## テスト実行

```bash
# 全テスト実行
nix run .#test

# 特定テストのみ実行（Nix 環境内）
./gradlew test --tests "*TransactionAccumulatorTest*"

# テストカバレッジ確認
nix run .#coverage
```

## ビルド・検証

```bash
# フルワークフロー（テスト → ビルド → インストール）
nix run .#verify

# 個別コマンド
nix run .#build    # デバッグ APK ビルド
nix run .#lint     # 静的解析
```

## 実機検証手順

1. `nix run .#verify` でアプリをインストール
2. アプリを起動
3. プロファイルを選択
4. 取引一覧タブに移動
5. アカウントフィルタを適用
6. 累計残高の通貨フォーマットが正しく表示されることを確認

## トラブルシューティング

### テストが Application コンテキストを要求する場合

**症状**: `UninitializedPropertyAccessException: lateinit property has not been initialized`

**原因**: TransactionAccumulator がまだ App.currencyFormatter() を呼び出している

**解決**: summarizeRunningTotal() で注入されたフォーマッターを使用していることを確認

### フォーマット出力が異なる場合

**症状**: テストで期待値と異なるフォーマットが返される

**原因**: FakeCurrencyFormatter のロケール設定

**解決**: FakeCurrencyFormatter は Locale.US で "#,##0.00" 形式。テスト期待値を合わせる

## 成功基準チェックリスト

- [ ] `nix run .#test` が成功
- [ ] `nix run .#build` が成功
- [ ] `nix run .#lint` が新しいエラーを生成しない
- [ ] TransactionAccumulatorTest.kt が存在し、パスする
- [ ] TransactionAccumulator.kt に `App.currencyFormatter()` への参照がない
- [ ] 実機で取引一覧の通貨表示が正しい
