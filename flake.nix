{
  description = "MoLe - Mobile Ledger development environment and build system";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
    android-nixpkgs = {
      url = "github:tadfisher/android-nixpkgs";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    pre-commit-hooks = {
      url = "github:cachix/pre-commit-hooks.nix";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs = { self, nixpkgs, flake-utils, android-nixpkgs, pre-commit-hooks }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config = {
            allowUnfree = true;
            android_sdk.accept_license = true;
          };
        };

        # バージョン管理
        version = "0.22.1";
        appName = "MoLe";
        javaVersion = pkgs.jdk17;   # Java Development Kit

        # Android SDKバージョン (メタデータ用)
        androidVersions = {
          buildTools = "34.0.0";    # Android SDK Build Tools 34.0.0
          platform = "34";          # Android 14 (API Level 34)
          compileSdk = "34";
          targetSdk = "34";
        };

        # Android SDK setup using android-nixpkgs
        # androidVersionsの値を使用してSDKパッケージを動的に選択
        android-sdk = android-nixpkgs.sdk.${system} (sdkPkgs: [
          sdkPkgs.cmdline-tools-latest
          sdkPkgs."build-tools-${builtins.replaceStrings ["."] ["-"] androidVersions.buildTools}"
          sdkPkgs.platform-tools
          sdkPkgs."platforms-android-${androidVersions.platform}"
        ]);

        # 共通の環境変数設定
        commonEnvVars = ''
          export JAVA_HOME="${javaVersion.home}"
          export ANDROID_HOME="${android-sdk}/share/android-sdk"
          export ANDROID_SDK_ROOT="$ANDROID_HOME"
          export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
        '';

        # local.properties生成
        makeLocalProperties = ''
          echo 'sdk.dir=${android-sdk}/share/android-sdk' > local.properties
        '';

        # FHS環境を構築
        fhsEnv = pkgs.buildFHSEnv {
          name = "mole-android-fhs";

          targetPkgs = p: (with p; [
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
          ]) ++ [ javaVersion android-sdk ];

          multiPkgs = p: with p; [
            # 32bitと64bitの両方が必要なライブラリ
            zlib
            stdenv.cc.cc.lib
          ];

          profile = ''
            ${commonEnvVars}

            # Gradle設定
            export GRADLE_USER_HOME="$HOME/.gradle"

            # ロケール設定
            export LANG=C.UTF-8
            export LC_ALL=C.UTF-8

            echo "================================================="
            echo "${appName} Android Build Environment (FHS)"
            echo "Version: ${version}"
            echo "================================================="
            echo ""
            echo "Java version:"
            java -version 2>&1 | head -1
            echo ""
            echo "Environment:"
            echo "  JAVA_HOME: $JAVA_HOME"
            echo "  ANDROID_HOME: $ANDROID_HOME"
            echo ""
            echo "Android SDK:"
            echo "  Build Tools: ${androidVersions.buildTools}"
            echo "  Platform: ${androidVersions.platform} (Android 14)"
            echo "  Target SDK: ${androidVersions.targetSdk}"
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

        # FHS環境でコマンドを実行するヘルパー
        runInFhs = command: ''
          ${fhsEnv}/bin/mole-android-fhs -c "
            ${commonEnvVars}
            ${command}
          "
        '';

        # APKビルド用のderivation
        buildApk = pkgs.stdenv.mkDerivation {
          name = "mole-apk";
          inherit version;

          src = ./.;

          nativeBuildInputs = [ fhsEnv ];

          buildPhase = runInFhs ''
            export GRADLE_USER_HOME=$TMPDIR/.gradle
            ${makeLocalProperties}
            ./gradlew --no-daemon assembleDebug
          '';

          installPhase = ''
            mkdir -p $out
            cp app/build/outputs/apk/debug/app-debug.apk $out/mole-${version}.apk

            # メタデータも保存
            echo "${appName} v${version}" > $out/VERSION
            echo "Built with Nix Flakes" >> $out/BUILD_INFO
            echo "Build date: $(date -u +%Y-%m-%d)" >> $out/BUILD_INFO
          '';

          meta = with pkgs.lib; {
            description = "${appName} - Mobile Ledger Android application (v${version})";
            license = licenses.gpl3Plus;
            platforms = platforms.linux;
          };
        };

        # ビルドヘルパースクリプト (nix run .#build で実行可能)
        buildScript = pkgs.writeShellScriptBin "build-mole" ''
          set -e
          echo "================================================="
          echo "Building ${appName} APK with Nix (v${version})"
          echo "================================================="

          ${runInFhs ''
            ${makeLocalProperties}
            ./gradlew --no-daemon assembleDebug
          ''}

          echo ""
          echo "✅ Build complete!"
          echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
          ls -lh app/build/outputs/apk/debug/app-debug.apk
        '';

        # リリースビルドヘルパースクリプト (nix run .#buildRelease で実行可能)
        buildReleaseScript = pkgs.writeShellScriptBin "build-mole-release" ''
          set -e
          echo "================================================="
          echo "Building ${appName} RELEASE APK with Nix (v${version})"
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

          ${runInFhs ''
            ${makeLocalProperties}
            echo 'Cleaning previous build...'
            ./gradlew --no-daemon clean
            echo ""
            echo 'Building release APK...'
            ./gradlew --no-daemon assembleRelease
          ''}

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

        # テスト実行スクリプト (nix run .#test で実行可能)
        testScript = pkgs.writeShellScriptBin "test-mole" ''
          set -e
          echo "================================================="
          echo "Running ${appName} Tests (v${version})"
          echo "================================================="

          ${runInFhs ''
            ${makeLocalProperties}
            ./gradlew --no-daemon test
          ''}

          echo ""
          echo "✅ Tests complete!"
        '';

        # クリーンスクリプト (nix run .#clean で実行可能)
        cleanScript = pkgs.writeShellScriptBin "clean-mole" ''
          set -e
          echo "================================================="
          echo "Cleaning ${appName} Build (v${version})"
          echo "================================================="

          ${runInFhs ''
            ${makeLocalProperties}
            ./gradlew --no-daemon clean
          ''}

          echo ""
          echo "✅ Clean complete!"
        '';

        # ビルド＋インストールスクリプト (nix run .#install で実行可能)
        installScript = pkgs.writeShellScriptBin "install-mole" ''
          set -e
          echo "================================================="
          echo "Building and Installing ${appName} (v${version})"
          echo "================================================="

          ${runInFhs ''
            ${makeLocalProperties}
            ./gradlew --no-daemon assembleDebug
          ''}

          echo ""
          echo "📱 Installing APK to connected device..."
          ${android-sdk}/share/android-sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk

          echo ""
          echo "✅ Build and install complete!"
          echo "Please verify on device and report any issues."
        '';

        # フルワークフロー: テスト → ビルド → インストール (nix run .#verify で実行可能)
        verifyScript = pkgs.writeShellScriptBin "verify-mole" ''
          set -e
          echo "================================================="
          echo "${appName} Verification Workflow (v${version})"
          echo "================================================="
          echo ""
          echo "Step 1/3: Running unit tests..."
          echo "-------------------------------------------------"

          ${runInFhs ''
            ${makeLocalProperties}
            ./gradlew --no-daemon test
          ''}

          echo ""
          echo "✅ Tests passed!"
          echo ""
          echo "Step 2/3: Building debug APK..."
          echo "-------------------------------------------------"

          ${runInFhs ''
            ${makeLocalProperties}
            ./gradlew --no-daemon assembleDebug
          ''}

          echo ""
          echo "✅ Build complete!"
          echo ""
          echo "Step 3/3: Installing to device..."
          echo "-------------------------------------------------"

          ${android-sdk}/share/android-sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk

          echo ""
          echo "================================================="
          echo "✅ Verification workflow complete!"
          echo ""
          echo "📱 APK has been installed on the connected device."
          echo "Please verify the following manually:"
          echo "  1. App launches without crash"
          echo "  2. Data refresh works correctly"
          echo "  3. Profile creation/editing works"
          echo "  4. Transaction submission works"
          echo ""
          echo "Report any issues found during verification."
          echo "================================================="
        '';

        # Pre-commit hooks 設定
        pre-commit-check = pre-commit-hooks.lib.${system}.run {
          src = ./.;
          hooks = {
            # Kotlin コードスタイルチェック（チェックのみ、修正しない）
            ktlint = {
              enable = true;
              name = "ktlint";
              description = "Kotlin linter and formatter";
              entry = "${pkgs.ktlint}/bin/ktlint --editorconfig=.editorconfig";
              files = "\\.kts?$";
              language = "system";
              types = [ "file" ];
            };

            # Kotlin 静的解析
            detekt = {
              enable = true;
              name = "detekt";
              description = "Kotlin static code analysis";
              entry = "${pkgs.writeShellScript "detekt-wrapper" ''
                # detekt expects comma-separated list of files
                files=$(echo "$@" | tr ' ' ',')
                if [ -n "$files" ]; then
                  ${pkgs.detekt}/bin/detekt --config detekt.yml --build-upon-default-config --input "$files"
                fi
              ''}";
              files = "\\.kts?$";
              language = "system";
              pass_filenames = true;
              types = [ "file" ];
            };
          };
        };

        # Android Lint スクリプト（CI用、コミット時は重いため別途実行）
        lintScript = pkgs.writeShellScriptBin "lint-mole" ''
          set -e
          echo "================================================="
          echo "Running ${appName} Android Lint (v${version})"
          echo "================================================="

          ${runInFhs ''
            ${makeLocalProperties}
            ./gradlew --no-daemon lintDebug
          ''}

          echo ""
          echo "Lint complete! Check app/build/reports/lint-results-debug.html for details."
        '';

        # コードカバレッジスクリプト (nix run .#coverage で実行可能)
        # Kover: Kotlin専用カバレッジツール (JetBrains製)
        coverageScript = pkgs.writeShellScriptBin "coverage-mole" ''
          set -e
          echo "================================================="
          echo "Running ${appName} Test Coverage Report (v${version})"
          echo "Using Kover (Kotlin-optimized coverage)"
          echo "================================================="

          ${runInFhs ''
            ${makeLocalProperties}
            ./gradlew --no-daemon koverHtmlReportDebug koverXmlReportDebug
          ''}

          echo ""
          echo "✅ Coverage report generated!"
          echo ""
          echo "📊 Report locations:"
          echo "  HTML: app/build/reports/kover/htmlDebug/index.html"
          echo "  XML:  app/build/reports/kover/reportDebug.xml"
        '';

      in
      {
        # パッケージ定義
        packages = {
          default = buildApk;
          apk = buildApk;
          build-script = buildScript;
          test-script = testScript;
          clean-script = cleanScript;
        };

        # アプリケーション定義
        apps = {
          build = {
            type = "app";
            program = "${buildScript}/bin/build-mole";
            meta = {
              description = "Build debug APK for ${appName}";
            };
          };
          buildRelease = {
            type = "app";
            program = "${buildReleaseScript}/bin/build-mole-release";
            meta = {
              description = "Build release APK for ${appName}";
            };
          };
          test = {
            type = "app";
            program = "${testScript}/bin/test-mole";
            meta = {
              description = "Run unit tests for ${appName}";
            };
          };
          clean = {
            type = "app";
            program = "${cleanScript}/bin/clean-mole";
            meta = {
              description = "Clean build artifacts for ${appName}";
            };
          };
          install = {
            type = "app";
            program = "${installScript}/bin/install-mole";
            meta = {
              description = "Build and install debug APK to connected device";
            };
          };
          verify = {
            type = "app";
            program = "${verifyScript}/bin/verify-mole";
            meta = {
              description = "Full verification workflow: test → build → install";
            };
          };
          lint = {
            type = "app";
            program = "${lintScript}/bin/lint-mole";
            meta = {
              description = "Run Android Lint checks via Gradle";
            };
          };
          coverage = {
            type = "app";
            program = "${coverageScript}/bin/coverage-mole";
            meta = {
              description = "Generate test coverage report with Kover (Kotlin-optimized)";
            };
          };
        };

        # Checks (現時点では pre-commit は devShell でのみ使用)
        # 既存コードの lint 対応が完了したら、以下を有効化:
        # checks = {
        #   pre-commit-check = pre-commit-check;
        # };

        # FHS環境でのビルド用シェル (推奨)
        devShells.fhs = fhsEnv;

        # 通常のNix環境（開発ツールのみ、ビルドは不可）
        devShells.default = pkgs.mkShell {
          inherit (pre-commit-check) shellHook;

          buildInputs = [
            # Java Development Kit
            javaVersion

            # Android SDK
            android-sdk

            # Pre-commit
            pkgs.pre-commit
          ] ++ (with pkgs; [
            # Git for version control
            git

            # Useful development tools
            which
            gnused
            findutils
            coreutils

            # Lint tools
            ktlint
            detekt
          ]);

          # shellHook は pre-commit-check から継承されるため、
          # 追加のメッセージは MOTD スタイルで表示
          MOTD = ''
            =================================================
            ${appName} Development Environment
            Version: ${version}
            =================================================

            Pre-commit hooks: ENABLED
              - ktlint (Kotlin style)
              - detekt (Kotlin static analysis)

            Commands:
              pre-commit run --all-files  # 全ファイルに lint 実行
              pre-commit run ktlint       # ktlint のみ実行
              nix run .#lint              # Android Lint (CI用)
              nix flake check             # CI 用チェック

            Build commands:
              nix develop .#fhs           # FHS ビルド環境
              nix run .#build             # デバッグ APK ビルド
              nix run .#test              # テスト実行
            =================================================
          '';
        };
      }
    );
}
