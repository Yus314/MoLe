# Compose Patterns Skill

Jetpack Compose patterns and best practices for MoLe.

## State Management

### UiState Pattern
```kotlin
data class MyScreenUiState(
    val isLoading: Boolean = false,
    val items: List<Item> = emptyList(),
    val error: String? = null
)
```

### Event Pattern
```kotlin
sealed class MyScreenEvent {
    data class ItemClicked(val id: Long) : MyScreenEvent()
    object Refresh : MyScreenEvent()
}
```

### Effect Pattern
```kotlin
sealed class MyScreenEffect {
    data class ShowError(val message: String) : MyScreenEffect()
    object NavigateBack : MyScreenEffect()
}
```

## ViewModel Pattern

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyScreenUiState())
    val uiState: StateFlow<MyScreenUiState> = _uiState.asStateFlow()

    private val _effects = Channel<MyScreenEffect>()
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: MyScreenEvent) {
        when (event) {
            is MyScreenEvent.ItemClicked -> handleClick(event.id)
            is MyScreenEvent.Refresh -> refresh()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val items = repository.getItems()
                _uiState.update { it.copy(items = items, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
```

## Screen Composable Pattern

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen(
    viewModel: MyViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MyScreenEffect.NavigateBack -> onNavigateBack()
                is MyScreenEffect.ShowError -> { /* Show snackbar */ }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Screen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { paddingValues ->
        MyScreenContent(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun MyScreenContent(
    uiState: MyScreenUiState,
    onEvent: (MyScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Stateless content
}
```

## Performance Patterns

### Remember
```kotlin
// Remember expensive computations
val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
```

### derivedStateOf
```kotlin
val filteredList by remember(items) {
    derivedStateOf { items.filter { it.active } }
}
```

### LazyColumn with Keys
```kotlin
LazyColumn {
    items(items, key = { it.id }) { item ->
        ItemRow(item)
    }
}
```

### Stable Lambda
```kotlin
// Bad: recreated every recomposition
Button(onClick = { viewModel.onClick(item.id) })

// Good: stable reference
val onClick = remember(item.id) { { viewModel.onClick(item.id) } }
Button(onClick = onClick)
```

## Common Components

### Loading State
```kotlin
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
```

### Error State
```kotlin
@Composable
fun ErrorMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message)
        Button(onClick = onRetry) { Text("Retry") }
    }
}
```

### Empty State
```kotlin
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}
```
