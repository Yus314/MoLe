MoLe is a front-end to [hledger-web](https://hledger.org/hledger-web.html)

# MoLe - Mobile Ledger

An Android client for [hledger-web](https://hledger.org/), providing mobile access to your plain text accounting data.

## Features

- View account balances and transaction history
- Add new transactions on the go
- Support for multiple hledger-web servers (profiles)
- Automatic server version detection
- Full support for hledger-web versions 1.14 through 1.51

## Supported hledger-web Versions

MoLe automatically detects and adapts to your hledger-web server version:

| hledger-web Version | Support Status | Notes |
|---------------------|----------------|-------|
| 1.14 - 1.23 | ✅ Supported | Legacy versions |
| 1.32 - 1.39 | ✅ Supported | Added account declaration info |
| 1.40 - 1.49 | ✅ Supported | Improved base-url handling |
| 1.50 - 1.51 | ✅ Supported | Latest stable versions |

The app will automatically select the most appropriate JSON API version based on your server's version.

## Installation

### From Source

#### Prerequisites

- Java 17 or later
- Android SDK (API level 33 or later)
- Gradle 8.0 or later

#### Building with NixOS

MoLe includes a Nix flake for reproducible builds on NixOS:

```bash
# Clone the repository
git clone https://github.com/yourusername/mobileledger.git
cd mobileledger

# Method 1: One-command build (推奨)
nix run .#build

# Method 2: Using Nix FHS shell
nix develop .#fhs
./gradlew assembleDebug

# Method 3: Using standalone FHS environment
nix-build shell-fhs.nix -o result-fhs
./result-fhs/bin/mole-android-env -c "./gradlew assembleDebug"
```

The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

**Note**: Pure `nix build` is not supported due to Gradle's network requirements. Use `nix run .#build` instead.

#### Building with Standard Android Tools

```bash
# Clone the repository
git clone https://github.com/yourusername/mobileledger.git
cd mobileledger

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing configuration)
./gradlew assembleRelease
```

### From Release

Download the latest APK from the [Releases](https://github.com/yourusername/mobileledger/releases) page.

## Usage

1. Install the APK on your Android device
2. Open MoLe and create a new profile
3. Enter your hledger-web server URL
4. MoLe will automatically detect the server version and configure itself

## Development

See [HLEDGER_WEB_UPGRADE_PLAN.md](HLEDGER_WEB_UPGRADE_PLAN.md) for implementation details of version support.

For build environment details and options, see [docs/BUILD_OPTIONS_COMPARISON.md](docs/BUILD_OPTIONS_COMPARISON.md).

## Version History

See [CHANGES.md](CHANGES.md) for the complete version history.

### Latest Version: 0.22.0 (2026-01-02)

- Added support for hledger-web v1.32, v1.40, and v1.50
- Improved automatic version detection
- Added account declaration info support (hledger-web v1.32+)

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## License

# Copyright and licensing

## Main software

Copyright ⓒ 2018, 2019, 2020, 2021 Damyan Ivanov <dam+mole@ktnx.net>

MoLe is free software: you can distribute it and/or modify it
under the term of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your opinion), any later version.

MoLe is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License terms for details.

You should have received a copy of the GNU General Public License
along with Mobile-Ledger in a file named COPYING.txt. If not, see
<https://www.gnu.org/licenses/>.

## Other items

Some icons taken from the Android open-source project are Copyright Google Inc
and/or Android open-source project and licensed under the Apache License,
version 2.0 (See Apache-2.0.txt or
<https://www.apache.org/licenses/LICENSE-2.0>)
