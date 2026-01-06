# Quickstart: Kotlin 2.0.21 Update

**Feature**: 003-kotlin-update
**Date**: 2026-01-06

## Prerequisites

- Nix flake 環境（`nix develop .#fhs`）
- Android 実機またはエミュレータ（検証用）

## Quick Verification

```bash
# フル検証（テスト → ビルド → インストール）
nix run .#verify

# 個別コマンド
nix run .#test     # ユニットテスト
nix run .#build    # デバッグビルド
nix run .#install  # 実機インストール
```

## Key Files Changed

### Build Configuration

| File | Change |
|------|--------|
| `gradle/libs.versions.toml` | kotlin: 1.9.25 → 2.0.21, coroutines: 1.7.3 → 1.9.0 |
| `app/build.gradle` | kotlinOptions → compilerOptions 移行 |

### Source Code

| Pattern | Change |
|---------|--------|
| `appendln()` | → `appendLine()` |
| `toUpperCase()` | → `uppercase()` |
| `toLowerCase()` | → `lowercase()` |
| `object X : SealedClass` | → `data object X : SealedClass` (該当箇所のみ) |

## Verification Checklist

- [ ] `nix run .#build` が成功
- [ ] `nix run .#test` が全パス
- [ ] Deprecation warning が 0 件
- [ ] 実機でアプリ起動確認
- [ ] プロファイル作成・編集が動作
- [ ] データ更新（同期）が動作
- [ ] 取引登録が動作

## Rollback

問題発生時は以下で元のバージョンに戻す:

```bash
git checkout master -- gradle/libs.versions.toml app/build.gradle
nix run .#build
```

## Known Issues

### Room + kapt Warning

Room 2.4.2 + kapt で以下の警告が出る可能性あり:
```
w: [kapt] Incremental annotation processing requested...
```
→ 警告は無視可能。エラーの場合は Room 2.6.1 へアップグレードを検討。

### K2 Compiler Strictness

K2 コンパイラは型推論がより厳密:
- 以前は警告だったものがエラーになる可能性
- 修正方法は各エラーメッセージに従う
