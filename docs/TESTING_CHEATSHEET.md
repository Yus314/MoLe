# MoLe 実機テスト チートシート

このドキュメントは、実機テストでよく使うコマンドやTipsをまとめたクイックリファレンスです。

---

## 📱 デバイス管理

### デバイスの接続確認
```bash
# 接続されているデバイス一覧
adb devices

# 期待される出力:
# List of devices attached
# ABC123456789    device
```

### デバイス情報の取得
```bash
# Androidバージョン
adb shell getprop ro.build.version.release

# APIレベル
adb shell getprop ro.build.version.sdk

# デバイス名
adb shell getprop ro.product.model

# メーカー
adb shell getprop ro.product.manufacturer
```

### ワイヤレスデバッグ（Android 11+）
```bash
# ペアリング
adb pair <IP>:<PORT>

# 接続
adb connect <IP>:5555

# 切断
adb disconnect
```

---

## 📦 APK管理

### インストール
```bash
# 基本インストール
adb install app/build/outputs/apk/debug/app-debug.apk

# 既存アプリを上書き
adb install -r app/build/outputs/apk/debug/app-debug.apk

# ダウングレードを許可
adb install -d app/build/outputs/apk/debug/app-debug.apk
```

### アンインストール
```bash
# アプリのアンインストール
adb uninstall net.ktnx.mobileledger.debug

# データを保持してアンインストール
adb uninstall -k net.ktnx.mobileledger.debug
```

### インストール確認
```bash
# パッケージ一覧
adb shell pm list packages | grep mobileledger

# アプリ情報
adb shell dumpsys package net.ktnx.mobileledger.debug | grep version

# APKのパス
adb shell pm path net.ktnx.mobileledger.debug
```

---

## 🔍 ログ監視

### リアルタイムログ
```bash
# 全ログ
adb logcat

# MoLe関連のみ
adb logcat | grep -i mole

# エラーのみ
adb logcat *:E

# 特定タグ
adb logcat -s "MoLe"

# クラッシュログ
adb logcat | grep -E "AndroidRuntime|FATAL"
```

### ログの保存
```bash
# ファイルに保存
adb logcat > mole-test-log.txt

# 既存ログをダンプ
adb logcat -d > mole-crash-log.txt

# ログをクリア
adb logcat -c
```

### フィルタリング
```bash
# JSON関連
adb logcat | grep -i json

# ネットワーク関連
adb logcat | grep -iE "http|network|connection"

# パース関連
adb logcat | grep -iE "parse|account|transaction"
```

---

## 🗄️ アプリデータ管理

### データのクリア
```bash
# アプリデータを完全削除
adb shell pm clear net.ktnx.mobileledger.debug
```

### データの取得
```bash
# データディレクトリ一覧
adb shell run-as net.ktnx.mobileledger.debug ls /data/data/net.ktnx.mobileledger.debug

# SQLiteデータベース
adb shell run-as net.ktnx.mobileledger.debug cat /data/data/net.ktnx.mobileledger.debug/databases/[DB_NAME]

# SharedPreferences
adb shell run-as net.ktnx.mobileledger.debug cat /data/data/net.ktnx.mobileledger.debug/shared_prefs/[PREF_NAME].xml
```

---

## 🌐 ネットワーク

### PCのIPアドレス確認
```bash
# Linux/macOS
ip addr show | grep "inet " | grep -v 127.0.0.1

# または
ifconfig | grep "inet " | grep -v 127.0.0.1

# macOS簡易版
ipconfig getifaddr en0
```

### 接続テスト
```bash
# PCからサーバーに接続
curl http://localhost:5032/version

# 実機から接続（adb shell経由）
adb shell curl http://192.168.1.100:5032/version
```

### ポートフォワーディング
```bash
# PCのポートを実機にフォワード
adb forward tcp:5032 tcp:5032

# フォワーディング一覧
adb forward --list

# フォワーディング削除
adb forward --remove-all
```

---

