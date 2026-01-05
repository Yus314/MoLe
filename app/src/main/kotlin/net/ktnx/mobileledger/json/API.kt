/*
 * Copyright © 2020, 2024 Damyan Ivanov.
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

package net.ktnx.mobileledger.json

import android.content.res.Resources
import android.util.SparseArray
import net.ktnx.mobileledger.R

enum class API(private val value: Int) {
    auto(0),
    html(-1),
    v1_14(-2),
    v1_15(-3),
    v1_19_1(-4),
    v1_23(-5),
    v1_32(-6),
    v1_40(-7),
    v1_50(-8);

    fun toInt(): Int = value

    val description: String
        get() = when (this) {
            auto -> "(automatic)"
            html -> "(HTML)"
            v1_14 -> "1.14"
            v1_15 -> "1.15"
            v1_19_1 -> "1.19.1"
            v1_23 -> "1.23"
            v1_32 -> "1.32"
            v1_40 -> "1.40"
            v1_50 -> "1.50"
        }

    fun getDescription(resources: Resources): String = when (this) {
        auto -> resources.getString(R.string.api_auto)
        html -> resources.getString(R.string.api_html)
        v1_14 -> resources.getString(R.string.api_1_14)
        v1_15 -> resources.getString(R.string.api_1_15)
        v1_19_1 -> resources.getString(R.string.api_1_19_1)
        v1_23 -> resources.getString(R.string.api_1_23)
        v1_32 -> resources.getString(R.string.api_1_32)
        v1_40 -> resources.getString(R.string.api_1_40)
        v1_50 -> resources.getString(R.string.api_1_50)
    }

    companion object {
        private val map = SparseArray<API>()

        @JvmField
        val allVersions = arrayOf(v1_50, v1_40, v1_32, v1_23, v1_19_1, v1_15, v1_14)

        init {
            for (item in entries) {
                map.put(item.value, item)
            }
        }

        @JvmStatic
        fun valueOf(i: Int): API = map.get(i, auto)
    }
}
