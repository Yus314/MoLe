# MoLe v0.22.0 リリースノート

**リリース日**: 2026-01-03
**バージョン**: 0.22.0 (versionCode: 58)

---

## 新機能

### 🎉 hledger-web v1.32-v1.50 サポート

MoLeは最新のhledger-webバージョンに対応しました：

| hledger-web バージョン | サポート状況 | 主な特徴 |
|---------------------|------------|---------|
| **v1.14 - v1.23** | ✅ サポート継続 | レガシーバージョン（後方互換性） |
| **v1.32 - v1.39** | ✅ 新規サポート | アカウント宣言情報（account declaration info）対応 |
| **v1.40 - v1.49** | ✅ 新規サポート | 改善されたbase-url処理 |
| **v1.50 - v1.51** | ✅ 新規サポート | 最新安定版 |

**重要**: 既存のv1.14-v1.23サーバーとも完全に互換性があります。

### 🔍 自動バージョン検出の改善

MoLeは接続時にhledger-webサーバーのバージョンを自動検出し、最適なJSON API バージョンを選択します。

- サーバーバージョンに応じた最適なAPI選択
- シームレスな接続体験
- 手動設定不要

---

## 改善点

### パフォーマンスと安定性

- **より堅牢なエラーハンドリング**: トランザクション取得時のクラッシュを修正
- **Null安全性の向上**: アカウント残高表示時のNullPointerExceptionを修正
- **互換性の向上**: Android 13 (API 33) での非推奨API警告を解消

### 開発者向け

- **Nix Flakes サポート**: 完全に再現可能なビルド環境
  ```bash
  nix run .#build  # ワンコマンドビルド
  ```
- **ビルド高速化**: 増分ビルド時間を約2秒に短縮
- **包括的なテスト環境**: 4バージョンのhledger-webを同時テスト可能

---

## バグ修正

### 重大なバグ修正

- **トランザクション取得エラー**: v1.32/v1.40/v1.50でトランザクション一覧が取得できなかった問題を修正
  - `TransactionListParser` に新しいAPIバージョンのサポートを追加

- **残高表示のクラッシュ**: アカウント残高表示時のNullPointerExceptionを修正
  - `LedgerAccount.toDBOWithAmounts()` に null チェックを追加
  - 全APIバージョン（v1_14-v1_50）の `ParsedLedgerAccount.getSimpleBalance()` に防御的プログラミングを適用

### その他の修正

- Android 13以降での非推奨API使用警告を解消（`PackageManager.getPackageInfo()`）

---

## アップグレード方法

### 既存ユーザー

1. **データのバックアップ**（推奨）
   - MoLeのデータはデバイス内に保存されています
   - アンインストールしてもデータは保持されますが、念のためバックアップを推奨します

2. **新しいAPKをインストール**
   ```bash
   adb install -r MoLe-v0.22.0-release.apk
   ```
   または、APKファイルをデバイスに転送して手動インストール

3. **アプリを起動**
   - 既存のプロファイルは自動的に移行されます
   - サーバー接続を確認してください

### 新規ユーザー

1. APKをダウンロード
2. デバイスにインストール
3. プロファイルを作成してhledger-webサーバーに接続

---

## 互換性

### Android要件

- **最小**: Android 5.1 (API 22)
- **推奨**: Android 9.0 (API 28) 以上
- **最新テスト済み**: Android 14 (API 34)

### hledger-web要件

- **対応バージョン**: v1.14 - v1.51
- **推奨バージョン**: v1.50 以上（最新機能を利用可能）
- **最小バージョン**: v1.14（基本機能のみ）

---

## 既知の問題と制限事項

### 現在既知の問題

現時点では重大な既知の問題はありません。

### 制限事項

- **オフラインモード**: MoLeはhledger-webサーバーとの接続が必要です（オフライン閲覧は未対応）
- **編集機能**: 既存トランザクションの編集は未対応（削除と再作成が必要）
- **複数通貨**: 複数通貨の表示に一部制限があります

問題を発見した場合は、[GitHub Issues](https://github.com/[username]/MoLe/issues)で報告してください。

---

## テスト環境

本リリースは以下の環境でテストされています：

### hledger-webバージョン

- ✅ hledger-web 1.23（後方互換性テスト）
- ✅ hledger-web 1.32.1（adeclarationinfo テスト）
- ✅ hledger-web 1.40（中間バージョンテスト）
- ✅ hledger-web 1.50（最新版テスト）

### Androidデバイス

- ✅ Android 9 (API 28)
- ✅ Android 12 (API 31)
- ✅ Android 13 (API 33)

---

## ダウンロード

### APK

- **リリースAPK**: [MoLe-v0.22.0-release.apk](https://github.com/[username]/MoLe/releases/download/v0.22.0/MoLe-v0.22.0-release.apk)
- **SHA256**: [MoLe-v0.22.0-release.apk.sha256](https://github.com/[username]/MoLe/releases/download/v0.22.0/MoLe-v0.22.0-release.apk.sha256)

### ソースコード

- **Git Tag**: [v0.22.0](https://github.com/[username]/MoLe/releases/tag/v0.22.0)
- **Tarball**: [v0.22.0.tar.gz](https://github.com/[username]/MoLe/archive/refs/tags/v0.22.0.tar.gz)
- **ZIP**: [v0.22.0.zip](https://github.com/[username]/MoLe/archive/refs/tags/v0.22.0.zip)

### インストール前の確認

SHA256チェックサムを確認してください：

```bash
sha256sum -c MoLe-v0.22.0-release.apk.sha256
```

---

## ビルド方法（開発者向け）

### 推奨: Nix Flakes

```bash
git clone https://github.com/[username]/MoLe.git
cd MoLe
git checkout v0.22.0

# ワンコマンドビルド
nix run .#build
```

### 標準: Gradle

```bash
git clone https://github.com/[username]/MoLe.git
cd MoLe
git checkout v0.22.0

# デバッグビルド
./gradlew assembleDebug

# リリースビルド（署名鍵が必要）
./gradlew assembleRelease
```

詳細は[ビルドガイド](docs/BUILDING.md)を参照してください。

---

## 貢献者

このリリースに貢献してくださった皆様に感謝します。

- バグ報告、機能リクエスト
- コードレビュー
- ドキュメント改善
- テスト協力

---

## サポート

### ドキュメント

- [README.md](README.md) - プロジェクト概要
- [BUILDING.md](docs/BUILDING.md) - ビルド手順
- [TESTING_GUIDE.md](docs/TESTING_GUIDE.md) - テスト手順
- [CHANGES.md](CHANGES.md) - 変更履歴

### コミュニティ

- **GitHub Issues**: バグ報告、機能リクエスト
- **GitHub Discussions**: 質問、議論

---

## 次のステップ

インストール後：

1. ✅ プロファイルを作成
2. ✅ hledger-webサーバーに接続
3. ✅ アカウント一覧を確認
4. ✅ トランザクションを追加

詳細な使用方法は[ユーザーガイド](docs/USER_GUIDE.md)（準備中）を参照してください。

---

**MoLe v0.22.0をお楽しみください！**

フィードバックやバグ報告は[GitHub Issues](https://github.com/[username]/MoLe/issues)までお願いします。
