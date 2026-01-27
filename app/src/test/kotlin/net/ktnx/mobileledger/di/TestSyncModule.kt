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
import net.ktnx.mobileledger.core.sync.TransactionSyncer
import net.ktnx.mobileledger.core.sync.di.SyncModule
import net.ktnx.mobileledger.fake.FakeTransactionSyncer

/**
 * Test module that replaces SyncModule in Hilt instrumented tests.
 *
 * Provides FakeTransactionSyncer for testing sync operations.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [SyncModule::class]
)
object TestSyncModule {

    /**
     * Provides FakeTransactionSyncer for testing sync operations.
     *
     * Test scenarios:
     * - Success: syncer.shouldSucceed = true
     * - Failure: syncer.shouldSucceed = false; syncer.errorToThrow = SyncError.NetworkError()
     * - Progress: syncer.progressSteps = 5; syncer.delayPerStepMs = 100
     */
    @Provides
    @Singleton
    fun provideTransactionSyncer(): TransactionSyncer = FakeTransactionSyncer()
}
