# クイックスタート: KAPT から KSP への移行

## 概要

このガイドでは、MoLe プロジェクトのビルド設定を KAPT から KSP に移行する手順を説明します。

## 前提条件

- Nix 開発環境がセットアップ済み
- Git リポジトリがクリーンな状態

## 移行手順

### ステップ 1: ブランチの確認

```bash
git checkout 004-kapt-ksp-migration
```

### ステップ 2: バージョンカタログの更新

`gradle/libs.versions.toml` に KSP を追加：

```toml
[versions]
ksp = "2.0.21-1.0.26"

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

KAPT プラグインの行を削除：
```toml
# 削除
kotlin-kapt = { id = "org.jetbrains.kotlin.kapt", version.ref = "kotlin" }
```

### ステップ 3: ルート build.gradle の更新

KAPT を KSP に置き換え：

```groovy
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false  // 変更
}
```

### ステップ 4: app/build.gradle の更新

1. プラグインセクション：
```groovy
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)  // 変更
}
```

2. `javaCompileOptions` と `kapt {}` ブロックを削除

3. `ksp {}` ブロックを追加（`android {}` ブロックの外）：
```groovy
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}
```

4. 依存関係を更新：
```groovy
dependencies {
    // Room
    implementation libs.bundles.room
    ksp libs.androidx.room.compiler  // kapt → ksp
}
```

### ステップ 5: 検証

```bash
# テスト実行
nix run .#test

# ビルド確認
nix run .#build

# フル検証（テスト → ビルド → インストール）
nix run .#verify
```

## トラブルシューティング

### Gradle 同期エラー

```
Could not find com.google.devtools.ksp:...
```

**解決策**: バージョンカタログの KSP バージョンが Kotlin バージョンと一致しているか確認

### Room コンパイルエラー

```
Cannot find implementation for ...
```

**解決策**: `ksp {}` ブロックの設定を確認、特に `room.schemaLocation`

### スキーマファイルが生成されない

**解決策**: `$projectDir/schemas` ディレクトリが存在することを確認

## 参照

- [実装計画](./plan.md)
- [機能仕様](./spec.md)
- [リサーチ結果](./research.md)
