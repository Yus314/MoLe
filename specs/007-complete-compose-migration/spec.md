# Feature Specification: Complete Compose Migration

**Feature Branch**: `007-complete-compose-migration`
**Created**: 2026-01-10
**Status**: Draft
**Input**: User description: "Viewの完全移行：サポート画面、ダイアログ、アダプターなどを全てJetpack Composeに移行"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - ダイアログのCompose化 (Priority: P1)

ユーザーが日付選択や通貨選択などのダイアログを使用する際、従来のFragment DialogではなくCompose Dialogで一貫したUI体験を提供する。

**Why this priority**: ダイアログは取引登録画面やプロファイル編集など、主要機能で頻繁に使用される。Compose化により、既存のCompose画面との統合がシームレスになり、コードの一貫性が向上する。

**Independent Test**: 取引登録画面で日付選択、通貨選択を行い、ダイアログが正常に表示・動作することを確認できる。

**Acceptance Scenarios**:

1. **Given** 取引登録画面が表示されている, **When** 日付フィールドをタップする, **Then** Composeベースの日付ピッカーダイアログが表示され、日付を選択できる
2. **Given** 取引登録画面が表示されている, **When** 通貨選択が必要な場面で通貨ボタンをタップする, **Then** Composeベースの通貨選択ダイアログが表示され、通貨を選択できる
3. **Given** ダイアログが表示されている, **When** 外部をタップまたは戻るボタンを押す, **Then** ダイアログが閉じ、選択がキャンセルされる

---

### User Story 2 - スプラッシュ画面のCompose化 (Priority: P2)

アプリ起動時のスプラッシュ画面をComposeで実装し、起動体験を統一する。

**Why this priority**: スプラッシュ画面はユーザーが最初に目にする画面であり、アプリの第一印象を決める。ただし、機能的な影響は小さいため、ダイアログより優先度が低い。

**Independent Test**: アプリを起動し、スプラッシュ画面が表示され、メイン画面への遷移が正常に行われることを確認できる。

**Acceptance Scenarios**:

1. **Given** アプリがインストールされている, **When** アプリを起動する, **Then** Composeベースのスプラッシュ画面が表示される
2. **Given** スプラッシュ画面が表示されている, **When** 初期化処理が完了する, **Then** スムーズにメイン画面へ遷移する
3. **Given** アプリが初回起動である, **When** プロファイルが未設定の場合, **Then** ウェルカム画面またはプロファイル作成画面へ遷移する

---

### User Story 3 - バックアップ画面のCompose化 (Priority: P3)

バックアップ・リストア機能の画面をComposeで再実装し、UIの一貫性を確保する。

**Why this priority**: バックアップ機能は使用頻度が低いが、データ保護という重要な機能を担う。主要画面の移行が完了した後に対応する。

**Independent Test**: バックアップ画面を開き、バックアップの作成・リストア操作が正常に動作することを確認できる。

**Acceptance Scenarios**:

1. **Given** 設定メニューが表示されている, **When** バックアップオプションをタップする, **Then** Composeベースのバックアップ画面が表示される
2. **Given** バックアップ画面が表示されている, **When** バックアップ作成ボタンをタップする, **Then** バックアップが作成され、成功メッセージが表示される
3. **Given** バックアップファイルが存在する, **When** リストアボタンをタップする, **Then** リストア確認ダイアログが表示され、リストアを実行できる

---

### User Story 4 - レガシーアダプターの削除 (Priority: P4)

使用されていないレガシーのRecyclerViewアダプターを削除し、コードベースをクリーンアップする。

**Why this priority**: アダプターの削除は機能に直接影響しないが、コードの保守性向上とビルドサイズ削減に貢献する。他の移行完了後に実施する。

**Independent Test**: アプリ全体の機能テストを実行し、削除によるリグレッションがないことを確認できる。

**Acceptance Scenarios**:

1. **Given** レガシーアダプターが削除されている, **When** アプリをビルドする, **Then** ビルドが成功し、警告が減少する
2. **Given** レガシーアダプターが削除されている, **When** 取引登録画面でアカウントオートコンプリートを使用する, **Then** Composeベースのオートコンプリートが正常に動作する

---

### User Story 5 - クラッシュレポートダイアログのCompose化 (Priority: P5)

クラッシュレポート送信ダイアログをComposeで実装する。

