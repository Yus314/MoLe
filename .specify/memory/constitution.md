<!--
===============================================================================
同期影響レポート
===============================================================================
バージョン変更: 1.4.0 → 1.5.0 (階層型アーキテクチャ原則の追加)

変更された原則:
- なし

追加されたセクション:
- X. 階層型アーキテクチャ（Googleアーキテクチャガイドライン準拠）
  - 関心の分離
  - 単方向データフロー
  - 信頼できる唯一の情報源（Single Source of Truth）
  - データレイヤ（Repositoryパターン）
  - ドメインレイヤ（UseCaseパターン）
  - UIレイヤ（ViewModel + Compose）
  - Coroutines/Flow

削除されたセクション: なし

更新が必要なテンプレート:
- .specify/templates/plan-template.md ✅ 互換性あり（Constitution Checkセクションが存在）
- .specify/templates/spec-template.md ✅ 互換性あり（User Scenarios & Testingセクションが整合）
- .specify/templates/tasks-template.md ✅ 互換性あり（Phase構造が柔軟）
- .specify/templates/checklist-template.md ✅ 互換性あり（汎用構造）
- .specify/templates/agent-file-template.md ✅ 互換性あり（汎用構造）

関連ドキュメント:
- CLAUDE.md ⚠ 更新推奨: 階層型アーキテクチャのパッケージ構成を追記すべき

フォローアップTODO:
- CLAUDE.mdに新しいパッケージ構成（data/domain/ui）を追記
- 既存コードの段階的なアーキテクチャ移行計画を策定

===============================================================================
-->

# MoLe (Mobile Ledger) 憲章

## コア原則

### I. コードの可読性とメンテナンス性

**絶対ルール**: コードの可読性とメンテナンス性は、すべての開発判断において最優先事項とする。

- すべてのコードは明確な命名規則による自己文書化が必須
- 複雑なロジックには説明コメントを必ず含める
- 関数は単一責任を持ち、簡潔であること
- コードの重複は適切な抽象化により排除する
- すべての公開APIにはKDocドキュメントが必須
- コード構造は確立されたプロジェクトパターンに一貫して従う

**根拠**: 可読性とメンテナンス性の高いコードは技術的負債を削減し、効率的なコラボレーションを
可能にし、プロジェクトの長期的な持続可能性を確保する。

### II. テスト駆動開発 (TDD)

**絶対ルール**: すべての新機能・修正はテスト駆動開発のサイクルに従う。
テストは実装より先に書き、失敗することを確認してから実装を行う。

#### TDDサイクル

1. **Red**: 失敗するテストを先に書く
2. **Green**: テストを通過する最小限の実装を行う
3. **Refactor**: コードを改善し、テストが通過し続けることを確認する

#### テストの原則

- すべての新しいクラスや関数はマージ前に単体テストが必須
- テストは正常系と異常系（エッジケース）の両方をカバーする
- 新規コードのテストカバレッジは最低80%を目標とすべき
- テストは独立性、再現性、高速性を持つこと
- テスト名はテスト対象の動作を明確に記述する
- 外部依存関係はモック化してユニットの分離を確保する

#### テストファーストの実践

- **新機能開発**: テストケースを定義 → 失敗確認 → 実装 → リファクタリング
- **バグ修正**: バグを再現するテストを書く → 失敗確認 → 修正 → テスト通過確認
- **リファクタリング**: 既存テストが通過することを確認 → 変更 → テスト再実行

#### テストカテゴリ

| カテゴリ | 目的 | 実行タイミング |
|----------|------|----------------|
| 単体テスト | 個々のクラス・関数の動作検証 | コミット前 |
| 統合テスト | コンポーネント間の連携検証 | PR作成前 |
| インストルメンテーションテスト | 実機/エミュレータでの動作検証 | リリース前 |

**根拠**: テスト駆動開発はリグレッションを防止し、設計を改善し、期待される動作を文書化する。
テストを先に書くことで、実装の方向性が明確になり、過剰な実装を防ぐ。

### III. 最小構築・段階的開発

