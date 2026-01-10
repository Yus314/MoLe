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

package net.ktnx.mobileledger.ui.components

/**
 * UI state for the currency picker dialog.
 */
data class CurrencyPickerUiState(
    /** Available currencies to display */
    val currencies: List<String> = emptyList(),
    /** Whether the add new currency mode is active */
    val isAddingNew: Boolean = false,
    /** Current input for new currency name */
    val newCurrencyName: String = "",
    /** Currency position (before or after amount) */
    val currencyPosition: CurrencyPosition = CurrencyPosition.BEFORE,
    /** Whether to add gap between currency and amount */
    val currencyGap: Boolean = true,
    /** Whether to show position and gap settings */
    val showPositionSettings: Boolean = true
)

/**
 * Currency position relative to amount.
 */
enum class CurrencyPosition {
    /** Currency symbol before amount (e.g., $100) */
    BEFORE,

    /** Currency symbol after amount (e.g., 100 EUR) */
    AFTER
}

/**
 * Events from UI to ViewModel for currency picker.
 */
sealed class CurrencyPickerEvent {
    /** A currency was selected from the list */
    data class CurrencySelected(val currency: String) : CurrencyPickerEvent()

    /** A currency was deleted (long press) */
    data class CurrencyDeleted(val currency: String) : CurrencyPickerEvent()

    /** User clicked add new currency button */
    data object AddNewClicked : CurrencyPickerEvent()

    /** New currency name input changed */
    data class NewCurrencyNameChanged(val name: String) : CurrencyPickerEvent()

    /** User confirmed adding new currency */
    data object AddCurrencyConfirmed : CurrencyPickerEvent()

    /** User cancelled adding new currency */
    data object AddCurrencyCancelled : CurrencyPickerEvent()

    /** Currency position changed */
    data class PositionChanged(val position: CurrencyPosition) : CurrencyPickerEvent()

    /** Gap setting changed */
    data class GapChanged(val gap: Boolean) : CurrencyPickerEvent()

    /** User selected "no currency" option */
    data object NoCurrencySelected : CurrencyPickerEvent()

    /** Dialog dismissed */
    data object Dismissed : CurrencyPickerEvent()
}
