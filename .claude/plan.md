# 実装計画: シングルモジュールからマルチモジュールへの移行

## 概要

MoLeプロジェクト（313個のソースファイル）を、クリーンアーキテクチャに基づいた6モジュール構成へ段階的に移行します。現在のプロジェクトは既にレイヤー別に整理されているため、移行リスクは低く、段階的アプローチにより安全に実施可能です。

## 現状分析

### 現在の構造
```
:app (単一モジュール)
├── di/           (10 files)  - Hilt DI modules
├── domain/       (88 files)  - Domain models, repositories, use cases
├── data/         (14 files)  - Repository implementations, mappers
├── db/           (14 files)  - Room entities
├── dao/          (10 files)  - Data Access Objects
├── ui/           (84 files)  - Compose UI
├── service/      (15 files)  - Application services
├── network/      (4 files)   - Network layer
├── json/         (53 files)  - JSON serialization
├── backup/       (6 files)   - Backup functionality
├── async/        (3 files)   - Async utilities
└── utils/        (10 files)  - Utilities
```

### 目標の構造
```
:app                    - UI, Activities, ViewModels (95 files)
:core:domain            - Domain models, repository interfaces, use cases (88 files)
:core:data              - Repository implementations, mappers (20 files)
:core:database          - Room entities, DAOs, DB class (24 files)
:core:network           - Ktor HTTP client, network utilities (10 files)
:core:common            - Shared utilities, extensions (15 files)
```

## 実装ステップ

### Phase 1: プロジェクト基盤の準備 (推定: 1-2日)

#### Step 1.1: Version Catalog の拡張
```kotlin
// gradle/libs.versions.toml に追加
[plugins]
android-library = { id = "com.android.library", version.ref = "agp" }

[bundles]
# モジュール別の依存関係バンドルを定義
core-domain = ["kotlinx-coroutines-core", "kotlinx-collections-immutable"]
core-data = ["hilt-android", "room-ktx"]
core-database = ["room-runtime", "room-ktx"]
core-network = ["ktor-client-core", "ktor-client-okhttp", "ktor-client-content-negotiation"]
```

#### Step 1.2: Convention Plugin の作成
```
build-logic/
├── build.gradle.kts
├── settings.gradle.kts
└── src/main/kotlin/
    ├── AndroidLibraryConventionPlugin.kt
    ├── HiltConventionPlugin.kt
    └── KotlinAndroidConventionPlugin.kt
```

**理由**: 各モジュールで共通のビルド設定を再利用し、boilerplateを削減

#### Step 1.3: settings.gradle.kts の更新
```kotlin
pluginManagement {
    includeBuild("build-logic")
    // ...
}

include(":app")
include(":core:domain")
include(":core:data")
include(":core:database")
include(":core:network")
include(":core:common")
```

---

### Phase 2: :core:common モジュールの作成 (推定: 0.5日)

#### 対象ファイル
- `utils/` (10 files)
- `async/` (3 files)
- 共通拡張関数

#### build.gradle.kts
```kotlin
plugins {
    id("mole.android.library")
    id("mole.kotlin.android")
}

android {
    namespace = "net.ktnx.mobileledger.core.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
```

#### 移行ファイル
```
core/common/src/main/kotlin/net/ktnx/mobileledger/core/common/
├── async/
│   ├── TaskState.kt
│   └── ...
├── utils/
│   ├── Extensions.kt
│   └── ...
└── Result.kt
```

---

### Phase 3: :core:domain モジュールの作成 (推定: 1-2日)

#### 対象ファイル
- `domain/model/` (31 files)
- `domain/repository/` (7 files - interfaces only)
- `domain/usecase/` (50 files)

#### build.gradle.kts
```kotlin
plugins {
    id("mole.android.library")
    id("mole.kotlin.android")
    id("mole.hilt")
}

android {
    namespace = "net.ktnx.mobileledger.core.domain"
}

dependencies {
    implementation(project(":core:common"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
```

#### 移行ファイル
```
core/domain/src/main/kotlin/net/ktnx/mobileledger/core/domain/
├── model/
│   ├── Profile.kt
│   ├── Transaction.kt
│   ├── TransactionLine.kt
│   ├── Account.kt
│   ├── Template.kt
│   ├── Currency.kt
│   └── ... (31 files)
├── repository/
│   ├── ProfileRepository.kt
│   ├── TransactionRepository.kt
│   ├── AccountRepository.kt
│   ├── CurrencyRepository.kt
│   ├── TemplateRepository.kt
│   ├── OptionRepository.kt
│   └── PreferencesRepository.kt
└── usecase/
    ├── TransactionSender.kt
    ├── TransactionSyncer.kt
    ├── ConfigBackup.kt
    ├── DatabaseInitializer.kt
    └── ... (50 files)
```

