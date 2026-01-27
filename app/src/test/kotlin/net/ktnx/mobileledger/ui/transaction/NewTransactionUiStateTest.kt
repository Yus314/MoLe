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

package net.ktnx.mobileledger.ui.transaction

import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.domain.model.CurrencyPosition
import net.ktnx.mobileledger.core.domain.model.FutureDates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for NewTransactionUiState and related data classes.
 */
class NewTransactionUiStateTest {

    // ========================================
    // NewTransactionUiState.isSubmittable tests
    // ========================================

    @Test
    fun `isSubmittable returns false when description is blank`() {
        val state = NewTransactionUiState(
            description = "",
            accounts = listOf(
                createAccountRow(1, "Assets:Bank", "100"),
                createAccountRow(2, "Expenses:Food", "-100")
            )
        )
        assertFalse(state.isSubmittable)
    }

    @Test
    fun `isSubmittable returns false when description is whitespace only`() {
        val state = NewTransactionUiState(
            description = "   ",
            accounts = listOf(
                createAccountRow(1, "Assets:Bank", "100"),
                createAccountRow(2, "Expenses:Food", "-100")
            )
        )
        assertFalse(state.isSubmittable)
    }

    @Test
    fun `isSubmittable returns false when less than two accounts have names`() {
        val state = NewTransactionUiState(
            description = "Test",
            accounts = listOf(
                createAccountRow(1, "Assets:Bank", "100"),
                createAccountRow(2, "", "")
            )
        )
        assertFalse(state.isSubmittable)
    }

    @Test
    fun `isSubmittable returns false when account with amount has no name`() {
        val state = NewTransactionUiState(
            description = "Test",
            accounts = listOf(
                createAccountRow(1, "Assets:Bank", "100"),
                createAccountRow(2, "", "50"), // Has amount but no name
                createAccountRow(3, "Expenses:Food", "-150")
            )
        )
        assertFalse(state.isSubmittable)
    }

    @Test
    fun `isSubmittable returns true when amounts balance to zero`() {
        val state = NewTransactionUiState(
            description = "Test Transaction",
            accounts = listOf(
                createAccountRow(1, "Assets:Bank", "-100"),
                createAccountRow(2, "Expenses:Food", "100")
            )
        )
        assertTrue(state.isSubmittable)
    }

    @Test
    fun `isSubmittable returns true with one balance receiver`() {
        val state = NewTransactionUiState(
            description = "Test Transaction",
            accounts = listOf(
                createAccountRow(1, "Assets:Bank", "-100"),
                createAccountRow(2, "Expenses:Food", "") // balance receiver
            )
        )
        assertTrue(state.isSubmittable)
    }

    @Test
    fun `isSubmittable returns false when unbalanced and no balance receiver`() {
        val state = NewTransactionUiState(
            description = "Test Transaction",
            accounts = listOf(
                createAccountRow(1, "Assets:Bank", "-100"),
                createAccountRow(2, "Expenses:Food", "50")
            )
        )
        assertFalse(state.isSubmittable)
    }

    @Test
    fun `isSubmittable returns false when unbalanced with multiple balance receivers`() {
        val state = NewTransactionUiState(
            description = "Test Transaction",
            accounts = listOf(
                createAccountRow(1, "Assets:Bank", "-100"),
                createAccountRow(2, "Expenses:Food", ""), // balance receiver
                createAccountRow(3, "Expenses:Other", "") // another balance receiver
            )
        )
        assertFalse(state.isSubmittable)
    }

    @Test
    fun `isSubmittable handles multiple currencies correctly`() {
        val state = NewTransactionUiState(
            description = "Test Transaction",
            accounts = listOf(
                createAccountRow(1, "Assets:Bank", "-100", "USD"),
                createAccountRow(2, "Expenses:Food", "100", "USD"),
                createAccountRow(3, "Assets:Bank:EUR", "-50", "EUR"),
                createAccountRow(4, "Expenses:Travel", "50", "EUR")
            )
        )
        assertTrue(state.isSubmittable)
    }

