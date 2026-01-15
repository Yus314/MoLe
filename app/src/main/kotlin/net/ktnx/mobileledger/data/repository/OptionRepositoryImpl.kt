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

package net.ktnx.mobileledger.data.repository

import androidx.lifecycle.asFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import net.ktnx.mobileledger.dao.OptionDAO
import net.ktnx.mobileledger.db.Option

/**
 * Implementation of [OptionRepository] that wraps the existing OptionDAO.
 *
 * This implementation:
 * - Converts LiveData to Flow for reactive data access
 * - Uses Dispatchers.IO for database operations
 * - Delegates all operations to the underlying DAO
 *
 * Thread-safety: All operations are safe to call from any coroutine context.
 */
@Singleton
class OptionRepositoryImpl @Inject constructor(
    private val optionDAO: OptionDAO
) : OptionRepository {

    // ========================================
    // Query Operations
    // ========================================

    override fun getOption(profileId: Long, name: String): Flow<Option?> = optionDAO.load(profileId, name).asFlow()

    override suspend fun getOptionSync(profileId: Long, name: String): Option? = withContext(Dispatchers.IO) {
        optionDAO.loadSync(profileId, name)
    }

    override suspend fun getAllOptionsForProfileSync(profileId: Long): List<Option> = withContext(Dispatchers.IO) {
        optionDAO.allForProfileSync(profileId)
    }

    // ========================================
    // Mutation Operations
    // ========================================

    override suspend fun insertOption(option: Option): Long = withContext(Dispatchers.IO) {
        optionDAO.insertSync(option)
    }

    override suspend fun deleteOption(option: Option) {
        withContext(Dispatchers.IO) {
            optionDAO.deleteSync(option)
        }
    }

    override suspend fun deleteOptionsForProfile(profileId: Long) {
        withContext(Dispatchers.IO) {
            val options = optionDAO.allForProfileSync(profileId)
            optionDAO.deleteSync(options)
        }
    }

    override suspend fun deleteAllOptions() {
        withContext(Dispatchers.IO) {
            optionDAO.deleteAllSync()
        }
    }
}
