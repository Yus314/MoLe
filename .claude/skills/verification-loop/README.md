# Verification Loop Skill

Systematic verification process for Android code changes.

## The Verification Loop

```
    ┌────────────────────────────────────────────┐
    │                                            │
    │   Code → Test → Build → Install → Check    │
    │    ↑                                 │     │
    │    └─────────── Fix Issues ──────────┘     │
    │                                            │
    └────────────────────────────────────────────┘
```

## Steps

### 1. Code Review
Before verification, review code:
- Run lint: `pre-commit run --all-files`
- Check for issues

### 2. Unit Tests
```bash
nix run .#test
```
Ensure all tests pass.

### 3. Build
```bash
nix run .#build
```
Ensure compilation succeeds.

### 4. Install on Device
```bash
nix run .#verify
```
Or manually: `nix run .#install`

### 5. Device Verification

**Launch App**
```
adb_activity_manager:
  amCommand: start
  amArgs: -n net.ktnox.mobileledger.debug/net.ktnx.mobileledger.ui.activity.SplashActivity
```

**Check Logs**
```
adb_logcat:
  filter: mobileledger
  lines: 50
```

**Screenshot**
```
dump_image:
  asBase64: false
```

**UI Hierarchy (if needed)**
```
inspect_ui:
  asBase64: false
```

## Verification Checklist

### Basic Checks
- [ ] App launches without crash
- [ ] No E-level errors in logcat
- [ ] UI displays correctly

### Feature-Specific Checks
- [ ] Changed functionality works
- [ ] Related features not broken
- [ ] Edge cases handled

### Performance Checks
- [ ] App responsive
- [ ] No ANR warnings
- [ ] Memory reasonable

## Quick Reference

| Action | Command |
|--------|---------|
| Full workflow | `nix run .#verify` |
| Tests only | `nix run .#test` |
| Build only | `nix run .#build` |
| Coverage | `nix run .#coverage` |
| Lint | `nix run .#lint` |

## Troubleshooting

| Issue | Action |
|-------|--------|
| Test fails | Fix code, re-run tests |
| Build fails | Use `/build-fix` |
| App crashes | Check logcat |
| UI broken | Check `dump_image` |
