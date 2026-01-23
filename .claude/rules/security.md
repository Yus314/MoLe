# Security Rules for Android Development

## CRITICAL: Never Do

1. **Never hardcode credentials**
   - No API keys in source code
   - No passwords or tokens
   - Use `BuildConfig` fields for debug/release

2. **Never log sensitive data**
   - No passwords in logs
   - No tokens in logs
   - No PII in logs

3. **Never store sensitive data in plain text**
   - Use `EncryptedSharedPreferences` for secrets
   - Use Android Keystore for cryptographic keys

## Data Storage

### SharedPreferences
```kotlin
// BAD
prefs.putString("api_key", key)

// GOOD
EncryptedSharedPreferences.create(...)
```

### Room Database
- All queries use parameterized binding (`:param`)
- Never concatenate user input into queries
- Export schema disabled in release builds

## Network

- HTTPS only (no cleartext traffic)
- Certificate validation enabled
- Sensitive data not in URLs

## Intents

- Validate all intent extras before use
- Use `PendingIntent.FLAG_IMMUTABLE`
- Limit exported components

## Immediate Actions

If you discover a security issue:
1. Flag it immediately
2. Do not commit vulnerable code
3. Propose secure alternative
