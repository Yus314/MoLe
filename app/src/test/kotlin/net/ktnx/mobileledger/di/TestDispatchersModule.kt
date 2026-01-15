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

package net.ktnx.mobileledger.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher

/**
 * Test module that replaces DispatcherModule in Hilt instrumented tests.
 *
 * This module provides TestDispatcher implementations that enable:
 * - Instant test completion (no real delays)
 * - Virtual time control with advanceTimeBy() and advanceUntilIdle()
 * - Deterministic concurrent behavior testing
 *
 * Usage in instrumented tests:
 * ```kotlin
 * @HiltAndroidTest
 * @UninstallModules(DispatcherModule::class)
 * class MyUseCaseTest {
 *     @get:Rule
 *     val hiltRule = HiltAndroidRule(this)
 *
 *     @Inject
 *     @IoDispatcher
 *     lateinit var ioDispatcher: CoroutineDispatcher
 *
 *     @Before
 *     fun setup() {
 *         hiltRule.inject()
 *         // ioDispatcher is now TestDispatcher
 *     }
 *
 *     @Test
 *     fun testWithVirtualTime() = runTest {
 *         // delay(1000) completes instantly
 *         advanceUntilIdle()
 *     }
 * }
 * ```
 *
 * Note: For unit tests (non-Hilt), use MainDispatcherRule and
 * inject TestDispatcher directly in constructor.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DispatcherModule::class]
)
object TestDispatchersModule {

    /**
     * Shared TestDispatcher instance for consistent virtual time across all dispatchers.
     *
     * Using a single TestDispatcher ensures that:
     * - All coroutines share the same virtual clock
     * - advanceUntilIdle() advances all pending work
     * - Time-based assertions are deterministic
     */
    private val sharedTestDispatcher: TestDispatcher = StandardTestDispatcher()

    /**
     * Provides TestDispatcher as IoDispatcher for I/O operations.
     *
     * In production: Dispatchers.IO (thread pool for blocking I/O)
     * In tests: TestDispatcher (virtual time, instant completion)
     */
    @IoDispatcher
    @Provides
    @Singleton
    fun provideTestIoDispatcher(): CoroutineDispatcher = sharedTestDispatcher

    /**
     * Provides TestDispatcher as DefaultDispatcher for CPU-bound work.
     *
     * In production: Dispatchers.Default (CPU-bound computation)
     * In tests: TestDispatcher (virtual time, instant completion)
     */
    @DefaultDispatcher
    @Provides
    @Singleton
    fun provideTestDefaultDispatcher(): CoroutineDispatcher = sharedTestDispatcher

    /**
     * Provides TestDispatcher as MainDispatcher for UI operations.
     *
     * In production: Dispatchers.Main (Android main thread)
     * In tests: TestDispatcher (virtual time, instant completion)
     *
     * Note: For unit tests, prefer MainDispatcherRule which handles
     * setup/teardown automatically with JUnit lifecycle.
     */
    @MainDispatcher
    @Provides
    @Singleton
    fun provideTestMainDispatcher(): CoroutineDispatcher = sharedTestDispatcher
}
