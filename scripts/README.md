# MoLe テストスクリプト

このディレクトリには、MoLeアプリのテストとデプロイメントを支援するスクリプトが含まれています。

## スクリプト一覧

### test-quick-start.sh

**バージョン**: 2.0
**最終更新**: 2026-01-03

実機テスト環境を自動的にセットアップするインタラクティブスクリプト。

#### 機能

1. **環境チェック**
   - `adb` コマンドの確認
   - `docker` コマンドの確認（オプション）
   - バージョン情報の表示

2. **デバイス管理**
   - 接続されたAndroidデバイスの検出
   - デバイス情報の表示
   - 接続ガイダンス

3. **APK管理**
   - APKファイルの存在確認
   - バージョン情報の表示（aapt使用時）
   - ビルド方法の提示（APKがない場合）
     - Nix build
     - FHS環境ビルド
     - 標準ビルド

4. **テストサーバー管理**
   - 4つのhledger-webバージョンの起動（v1.23, v1.32, v1.40, v1.50）
   - サーバー起動の確認
   - バージョン検出
   - ネットワーク情報の表示

5. **アプリケーション管理**
   - 既存アプリの削除
   - APKのインストール（-r フラグで上書き）
   - インストール確認
   - アプリの起動（オプション）
     - `monkey` コマンドでLAUNCHERインテントを使用（推奨）
     - フォールバック: SplashActivityを直接起動

6. **ログ監視**
   - リアルタイムログ表示
   - エラーのカラーハイライト
   - フィルタリング（mobileledger関連のみ）

#### 使用方法

```bash
# 実行権限を付与
chmod +x scripts/test-quick-start.sh

# スクリプトを実行
./scripts/test-quick-start.sh
```

#### 対話的プロンプト

スクリプトは以下のプロンプトを表示します：

1. **サーバー起動**: `hledger-webサーバーを起動しますか？ (y/n)`
2. **APKインストール**: `APKをインストールしますか？ (y/n)`
3. **アプリ起動**: `アプリを起動しますか？ (y/n)`
4. **ログ監視**: `アプリのログを監視しますか？ (y/n)`

各ステップはスキップ可能です。

#### 前提条件

**必須**:
- Android SDK Platform-Tools（adb）
- USBデバッグが有効なAndroidデバイス
- ビルド済みのAPK（`app/build/outputs/apk/debug/app-debug.apk`）

**オプション**:
- Docker & Docker Compose（テストサーバー起動用）
- `curl`（サーバーバージョン確認用）
- `aapt`（APK情報表示用）

#### 出力例

```
=================================================
ステップ1: 環境チェック
=================================================

✅ adb コマンドが見つかりました
ℹ️  バージョン: Android Debug Bridge version 1.0.41

=================================================
ステップ2: Androidデバイスの確認
=================================================

✅ Androidデバイスが 1 台接続されています

ℹ️  接続されているデバイス:
List of devices attached
ABC123DEF456    device

=================================================
ステップ3: APKファイルの確認
=================================================

✅ APKファイルが見つかりました
ℹ️  サイズ: 7.0M
ℹ️  バージョン: 0.22.0-debug (versionCode: 57)

=================================================
ステップ4: hledger-webサーバーの起動
=================================================

hledger-webサーバーを起動しますか？ (y/n)
y
ℹ️  docker-composeでサーバーを起動しています...
✅ hledger-webサーバーを起動しました

ℹ️  サーバーのバージョンを確認しています...

✅ Port 5023: hledger-web 1.23 - v1.23 (後方互換性)
✅ Port 5032: hledger-web 1.32.1 - v1.32 (adeclarationinfo)
✅ Port 5040: hledger-web 1.40 - v1.40 (中間バージョン)
✅ Port 5050: hledger-web 1.50 - v1.50 (最新)

✅ 全てのテストサーバーが正常に動作しています (4/4)

ℹ️  PCのIPアドレス:
✅ IP: 192.168.1.100

ℹ️  実機からアクセスする場合のURL:
  v1.23: http://192.168.1.100:5023
  v1.32: http://192.168.1.100:5032
  v1.40: http://192.168.1.100:5040
  v1.50: http://192.168.1.100:5050
```

#### エラーハンドリング

スクリプトは以下のエラーを検出して適切なメッセージを表示します：

- adbコマンドが見つからない → インストール方法を提示
- デバイスが接続されていない → 接続手順を表示
- APKファイルがない → ビルド方法を提示（Nix/標準）
- サーバーが起動しない → 設定確認を促す
- APKインストールに失敗 → エラーログを表示

#### 改善点（v2.0）

1. **ビルド方法の提示**
   - Nixビルド（`nix run .#build`）を推奨方法として追加
   - FHS環境ビルドと標準ビルドの選択肢も提示

2. **サーバー監視の強化**
   - 各サーバーのバージョンとラベルを表示
   - 成功/失敗カウント（X/Y形式）
   - より詳細なエラーメッセージ

3. **APK管理の改善**
   - バージョン情報の自動表示（aapt使用時）
   - インストール後のアプリ起動オプション
   - エラーログの保存と表示

4. **ログ監視の向上**
   - カラーハイライト（FATAL=赤、Exception=黄）
   - より具体的なフィルタリング
   - ログクリア機能

5. **UX改善**
   - 絵文字を使用した分かりやすい表示
   - 詳細なステップガイダンス
   - テストサーバーURL一覧の表示

#### トラブルシューティング

**問題**: アプリ起動時に "Permission Denial: not exported" エラー

**原因**: Android 12以降では、外部から起動するActivityは `android:exported="true"` が必要です。

**解決**: スクリプトは自動的にフォールバック処理を行います：
1. `monkey` コマンドでLAUNCHERインテントを使用（推奨）
2. 失敗した場合、SplashActivityを直接起動

手動で起動する場合：
```bash
# 方法1: monkeyコマンド（推奨）
adb shell monkey -p net.ktnx.mobileledger.debug -c android.intent.category.LAUNCHER 1

# 方法2: SplashActivityを起動
adb shell am start -n net.ktnx.mobileledger.debug/.ui.activity.SplashActivity
```

---

**問題**: デバイスが認識されない

```bash
# USB接続を確認
lsusb

# adbサーバーを再起動
adb kill-server
adb start-server
adb devices
```

**問題**: サーバーが起動しない

```bash
# Dockerサービスの確認
docker ps
docker-compose -f docker-compose.test.yml logs

# ポートの確認
netstat -tlnp | grep -E "5023|5032|5040|5050"
```

**問題**: APKインストールに失敗

```bash
# デバイス容量の確認
adb shell df -h

# 既存アプリの手動削除
adb uninstall net.ktnx.mobileledger.debug

# 手動インストール
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

#### 関連ドキュメント

- [テストガイド](../docs/TESTING_GUIDE.md) - 詳細なテスト手順
- [ビルドガイド](../docs/BUILDING.md) - APKビルド方法
- [Nixビルドガイド](../docs/NIX_BUILD_GUIDE.md) - Nix Flakesでのビルド
- [テストチートシート](../docs/TESTING_CHEATSHEET.md) - コマンドリファレンス

## ライセンス

このスクリプトは MoLe プロジェクトの一部であり、GPL v3+ ライセンスの下で提供されています。
