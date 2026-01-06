# Quickstart: Java から Kotlin への移行

**Branch**: `001-java-kotlin-migration` | **Date**: 2026-01-05
**Purpose**: 開発環境セットアップと移行作業の開始手順

## Prerequisites

- Nix (with flakes enabled)
- Git
- Android device or emulator (API 22+)
- (Optional) Docker for local hledger-web server

---

## 1. Environment Setup

### Nix Development Environment

```bash
# リポジトリをクローン
git clone https://github.com/ktnx/mole.git
cd mole

# ブランチをチェックアウト
git checkout 001-java-kotlin-migration

# Nix 開発環境に入る (FHS 推奨)
nix develop .#fhs

# または通常の開発シェル
nix develop
```

### 環境確認

```bash
# Java バージョン確認 (17 必須)
java -version
# openjdk version "17.x.x"

# Android SDK 確認
echo $ANDROID_HOME
# /nix/store/.../android-sdk

# Gradle 確認
./gradlew --version
```

---

## 2. Build Verification

### 現状のビルド確認

移行前に現在の状態でビルドが成功することを確認。

```bash
# Debug ビルド
./gradlew assembleDebug

# テスト実行
./gradlew test

# 結果確認
ls -la app/build/outputs/apk/debug/
```

### APK のインストール

```bash
# デバイス接続確認
adb devices

# APK インストール
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 3. Kotlin Setup

### build.gradle 更新

`app/build.gradle` に以下を追加:

```gradle
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android' version '1.9.25'
    id 'com.google.devtools.ksp' version '1.9.25-1.0.20'
}

android {
    // ... existing config ...

    kotlinOptions {
        jvmTarget = '1.8'
        freeCompilerArgs += ["-Xjsr305=strict"]
    }

    sourceSets {
        main {
            java.srcDirs += 'src/main/kotlin'
        }
        test {
            java.srcDirs += 'src/test/kotlin'
        }
        androidTest {
            java.srcDirs += 'src/androidTest/kotlin'
        }
    }
}

dependencies {
    // Room KSP (replace annotationProcessor)
    ksp "androidx.room:room-compiler:2.4.2"

    // Kotlin
    implementation "org.jetbrains.kotlin:kotlin-stdlib:1.9.25"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"

    // Jackson Kotlin Module
    implementation "com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1"
}
```

### ディレクトリ作成

```bash
mkdir -p app/src/main/kotlin/net/ktnx/mobileledger
mkdir -p app/src/test/kotlin/net/ktnx/mobileledger
mkdir -p app/src/androidTest/kotlin/net/ktnx/mobileledger
```

### Kotlin ビルド確認

```bash
# 設定変更後の再ビルド
./gradlew clean assembleDebug

# テスト実行
./gradlew test
```

---

## 4. Migration Workflow

### Step 1: Nullability Annotation (Pre-Migration)

変換前に Java コードにアノテーションを追加。

```bash
# Android Studio で
# Analyze > Infer Nullity > Whole Project
```

### Step 2: File Conversion

Android Studio の自動変換を使用:

1. 変換対象の `.java` ファイルを開く
2. `Code > Convert Java File to Kotlin File` (Ctrl+Alt+Shift+K)
3. 変換後のコードをレビュー
4. 必要に応じて Kotlin イディオムに調整

### Step 3: Manual Improvements

自動変換後に手動で改善:

```kotlin
// Before (自動変換)
class Account {
    private var name: String? = null

    fun getName(): String? {
        return name
    }

    fun setName(name: String?) {
        this.name = name
    }
}

// After (改善後)
data class Account(
    val name: String
)
```

### Step 4: Verification

各ファイル変換後に検証:

```bash
# コンパイル確認
./gradlew compileDebugKotlin

# テスト実行
./gradlew test

# Lint チェック
./gradlew lintDebug
```

---

## 5. Testing with hledger-web

### ローカルサーバー (Docker)

```bash
# hledger-web サーバー起動
docker run -d \
  -p 5000:5000 \
  -v $(pwd)/docs/test-data:/data \
  --name hledger-web \
  hledger/hledger-web:latest \
  hledger-web -f /data/test.journal --serve --host 0.0.0.0

# 確認
curl http://localhost:5000/api/v1/accounts
```

### 本番サーバー接続

spec に記載のサーバーを使用:

```
URL: https://ledger.mdip2home.com
```

### アプリでの接続テスト

1. アプリを起動
2. 新しいプロファイルを作成
3. サーバー URL を入力
4. 同期を実行
5. アカウント・取引が表示されることを確認

---

## 6. Phase-by-Phase Migration

### Phase 1: utils, err (14 files)

```bash
# 対象ファイル一覧
ls app/src/main/java/net/ktnx/mobileledger/utils/
ls app/src/main/java/net/ktnx/mobileledger/err/

# 変換
# 1. Misc.java → Misc.kt
# 2. SimpleDate.java → SimpleDate.kt
# ... etc.

# 検証
./gradlew test
```

### Phase 2: model (16 files)

```bash
# data class 化の主要対象
ls app/src/main/java/net/ktnx/mobileledger/model/

