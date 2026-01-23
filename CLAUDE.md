# MoLe Development Guidelines

> Android ledger application built with Kotlin, Jetpack Compose, and Hilt DI.

## Quick Reference

| Command | Description |
|---------|-------------|
| `nix run .#build` | Debug APK build |
| `nix run .#test` | Run unit tests |
| `nix run .#verify` | **Full workflow**: test → build → install |
| `nix run .#coverage` | Generate Kover coverage report |
| `nix run .#lint` | Run Android Lint |
| `nix run .#clean` | Clean build artifacts |

## Technology Stack

- **Language**: Kotlin 2.0.21 / JVM target 1.8
- **Build**: AGP 8.7.3 / Gradle 8.9 / Nix Flake
- **DI**: Hilt 2.51.1
- **UI**: Jetpack Compose (BOM 2024.12.01) / Material3
- **Database**: Room 2.4.2 / KSP 2.0.21-1.0.26
- **Async**: Kotlin Coroutines 1.9.0
- **JSON**: kotlinx-serialization 1.7.3

## Project Structure

```
app/
├── src/main/kotlin/net/ktnox/mobileledger/
│   ├── di/                    # Hilt modules
│   ├── domain/model/          # Domain models
│   ├── data/repository/       # Repositories
│   ├── db/                    # Room entities & DAOs
│   └── ui/                    # Compose UI
├── src/test/                  # Unit tests
└── src/androidTest/           # Instrumentation tests
```

## Architecture

```
UI (Compose) → ViewModel → Repository → DAO → Room
                   ↓
            Domain Models
```

## Rules Reference

Detailed guidelines are defined in `.claude/rules/`:

| File | Content |
|------|---------|
| `coding-style.md` | Kotlin style (val/var, nullability, scope functions) |
| `testing.md` | Test coverage, fakes, ViewModel testing |
| `security.md` | Credentials, logging, Room queries |
| `patterns.md` | Repository pattern, domain models, UiState |
| `git-workflow.md` | Commit format, PR guidelines |
| `performance.md` | Compose optimization, database performance |

## Available Agents

| Agent | Purpose | Usage |
|-------|---------|-------|
| `planner` | Implementation planning | Complex features |
| `architect` | Architecture decisions | Design reviews |
| `code-reviewer` | Code quality review | After code changes |
| `security-reviewer` | Security analysis | Sensitive code |
| `tdd-guide` | Test-driven development | Writing tests |
| `ui-test-runner` | UI testing | Device verification |
| `build-error-resolver` | Build issues | Compilation errors |
| `doc-updater` | Documentation | Keep docs current |
| `refactor-cleaner` | Dead code cleanup | Code maintenance |

## Available Commands

| Command | Description |
|---------|-------------|
| `/plan` | Create implementation plan |
| `/tdd` | TDD workflow |
| `/code-review` | Review code changes |
| `/build-fix` | Fix build errors |
| `/verify` | Full verification |
| `/refactor-clean` | Remove dead code |
| `/test-coverage` | Analyze coverage |
| `/update-docs` | Update documentation |

## Device Debugging

### adb MCP Tools

| Tool | Purpose |
|------|---------|
| `adb_devices` | List connected devices |
| `adb_logcat` | View logs |
| `inspect_ui` | UI hierarchy |
| `dump_image` | Screenshot |
| `adb_activity_manager` | Launch activities |
| `adb_shell` | Shell commands |

### Package Info

| Build | Package |
|-------|---------|
| Debug | `net.ktnox.mobileledger.debug` |
| Release | `net.ktnox.mobileledger` |

### Launch App
```
adb_activity_manager:
  amCommand: start
  amArgs: -n net.ktnox.mobileledger.debug/net.ktnox.mobileledger.ui.activity.SplashActivity
```

## Hilt Modules

### RepositoryModule
- `ProfileRepository`
- `TransactionRepository`
- `AccountRepository`
- `TemplateRepository`
- `CurrencyRepository`

### UseCaseModule
- `TransactionSender`
- `TransactionSyncer`
- `ConfigBackup`
- `DatabaseInitializer`
- `VersionDetector`

### ServiceModule
- `BackgroundTaskManager`
- `CurrencyFormatter`
- `AppStateService`

## Repository Pattern

### Naming Conventions
- `observe*()`: Returns Flow (reactive)
- `get*()`: Returns single value (suspend)
- `save*()`, `delete*()`: Mutations (suspend)

### Example
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    val profiles = profileRepository.observeAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```

## Domain Models

| Model | Use Case |
|-------|----------|
| `Profile` | Profile settings |
| `Transaction`, `TransactionLine` | Transaction data |
| `Account` | Account info |
| `Template`, `TemplateLine` | Templates |
| `Currency` | Currency settings |

## Testing

### Available Fakes
- `FakeProfileRepository`
- `FakeTransactionRepository`
- `FakeTransactionSender`
- `FakeConfigBackup`
- `FakeCurrencyFormatter`

## Verification Checklist

- [ ] Tests pass: `nix run .#test`
- [ ] Build succeeds: `nix run .#build`
- [ ] Lint clean: `pre-commit run --all-files`
- [ ] App launches on device
- [ ] No E-level errors in logcat
- [ ] Changed functionality works

## Recent Changes

- Migrated to everything-claude-code configuration
- Added 9 specialized agents
- Added 6 rules, 8 commands, 5 skills
- Added hooks.json for workflow automation
- Optimized CLAUDE.md to reference .claude/rules/ (reduced token duplication)
