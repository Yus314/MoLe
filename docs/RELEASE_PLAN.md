# MoLe v0.22.0 リリース計画

**作成日**: 2026-01-03
**対象バージョン**: v0.22.0
**リリース予定日**: TBD

---

## 概要

MoLe v0.22.0は、hledger-web v1.32、v1.40、v1.50への対応を追加する重要なリリースです。本ドキュメントはリリースまでの全ステップを記載します。

## リリースの主な変更点

- ✅ hledger-web v1.32, v1.40, v1.50 サポート追加
- ✅ 自動バージョン検出の改善
- ✅ account declaration info サポート（v1.32+）
- ✅ バグ修正（TransactionListParser、NullPointerException）

---

## フェーズ1: リリース前準備（現在のフェーズ）

### 1.1 コード品質チェック ⏳ 未完了

#### タスク：
- [ ] 全てのコンパイル警告を確認・修正
  - 現在の警告：`getPackageInfo()` の非推奨警告（2箇所）
  - 対応方法：Android API 33+向けに `PackageManager.PackageInfoFlags` を使用
- [ ] 未使用のインポート削除
- [ ] コードフォーマット統一確認
- [ ] TODO/FIXMEコメントの確認と対応

#### コマンド：
```bash
# 警告付きビルドで全警告を確認
nix develop .#fhs
./gradlew assembleDebug --warning-mode all

# Lintチェック実行
./gradlew lintDebug
```

---

### 1.2 包括的テスト ⏳ 未完了

#### 1.2.1 自動テスト

- [ ] ユニットテストの実行と修正
  ```bash
  ./gradlew test
  ```
- [ ] 計装テスト（Android Test）の実行
  ```bash
  ./gradlew connectedAndroidTest
  ```
- [ ] テストカバレッジの確認
  ```bash
  ./gradlew jacocoTestReport
  ```

**既知の問題**: テストコードにコンパイルエラーが存在する可能性があります。修正が必要です。

#### 1.2.2 実機テスト（全バージョン）

以下の4つのhledger-webバージョンで動作確認：

- [ ] **v1.23** (後方互換性テスト)
  - [ ] アカウント一覧表示
  - [ ] トランザクション一覧表示
  - [ ] 新規トランザクション作成
  - [ ] プロファイル切り替え

- [ ] **v1.32** (adeclarationinfo対応テスト)
  - [ ] アカウント一覧表示（宣言情報を含む）
  - [ ] トランザクション一覧表示
  - [ ] 新規トランザクション作成

- [ ] **v1.40** (中間バージョンテスト)
  - [ ] アカウント一覧表示
  - [ ] トランザクション一覧表示
  - [ ] 新規トランザクション作成

- [ ] **v1.50** (最新バージョンテスト)
  - [ ] アカウント一覧表示
  - [ ] トランザクション一覧表示
  - [ ] 新規トランザクション作成
  - [ ] エッジケース（大量データ、特殊文字等）

#### テスト実施方法：

```bash
# テストサーバー起動
docker-compose -f docker-compose.test.yml up -d

# 実機にAPKインストール
./scripts/test-quick-start.sh

# テストガイドに従って手動テスト実施
# - docs/TESTING_GUIDE.md
# - docs/TESTING_CHEATSHEET.md
```

#### テスト結果の記録：

```bash
# テスト報告書を作成
cp docs/DEVICE_TEST_REPORT_TEMPLATE.md docs/reports/v0.22.0-test-report-$(date +%Y%m%d).md
# 報告書に結果を記入
```

#### 1.2.3 複数デバイステスト

推奨：異なるAndroidバージョンでテスト

- [ ] Android 9 (API 28) - 最小サポートバージョン付近
- [ ] Android 12 (API 31) - `exported` 要件テスト
- [ ] Android 13/14 (API 33/34) - 最新版

---

### 1.3 ドキュメント整備 ⏳ 未完了

#### 更新が必要なドキュメント：

- [x] **README.md**
  - 対応バージョンテーブル更新済み
  - ビルド手順更新済み

