# トランザクション作成のトラブルシューティングガイド

## 問題: Dockerサーバーでは動作するが、本番サーバー(https://ledger.mdip2home.com)では動作しない

---

## 📋 診断チェックリスト

### ステップ1: アプリのバージョン確認

**確認方法:**
1. MoLeアプリを開く
2. メニュー → 「設定」または「バージョン情報」
3. バージョン番号を確認

**期待値:** `0.22.1` 以降

**v0.22.0以前の場合:**
```bash
# 最新版をビルドしてインストール
cd /home/kaki/MoLe
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

**理由:** v0.22.0には、hledger-web v1.32以降へのトランザクション追加が失敗する既知の問題があります（docs/RELEASE_NOTES_v0.22.1.md参照）

---

### ステップ2: サーバーのhledger-webバージョン確認

**確認方法:**
```bash
# サーバー上で
hledger-web --version

# または、ブラウザで
https://ledger.mdip2home.com/version
```

**期待値:** バージョン番号を記録

**v1.32以降の場合:**
- アプリがv0.22.1以降であることを再確認
- v0.22.0以前の場合、必ずアップデートが必要

---

### ステップ3: プロファイル設定の確認

**確認方法:**
1. MoLeアプリを開く
2. プロファイルを選択
3. 設定を開く
4. 「投稿を許可」または「Permit Posting」の設定を確認

**期待値:** ✅ 有効（チェックされている）

**無効の場合:**
- 有効にする
- これが無効の場合、トランザクション作成ボタン（FAB）が表示されません

---

### ステップ4: Androidログの確認

**確認方法:**
```bash
# Android端末を接続し、logcatを監視
adb logcat | grep -E "network|http-auth|save-transaction|crash"

# トランザクション作成を試行し、エラーを確認
```

**よくあるエラーパターン:**

| エラーログ | 原因 | 解決方法 |
|----------|------|---------|
| `Requesting host [...] differs from expected [...]` | URL設定の不一致 | プロファイルURLを正確に設定 |
| `SSLHandshakeException` | 証明書の問題 | 証明書を修正またはインストール |
| `Response: 400` | JSON形式の不一致 | アプリをv0.22.1にアップデート |
| `Response: 401` | 認証失敗 | ユーザー名/パスワードを確認 |
| `Response: 403` | 権限エラー | サーバー側で`--allow=add`を設定 |
| `Response: 405` | メソッド不許可 | サーバー設定を確認 |
| `Response: 500` | サーバー内部エラー | サーバーログを確認 |

---

### ステップ5: サーバー側の設定確認

**確認方法:**
```bash
# サーバー上で、hledger-webのプロセスを確認
ps aux | grep hledger-web

# または、systemdサービスの場合
systemctl status hledger-web
journalctl -u hledger-web -f
```

**期待される起動コマンド:**
```bash
hledger-web \
  --serve \
  --host=0.0.0.0 \
  --port 5000 \
  --file /path/to/ledger.journal \
  --base-url https://ledger.mdip2home.com \
  --allow=view,add  # ← 重要: addを許可
```

**`--allow=add`が設定されていない場合:**
```bash
# サービスファイルを編集（systemdの場合）
sudo systemctl edit hledger-web

# または、起動コマンドに追加
```

---

### ステップ6: ジャーナルファイルの権限確認

**確認方法:**
```bash
# サーバー上で
ls -la /path/to/ledger.journal

# hledger-webを実行しているユーザーを確認
ps aux | grep hledger-web | awk '{print $1}'
```

**期待値:** hledger-webを実行しているユーザーに書き込み権限がある

**権限がない場合:**
```bash
# 権限を付与
chmod 664 /path/to/ledger.journal
chown hledger-user:hledger-group /path/to/ledger.journal
```

---

### ステップ7: HTTPS証明書の検証

**確認方法:**
```bash
# サーバー証明書を検証
openssl s_client -connect ledger.mdip2home.com:443 -servername ledger.mdip2home.com < /dev/null 2>&1 | grep -E "Verify|subject|issuer|CN="

# または
curl -vI https://ledger.mdip2home.com 2>&1 | grep -E "SSL|certificate|CN="
```

**期待される出力:**
```
Verify return code: 0 (ok)
subject=CN=ledger.mdip2home.com
```

**証明書エラーの場合:**
- Let's Encryptなどで有効な証明書を取得
- または、自己署名証明書をAndroidにインストール

---

### ステップ8: ネットワーク接続の確認

**確認方法:**
```bash
# Android端末から本番サーバーへの接続テスト
adb shell ping ledger.mdip2home.com

