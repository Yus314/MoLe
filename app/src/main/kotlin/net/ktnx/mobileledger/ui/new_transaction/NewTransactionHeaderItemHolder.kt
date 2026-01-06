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
import android.view.View
import android.widget.EditText
import android.widget.ListAdapter
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormatSymbols
import java.text.ParseException
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.databinding.NewTransactionHeaderRowBinding
import net.ktnx.mobileledger.db.TransactionDescriptionAutocompleteAdapter
import net.ktnx.mobileledger.model.Data
import net.ktnx.mobileledger.model.FutureDates
import net.ktnx.mobileledger.ui.DatePickerFragment
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.Misc
import net.ktnx.mobileledger.utils.SimpleDate

internal class NewTransactionHeaderItemHolder(
    private val b: NewTransactionHeaderRowBinding,
    adapter: NewTransactionItemsAdapter
) : NewTransactionItemViewHolder(b.root), DatePickerFragment.DatePickedListener {

    private var ignoreFocusChanges = false
    private var decimalSeparator: String = ""
    private var inUpdate = false
    private var syncingData = false

    init {
        b.newTransactionDescription.nextFocusForwardId = View.NO_ID

        b.newTransactionDate.setOnClickListener { pickTransactionDate() }

        b.transactionCommentButton.setOnClickListener {
            b.transactionComment.visibility = View.VISIBLE
            b.transactionComment.requestFocus()
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
                        R.id.transaction_comment -> adapter.noteFocusIsOnTransactionComment(pos)
                        R.id.new_transaction_description -> adapter.noteFocusIsOnDescription(pos)
                        else -> throw IllegalStateException("Where is the focus? $id")
                    }
                } finally {
                    syncingData = wasSyncing
                }
            }

            if (id == R.id.transaction_comment) {
                commentFocusChanged(b.transactionComment, hasFocus)
            }
        }

        b.newTransactionDescription.onFocusChangeListener = focusMonitor
        b.transactionComment.onFocusChangeListener = focusMonitor

        val activity = b.root.context as NewTransactionActivity

        b.newTransactionDescription.setAdapter(
            TransactionDescriptionAutocompleteAdapter(activity)
        )
        b.newTransactionDescription.setOnItemClickListener { parent, _, position, _ ->
            activity.onDescriptionSelected(
                parent.getItemAtPosition(position).toString()
            )
        }

        Data.locale.observe(activity) { locale ->
            decimalSeparator = DecimalFormatSymbols.getInstance(locale)
                .monetaryDecimalSeparator.toString()
        }

        val tw = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
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
        b.newTransactionDescription.addTextChangedListener(tw)
        monitorComment(b.transactionComment)

        commentFocusChanged(b.transactionComment, false)

        adapter.model.getFocusInfo().observe(activity) { focusInfo ->
            applyFocus(focusInfo)
        }

        adapter.model.getShowComments().observe(activity) { show ->
            b.transactionCommentLayout.visibility = if (show) View.VISIBLE else View.GONE
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

            val currentItem = item ?: return

            val head = currentItem.toTransactionHead()
            // bad idea - double pop-up, and not really necessary.
            // the user can tap the input to get the calendar
            // if (!tvDate.hasFocus()) tvDate.requestFocus();
            when (focusInfo.element) {
                FocusedElement.TransactionComment -> {
                    b.transactionComment.visibility = View.VISIBLE
                    b.transactionComment.requestFocus()
                }
                FocusedElement.Description -> {
                    val focused = b.newTransactionDescription.requestFocus()
                    // tvDescription.dismissDropDown();
                    if (focused) {
                        Misc.showSoftKeyboard(b.root.context as NewTransactionActivity)
                    }
                }
                else -> { /* no-op */ }
            }
        } finally {
            ignoreFocusChanges = false
        }
    }

    private fun monitorComment(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
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
        @ColorInt
        var textColor = b.dummyText.textColors.defaultColor
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

    private fun setEditable(editable: Boolean) {
        b.newTransactionDate.isEnabled = editable
        b.newTransactionDescription.isEnabled = editable
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

        var significantChange = false

        syncingData = true
        try {
            val currentItem = item ?: return false

            val head = currentItem.toTransactionHead()

            head.setDate(b.newTransactionDate.text.toString())

            // transaction description is required
            if (TextUtils.isEmpty(head.description) !=
                TextUtils.isEmpty(b.newTransactionDescription.text)
            ) {
                significantChange = true
            }

            head.description = b.newTransactionDescription.text.toString()
            head.comment = b.transactionComment.text.toString()

            return significantChange
        } catch (e: ParseException) {
            throw RuntimeException("Should not happen", e)
        } finally {
            syncingData = false
        }
    }

    private fun pickTransactionDate() {
        val picker = DatePickerFragment()
        picker.setFutureDates(FutureDates.valueOf(mProfile?.futureDates ?: 0))
        picker.setOnDatePickedListener(this)
        picker.setCurrentDateFromText(b.newTransactionDate.text)
        picker.show(
            (b.root.context as NewTransactionActivity).supportFragmentManager,
            null
        )
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
                val head = item.toTransactionHead()
                b.newTransactionDate.setText(head.getFormattedDate())

                // avoid triggering completion pop-up
                val a: ListAdapter? = b.newTransactionDescription.adapter
                try {
                    b.newTransactionDescription.setAdapter(null)
                    b.newTransactionDescription.setText(head.description)
                } finally {
                    b.newTransactionDescription.setAdapter(
                        a as? TransactionDescriptionAutocompleteAdapter
                    )
                }

                val comment = head.comment
                b.transactionComment.setText(comment)
                styleComment(b.transactionComment, comment) // would hide or make it visible

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
        editText.setTypeface(
            null,
            if (focusedView === editText) Typeface.NORMAL else Typeface.ITALIC
        )
        editText.visibility =
            if (focusedView !== editText && TextUtils.isEmpty(comment)) View.INVISIBLE
            else View.VISIBLE
    }

    override fun onDatePicked(year: Int, month: Int, day: Int) {
        val currentItem = item ?: return

        val head = currentItem.toTransactionHead()
        head.date = SimpleDate(year, month + 1, day)
        b.newTransactionDate.setText(head.getFormattedDate())

        val focused = b.newTransactionDescription.requestFocus()
        if (focused) {
            Misc.showSoftKeyboard(b.root.context as NewTransactionActivity)
        }
    }
}
