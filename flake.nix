{
  description = "MoLe - Mobile Ledger development environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
    android-nixpkgs = {
      url = "github:tadfisher/android-nixpkgs";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs = { self, nixpkgs, flake-utils, android-nixpkgs }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config = {
            allowUnfree = true;
            android_sdk.accept_license = true;
          };
        };

        # Android SDK setup using android-nixpkgs
        android-sdk = android-nixpkgs.sdk.${system} (sdkPkgs: with sdkPkgs; [
          cmdline-tools-latest
          build-tools-30-0-3
          platform-tools
          platforms-android-33
        ]);

        # FHS環境を構築
        fhsEnv = pkgs.buildFHSEnv {
          name = "mole-android-fhs";

          targetPkgs = p: (with p; [
            # Java Development Kit
            jdk17

            # 基本的な開発ツール
            git
            which
            gnused
            findutils
            coreutils

            # Gradleが必要とする動的ライブラリ
            zlib
            stdenv.cc.cc.lib
            ncurses5

            # グラフィカルツール用のライブラリ
            fontconfig
            freetype
            libglvnd
            xorg.libX11
            xorg.libXext
            xorg.libXi
            xorg.libXrender
            xorg.libXtst
            pulseaudio

            # 追加の依存関係
            glibc
            expat
            libxcrypt-legacy
          ]) ++ [ android-sdk ];

          multiPkgs = p: with p; [
            # 32bitと64bitの両方が必要なライブラリ
            zlib
            stdenv.cc.cc.lib
          ];

          profile = ''
            # Java環境変数（Nixストアから直接）
            export JAVA_HOME="${pkgs.jdk17.home}"

            # Android SDK環境変数
            export ANDROID_HOME="${android-sdk}/share/android-sdk"
            export ANDROID_SDK_ROOT="$ANDROID_HOME"

            # パス設定
            export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

            # Gradle設定
            export GRADLE_USER_HOME="$HOME/.gradle"

            # ロケール設定
            export LANG=C.UTF-8
            export LC_ALL=C.UTF-8

            echo "================================================="
            echo "MoLe Android Build Environment (FHS)"
            echo "================================================="
            echo ""
            echo "Java version:"
            java -version 2>&1 | head -1
            echo ""
            echo "JAVA_HOME: $JAVA_HOME"
            echo "ANDROID_HOME: $ANDROID_HOME"
            echo ""
            echo "この環境内でGradleビルドが実行できます:"
            echo "  ./gradlew assembleDebug    # デバッグAPKビルド"
            echo "  ./gradlew test              # テスト実行"
            echo "  ./gradlew clean             # クリーンビルド"
            echo ""
            echo "Note: この環境はFHS互換モードで動作しています。"
            echo "      通常のLinuxバイナリ（AAPT2等）が動作します。"
            echo "================================================="
          '';

          runScript = "bash";
        };

        # APKビルド用のderivation
        buildApk = pkgs.stdenv.mkDerivation {
          name = "mole-apk";
          version = "0.22.0";

          src = ./.;

          nativeBuildInputs = [ fhsEnv ];

          buildPhase = ''
            # FHS環境内でGradleビルドを実行
            ${fhsEnv}/bin/mole-android-fhs -c "
              export JAVA_HOME=${pkgs.jdk17.home}
              export ANDROID_HOME=${android-sdk}/share/android-sdk
              export ANDROID_SDK_ROOT=\$ANDROID_HOME
              export GRADLE_USER_HOME=$TMPDIR/.gradle
              export PATH=\$JAVA_HOME/bin:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$PATH

              # local.propertiesを生成
              echo 'sdk.dir=${android-sdk}/share/android-sdk' > local.properties

              # ビルド実行
              ./gradlew --no-daemon assembleDebug
            "
          '';

          installPhase = ''
            mkdir -p $out
            cp app/build/outputs/apk/debug/app-debug.apk $out/mole-0.22.0.apk

            # メタデータも保存
            echo "MoLe v0.22.0" > $out/VERSION
            echo "Built with Nix Flakes" >> $out/BUILD_INFO
            echo "Build date: $(date -u +%Y-%m-%d)" >> $out/BUILD_INFO
          '';

          meta = with pkgs.lib; {
            description = "MoLe - Mobile Ledger Android application";
            license = licenses.gpl3Plus;
            platforms = platforms.linux;
          };
        };

        # ビルドヘルパースクリプト (nix run .#build で実行可能)
        buildScript = pkgs.writeShellScriptBin "build-mole" ''
          set -e
          echo "================================================="
          echo "Building MoLe APK with Nix"
          echo "================================================="

          # FHS環境でビルド
          ${fhsEnv}/bin/mole-android-fhs -c "
            export JAVA_HOME=${pkgs.jdk17.home}
            export ANDROID_HOME=${android-sdk}/share/android-sdk
            export ANDROID_SDK_ROOT=\$ANDROID_HOME
            export PATH=\$JAVA_HOME/bin:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$PATH

            # local.propertiesを生成
            echo 'sdk.dir=${android-sdk}/share/android-sdk' > local.properties

            # ビルド実行
            ./gradlew assembleDebug
          "

          echo ""
          echo "✅ Build complete!"
          echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
          ls -lh app/build/outputs/apk/debug/app-debug.apk
        '';

        # リリースビルドヘルパースクリプト (nix run .#buildRelease で実行可能)
        buildReleaseScript = pkgs.writeShellScriptBin "build-mole-release" ''
          set -e
          echo "================================================="
          echo "Building MoLe RELEASE APK with Nix"
          echo "================================================="
          echo ""

          # keystore.propertiesの存在確認
          if [ ! -f "keystore.properties" ]; then
            echo "❌ Error: keystore.properties not found!"
            echo "Please create keystore.properties with your signing information."
            exit 1
          fi

          echo "✅ keystore.properties found"
          echo ""

          # FHS環境でビルド
          ${fhsEnv}/bin/mole-android-fhs -c "
            export JAVA_HOME=${pkgs.jdk17.home}
            export ANDROID_HOME=${android-sdk}/share/android-sdk
            export ANDROID_SDK_ROOT=\$ANDROID_HOME
            export PATH=\$JAVA_HOME/bin:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$PATH

            # local.propertiesを生成
            echo 'sdk.dir=${android-sdk}/share/android-sdk' > local.properties

            # クリーンビルド
            echo 'Cleaning previous build...'
            ./gradlew clean

            echo ""
            echo 'Building release APK...'
            # リリースビルド実行
            ./gradlew assembleRelease
          "

          echo ""
          echo "✅ Release build complete!"
          echo ""
          if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
            echo "📦 APK location: app/build/outputs/apk/release/app-release.apk"
            ls -lh app/build/outputs/apk/release/app-release.apk
          else
            echo "❌ Release APK not found!"
            exit 1
          fi
        '';

      in
      {
        # パッケージ定義
        packages = {
          default = buildApk;
          apk = buildApk;
          build-script = buildScript;
        };

        # アプリケーション定義
        apps = {
          build = {
            type = "app";
            program = "${buildScript}/bin/build-mole";
          };
          buildRelease = {
            type = "app";
            program = "${buildReleaseScript}/bin/build-mole-release";
          };
        };

        # FHS環境でのビルド用シェル (推奨)
        devShells.fhs = fhsEnv;

        # 通常のNix環境（開発ツールのみ、ビルドは不可）
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            # Java Development Kit
            jdk17

            # Android SDK
            android-sdk

            # Git for version control
            git

            # Useful development tools
            which
            gnused
            findutils
            coreutils
          ];

          shellHook = ''
            # Set JAVA_HOME
            export JAVA_HOME="${pkgs.jdk17}"

            # Set Android SDK environment variables
            export ANDROID_HOME="${android-sdk}/share/android-sdk"
            export ANDROID_SDK_ROOT="$ANDROID_HOME"

            # Add tools to PATH
            export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

            echo "================================================="
            echo "MoLe Development Environment"
            echo "================================================="
            echo ""
            echo "Java version:"
            java -version 2>&1 | head -1
            echo ""
            echo "JAVA_HOME: $JAVA_HOME"
            echo "ANDROID_HOME: $ANDROID_HOME"
            echo ""
            echo "⚠️  WARNING: ビルドにはFHS環境が必要です"
            echo ""
            echo "ビルドするには以下を使用してください:"
            echo "  nix develop .#fhs"
            echo ""
            echo "このシェルは開発ツール（git等）のみ利用可能です。"
            echo "================================================="
          '';
        };
      }
    );
}