# HTTPSアクセステスト
adb shell "curl -I https://ledger.mdip2home.com"
```

**期待値:** 接続成功、HTTP 200応答

---

### ステップ9: 詳細なデバッグログの取得

**SendTransactionTask.java のログを有効化:**

SendTransactionTask.java では以下のログが出力されます:
- `Logger.debug("network", "Sending using API " + apiVersion)` (行81)
- `Logger.debug("network", "Request body: " + body)` (行109)
- `Logger.debug("network", String.format("Response: %d %s", responseCode, ...))` (行113)

**ログの確認:**
```bash
# Android端末でログを監視
adb logcat | grep -E "network"

# トランザクション作成を試行
# 以下のような出力を確認:
# network: Sending using API v1_32
# network: Request body: {"ptransaction_":"1",...}
# network: Response: 400 Bad Request
```

---

## 🔧 よくある解決パターン

### パターン1: アプリバージョンが古い（最頻出）

**症状:**
- HTTP 400エラー
- サーバーログに「JSON parse error」

**解決:**
```bash
cd /home/kaki/MoLe
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

### パターン2: サーバー設定で投稿が許可されていない

**症状:**
- HTTP 403エラー
- ログに「Operation not permitted」

**解決:**
```bash
# hledger-webの起動コマンドに --allow=add を追加
hledger-web --serve --allow=view,add --file /path/to/ledger.journal
```

---

### パターン3: 認証情報の不一致

**症状:**
- HTTP 401エラー
- logcatに「http-auth」エラー

**解決:**
1. プロファイル設定で認証情報を再入力
2. URLが正確であることを確認（末尾のスラッシュ、http/httpsスキーム）

---

### パターン4: 証明書の問題

**症状:**
- 接続失敗
- logcatに「SSLHandshakeException」

**解決:**
```bash
# Let's Encryptで証明書を取得（サーバー側）
sudo certbot --nginx -d ledger.mdip2home.com

# または、自己署名証明書をAndroidにインストール（開発用）
```

---

## 📊 診断フローチャート

```
開始
 │
 ├─[1] アプリバージョン確認
 │   ├─ v0.22.1以降 → [2]へ
 │   └─ v0.22.0以前 → ✅ アップデート → 終了
 │
 ├─[2] サーバーバージョン確認
 │   ├─ v1.32以降 → [3]へ
 │   └─ v1.31以前 → [3]へ
 │
 ├─[3] プロファイル設定確認
 │   ├─ 投稿許可ON → [4]へ
 │   └─ 投稿許可OFF → ✅ 有効化 → 終了
 │
 ├─[4] Androidログ確認
 │   ├─ エラーなし → [5]へ
 │   ├─ http-authエラー → ✅ URL修正 → 終了
 │   ├─ SSLエラー → ✅ 証明書修正 → 終了
 │   ├─ HTTP 400 → ✅ アプリアップデート → 終了
 │   ├─ HTTP 401 → ✅ 認証情報確認 → 終了
 │   ├─ HTTP 403 → [5]へ
 │   └─ HTTP 500 → [6]へ
 │
 ├─[5] サーバー設定確認
 │   ├─ --allow=add 設定済 → [6]へ
 │   └─ 未設定 → ✅ 設定追加 → 終了
 │
 ├─[6] ファイル権限確認
 │   ├─ 書き込み可能 → [7]へ
 │   └─ 権限なし → ✅ 権限付与 → 終了
 │
 └─[7] サーバーログ確認
     ├─ エラーあり → ✅ エラー対処
     └─ エラーなし → 開発者に問い合わせ
```

---

## 📝 問題解決後の確認

1. ✅ トランザクション作成ボタン（FAB: +）が表示されるか
2. ✅ トランザクションを作成して送信できるか
3. ✅ 送信後、サーバー側でトランザクションが反映されるか
4. ✅ トランザクション一覧で新しいトランザクションが表示されるか

---

## 🆘 サポート

上記の手順で解決しない場合:

1. **Androidログを取得:**
   ```bash
   adb logcat > mole_debug.log
   # トランザクション作成を試行
   # Ctrl+C で停止
   ```

2. **サーバーログを取得:**
   ```bash
   journalctl -u hledger-web -n 100 > hledger_debug.log
   ```

3. **以下の情報をまとめて開発者に報告:**
   - MoLeバージョン
   - hledger-webバージョン
   - Androidログ (mole_debug.log)
   - サーバーログ (hledger_debug.log)
   - サーバーの起動コマンド
   - エラーが発生するタイミングの詳細

---

## 参考資料

- [リリースノート v0.22.1](docs/RELEASE_NOTES_v0.22.1.md)
- [hledger-webアップグレード計画](HLEDGER_WEB_UPGRADE_PLAN.md)
- [MoLe README](README.md)
- [hledger-web公式ドキュメント](https://hledger.org/hledger-web.html)

---

**作成日:** 2026-01-05
**最終更新:** 2026-01-05
