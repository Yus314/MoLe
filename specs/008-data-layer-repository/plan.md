# Implementation Plan: Data Layer Repository Migration

**Branch**: `008-data-layer-repository` | **Date**: 2026-01-10 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/008-data-layer-repository/spec.md`

## Summary

Data.ktシングルトンからGoogleが推奨するRepositoryパターンへの段階的移行。TransactionRepository、ProfileRepository、AccountRepository、TemplateRepositoryを新規作成し、既存のDAOをラップ。各Repositoryはインターフェース+実装クラス分離パターンを採用。Data.ktはAppStateManagerにリネームしてUI/App状態専用として維持。

## Technical Context

**Language/Version**: Kotlin 2.0.21 / JVM target 1.8
**Primary Dependencies**: Hilt 2.51.1, Room 2.4.2, Coroutines 1.9.0, Jetpack Compose
**Storage**: Room Database (SQLite)
**Testing**: JUnit + MockK + Hilt Testing
**Target Platform**: Android (minSdk 26, targetSdk 34)
**Project Type**: Android Mobile Application
**Performance Goals**: Existing performance maintained (no regression)
**Constraints**: 後方互換性維持、ViewModel単位での完全移行
**Scale/Scope**: 4 ViewModels (MainViewModel, NewTransactionViewModel, ProfileDetailViewModel, BackupsViewModel)、8 DAOs

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| X. 階層型アーキテクチャ | ✅ ALIGNED | Repositoryパターンは憲章で推奨されている |
| VIII. 依存性注入 (Hilt) | ✅ ALIGNED | Repository/Implをコンストラクタインジェクションで提供 |
| II. テスト駆動開発 | ✅ ALIGNED | インターフェース分離によりモック容易 |
| VI. Kotlinコード標準 | ✅ ALIGNED | Coroutines/Flowを使用 |
| I. コードの可読性 | ✅ ALIGNED | 責務分離により可読性向上 |

**Gates passed. Proceeding to Phase 0.**

## Project Structure

### Documentation (this feature)

```text
specs/008-data-layer-repository/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (Repository interfaces)
└── tasks.md             # Phase 2 output (by /speckit.tasks)
```

### Source Code (repository root)

```text
app/src/main/kotlin/net/ktnx/mobileledger/
├── data/                           # NEW: Data Layer
│   └── repository/                 # NEW: Repository implementations
│       ├── TransactionRepository.kt      # Interface
│       ├── TransactionRepositoryImpl.kt  # Implementation
│       ├── ProfileRepository.kt          # Interface
│       ├── ProfileRepositoryImpl.kt      # Implementation
│       ├── AccountRepository.kt          # Interface
│       ├── AccountRepositoryImpl.kt      # Implementation
│       ├── TemplateRepository.kt         # Interface
│       └── TemplateRepositoryImpl.kt     # Implementation
│
├── dao/                            # EXISTING: Room DAOs (unchanged)
│   ├── ProfileDAO.kt
│   ├── TransactionDAO.kt
│   ├── AccountDAO.kt
│   └── ...
│
├── model/
│   ├── Data.kt → AppStateManager.kt  # RENAME: UI/App state only
│   └── ...
│
├── di/
│   ├── DatabaseModule.kt           # EXISTING: Provides DAOs
│   ├── DataModule.kt → AppStateModule.kt  # RENAME: Provides AppStateManager
│   └── RepositoryModule.kt         # NEW: Provides Repositories
│
└── ui/
    ├── main/MainViewModel.kt       # MODIFY: Inject repositories
    ├── transaction/NewTransactionViewModel.kt  # MODIFY: Inject repositories
    ├── profile/ProfileDetailViewModel.kt      # MODIFY: Inject repositories
    └── backups/BackupsViewModel.kt # MODIFY: Inject repositories

app/src/test/kotlin/net/ktnx/mobileledger/
└── data/
    └── repository/                 # NEW: Repository unit tests
        ├── TransactionRepositoryTest.kt
        ├── ProfileRepositoryTest.kt
        ├── AccountRepositoryTest.kt
        └── TemplateRepositoryTest.kt
```

**Structure Decision**: Mobile application with Data Layer (Repository pattern) added between existing DAO layer and ViewModel/UI layer. Follows Google Android architecture guidelines.

## Constitution Check (Post-Design)

*Re-check after Phase 1 design completed.*

| Gate | Status | Notes |
|------|--------|-------|
| X. 階層型アーキテクチャ | ✅ ALIGNED | Repository Interface/Impl分離、単方向データフロー(Flow)採用 |
| VIII. 依存性注入 (Hilt) | ✅ ALIGNED | RepositoryModuleでインターフェースをBinds、Singletonスコープ適切 |
| II. テスト駆動開発 | ✅ ALIGNED | contracts/にインターフェース定義、FakeDAOでテスト可能 |
| VI. Kotlinコード標準 | ✅ ALIGNED | Coroutines suspend fun + Flow採用、LiveData→Flow変換 |
| I. コードの可読性 | ✅ ALIGNED | Repository責務明確、AppStateManager分離 |
| III. 最小構築・段階的開発 | ✅ ALIGNED | P1-P4の段階的移行、ViewModel単位での完全移行 |
| IX. 静的解析とリント | ✅ ALIGNED | 既存のktlint/detekt設定を継承 |

**All gates passed. Ready for task generation.**

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| インターフェース+実装分離 | テスト時のFake差し替え容易化 | 具象クラスのみではMockitoに依存し、テスト複雑化 |
| AppStateManager維持 | UI状態(drawer、locale等)は純粋なアプリ状態でありRepository責務外 | 全てProfileRepositoryに統合するとSRPに違反 |
