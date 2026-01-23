# Performance Rules

## Compose Optimization

### Use remember
```kotlin
// BAD: Recreated every recomposition
val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

// GOOD: Remembered across recompositions
val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
```

### Use derivedStateOf
```kotlin
// BAD: Recalculated on every recomposition
val filteredList = items.filter { it.active }

// GOOD: Only recalculated when items change
val filteredList by remember(items) {
    derivedStateOf { items.filter { it.active } }
}
```

### LazyColumn Keys
```kotlin
// BAD: No key, poor performance on changes
LazyColumn {
    items(transactions) { transaction ->
        TransactionItem(transaction)
    }
}

// GOOD: With key for stable identity
LazyColumn {
    items(transactions, key = { it.id }) { transaction ->
        TransactionItem(transaction)
    }
}
```

### Avoid Unnecessary Recomposition
```kotlin
// BAD: Lambda recreated every recomposition
Button(onClick = { viewModel.onClick(item.id) })

// GOOD: Stable reference
val onClick = remember(item.id) { { viewModel.onClick(item.id) } }
Button(onClick = onClick)
```

## Database Performance

### Avoid N+1 Queries
```kotlin
// BAD: N+1 queries
val accounts = accountDao.getAll()
accounts.forEach { account ->
    val balance = balanceDao.getForAccount(account.id)  // N queries
}

// GOOD: Single query with join
@Query("SELECT a.*, b.amount FROM accounts a LEFT JOIN balances b ON a.id = b.account_id")
fun getAccountsWithBalances(): Flow<List<AccountWithBalance>>
```

### Use Flow for Reactive Data
```kotlin
// BAD: Manual polling
while (true) {
    val data = dao.getData()
    delay(1000)
}

// GOOD: Reactive Flow
dao.observeData().collect { data ->
    // Automatically updated
}
```

## Memory Management

### Avoid Leaks
- Cancel coroutines in `onCleared()`
- Use `viewModelScope` for ViewModel coroutines
- Avoid storing Context in ViewModel

### Large Lists
- Use `LazyColumn` not `Column` with `forEach`
- Implement pagination for large datasets
- Use `AsyncImage` for image loading

## Profiling Commands

```bash
# Check memory
adb shell dumpsys meminfo net.ktnx.mobileledger.debug

# Check CPU
adb shell top -n 1 | grep mobileledger

# Profile startup
adb shell am start -W net.ktnox.mobileledger.debug/.ui.activity.SplashActivity
```

## Performance Checklist

- [ ] LazyColumn has `key` parameter
- [ ] Heavy computations use `remember`
- [ ] No N+1 queries in Repository
- [ ] Images loaded asynchronously
- [ ] No blocking calls on main thread