    @Test
    fun `isSubmittable returns false when any currency is unbalanced`() {
        val state = NewTransactionUiState(
            description = "Test Transaction",
            accounts = listOf(
                createAccountRow(1, "Assets:Bank", "-100", "USD"),
                createAccountRow(2, "Expenses:Food", "100", "USD"),
                createAccountRow(3, "Assets:Bank:EUR", "-50", "EUR"),
                createAccountRow(4, "Expenses:Travel", "30", "EUR") // EUR not balanced
            )
        )
        assertFalse(state.isSubmittable)
    }

    @Test
    fun `isSubmittable returns false when account has invalid amount`() {
        val state = NewTransactionUiState(
            description = "Test Transaction",
            accounts = listOf(
                createAccountRow(1, "Assets:Bank", "-100", isAmountValid = true),
                createAccountRow(2, "Expenses:Food", "abc", isAmountValid = false)
            )
        )
        assertFalse(state.isSubmittable)
    }

    @Test
    fun `isSubmittable handles accounts with no amounts in currency group`() {
        val state = NewTransactionUiState(
            description = "Test Transaction",
            accounts = listOf(
                createAccountRow(1, "Assets:Bank", ""),
                createAccountRow(2, "Expenses:Food", "")
            )
        )
        assertTrue(state.isSubmittable)
    }

    @Test
    fun `isSubmittable accepts small balance discrepancy within epsilon`() {
        val state = NewTransactionUiState(
            description = "Test Transaction",
            accounts = listOf(
                createAccountRow(1, "Assets:Bank", "-100.001"),
                createAccountRow(2, "Expenses:Food", "100.00")
            )
        )
        assertTrue(state.isSubmittable)
    }

    // ========================================
    // NewTransactionUiState.hasUnsavedChanges tests
    // ========================================

