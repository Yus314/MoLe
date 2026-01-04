#!/usr/bin/env bash
# MoLe 実機テスト クイックスタートスクリプト
# バージョン: 2.0
# 最終更新: 2026-01-03
#
# このスクリプトはMoLeアプリの実機テスト環境を素早くセットアップします。
#
# 機能:
#   1. 環境チェック（adb, docker）
#   2. Androidデバイスの確認
#   3. APKファイルの確認（なければビルド方法を提示）
#   4. hledger-webテストサーバーの起動（4バージョン対応）
#   5. APKのインストールとアプリ起動
#   6. リアルタイムログ監視
#
# 使用方法:
#   chmod +x scripts/test-quick-start.sh
#   ./scripts/test-quick-start.sh
#
# 前提条件:
#   - Android SDK Platform-Tools (adb)
#   - Docker & Docker Compose (サーバー起動用、オプション)
#   - USBデバッグが有効なAndroidデバイス
#
# 参考資料:
#   - テストガイド: docs/TESTING_GUIDE.md
#   - ビルド手順: docs/BUILDING.md
#   - Nixビルド: docs/NIX_BUILD_GUIDE.md

set -e

# 色の定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ヘルパー関数
print_header() {
    echo ""
    echo -e "${BLUE}=================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}=================================================${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# ステップ1: 環境チェック
print_header "ステップ1: 環境チェック"

# adbコマンドの確認
if command -v adb &> /dev/null; then
    print_success "adb コマンドが見つかりました"
    ADB_VERSION=$(adb version | head -1)
    print_info "バージョン: $ADB_VERSION"
else
    print_error "adb コマンドが見つかりません"
    print_info "Android SDK Platform-Tools をインストールしてください"
    exit 1
fi

# dockerコマンドの確認
if command -v docker &> /dev/null; then
    print_success "docker コマンドが見つかりました"
else
    print_warning "docker コマンドが見つかりません"
    print_info "hledger-webサーバーを手動で起動する必要があります"
fi

# ステップ2: デバイスの確認
print_header "ステップ2: Androidデバイスの確認"

DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l)

if [ "$DEVICES" -eq 0 ]; then
    print_error "Androidデバイスが接続されていません"
    print_info "以下を確認してください:"
    print_info "  1. デバイスがUSBで接続されているか"
    print_info "  2. USBデバッグが有効になっているか"
    print_info "  3. デバッグ許可のダイアログで「許可」をタップしたか"
    echo ""
    print_info "接続を確認したら Enter キーを押してください..."
    read -r
    DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l)
    if [ "$DEVICES" -eq 0 ]; then
        print_error "デバイスが見つかりません。終了します。"
        exit 1
    fi
fi

print_success "Androidデバイスが $DEVICES 台接続されています"
echo ""
print_info "接続されているデバイス:"
adb devices

# ステップ3: APKの確認
print_header "ステップ3: APKファイルの確認"

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

if [ -f "$APK_PATH" ]; then
    print_success "APKファイルが見つかりました"
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    print_info "サイズ: $APK_SIZE"

    # APKのバージョン情報を表示
    if command -v aapt &> /dev/null; then
        VERSION_NAME=$(aapt dump badging "$APK_PATH" 2>/dev/null | grep "versionName" | sed "s/.*versionName='//" | sed "s/' .*//" || echo "不明")
        VERSION_CODE=$(aapt dump badging "$APK_PATH" 2>/dev/null | grep "versionCode" | sed "s/.*versionCode='//" | sed "s/' .*//" || echo "不明")
        print_info "バージョン: $VERSION_NAME (versionCode: $VERSION_CODE)"
    fi
else
    print_error "APKファイルが見つかりません: $APK_PATH"
    echo ""
    print_info "まずビルドを実行してください:"
    echo ""
    print_info "【推奨】Nixでビルド（NixOS環境の場合）:"
    echo "  nix run .#build"
    echo ""
    print_info "または、FHS環境でビルド:"
    echo "  nix develop .#fhs"
    echo "  ./gradlew assembleDebug"
    echo ""
    print_info "または、標準環境でビルド:"
    echo "  ./gradlew assembleDebug"
    echo ""
    exit 1
fi

# ステップ4: hledger-webサーバーの起動（オプション）
print_header "ステップ4: hledger-webサーバーの起動"

