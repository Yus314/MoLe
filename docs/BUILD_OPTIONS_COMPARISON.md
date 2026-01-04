# NixOSでのMoLeビルドオプション比較

## 概要

NixOSでMoLeアプリをビルドする際の各アプローチの比較表

| オプション | 難易度 | Nix統合 | 成功率 | 保守性 | 推奨度 |
|-----------|--------|---------|--------|--------|--------|
| 1. buildFHSUserEnv | ⭐⭐ 中 | ⭐⭐⭐ 中 | ⭐⭐⭐⭐⭐ 高 | ⭐⭐⭐⭐ 良 | ⭐⭐⭐⭐⭐ 最推奨 |
| 2. android-nixpkgs + FHS | ⭐⭐⭐ 高 | ⭐⭐⭐⭐ 高 | ⭐⭐⭐⭐ 高 | ⭐⭐⭐⭐⭐ 優 | ⭐⭐⭐⭐ 推奨 |
| 3. patchelf (手動) | ⭐⭐ 中 | ⭐⭐ 低 | ⭐⭐⭐ 中 | ⭐⭐ 低 | ⭐⭐ 非推奨 |
| 4. Gradle Hook (自動パッチ) | ⭐⭐⭐⭐ 高 | ⭐⭐⭐ 中 | ⭐⭐ 低 | ⭐ 悪 | ⭐ 非推奨 |
| 5. Docker | ⭐ 低 | - なし | ⭐⭐⭐⭐⭐ 高 | ⭐⭐⭐⭐ 良 | ⭐⭐⭐ 代替案 |

---

## 詳細比較

### オプション1: buildFHSUserEnv ⭐⭐⭐⭐⭐ 最推奨

**ファイル**: `shell-fhs.nix`

**メリット**:
- ✅ NixOSでも標準的なLinuxバイナリが動作
- ✅ GradleがダウンロードするAAPT2などが問題なく動作
- ✅ 設定がシンプル
- ✅ 再現性が高い
- ✅ 他の開発者も同じ環境を構築可能

**デメリット**:
- ⚠️ Android SDKを手動インストールする必要がある場合がある
- ⚠️ 完全なNix純粋性は失われる（FHS環境を使用するため）

**使用例**:
```bash
nix-shell shell-fhs.nix
./gradlew assembleDebug
```

**推奨される理由**:
- バランスが良い（実用性とNix統合のトレードオフ）
- 最も成功率が高い
- 保守が容易

---

### オプション2: android-nixpkgs + buildFHSUserEnv ⭐⭐⭐⭐

**ファイル**: `flake-fhs.nix`

**メリット**:
- ✅ Android SDKもNixで完全管理
- ✅ FHS環境でGradleツールが動作
- ✅ 最も「Nix的」なソリューション
- ✅ 再現性が最高レベル
- ✅ flake.lockで完全にバージョン固定可能

**デメリット**:
- ⚠️ 設定がやや複雑
- ⚠️ android-nixpkgsの学習が必要
- ⚠️ flakesの有効化が必要

**使用例**:
```bash
nix develop .#fhs
./gradlew assembleDebug
```

**推奨される理由**:
- 長期的には最も保守しやすい
- チーム開発に最適
- CI/CDにも統合しやすい

---

### オプション3: patchelf (手動パッチ) ⭐⭐

**ファイル**: `scripts/patch-android-tools.sh`

**メリット**:
- ✅ 既存のflake.nixをそのまま使える
- ✅ 必要なバイナリだけをパッチ
- ✅ NixOSの問題を直接解決

**デメリット**:
- ⚠️ Gradleがツールを更新するたびに再パッチが必要
- ⚠️ 手動操作が必要（自動化が困難）
- ⚠️ エラーが発生しやすい
- ⚠️ すべてのバイナリをカバーできない可能性

**使用例**:
```bash
# ビルド試行（エラー発生）
./gradlew assembleDebug

# パッチ適用
./scripts/patch-android-tools.sh

# 再ビルド
./gradlew assembleDebug
```

