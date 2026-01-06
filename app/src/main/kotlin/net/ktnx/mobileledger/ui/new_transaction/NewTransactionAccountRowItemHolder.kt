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

package net.ktnx.mobileledger.ui.new_transaction

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.databinding.NewTransactionAccountRowBinding
import net.ktnx.mobileledger.db.AccountWithAmountsAutocompleteAdapter
import net.ktnx.mobileledger.model.Currency
import net.ktnx.mobileledger.model.Data
import net.ktnx.mobileledger.ui.CurrencySelectorFragment
import net.ktnx.mobileledger.ui.TextViewClearHelper
import net.ktnx.mobileledger.utils.DimensionUtils
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.Misc
import java.text.ParseException

internal class NewTransactionAccountRowItemHolder(
    private val b: NewTransactionAccountRowBinding,
    adapter: NewTransactionItemsAdapter
) : NewTransactionItemViewHolder(b.root) {

    private var ignoreFocusChanges = false
    private var inUpdate = false
    private var syncingData = false

    init {
        TextViewClearHelper().attachToTextView(b.comment)

        b.accountRowAccName.nextFocusForwardId = View.NO_ID
        b.accountRowAccAmounts.nextFocusForwardId = View.NO_ID // magic!

        b.accountCommentButton.setOnClickListener {
            b.comment.visibility = View.VISIBLE
            b.comment.requestFocus()
        }

        @SuppressLint("DefaultLocale")
        val focusMonitor = View.OnFocusChangeListener { v, hasFocus ->
            val id = v.id
            if (hasFocus) {
                val wasSyncing = syncingData
                syncingData = true
                try {
                    val pos = bindingAdapterPosition
                    when (id) {
                        R.id.account_row_acc_name -> adapter.noteFocusIsOnAccount(pos)
                        R.id.account_row_acc_amounts -> adapter.noteFocusIsOnAmount(pos)
                        R.id.comment -> adapter.noteFocusIsOnComment(pos)
                        else -> throw IllegalStateException("Where is the focus? $id")
                    }
                } finally {
                    syncingData = wasSyncing
                }
            } else { // lost focus
                if (id == R.id.account_row_acc_amounts) {
                    try {
                        var input = b.accountRowAccAmounts.text.toString()
                        input = input.replace(Data.getDecimalSeparator(), Data.decimalDot)
                        val newText = Data.formatNumber(input.toFloat())
                        if (newText != input) {
                            val wasSyncingData = syncingData
                            syncingData = true
                            try {
                                // there is a listener that will propagate the change to the model
                                b.accountRowAccAmounts.setText(newText)
                            } finally {
                                syncingData = wasSyncingData
                            }
                        }
                    } catch (ex: NumberFormatException) {
                        // ignored
                    }
                }
            }

            if (id == R.id.comment) {
                commentFocusChanged(b.comment, hasFocus)
            }
        }

        b.accountRowAccName.onFocusChangeListener = focusMonitor
        b.accountRowAccAmounts.onFocusChangeListener = focusMonitor
        b.comment.onFocusChangeListener = focusMonitor

        val activity = b.root.context as NewTransactionActivity

        mProfile?.let { profile ->
            b.accountRowAccName.setAdapter(
                AccountWithAmountsAutocompleteAdapter(
                    b.root.context,
                    profile
                )
            )
        }
        b.accountRowAccName.setOnItemClickListener { _, _, position, _ ->
            adapter.noteFocusIsOnAmount(position)
        }

        val tw = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                // debug("input", "text changed")
                if (inUpdate) return

                Logger.debug("textWatcher", "calling syncData()")
                if (syncData()) {
                    Logger.debug(
                        "textWatcher",
                        "syncData() returned, checking if transaction is submittable"
                    )
                    adapter.model.checkTransactionSubmittable(null)
                }
                Logger.debug("textWatcher", "done")
            }
        }

        val amountWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                checkAmountValid(s.toString())

                if (syncData()) {
                    adapter.model.checkTransactionSubmittable(null)
                }
            }
        }

        b.accountRowAccName.addTextChangedListener(tw)
        monitorComment(b.comment)
        b.accountRowAccAmounts.addTextChangedListener(amountWatcher)

        b.currencyButton.setOnClickListener {
            val cpf = CurrencySelectorFragment()
            cpf.showPositionAndPadding()
            cpf.setOnCurrencySelectedListener { c ->
                adapter.setItemCurrency(bindingAdapterPosition, c)
            }
            cpf.show(activity.supportFragmentManager, "currency-selector")
        }

        commentFocusChanged(b.comment, false)

        adapter.model.getFocusInfo().observe(activity) { focusInfo ->
            applyFocus(focusInfo)
        }

        Data.currencyGap.observe(activity) { hasGap ->
            updateCurrencyPositionAndPadding(Data.currencySymbolPosition.value, hasGap)
        }

        Data.currencySymbolPosition.observe(activity) { position ->
            updateCurrencyPositionAndPadding(position, Data.currencyGap.value)
        }

        adapter.model.getShowCurrency().observe(activity) { showCurrency ->
            if (showCurrency) {
                b.currency.visibility = View.VISIBLE
                b.currencyButton.visibility = View.VISIBLE
                setCurrencyString(mProfile?.getDefaultCommodityOrEmpty() ?: "")
            } else {
                b.currency.visibility = View.GONE
                b.currencyButton.visibility = View.GONE
                setCurrencyString(null)
            }
        }

        adapter.model.getShowComments().observe(activity) { show ->
            b.commentLayout.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    private fun applyFocus(focusInfo: NewTransactionModel.FocusInfo?) {
        if (ignoreFocusChanges) {
            Logger.debug("new-trans", "Ignoring focus change")
            return
        }
        ignoreFocusChanges = true
        try {
            if (focusInfo == null || focusInfo.element == null ||
                focusInfo.position != bindingAdapterPosition
            ) {
                return
            }

            val item = item ?: return

            val acc = item.toTransactionAccount()
            when (focusInfo.element) {
                FocusedElement.Amount -> b.accountRowAccAmounts.requestFocus()
                FocusedElement.Comment -> {
                    b.comment.visibility = View.VISIBLE
                    b.comment.requestFocus()
                }
                FocusedElement.Account -> {
                    val focused = b.accountRowAccName.requestFocus()
                    // b.accountRowAccName.dismissDropDown()
                    if (focused) {
                        Misc.showSoftKeyboard(b.root.context as NewTransactionActivity)
                    }
                }
                else -> { /* ignore other focus elements */ }
            }
        } finally {
            ignoreFocusChanges = false
        }
    }

    fun checkAmountValid(s: String) {
        // FIXME this needs to be done in the model only
        var valid = true
        try {
            if (s.isNotEmpty()) {
                s.replace(Data.getDecimalSeparator(), Data.decimalDot).toFloat()
            }
        } catch (ex: NumberFormatException) {
            try {
                Data.parseNumber(s)
            } catch (ex2: ParseException) {
                valid = false
            }
        }

        displayAmountValidity(valid)
    }

    private fun displayAmountValidity(valid: Boolean) {
        b.accountRowAccAmounts.setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (valid) 0 else R.drawable.ic_error_outline_black_24dp, 0, 0, 0
        )
        b.accountRowAccAmounts.minEms = if (valid) 4 else 5
    }

    private fun monitorComment(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                // debug("input", "text changed")
                if (inUpdate) return

                Logger.debug("textWatcher", "calling syncData()")
                syncData()
                Logger.debug(
                    "textWatcher",
                    "syncData() returned, checking if transaction is submittable"
                )
                styleComment(editText, s.toString())
                Logger.debug("textWatcher", "done")
            }
        })
    }

    private fun commentFocusChanged(textView: TextView, hasFocus: Boolean) {
        @ColorInt var textColor = b.dummyText.textColors.defaultColor
        if (hasFocus) {
            textView.setTypeface(null, Typeface.NORMAL)
            textView.setHint(R.string.transaction_account_comment_hint)
        } else {
            var alpha = (textColor shr 24) and 0xff
            alpha = 3 * alpha / 4
            textColor = (alpha shl 24) or (0x00ffffff and textColor)
            textView.setTypeface(null, Typeface.ITALIC)
            textView.hint = ""
            if (TextUtils.isEmpty(textView.text)) {
                textView.visibility = View.INVISIBLE
            }
        }
        textView.setTextColor(textColor)
    }

    private fun updateCurrencyPositionAndPadding(position: Currency.Position?, hasGap: Boolean?) {
        val amountLP = b.accountRowAccAmounts.layoutParams as ConstraintLayout.LayoutParams
        val currencyLP = b.currency.layoutParams as ConstraintLayout.LayoutParams

        if (position == Currency.Position.before) {
            currencyLP.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            currencyLP.endToEnd = ConstraintLayout.LayoutParams.UNSET

            amountLP.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            amountLP.endToStart = ConstraintLayout.LayoutParams.UNSET
            amountLP.startToStart = ConstraintLayout.LayoutParams.UNSET
            amountLP.startToEnd = b.currency.id

            b.currency.gravity = Gravity.END
        } else {
            currencyLP.startToStart = ConstraintLayout.LayoutParams.UNSET
            currencyLP.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID

            amountLP.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            amountLP.startToEnd = ConstraintLayout.LayoutParams.UNSET
            amountLP.endToEnd = ConstraintLayout.LayoutParams.UNSET
            amountLP.endToStart = b.currency.id

            b.currency.gravity = Gravity.START
        }

        amountLP.resolveLayoutDirection(b.accountRowAccAmounts.layoutDirection)
        currencyLP.resolveLayoutDirection(b.currency.layoutDirection)

        b.accountRowAccAmounts.layoutParams = amountLP
        b.currency.layoutParams = currencyLP

        // distance between the amount and the currency symbol
        val gapSize = DimensionUtils.sp2px(b.currency.context, 5f)

        if (position == Currency.Position.before) {
            b.currency.setPaddingRelative(0, 0, if (hasGap == true) gapSize else 0, 0)
        } else {
            b.currency.setPaddingRelative(if (hasGap == true) gapSize else 0, 0, 0, 0)
        }
    }

    private fun setCurrencyString(currency: String?) {
        @ColorInt val textColor = b.dummyText.textColors.defaultColor
        if (TextUtils.isEmpty(currency)) {
            b.currency.setText(R.string.currency_symbol)
            var alpha = (textColor shr 24) and 0xff
            alpha = alpha * 3 / 4
            b.currency.setTextColor((alpha shl 24) or (0x00ffffff and textColor))
        } else {
            b.currency.text = currency
            b.currency.setTextColor(textColor)
        }
    }

    private fun setCurrency(currency: Currency?) {
        setCurrencyString(currency?.name)
    }

    private fun setEditable(editable: Boolean?) {
        b.accountRowAccName.isEnabled = editable ?: false
        b.accountRowAccAmounts.isEnabled = editable ?: false
    }

    private fun beginUpdates() {
        if (inUpdate) throw RuntimeException("Already in update mode")
        inUpdate = true
    }

    private fun endUpdates() {
        if (!inUpdate) throw RuntimeException("Not in update mode")
        inUpdate = false
    }

    /**
     * syncData()
     *
     * Stores the data from the UI elements into the model item
     * Returns true if there were changes made that suggest transaction has to be
     * checked for being submittable
     */
    private fun syncData(): Boolean {
        if (syncingData) {
            Logger.debug("new-trans", "skipping syncData() loop")
            return false
        }

        if (bindingAdapterPosition == RecyclerView.NO_POSITION) {
            // probably the row was swiped out
            Logger.debug("new-trans", "Ignoring request to syncData(): adapter position negative")
            return false
        }

        val item = item ?: return false

        syncingData = true

        var significantChange = false

        try {
            val acc = item.toTransactionAccount()

            // having account name is important
            val incomingAccountName = b.accountRowAccName.text
            if (TextUtils.isEmpty(acc.accountName) != TextUtils.isEmpty(incomingAccountName)) {
                significantChange = true
            }

            acc.accountName = incomingAccountName.toString()
            val accNameSelEnd = b.accountRowAccName.selectionEnd
            val accNameSelStart = b.accountRowAccName.selectionStart
            acc.accountNameCursorPosition = accNameSelEnd

            acc.comment = b.comment.text.toString()

            val amount = b.accountRowAccAmounts.text.toString()

            if (acc.setAndCheckAmountText(Misc.nullIsEmpty(amount))) {
                significantChange = true
            }
            displayAmountValidity(!acc.isAmountSet || acc.isAmountValid)

            val curr = b.currency.text.toString()
            val currValue = if (curr == b.currency.context.resources.getString(R.string.currency_symbol) ||
                curr.isEmpty()
            ) {
                null
            } else {
                curr
            }

            if (!significantChange && !Misc.equalStrings(acc.currency, currValue)) {
                significantChange = true
            }
            acc.currency = currValue ?: ""

            return significantChange
        } finally {
            syncingData = false
        }
    }

    /**
     * bind
     *
     * @param item updates the UI elements with the data from the model item
     */
    @SuppressLint("DefaultLocale")
    override fun bind(item: NewTransactionModel.Item) {
        beginUpdates()
        try {
            syncingData = true
            try {
                val acc = item.toTransactionAccount()

                val incomingAccountName = acc.accountName
                val presentAccountName = b.accountRowAccName.text.toString()
                if (!Misc.equalStrings(incomingAccountName, presentAccountName)) {
                    Logger.debug(
                        "bind",
                        String.format(
                            "Setting account name from '%s' to '%s' (| @ %d)",
                            presentAccountName, incomingAccountName,
                            acc.accountNameCursorPosition
                        )
                    )
                    // avoid triggering completion pop-up
                    val a = b.accountRowAccName.adapter as? AccountWithAmountsAutocompleteAdapter
                    try {
                        b.accountRowAccName.setAdapter(null)
                        b.accountRowAccName.setText(incomingAccountName)
                        b.accountRowAccName.setSelection(acc.accountNameCursorPosition)
                    } finally {
                        b.accountRowAccName.setAdapter(a)
                    }
                }

                val amountHint = acc.amountHint
                if (amountHint == null) {
                    b.accountRowAccAmounts.setHint(R.string.zero_amount)
                } else {
                    b.accountRowAccAmounts.hint = amountHint
                }

                b.accountRowAccAmounts.imeOptions =
                    if (acc.isLast) EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_NEXT

                setCurrencyString(acc.currency)
                b.accountRowAccAmounts.setText(acc.amountText)
                displayAmountValidity(!acc.isAmountSet || acc.isAmountValid)

                val comment = acc.comment
                b.comment.setText(comment)
                styleComment(b.comment, comment)

                setEditable(true)

                val adapter = bindingAdapter as? NewTransactionItemsAdapter
                if (adapter != null) {
                    applyFocus(adapter.model.getFocusInfo().value)
                }
            } finally {
                syncingData = false
            }
        } finally {
            endUpdates()
        }
    }

    private fun styleComment(editText: EditText, comment: String?) {
        val focusedView = editText.findFocus()
        editText.setTypeface(null, if (focusedView == editText) Typeface.NORMAL else Typeface.ITALIC)
        editText.visibility =
            if (focusedView != editText && TextUtils.isEmpty(comment)) View.INVISIBLE else View.VISIBLE
    }
}
