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

package net.ktnx.mobileledger.domain.model

/**
 * SyncErrorをラップする例外クラス
 *
 * Flow内でSyncErrorを伝播するために使用。
 * catch節でSyncExceptionを捕捉し、syncErrorプロパティから
 * 型安全なエラー情報を取得できる。
 *
 * 使用例:
 * ```kotlin
 * flow.catch { e ->
 *     val error = when (e) {
 *         is SyncException -> e.syncError
 *         else -> SyncError.UnknownError(cause = e)
 *     }
 *     _state.value = SyncState.Failed(error)
 * }
 * ```
 */
class SyncException(
    val syncError: SyncError
) : Exception(syncError.message)
