# MoLe - ビルド手順書

**対象バージョン**: v0.22.0
**最終更新**: 2026-01-03

---

## 目次

1. [概要](#概要)
2. [前提条件](#前提条件)
3. [NixOSでのビルド (推奨)](#nixosでのビルド-推奨)
4. [標準Android環境でのビルド](#標準android環境でのビルド)
5. [Dockerでのビルド](#dockerでのビルド)
6. [トラブルシューティング](#トラブルシューティング)
7. [CI/CD統合](#cicd統合)

---

## 概要

MoLeはAndroidアプリケーションで、以下の方法でビルドできます：

- **NixOS** (FHS環境) - 完全に再現可能
- **標準Android開発環境** (Android Studio, Windows, macOS, Linux)
- **Docker** - プラットフォーム非依存

---

## 前提条件

### 共通要件

- **Java**: OpenJDK 17以上
- **Git**: ソースコード取得用
- **インターネット接続**: 依存関係のダウンロード用

### 環境別要件

#### NixOS
- Nix 2.4以上 (flakes有効化)
- 約2GB以上の空きディスク容量

#### 標準環境
- Android SDK (API level 33以上)
- Gradle 8.0以上（Gradle Wrapperを使用する場合は不要）

#### Docker
- Docker 20.10以上
- Docker Compose 2.0以上（オプション）

---

## NixOSでのビルド (推奨)

### 方法1: nix run を使用 (最も簡単) ⭐

Nix Flakesを使用したワンコマンドビルド：

```bash
cd /path/to/MoLe
nix run .#build
```

**特徴**:
- ワンコマンドで完結
- 依存関係の自動セットアップ
- FHS環境の自動構築
- ビルド結果の即座の確認

**ビルド時間**:
- 初回: 約30-40秒
- 2回目以降: 約5秒（増分ビルド）

APKは `app/build/outputs/apk/debug/app-debug.apk` に生成されます。

---

### 方法2: shell-fhs.nixを使用

#### ステップ1: 環境のビルド

```bash
cd /path/to/MoLe
nix-build shell-fhs.nix -o result-fhs
```

**初回実行時の所要時間**: 約5-10分（依存関係のダウンロード）

#### ステップ2: ビルド実行

```bash
# FHS環境でビルドを実行
./result-fhs/bin/mole-android-env -c "./gradlew assembleDebug"
```

**ビルド時間**:
- 初回: 約30-40秒
- 2回目以降: 約5秒（クリーンビルド）、約500ms（増分ビルド）

#### ステップ3: APKの確認

```bash
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

出力例：
```
-rw-r--r-- 1 user users 7.0M Jan 3 18:11 app/build/outputs/apk/debug/app-debug.apk
```

### 方法2: flake.nixを使用

#### ステップ1: FHS環境に入る

```bash
nix develop .#fhs
```

#### ステップ2: 通常通りビルド

```bash
./gradlew assembleDebug
```

### local.propertiesの設定

Nix環境では、Android SDKのパスを`local.properties`に設定する必要があります：

```bash
# Android SDKパスを確認
nix eval --raw --impure --expr 'let flake = builtins.getFlake "path:/home/kaki/MoLe"; in flake.devShells.x86_64-linux.default.shellHook' 2>&1 | grep ANDROID_HOME

# local.propertiesを作成
cat > local.properties <<EOF
sdk.dir=/nix/store/...-android-sdk-env/share/android-sdk
EOF
```

**注意**: `/nix/store`のパスは環境により異なります。上記コマンドで取得したパスを使用してください。

### クリーンビルド

```bash
./result-fhs/bin/mole-android-env -c "./gradlew clean assembleDebug"
```

### リリースビルド

```bash
./result-fhs/bin/mole-android-env -c "./gradlew assembleRelease"
```

**注意**: リリースビルドには署名が必要です。

---

## 標準Android環境でのビルド

### Android Studioを使用

#### ステップ1: プロジェクトを開く

1. Android Studioを起動
2. "Open an Existing Project"を選択
3. MoLeディレクトリを選択

#### ステップ2: ビルド

1. メニューから `Build` → `Make Project` を選択
2. または `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`

#### ステップ3: APKの確認

`app/build/outputs/apk/debug/app-debug.apk`

### コマンドラインを使用

#### Linux / macOS

```bash
cd /path/to/MoLe

# デバッグビルド
./gradlew assembleDebug

# リリースビルド
./gradlew assembleRelease
```

#### Windows

```cmd
cd C:\path\to\MoLe

REM デバッグビルド
gradlew.bat assembleDebug

REM リリースビルド
gradlew.bat assembleRelease
```

### 環境変数の設定

Android SDKの場所を指定する場合：

```bash
export ANDROID_HOME=/path/to/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
```

または`local.properties`ファイルを作成：

```properties
sdk.dir=/path/to/android-sdk
```

---

## Dockerでのビルド

### 方法1: Dockerfileを使用

#### ステップ1: Dockerイメージをビルド

```bash
docker build -t mole-builder -f Dockerfile.build .
```

#### ステップ2: コンテナでビルド

```bash
docker run --rm -v $(pwd):/app -w /app mole-builder ./gradlew assembleDebug
```

#### ステップ3: APKの確認

```bash
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

### 方法2: Docker Composeを使用

#### ステップ1: ビルド実行

```bash
docker-compose -f docker-compose.build.yml up
```

#### ステップ2: APKの取得

APKは`app/build/outputs/apk/debug/`に生成されます。

### Dockerのメリット

- ✅ ホスト環境に依存しない
- ✅ CI/CDに統合しやすい
- ✅ 再現性が高い

### Dockerのデメリット

- ⚠️ ディスク使用量が多い
- ⚠️ ビルドキャッシュの管理が必要

---

## トラブルシューティング

### エラー: SDK location not found

**原因**: Android SDKのパスが設定されていない

**解決策**:

1. `local.properties`ファイルを作成：
   ```properties
   sdk.dir=/path/to/android-sdk
   ```

2. または環境変数を設定：
   ```bash
   export ANDROID_HOME=/path/to/android-sdk
   ```

### エラー: Could not start dynamically linked executable (NixOS)

**原因**: NixOSで標準のLinuxバイナリが実行できない

**解決策**: FHS環境を使用してください：

```bash
nix-build shell-fhs.nix -o result-fhs
./result-fhs/bin/mole-android-env -c "./gradlew assembleDebug"
```

### エラー: Compilation failed

**原因**: テストコードにコンパイルエラーがある（既知の問題）

**解決策**: テストをスキップしてビルド：

```bash
./gradlew assembleDebug -x test
```

### ビルドが遅い

**解決策**:

1. Gradle Daemonを有効化（通常はデフォルトで有効）
2. ビルドキャッシュを利用（`~/.gradle/caches`）
3. 増分ビルドを活用（変更したファイルのみ再コンパイル）

### メモリ不足エラー

**解決策**: Gradleのヒープサイズを増やす

`gradle.properties`に追加：
```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m
```

---

## CI/CD統合

### GitHub Actions

`.github/workflows/build.yml`の例：

```yaml
name: Build APK

on:
  push:
    branches: [ main, master ]
  pull_request:
    branches: [ main, master ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Setup Android SDK
      uses: android-actions/setup-android@v2

    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-

    - name: Build debug APK
      run: ./gradlew assembleDebug

    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
```

### GitLab CI

`.gitlab-ci.yml`の例：

```yaml
image: openjdk:17-jdk

variables:
  ANDROID_COMPILE_SDK: "33"
  ANDROID_BUILD_TOOLS: "30.0.3"

before_script:
  - apt-get --quiet update --yes
  - apt-get --quiet install --yes wget unzip
  - export ANDROID_HOME="${PWD}/android-sdk-linux"
  - wget --quiet --output-document=android-sdk.zip https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
  - unzip -q android-sdk.zip -d $ANDROID_HOME
  - mkdir -p $ANDROID_HOME/cmdline-tools/latest
  - mv $ANDROID_HOME/cmdline-tools/* $ANDROID_HOME/cmdline-tools/latest/
  - echo y | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platforms;android-${ANDROID_COMPILE_SDK}" "build-tools;${ANDROID_BUILD_TOOLS}"
  - chmod +x ./gradlew

stages:
  - build

build:
  stage: build
  script:
    - ./gradlew assembleDebug
  artifacts:
    paths:
      - app/build/outputs/apk/debug/app-debug.apk
```

---

## ビルド最適化

### 並列ビルド

`gradle.properties`に追加：
```properties
org.gradle.parallel=true
org.gradle.workers.max=4
```

### Configuration Cache

Gradle 8.0以降では自動的に有効化されますが、明示的に設定する場合：

```bash
./gradlew assembleDebug --configuration-cache
```

### ビルドキャッシュ

`gradle.properties`に追加：
```properties
org.gradle.caching=true
```

---

## まとめ

### 推奨される方法

| 環境 | 推奨方法 | 理由 |
|------|---------|------|
| **NixOS** | shell-fhs.nix | 再現性が高く、簡単 |
| **Linux/Mac** | Gradle CLI | シンプル、高速 |
| **Windows** | Android Studio | GUI、デバッグしやすい |
| **CI/CD** | Docker | プラットフォーム非依存 |

### ビルド時間の目安

| ビルドタイプ | 初回 | 2回目以降 |
|-------------|------|-----------|
| クリーンビルド | 30-40秒 | 5-10秒 |
| 増分ビルド | - | 500ms-2秒 |

### 次のステップ

ビルドが成功したら：

1. ✅ **テスト実行**: `./gradlew test`（オプション）
2. ✅ **APK検証**: `app/build/outputs/apk/debug/app-debug.apk`
3. ✅ **実機テスト**: エミュレーターまたは実機にインストール
4. ✅ **リリース準備**: 署名付きリリースAPKの作成

---

**参考資料**:
- [Android開発ガイド](https://developer.android.com/guide)
- [Gradle公式ドキュメント](https://docs.gradle.org/)
- [Nix公式ドキュメント](https://nixos.org/manual/nix/stable/)
