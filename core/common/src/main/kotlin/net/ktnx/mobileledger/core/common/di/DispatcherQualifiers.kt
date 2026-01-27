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

package net.ktnx.mobileledger.core.common.di

import javax.inject.Qualifier

/**
 * I/O操作用のDispatcherを識別するためのQualifier
 *
 * UseCase実装でDispatcherを注入する際に使用する。
 * テスト時はTestDispatcherに置き換えることができる。
 *
 * ## Usage
 *
 * ```kotlin
 * class MyUseCaseImpl @Inject constructor(
 *     @IoDispatcher private val ioDispatcher: CoroutineDispatcher
 * ) : MyUseCase {
 *     override suspend fun execute() = withContext(ioDispatcher) {
 *         // I/O operations
 *     }
 * }
 * ```
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * デフォルト計算用のDispatcherを識別するためのQualifier
 *
 * CPU集約処理で使用する。
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/**
 * メインスレッド用のDispatcherを識別するためのQualifier
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher
