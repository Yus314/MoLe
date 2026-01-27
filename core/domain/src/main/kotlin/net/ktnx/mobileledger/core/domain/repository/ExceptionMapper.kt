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

package net.ktnx.mobileledger.core.domain.repository

import net.ktnx.mobileledger.core.domain.model.AppError

/**
 * Interface for mapping exceptions to application errors.
 *
 * This interface allows repository implementations in core:data to handle
 * exceptions without depending on app-specific exception mappers.
 *
 * The app module provides the implementation that knows how to map
 * various exceptions (network, database, sync, etc.) to AppError.
 */
interface ExceptionMapper {
    /**
     * Maps a throwable to an AppError.
     *
     * @param e The throwable to map
     * @return An appropriate AppError subtype
     */
    fun map(e: Throwable): AppError
}
