/*
 * Copyright © 2021 Damyan Ivanov.
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

package net.ktnx.mobileledger.core.database.dao

/**
 * Base class for Room DAOs providing common sync methods.
 *
 * All async operations should use Kotlin Coroutines via Repository layer
 * instead of the legacy Executor pattern.
 */
abstract class BaseDAO<T> {
    abstract fun insertSync(item: T): Long

    abstract fun updateSync(item: T)

    abstract fun deleteSync(item: T)
}