**絶対ルール**: 開発は最小限の構築から始め、各ステップで検証しながら段階的に進める。
こまめなコミットにより進捗を記録し、変更履歴を追跡可能に保つ。

- 最小限の実行可能な実装から開始する
- 次に進む前に各変更が動作することを確認する
- 変更は原子的で、独立してレビュー可能であること
- 各段階では、コードベースを動作する状態に保つ
- 本番ブランチの未完成機能にはフィーチャーフラグを使用すべき
- **こまめなコミット**: 論理的な区切りごとにコミットを行い、大きな変更を蓄積しない
- コミットは小さく、焦点を絞り、自己完結的であること

**根拠**: 段階的開発とこまめなコミットはリスクを軽減し、デバッグを簡素化し、
問題発生時のロールバックを容易にする。継続的インテグレーションの実践を支え、
チームメンバー間の変更追跡を明確にする。

### IV. パフォーマンス最適化

**絶対ルール**: パフォーマンス改善は効果の大きいものから優先的に行う。

- 最適化の前にプロファイリングと計測を行う
- 高影響の最適化（多くのユーザーやクリティカルパスに影響）を最優先で対処する
- 早すぎる最適化は避ける
- パフォーマンス変更にはベンチマーク比較を含める
- 実行速度だけでなくメモリ使用量も考慮する
- データベースクエリは一般的なアクセスパターンに最適化する

**根拠**: 高影響の最適化に集中することで、投資した開発工数あたりのユーザー体験向上を
最大化できる。

### V. アクセシビリティ (Androidアクセシビリティガイドライン)

**絶対ルール**: すべてのユーザーインターフェースコンポーネントはGoogleの
Androidアクセシビリティガイドラインを満たすこと。

- すべてのUI要素にはcontentDescriptionまたはlabelForを設定する
- タッチターゲットは最低48x48dpを確保する（推奨）
- 色のコントラストは最小比率を満たすこと（通常テキスト4.5:1、大きいテキスト3:1）
- TalkBackでの操作を検証し、適切な読み上げ順序を確保する
- フォーカスナビゲーション（D-pad、キーボード）をサポートする
- LiveRegionを使用して動的コンテンツの変更をアナウンスする
- Accessibility Scannerでの検証を実施する
- カスタムViewにはAccessibilityNodeInfoを適切に実装する

**根拠**: Androidアクセシビリティガイドラインに準拠することで、TalkBackユーザーや
運動機能に制限のあるユーザーを含むすべてのAndroidユーザーがアプリを使用できる。

### VI. Kotlinコード標準

**絶対ルール**: すべてのコードはKotlinで記述する。Kotlinのイディオムとモダンな機能を
最大限に活用する。

- Kotlinのモダンな機能を使用する（null安全性、コルーチン、拡張関数）
- データ保持エンティティにはデータクラスを使用する
- 制限された階層の表現にはシールドクラスを使用すべき
- 非同期操作にはKotlinコルーチンを使用する
- Javaコードの新規導入は禁止（プロジェクトは100% Kotlin）

**根拠**: KotlinはJavaと比較してより安全で表現力豊かなコードを提供し、Androidとの
統合が優れており、ボイラープレートを削減する。プロジェクト全体でKotlinに統一することで
一貫性が向上し、メンテナンスが容易になる。

#### Kotlinコードレビュー基準

コードレビューでは以下のKotlin固有の観点を確認する：

**Null安全性**
- `!!`演算子の使用は原則禁止。使用する場合は理由をコメントで明記する
- エルビス演算子（`?:`）やセーフコール（`?.`）を適切に活用する

**不変性**
- `var`より`val`を優先する。可変が必要な場合は理由を明確にする
- `MutableList`/`MutableMap`より不変コレクションを優先する
- データクラスのプロパティは原則`val`で定義する

**スコープ関数**
- `let`、`apply`、`also`、`run`、`with`を用途に応じて適切に使い分ける
- スコープ関数のネストは2段階までとし、可読性を維持する
- 過度なチェーンは避け、必要に応じてローカル変数に分割する

