---
name: verify
description: Run full verification workflow (test → build → install → device check)
---

# Full Verification Workflow

Complete verification for code changes, especially UI modifications.

## Verification Steps

### Step 1: Run Tests
```bash
nix run .#test
```
Ensure all unit tests pass.

### Step 2: Build and Install
```bash
nix run .#verify
```
This runs: test → build → install on connected device

### Step 3: Device Verification

**Launch App**
```
adb_activity_manager:
  amCommand: start
  amArgs: -n net.ktnx.mobileledger.debug/net.ktnx.mobileledger.ui.activity.SplashActivity
```

**Check Logs**
```
adb_logcat:
  filter: mobileledger
  lines: 50
```

**Capture Screenshot**
```
dump_image:
  asBase64: false
```

**Inspect UI (if needed)**
```
inspect_ui:
  asBase64: false
```

### Step 4: Manual Verification Checklist

- [ ] App launches without crash
- [ ] No E-level errors in logcat
- [ ] UI displays correctly
- [ ] Changed functionality works
- [ ] No regressions in related areas

## Quick Commands

| Check | Command |
|-------|---------|
| Tests only | `nix run .#test` |
| Build only | `nix run .#build` |
| Full workflow | `nix run .#verify` |
| Coverage | `nix run .#coverage` |
| Lint | `nix run .#lint` |

## Package Info

| Build | Package |
|-------|---------|
| Debug | `net.ktnx.mobileledger.debug` |
| Release | `net.ktnx.mobileledger` |

Note: Activity class paths use original package name even for debug builds.

## Troubleshooting

| Issue | Check |
|-------|-------|
| App crashes | `adb_logcat` for stack trace |
| UI broken | `dump_image` + `inspect_ui` |
| Data issues | `adb_shell` to check database |
| Build fails | See `/build-fix` command |
