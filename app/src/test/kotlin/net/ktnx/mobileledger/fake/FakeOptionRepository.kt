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

package net.ktnx.mobileledger.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.ktnx.mobileledger.core.domain.model.AppOption
import net.ktnx.mobileledger.core.domain.repository.OptionRepository

/**
 * Fake OptionRepository for ViewModel testing.
 */
class FakeOptionRepository : OptionRepository {
    private val options = mutableMapOf<String, AppOption>()

    private fun makeKey(profileId: Long, name: String): String = "$profileId:$name"

    override fun observeOption(profileId: Long, name: String): Flow<AppOption?> =
        MutableStateFlow(options[makeKey(profileId, name)])

    override suspend fun getOption(profileId: Long, name: String): AppOption? = options[makeKey(profileId, name)]

    override suspend fun getAllOptionsForProfile(profileId: Long): List<AppOption> =
        options.values.filter { it.profileId == profileId }

    override suspend fun insertOption(option: AppOption): Long {
        options[makeKey(option.profileId, option.name)] = option
        return 1L
    }

    override suspend fun deleteOption(option: AppOption) {
        options.remove(makeKey(option.profileId, option.name))
    }

    override suspend fun deleteOptionsForProfile(profileId: Long) {
        options.keys.filter { it.startsWith("$profileId:") }.forEach { options.remove(it) }
    }

    override suspend fun deleteAllOptions() {
        options.clear()
    }

    override suspend fun setLastSyncTimestamp(profileId: Long, timestamp: Long) {
        insertOption(AppOption(profileId, AppOption.OPT_LAST_SCRAPE, timestamp.toString()))
    }

    override suspend fun getLastSyncTimestamp(profileId: Long): Long? =
        options[makeKey(profileId, AppOption.OPT_LAST_SCRAPE)]?.valueAsLong()
}
