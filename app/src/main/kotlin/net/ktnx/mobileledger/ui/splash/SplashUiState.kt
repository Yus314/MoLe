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

package net.ktnx.mobileledger.ui.splash

/**
 * UI state for the splash screen
 */
data class SplashUiState(
    /** DB initialization completed flag */
    val isInitialized: Boolean = false,
    /** Minimum display time elapsed flag */
    val minDisplayTimeElapsed: Boolean = false
) {
    /** Ready to navigate to main screen */
    val canNavigate: Boolean get() = isInitialized && minDisplayTimeElapsed
}

/**
 * One-shot effects for splash screen
 */
sealed class SplashEffect {
    /** Navigate to main activity */
    data object NavigateToMain : SplashEffect()
}
