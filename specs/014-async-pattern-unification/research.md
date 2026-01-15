# Research: 非同期処理パターンの統一

**Feature Branch**: `014-async-pattern-unification`
**Date**: 2026-01-15

## Executive Summary

MoLeコードベースには**4つの異なる非同期パターン**が混在しており、開発者の学習コストとメンテナンスの複雑さを増大させている。調査の結果、既存のThread継承パターンをKotlin Coroutinesに完全移行することが可能であり、外部インターフェース（UseCaseインターフェース）は既にCoroutinesベースで定義済みのため、内部実装の移行に集中できることが判明した。

---

## 1. 既存の非同期パターン分析

### 1.1 Pattern 1: Thread継承 (Legacy - 移行優先度: 高)

**特徴**:
- `Thread`クラスを直接継承
- `run()`メソッドでメインロジックを実装
- 手動で`start()`, `join()`, `interrupt()`を呼び出し
- キャンセルは`interrupt()`に依存
- コールバックベースの結果通知

**該当ファイル**:

| ファイル | クラス | 行数 | 複雑度 | 状態 |
|---------|--------|------|--------|------|
| `async/RetrieveTransactionsTask.kt` | `RetrieveTransactionsTask : Thread()` | 813 | **非常に高** | ラッパー経由で使用中 |
| `async/SendTransactionTask.kt` | `SendTransactionTask : Thread()` | 100+ | 高 | ラッパー経由で使用中 |
| `backup/ConfigIO.kt` | `ConfigIO : Thread()` | 100+ | 高 | 抽象基底クラス |
| `backup/ConfigReader.kt` | `ConfigReader : ConfigIO` | 80+ | 中 | ラッパー経由で使用中 |
| `backup/ConfigWriter.kt` | `ConfigWriter : ConfigIO` | 69 | 中 | ラッパー経由で使用中 |

**問題点**:
- `RetrieveTransactionsTask`は813行のモノリシックなクラス
- `ConfigReader`内で`runBlocking`を使用（アンチパターン）
- 構造化されたキャンセルトークンがない
- テストに実際のネットワーク/ファイルI/Oが必要

### 1.2 Pattern 2: ExecutorService + コールバック (Legacy - 移行優先度: 中)

**特徴**:
- `Executors.newFixedThreadPool()`を使用
- コールバックベースの結果通知
- メインスレッドへのマーシャリングが必要

**該当ファイル**:

| ファイル | オブジェクト | 行数 | 複雑度 |
|---------|-------------|------|--------|
| `async/GeneralBackgroundTasks.kt` | `object GeneralBackgroundTasks` | 64 | 低 |

**問題点**:
- コールバック地獄の可能性
- キャンセルサポートなし
- アプリ全体で共有されるシングルトン

### 1.3 Pattern 3: レガシーコールバック (Legacy - 使用頻度: 低)

**特徴**:
- `fun interface`によるコールバック
- エラーと結果が別パラメータ
- 呼び出し元でメインスレッドマーシャリングが必要

**該当ファイル**:

| ファイル | インターフェース | 使用箇所 |
|---------|-----------------|----------|
| `async/TaskCallback.kt` | `fun interface TaskCallback` | `SendTransactionTask` |
| `dao/AsyncResultCallback.kt` | `fun interface AsyncResultCallback<T>` | Room DAO非同期操作 |

### 1.4 Pattern 4: 現代的Coroutines (現在のベストプラクティス)

**特徴**:
- `suspend`関数と`withContext(Dispatchers.IO)`
- `Flow<T>`でストリーミング結果
- `StateFlow<T>`で状態管理
- `CancellationException`による適切なキャンセル
- `viewModelScope`とのViewModel統合

**実装済みコンポーネント**:

