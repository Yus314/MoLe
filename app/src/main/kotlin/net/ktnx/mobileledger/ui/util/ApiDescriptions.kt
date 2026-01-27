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

package net.ktnx.mobileledger.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.core.domain.model.API

/**
 * Composable extension to get localized description of API version.
 *
 * This separates Android Resource access from the API enum,
 * keeping the enum portable to core modules.
 */
@Composable
fun API.localizedDescription(): String = when (this) {
    API.auto -> stringResource(R.string.api_auto)
    API.v1_32 -> stringResource(R.string.api_1_32)
    API.v1_40 -> stringResource(R.string.api_1_40)
    API.v1_50 -> stringResource(R.string.api_1_50)
}
