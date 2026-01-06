# 実装計画: KAPT から KSP への移行

**ブランチ**: `004-kapt-ksp-migration` | **作成日**: 2026-01-06 | **仕様**: [spec.md](./spec.md)
**入力**: 機能仕様 `/specs/004-kapt-ksp-migration/spec.md`

## サマリー

Android プロジェクトのビルド設定を KAPT (Kotlin Annotation Processing Tool) から KSP (Kotlin Symbol Processing) に移行する。Room ライブラリのアノテーション処理をより高速で効率的な KSP に切り替え、インクリメンタルビルドのパフォーマンスを向上させる。

## 技術コンテキスト

**言語/バージョン**: Kotlin 2.0.21, Java 8 互換 (JVM target 1.8)
**主要依存関係**: Room 2.4.2, KSP 2.0.21-1.0.26 (新規追加)
**ストレージ**: Room Database (SQLite) - 変更なし
**テスト**: JUnit 4, Espresso
**対象プラットフォーム**: Android (minSdk 22, targetSdk 34)
**プロジェクトタイプ**: Android モバイルアプリ
**パフォーマンス目標**: インクリメンタルビルド時間の短縮
**制約**: Java 8 互換性維持必須、既存テストの変更不可
**スケール/スコープ**: ビルド設定ファイル 3 つの変更

## 憲章チェック

*ゲート: Phase 0 リサーチ前に通過必須。Phase 1 設計後に再チェック。*

| 原則 | ステータス | 備考 |
|------|----------|------|
| I. コードの可読性とメンテナンス性 | ✅ 準拠 | ビルド設定のみ、アプリコード変更なし |
| II. 単体テスト | ✅ 準拠 | 既存テストで検証、新規テスト不要 |
| III. 最小構築・段階的開発 | ✅ 準拠 | 3ファイルの原子的変更 |
| IV. パフォーマンス最適化 | ✅ 準拠 | KSP によるビルド時間短縮 |
| V. アクセシビリティ | N/A | UI 変更なし |
| VI. Kotlin移行 | ✅ 準拠 | KSP は Kotlin エコシステムの推奨ツール |
| VII. Nix開発環境 | ✅ 準拠 | Nix コマンドで検証可能 |

**ゲート結果**: ✅ 全項目準拠 - 計画続行

## プロジェクト構造

### ドキュメント (この機能)

```text
specs/004-kapt-ksp-migration/
├── spec.md              # 機能仕様書
├── plan.md              # この計画ファイル
├── research.md          # Phase 0 リサーチ結果
├── checklists/
│   └── requirements.md  # 品質チェックリスト
└── tasks.md             # Phase 2 タスク (/speckit.tasks で生成)
```

### ソースコード (変更対象ファイル)

```text
/
├── gradle/
│   └── libs.versions.toml   # KSP バージョン・プラグイン追加
├── build.gradle             # KAPT → KSP プラグイン置換
└── app/
    └── build.gradle         # kapt() → ksp() 依存関係変更、ksp{} ブロック追加
```

**構造決定**: 既存の Android プロジェクト構造を維持。ビルド設定ファイルのみ変更。

## 複雑さ追跡

> 憲章違反がないため、このセクションは不要

該当なし - すべての憲章原則に準拠

## 実装アプローチ

### 変更詳細

#### 1. gradle/libs.versions.toml

```diff
 [versions]
 agp = "8.7.3"
 kotlin = "2.0.21"
+ksp = "2.0.21-1.0.26"
 coroutines = "1.9.0"
 ...

 [plugins]
 android-application = { id = "com.android.application", version.ref = "agp" }
 kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
-kotlin-kapt = { id = "org.jetbrains.kotlin.kapt", version.ref = "kotlin" }
+ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

#### 2. build.gradle (ルート)

```diff
 plugins {
     alias(libs.plugins.android.application) apply false
     alias(libs.plugins.kotlin.android) apply false
-    alias(libs.plugins.kotlin.kapt) apply false
+    alias(libs.plugins.ksp) apply false
 }
```

#### 3. app/build.gradle

```diff
 plugins {
     alias(libs.plugins.android.application)
     alias(libs.plugins.kotlin.android)
-    alias(libs.plugins.kotlin.kapt)
-    // KSP available but disabled during Java→Kotlin migration.
-    // Enable after DAO conversion: id 'com.google.devtools.ksp' version '1.9.25-1.0.20'
+    alias(libs.plugins.ksp)
 }

 android {
     ...
     defaultConfig {
         ...
-        javaCompileOptions {
-            annotationProcessorOptions {
-                arguments += [
-                        "room.schemaLocation"  : "$projectDir/schemas".toString(),
-                        "room.incremental"     : "true",
-                        "room.expandProjection": "true"
-                ]
-            }
-        }
-
-        // KSP Room configuration (disabled during migration)
-        // ksp {
-        //     arg("room.schemaLocation", "$projectDir/schemas")
-        //     arg("room.incremental", "true")
-        //     arg("room.expandProjection", "true")
-        // }
     }
     ...
-    kapt {
-        arguments {
-            arg("room.schemaLocation", "$projectDir/schemas")
-            arg("room.incremental", "true")
-            arg("room.expandProjection", "true")
-        }
-    }
     ...
 }

+ksp {
+    arg("room.schemaLocation", "$projectDir/schemas")
+    arg("room.incremental", "true")
+    arg("room.expandProjection", "true")
+}

 dependencies {
     ...
     // Room
-    // Note: Using kapt for Room annotation processing with Kotlin files.
-    // Will switch to KSP once migration is complete for better performance.
     implementation libs.bundles.room
-    kapt libs.androidx.room.compiler
+    ksp libs.androidx.room.compiler
     ...
 }
```

## 検証計画

### 必須検証

1. **ユニットテスト**: `nix run .#test`
2. **ビルド検証**: `nix run .#build`
3. **フル検証**: `nix run .#verify`

### 確認項目

- [ ] Room スキーマファイルが `app/schemas/` に生成される
- [ ] すべてのユニットテストが合格
- [ ] デバッグ APK が正常にビルド
- [ ] アプリが実機で起動・動作
- [ ] ビルドログに KAPT 関連のエラー・警告なし

## リスクと軽減策

| リスク | 確率 | 影響 | 軽減策 |
|--------|------|------|--------|
| Room コード生成の差異 | 低 | 中 | ユニットテストで検証 |
| スキーマ生成パス問題 | 低 | 低 | 既存スキーマと比較 |
| Gradle 同期エラー | 低 | 低 | エラーメッセージに従い修正 |

## 参照

- [research.md](./research.md) - 技術リサーチ結果
- [Android Developer: Migrate from kapt to KSP](https://developer.android.com/build/migrate-to-ksp)
- [KSP Quickstart](https://kotlinlang.org/docs/ksp-quickstart.html)
- [Maven: KSP 2.0.21-1.0.26](https://mvnrepository.com/artifact/com.google.devtools.ksp/com.google.devtools.ksp.gradle.plugin/2.0.21-1.0.26)
