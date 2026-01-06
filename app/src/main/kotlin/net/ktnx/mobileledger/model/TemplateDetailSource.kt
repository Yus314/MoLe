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

package net.ktnx.mobileledger.model

import androidx.recyclerview.widget.DiffUtil
import java.io.Serializable

data class TemplateDetailSource(
    var groupNumber: Short = 0,
    var matchedText: String = ""
) : Serializable {

    companion object {
        @JvmField
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TemplateDetailSource>() {
            override fun areItemsTheSame(
                oldItem: TemplateDetailSource,
                newItem: TemplateDetailSource
            ): Boolean = oldItem.groupNumber == newItem.groupNumber

            override fun areContentsTheSame(
                oldItem: TemplateDetailSource,
                newItem: TemplateDetailSource
            ): Boolean = oldItem.matchedText == newItem.matchedText
        }
    }
}