**コルーチン**
- `GlobalScope`の使用は禁止。適切なスコープ（`viewModelScope`等）を使用する
- UIスレッドでの長時間処理は`withContext(Dispatchers.IO)`で回避する
- 例外処理を適切に実装し、エラーがユーザーに通知されることを確認する
- キャンセルに対応し、不要な処理が継続しないことを確認する

### VII. Nix開発環境

**絶対ルール**: 開発環境はNixを使用して再現可能でなければならない。

- すべてのビルド依存関係はflake.nixで指定する
- `nix develop`または`nix develop .#fhs`で完全な開発環境を提供する
- ビルド手順は追加のシステムパッケージなしでNixOSで動作すること
- CI/CDパイプラインは一貫性のために同じNix環境を使用すべき
- Android SDKとツールチェーンのバージョンはNix設定で固定する
- 環境セットアップドキュメントはNix設定と同期して最新に保つ

**根拠**: Nixはすべての開発者マシンとCIシステム間で再現可能なビルドを保証し、
「自分の環境では動く」問題を排除する。

### VIII. 依存性注入 (Hilt)

**絶対ルール**: 依存関係の管理にはHiltを使用し、コンストラクタインジェクションを優先する。
これによりテスト容易性と疎結合を実現する。

#### DIの原則

- すべての新規ViewModelは`@HiltViewModel`と`@Inject constructor`を使用する
- すべての新規ActivityおよびFragmentは`@AndroidEntryPoint`でマークする
- 依存関係はコンストラクタインジェクションで受け取る（フィールドインジェクションより優先）
- シングルトンスコープは本当に必要な場合のみ使用する
- テスト時はDIモジュールを差し替えてモックを注入する

#### 利用可能なDIモジュール

| モジュール | 提供する依存関係 |
|------------|------------------|
| DatabaseModule | DB, ProfileDAO, TransactionDAO, AccountDAO, AccountValueDAO, TemplateHeaderDAO, TemplateAccountDAO, CurrencyDAO, OptionDAO |
| DataModule | Data（グローバル状態） |

#### テストにおけるDI

```kotlin
// プロダクションコード
@HiltViewModel
class MyViewModel @Inject constructor(
    private val profileDAO: ProfileDAO,
    private val data: Data
) : ViewModel()

// テストコード - モジュール差し替え
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class]
)
@Module
object TestDatabaseModule {
    @Provides
    fun provideProfileDAO(): ProfileDAO = mockk()
}
```

#### 移行ガイドライン

- 新規コードはDIを使用する
- 既存の`DB.get()`や`Data`への直接アクセスは動作するが、新規コードでは使用しない
- リファクタリング時は段階的にDIに移行する

**根拠**: 依存性注入はテスト容易性を大幅に向上させ、コンポーネント間の疎結合を実現する。
Hiltを使用することでボイラープレートを最小化しつつ、Androidライフサイクルとの統合が容易になる。
TDDの実践においてモックの注入が容易になり、単体テストの品質が向上する。

### IX. 静的解析とリント

**絶対ルール**: すべてのKotlinコードは静的解析ツールによるチェックを通過すること。
コードスタイルの一貫性と潜在的なバグの早期発見を自動化する。

#### リントツール構成（flake.nixで管理）

| ツール | 目的 | 実行タイミング |
|--------|------|----------------|
| ktlint | Kotlinコードスタイルチェック | コミット前（pre-commit hook） |
| detekt | Kotlin静的解析 | コミット前（pre-commit hook） |
| Android Lint | Android固有の問題検出 | CI/手動実行（`nix run .#lint`） |

#### Pre-commit Hooks（自動実行）

開発環境（`nix develop`）に入ると、pre-commit hooksが自動的にインストールされる。

```bash
# 手動実行コマンド
pre-commit run --all-files    # 全ファイルをチェック
pre-commit run ktlint         # ktlintのみ実行
pre-commit run detekt         # detektのみ実行
```

#### ktlintルール設定

設定ファイル: `.editorconfig`

永久無効化されているルール（互換性のため）:

| ルール | 無効化理由 |
|--------|-----------|
| `filename` | Abstract* プレフィックスは意図的 |
| `property-naming` | JSONフィールド名との互換性（ptransaction_ 等）|
| `backing-property-naming` | 特殊なパターンの許容 |
| `function-naming` | 特殊な関数名の許容 |
| `no-wildcard-imports` | Android Studioデフォルト動作 |
| `package-name` | APIバージョンサフィックス（v1_14 等）|
| `enum-entry-name-case` | JSONシリアライズ互換性 |
| `kdoc` | KDocスタイル強制なし |

#### detekt設定

設定ファイル: `detekt.yml`
- `maxIssues: -1`: 警告のみ出力、エラーにしない（段階的改善のため）
- 設定ファイルでルールをカスタマイズ可能

#### Android Lint（CI用）

```bash
# Gradle経由で実行
nix run .#lint

# FHS環境内で直接実行
./gradlew lintDebug
```

出力: `app/build/reports/lint-results-debug.html`

Android Lintはコミット前hookには含まれない（実行時間が長いため）。
CI/CDパイプラインまたは手動で実行する。

#### テストとの関係

| 検証タイプ | 実行タイミング | 目的 |
|------------|----------------|------|
| ktlint | コミット前 | コードスタイル統一 |
| detekt | コミット前 | 静的解析（複雑度、コードスメル） |
| 単体テスト | コミット前 | ロジックの正確性検証 |
| Android Lint | PR作成前/CI | Android固有の問題検出 |
| 統合テスト | PR作成前 | コンポーネント間連携検証 |

#### Nixコマンド一覧

```bash
nix develop          # 開発シェル（pre-commit自動インストール）
nix run .#test       # ユニットテスト実行
nix run .#lint       # Android Lint実行
nix run .#build      # デバッグAPKビルド
nix run .#verify     # フルワークフロー（test → build → install）
```

**根拠**: 静的解析ツールは人間のレビューでは見落としがちなコードスタイル違反や
潜在的バグを自動検出する。pre-commitで自動実行することで、問題のあるコードが
リポジトリに混入することを防ぎ、コードレビューの負担を軽減する。
Nixで環境を統一することで、開発者間やCI環境でのツールバージョンの差異を排除する。

### X. 階層型アーキテクチャ（Googleアーキテクチャガイドライン）

**絶対ルール**: Googleが推奨する階層型アーキテクチャを採用する。
関心の分離、単方向データフロー、信頼できる唯一の情報源（Single Source of Truth）を
厳守し、テスト容易性と保守性を最大化する。

#### アーキテクチャの階層

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                              UI Layer                                    │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  UI Elements (Compose)  ←─────────  ViewModel                    │    │
│  │  - 状態の表示のみ           - UiStateの保持・公開               │    │
│  │  - イベントの発火のみ       - ユーザーイベントの処理             │    │
│  └─────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────┬──────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           Domain Layer (Optional)                        │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  UseCase                                                         │    │
│  │  - 複雑なビジネスロジックのカプセル化                            │    │
│  │  - 複数Repositoryの組み合わせ                                   │    │
│  │  - 再利用可能なビジネスルール                                    │    │
│  └─────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────┬──────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                             Data Layer                                   │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  Repository                                                      │    │
│  │  - データアクセスの抽象化                                        │    │
│  │  - 単一のデータソースを公開（Single Source of Truth）           │    │
│  │  - キャッシュ戦略の管理                                         │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  Data Sources                                                    │    │
│  │  - Local: Room Database (DAO)                                    │    │
│  │  - Remote: Retrofit/OkHttp (API)                                 │    │
│  └─────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
```

#### パッケージ構成

```text
app/src/main/kotlin/net/ktnx/mobileledger/
├── data/                    # Data Layer
│   ├── repository/          # Repository実装
│   │   ├── ProfileRepository.kt
│   │   ├── TransactionRepository.kt
│   │   └── AccountRepository.kt
│   ├── local/               # ローカルデータソース
│   │   ├── dao/             # Room DAO（既存）
│   │   └── entity/          # Room Entity（既存）
│   └── remote/              # リモートデータソース
│       └── api/             # Retrofit API（既存）
│
├── domain/                  # Domain Layer
│   ├── usecase/             # UseCase実装
│   │   ├── profile/
│   │   ├── transaction/
│   │   └── account/
│   └── model/               # ドメインモデル
│
├── ui/                      # UI Layer
│   ├── main/                # メイン画面
│   │   ├── MainScreen.kt
│   │   ├── MainViewModel.kt
│   │   └── MainUiState.kt
│   ├── transaction/         # 取引画面
│   ├── profile/             # プロファイル画面
│   ├── theme/               # Composeテーマ
│   └── components/          # 共通UIコンポーネント
│
└── di/                      # Hilt DIモジュール
    ├── DatabaseModule.kt
    ├── DataModule.kt
    ├── RepositoryModule.kt  # 新規追加
    └── UseCaseModule.kt     # 新規追加（必要に応じて）