## 🐳 Docker（hledger-webサーバー）

### サーバーの起動
```bash
# 全サーバー起動
docker-compose -f docker-compose.test.yml up -d

# 特定のバージョンのみ
docker-compose -f docker-compose.test.yml up -d hledger-web-1-32

# フォアグラウンドで起動（ログ表示）
docker-compose -f docker-compose.test.yml up
```

### サーバーの確認
```bash
# ステータス確認
docker-compose -f docker-compose.test.yml ps

# ログ確認
docker-compose -f docker-compose.test.yml logs -f

# 特定サービスのログ
docker-compose -f docker-compose.test.yml logs -f hledger-web-1-32
```

### サーバーの停止
```bash
# 停止
docker-compose -f docker-compose.test.yml stop

# 停止して削除
docker-compose -f docker-compose.test.yml down

# ボリュームも削除
docker-compose -f docker-compose.test.yml down -v
```

### バージョン確認
```bash
# 各サーバーのバージョン
curl http://localhost:5023/version  # v1.23
curl http://localhost:5032/version  # v1.32
curl http://localhost:5040/version  # v1.40
curl http://localhost:5050/version  # v1.50
```

### JSON API確認
```bash
# アカウント一覧
curl http://localhost:5032/json | jq .

# 整形して表示
curl http://localhost:5032/json | jq '.accounts[] | {name: .aname, balance: .aibalance}'
```

---

## 📊 パフォーマンス

### メモリ使用量
```bash
# 基本情報
adb shell dumpsys meminfo net.ktnx.mobileledger.debug

# サマリーのみ
adb shell dumpsys meminfo net.ktnx.mobileledger.debug | grep "TOTAL"

# PSS（実際のメモリ使用量）
adb shell dumpsys meminfo net.ktnx.mobileledger.debug | grep "TOTAL PSS"
```

### CPU使用率
```bash
# プロセス一覧
adb shell top -n 1 | grep mobileledger

# 詳細
adb shell top -p $(adb shell pidof net.ktnx.mobileledger.debug)
```

### バッテリー消費
```bash
# バッテリー統計
adb shell dumpsys batterystats net.ktnx.mobileledger.debug

# バッテリーレベル
adb shell dumpsys battery | grep level
```

---

## 📸 スクリーンショット・録画

### スクリーンショット
```bash
# スクリーンショットを撮影
adb shell screencap /sdcard/screenshot.png

# PCに転送
adb pull /sdcard/screenshot.png ./screenshot-$(date +%Y%m%d-%H%M%S).png

# ワンライナー
adb shell screencap /sdcard/screenshot.png && adb pull /sdcard/screenshot.png ./screenshot.png
```

### 画面録画
```bash
# 録画開始（最大3分）
adb shell screenrecord /sdcard/test-recording.mp4

# 時間指定（例: 30秒）
adb shell screenrecord --time-limit 30 /sdcard/test-recording.mp4

# 録画を終了: Ctrl+C

# PCに転送
adb pull /sdcard/test-recording.mp4 ./test-recording.mp4
```

---

## 🔧 デバッグ

### アプリの起動
```bash
# アプリを起動
adb shell am start -n net.ktnx.mobileledger.debug/.ui.activity.MainActivity

# 特定のActivityを起動
adb shell am start -n net.ktnx.mobileledger.debug/.ui.activity.[ActivityName]
```

### アプリの強制停止
```bash
adb shell am force-stop net.ktnx.mobileledger.debug
```

### データベースの確認
```bash
# SQLiteを起動
adb shell "run-as net.ktnx.mobileledger.debug sqlite3 /data/data/net.ktnx.mobileledger.debug/databases/[DB_NAME]"

# テーブル一覧
adb shell "run-as net.ktnx.mobileledger.debug sqlite3 /data/data/net.ktnx.mobileledger.debug/databases/[DB_NAME] '.tables'"
```

