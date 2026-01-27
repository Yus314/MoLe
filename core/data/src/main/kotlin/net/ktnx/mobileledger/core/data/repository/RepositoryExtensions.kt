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

package net.ktnx.mobileledger.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import net.ktnx.mobileledger.core.domain.model.AppError
import net.ktnx.mobileledger.core.domain.model.AppException
import net.ktnx.mobileledger.core.domain.repository.ExceptionMapper

/**
 * suspend関数をResult<T>でラップするユーティリティ
 *
 * 例外をキャッチしてAppExceptionにマッピングし、Result.failureとして返す。
 *
 * 使用例:
 * ```kotlin
 * override suspend fun getById(id: Long): Result<Entity?> = safeCall(mapper) {
 *     dao.getById(id)
 * }
 * ```
 */
suspend inline fun <T> safeCall(mapper: ExceptionMapper, crossinline block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: Exception) {
    Result.failure(AppException(mapper.map(e)))
}

/**
 * FlowをResult<T>を返すFlowに変換する
 *
 * 例外をキャッチしてAppExceptionにマッピングし、Result.failureとしてエミットする。
 *
 * 使用例:
 * ```kotlin
 * override fun observeAll(): Flow<Result<List<Entity>>> =
 *     dao.observeAll()
 *         .map { entities -> entities.map { it.toDomain() } }
 *         .asResultFlow(mapper)
 * ```
 */
fun <T> Flow<T>.asResultFlow(mapper: ExceptionMapper): Flow<Result<T>> = this.map<T, Result<T>> { Result.success(it) }
    .catch { e -> emit(Result.failure(AppException(mapper.map(e)))) }

/**
 * ResultのfailureをAppErrorに変換する拡張関数
 *
 * 既にAppExceptionでない場合、ExceptionMapperを使用して変換する。
 */
fun <T> Result<T>.mapErrorToAppError(mapper: ExceptionMapper): Result<T> = this.fold(
    onSuccess = { Result.success(it) },
    onFailure = { e ->
        when (e) {
            is AppException -> Result.failure(e)
            else -> Result.failure(AppException(mapper.map(e)))
        }
    }
)

/**
 * ResultからAppErrorを抽出するヘルパー
 *
 * 使用例:
 * ```kotlin
 * result.onFailure { e ->
 *     val appError = e.toAppError(mapper)
 *     showError(appError.message)
 * }
 * ```
 */
fun Throwable.toAppError(mapper: ExceptionMapper): AppError = when (this) {
    is AppException -> this.appError
    else -> mapper.map(this)
}
