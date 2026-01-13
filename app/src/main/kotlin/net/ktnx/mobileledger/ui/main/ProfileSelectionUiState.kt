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

package net.ktnx.mobileledger.ui.main

/**
 * UI state for profile selection in the navigation drawer.
 *
 * Contains the current profile information and the list of all available profiles.
 * This state is managed by [ProfileSelectionViewModel] and consumed by the
 * navigation drawer Composable.
 */
data class ProfileSelectionUiState(
    val currentProfileId: Long? = null,
    val currentProfileName: String = "",
    val currentProfileTheme: Int = -1,
    val currentProfileCanPost: Boolean = false,
    val profiles: List<ProfileListItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