- [x] **CHANGES.md**
  - v0.22.0のエントリ作成済み
  - バグ修正内容記載済み

- [ ] **docs/TESTING_GUIDE.md**
  - v1.32/v1.40/v1.50のテスト手順追加が必要か確認

- [ ] **app/src/main/res/values/strings.xml**
  - バージョン番号の確認
  - 新機能の説明文追加（必要に応じて）

- [ ] **リリースノート作成**
  - ユーザー向けの分かりやすい変更点説明
  - 既知の問題・制限事項
  - アップグレード手順

#### 新規ドキュメント作成：

- [ ] **RELEASE_NOTES_v0.22.0.md**（ユーザー向け）
  - 主な変更点
  - 対応バージョン一覧
  - アップグレード方法
  - スクリーンショット（オプション）

---

### 1.4 署名鍵の準備 ⏳ 未完了

#### 確認事項：

- [ ] リリース署名鍵の存在確認
  ```bash
  ls -la ~/.android/release-keystore.jks
  # または
  ls -la keystore/mole-release.jks
  ```

- [ ] `keystore.properties` の設定
  ```properties
  storeFile=/path/to/keystore.jks
  storePassword=***
  keyAlias=mole-release
  keyPassword=***
  ```

#### 署名鍵が未作成の場合：

```bash
# 新規作成
keytool -genkey -v \
  -keystore ~/.android/mole-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias mole-release

# keystore.propertiesに記載
cat > keystore.properties <<EOF
storeFile=$HOME/.android/mole-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=mole-release
keyPassword=YOUR_KEY_PASSWORD
EOF

# Gitから除外されているか確認
grep keystore.properties .gitignore
```

**重要**: 署名鍵とパスワードは厳重に保管してください。紛失すると同じパッケージ名でのアップデートができなくなります。

---

## フェーズ2: リリースビルド

### 2.1 バージョン番号の確定 ⏳ 未完了

- [ ] `app/build.gradle` でバージョン情報を確認・更新
  ```gradle
  versionCode = 58  // インクリメント（現在57）
  versionName = "0.22.0"
  ```

- [ ] バージョン番号がCHANGES.mdと一致しているか確認

---

### 2.2 リリースAPKのビルド ⏳ 未完了

#### 方法1: Nixビルド環境（推奨）

```bash
# FHS環境に入る
nix develop .#fhs

# クリーンビルド
./gradlew clean

# リリースビルド
./gradlew assembleRelease

# 生成されたAPKの確認
ls -lh app/build/outputs/apk/release/app-release.apk
```

#### 方法2: 標準環境

```bash
# クリーンビルド
./gradlew clean assembleRelease
```

#### ビルド成果物の確認：

```bash
# APKサイズ確認
du -h app/build/outputs/apk/release/app-release.apk

# 署名確認
apksigner verify --verbose app/build/outputs/apk/release/app-release.apk

# APK情報確認
aapt dump badging app/build/outputs/apk/release/app-release.apk | grep -E "package|versionCode|versionName"
```

**期待される出力**:
```
package: name='net.ktnx.mobileledger' versionCode='58' versionName='0.22.0'
Verified using v1 scheme (JAR signing): true
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): true
```

---

### 2.3 リリースAPKのテスト ⏳ 未完了

**重要**: デバッグAPKとは別に、リリースAPKを実機でテストしてください。

- [ ] リリースAPKをインストール
  ```bash
  adb install app/build/outputs/apk/release/app-release.apk
  ```

- [ ] 基本動作確認
  - [ ] アプリ起動
  - [ ] プロファイル作成
  - [ ] 各バージョン（v1.23, v1.32, v1.40, v1.50）で接続テスト
  - [ ] トランザクション作成

- [ ] ProGuard/R8の影響確認
  - [ ] JSON パース処理
  - [ ] データベース操作
  - [ ] ネットワーク通信

- [ ] パフォーマンス確認
  - [ ] 起動時間
  - [ ] 画面遷移の滑らかさ
  - [ ] メモリ使用量

