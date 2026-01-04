#!/usr/bin/env bash
# Android build toolsのバイナリをNixOS用にパッチするスクリプト

set -e

echo "Patching Android build tools for NixOS..."

# Gradleキャッシュディレクトリ
GRADLE_CACHE="$HOME/.gradle/caches"

# NIX環境のインタープリター
INTERPRETER=$(cat /nix/store/*-glibc-*/nix-support/dynamic-linker)

# AAPT2バイナリを検索してパッチ
find "$GRADLE_CACHE" -name "aapt2" -type f 2>/dev/null | while read -r aapt2_bin; do
    echo "Found AAPT2: $aapt2_bin"

    # すでにパッチ済みかチェック
    if patchelf --print-interpreter "$aapt2_bin" 2>/dev/null | grep -q "/nix/store"; then
        echo "  Already patched, skipping"
        continue
    fi

    # バックアップ作成
    cp "$aapt2_bin" "$aapt2_bin.backup"

    # インタープリターをパッチ
    patchelf --set-interpreter "$INTERPRETER" "$aapt2_bin" || {
        echo "  Failed to patch, restoring backup"
        mv "$aapt2_bin.backup" "$aapt2_bin"
        continue
    }

    # rpath設定
    patchelf --set-rpath "$(patchelf --print-rpath "$aapt2_bin"):${pkgs.stdenv.cc.cc.lib}/lib:${pkgs.zlib}/lib" "$aapt2_bin" || {
        echo "  Warning: Failed to set rpath"
    }

    echo "  Successfully patched!"
done

# その他のAndroidツールも同様にパッチ可能
# (aidl, zipalign, など)

echo "Patching complete!"
echo ""
echo "Note: 新しいGradleビルドツールがダウンロードされたら、"
echo "このスクリプトを再実行してください。"
