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
import net.ktnx.mobileledger.di.UseCaseModule
import net.ktnx.mobileledger.domain.usecase.ConfigBackup
import net.ktnx.mobileledger.domain.usecase.DatabaseInitializer
import net.ktnx.mobileledger.domain.usecase.TransactionSyncer
import net.ktnx.mobileledger.domain.usecase.VersionDetector
import net.ktnx.mobileledger.fake.FakeConfigBackup
import net.ktnx.mobileledger.fake.FakeDatabaseInitializer
import net.ktnx.mobileledger.fake.FakeTransactionSyncer
import net.ktnx.mobileledger.fake.FakeVersionDetector

/**
 * Test module that replaces UseCaseModule in Hilt instrumented tests.
 *
 * This module provides Fake implementations of all use case interfaces,
 * enabling isolated testing without real network or database operations.
 *
 * Usage in tests:
 * ```
 * @HiltAndroidTest
 * @UninstallModules(UseCaseModule::class)
 * class MyViewModelTest {
 *     @get:Rule
 *     val hiltRule = HiltAndroidRule(this)
 *
 *     @Inject
 *     lateinit var transactionSyncer: TransactionSyncer
 *
 *     @Before
 *     fun setup() {
 *         hiltRule.inject()
 *         // transactionSyncer is now FakeTransactionSyncer
 *     }
 * }
 * ```
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [UseCaseModule::class]
)
object TestUseCaseModule {

    /**
     * T036: Provides FakeTransactionSyncer for testing sync operations.
     *
     * Test scenarios:
     * - Success: syncer.shouldSucceed = true
     * - Failure: syncer.shouldSucceed = false; syncer.errorToThrow = SyncError.NetworkError()
     * - Progress: syncer.progressSteps = 5; syncer.delayPerStepMs = 100
     */
    @Provides
    @Singleton
    fun provideTransactionSyncer(): TransactionSyncer = FakeTransactionSyncer()

    /**
     * T037: Provides FakeConfigBackup for testing backup/restore operations.
     *
     * Test scenarios:
     * - Backup success: configBackup.shouldBackupSucceed = true
     * - Restore success: configBackup.shouldRestoreSucceed = true
     * - Failure: configBackup.shouldBackupSucceed = false
     */
    @Provides
    @Singleton
    fun provideConfigBackup(): ConfigBackup = FakeConfigBackup()

    /**
     * T038: Provides FakeVersionDetector for testing version detection.
     *
     * Test scenarios:
     * - Success: versionDetector.detectedVersion = "1.32"
     * - Failure: versionDetector.shouldSucceed = false
     */
    @Provides
    @Singleton
    fun provideVersionDetector(): VersionDetector = FakeVersionDetector()

    /**
     * T039: Provides FakeDatabaseInitializer for testing initialization.
     *
     * Test scenarios:
     * - Has profiles: initializer.hasProfiles = true
     * - No profiles: initializer.hasProfiles = false
     * - Failure: initializer.shouldSucceed = false
     */
    @Provides
    @Singleton
    fun provideDatabaseInitializer(): DatabaseInitializer = FakeDatabaseInitializer()
}
