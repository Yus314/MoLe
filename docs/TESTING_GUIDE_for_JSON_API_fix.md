# テストガイド

## 概要
このガイドでは、JSON API修正後のMoLeアプリをテストする手順を説明します。

---

## ビルド手順

### 方法1: Nixを使用（推奨）

```bash
# 1. プロジェクトディレクトリに移動
cd /home/kaki/MoLe

# 2. ビルド実行
nix run .#build

# または FHS環境を使用
nix develop .#fhs --command ./gradlew assembleDebug
```

### 方法2: 通常のGradleビルド

```bash
# JAVA_HOME と ANDROID_HOME が設定されていることを確認
./gradlew assembleDebug
```

ビルド成功後、APKは以下の場所に生成されます:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## テスト環境の準備

### 1. hledger-web サーバーの起動

すでにDocker環境がある場合:
```bash
cd /home/kaki/MoLe
docker-compose -f docker-compose.test.yml up -d
```

サーバーの確認:
```bash
# バージョン確認
curl http://192.168.1.12:5050/version

# 期待される出力: 1.50 または類似
```

### 2. サーバーの設定確認

サーバーが以下のフラグで起動していることを確認:
- `--allow=add` (トランザクション追加を許可)
- `--host=0.0.0.0` (外部接続を許可)

---

## アプリのインストールとテスト

### 1. APKのインストール

```bash
# Android端末/エミュレータにインストール
adb install app/build/outputs/apk/debug/app-debug.apk

# 既存のアプリを上書きする場合
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. プロファイル設定

1. MoLeアプリを起動
2. 設定 → プロファイル
3. 新しいプロファイルを作成または既存のものを編集:
   - **Name:** Test Profile
   - **URL:** http://192.168.1.12:5050
   - **Version:** 自動検出（または手動で1.50を選択）

### 3. 基本的な動作確認

#### テスト1: 接続テスト
1. プロファイルを選択
2. アカウント一覧が正しく表示されることを確認
3. エラーメッセージが出ないことを確認

#### テスト2: トランザクション追加（最重要）
1. 「+」ボタンをタップ
2. 以下の情報を入力:
   ```
   日付: 今日の日付
   説明: Test Transaction

   明細行1:
     勘定科目: Expenses:Food
     金額: 10.00
     通貨: USD

   明細行2:
     勘定科目: Assets:Cash
     金額: -10.00
     通貨: USD
   ```
3. 保存ボタンをタップ
4. **成功の確認:**
   - エラーメッセージが表示されない
   - トランザクション一覧に追加されたトランザクションが表示される
   - サーバー側でもトランザクションが保存されている

---

## トラブルシューティング

### エラー: "Error storing transaction on backend server"

**原因:** まだJSON形式に問題がある可能性があります。

**診断手順:**

1. **Logcatでエラー詳細を確認:**
```bash
adb logcat | grep -i "mobileledger\|error\|exception"
```

2. **サーバー側のログを確認:**
```bash
docker logs hledger-web-150
```

3. **手動でJSONをテスト:**
```bash
# MoLeが送信しているJSONを確認するため、サーバーログをモニタリング
docker logs -f hledger-web-150

# その後、アプリでトランザクション追加を試みる
```

### エラー: ビルド失敗

**Java/Android SDKの問題:**
```bash
# Nix環境を確認
nix develop

# Java バージョン確認
java -version  # 17以上である必要がある

# Android SDK確認
echo $ANDROID_HOME
```

### エラー: 接続失敗

**ネットワークの確認:**
```bash
# サーバーが起動しているか確認
curl -I http://192.168.1.12:5050/

# 期待される出力: HTTP/1.1 200 OK
```

---

## デバッグ用のコマンド

### JSONリクエストの確認

実際にMoLeが送信するJSONを確認したい場合:

```bash
# 方法1: tcpdump でネットワークトラフィックをキャプチャ
sudo tcpdump -i any -A 'host 192.168.1.12 and port 5050'

# 方法2: mitmproxy を使用
mitmproxy -p 8080
# その後、アプリのプロキシ設定を mitmproxy に向ける
```

### 期待されるJSON形式

修正後のMoLeは以下のような形式でJSONを送信するはずです:

```json
{
  "tindex": 1,
  "tdate": "2026-01-04",
  "tdescription": "Test Transaction",
  "tcomment": "",
  "tpostings": [
    {
      "paccount": "Expenses:Food",
      "pamount": [
        {
          "acommodity": "USD",
          "aquantity": {
            "decimalPlaces": 2,
            "decimalMantissa": 1000
          },
          "aismultiplier": false,
          "astyle": {
            "asdecimalmark": ".",
            "asrounding": "NoRounding",
            "asprecision": 2
          },
          "acost": null
        }
      ],
      "ptransaction_": "1",
      "pstatus": "Unmarked"
    }
  ]
}
```

**重要なポイント:**
- ✅ `ptransaction_` は文字列 `"1"` (数値ではない)
- ✅ `asdecimalmark` が存在する (asdecimalpoint ではない)
- ✅ `asrounding` が存在する
- ✅ `acost` が存在する (aprice ではない)

---

## 成功の確認

以下の全てが問題なく動作すれば、修正は成功です:

- [x] アプリがビルドできる
- [x] サーバーに接続できる
- [x] アカウント一覧が表示される
- [ ] **トランザクションが正常に追加できる** ← 最重要
- [ ] 追加したトランザクションがサーバー側に保存される
- [ ] 金額、通貨、コメントが正しく表示される

---

## 次のアクション

### テストが成功した場合:

1. **バージョン更新**
   ```bash
   # app/build.gradle のバージョンを更新
   versionName "0.22.1"
   versionCode 23
   ```

2. **リリースビルド**
   ```bash
   ./gradlew assembleRelease
   ```

3. **Gitコミット**
   ```bash
   git add -A
   git commit -m "Fix JSON API compatibility with hledger-web v1.50

   - Change ptransaction_ type from int to String
   - Add asdecimalmark and asrounding fields to ParsedStyle
   - Rename aprice to acost in ParsedAmount

   Fixes #<issue-number>"

   git tag v0.22.1
   git push origin master --tags
   ```

### テストが失敗した場合:

1. `ERROR_RESOLUTION_REPORT.md` を再確認
2. Logcatとサーバーログから詳細なエラー情報を収集
3. 必要に応じて追加の修正を実施

---

## 参考資料

- **エラー分析:** `ERROR_ANALYSIS.md`
- **根本原因レポート:** `ERROR_RESOLUTION_REPORT.md`
- **修正内容サマリー:** `FIXES_APPLIED.md`
- **検証レポート:** `VERIFICATION_REPORT.md`
- **hledger-web API仕様:** https://hledger.org/hledger-web.html

---

## サポート

問題が発生した場合は、以下の情報を含めて報告してください:

1. Logcatの出力
2. サーバーログ
3. 送信しようとしたトランザクションの内容
4. hledger-webのバージョン
5. MoLeのバージョン