**非推奨の理由**:
- 保守が大変
- 新しいツールがダウンロードされるたびに問題が発生
- 不安定

---

### オプション4: Gradle Build Hook (自動パッチ)

**メリット**:
- ✅ 理論上は自動化可能

**デメリット**:
- ❌ 実装が非常に複雑
- ❌ Gradleビルドプロセスの深い理解が必要
- ❌ 壊れやすい
- ❌ デバッグが困難
- ❌ 保守が事実上不可能

**推奨しない理由**:
- リスクが高すぎる
- 他のオプションの方が遥かに実用的

---

### オプション5: Docker ⭐⭐⭐

**ファイル**: `Dockerfile.build`, `docker-compose.build.yml`

**メリット**:
- ✅ NixOSの制約を完全に回避
- ✅ 標準的なAndroid開発環境
- ✅ CI/CDにも使用可能
- ✅ 非常に安定
- ✅ 他のLinuxディストリビューションと同じ環境

**デメリット**:
- ⚠️ Nixの恩恵を受けられない
- ⚠️ Dockerのオーバーヘッド（ディスク、メモリ）
- ⚠️ Nixエコシステムから外れる

**使用例**:
```bash
docker-compose -f docker-compose.build.yml build
docker-compose -f docker-compose.build.yml up
```

**代替案として推奨される理由**:
- 確実に動作する
- シンプル
- Nix環境の問題を完全に回避

---

## 推奨される選択フロー

```
NixOSでMoLeをビルドしたい
  │
  ├─ Nixで完全に管理したい？
  │   │
  │   ├─ Yes → オプション2 (android-nixpkgs + FHS) ⭐⭐⭐⭐
  │   │        最も「Nix的」、長期的に最良
  │   │
  │   └─ No → オプション1 (buildFHSUserEnv) ⭐⭐⭐⭐⭐
  │            実用的、バランス良好
  │
  └─ Nixにこだわらない？
      │
      └─ Yes → オプション5 (Docker) ⭐⭐⭐
               確実、シンプル
```

---

## 実装の優先順位

### 即座に試すべき
1. **オプション1 (buildFHSUserEnv)** - 最もバランスが良い

### 時間があれば
2. **オプション2 (android-nixpkgs + FHS)** - 長期的に最良

### 代替案として
3. **オプション5 (Docker)** - Nix環境で問題が続く場合

### 避けるべき
- オプション3 (手動patchelf) - 保守が大変
- オプション4 (Gradle Hook) - リスクが高い

---

## 具体的な実装手順

### 推奨: オプション1を試す

```bash
cd /home/kaki/MoLe

# FHS環境に入る
nix-shell shell-fhs.nix

# 環境内でビルド
./gradlew assembleDebug
```

**予想される問題と対処法**:

1. **Android SDKが見つからない**
   ```bash
   # Android SDKを手動でインストール
   # または flake-fhs.nix を使用
   ```

2. **権限エラー**
   ```bash
   chmod +x gradlew
   ```

3. **依存関係エラー**
   ```bash
   # 初回ビルドは時間がかかる（依存関係ダウンロード）
   # インターネット接続を確認
   ```

---

### 代替: オプション5 (Docker) を試す

```bash
cd /home/kaki/MoLe

# Dockerイメージをビルド
docker-compose -f docker-compose.build.yml build

# ビルド実行
docker-compose -f docker-compose.build.yml up

# APKの確認
ls app/build/outputs/apk/debug/
```

---

## まとめ

**最推奨**: オプション1 (buildFHSUserEnv)
- 実用的
- 成功率が高い
- 保守が容易

**長期的ベスト**: オプション2 (android-nixpkgs + FHS)
- 完全なNix管理
- 再現性最高

**確実な代替**: オプション5 (Docker)
- Nix問題を回避
- 標準的な開発環境

**避けるべき**: オプション3, 4
- 保守が困難
- リスクが高い
