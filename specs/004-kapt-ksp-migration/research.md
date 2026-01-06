# リサーチ: KAPT から KSP への移行

**作成日**: 2026-01-06
**機能ブランチ**: `004-kapt-ksp-migration`

## 調査項目

### 1. KSP バージョン互換性

**決定**: KSP 2.0.21-1.0.26 を使用

**根拠**:
- Kotlin 2.0.21 と厳密に互換性がある（KSP バージョンの最初の部分が Kotlin バージョンと一致する必要がある）
- 2024年10月24日にリリースされた安定版
- Maven Central で利用可能

**検討した代替案**:
- KSP 2.0.21-1.0.25: 利用可能だが、1.0.26 がより新しい
- KSP 2.3.x (新バージョン体系): Kotlin バージョンに依存しない新体系だが、このプロジェクトでは既存の互換性モデルに従う

**参照**:
- [Maven Repository: KSP 2.0.21-1.0.26](https://mvnrepository.com/artifact/com.google.devtools.ksp/com.google.devtools.ksp.gradle.plugin/2.0.21-1.0.26)
- [Google KSP Releases](https://github.com/google/ksp/releases)

---

### 2. Room の KSP サポート

**決定**: Room 2.4.2 は KSP をサポートしている

**根拠**:
- Room は 2.4.0 から KSP をサポート開始
- 現在のプロジェクトは Room 2.4.2 を使用
- 同じアーティファクト (`androidx.room:room-compiler`) を `kapt` から `ksp` に変更するだけで移行可能

**検討した代替案**:
- Room バージョンのアップグレード: 不要（現在のバージョンで KSP サポート済み）

**参照**:
- [Android Developer: Migrate from kapt to KSP](https://developer.android.com/build/migrate-to-ksp)

---

### 3. KSP 設定オプション

**決定**: Room の KSP 引数を `ksp {}` ブロックで設定

**根拠**:
- `room.schemaLocation`: スキーマ出力ディレクトリ（必須）
- `room.incremental`: インクリメンタル処理の有効化（パフォーマンス向上）
- `room.expandProjection`: クエリ最適化（既存設定を維持）

**移行パターン**:
```groovy
// 移行前 (KAPT)
kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.expandProjection", "true")
    }
}

// 移行後 (KSP)
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}
```

---

### 4. Gradle バージョンカタログ統合

**決定**: バージョンカタログに KSP プラグインを追加

**根拠**:
- 既存のプロジェクトは Gradle バージョンカタログ (`libs.versions.toml`) を使用
- 一貫性のため、KSP もカタログで管理

**実装パターン**:
```toml
[versions]
ksp = "2.0.21-1.0.26"

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

---

### 5. 移行リスク分析

**決定**: 低リスク移行

**リスク要因**:

| リスク | 影響度 | 確率 | 軽減策 |
|--------|--------|------|--------|
| Room コード生成の差異 | 低 | 低 | ユニットテストで検証 |
| ビルド設定エラー | 中 | 低 | 段階的な変更とテスト |
| スキーマ生成の問題 | 低 | 低 | 既存スキーマとの比較 |

**根拠**:
- Room の KSP サポートは成熟している
- プロジェクトで使用している唯一のアノテーションプロセッサ
- 既存のテストスイートで回帰を検出可能

---

## 結論

すべての技術的な不明点が解決されました。この移行は低リスクであり、以下の手順で実行可能です：

1. バージョンカタログに KSP を追加
2. ルート build.gradle の KAPT プラグインを KSP に置き換え
3. App build.gradle の `kapt()` を `ksp()` に変更、`ksp {}` ブロックを追加
4. 不要な KAPT 設定を削除
5. テストとビルドで検証
