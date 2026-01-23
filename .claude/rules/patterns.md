# Architecture Patterns Rules

## Layer Architecture

```
UI (Compose) → ViewModel → Repository → DAO → Room
                   ↓
            Domain Models
```

## Repository Pattern

### ViewModel Must Use Repository
```kotlin
// BAD: DAO in ViewModel
@HiltViewModel
class MyViewModel @Inject constructor(
    private val profileDao: ProfileDAO  // Never do this
) : ViewModel()

// GOOD: Repository in ViewModel
@HiltViewModel
class MyViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel()
```

### Repository Methods
- `observe*()`: Returns Flow (reactive)
- `get*()`: Returns single value (suspend)
- `save*()`, `delete*()`: Mutations (suspend)

## Domain Models

### ViewModel Must Use Domain Models
```kotlin
// BAD: DB entity in ViewModel
val profile: StateFlow<db.Profile>  // Never do this

// GOOD: Domain model in ViewModel
val profile: StateFlow<Profile>  // domain.model.Profile
```

### Available Domain Models
- `Profile` (not `db.Profile`)
- `Transaction`, `TransactionLine`
- `Account`
- `Template`, `TemplateLine`
- `Currency`

## UiState Pattern

```kotlin
data class MyUiState(
    val isLoading: Boolean = false,
    val data: List<Item> = emptyList(),
    val error: String? = null
)

sealed class MyEvent {
    data class ItemClicked(val id: Long) : MyEvent()
    object Refresh : MyEvent()
}

sealed class MyEffect {
    data class ShowError(val message: String) : MyEffect()
    object NavigateBack : MyEffect()
}
```

## ViewModel Pattern

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyUiState())
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()

    private val _effects = Channel<MyEffect>()
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: MyEvent) {
        when (event) {
            is MyEvent.Refresh -> refresh()
            is MyEvent.ItemClicked -> handleClick(event.id)
        }
    }
}
```

## Compose Pattern

```kotlin
@Composable
fun MyScreen(
    viewModel: MyViewModel = hiltViewModel(),
    onNavigate: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MyEffect.NavigateBack -> onNavigate()
            }
        }
    }

    // UI content
}
```

## Never Do

- Access DAO from ViewModel
- Use `db.*` entities in ViewModel
- Put business logic in Composables
- Use `var` for StateFlow (use `_private` + `public`)