    @Test
    fun `hasUnsavedChanges returns false for default state`() {
        val state = NewTransactionUiState()
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun `hasUnsavedChanges returns true when description is not blank`() {
        val state = NewTransactionUiState(description = "Test")
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun `hasUnsavedChanges returns true when transaction comment is not blank`() {
        val state = NewTransactionUiState(transactionComment = "Some comment")
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun `hasUnsavedChanges returns true when any account is not empty`() {
        val state = NewTransactionUiState(
            accounts = listOf(
                createAccountRow(1, "Assets:Bank", "")
            )
        )
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun `hasUnsavedChanges returns false when accounts are all empty`() {
        val state = NewTransactionUiState(
            accounts = listOf(
                createAccountRow(1, "", ""),
                createAccountRow(2, "", "")
            )
        )
        assertFalse(state.hasUnsavedChanges)
    }

    // ========================================
    // TransactionAccountRow tests
    // ========================================

    @Test
    fun `isAmountSet returns false when amountText is blank`() {
        val row = TransactionAccountRow(id = 1, amountText = "")
        assertFalse(row.isAmountSet)
    }

    @Test
    fun `isAmountSet returns false when amountText is whitespace`() {
        val row = TransactionAccountRow(id = 1, amountText = "   ")
        assertFalse(row.isAmountSet)
    }

    @Test
    fun `isAmountSet returns true when amountText has value`() {
        val row = TransactionAccountRow(id = 1, amountText = "100")
        assertTrue(row.isAmountSet)
    }

    @Test
    fun `amount returns null when amountText is blank`() {
        val row = TransactionAccountRow(id = 1, amountText = "")
        assertNull(row.amount)
    }

    @Test
    fun `amount returns null when isAmountValid is false`() {
        val row = TransactionAccountRow(id = 1, amountText = "100", isAmountValid = false)
        assertNull(row.amount)
    }

    @Test
    fun `amount returns parsed float when valid`() {
        val row = TransactionAccountRow(id = 1, amountText = "100.50", isAmountValid = true)
        assertEquals(100.50f, row.amount!!, 0.001f)
    }

    @Test
    fun `amount handles comma as decimal separator`() {
        val row = TransactionAccountRow(id = 1, amountText = "100,50", isAmountValid = true)
        assertEquals(100.50f, row.amount!!, 0.001f)
    }

    @Test
    fun `amount handles negative values`() {
        val row = TransactionAccountRow(id = 1, amountText = "-50.25", isAmountValid = true)
        assertEquals(-50.25f, row.amount!!, 0.001f)
    }

    @Test
    fun `amount returns null for unparseable value`() {
        val row = TransactionAccountRow(id = 1, amountText = "abc", isAmountValid = true)
        assertNull(row.amount)
    }

    @Test
    fun `isEmpty returns true when all fields are blank`() {
        val row = TransactionAccountRow(id = 1, accountName = "", amountText = "", comment = "")
        assertTrue(row.isEmpty)
    }

    @Test
    fun `isEmpty returns false when accountName is set`() {
        val row = TransactionAccountRow(id = 1, accountName = "Assets:Bank", amountText = "", comment = "")
        assertFalse(row.isEmpty)
    }

    @Test
    fun `isEmpty returns false when amountText is set`() {
        val row = TransactionAccountRow(id = 1, accountName = "", amountText = "100", comment = "")
        assertFalse(row.isEmpty)
    }

    @Test
    fun `isEmpty returns false when comment is set`() {
        val row = TransactionAccountRow(id = 1, accountName = "", amountText = "", comment = "Some note")
        assertFalse(row.isEmpty)
    }

    @Test
    fun `TransactionAccountRow default values are correct`() {
        val row = TransactionAccountRow(id = 1)
        assertEquals("", row.accountName)
        assertEquals("", row.amountText)
        assertNull(row.amountHint)
        assertEquals("", row.currency)
        assertEquals("", row.comment)
        assertFalse(row.isCommentExpanded)
        assertTrue(row.isAmountValid)
        assertFalse(row.isLast)
    }

    // ========================================
    // TemplateItem tests
    // ========================================

    @Test
    fun `TemplateItem constructor sets all fields`() {
        val item = TemplateItem(id = 1, name = "Test Template", description = "A test", regex = ".*")
        assertEquals(1L, item.id)
        assertEquals("Test Template", item.name)
        assertEquals("A test", item.description)
        assertEquals(".*", item.regex)
    }

    @Test
    fun `TemplateItem description and regex are optional`() {
        val item = TemplateItem(id = 1, name = "Simple")
        assertEquals(1L, item.id)
        assertEquals("Simple", item.name)
        assertNull(item.description)
        assertNull(item.regex)
    }

    // ========================================
    // FocusedElement enum tests
    // ========================================

    @Test
    fun `FocusedElement has expected values`() {
        val values = FocusedElement.values()
        assertEquals(5, values.size)
        assertTrue(values.contains(FocusedElement.Description))
        assertTrue(values.contains(FocusedElement.TransactionComment))
        assertTrue(values.contains(FocusedElement.Account))
        assertTrue(values.contains(FocusedElement.Amount))
        assertTrue(values.contains(FocusedElement.AccountComment))
    }

    // ========================================
    // ValidationField enum tests
    // ========================================

    @Test
    fun `ValidationField has expected values`() {
        val values = ValidationField.values()
        assertEquals(4, values.size)
        assertTrue(values.contains(ValidationField.DESCRIPTION))
        assertTrue(values.contains(ValidationField.DATE))
        assertTrue(values.contains(ValidationField.ACCOUNT))
        assertTrue(values.contains(ValidationField.AMOUNT))
    }

    // ========================================
    // NewTransactionUiState default values tests
    // ========================================

    @Test
    fun `NewTransactionUiState default values are correct`() {
        val state = NewTransactionUiState()
        assertNull(state.profileId)
        assertEquals("", state.description)
        assertEquals("", state.transactionComment)
        assertTrue(state.accounts.isEmpty())
        assertFalse(state.showCurrency)
        assertFalse(state.isTransactionCommentExpanded)
        assertNull(state.focusedRowId)
        assertNull(state.focusedElement)
        assertFalse(state.isSubmitting)
        assertNull(state.submitError)
        assertFalse(state.showDatePicker)
        assertFalse(state.showTemplateSelector)
        assertFalse(state.showCurrencySelector)
        assertNull(state.currencySelectorRowId)
        assertTrue(state.availableCurrencies.isEmpty())
        assertEquals(FutureDates.All, state.futureDates)
        assertTrue(state.availableTemplates.isEmpty())
        assertTrue(state.accountSuggestions.isEmpty())
        assertEquals(0, state.accountSuggestionsVersion)
        assertNull(state.accountSuggestionsForRowId)
        assertTrue(state.descriptionSuggestions.isEmpty())
        assertFalse(state.isSimulateSave)
        assertFalse(state.isBusy)
        assertTrue(state.validationErrors.isEmpty())
    }

    // ========================================
    // NewTransactionEvent tests
    // ========================================

    @Test
    fun `NewTransactionEvent UpdateDate contains date`() {
        val date = SimpleDate(2026, 1, 21)
        val event = NewTransactionEvent.UpdateDate(date)
        assertEquals(date, event.date)
    }

    @Test
    fun `NewTransactionEvent UpdateDescription contains description`() {
        val event = NewTransactionEvent.UpdateDescription("Test")
        assertEquals("Test", event.description)
    }

    @Test
    fun `NewTransactionEvent UpdateAccountName contains rowId and name`() {
        val event = NewTransactionEvent.UpdateAccountName(1, "Assets:Bank")
        assertEquals(1, event.rowId)
        assertEquals("Assets:Bank", event.name)
    }

    @Test
    fun `NewTransactionEvent UpdateAmount contains rowId and amount`() {
        val event = NewTransactionEvent.UpdateAmount(1, "100.50")
        assertEquals(1, event.rowId)
        assertEquals("100.50", event.amount)
    }

    @Test
    fun `NewTransactionEvent AddCurrency contains all fields`() {
        val event = NewTransactionEvent.AddCurrency("USD", CurrencyPosition.BEFORE, true)
        assertEquals("USD", event.name)
        assertEquals(CurrencyPosition.BEFORE, event.position)
        assertTrue(event.gap)
    }

    @Test
    fun `NewTransactionEvent ShowCurrencySelector contains rowId`() {
        val event = NewTransactionEvent.ShowCurrencySelector(2)
        assertEquals(2, event.rowId)
    }

    @Test
    fun `NewTransactionEvent MoveAccountRow contains indices`() {
        val event = NewTransactionEvent.MoveAccountRow(0, 2)
        assertEquals(0, event.fromIndex)
        assertEquals(2, event.toIndex)
    }

    // ========================================
    // NewTransactionEffect tests
    // ========================================

    @Test
    fun `NewTransactionEffect ShowError contains message`() {
        val effect = NewTransactionEffect.ShowError("Error occurred")
        assertEquals("Error occurred", effect.message)
    }

    @Test
    fun `NewTransactionEffect RequestFocus contains rowId and element`() {
        val effect = NewTransactionEffect.RequestFocus(1, FocusedElement.Amount)
        assertEquals(1, effect.rowId)
        assertEquals(FocusedElement.Amount, effect.element)
    }

    @Test
    fun `NewTransactionEffect RequestFocus rowId can be null`() {
        val effect = NewTransactionEffect.RequestFocus(null, FocusedElement.Description)
        assertNull(effect.rowId)
        assertEquals(FocusedElement.Description, effect.element)
    }

    // ========================================
    // Helper functions
    // ========================================

    private fun createAccountRow(
        id: Int,
        accountName: String,
        amountText: String,
        currency: String = "",
        isAmountValid: Boolean = true
    ): TransactionAccountRow = TransactionAccountRow(
        id = id,
        accountName = accountName,
        amountText = amountText,
        currency = currency,
        isAmountValid = isAmountValid
    )
}
