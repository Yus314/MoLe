# Quickstart: Data Layer Repository Migration

**Feature**: 008-data-layer-repository
**Date**: 2026-01-10

## Overview

このドキュメントは、Repository層の実装を素早く開始するためのガイドです。

---

## Prerequisites

1. ブランチ `008-data-layer-repository` にチェックアウト済み
2. `nix develop` で開発環境に入っている
3. `nix run .#build` が成功する状態

---

## Step 1: Create Repository Directory Structure

```bash
mkdir -p app/src/main/kotlin/net/ktnx/mobileledger/data/repository
mkdir -p app/src/test/kotlin/net/ktnx/mobileledger/data/repository
```

---

## Step 2: Create ProfileRepository (P2 Priority)

### 2.1 Interface

```kotlin
// app/src/main/kotlin/net/ktnx/mobileledger/data/repository/ProfileRepository.kt
package net.ktnx.mobileledger.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import net.ktnx.mobileledger.db.Profile

interface ProfileRepository {
    val currentProfile: StateFlow<Profile?>
    fun setCurrentProfile(profile: Profile?)
    fun getAllProfiles(): Flow<List<Profile>>
    suspend fun getProfileById(id: Long): Profile?
    suspend fun insertProfile(profile: Profile): Long
    suspend fun updateProfile(profile: Profile)
    suspend fun deleteProfile(profile: Profile)
}
```

### 2.2 Implementation

```kotlin
// app/src/main/kotlin/net/ktnx/mobileledger/data/repository/ProfileRepositoryImpl.kt
package net.ktnx.mobileledger.data.repository

import androidx.lifecycle.asFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import net.ktnx.mobileledger.dao.ProfileDAO
import net.ktnx.mobileledger.db.Profile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileDAO: ProfileDAO
) : ProfileRepository {

    private val _currentProfile = MutableStateFlow<Profile?>(null)
    override val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    override fun setCurrentProfile(profile: Profile?) {
        _currentProfile.value = profile
    }

    override fun getAllProfiles(): Flow<List<Profile>> =
        profileDAO.getAllOrdered().asFlow()

    override suspend fun getProfileById(id: Long): Profile? =
        withContext(Dispatchers.IO) {
            profileDAO.getByIdSync(id)
        }

    override suspend fun insertProfile(profile: Profile): Long =
        withContext(Dispatchers.IO) {
            profileDAO.insertLastSync(profile)
        }

    override suspend fun updateProfile(profile: Profile) =
        withContext(Dispatchers.IO) {
            profileDAO.updateSync(profile)
        }

    override suspend fun deleteProfile(profile: Profile) =
        withContext(Dispatchers.IO) {
            profileDAO.deleteSync(profile)
        }
}
```

---

## Step 3: Create RepositoryModule

```kotlin
// app/src/main/kotlin/net/ktnx/mobileledger/di/RepositoryModule.kt
package net.ktnx.mobileledger.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.data.repository.ProfileRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: ProfileRepositoryImpl
    ): ProfileRepository
}
```

---

## Step 4: Rename Data.kt to AppStateManager

```kotlin
// app/src/main/kotlin/net/ktnx/mobileledger/model/AppStateManager.kt
package net.ktnx.mobileledger.model

// Keep only UI/App state, remove profiles and profile LiveData
object AppStateManager {
    @JvmField
    val backgroundTasksRunning = MutableLiveData(false)

    @JvmField
    val backgroundTaskProgress = MutableLiveData<RetrieveTransactionsTask.Progress>()

    @JvmField
    val drawerOpen = MutableLiveData(false)

    // ... keep other UI state
    // REMOVE: profiles, profile (moved to ProfileRepository)
}
```

Update DataModule:

```kotlin
// app/src/main/kotlin/net/ktnx/mobileledger/di/AppStateModule.kt
package net.ktnx.mobileledger.di

@Module
@InstallIn(SingletonComponent::class)
object AppStateModule {

    @Provides
    @Singleton
    fun provideAppStateManager(): AppStateManager = AppStateManager
}
```

---

## Step 5: Update ViewModel (Example: ProfileDetailViewModel)

```kotlin
// Before
@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    private val profileDAO: ProfileDAO,
    savedStateHandle: SavedStateHandle
) : ViewModel()

// After
@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val profiles = profileRepository.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
```

---

## Step 6: Write Unit Test

```kotlin
// app/src/test/kotlin/net/ktnx/mobileledger/data/repository/ProfileRepositoryTest.kt
package net.ktnx.mobileledger.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileRepositoryTest {

    private lateinit var fakeProfileDAO: FakeProfileDAO
    private lateinit var repository: ProfileRepositoryImpl

    @Before
    fun setup() {
        fakeProfileDAO = FakeProfileDAO()
        repository = ProfileRepositoryImpl(fakeProfileDAO)
    }

    @Test
    fun `currentProfile is null initially`() {
        assertNull(repository.currentProfile.value)
    }

    @Test
    fun `setCurrentProfile updates state`() {
        val profile = Profile(id = 1, name = "Test")

        repository.setCurrentProfile(profile)

        assertEquals(profile, repository.currentProfile.value)
    }

    @Test
    fun `getAllProfiles returns ordered list`() = runTest {
        fakeProfileDAO.insertProfile(Profile(name = "Profile B", orderNo = 2))
        fakeProfileDAO.insertProfile(Profile(name = "Profile A", orderNo = 1))

        val profiles = repository.getAllProfiles().first()

        assertEquals(2, profiles.size)
        assertEquals("Profile A", profiles[0].name)
    }
}
```

---

## Step 7: Build and Test

```bash
# Run tests
nix run .#test

# Build
nix run .#build

# Full verification (test + build + install)
nix run .#verify
```

---

## Migration Checklist

- [ ] Create `data/repository/` directory
- [ ] Create ProfileRepository interface + impl
- [ ] Create TransactionRepository interface + impl
- [ ] Create AccountRepository interface + impl
- [ ] Create TemplateRepository interface + impl
- [ ] Create RepositoryModule
- [ ] Rename Data.kt to AppStateManager
- [ ] Update DataModule to AppStateModule
- [ ] Migrate MainViewModel
- [ ] Migrate NewTransactionViewModel
- [ ] Migrate ProfileDetailViewModel
- [ ] Migrate BackupsViewModel
- [ ] Write unit tests for each Repository
- [ ] Run full verification

---

## Common Patterns

### LiveData to Flow Conversion

```kotlin
// DAO returns LiveData
fun getAllOrdered(): LiveData<List<Profile>>

// Repository converts to Flow
override fun getAllProfiles(): Flow<List<Profile>> =
    profileDAO.getAllOrdered().asFlow()
```

### Sync Methods with Coroutines

```kotlin
// DAO has sync method
fun getByIdSync(id: Long): Profile?

// Repository wraps with Dispatchers.IO
override suspend fun getProfileById(id: Long): Profile? =
    withContext(Dispatchers.IO) {
        profileDAO.getByIdSync(id)
    }
```

### StateFlow in ViewModel

```kotlin
val profiles: StateFlow<List<Profile>> = profileRepository
    .getAllProfiles()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
```
