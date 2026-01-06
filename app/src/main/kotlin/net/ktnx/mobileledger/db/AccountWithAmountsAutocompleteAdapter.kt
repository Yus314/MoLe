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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.dao.AccountDAO
import net.ktnx.mobileledger.model.Data
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.Misc

class AccountWithAmountsAutocompleteAdapter(
    context: Context,
    profile: Profile
) : ArrayAdapter<AccountWithAmounts>(context, R.layout.account_autocomplete_row) {

    private val filter = AccountFilter()
    private val profileId: Long = profile.id

    override fun getFilter(): Filter = filter

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context)
            .inflate(R.layout.account_autocomplete_row, parent, false)

        val item = getItem(position)
        view.findViewById<TextView>(R.id.account_name).text = item?.account?.name ?: ""

        val amountsText = StringBuilder()
        item?.amounts?.forEach { amt ->
            if (amountsText.isNotEmpty()) {
                amountsText.append('\n')
            }
            val currency = amt.currency
            if (Misc.emptyIsNull(currency) != null) {
                amountsText.append(currency).append(' ')
            }
            amountsText.append(Data.formatNumber(amt.value))
        }
        view.findViewById<TextView>(R.id.amounts).text = amountsText.toString()

        return view
    }

    inner class AccountFilter : Filter() {
        private val dao: AccountDAO = DB.get().getAccountDAO()

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = FilterResults()
            if (constraint == null) {
                results.count = 0
                return results
            }

            Logger.debug("acc", String.format("Looking for account '%s'", constraint))
            val matches = dao.lookupWithAmountsInProfileByNameSync(
                profileId,
                constraint.toString().uppercase()
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
                addAll(results.values as List<AccountWithAmounts>)
                notifyDataSetChanged()
            }
        }
    }
}