```

#### 単方向データフロー（Unidirectional Data Flow）

```text
Events (User actions)
        │
        ▼
┌───────────────┐     ┌─────────────────┐     ┌────────────────┐
│   UI Element  │────►│   ViewModel     │────►│   UseCase/     │
│   (Compose)   │     │                 │     │   Repository   │
└───────────────┘     └─────────────────┘     └────────────────┘
        ▲                     │                        │
        │                     │                        │
        │              ┌──────▼──────┐                │
        └──────────────│   UiState   │◄───────────────┘
                       │   (Flow)    │
                       └─────────────┘

State (Immutable)
```

**データフローの原則**:
- UIはViewModelからUiState（Flow）を収集して表示する
- ユーザーアクションはイベントとしてViewModelに伝達する
- ViewModelはUseCase/Repositoryを呼び出しデータを更新する
- データ変更はFlowを通じてUIに自動的に反映される

#### Repository パターン

**絶対ルール**: アプリケーションデータはRepositoryを通じて公開する。
ViewModelはDAOに直接アクセスしない。

```kotlin
// Repository インターフェース
interface ProfileRepository {
    fun getAllProfiles(): Flow<List<Profile>>
    suspend fun getProfileById(id: Long): Profile?
    suspend fun insertProfile(profile: Profile): Long
    suspend fun updateProfile(profile: Profile)
    suspend fun deleteProfile(profile: Profile)
}

// Repository 実装
class ProfileRepositoryImpl @Inject constructor(
    private val profileDAO: ProfileDAO
) : ProfileRepository {
    override fun getAllProfiles(): Flow<List<Profile>> =
        profileDAO.getAllProfiles()

    override suspend fun getProfileById(id: Long): Profile? =
        profileDAO.getProfileById(id)
    // ...
}