| コンポーネント | ファイル | パターン | 状態 |
|---------------|---------|----------|------|
| TransactionSyncer | `domain/usecase/TransactionSyncer.kt` | Flowベース | インターフェース定義済み |
| TransactionSyncerImpl | `domain/usecase/TransactionSyncerImpl.kt` | `callbackFlow`でラップ | レガシーをラップ中 |
| TransactionSender | `domain/usecase/TransactionSender.kt` | suspendでResult返却 | インターフェース定義済み |
| TransactionSenderImpl | `domain/usecase/TransactionSenderImpl.kt` | `suspendCancellableCoroutine` | レガシーをラップ中 |
| ConfigBackup | `domain/usecase/ConfigBackup.kt` | suspendでResult返却 | インターフェース定義済み |
| ConfigBackupImpl | `domain/usecase/ConfigBackupImpl.kt` | `suspendCancellableCoroutine` | レガシーをラップ中 |
| DatabaseInitializer | `domain/usecase/DatabaseInitializer.kt` | suspend関数 | インターフェース定義済み |
| DatabaseInitializerImpl | `domain/usecase/DatabaseInitializerImpl.kt` | `withContext(Dispatchers.IO)` | **移行完了** |
| VersionDetector | `domain/usecase/VersionDetector.kt` | suspendでResult返却 | インターフェース定義済み |
| VersionDetectorImpl | `domain/usecase/VersionDetectorImpl.kt` | `withContext(Dispatchers.IO)` | **移行完了** |

---

## 2. 移行準備状況の評価

### 2.1 複雑度ランキング（簡単→難しい順）

1. **VersionDetector** ✅ 移行完了
   - 状態: 既に純粋なCoroutines実装
   - パターン: シンプルなHTTP + パース
   - リスク: 非常に低

2. **DatabaseInitializer** ✅ 移行完了
   - 状態: 既に純粋なCoroutines実装
   - パターン: Repositoryアクセス
   - リスク: 非常に低

3. **ConfigBackup** 🟡 移行予定
   - 状態: ConfigIOをラップ中
   - ブロッカー: ConfigIOがThread継承、ConfigReaderで`runBlocking`使用
   - 作業: ConfigIO階層をsuspend関数に変換
   - リスク: 中（ファイルI/O、例外処理）

4. **TransactionSender** 🟡 移行予定
   - 状態: SendTransactionTaskをラップ中
   - ブロッカー: SendTransactionTask (Thread)の完全書き換え必要
   - 作業: SendTransactionTaskのロジックをsuspend関数に変換
   - リスク: 中（ネットワークI/O、認証、複雑なリクエスト構築）

5. **TransactionSyncer** 🔴 移行予定（最も複雑）
   - 状態: RetrieveTransactionsTaskを複雑にラップ中
   - ブロッカー: RetrieveTransactionsTask (813行のThreadクラス)がモノリシック
   - 作業: パース処理の抽出、suspend関数への変換、Repository統合
   - リスク: 高（最大のコード量、複雑な状態管理、進捗レポート）

### 2.2 移行に必要な作業

| コンポーネント | 必要な作業 | 推定コード削減 |
|---------------|-----------|---------------|
| VersionDetector | なし（完了） | - |
| DatabaseInitializer | なし（完了） | - |
| ConfigBackup | ConfigIO/Reader/Writer → suspend関数 | 約30% |
| TransactionSender | SendTransactionTask → suspend関数 | 約25% |
| TransactionSyncer | RetrieveTransactionsTask分解 + suspend関数 | 約20% |

---

## 3. ブロッカー分析

### 3.1 ハードブロッカー

1. **RetrieveTransactionsTaskの450行のrun()メソッド**
   - 問題: パース処理がThreadライフサイクルに密結合
   - 解決策: テスト可能なユニットにパース処理を抽出

2. **TransactionSyncerImplのThread.join()待機**
   - 問題: タスク完了を待つためだけにラッパースレッドを作成
   - 解決策: タスク自体をsuspendに変換

