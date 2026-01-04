# MoLe v0.22.0 - 実機テストガイド

**対象バージョン**: v0.22.0
**最終更新**: 2026-01-03

---

## 目次

1. [概要](#概要)
2. [テスト環境の準備](#テスト環境の準備)
3. [APKのインストール](#apkのインストール)
4. [hledger-webサーバーの準備](#hledger-webサーバーの準備)
5. [基本機能テスト](#基本機能テスト)
6. [新規実装のテスト](#新規実装のテスト)
7. [トラブルシューティング](#トラブルシューティング)
8. [テスト結果の記録](#テスト結果の記録)

---

## 概要

このガイドでは、MoLe v0.22.0の実機テストを実施する手順を説明します。

### テストの目的

1. **基本機能の動作確認**: 既存機能が正常に動作することを確認
2. **新規実装の検証**: v1.32, v1.40, v1.50サポートの動作確認
3. **後方互換性の確認**: v1.14-v1.23サーバーでも動作することを確認
4. **ユーザー体験の評価**: UI/UXに問題がないか確認

### テストの種類

| テスト種類 | 所要時間 | 難易度 |
|-----------|---------|--------|
| エミュレーターテスト | 30分 | ⭐ 簡単 |
| 実機テスト | 20分 | ⭐⭐ 中 |
| 統合テスト（hledger-web） | 1時間 | ⭐⭐⭐ 高 |

---

## テスト環境の準備

### オプション1: Androidエミュレーター（推奨：初心者向け）

#### ステップ1: Android Studioのインストール

1. [Android Studio](https://developer.android.com/studio)をダウンロード
2. インストールウィザードに従ってインストール
3. 初回起動時にSDKをダウンロード

#### ステップ2: エミュレーターの作成

1. Android Studioを起動
2. `Tools` → `Device Manager` を選択
3. `Create Device` をクリック

**推奨設定**:
- **デバイス**: Pixel 6 Pro
- **システムイメージ**: Android 13 (API level 33) または 14 (API level 34)
- **RAM**: 2048 MB以上
- **内部ストレージ**: 2048 MB以上

#### ステップ3: エミュレーターの起動

1. Device Managerでデバイスを選択
2. ▶️ ボタンをクリックして起動

**起動確認**:
```bash
# adbコマンドでデバイスを確認
adb devices
```

出力例:
```
List of devices attached
emulator-5554   device
```

### オプション2: 実機（推奨：実際のユーザー体験）

#### ステップ1: 開発者オプションの有効化

**Android 8.0以降**:
1. `設定` → `デバイス情報` を開く
2. `ビルド番号` を7回タップ
3. 「開発者になりました」と表示される

#### ステップ2: USBデバッグの有効化

1. `設定` → `システム` → `開発者向けオプション` を開く
2. `USBデバッグ` を有効化
3. 警告を読んで `OK` をタップ

#### ステップ3: デバイスの接続

1. USBケーブルでPCと実機を接続
2. 実機に「USBデバッグを許可しますか？」と表示されたら `許可` をタップ

**接続確認**:
```bash
adb devices
```

出力例:
```
List of devices attached
ABC123456789    device
```

### オプション3: ワイヤレスデバッグ（Android 11以降）

#### ステップ1: ワイヤレスデバッグの有効化

1. `設定` → `開発者向けオプション` を開く
2. `ワイヤレスデバッグ` を有効化

#### ステップ2: ペアリング

1. `ペアリングコードによるデバイスのペアリング` をタップ
2. PCで以下を実行:
```bash
adb pair <IP>:<PORT>
# ペアリングコードを入力
```

#### ステップ3: 接続

```bash
adb connect <IP>:5555
adb devices
```

---

## APKのインストール

### 方法1: adbコマンドを使用（推奨）

#### ステップ1: APKの場所を確認

```bash
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

#### ステップ2: デバイスにインストール

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

**成功時の出力**:
```
Performing Streamed Install
Success
```

#### エラー時の対処

**エラー**: `INSTALL_FAILED_UPDATE_INCOMPATIBLE`

**原因**: 既に別の署名のアプリがインストールされている

**解決策**:
```bash
# 既存アプリをアンインストール
adb uninstall net.ktnx.mobileledger.debug

# 再度インストール
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 方法2: 実機に直接転送

#### ステップ1: APKをデバイスに転送

```bash
adb push app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/
```

#### ステップ2: デバイスでインストール

1. ファイルマネージャーアプリを開く
2. `Download` フォルダを開く
3. `app-debug.apk` をタップ
4. 「この提供元を許可」を有効化（初回のみ）
5. `インストール` をタップ

### 方法3: エミュレーターにドラッグ&ドロップ

1. エミュレーターを起動
2. APKファイルをエミュレーター画面にドラッグ
3. 自動的にインストールが開始

### インストールの確認

```bash
# インストール済みアプリを確認
adb shell pm list packages | grep mobileledger
```

出力例:
```
package:net.ktnx.mobileledger.debug
```

**または実機で確認**:
- アプリドロワーに「MoLe」アイコンが表示される
- アプリ情報でバージョン「0.22.0-debug」を確認

---

## hledger-webサーバーの準備

実際のhledger-webサーバーでテストするための環境を準備します。

### オプション1: Dockerで複数バージョンを起動（推奨）

#### ステップ1: テストデータの準備

```bash
mkdir -p ~/hledger-test-data
cd ~/hledger-test-data

cat > test.journal <<EOF
; テスト用仕訳データ

account assets:bank:checking
account expenses:food
account expenses:transport
account income:salary

2024-01-01 * Opening Balance
    assets:bank:checking    1000.00 USD
    equity:opening balances

2024-01-05 * Grocery Shopping
    expenses:food    50.00 USD
    assets:bank:checking

2024-01-10 * Subway Ticket
    expenses:transport    2.50 USD
    assets:bank:checking

2024-01-15 * Monthly Salary
    income:salary    -3000.00 USD
    assets:bank:checking

2024-01-20 * Restaurant
    expenses:food    75.00 USD
    assets:bank:checking
EOF
```

#### ステップ2: docker-compose.ymlの作成

```bash
cat > docker-compose.test.yml <<EOF
version: '3.8'

services:
  # hledger-web v1.32 (最初に追加したバージョン)
  hledger-web-1-32:
    image: dastapov/hledger:1.32
    ports:
      - "5032:5000"
    volumes:
      - ./test.journal:/data/test.journal:ro
    command: hledger-web --serve --port 5000 --file /data/test.journal --base-url http://localhost:5032
    restart: unless-stopped

  # hledger-web v1.40 (中間バージョン)
  hledger-web-1-40:
    image: dastapov/hledger:1.40
    ports:
      - "5040:5000"
    volumes:
      - ./test.journal:/data/test.journal:ro
    command: hledger-web --serve --port 5000 --file /data/test.journal --base-url http://localhost:5040
    restart: unless-stopped

  # hledger-web v1.50 (最新バージョン)
  hledger-web-1-50:
    image: dastapov/hledger:1.50
    ports:
      - "5050:5000"
    volumes:
      - ./test.journal:/data/test.journal:ro
    command: hledger-web --serve --port 5000 --file /data/test.journal --base-url http://localhost:5050
    restart: unless-stopped

  # hledger-web v1.23 (後方互換性テスト用)
  hledger-web-1-23:
    image: dastapov/hledger:1.23
    ports:
      - "5023:5000"
    volumes:
      - ./test.journal:/data/test.journal:ro
    command: hledger-web --serve --port 5000 --file /data/test.journal
    restart: unless-stopped
EOF
```

#### ステップ3: サーバーの起動

```bash
docker-compose -f docker-compose.test.yml up -d
```

#### ステップ4: サーバーの確認

```bash
# 全サーバーの状態確認
docker-compose -f docker-compose.test.yml ps

# バージョンの確認
curl http://localhost:5032/version  # v1.32
curl http://localhost:5040/version  # v1.40
curl http://localhost:5050/version  # v1.50
curl http://localhost:5023/version  # v1.23
```

**成功時の出力例**:
```
1.32.3
1.40.1
1.50.0
1.23.0
```

### オプション2: ローカルでhledgerをインストール

#### Linux/macOS

```bash
# Stackを使用してインストール
curl -sSL https://get.haskellstack.org/ | sh
stack install hledger-web-1.32
hledger-web --serve --port 5032 --file ~/test.journal
```

#### NixOS

```bash
nix-shell -p hledger-web
hledger-web --serve --port 5000 --file ~/test.journal
```

### デバイスからのアクセス設定

#### PCのIPアドレスを確認

```bash
# Linux/macOS
ip addr show | grep "inet " | grep -v 127.0.0.1

# または
ifconfig | grep "inet " | grep -v 127.0.0.1
```

出力例:
```
inet 192.168.1.100/24
```

#### ファイアウォールの設定

**Linux (ufw)**:
```bash
sudo ufw allow 5032/tcp
sudo ufw allow 5040/tcp
sudo ufw allow 5050/tcp
sudo ufw allow 5023/tcp
```

**macOS**:
```bash
# システム環境設定 → セキュリティとプライバシー → ファイアウォール
# で該当ポートを許可
```

#### 接続テスト

実機のブラウザで以下にアクセス:
```
http://192.168.1.100:5032
http://192.168.1.100:5040
http://192.168.1.100:5050
http://192.168.1.100:5023
```

---

## 基本機能テスト

### テストチェックリスト

#### 1. アプリの起動 ✅

**手順**:
1. MoLeアプリを起動
2. スプラッシュ画面が表示される
3. メイン画面が表示される

**確認事項**:
- ✅ クラッシュせずに起動する
- ✅ バージョン情報が表示される（メニューから確認）
- ✅ UI要素が正しく表示される

#### 2. プロファイルの作成 ✅

**手順**:
1. メイン画面で `+` ボタンをタップ
2. 「新しいプロファイル」を選択
3. 以下の情報を入力:
   - **プロファイル名**: Test v1.32
   - **URL**: `http://192.168.1.100:5032` (PCのIPに置き換え)
4. 「保存」をタップ

**確認事項**:
- ✅ プロファイルが作成される
- ✅ バージョン検出が開始される（「Detecting version...」表示）
- ✅ 検出完了後、APIバージョンが表示される

**期待される結果**:
- API version: `1.32` または `v1_32`

#### 3. アカウント一覧の表示 ✅

**手順**:
1. 作成したプロファイルを選択
2. アカウント一覧画面が表示されるまで待機

**確認事項**:
- ✅ アカウントが表示される:
  - `assets:bank:checking`
  - `expenses:food`
  - `expenses:transport`
  - `income:salary`
- ✅ 残高が正しく表示される
- ✅ 階層構造が正しく表示される

**期待される残高** (test.journalの場合):
- `assets:bank:checking`: $3,872.50
- `expenses:food`: $125.00
- `expenses:transport`: $2.50
- `income:salary`: -$3,000.00

#### 4. トランザクション一覧の表示 ✅

**手順**:
1. アカウント（例: `assets:bank:checking`）をタップ
2. トランザクション一覧が表示される

**確認事項**:
- ✅ トランザクションが表示される（5件）
- ✅ 日付が正しい
- ✅ 説明文が正しい
- ✅ 金額が正しい
- ✅ スクロールがスムーズ

#### 5. トランザクションの追加 ✅

**手順**:
1. トランザクション一覧画面で `+` ボタンをタップ
2. 新規トランザクション画面で以下を入力:
   - **日付**: 今日の日付
   - **説明**: "Test Transaction"
   - **アカウント1**: `expenses:food`
   - **金額1**: `10.00`
   - **通貨**: `USD`
   - **アカウント2**: `assets:bank:checking` (自動入力されるはず)
3. 「保存」をタップ

**確認事項**:
- ✅ トランザクションが保存される（エラーなし）
- ✅ 一覧に新しいトランザクションが表示される
- ✅ 残高が更新される

**期待される変化**:
- `assets:bank:checking`: $3,872.50 → $3,862.50 (-$10.00)
- `expenses:food`: $125.00 → $135.00 (+$10.00)

#### 6. データの更新 ✅

**手順**:
1. メイン画面に戻る
2. 下にスワイプして更新（Pull to refresh）

**確認事項**:
- ✅ 更新アニメーションが表示される
- ✅ データが再読み込みされる
- ✅ 最新のデータが表示される

---

## 新規実装のテスト

### テスト1: バージョン検出の確認

各バージョンのサーバーで正しいAPIが選択されることを確認します。

#### v1.32サーバー

**手順**:
1. 新しいプロファイルを作成
   - 名前: "Test v1.32"
   - URL: `http://192.168.1.100:5032`
2. プロファイル詳細を確認

**期待される結果**:
- ✅ API version: `v1_32` または `Version 1.32`
- ✅ サーバーバージョン: `1.32.x`

**確認方法**:
1. プロファイル一覧で作成したプロファイルを長押し
2. 「編集」を選択
3. APIバージョンのフィールドを確認

#### v1.40サーバー

**手順**:
1. 新しいプロファイルを作成
   - 名前: "Test v1.40"
   - URL: `http://192.168.1.100:5040`
2. プロファイル詳細を確認

**期待される結果**:
- ✅ API version: `v1_40` または `Version 1.40`
- ✅ サーバーバージョン: `1.40.x`

#### v1.50サーバー

**手順**:
1. 新しいプロファイルを作成
   - 名前: "Test v1.50"
   - URL: `http://192.168.1.100:5050`
2. プロファイル詳細を確認

**期待される結果**:
- ✅ API version: `v1_50` または `Version 1.50`
- ✅ サーバーバージョン: `1.50.x`

### テスト2: 後方互換性の確認

#### v1.23サーバー（既存サポート）

**手順**:
1. 新しいプロファイルを作成
   - 名前: "Test v1.23 (Legacy)"
   - URL: `http://192.168.1.100:5023`
2. アカウント一覧、トランザクション追加を実施

**期待される結果**:
- ✅ API version: `v1_23` または `Version 1.23`
- ✅ 全ての機能が正常に動作
- ✅ エラーが発生しない

### テスト3: 機能の互換性確認

全てのバージョンで同じ操作を実行して、結果が一貫していることを確認します。

#### テストシナリオ

| 操作 | v1.23 | v1.32 | v1.40 | v1.50 |
|------|-------|-------|-------|-------|
| アカウント取得 | ✅ | ✅ | ✅ | ✅ |
| トランザクション取得 | ✅ | ✅ | ✅ | ✅ |
| トランザクション追加 | ✅ | ✅ | ✅ | ✅ |
| データ更新 | ✅ | ✅ | ✅ | ✅ |

**手順**:
各バージョンのプロファイルで以下を実行:
1. アカウント一覧を表示
2. トランザクション一覧を表示
3. 新しいトランザクションを追加
4. データを更新

**確認事項**:
- ✅ 全てのバージョンで同じ結果が得られる
- ✅ エラーが発生しない
- ✅ UIの応答が適切

### テスト4: adeclarationinfo フィールド（v1.32+の新機能）

v1.32以降では、アカウントに宣言情報が含まれます。

**確認方法**:

1. PCでログを確認（adb logcat）:
```bash
adb logcat | grep -i "declaration"
```

2. アプリのログレベルをDebugに設定（開発者向け）

**期待される動作**:
- v1.32, v1.40, v1.50: `adeclarationinfo` フィールドが解析される（ログに表示される可能性）
- v1.23以下: フィールドなし（エラーにならない）

**注意**: 現在のMoLe実装では、`adeclarationinfo`を取得しますが画面には表示しません。将来のバージョンで活用される可能性があります。

---

## トラブルシューティング

### アプリがクラッシュする

#### ログの確認

```bash
adb logcat | grep -E "MoLe|AndroidRuntime"
```

#### クラッシュレポートの取得

```bash
adb logcat -d > crash.log
```

#### 一般的な原因

1. **ネットワークエラー**: サーバーに到達できない
   - WiFi接続を確認
   - PCのIPアドレスを確認
   - ファイアウォール設定を確認

2. **JSONパースエラー**: サーバーレスポンスが不正
   - サーバーのバージョンを確認
   - ブラウザでJSON APIにアクセスして確認
   - 例: `http://192.168.1.100:5032/json`

3. **バージョン検出失敗**: サーバーバージョンを取得できない
   - `/version` エンドポイントが利用可能か確認
   - 例: `http://192.168.1.100:5032/version`

### サーバーに接続できない

#### PCから確認

```bash
# サーバーが起動しているか確認
curl http://localhost:5032/version

# ファイアウォールを確認
sudo ufw status
```

#### 実機から確認

実機のブラウザで以下にアクセス:
```
http://192.168.1.100:5032
```

アクセスできない場合:
- PC と実機が同じネットワークにいるか確認
- ファイアウォールを一時的に無効化して確認
- PCのIPアドレスが変わっていないか確認

### データが表示されない

#### 原因の特定

1. **サーバーにデータがあるか確認**:
```bash
curl http://localhost:5032/json | jq .
```

2. **アプリのログを確認**:
```bash
adb logcat | grep -i "json\|parse\|account"
```

3. **プロファイル設定を確認**:
- URL が正しいか
- API バージョンが正しいか

#### 対処法

1. プロファイルを削除して再作成
2. アプリデータをクリア:
```bash
adb shell pm clear net.ktnx.mobileledger.debug
```
3. アプリを再インストール

### パフォーマンスの問題

#### 動作が遅い

**原因**:
- ネットワーク遅延
- 大量のデータ
- エミュレーターのパフォーマンス

**対処法**:
1. 実機でテスト
2. ネットワーク接続を確認
3. データ量を減らす（テスト用データを使用）

---

## テスト結果の記録

### テストレポートテンプレート

```markdown
# MoLe v0.22.0 実機テストレポート

**テスト実施日**: 2026-01-03
**テスト実施者**: [名前]
**テスト環境**:
- デバイス: [デバイス名/エミュレーター]
- Androidバージョン: [例: Android 13]
- MoLeバージョン: 0.22.0-debug

## 基本機能テスト

| 機能 | 結果 | 備考 |
|------|------|------|
| アプリ起動 | ✅/❌ | |
| プロファイル作成 | ✅/❌ | |
| アカウント一覧表示 | ✅/❌ | |
| トランザクション一覧表示 | ✅/❌ | |
| トランザクション追加 | ✅/❌ | |
| データ更新 | ✅/❌ | |

## バージョン検出テスト

| hledger-webバージョン | 検出結果 | 選択されたAPI | 結果 |
|---------------------|----------|---------------|------|
| v1.23 | | | ✅/❌ |
| v1.32 | | | ✅/❌ |
| v1.40 | | | ✅/❌ |
| v1.50 | | | ✅/❌ |

## 問題・バグ

1. [問題の説明]
   - 再現手順:
   - 期待される動作:
   - 実際の動作:
   - スクリーンショット:

## 総合評価

- [ ] 全てのテストが成功
- [ ] 一部のテストが失敗（詳細は上記）
- [ ] リリース可能
- [ ] 修正が必要

## 追加コメント

[その他気づいた点など]
```

### テスト結果の保存

```bash
# テストレポートを作成
cd /home/kaki/MoLe/docs
vim DEVICE_TEST_REPORT_$(date +%Y%m%d).md
```

---

## 自動化されたテスト（オプション）

### Espressoテストの実行

```bash
# 実機/エミュレーターでUIテストを実行
./result-fhs/bin/mole-android-env -c "./gradlew connectedAndroidTest"
```

### Monkey テスト（ストレステスト）

```bash
# ランダム操作を1000回実行
adb shell monkey -p net.ktnx.mobileledger.debug -v 1000
```

---

## まとめ

### テスト完了の基準

以下の全てが満たされれば、テスト合格です：

- ✅ 基本機能テストが全て成功
- ✅ v1.32, v1.40, v1.50サーバーで正しいAPIが選択される
- ✅ v1.23サーバーでも正常に動作（後方互換性）
- ✅ トランザクションの追加・表示が正常
- ✅ クリティカルなバグが存在しない

### 次のステップ

テストが成功したら：

1. ✅ **テスト結果をドキュメント化**
2. ✅ **発見したバグを修正**（あれば）
3. ✅ **リリースビルドの作成**
4. ✅ **ベータテスト**（オプション）
5. ✅ **正式リリース**

### 参考資料

- [Android Testing Guide](https://developer.android.com/training/testing)
- [adb documentation](https://developer.android.com/studio/command-line/adb)
- [hledger-web documentation](https://hledger.org/hledger-web.html)