if command -v docker &> /dev/null; then
    echo "hledger-webサーバーを起動しますか？ (y/n)"
    read -r START_SERVERS

    if [ "$START_SERVERS" = "y" ] || [ "$START_SERVERS" = "Y" ]; then
        print_info "docker-composeでサーバーを起動しています..."

        if [ -f "docker-compose.test.yml" ]; then
            docker-compose -f docker-compose.test.yml up -d
            print_success "hledger-webサーバーを起動しました"

            # サーバーの起動を待つ
            print_info "サーバーの起動を待っています（5秒）..."
            sleep 5

            # バージョン確認
            echo ""
            print_info "サーバーのバージョンを確認しています..."
            echo ""

            SERVERS_OK=0
            SERVERS_TOTAL=4

            for PORT in 5023 5032 5040 5050; do
                VERSION=$(curl -s http://localhost:$PORT/version 2>/dev/null | tr -d '"' || echo "")
                case $PORT in
                    5023) LABEL="v1.23 (後方互換性)";;
                    5032) LABEL="v1.32 (adeclarationinfo)";;
                    5040) LABEL="v1.40 (中間バージョン)";;
                    5050) LABEL="v1.50 (最新)";;
                esac

                if [ -n "$VERSION" ]; then
                    print_success "Port $PORT: hledger-web $VERSION - $LABEL"
                    SERVERS_OK=$((SERVERS_OK + 1))
                else
                    print_error "Port $PORT: 応答なし - $LABEL"
                fi
            done

            echo ""
            if [ $SERVERS_OK -eq $SERVERS_TOTAL ]; then
                print_success "全てのテストサーバーが正常に動作しています ($SERVERS_OK/$SERVERS_TOTAL)"
            elif [ $SERVERS_OK -gt 0 ]; then
                print_warning "一部のサーバーが応答していません ($SERVERS_OK/$SERVERS_TOTAL)"
                print_info "起動に時間がかかる場合があります。しばらく待ってから再確認してください"
            else
                print_error "全てのサーバーが応答していません"
                print_info "docker-compose.test.yml の設定を確認してください"
            fi

            # IPアドレスの表示
            echo ""
            print_info "PCのIPアドレス:"
            if command -v ip &> /dev/null; then
                IP_ADDR=$(ip addr show | grep "inet " | grep -v 127.0.0.1 | head -1 | awk '{print $2}' | cut -d'/' -f1)
            else
                IP_ADDR=$(ifconfig | grep "inet " | grep -v 127.0.0.1 | head -1 | awk '{print $2}')
            fi
            print_success "IP: $IP_ADDR"
            echo ""
            print_info "実機からアクセスする場合のURL:"
            for PORT in 5023 5032 5040 5050; do
                case $PORT in
                    5023) VERSION="v1.23";;
                    5032) VERSION="v1.32";;
                    5040) VERSION="v1.40";;
                    5050) VERSION="v1.50";;
                esac
                echo "  $VERSION: http://$IP_ADDR:$PORT"
            done
        else
            print_error "docker-compose.test.yml が見つかりません"
        fi
    else
        print_info "サーバーの起動をスキップしました"
        print_info "手動でサーバーを起動してください"
    fi
else
    print_warning "dockerが利用できないため、サーバー起動をスキップします"
fi

# ステップ5: APKのインストール
print_header "ステップ5: APKのインストール"

echo "APKをインストールしますか？ (y/n)"
read -r INSTALL_APK

if [ "$INSTALL_APK" = "y" ] || [ "$INSTALL_APK" = "Y" ]; then
    # デバッグビルドのパッケージ名
    DEBUG_PACKAGE="net.ktnx.mobileledger.debug"
    RELEASE_PACKAGE="net.ktnx.mobileledger"

    print_info "既存のアプリを確認しています..."

    # デバッグ版の確認
    if adb shell pm list packages | grep -q "$DEBUG_PACKAGE"; then
        print_info "既存のデバッグ版を削除しています..."
        adb uninstall "$DEBUG_PACKAGE" 2>/dev/null || true
    fi

    # リリース版の確認（警告のみ）
    if adb shell pm list packages | grep -q "^package:$RELEASE_PACKAGE$"; then
        print_warning "リリース版がインストールされています"
        print_info "テストにはデバッグ版を使用することを推奨します"
    fi

    print_info "APKをインストールしています..."
    if adb install -r "$APK_PATH" 2>&1 | tee /tmp/adb-install.log; then
        print_success "APKのインストールが完了しました"

        # インストール確認
        sleep 1
        if adb shell pm list packages | grep -q "$DEBUG_PACKAGE"; then
            print_success "インストールを確認しました"

            # アプリの起動オプション
            echo ""
            echo "アプリを起動しますか？ (y/n)"
            read -r LAUNCH_APP

            if [ "$LAUNCH_APP" = "y" ] || [ "$LAUNCH_APP" = "Y" ]; then
                print_info "アプリを起動しています..."
                # パッケージ名で起動（LAUNCHERインテントを使用）
                if adb shell monkey -p "$DEBUG_PACKAGE" -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1; then
                    print_success "アプリを起動しました"
                else
                    # fallback: 直接LAUNCHERアクティビティ（SplashActivity）を起動
                    adb shell am start -n "$DEBUG_PACKAGE/.ui.activity.SplashActivity" > /dev/null 2>&1
                    print_success "アプリを起動しました"
                fi
            fi
        else
            print_error "インストールの確認に失敗しました"
        fi
    else
        print_error "APKのインストールに失敗しました"
        print_info "エラーログ:"
        cat /tmp/adb-install.log
        exit 1
    fi