### ファイルシステム
```bash
# アプリディレクトリの内容
adb shell ls -la /data/data/net.ktnx.mobileledger.debug/

# ファイルを取得
adb shell run-as net.ktnx.mobileledger.debug cat /data/data/net.ktnx.mobileledger.debug/files/[filename] > local-file.txt
```

---

## 🧪 テストシナリオ

### 基本機能テスト
```bash
# 1. APKインストール
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 2. ログ監視開始
adb logcat -c && adb logcat | grep -i mole &

# 3. アプリ起動
adb shell am start -n net.ktnx.mobileledger.debug/.ui.activity.MainActivity

# 4. スクリーンショット撮影
adb shell screencap /sdcard/test-start.png && adb pull /sdcard/test-start.png

# 5. テスト実施（手動操作）

# 6. ログ保存
adb logcat -d > test-log-$(date +%Y%m%d-%H%M%S).txt
```

### クラッシュ時のデバッグ
```bash
# クラッシュログを取得
adb logcat -d | grep -A 50 "FATAL EXCEPTION" > crash-log.txt

# スタックトレースを抽出
adb logcat -d | grep -E "AndroidRuntime|System.err" > stacktrace.txt
```

---

## 🎯 クイックテスト

### ワンライナー集
```bash
# 全サーバー起動 + APKインストール + ログ監視
docker-compose -f docker-compose.test.yml up -d && \
adb install -r app/build/outputs/apk/debug/app-debug.apk && \
adb logcat | grep -i mole

# クリーンインストール
adb uninstall net.ktnx.mobileledger.debug; \
adb install app/build/outputs/apk/debug/app-debug.apk && \
adb shell am start -n net.ktnx.mobileledger.debug/.ui.activity.MainActivity

# バージョン情報の一括確認
for PORT in 5023 5032 5040 5050; do \
  echo "Port $PORT: $(curl -s http://localhost:$PORT/version)"; \
done
```

---

## 📋 トラブルシューティング

### デバイスが認識されない
```bash
# ADBサーバーを再起動
adb kill-server
adb start-server
adb devices
```

### 権限エラー
```bash
# Linuxでの権限問題
sudo adb kill-server
sudo adb start-server
# または udev rulesを設定
```

### インストールエラー
```bash
# INSTALL_FAILED_UPDATE_INCOMPATIBLE
adb uninstall net.ktnx.mobileledger.debug
adb install app/build/outputs/apk/debug/app-debug.apk

# INSTALL_FAILED_INSUFFICIENT_STORAGE
adb shell pm trim-caches 100M
```

### ネットワーク接続エラー
```bash
# ファイアウォール確認（Linux）
sudo ufw status
sudo ufw allow 5032/tcp

# 接続テスト
ping 192.168.1.100
curl http://192.168.1.100:5032/version
```

---

## 📚 参考資料

- **adb公式ドキュメント**: https://developer.android.com/studio/command-line/adb
- **logcat公式ドキュメント**: https://developer.android.com/studio/command-line/logcat
- **hledger-web**: https://hledger.org/hledger-web.html

---

## 💡 Tips

### エイリアスの設定
```bash
# .bashrc または .zshrc に追加
alias mole-install="adb install -r app/build/outputs/apk/debug/app-debug.apk"
alias mole-log="adb logcat | grep -i mole"
alias mole-start="adb shell am start -n net.ktnx.mobileledger.debug/.ui.activity.MainActivity"
alias mole-servers="docker-compose -f docker-compose.test.yml up -d"
```

### テストデータのバックアップ
```bash
# データベースをバックアップ
adb backup -f mole-backup.ab -noapk net.ktnx.mobileledger.debug

# リストア
adb restore mole-backup.ab
```

### 効率的なテストフロー
1. `./scripts/test-quick-start.sh` で環境セットアップ
2. `docs/TESTING_GUIDE.md` に従ってテスト実施
3. `docs/DEVICE_TEST_REPORT_TEMPLATE.md` に結果を記録

---

**このチートシートは随時更新してください！**
