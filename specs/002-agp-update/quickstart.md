# クイックスタート: AGP 8.7.3 アップグレード

**機能ブランチ**: `002-agp-update`
**所要時間**: 約15分

## 概要

Android Gradle Plugin (AGP) を 8.0.2 から 8.7.3 に、Gradle を 8.0 から 8.9 にアップグレードする手順。

## 前提条件

- [x] Nix 開発環境が利用可能
- [x] 物理 Android デバイスが接続されている（実機テスト用）
- [x] 現在のブランチ: `002-agp-update`

## アップグレード手順

### ステップ 1: Version Catalog の更新

`gradle/libs.versions.toml` を編集:

```diff
[versions]
-agp = "8.0.2"
+agp = "8.7.3"
```

### ステップ 2: Gradle Wrapper の更新

`gradle/wrapper/gradle-wrapper.properties` を編集:

```diff
-distributionUrl=https\://services.gradle.org/distributions/gradle-8.0-bin.zip
+distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
```

### ステップ 3: ビルド確認

```bash
# デバッグビルド
nix run .#build

# テスト実行
nix run .#test
```

### ステップ 4: 実機確認

```bash
# 実機にインストール
nix run .#install

# または、フルワークフロー（テスト → ビルド → インストール）
nix run .#verify
```

### ステップ 5: アプリ動作確認

1. アプリを起動
2. アカウント表示を確認
3. 取引作成を試行

## トラブルシューティング

### Gradle キャッシュの問題

```bash
# キャッシュをクリア
nix run .#clean

# .gradle ディレクトリを削除（必要に応じて）
rm -rf ~/.gradle/caches/
rm -rf .gradle/
```

### Lint エラーが発生した場合

AGP 8.7.x では LintError がビルドを中断する可能性があります。

```groovy
// app/build.gradle に追加（必要に応じて）
android {
    lint {
        checkReleaseBuilds false
        abortOnError false
    }
}
```

### 依存関係エラーが発生した場合

```bash
# 依存関係ツリーを確認
nix develop .#fhs --command ./gradlew app:dependencies
```

## 検証チェックリスト

| 項目 | コマンド | 期待結果 |
|------|---------|---------|
| デバッグビルド | `nix run .#build` | 成功 |
| リリースビルド | `nix run .#buildRelease` | 成功（署名設定がある場合） |
| ユニットテスト | `nix run .#test` | 全テスト成功 |
| 実機インストール | `nix run .#install` | インストール成功 |
| アプリ起動 | 手動確認 | クラッシュなし |

## 変更ファイル一覧

| ファイル | 変更内容 |
|---------|---------|
| `gradle/libs.versions.toml` | AGP バージョン更新 |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle バージョン更新 |

## ロールバック手順

問題が発生した場合:

```bash
# 変更を元に戻す
git checkout -- gradle/libs.versions.toml
git checkout -- gradle/wrapper/gradle-wrapper.properties

# キャッシュをクリア
nix run .#clean
```
