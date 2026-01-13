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
 * Events for profile selection.
 *
 * These events are dispatched from the navigation drawer UI and handled by
 * [ProfileSelectionViewModel] to update profile state.
 */
sealed class ProfileSelectionEvent {
    /**
     * Select a profile by its ID.
     */
    data class SelectProfile(val profileId: Long) : ProfileSelectionEvent()

    /**
     * Reorder the profiles list.
     * The list should contain all profiles in the new desired order.
     */
    data class ReorderProfiles(val orderedProfiles: List<ProfileListItem>) : ProfileSelectionEvent()
}
