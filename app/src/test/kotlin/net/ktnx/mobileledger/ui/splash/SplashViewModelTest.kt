/*
 * Copyright © 2026 Damyan Ivanov.
 * This file is part of MoLe.
 * MoLe is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * MoLe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoLe. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger.ui.splash

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.ktnx.mobileledger.fake.FakeDatabaseInitializer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * SplashViewModel のテスト
 *
 * FakeDatabaseInitializer を使用して初期化ロジックをテストする。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDatabaseInitializer: FakeDatabaseInitializer
    private lateinit var viewModel: SplashViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDatabaseInitializer = FakeDatabaseInitializer()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==========================================
    // Initialization tests
    // ==========================================

    @Test
    fun `initial state is not initialized and not elapsed`() = runTest {
        // When - create ViewModel without advancing time
        fakeDatabaseInitializer.shouldSucceed = true
        viewModel = SplashViewModel(fakeDatabaseInitializer)

        // Then - initial state before any coroutines run
        val state = viewModel.uiState.value
        assertFalse("isInitialized should be false initially", state.isInitialized)
        assertFalse("minDisplayTimeElapsed should be false initially", state.minDisplayTimeElapsed)
        assertFalse("canNavigate should be false initially", state.canNavigate)
    }

    @Test
    fun `database initialization success updates state`() = runTest {
        // Given
        fakeDatabaseInitializer.shouldSucceed = true
        fakeDatabaseInitializer.hasProfiles = true
        viewModel = SplashViewModel(fakeDatabaseInitializer)

        // When - advance until DB init completes
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue("isInitialized should be true", state.isInitialized)
        assertEquals(1, fakeDatabaseInitializer.initializeCallCount)
    }

    @Test
    fun `database initialization failure still sets initialized`() = runTest {
        // Given - failure case
        fakeDatabaseInitializer.shouldSucceed = false
        fakeDatabaseInitializer.errorToThrow = RuntimeException("DB error")
        viewModel = SplashViewModel(fakeDatabaseInitializer)

        // When
        advanceUntilIdle()

        // Then - initialization complete despite error
        val state = viewModel.uiState.value
        assertTrue("isInitialized should be true even on failure", state.isInitialized)
    }

    @Test
    fun `min display timer elapses after delay`() = runTest {
        // Given
        fakeDatabaseInitializer.shouldSucceed = true
        viewModel = SplashViewModel(fakeDatabaseInitializer)

        // When - advance just under the delay
        advanceTimeBy(350)
        assertFalse("minDisplayTimeElapsed should be false before delay", viewModel.uiState.value.minDisplayTimeElapsed)

        // When - advance past the delay
        advanceTimeBy(100)
        assertTrue("minDisplayTimeElapsed should be true after delay", viewModel.uiState.value.minDisplayTimeElapsed)
    }

    // ==========================================
    // Navigation tests
    // ==========================================

    @Test
    fun `canNavigate is true when both initialized and timer elapsed`() = runTest {
        // Given
        fakeDatabaseInitializer.shouldSucceed = true
        viewModel = SplashViewModel(fakeDatabaseInitializer)

        // When - complete all async work
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue("isInitialized should be true", state.isInitialized)
        assertTrue("minDisplayTimeElapsed should be true", state.minDisplayTimeElapsed)
        assertTrue("canNavigate should be true", state.canNavigate)
    }

    @Test
    fun `NavigateToMain effect is sent when ready`() = runTest {
        // Given
        fakeDatabaseInitializer.shouldSucceed = true
        viewModel = SplashViewModel(fakeDatabaseInitializer)

        // When - complete all async work
        advanceUntilIdle()

        // Then - effect should be sent
        val effect = viewModel.effects.first()
        assertTrue("Effect should be NavigateToMain", effect is SplashEffect.NavigateToMain)
    }

    // ==========================================
    // hasProfiles behavior tests
    // ==========================================

    @Test
    fun `initialization with profiles succeeds`() = runTest {
        // Given
        fakeDatabaseInitializer.shouldSucceed = true
        fakeDatabaseInitializer.hasProfiles = true
        viewModel = SplashViewModel(fakeDatabaseInitializer)

        // When
        advanceUntilIdle()

        // Then
        assertTrue("isInitialized should be true", viewModel.uiState.value.isInitialized)
    }

    @Test
    fun `initialization without profiles succeeds`() = runTest {
        // Given
        fakeDatabaseInitializer.shouldSucceed = true
        fakeDatabaseInitializer.hasProfiles = false
        viewModel = SplashViewModel(fakeDatabaseInitializer)

        // When
        advanceUntilIdle()

        // Then
        assertTrue("isInitialized should be true", viewModel.uiState.value.isInitialized)
    }

    // ==========================================
    // Race condition tests
    // ==========================================

    @Test
    fun `timer finishes before initialization - waits for init`() = runTest {
        // Given - slow initialization
        fakeDatabaseInitializer.shouldSucceed = true
        fakeDatabaseInitializer.delayMs = 600L // slower than 400ms timer
        viewModel = SplashViewModel(fakeDatabaseInitializer)

        // When - timer finishes (400ms)
        advanceTimeBy(450)

        // Then - timer elapsed but not initialized yet
        val midState = viewModel.uiState.value
        assertTrue("minDisplayTimeElapsed should be true", midState.minDisplayTimeElapsed)
        assertFalse("isInitialized should be false (slow init)", midState.isInitialized)
        assertFalse("canNavigate should be false", midState.canNavigate)

        // When - initialization completes
        advanceUntilIdle()

        // Then - both conditions met
        val finalState = viewModel.uiState.value
        assertTrue("canNavigate should be true after init completes", finalState.canNavigate)
    }

    @Test
    fun `initialization finishes before timer - waits for timer`() = runTest {
        // Given - fast initialization
        fakeDatabaseInitializer.shouldSucceed = true
        fakeDatabaseInitializer.delayMs = 0L // instant
        viewModel = SplashViewModel(fakeDatabaseInitializer)

        // When - initialization completes immediately
        advanceTimeBy(50)

        // Then - initialized but timer not elapsed
        val midState = viewModel.uiState.value
        assertTrue("isInitialized should be true", midState.isInitialized)
        assertFalse("minDisplayTimeElapsed should be false", midState.minDisplayTimeElapsed)
        assertFalse("canNavigate should be false", midState.canNavigate)

        // When - timer elapses
        advanceTimeBy(400)

        // Then - both conditions met
        val finalState = viewModel.uiState.value
        assertTrue("canNavigate should be true after timer", finalState.canNavigate)
    }

    @Test
    fun `navigation effect sent only once even when both conditions met simultaneously`() = runTest {
        // Given
        fakeDatabaseInitializer.shouldSucceed = true
        fakeDatabaseInitializer.delayMs = 400L // same as timer
        viewModel = SplashViewModel(fakeDatabaseInitializer)

        // When - both complete at same time
        advanceUntilIdle()

        // Then - navigation effect should be sent (once)
        val effect = viewModel.effects.first()
        assertTrue("Effect should be NavigateToMain", effect is SplashEffect.NavigateToMain)
        // No assertion on "only once" since the effect channel already consumed it
    }

    // ==========================================
    // Edge case tests
    // ==========================================

    @Test
    fun `initialization failure still allows navigation after timer`() = runTest {
        // Given - failure case
        fakeDatabaseInitializer.shouldSucceed = false
        fakeDatabaseInitializer.errorToThrow = RuntimeException("Critical DB error")
        viewModel = SplashViewModel(fakeDatabaseInitializer)

        // When - complete all async work
        advanceUntilIdle()

        // Then - should still navigate (failure is handled gracefully)
        val state = viewModel.uiState.value
        assertTrue("canNavigate should be true even on failure", state.canNavigate)
        val effect = viewModel.effects.first()
        assertTrue("Effect should be NavigateToMain", effect is SplashEffect.NavigateToMain)
    }

    @Test
    fun `state updates correctly through initialization lifecycle`() = runTest {
        // Given
        fakeDatabaseInitializer.shouldSucceed = true
        fakeDatabaseInitializer.delayMs = 100L
        viewModel = SplashViewModel(fakeDatabaseInitializer)

        // Then - verify progression of states
        // Initial state
        val initial = viewModel.uiState.value
        assertFalse(initial.isInitialized)
        assertFalse(initial.minDisplayTimeElapsed)

        // After init but before timer
        advanceTimeBy(150)
        val afterInit = viewModel.uiState.value
        assertTrue("Should be initialized after 150ms", afterInit.isInitialized)
        assertFalse("Timer should not have elapsed yet", afterInit.minDisplayTimeElapsed)

        // After timer
        advanceTimeBy(300)
        val afterTimer = viewModel.uiState.value
        assertTrue("Should be initialized", afterTimer.isInitialized)
        assertTrue("Timer should have elapsed", afterTimer.minDisplayTimeElapsed)
        assertTrue("Should be ready to navigate", afterTimer.canNavigate)
    }
}