---

## フェーズ3: リリース準備

### 3.1 リリースノート作成 ⏳ 未完了

ユーザー向けのリリースノートを作成：

```markdown
# MoLe v0.22.0 リリースノート

リリース日: 2026-01-XX

## 新機能

### hledger-web v1.32-v1.50 サポート

MoLeは最新のhledger-webバージョンに対応しました：

- **v1.32-v1.39**: account declaration info サポート
- **v1.40-v1.49**: 改善されたbase-url処理
- **v1.50-v1.51**: 最新の安定版

既存のv1.14-v1.23サーバーとも完全に互換性があります。

## 改善点

- 自動バージョン検出が強化され、サーバーバージョンに最適なAPI選択を行います
- より堅牢なエラーハンドリング

## バグ修正

- トランザクション取得時のクラッシュを修正
- アカウント表示時の NullPointerException を修正
- 残高表示の不具合を修正

## アップグレード方法

1. 既存のMoLeアプリをアンインストール（データは保持されます）
2. 新しいAPKをインストール
3. 既存のプロファイルは自動的に移行されます

## 既知の問題

（特になし）

## 対応環境

- Android 9.0 (API 28) 以上
- hledger-web 1.14 - 1.51

## ダウンロード

- APK: [app-release.apk](リンク)
- ソースコード: [v0.22.0](リンク)
```

---

### 3.2 GitHubリリース準備 ⏳ 未完了

#### タグの作成：

```bash
# 最新のコミットを確認
git log --oneline -5

# タグを作成
git tag -a v0.22.0 -m "Release v0.22.0: Support for hledger-web v1.32-v1.50"

# タグをプッシュ
git push origin v0.22.0
```

#### リリースアセットの準備：

- [ ] リリースAPKのリネーム
  ```bash
  cp app/build/outputs/apk/release/app-release.apk \
     releases/MoLe-v0.22.0-release.apk
  ```

- [ ] SHA256チェックサムの生成
  ```bash
  sha256sum releases/MoLe-v0.22.0-release.apk > \
    releases/MoLe-v0.22.0-release.apk.sha256
  ```

- [ ] リリースノートのMarkdownファイル
  ```bash
  cp docs/RELEASE_NOTES_v0.22.0.md releases/
  ```

---

### 3.3 最終チェックリスト ⏳ 未完了

リリース前の最終確認：

- [ ] 全てのテストが成功している
- [ ] CHANGES.mdが最新である
- [ ] README.mdのバージョン情報が正しい
- [ ] リリースAPKが署名されている
- [ ] リリースAPKが実機でテスト済み
- [ ] リリースノートが完成している
- [ ] Gitタグが作成されている
- [ ] 全ての変更がコミット・プッシュされている
- [ ] 署名鍵のバックアップが安全に保管されている

---

## フェーズ4: リリース実施

### 4.1 GitHub Releases での公開 ⏳ 未完了

#### Web UIを使用する場合：

1. https://github.com/[username]/MoLe/releases/new にアクセス
2. タグ `v0.22.0` を選択
3. リリースタイトル: "MoLe v0.22.0 - hledger-web v1.32-v1.50 Support"
4. リリースノートを貼り付け
5. アセットを添付:
   - `MoLe-v0.22.0-release.apk`
   - `MoLe-v0.22.0-release.apk.sha256`
6. "Publish release" をクリック

#### GitHub CLIを使用する場合：

```bash
# GitHub CLIのインストール確認
gh --version

# リリース作成
gh release create v0.22.0 \
  --title "MoLe v0.22.0 - hledger-web v1.32-v1.50 Support" \
  --notes-file releases/RELEASE_NOTES_v0.22.0.md \
  releases/MoLe-v0.22.0-release.apk \
  releases/MoLe-v0.22.0-release.apk.sha256
```

---

### 4.2 その他の配布チャネル（オプション）

#### F-Droid

F-Droidで配布する場合：

