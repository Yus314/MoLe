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

import android.content.res.Resources
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.core.domain.model.FutureDates

/**
 * Extension function to get display text for FutureDates.
 * This keeps the core domain model free of Android dependencies.
 */
fun FutureDates.getText(resources: Resources): String = when (this) {
    FutureDates.OneWeek -> resources.getString(R.string.future_dates_7)
    FutureDates.TwoWeeks -> resources.getString(R.string.future_dates_14)
    FutureDates.OneMonth -> resources.getString(R.string.future_dates_30)
    FutureDates.TwoMonths -> resources.getString(R.string.future_dates_60)
    FutureDates.ThreeMonths -> resources.getString(R.string.future_dates_90)
    FutureDates.SixMonths -> resources.getString(R.string.future_dates_180)
    FutureDates.OneYear -> resources.getString(R.string.future_dates_365)
    FutureDates.All -> resources.getString(R.string.future_dates_all)
    FutureDates.None -> resources.getString(R.string.future_dates_none)
}