# LedgerAccount, LedgerTransaction 等
```

### Phase 3-7: 以降のフェーズ

詳細は `plan.md` を参照。

---

## 7. Troubleshooting

### コンパイルエラー: Platform 型

```
Type mismatch: inferred type is String? but String was expected
```

**解決策**: Java 側に `@NonNull` / `@Nullable` を追加してから再変換。

### Room エラー: cannot find symbol

```
error: cannot find symbol
  symbol: method getX()
```

**解決策**: data class のプロパティ名が正しいことを確認。Room は getter 名を期待する。

### KSP エラー: annotation processor

```
w: [ksp] No providers were found
```

**解決策**: `annotationProcessor` を `ksp` に置換し、正しいバージョンを使用。

### Jackson エラー: No primary or single unique constructor

```
InvalidDefinitionException: No primary or single unique constructor found
```

**解決策**: `jackson-module-kotlin` を追加し、ObjectMapper に登録。

```kotlin
val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
```

---

## 8. Nix Flake コマンド（推奨）

Nix Flake を使用した開発ワークフローコマンドが用意されています。

### 利用可能なコマンド一覧

| コマンド | 説明 |
|----------|------|
| `nix run .#build` | デバッグ APK ビルド |
| `nix run .#test` | ユニットテスト実行 |
| `nix run .#clean` | ビルド成果物のクリーン |
| `nix run .#install` | ビルド → 実機インストール |
| `nix run .#verify` | フルワークフロー（テスト → ビルド → インストール）|
| `nix run .#buildRelease` | リリース APK ビルド（署名必要）|

### 基本的な使い方

```bash
# デバッグビルドのみ
nix run .#build

# ユニットテストのみ
nix run .#test

# ビルドして実機にインストール
nix run .#install

# フルワークフロー（推奨）
nix run .#verify
```

### 開発シェル

```bash
# FHS 互換環境（ビルド可能）
nix develop .#fhs

# 通常の開発シェル
nix develop
```

---

## 9. 実機テストワークフロー

### 重要: 全ての機能変更は実機で検証する

Kotlin 移行作業では、各変更後に必ず実機でテストを行います。

### 検証ワークフロー

```
┌─────────────────┐
│ 機能実装/修正   │
└────────┬────────┘
         ▼
┌─────────────────┐
│ ユニットテスト  │ ← nix run .#test
└────────┬────────┘
         ▼
┌─────────────────┐
│ ビルド確認      │ ← nix run .#build
└────────┬────────┘
         ▼
┌─────────────────┐
│ 実機インストール│ ← nix run .#install
└────────┬────────┘
         ▼
┌─────────────────┐
│ 手動検証        │ ← 開発者が実機で確認
└────────┬────────┘
         ▼
┌─────────────────┐
│ 問題報告/次へ   │
└─────────────────┘
```

### 推奨コマンド: `nix run .#verify`

このコマンドは上記ワークフローを自動化します:

```bash
nix run .#verify
```

実行結果:
1. ユニットテストを実行
2. デバッグ APK をビルド
3. 接続されたデバイスに APK をインストール
4. 手動確認チェックリストを表示

### 実機確認チェックリスト

`nix run .#verify` 実行後、以下を手動で確認:

- [ ] アプリが正常に起動する
- [ ] データ更新（リフレッシュ）が動作する
- [ ] プロファイルの作成/編集ができる
- [ ] 取引の登録ができる
- [ ] アカウント一覧が正しく表示される
- [ ] エラーメッセージが適切に表示される

### デバイス接続の確認

```bash
# 接続されたデバイスの確認
adb devices

# 期待される出力:
# List of devices attached
# XXXXXXXXX    device
```

デバイスが表示されない場合:
1. USB デバッグが有効になっているか確認
2. USB ケーブルを再接続
3. デバイスで接続許可ダイアログを確認

### ログの確認

問題が発生した場合、ログを確認:

```bash
# MoLe アプリのログをフィルタリング
adb logcat | grep -E "(MoLe|mobileledger)"

# または tag でフィルタリング
adb logcat -s MoLe:V
```

---

## 10. Gradle コマンド（直接実行）

FHS 環境内 (`nix develop .#fhs`) で直接 Gradle を実行することも可能です。

```bash
# クリーンビルド
./gradlew clean assembleDebug

# テストのみ
./gradlew test

# 特定のテスト
./gradlew test --tests "*.AmountStyleTest"

# Lint
./gradlew lintDebug

# 依存関係確認
./gradlew dependencies

# APK サイズ分析
./gradlew analyzeDebugBundle
```

---

## 11. Code Review Checklist

変換後の各ファイルで確認:

- [ ] `!!` 演算子の使用がないこと（または理由コメントあり）
- [ ] `var` より `val` を優先
- [ ] data class が適切に使用されている
- [ ] スコープ関数のネストが 2 段階以内
- [ ] `@JvmStatic` / `@JvmField` が Java 互換性のために追加されている
- [ ] KDoc が公開 API に追加されている

---

## 12. Next Steps

1. `specs/001-java-kotlin-migration/tasks.md` でタスク一覧を生成
2. Phase 1 (utils, err) から移行開始
3. 各 Phase 完了後にテスト実行と PR 作成
4. 全 Phase 完了後に最終検証
