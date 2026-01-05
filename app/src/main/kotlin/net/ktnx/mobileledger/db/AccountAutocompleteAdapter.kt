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

package net.ktnx.mobileledger.db

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import net.ktnx.mobileledger.dao.AccountDAO
import net.ktnx.mobileledger.utils.Logger

class AccountAutocompleteAdapter : ArrayAdapter<String> {
    private val filter = AccountFilter()
    private val dao: AccountDAO = DB.get().accountDAO
    private var profileId: Long = Profile.NO_PROFILE_ID

    constructor(context: Context) : super(
        context,
        android.R.layout.simple_dropdown_item_1line,
        ArrayList()
    )

    constructor(context: Context, profile: Profile) : this(context) {
        profileId = profile.id
    }

    fun setProfileId(profileId: Long) {
        this.profileId = profileId
    }

    override fun getFilter(): Filter = filter

    inner class AccountFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = FilterResults()
            if (constraint == null) {
                results.count = 0
                return results
            }

            Logger.debug("acc", String.format("Looking for account '%s'", constraint))
            val matches = AccountDAO.unbox(
                if (profileId == Profile.NO_PROFILE_ID) {
                    dao.lookupNamesByNameSync(constraint.toString().uppercase())
                } else {
                    dao.lookupNamesInProfileByNameSync(profileId, constraint.toString().uppercase())
                }
            )
            results.values = matches
            results.count = matches.size

            return results
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            if (results.values == null) {
                notifyDataSetInvalidated()
            } else {
                setNotifyOnChange(false)
                clear()
                addAll(results.values as List<String>)
                notifyDataSetChanged()
            }
        }
    }
}