else
    print_info "APKのインストールをスキップしました"
fi

# ステップ6: テスト準備完了
print_header "✅ テスト準備完了"

echo ""
print_success "全ての準備が整いました！"
echo ""

# サマリー情報の表示
print_header "📋 環境サマリー"
echo ""

if [ -n "$IP_ADDR" ]; then
    print_info "テストサーバーURL（実機からアクセス）:"
    for PORT in 5023 5032 5040 5050; do
        case $PORT in
            5023) VERSION="v1.23";;
            5032) VERSION="v1.32";;
            5040) VERSION="v1.40";;
            5050) VERSION="v1.50";;
        esac
        echo "  $VERSION: http://$IP_ADDR:$PORT"
    done
    echo ""
fi

print_info "APK情報:"
if [ -f "$APK_PATH" ]; then
    echo "  場所: $APK_PATH"
    echo "  サイズ: $(du -h "$APK_PATH" | cut -f1)"
fi
echo ""

print_info "次のステップ:"
echo ""
echo "  1️⃣  実機でMoLeアプリを開く"
echo ""
echo "  2️⃣  新しいプロファイルを作成"
echo "     - プロファイル名: 任意（例: Test Server v1.32）"
echo "     - サーバーURL: http://$IP_ADDR:5032"
echo "     - バージョン検出: 自動"
echo ""
echo "  3️⃣  テストガイドに従ってテストを実施"
echo "     - アカウント一覧の表示確認"
echo "     - トランザクション表示確認"
echo "     - 新規トランザクション作成"
echo "     - 各バージョン（v1.23, v1.32, v1.40, v1.50）で動作確認"
echo ""
echo "  4️⃣  テスト結果を記録"
echo "     - テンプレート: docs/DEVICE_TEST_REPORT_TEMPLATE.md"
echo ""

print_info "📚 参考資料:"
echo "  - テストガイド: docs/TESTING_GUIDE.md"
echo "  - テストチートシート: docs/TESTING_CHEATSHEET.md"
echo "  - テストデータ: docs/test-data/test.journal"
echo ""

# ログの監視オプション
echo "アプリのログを監視しますか？ (y/n)"
read -r MONITOR_LOG

if [ "$MONITOR_LOG" = "y" ] || [ "$MONITOR_LOG" = "Y" ]; then
    print_info "ログを監視しています（Ctrl+Cで終了）..."
    echo ""
    print_info "監視対象:"
    echo "  - MoLe アプリケーションログ"
    echo "  - エラー・クラッシュ情報"
    echo "  - ネットワーク通信ログ"
    echo ""

    # ログをクリア
    adb logcat -c 2>/dev/null || true

    # カラー付きログ監視
    adb logcat -v time | grep -E --line-buffered \
        "mobileledger|MoLe|AndroidRuntime.*FATAL|System\.err|java\.lang\..*Exception" | \
        while IFS= read -r line; do
            if echo "$line" | grep -q "FATAL"; then
                echo -e "${RED}$line${NC}"
            elif echo "$line" | grep -q "Exception"; then
                echo -e "${YELLOW}$line${NC}"
            elif echo "$line" | grep -q "System.err"; then
                echo -e "${YELLOW}$line${NC}"
            else
                echo "$line"
            fi
        done
fi

echo ""
print_success "テスト準備スクリプトを終了します"
echo ""
