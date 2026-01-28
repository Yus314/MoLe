/*
 * Copyright © 2024 Damyan Ivanov.
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

@file:Suppress("UNUSED", "MatchingDeclarationName")

package net.ktnx.mobileledger.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.domain.model.FutureDates

// Re-export from core:ui for backward compatibility
// New code should import from net.ktnx.mobileledger.core.ui.components directly

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoleDatePickerDialog(
    initialDate: SimpleDate,
    minDate: SimpleDate? = null,
    maxDate: SimpleDate? = null,
    futureDates: FutureDates = FutureDates.All,
    onDateSelected: (SimpleDate) -> Unit,
    onDismiss: () -> Unit
) = net.ktnx.mobileledger.core.ui.components.MoleDatePickerDialog(
    initialDate = initialDate,
    minDate = minDate,
    maxDate = maxDate,
    futureDates = futureDates,
    onDateSelected = onDateSelected,
    onDismiss = onDismiss,
    confirmText = "OK",
    dismissText = "Cancel"
)