3. **ConfigReaderのrunBlocking()**
   - 問題: Thread内でCoroutinesをブロッキング
   - 解決策: ConfigReaderをsuspend関数に変換

4. **パーサーのRetrieveTransactionsTask依存**
   - 問題: AccountListParser, TransactionListParserがThreadクラスに密結合
   - 解決策: 純粋関数またはCoroutines対応クラスに抽出

### 3.2 ソフトブロッカー

1. **BackgroundTaskManagerのThread参照保持**
   - 問題: Taskベースの進捗管理向け設計
   - 解決策: suspend/Flowベースの進捗にも対応するよう拡張

2. **コールバックベースの進捗レポート**
   - 問題: Flow emissionに置き換えるべき
   - 解決策: Flow collectorsで進捗を受信

---

## 4. テストインフラストラクチャ

### 4.1 利用可能なFake実装

以下のFake実装が既に準備済み：

- `FakeTransactionSyncer.kt` - 同期処理テスト用
- `FakeTransactionSender.kt` - 取引送信テスト用
- `FakeConfigBackup.kt` - バックアップ/リストアテスト用
- `FakeDatabaseInitializer.kt` - DB初期化テスト用
- `FakeVersionDetector.kt` - バージョン検出テスト用

### 4.2 テストパターン

```kotlin
@Test
fun `sync success updates UI state`() = runTest {
    fakeTransactionSyncer.shouldSucceed = true
    viewModel.startSync(profile)
    assertEquals(SyncState.Completed, viewModel.syncState.value)
}
```

- 特徴: `runTest {}`でCoroutinesテストをサポート
- メリット: 実際のネットワーク/ディスクI/O不要

---

## 5. 決定事項

### Decision 1: 移行アプローチ

**決定**: 段階的移行（仕様で確定済み）

**理由**:
- リスク最小化
- 各段階で動作確認可能
- ロールバックが容易

**却下した代替案**:
- 一括移行: リスクが高すぎる
- ハイブリッド永続: メンテナンスコストが継続

### Decision 2: 移行順序

**決定**: 複雑度順（簡単→難しい）

1. VersionDetector（完了）
2. DatabaseInitializer（完了）
3. ConfigBackup
4. TransactionSender
5. TransactionSyncer

**理由**:
- パターンを確立してから複雑な処理に適用
- 学習曲線の最適化
- 早期のフィードバック獲得

### Decision 3: 古いコードの削除タイミング

**決定**: 全処理の移行完了後に一括削除（仕様で確定済み）

**理由**:
- ロールバック容易性を優先
- 移行期間中の安全ネット

### Decision 4: 外部インターフェース互換性

**決定**: 新インターフェース導入済み + 既存は非推奨

**理由**:
- 呼び出し元（ViewModel等）は段階的に移行可能
- 新しいコードは即座に新パターンを使用可能

---

## 6. 参考ファイル

### 移行テンプレート（参考用）

**純粋Coroutines実装例**:
- `domain/usecase/VersionDetectorImpl.kt` - シンプルなHTTP処理
- `domain/usecase/DatabaseInitializerImpl.kt` - Repository呼び出し

**現在のラッパー実装**:
- `domain/usecase/TransactionSyncerImpl.kt` - callbackFlow + Thread
- `domain/usecase/TransactionSenderImpl.kt` - suspendCancellableCoroutine + Thread
- `domain/usecase/ConfigBackupImpl.kt` - suspendCancellableCoroutine + Thread

**削除対象**:
- `async/RetrieveTransactionsTask.kt` - 813行のThreadクラス
- `async/SendTransactionTask.kt` - Thread継承
- `backup/ConfigIO.kt` - Thread継承の基底クラス
- `backup/ConfigReader.kt` - Thread継承 + runBlocking
- `backup/ConfigWriter.kt` - Thread継承

---

## 7. 未解決の課題

すべての技術的課題は調査により解決済み。移行作業の実行段階に進むことが可能。
