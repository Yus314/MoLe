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
 * バリデーション結果を型安全に表現するsealed class
 */
sealed class ValidationResult {
    /**
     * バリデーション成功
     */
    data object Success : ValidationResult()

    /**
     * バリデーション失敗
     */
    data class Error(
        val reasons: List<String>
    ) : ValidationResult() {
        constructor(reason: String) : this(listOf(reason))
    }

    /**
     * 成功かどうか
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * 失敗かどうか
     */
    val isError: Boolean get() = this is Error
}
