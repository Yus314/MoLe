# クイックスタート: Hilt 依存性注入

**作成日**: 2026-01-06
**ブランチ**: `005-hilt-di-setup`

## 概要

このガイドでは、MoLeプロジェクトでHilt依存性注入を使用する方法を説明します。

## 新しいViewModelの作成

### 1. ViewModelクラスに`@HiltViewModel`を追加

```kotlin
@HiltViewModel
class MyNewViewModel @Inject constructor(
    private val profileDAO: ProfileDAO,
    private val transactionDAO: TransactionDAO
) : ViewModel() {

    // ビジネスロジック
    fun loadProfiles(): LiveData<List<Profile>> {
        return profileDAO.getAllOrdered()
    }
}
```

### 2. Activity/Fragmentに`@AndroidEntryPoint`を追加

```kotlin
@AndroidEntryPoint
class MyNewActivity : AppCompatActivity() {

    private val viewModel: MyNewViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // viewModelは自動的に注入される
    }
}
```

## 利用可能な依存関係

### DatabaseModule提供

| 依存関係 | 型 | 用途 |
|----------|-----|------|
| DB | DB | Roomデータベースインスタンス |
| ProfileDAO | ProfileDAO | プロファイルCRUD |
| TransactionDAO | TransactionDAO | 取引CRUD |
| AccountDAO | AccountDAO | 勘定科目CRUD |
| AccountValueDAO | AccountValueDAO | 勘定科目残高 |
| TemplateHeaderDAO | TemplateHeaderDAO | テンプレートヘッダー |
| TemplateAccountDAO | TemplateAccountDAO | テンプレート勘定科目 |
| CurrencyDAO | CurrencyDAO | 通貨 |
| OptionDAO | OptionDAO | オプション設定 |

### DataModule提供

| 依存関係 | 型 | 用途 |
|----------|-----|------|
| Data | Data | グローバル状態（profiles, profile等） |

## ユニットテストの書き方

### MockKを使用したテスト例

```kotlin
class MyNewViewModelTest {

    private lateinit var mockProfileDAO: ProfileDAO
    private lateinit var mockTransactionDAO: TransactionDAO
    private lateinit var viewModel: MyNewViewModel

    @Before
    fun setUp() {
        mockProfileDAO = mockk()
        mockTransactionDAO = mockk()
        viewModel = MyNewViewModel(mockProfileDAO, mockTransactionDAO)
    }

    @Test
    fun `loadProfiles returns profiles from DAO`() {
        // Given
        val expectedProfiles = MutableLiveData<List<Profile>>()
        every { mockProfileDAO.getAllOrdered() } returns expectedProfiles

        // When
        val result = viewModel.loadProfiles()

        // Then
        assertSame(expectedProfiles, result)
        verify { mockProfileDAO.getAllOrdered() }
    }
}
```

### 重要ポイント

1. **Hiltはユニットテストで不要**: コンストラクタインジェクションを使用しているため、直接モックを渡せる
2. **テストは高速**: JVM上で実行、Androidコンテキスト不要
3. **テスト分離**: 各テストで新しいモックインスタンスを使用

## インストルメンテーションテストの書き方

### 基本セットアップ

```kotlin
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MyNewActivityTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    var activityRule = ActivityScenarioRule(MyNewActivity::class.java)

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun testActivityLaunches() {
        // テスト内容
    }
}
```

### テストデータベースの使用

```kotlin
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class]
)
object TestDatabaseModule {

    @Provides
    @Singleton
    fun provideTestDatabase(@ApplicationContext context: Context): DB {
        return Room.inMemoryDatabaseBuilder(context, DB::class.java)
            .allowMainThreadQueries()
            .build()
    }

    // 他のDAO提供メソッド...
}
```

## よくある質問

### Q: 既存のグローバルシングルトンアクセスはまだ使えますか？

**A**: はい、`DB.get()`や`Data`への直接アクセスは引き続き動作します。ただし、新しいコードではコンストラクタインジェクションを使用してください。

### Q: どのActivityに`@AndroidEntryPoint`が必要ですか？

**A**: Hilt注入されたViewModelを使用するすべてのActivityに必要です。現在はMainActivityのみがマークされています。

### Q: 循環依存エラーが出た場合は？

**A**: Hiltはコンパイル時に循環依存を検出します。依存グラフを見直し、インターフェース抽出や`Lazy<T>`注入を検討してください。

## ビルドとテスト

```bash
# ビルド
nix run .#build

# ユニットテスト
nix run .#test

# 検証（テスト + ビルド + インストール）
nix run .#verify
```