// ViewModel での使用
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository  // DAOではなくRepository
) : ViewModel() {
    val profiles: StateFlow<List<Profile>> = profileRepository
        .getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
```

**Repository の責務**:
- データソース（ローカル/リモート）の切り替えを隠蔽する
- キャッシュ戦略を実装する（必要に応じて）
- データの整合性を保証する
- 上位層にFlowでリアクティブなデータを公開する

#### UseCase パターン（ドメインレイヤ）

**絶対ルール**: 複雑なビジネスロジックはUseCaseにカプセル化する。
単純なCRUD操作のみの場合はUseCaseをスキップしてRepositoryを直接使用してもよい。

```kotlin
// UseCase の例
class SyncTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(profileId: Long): Result<SyncResult> {
        val profile = profileRepository.getProfileById(profileId)
            ?: return Result.failure(ProfileNotFoundException())

        return try {
            val transactions = transactionRepository.fetchRemoteTransactions(profile)
            transactionRepository.saveTransactions(transactions)
            Result.success(SyncResult(transactions.size))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**UseCase を作成する条件**:
- 複数のRepositoryを組み合わせる必要がある場合
- ビジネスルールが複雑で再利用可能な場合
- 同じロジックを複数のViewModelで使用する場合

**UseCase を作成しない条件**:
- 単純なCRUD操作（Repository直接呼び出しで十分）
- ロジックが1つのViewModelでのみ使用される場合

#### UiState パターン

**絶対ルール**: UIの状態は不変のデータクラス（UiState）として表現する。
ViewModelはStateFlowでUiStateを公開し、UIはこれを収集して表示する。

```kotlin
// UiState の定義
data class MainUiState(
    val isLoading: Boolean = false,
    val profiles: List<Profile> = emptyList(),
    val selectedProfile: Profile? = null,
    val error: String? = null
)

// イベント（ユーザーアクション）
sealed class MainEvent {
    data class ProfileSelected(val profileId: Long) : MainEvent()
    object RefreshRequested : MainEvent()
    object ErrorDismissed : MainEvent()
}

// エフェクト（1回限りのナビゲーションやダイアログ）
sealed class MainEffect {
    data class NavigateToProfile(val profileId: Long) : MainEffect()
    data class ShowSnackbar(val message: String) : MainEffect()
}

// ViewModel
@HiltViewModel
class MainViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _effects = Channel<MainEffect>()
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: MainEvent) {
        when (event) {
            is MainEvent.ProfileSelected -> selectProfile(event.profileId)
            is MainEvent.RefreshRequested -> refresh()
            is MainEvent.ErrorDismissed -> dismissError()
        }
    }

    private fun selectProfile(profileId: Long) {
        viewModelScope.launch {
            _effects.send(MainEffect.NavigateToProfile(profileId))
        }
    }
}
```

#### Coroutines と Flow

**絶対ルール**: 非同期処理にはKotlin CoroutinesとFlowを使用する。
RxJavaやコールバックベースの非同期処理は新規コードでは使用しない。

**Coroutines のルール**:
- `viewModelScope`を使用する（`GlobalScope`禁止）
- 長時間処理は`withContext(Dispatchers.IO)`で実行する
- 例外処理は`try-catch`または`runCatching`で適切に行う
- キャンセルに対応する（`isActive`チェック、`ensureActive()`）

**Flow のルール**:
- Repositoryはデータを`Flow`で公開する
- ViewModelは`Flow`を`StateFlow`に変換して公開する
- UIは`collectAsState()`でFlowを収集する
- `stateIn()`で適切な`SharingStarted`ポリシーを選択する

```kotlin
// Repository
fun getAllProfiles(): Flow<List<Profile>>

// ViewModel
val profiles: StateFlow<List<Profile>> = profileRepository
    .getAllProfiles()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

// Compose UI
@Composable
fun ProfileList(viewModel: ProfileViewModel = hiltViewModel()) {
    val profiles by viewModel.profiles.collectAsState()
    // UIを描画
}
```

#### 移行ガイドライン

**新規コード**: 必ず階層型アーキテクチャに従う

**既存コード**: 段階的に移行する

1. **Phase 1**: 新規機能でRepositoryパターンを導入
2. **Phase 2**: 既存ViewModelを段階的にRepository経由に移行
3. **Phase 3**: 複雑なロジックをUseCaseに抽出
4. **Phase 4**: 全DAOアクセスをRepository経由に統一

**既存コードとの共存**:
- 既存のDAO直接アクセスは動作するが、新規コードでは使用しない
- リファクタリング時に段階的にRepository経由に移行する

**根拠**: Googleが推奨する階層型アーキテクチャは、関心の分離を徹底することで
テスト容易性、保守性、拡張性を最大化する。単方向データフローにより状態管理が
予測可能になり、バグの発生を抑制する。Repositoryパターンによりデータソースの
実装詳細を隠蔽し、将来的なデータソース変更にも柔軟に対応できる。

## 開発ワークフロー

### TDD開発フロー

```
┌─────────────────────────────────────────────────────────────┐
│  1. 要件分析: テストケースの洗い出し                         │
├─────────────────────────────────────────────────────────────┤
│  2. RED: 失敗するテストを書く                                │
│     - テストが失敗することを確認                             │
│     - テストが正しい理由で失敗することを確認                 │
├─────────────────────────────────────────────────────────────┤
│  3. GREEN: テストを通過する最小限のコードを書く              │
│     - 過剰な実装は避ける                                     │
│     - テストが通過することを確認                             │
├─────────────────────────────────────────────────────────────┤
│  4. REFACTOR: コードを改善                                   │
│     - 重複を排除                                             │
│     - 命名を改善                                             │
│     - テストが通過し続けることを確認                         │
├─────────────────────────────────────────────────────────────┤
│  5. コミット: 論理的な区切りでコミット                       │
│     - pre-commit hookがktlint/detektを自動実行              │
├─────────────────────────────────────────────────────────────┤
│  6. 次のテストケースへ（2に戻る）                           │
└─────────────────────────────────────────────────────────────┘
```

### コードレビュープロセス

- すべての変更は少なくとも1人の他の開発者によるレビューが必須
- レビューはすべての憲章原則への準拠を確認する
- 自動チェック（テスト、リント、フォーマット）はレビュー前に通過すること
- レビューコメントはマージ前に対応する
- ドキュメントのみの変更にはセルフレビューが許可される

### テストゲート

- マージ前に単体テストが通過すること
- 統合テスト（該当する場合）が通過すること
- UI変更にはアクセシビリティチェックを実施する
- クリティカルパスの変更にはパフォーマンス回帰テストを実行すべき
- **新規コードには対応するテストが含まれていること**
- **バグ修正にはバグを再現するテストが含まれていること**
- **ktlint/detektのチェックを通過すること（pre-commit hookで自動実行）**

### コミット基準

**こまめなコミットの実践**:

- コミットは論理的な区切りごとに行う（ファイル単位、機能単位、修正単位）
- 作業途中でも動作する状態であればコミットする
- 1時間以上コミットしない状況は避ける
- コミットを先延ばしにして大きな変更を蓄積しない
- **テストとその実装は同じコミットに含める**
- **pre-commit hookが失敗した場合は修正してから再コミットする**

**コミットメッセージ**:

- コミットメッセージはconventional commit形式に従う
- 各コミットは単一の論理的変更を表す
- 作業中のコミットはマージ前にスカッシュする

**根拠**: こまめなコミットにより、問題発生時の原因特定が容易になり、
変更履歴が明確になる。また、作業の進捗が可視化され、
万が一の作業喪失リスクを最小化できる。

## 品質基準

### コード品質

- Kotlinコードはktlintフォーマットチェックを通過すること
- detekt静的解析の警告は確認し、必要に応じて対処する
- 静的解析の警告はマージ前に対処する
- TODOコメントにはトラッキングissue参照を含める
- 不要なコードはコメントアウトではなく削除する

### テスト品質

- テストは独立して実行可能であること（他のテストに依存しない）
- テストは決定論的であること（毎回同じ結果を返す）
- テストは高速であること（単体テストは100ms以内）
- テスト名は「何をテストしているか」を明確に記述する
- Arrangeー（準備）、Act（実行）、Assert（検証）の構造に従う
- モックは最小限に抑え、必要な依存関係のみをモック化する

### ドキュメント

- 公開APIにはKDocドキュメントが必須
- セットアッププロセスの変更時にREADMEを更新する
- アーキテクチャ決定はADR形式で文書化すべき
- ユーザー向け変更には関連するユーザードキュメントを更新する

### パフォーマンス

- アプリ起動時間は参照デバイスで2秒以下を維持する
- UI操作はレスポンシブに感じる100ms以内で完了する
- メモリリークはリリース前に修正する
- バッテリー消費の多い操作は最小化する

## ガバナンス

### 修正プロセス

1. 専用のissueまたはディスカッションで修正を提案する
2. チームレビューとフィードバックのために最低7日間を設ける
3. アクティブなメンテナーからの合意を得る
4. 破壊的変更に対しては移行計画を文書化する
5. 影響を受けるすべてのテンプレートとドキュメントを更新する

### バージョニングポリシー

- **MAJOR**: 原則の削除または根本的な再定義
- **MINOR**: 新しい原則の追加または重要なガイダンスの拡張
- **PATCH**: 明確化、誤字修正、非意味的な改良

### コンプライアンスレビュー

- 憲章への準拠はコードレビューで確認する
- 定期的な監査で原則への準拠を評価すべき
- 違反は文書化され、是正される
- 例外は正当化され、機能計画のComplexity Trackingセクションに文書化する

**バージョン**: 1.5.0 | **批准日**: 2026-01-05 | **最終修正日**: 2026-01-10
