# Research: Kotlin 1.9.25 → 2.0.21 Update

**Feature**: 003-kotlin-update
**Date**: 2026-01-06

## 1. AGP 8.7.3 + Kotlin 2.0.21 Compatibility

**Decision**: AGP 8.7.3 は Kotlin 2.0.21 と互換性がある。更新不要。

**Rationale**:
- Kotlin 2.0 の最小要件は AGP 8.5
- AGP 8.7.3 は要件を超過しており、完全互換
- 公式互換性マトリックス: https://developer.android.com/build/kotlin-support

**Alternatives Considered**:
- AGP 8.8+ へアップグレード: 不要、現行バージョンで十分

---

## 2. Coroutines Version Update

**Decision**: kotlinx-coroutines 1.7.3 → 1.9.0 へアップグレード

**Rationale**:
- Kotlin 2.0 の最小要件は Coroutines 1.9.0（2.0 でコンパイル済み）
- 1.7.x は Kotlin 2.0 と非互換（コンパイルエラー発生）

| Coroutines Version | Minimum Kotlin Version |
|-------------------|------------------------|
| 1.7.x             | 1.8.x                  |
| 1.8.x             | 1.9.21                 |
| **1.9.0**         | **2.0** (required)     |

**Alternatives Considered**:
- 1.10.2（最新）: 動作するが Kotlin 2.1 向け。2.0.21 では 1.9.0 で十分
- 1.8.1: 非互換、採用不可

---

## 3. Room + KAPT Compatibility

**Decision**: Room 2.4.2 + kapt を継続使用（ksp 移行はスコープ外）

**Rationale**:
- 仕様の Out of Scope で ksp 完全移行は除外されている
- Room 2.4.2 + kapt は Kotlin 2.0 で動作確認要
- kapt は Kotlin 2.0 でもサポート継続（メンテナンスモード）

**リスク軽減策**:
- ビルド時に kapt 警告が出る可能性 → 警告は許容、エラーのみ対処
- metadata version mismatch エラーの場合 → Room 2.6.1 へ最小限アップグレード

**Alternatives Considered**:
- Room 2.6.1 + KSP: 推奨だがスコープ外
- Room 2.4.2 + kapt: 現行維持、問題発生時のみ対応

---

## 4. Dependency Version Matrix

**Decision**: 以下のバージョンに更新

| Dependency | Current | New | Notes |
|------------|---------|-----|-------|
| **Kotlin** | 1.9.25 | **2.0.21** | Target version |
| **AGP** | 8.7.3 | 8.7.3 (維持) | 互換性あり |
| **Coroutines** | 1.7.3 | **1.9.0** | 必須更新 |
| **Room** | 2.4.2 | 2.4.2 (維持) | kapt 継続、問題時のみ 2.6.1 |
| **Lifecycle** | 2.4.1 | 2.4.1 (維持) | Kotlin 2.0 互換 |
| **Navigation** | 2.4.2 | 2.4.2 (維持) | Kotlin 2.0 互換 |
| **Jackson** | 2.17.1 | 2.17.1 (維持) | 問題なし |

**Rationale**: 最小限の変更で Kotlin 2.0.21 互換性を確保。ライブラリの大規模アップグレードはスコープ外。

---

## 5. K2 Compiler Breaking Changes

**Decision**: K2 コンパイラのソース非互換変更に対応

**主な変更点**:

| Issue | Description | Action |
|-------|-------------|--------|
| KT-57750 | 曖昧な star import がエラー | import 競合を解決 |
| KT-56408 | 初期化前のプロパティアクセス | プロパティを適切に初期化 |
| KT-62998 | Java フィールドの nullable 代入 | null チェック追加 |

**対応方針**:
1. `nix run .#build` でエラーを確認
2. エラー箇所を個別に修正
3. 修正後、テストで動作確認

---

## 6. Kotlin 2.0 New Stable Features

**Decision**: 以下の安定機能を適用（可読性向上の場合のみ）

### 6.1 data object

シングルトンの sealed class メンバーに適用:

```kotlin
// Before
object EndOfFile : ReadResult {
    override fun toString() = "EndOfFile"
}

// After
data object EndOfFile : ReadResult  // toString() 自動生成
```

**適用候補**:
- `TemplatesRecyclerViewAdapter.kt` の `TemplateDivider`
- `StyleConfigurer.kt` の各 object
- `TransactionIdType.kt` の `IntType`, `StringType`

### 6.2 Smart Cast Improvements

K2 コンパイラで自動的に恩恵を受ける:
- 変数への型チェック結果の伝播
- OR 条件での共通スーパータイプへのキャスト
- nullable invoke 演算子のスマートキャスト

### 6.3 enumEntries<T>()

`enumValues<T>()` の代替（より効率的）:

```kotlin
// Before (毎回配列生成)
enumValues<RGB>().toList()

// After (同じリストを返す)
enumEntries<RGB>()
```

---

## 7. Deprecated APIs to Fix

**Decision**: 以下の非推奨 API を修正

| Deprecated | Replacement | Priority |
|------------|-------------|----------|
| `appendln()` | `appendLine()` | High |
| `toUpperCase()` | `uppercase()` | High |
| `toLowerCase()` | `lowercase()` | High |
| `kotlinOptions {}` | `compilerOptions {}` | Medium |

**検索コマンド**:
```bash
grep -r "appendln\|toUpperCase\|toLowerCase" app/src/main/kotlin/
```

---

## 8. Build Configuration Changes

**Decision**: `app/build.gradle` を更新

### kotlinOptions の更新

```groovy
// Before
kotlinOptions {
    jvmTarget = '1.8'
    freeCompilerArgs += ["-Xjsr305=strict"]
    javaParameters = true
}

// After (Kotlin 2.0 推奨)
compilerOptions {
    jvmTarget = JvmTarget.JVM_1_8
    freeCompilerArgs.add("-Xjsr305=strict")
    javaParameters = true
}
```

### libs.versions.toml の更新

```toml
[versions]
kotlin = "2.0.21"
coroutines = "1.9.0"
```

---

## Summary

| Topic | Decision | Risk |
|-------|----------|------|
| AGP 互換性 | 更新不要 | Low |
| Coroutines | 1.9.0 へ更新 | Low |
| Room + kapt | 継続、問題時のみ対応 | Medium |
| K2 breaking changes | ビルド時に対応 | Medium |
| 新機能適用 | data object など限定的に | Low |
| 非推奨 API | 修正必須 | High |