- [ ] F-Droidリポジトリにメタデータを追加
- [ ] ビルドレシピ（metadata/*.yml）を作成
- [ ] F-Droid RFP（Request For Packaging）を提出

参考: https://f-droid.org/docs/Submitting_to_F-Droid/

#### Google Play Store

Google Playで配布する場合：

- [ ] Google Play Developer アカウント登録（$25 登録料）
- [ ] アプリリスティング作成
- [ ] スクリーンショット準備
- [ ] プライバシーポリシー作成
- [ ] AAB（Android App Bundle）形式でビルド
  ```bash
  ./gradlew bundleRelease
  ```
- [ ] Google Play Console でアップロード

---

### 4.3 アナウンス ⏳ 未完了

リリースを告知：

- [ ] GitHubのREADMEを更新（最新バージョンへのリンク）
- [ ] プロジェクトのホームページを更新（存在する場合）
- [ ] hledgerコミュニティに通知（存在する場合）
  - hledger forum
  - hledger chat
  - reddit r/plaintextaccounting
- [ ] SNS等での告知（オプション）

---

## フェーズ5: リリース後の対応

### 5.1 モニタリング 継続中

- [ ] GitHub Issuesの監視
- [ ] クラッシュレポートの確認（導入している場合）
- [ ] ユーザーフィードバックの収集

### 5.2 ホットフィックス準備

重大なバグが発見された場合：

1. バグ修正ブランチを作成
   ```bash
   git checkout -b hotfix/v0.22.1
   ```
2. バグを修正
3. テスト実施
4. v0.22.1としてリリース

---

## タイムライン（目安）

| フェーズ | 作業内容 | 所要時間 | 担当 |
|---------|---------|---------|------|
| フェーズ1 | リリース前準備 | 2-3日 | - |
| フェーズ2 | リリースビルド | 1日 | - |
| フェーズ3 | リリース準備 | 1日 | - |
| フェーズ4 | リリース実施 | 1日 | - |
| フェーズ5 | リリース後対応 | 継続 | - |

**合計**: 約5-7日

---

## リスクと緩和策

### リスク1: テストで重大なバグが発見される

**緩和策**:
- 包括的なテストを早期に実施
- バグ修正→再テストのサイクルを繰り返す
- 必要に応じてリリース延期

### リスク2: 署名鍵の問題

**緩和策**:
- 事前に署名鍵の動作確認
- 署名鍵とパスワードの安全なバックアップ
- 失効証明書の準備（オプション）

### リスク3: ビルド環境の問題

**緩和策**:
- Nix Flakesによる再現可能なビルド環境
- Dockerビルドのフォールバック
- ビルド手順の詳細なドキュメント

### リスク4: リリース後の重大なバグ

**緩和策**:
- 徹底的な事前テスト
- 段階的ロールアウト（オプション）
- ホットフィックスの迅速な提供

---

## チェックリスト（全体）

### リリース前
- [ ] コード品質チェック完了
- [ ] 全自動テスト成功
- [ ] 全実機テスト成功（4バージョン）
- [ ] ドキュメント更新完了
- [ ] 署名鍵準備完了

### リリースビルド
- [ ] バージョン番号更新
- [ ] リリースAPKビルド成功
- [ ] リリースAPKテスト成功
- [ ] APK署名検証成功

### リリース準備
- [ ] リリースノート作成
- [ ] Gitタグ作成
- [ ] アセット準備完了
- [ ] 最終チェックリスト完了

### リリース実施
- [ ] GitHub Release公開
- [ ] アナウンス完了

### リリース後
- [ ] モニタリング開始
- [ ] フィードバック収集

---

## 参考資料

- [MoLe ビルドガイド](./BUILDING.md)
- [Nix ビルドガイド](./NIX_BUILD_GUIDE.md)
- [テストガイド](./TESTING_GUIDE.md)
- [Android アプリ署名](https://developer.android.com/studio/publish/app-signing)
- [GitHub Releases](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository)

---

**最終更新**: 2026-01-03
**ステータス**: フェーズ1（リリース前準備）