**Why this priority**: クラッシュレポートは通常のユーザーフローでは使用されず、エラー時のみ表示される。最低優先度だが、完全なCompose移行のために必要。

**Independent Test**: 意図的にクラッシュを発生させ、クラッシュレポートダイアログが表示されることを確認できる。

**Acceptance Scenarios**:

1. **Given** アプリがクラッシュした, **When** 再起動時にクラッシュレポートが利用可能な場合, **Then** Composeベースのクラッシュレポートダイアログが表示される
2. **Given** クラッシュレポートダイアログが表示されている, **When** 送信ボタンをタップする, **Then** レポートが送信され、ダイアログが閉じる

---

### Edge Cases

- 日付ピッカーで無効な日付範囲が指定された場合の処理
- 通貨選択で通貨リストが空の場合の表示
- バックアップファイルが破損している場合のリストア処理
- スプラッシュ画面表示中にアプリがバックグラウンドに移行した場合の処理
- ダイアログ表示中に画面回転が発生した場合の状態保持

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: システムはDatePickerFragmentをCompose DialogのDatePickerに置き換えなければならない
- **FR-002**: システムはCurrencySelectorFragmentをCompose Dialogの通貨選択に置き換えなければならない
- **FR-003**: システムはSplashActivityをCompose Activityに置き換えなければならない
- **FR-004**: システムはBackupsActivityをCompose Activityに置き換えなければならない
- **FR-005**: システムはCrashReportDialogFragmentをCompose Dialogに置き換えなければならない
- **FR-006**: システムは不要となるレガシーアダプター（AccountAutocompleteAdapter、AccountWithAmountsAutocompleteAdapter、TransactionDescriptionAutocompleteAdapter、CurrencySelectorRecyclerViewAdapter）を削除しなければならない
- **FR-011**: システムはProfilesRecyclerViewAdapterをCompose実装に置き換え、NavigationDrawer内のプロファイルリストをCompose化しなければならない
- **FR-007**: システムは既存のRoomデータベーススキーマを変更してはならない（データ互換性維持）
- **FR-008**: システムは削除されたFragment/Adapterに対応するXMLレイアウトファイルを削除しなければならない
- **FR-009**: 全てのCompose画面はMoLeThemeを使用し、プロファイルカラーに対応しなければならない
- **FR-010**: ダイアログは画面回転時に状態を保持しなければならない
- **FR-012**: システムはQRScanCapableFragment（抽象基底クラス）を削除しなければならない

### Key Entities

- **Dialog**: ユーザー入力を受け取るモーダルコンポーネント。日付選択、通貨選択、確認ダイアログなど。
- **Screen**: 独立した画面。スプラッシュ、バックアップなど。
- **Adapter**: RecyclerViewにデータをバインドするコンポーネント。Composeでは不要。

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 全てのXMLレイアウトファイルが削除され、app/src/main/res/layout/ディレクトリが空になる
- **SC-002**: 全てのFragment（DialogFragment含む）が削除され、Fragment依存コードがなくなる
- **SC-003**: ViewBindingの使用箇所がゼロになる
- **SC-004**: 既存の機能テストが全て通過する（リグレッションなし）
- **SC-005**: APKサイズが現在のサイズ（27MB debug）から増加しない、または5%以内の増加に抑える
- **SC-006**: アプリ起動時間が現在の平均（526ms cold start）から20%以上遅延しない
- **SC-007**: 全ての画面でプロファイルカラーテーマが正しく適用される

## Clarifications

### Session 2026-01-10

- Q: ProfilesRecyclerViewAdapterの扱いは？ → A: 移行対象に含める（NavigationDrawerのプロファイルリストをCompose化）
- Q: QRScanCapableFragmentの扱いは？ → A: 削除対象に含める（使用箇所を確認の上、削除）
- Q: CurrencySelectorRecyclerViewAdapterの扱いは？ → A: FR-006に追加して明示的に削除対象とする

## Assumptions

- 既存のComposeインフラ（MoLeTheme、共通コンポーネント）は006-compose-ui-rebuildで整備済み
- Material3のDatePickerおよびDialog APIを使用する
- クラッシュレポート機能の送信先やフォーマットは既存実装を踏襲する
- バックアップ機能のファイル形式やストレージロケーションは変更しない
