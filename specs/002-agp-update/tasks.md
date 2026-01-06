# Tasks: AGP 最新安定版へのアップデート

**入力**: `/specs/002-agp-update/` の設計ドキュメント
**前提**: plan.md (必須), spec.md (必須), research.md, quickstart.md

**テスト**: 既存テストの実行のみ（新規テスト作成なし）

**構成**: タスクはユーザーストーリーごとにグループ化され、独立した実装とテストが可能

## フォーマット: `[ID] [P?] [Story] 説明`

- **[P]**: 並列実行可能（異なるファイル、依存関係なし）
- **[Story]**: タスクが属するユーザーストーリー（例: US1, US2）
- 説明に正確なファイルパスを含める

## パス規則

- **ビルド設定**: `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`
- **アプリビルド**: `app/build.gradle`
- **Nix環境**: `flake.nix`

---

## Phase 1: Setup（準備）

**目的**: アップグレード前のベースライン確認とバックアップ

- [x] T001 現在のビルド状態を確認 (`nix run .#build` が成功することを確認)
- [x] T002 [P] 現在のテスト結果をベースラインとして記録 (`nix run .#test`)
- [x] T003 [P] 現在のバージョン情報を記録 (AGP 8.0.2, Gradle 8.0)

**チェックポイント**: アップグレード前の状態が正常であることを確認

---

## Phase 2: User Story 1 & 2 - ビルドシステムのアップグレード + Gradle Wrapper互換性 (優先度: P1) 🎯 MVP

**目的**: AGP を 8.7.3 に、Gradle を 8.9 に更新し、ビルドが成功することを確認

**独立テスト**: `nix run .#build` でデバッグAPKが正常にビルドされることを確認

### 実装

- [x] T004 [P] [US1] Version Catalog の AGP バージョンを更新 (`gradle/libs.versions.toml`: `agp = "8.0.2"` → `agp = "8.7.3"`)
- [x] T005 [P] [US2] Gradle Wrapper のバージョンを更新 (`gradle/wrapper/gradle-wrapper.properties`: `gradle-8.0-bin.zip` → `gradle-8.9-bin.zip`)
- [x] T006 [US1] Gradle キャッシュをクリア (`nix run .#clean`)
- [x] T007 [US1] デバッグビルドを実行して成功を確認 (`nix run .#build`)
- [x] T008 [US1] リリースビルドを実行して成功を確認 (`nix run .#buildRelease`)

**チェックポイント**: AGP 8.7.3 + Gradle 8.9 でビルドが成功

---

## Phase 3: User Story 3 - テストスイートの検証 (優先度: P1)

**目的**: アップグレード後も既存テストがすべて成功することを確認

**独立テスト**: `nix run .#test` で全テストが成功

### 実装

- [x] T009 [US3] ユニットテストを実行 (`nix run .#test`)
- [x] T010 [US3] テスト結果をベースライン (T002) と比較し、リグレッションがないことを確認

**チェックポイント**: 全既存テストが成功、リグレッションなし

---

## Phase 4: User Story 4 - 実機デプロイの検証 (優先度: P1)

**目的**: ビルドされたAPKが実機で正常に動作することを確認

**独立テスト**: `nix run .#install` で実機インストールが成功し、アプリが起動する

### 実装

- [x] T011 [US4] 実機にAPKをインストール (`nix run .#install`)
- [x] T012 [US4] アプリを起動してクラッシュがないことを確認
- [x] T013 [US4] コア操作を確認（アカウント表示、取引作成）

**チェックポイント**: 実機でアプリが正常動作

---

## Phase 5: User Story 5 - 依存関係の互換性 (優先度: P2)

**目的**: すべての依存関係が新しいAGP/Gradleと互換性があることを確認

**独立テスト**: ビルド出力に重大な非推奨警告がないことを確認

### 実装

- [x] T014 [US5] ビルドログを確認し、重大な非推奨警告がないことを確認
- [x] T015 [US5] Room/kapt アノテーション処理が正常に完了していることを確認

**チェックポイント**: 依存関係の互換性が確認済み

---

## Phase 6: Polish（仕上げ）

**目的**: ドキュメント更新と最終確認

- [x] T016 フルワークフローを実行して最終確認 (`nix run .#verify`)
- [x] T017 [P] CLAUDE.md の Active Technologies セクションを更新（AGP 8.7.3, Gradle 8.9）
- [x] T018 ビルド時間がベースラインから50%以上増加していないことを確認

**チェックポイント**: 全検証完了、ドキュメント更新済み

---

## 依存関係＆実行順序

### フェーズ依存関係

- **Setup (Phase 1)**: 依存なし - 即座に開始可能
- **US1 & US2 (Phase 2)**: Setup完了後 - ビルドシステムの核心変更
- **US3 (Phase 3)**: Phase 2完了後 - テスト検証
- **US4 (Phase 4)**: Phase 3完了後 - 実機検証
- **US5 (Phase 5)**: Phase 2完了後 - Phase 3/4と並列可能
- **Polish (Phase 6)**: Phase 3, 4, 5完了後 - 最終確認

### ユーザーストーリー依存関係

```
Phase 1 (Setup)
    ↓
Phase 2 (US1 + US2: ビルドシステム) ← 核心変更
    ↓
    ├── Phase 3 (US3: テスト検証)
    │       ↓
    │   Phase 4 (US4: 実機検証)
    │
    └── Phase 5 (US5: 依存関係) ← 並列可能
            ↓
        Phase 6 (Polish)
```

### 並列実行機会

- T001, T002, T003 - Setup内で並列可能
- T004, T005 - Phase 2内で並列可能（異なるファイル）
- Phase 3とPhase 5 - Phase 2完了後に並列可能
- T016, T017 - Polish内で並列可能

---

## 並列実行例: Phase 2

```bash
# Version Catalog と Gradle Wrapper を並列更新:
Task: "Version Catalog の AGP バージョンを更新 (gradle/libs.versions.toml)"
Task: "Gradle Wrapper のバージョンを更新 (gradle/wrapper/gradle-wrapper.properties)"

# その後、順次実行:
Task: "Gradle キャッシュをクリア"
Task: "デバッグビルド確認"
Task: "リリースビルド確認"
```

---

## 実装戦略

### MVP First (Phase 1 + Phase 2)

1. Phase 1: Setup 完了
2. Phase 2: ビルドシステムアップグレード完了
3. **停止して検証**: デバッグ/リリースビルドが成功
4. MVPとして使用可能

### 段階的デリバリー

1. Setup + US1/US2 完了 → ビルド成功
2. US3追加 → テスト成功 → 品質確認
3. US4追加 → 実機動作 → エンドユーザー検証
4. US5追加 → 依存関係確認 → 長期安定性
5. Polish → ドキュメント更新 → 完了

---

## メモ

- [P] タスク = 異なるファイル、依存関係なし
- [Story] ラベル = 特定のユーザーストーリーへのマッピング
- 各ユーザーストーリーは独立して完了・テスト可能
- タスクまたは論理グループの完了後にコミット
- 任意のチェックポイントで停止してストーリーを独立検証可能
- ソースコード変更は不要（ビルド設定のみ）