#### 重要な考慮事項
- UseCase interfaces と implementations を分離
- Interface は :core:domain に残す
- Implementation は後で :core:data または :app に移動

---

### Phase 4: :core:database モジュールの作成 (推定: 1日)

#### 対象ファイル
- `db/` (14 files - Room entities)
- `dao/` (10 files - DAOs)
- `db/DB.kt` (Database class)

#### build.gradle.kts
```kotlin
plugins {
    id("mole.android.library")
    id("mole.kotlin.android")
    id("mole.hilt")
    alias(libs.plugins.ksp)
}

android {
    namespace = "net.ktnx.mobileledger.core.database"

    defaultConfig {
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
            arg("room.generateKotlin", "true")
        }
    }
}

dependencies {
    implementation(project(":core:common"))

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
```

#### 移行ファイル
```
core/database/src/main/kotlin/net/ktnx/mobileledger/core/database/
├── entity/
│   ├── ProfileEntity.kt      (renamed from db/Profile.kt)
│   ├── TransactionEntity.kt
│   ├── AccountEntity.kt
│   ├── CurrencyEntity.kt
│   └── ... (14 files)
├── dao/
│   ├── ProfileDAO.kt
│   ├── TransactionDAO.kt
│   ├── AccountDAO.kt
│   └── ... (10 files)
└── MoLeDatabase.kt           (renamed from DB.kt)
```

#### 命名規則の変更
- `db.Profile` → `ProfileEntity` (DB entities にはEntity suffix)
- `domain.model.Profile` → `Profile` (Domain models はそのまま)

---

### Phase 5: :core:network モジュールの作成 (推定: 0.5日)

#### 対象ファイル
- `network/` (4 files)
- `json/` の一部 (API response models)

#### build.gradle.kts
```kotlin
plugins {
    id("mole.android.library")
    id("mole.kotlin.android")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.ktnx.mobileledger.core.network"
}

dependencies {
    implementation(project(":core:common"))

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.kotlinx.serialization.json)
}
```

#### 移行ファイル
```
core/network/src/main/kotlin/net/ktnx/mobileledger/core/network/
├── HttpClient.kt
├── RemoteDataSource.kt
├── api/
│   └── HledgerApi.kt
└── model/
    └── ApiResponse.kt
```

---

### Phase 6: :core:data モジュールの作成 (推定: 1-2日)

#### 対象ファイル
- `data/repository/` implementations (7 files)
- `data/repository/mapper/` (6 files)
- UseCase implementations (from domain/usecase/)
- `json/` の残り (mappers, config models)

#### build.gradle.kts
```kotlin
plugins {
    id("mole.android.library")
    id("mole.kotlin.android")
    id("mole.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.ktnx.mobileledger.core.data"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.serialization.json)
}
```

#### 移行ファイル
```
core/data/src/main/kotlin/net/ktnox/mobileledger/core/data/
├── repository/
│   ├── ProfileRepositoryImpl.kt
│   ├── TransactionRepositoryImpl.kt
│   ├── AccountRepositoryImpl.kt
│   └── ... (7 files)
├── mapper/
│   ├── ProfileMapper.kt
│   ├── TransactionMapper.kt
│   └── ... (6 files)
├── usecase/
│   ├── TransactionSenderImpl.kt
│   ├── TransactionSyncerImpl.kt
│   └── ... (implementations)
└── json/
    ├── config/
    ├── mapper/
    └── unified/
```

---

### Phase 7: :app モジュールのリファクタリング (推定: 1-2日)

#### 残留ファイル
- `ui/` (84 files)
- `di/` (10 files - updated for new modules)
- `service/` (15 files)
- `backup/` (6 files)
- `App.kt`

#### build.gradle.kts 更新
```kotlin
plugins {
    id("mole.android.application")
    id("mole.kotlin.android")
    id("mole.hilt")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "net.ktnx.mobileledger"
    // ...
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))

    // UI dependencies
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.navigation)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
}
```

#### DI モジュールの分割
```kotlin
// di/DatabaseModule.kt → core:database に移動
// di/RepositoryModule.kt → core:data に移動
// di/UseCaseModule.kt → core:data に移動
// di/NetworkModule.kt → core:network に移動

// app に残るもの:
// - ServiceModule.kt
// - DispatcherModule.kt
// - EntryPoints
```

---

### Phase 8: テストの移行 (推定: 1-2日)

#### テストファイルの移動
```
core/domain/src/test/
├── model/           (domain model tests)
└── usecase/         (usecase interface tests)

core/data/src/test/
├── repository/      (repository impl tests)
│   └── fake/        (fake implementations)
├── mapper/          (mapper tests)
└── usecase/         (usecase impl tests)

core/database/src/test/
├── dao/             (DAO tests with Robolectric)
└── migration/       (migration tests)

app/src/test/
├── ui/              (ViewModel tests)
└── service/         (service tests)

app/src/androidTest/
└── (instrumentation tests)
```

