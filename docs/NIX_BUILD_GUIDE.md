# Nix Flakeでのビルドガイド

**対象バージョン**: v0.22.0
**最終更新**: 2026-01-03

---

## 概要

MoLeはNix Flakesを使用した完全に再現可能なビルド環境を提供します。

## クイックスタート

```bash
# ワンコマンドビルド（最も簡単）
nix run .#build
```

APKは `app/build/outputs/apk/debug/app-debug.apk` に生成されます。

---

## 利用可能なコマンド

### 1. `nix run .#build` - ワンコマンドビルド ⭐推奨

FHS環境でAPKをビルドします。

```bash
nix run .#build
```

**特徴**:
- 依存関係の自動セットアップ
- ビルド環境の自動構築
- ビルド結果の即座の確認

**所要時間**:
- 初回: 約30-40秒
- 2回目以降: 約5-10秒（増分ビルド）

---

### 2. `nix develop .#fhs` - FHS開発シェル

FHS互換の開発環境に入ります。

```bash
nix develop .#fhs
./gradlew assembleDebug
./gradlew test
./gradlew clean
```

**用途**:
- 対話的な開発作業
- 複数のGradleコマンドの実行
- デバッグ作業

---

### 3. `nix develop` - 標準開発シェル

開発ツール（git, jdk等）のみ提供するシェル。

```bash
nix develop
```

**注意**: このシェルではAPKビルドは**できません**。ビルドには`nix develop .#fhs`を使用してください。

---

### 4. `nix flake show` - 利用可能な出力の確認

Flakeで提供されているパッケージやアプリケーションを表示します。

```bash
nix flake show
```

**出力例**:
```
git+file:///home/kaki/MoLe
├───apps
│   └───x86_64-linux
│       └───build: app
├───devShells
│   └───x86_64-linux
│       ├───default: development environment
│       └───fhs: development environment 'mole-android-fhs'
└───packages
    └───x86_64-linux
        ├───apk: package 'mole-apk'
        ├───build-script: package 'build-mole'
        └───default: package 'mole-apk'
```

---

## よくある質問

### Q1: `nix build`は使えますか？

**A**: いいえ、`nix build`はサポートされていません。

Gradleはビルド時にネットワークアクセス（依存関係のダウンロード）が必要ですが、Nixの純粋ビルドサンドボックスではネットワークアクセスが制限されます。

代わりに `nix run .#build` を使用してください。これはFHS環境内でビルドを実行するため、正常に動作します。

---

### Q2: ビルドが遅いのですが？

**A**: 初回ビルドでは以下の処理が発生します：

1. Nix依存関係のダウンロード（約2-3GB）
2. Android SDK のセットアップ
3. Gradle依存関係のダウンロード（約200MB）
4. Javaコードのコンパイル

2回目以降は、変更されたファイルのみが再コンパイルされるため、大幅に高速化されます（5-10秒程度）。

**高速化のヒント**:
- Gradle Daemonを有効化（デフォルトで有効）
- Configuration Cacheを利用（デフォルトで有効）
- 増分ビルドの活用（cleanを実行しない）

---

### Q3: エラー "evaluation warning: 'hostPlatform' has been renamed" が出ます

**A**: これは警告であり、ビルドには影響しません。

android-nixpkgsの古いパッケージ定義によるもので、Nixの新しいバージョンでは`stdenv.hostPlatform`が推奨されています。動作には問題ありません。

---

### Q4: クリーンビルドをしたい

**A**: 以下のコマンドを使用してください：

```bash
# FHS環境でクリーンビルド
nix run .#build
# その前に手動でクリーン（オプション）
rm -rf app/build .gradle build
```

または：

```bash
nix develop .#fhs
./gradlew clean assembleDebug
```

---

### Q5: リリースビルドはできますか？

**A**: はい、署名鍵を設定すればリリースビルドが可能です。

```bash
nix develop .#fhs

# 署名鍵の設定（keystore.propertiesに記載）
# リリースビルド
./gradlew assembleRelease
```

---

## トラブルシューティング

### 問題: JAVA_HOME が設定されていない

**症状**:
```
ERROR: JAVA_HOME is not set
```

**解決方法**:
`nix develop`ではなく`nix develop .#fhs`または`nix run .#build`を使用してください。

---

### 問題: Gradle Daemonの起動に失敗

**症状**:
```
Starting a Gradle Daemon, X incompatible Daemons could not be reused
```

**解決方法**:
これは正常な動作です。古いDaemonは停止され、新しいDaemonが起動します。ビルドには影響しません。

---

### 問題: ネットワークエラー

**症状**:
```
Could not resolve all dependencies
```

**解決方法**:
1. インターネット接続を確認
2. プロキシ設定を確認（必要に応じて）
3. Gradleキャッシュをクリア: `rm -rf ~/.gradle/caches`

---

## 参考資料

- [Nix Flakes Manual](https://nixos.org/manual/nix/stable/command-ref/new-cli/nix3-flake.html)
- [android-nixpkgs](https://github.com/tadfisher/android-nixpkgs)
- [MoLe Build Options Comparison](./BUILD_OPTIONS_COMPARISON.md)
- [MoLe Building Guide](./BUILDING.md)

---

## まとめ

| コマンド | 用途 | 推奨度 |
|---------|------|-------|
| `nix run .#build` | ワンコマンドビルド | ⭐⭐⭐⭐⭐ |
| `nix develop .#fhs` | 対話的開発 | ⭐⭐⭐⭐ |
| `nix develop` | ツールのみ | ⭐⭐ |
| `nix build` | **未サポート** | ❌ |

**初めてのビルドには `nix run .#build` を使用することを推奨します。**
