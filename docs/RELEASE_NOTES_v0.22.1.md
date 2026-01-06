# MoLe v0.22.1 - Bug Fix Release

**Release Date:** 2026-01-04

## 概要

v0.22.1 は、hledger-web v1.32 以降との互換性問題を修正するバグフィックスリリースです。

## 修正内容

### JSON API互換性の修正

v0.22.0 では、hledger-web v1.32 以降へのトランザクション追加が失敗する問題がありました。
この問題を修正しました。

**修正された問題:**
- トランザクション送信時に HTTP 400/500 エラーが発生していた
- JSON形式がサーバー側の期待する形式と一致していなかった

**技術的な変更:**

1. **ptransaction_ フィールドの型変更**
   - `int` から `String` に変更
   - hledger-web v1.32+ は文字列形式を期待しています

2. **ParsedStyle の新フィールド追加**
   - `asdecimalmark` フィールドを追加（従来の `asdecimalpoint` から変更）
   - `asrounding` フィールドを追加（デフォルト値: "NoRounding"）

3. **ParsedAmount のフィールド名変更**
   - `aprice` を `acost` に変更
   - hledger-web v1.32+ の新しい命名規則に対応

**影響を受けるバージョン:**
- v0.22.0 で hledger-web v1.32 以降へのトランザクション追加が失敗

## 検証済み環境

以下のバージョンで動作確認済み:
- ✅ hledger-web v1.32.1
- ✅ hledger-web v1.40
- ✅ hledger-web v1.50

## アップグレード方法

### 既存ユーザー

v0.22.0 から v0.22.1 へのアップグレードは、APKを上書きインストールするだけで完了します。

1. APKファイルをダウンロード
2. インストール（既存のアプリが上書きされます）
3. データ移行や設定変更は不要

### 新規インストール

通常のインストール手順に従ってください。

## 後方互換性

- v0.22.1 は v0.22.0 の完全な上位互換です
- 既存のデータやプロファイル設定に影響はありません
- 古いバージョンの hledger-web (v1.14-v1.23) との互換性も維持されています

## 既知の問題

特になし

## 次のバージョンについて

次のメジャーアップデートでは、以下の機能を検討しています:
- トランザクション編集機能の改善
- パフォーマンスの最適化
- UI/UX の改善

## 関連リンク

- [GitHub リポジトリ](https://github.com/Yus314/MoLe)
- [v0.22.1 変更履歴](https://github.com/Yus314/MoLe/compare/v0.22.0...v0.22.1)
- [Issue Tracker](https://github.com/Yus314/MoLe/issues)

## 謝辞

バグレポートとテストにご協力いただいた全てのユーザーの皆様に感謝いたします。

---

## Technical Details

### Modified Files

**JSON Serialization Classes (12 files):**
- `v1_32/ParsedAmount.java`
- `v1_32/ParsedLedgerTransaction.java`
- `v1_32/ParsedPosting.java`
- `v1_32/ParsedStyle.java`
- `v1_40/ParsedAmount.java`
- `v1_40/ParsedLedgerTransaction.java`
- `v1_40/ParsedPosting.java`
- `v1_40/ParsedStyle.java`
- `v1_50/ParsedAmount.java`
- `v1_50/ParsedLedgerTransaction.java`
- `v1_50/ParsedPosting.java`
- `v1_50/ParsedStyle.java`

**Build Configuration:**
- `app/build.gradle` - Version bump to 0.22.1

**Test Infrastructure:**
- `docker-compose.test.yml` - Test environment for multiple hledger-web versions

### Commits

- `1d3f967a` - Fix JSON API compatibility with hledger-web v1.32-v1.50
- `4f250c6b` - Bump version to 0.22.1

### JSON Format Changes

**Before (v0.22.0):**
```json
{
  "ptransaction_": 1,
  "pamount": [{
    "aprice": null,
    "astyle": {
      "asdecimalpoint": "."
    }
  }]
}
```

**After (v0.22.1):**
```json
{
  "ptransaction_": "1",
  "pamount": [{
    "acost": null,
    "astyle": {
      "asdecimalmark": ".",
      "asrounding": "NoRounding"
    }
  }]
}
```