#### Fake implementations の配置
- `FakeProfileRepository` → `core:domain` の testFixtures または `core:data/src/test/`
- テスト用モジュール `:core:testing` の作成も検討

---

## 依存関係グラフ

```
:app
  ├── :core:data
  │     ├── :core:domain
  │     │     └── :core:common
  │     ├── :core:database
  │     │     └── :core:common
  │     └── :core:network
  │           └── :core:common
  └── (直接参照も可)
        ├── :core:domain
        ├── :core:database
        └── :core:network
```

**依存ルール:**
- `:core:domain` は他のcoreモジュールに依存しない（:common以外）
- `:core:database` は :domain を知らない
- `:core:data` が :domain と :database を橋渡し
- `:app` は全モジュールにアクセス可能

---

## テスト戦略

### 各フェーズでの検証
```bash
# Phase完了ごとに実行
nix run .#test       # 全ユニットテスト
nix run .#build      # デバッグビルド
nix run .#lint       # Lintチェック
```

### 最終検証
```bash
nix run .#verify     # フルワークフロー (test → build → install → device check)
```

### テストカバレッジ目標
| モジュール | 目標カバレッジ |
|-----------|--------------|
| :core:domain | 70%+ |
| :core:data | 60%+ |
| :core:database | 50%+ |
| :app (ViewModels) | 70%+ |

---

## リスクと軽減策

| リスク | 影響度 | 軽減策 |
|--------|-------|--------|
| Hilt バインディングの破損 | 高 | Phase毎にビルド検証、EntryPoint活用 |
| Room マイグレーション | 中 | スキーマは変更なし、パッケージ移動のみ |
| 循環依存の発生 | 中 | 依存方向を厳格に管理、api/implementation分離 |
| テスト失敗 | 中 | 段階的移行、各Phase後に全テスト実行 |
| ビルド時間増加 | 低 | Convention Plugin でキャッシュ最適化 |

---

## 期待される成果

### ビルド時間
- **現状**: フルビルド時に全313ファイルをコンパイル
- **改善後**: 変更モジュールのみ再コンパイル、並列ビルド対応
- **期待削減率**: 20-40%

### テスト並列化
- 各モジュールのテストを独立して実行可能
- CI/CDパイプラインの高速化

### コードの保守性
- 関心の分離が明確化
- モジュール境界での強制的な依存チェック
- 新機能追加時の影響範囲が限定

### 再利用性
- `:core:*` モジュールを他プロジェクトで利用可能
- ライブラリとして切り出しが容易

---

## 最終的なモジュール構成

```
MoLe/
├── build-logic/                    # Convention plugins
│   └── src/main/kotlin/
├── app/                            # UI Layer (95 files)
│   ├── src/main/kotlin/.../
│   │   ├── di/
│   │   ├── ui/
│   │   ├── service/
│   │   └── App.kt
│   └── src/test/
├── core/
│   ├── common/                     # Shared utilities (15 files)
│   │   └── src/main/kotlin/.../
│   ├── domain/                     # Domain Layer (88 files)
│   │   └── src/main/kotlin/.../
│   │       ├── model/
│   │       ├── repository/
│   │       └── usecase/
│   ├── data/                       # Data Layer (70 files)
│   │   └── src/main/kotlin/.../
│   │       ├── repository/
│   │       ├── mapper/
│   │       ├── usecase/
│   │       └── json/
│   ├── database/                   # Database Layer (24 files)
│   │   └── src/main/kotlin/.../
│   │       ├── entity/
│   │       ├── dao/
│   │       └── MoLeDatabase.kt
│   └── network/                    # Network Layer (10 files)
│       └── src/main/kotlin/.../
├── gradle/
│   └── libs.versions.toml
├── settings.gradle.kts
└── build.gradle.kts
```

---

## 推奨される実装順序

1. **Phase 1**: プロジェクト基盤 (build-logic, settings.gradle.kts)
2. **Phase 2**: :core:common (最も依存される基盤)
3. **Phase 3**: :core:domain (ビジネスロジックの核)
4. **Phase 4**: :core:database (データ永続化)
5. **Phase 5**: :core:network (外部通信)
6. **Phase 6**: :core:data (橋渡し層)
7. **Phase 7**: :app リファクタリング (UI層)
8. **Phase 8**: テストの移行と最終検証

**総推定期間**: 7-12日（フルタイム作業の場合）

---

## 承認後の最初のアクション

1. build-logic ディレクトリの作成
2. Convention Plugin の実装
3. settings.gradle.kts の更新
4. :core:common モジュールの作成とファイル移動
5. ビルド検証: `nix run .#build`
