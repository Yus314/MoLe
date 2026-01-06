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

package net.ktnx.mobileledger.ui.templates

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import net.ktnx.mobileledger.BuildConfig
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.databinding.TemplateDetailsAccountBinding
import net.ktnx.mobileledger.databinding.TemplateDetailsHeaderBinding
import net.ktnx.mobileledger.db.AccountAutocompleteAdapter
import net.ktnx.mobileledger.db.DB
import net.ktnx.mobileledger.model.Data
import net.ktnx.mobileledger.model.TemplateDetailsItem
import net.ktnx.mobileledger.ui.CurrencySelectorFragment
import net.ktnx.mobileledger.ui.HelpDialog
import net.ktnx.mobileledger.ui.QR
import net.ktnx.mobileledger.ui.TemplateDetailSourceSelectorFragment
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.Misc
import java.text.ParseException
import java.util.Locale

internal class TemplateDetailsAdapter(
    private val mModel: TemplateDetailsViewModel
) : RecyclerView.Adapter<TemplateDetailsAdapter.ViewHolder>() {

    private val differ: AsyncListDiffer<TemplateDetailsItem>
    private val itemTouchHelper: ItemTouchHelper

    init {
        setHasStableIds(true)

        differ = AsyncListDiffer(this, object : DiffUtil.ItemCallback<TemplateDetailsItem>() {
            override fun areItemsTheSame(
                oldItem: TemplateDetailsItem,
                newItem: TemplateDetailsItem
            ): Boolean {
                if (oldItem.type != newItem.type) return false
                if (oldItem.type == TemplateDetailsItem.Type.HEADER) return true
                // the rest is comparing two account row items
                return oldItem.asAccountRowItem().id == newItem.asAccountRowItem().id
            }

            override fun areContentsTheSame(
                oldItem: TemplateDetailsItem,
                newItem: TemplateDetailsItem
            ): Boolean {
                return if (oldItem.type == TemplateDetailsItem.Type.HEADER) {
                    val oldHeader = oldItem.asHeaderItem()
                    val newHeader = newItem.asHeaderItem()
                    oldHeader.equalContents(newHeader)
                } else {
                    val oldAcc = oldItem.asAccountRowItem()
                    val newAcc = newItem.asAccountRowItem()
                    oldAcc.equalContents(newAcc)
                }
            }
        })

        itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMoveThreshold(viewHolder: RecyclerView.ViewHolder): Float = 0.5f

            override fun isLongPressDragEnabled(): Boolean = false

            override fun chooseDropTarget(
                selected: RecyclerView.ViewHolder,
                dropTargets: List<RecyclerView.ViewHolder>,
                curX: Int,
                curY: Int
            ): RecyclerView.ViewHolder? {
                var best: RecyclerView.ViewHolder? = null
                var bestDistance = 0

                for (v in dropTargets) {
                    if (v === selected) continue

                    val viewTop = v.itemView.top
                    val distance = kotlin.math.abs(viewTop - curY)
                    if (best == null) {
                        best = v
                        bestDistance = distance
                    } else {
                        if (distance < bestDistance) {
                            bestDistance = distance
                            best = v
                        }
                    }
                }

                Logger.debug("dnd", "Best target is $best")
                return best
            }

            override fun canDropOver(
                recyclerView: RecyclerView,
                current: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val adapterPosition = target.bindingAdapterPosition
                // first item is immovable
                if (adapterPosition == 0) return false
                return super.canDropOver(recyclerView, current, target)
            }

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                var flags = 0
                // the top item (transaction params) is always there
                val adapterPosition = viewHolder.bindingAdapterPosition
                if (adapterPosition > 0) {
                    flags = flags or makeFlag(
                        ItemTouchHelper.ACTION_STATE_DRAG,
                        ItemTouchHelper.UP or ItemTouchHelper.DOWN
                    ) or makeFlag(
                        ItemTouchHelper.ACTION_STATE_SWIPE,
                        ItemTouchHelper.START or ItemTouchHelper.END
                    )
                }
                return flags
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.bindingAdapterPosition
                val toPosition = target.bindingAdapterPosition
                if (fromPosition == toPosition) {
                    Logger.debug(
                        "drag",
                        String.format(
                            Locale.US,
                            "Ignoring request to move an account from position %d to %d",
                            fromPosition, toPosition
                        )
                    )
                    return false
                }

                Logger.debug(
                    "drag",
                    String.format(
                        Locale.US,
                        "Moving account from %d to %d",
                        fromPosition, toPosition
                    )
                )
                mModel.moveItem(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                mModel.removeItem(pos)
            }
        })
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        itemTouchHelper.attachToRecyclerView(null)
    }

    override fun getItemId(position: Int): Long {
        // header item is always first and IDs id may duplicate some of the account IDs
        if (position == 0) return 0
        val accRow = differ.currentList[position].asAccountRowItem()
        return accRow.id ?: 0
    }

    override fun getItemViewType(position: Int): Int {
        return differ.currentList[position].type.toInt()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TemplateDetailsItem.TYPE.header -> Header(
                TemplateDetailsHeaderBinding.inflate(inflater, parent, false)
            )
            TemplateDetailsItem.TYPE.accountItem -> AccountRow(
                TemplateDetailsAccountBinding.inflate(inflater, parent, false)
            )
            else -> throw IllegalStateException("Unsupported view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = differ.currentList[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = differ.currentList.size

    fun setItems(items: List<TemplateDetailsItem>) {
        if (BuildConfig.DEBUG) {
            Logger.debug("tmpl", "Got new list")
            for (i in 1 until items.size) {
                val item = items[i]
                Logger.debug(
                    "tmpl",
                    String.format(Locale.US, "  %d: id %d, pos %d", i, item.id, item.position)
                )
            }
        }
        differ.submitList(items)
    }

    fun getMatchGroupText(groupNumber: Int): String? {
        val header = getHeader()
        val p = header.compiledPattern ?: return null

        val testText = Misc.nullIsEmpty(header.testText)
        val m = p.matcher(testText)
        return if (m.matches() && m.groupCount() >= groupNumber) {
            m.group(groupNumber)
        } else {
            null
        }
    }

    protected fun getHeader(): TemplateDetailsItem.Header {
        return differ.currentList[0].asHeaderItem()
    }

    private enum class HeaderDetail {
        DESCRIPTION, COMMENT, DATE_YEAR, DATE_MONTH, DATE_DAY
    }

    private enum class AccDetail {
        ACCOUNT, COMMENT, AMOUNT, CURRENCY
    }

    abstract class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(item: TemplateDetailsItem)
    }

    abstract inner class BaseItem(itemView: View) : ViewHolder(itemView) {
        var updatePropagationDisabled: Boolean = false

        fun disableUpdatePropagation() {
            updatePropagationDisabled = true
        }

        fun enableUpdatePropagation() {
            updatePropagationDisabled = false
        }
    }

    inner class Header(
        private val b: TemplateDetailsHeaderBinding
    ) : BaseItem(b.root) {

        init {
            val templateNameWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    if (updatePropagationDisabled) return

                    val header = getItem()
                    Logger.debug(
                        D_TEMPLATE_UI,
                        "Storing changed template name $s; header=$header"
                    )
                    header.name = s.toString()
                }
            }
            b.templateName.addTextChangedListener(templateNameWatcher)

            val patternWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    if (updatePropagationDisabled) return

                    val header = getItem()
                    Logger.debug(
                        D_TEMPLATE_UI,
                        "Storing changed pattern $s; header=$header"
                    )
                    header.pattern = s.toString()
                    checkPatternError(header)
                }
            }
            b.pattern.addTextChangedListener(patternWatcher)

            val testTextWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    if (updatePropagationDisabled) return

                    val header = getItem()
                    Logger.debug(
                        D_TEMPLATE_UI,
                        "Storing changed test text $s; header=$header"
                    )
                    header.testText = s.toString()
                    checkPatternError(header)
                }
            }
            b.testText.addTextChangedListener(testTextWatcher)

            val transactionDescriptionWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    if (updatePropagationDisabled) return

                    val header = getItem()
                    Logger.debug(
                        D_TEMPLATE_UI,
                        "Storing changed transaction description $s; header=$header"
                    )
                    header.setTransactionDescription(s.toString())
                }
            }
            b.transactionDescription.addTextChangedListener(transactionDescriptionWatcher)

            val transactionCommentWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    if (updatePropagationDisabled) return

                    val header = getItem()
                    Logger.debug(
                        D_TEMPLATE_UI,
                        "Storing changed transaction description $s; header=$header"
                    )
                    header.setTransactionComment(s.toString())
                }
            }
            b.transactionComment.addTextChangedListener(transactionCommentWatcher)

            b.templateIsFallbackSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (updatePropagationDisabled) return@setOnCheckedChangeListener

                getItem().isFallback = isChecked
                b.templateIsFallbackText.setText(
                    if (isChecked) R.string.template_is_fallback_yes
                    else R.string.template_is_fallback_no
                )
            }

            val fallbackLabelClickListener = View.OnClickListener {
                b.templateIsFallbackSwitch.toggle()
            }
            b.templateIsFallbackLabel.setOnClickListener(fallbackLabelClickListener)
            b.templateIsFallbackText.setOnClickListener(fallbackLabelClickListener)

            b.templateParamsHelpButton.setOnClickListener {
                HelpDialog.show(
                    b.root.context,
                    R.string.template_details_template_params_label,
                    R.array.template_params_help
                )
            }
        }

        private fun getItem(): TemplateDetailsItem.Header {
            val pos = bindingAdapterPosition
            return differ.currentList[pos].asHeaderItem()
        }

        private fun selectHeaderDetailSource(v: View, detail: HeaderDetail) {
            val header = getItem()
            Logger.debug(D_TEMPLATE_UI, "header is $header")

            val sel = TemplateDetailSourceSelectorFragment.newInstance(
                1, header.pattern, header.testText
            )
            sel.setOnSourceSelectedListener { literal, group ->
                if (literal) {
                    when (detail) {
                        HeaderDetail.DESCRIPTION -> header.switchToLiteralTransactionDescription()
                        HeaderDetail.COMMENT -> header.switchToLiteralTransactionComment()
                        HeaderDetail.DATE_YEAR -> header.switchToLiteralDateYear()
                        HeaderDetail.DATE_MONTH -> header.switchToLiteralDateMonth()
                        HeaderDetail.DATE_DAY -> header.switchToLiteralDateDay()
                    }
                } else {
                    when (detail) {
                        HeaderDetail.DESCRIPTION -> header.setTransactionDescriptionMatchGroup(group.toInt())
                        HeaderDetail.COMMENT -> header.setTransactionCommentMatchGroup(group.toInt())
                        HeaderDetail.DATE_YEAR -> header.setDateYearMatchGroup(group.toInt())
                        HeaderDetail.DATE_MONTH -> header.setDateMonthMatchGroup(group.toInt())
                        HeaderDetail.DATE_DAY -> header.setDateDayMatchGroup(group.toInt())
                    }
                }
                notifyItemChanged(bindingAdapterPosition)
            }

            val activity = v.context as AppCompatActivity
            sel.show(activity.supportFragmentManager, "template-details-source-selector")
        }

        override fun bind(item: TemplateDetailsItem) {
            val header = item.asHeaderItem()
            Logger.debug(D_TEMPLATE_UI, "Binding to header $header")

            disableUpdatePropagation()
            try {
                val groupNoText = b.root.resources.getString(R.string.template_item_match_group_source)

                b.templateName.setText(header.name)
                b.pattern.setText(header.pattern)
                b.testText.setText(header.testText)

                if (header.hasLiteralDateYear()) {
                    b.yearSource.setText(R.string.template_details_source_literal)
                    val dateYear = header.getDateYear()
                    b.templateDetailsDateYear.setText(dateYear?.toString())
                    b.yearLayout.visibility = View.VISIBLE
                } else {
                    b.yearLayout.visibility = View.GONE
                    b.yearSource.text = String.format(
                        Locale.US, groupNoText,
                        header.getDateYearMatchGroup(),
                        getMatchGroupText(header.getDateYearMatchGroup())
                    )
                }
                b.yearSourceLabel.setOnClickListener { v ->
                    selectHeaderDetailSource(v, HeaderDetail.DATE_YEAR)
                }
                b.yearSource.setOnClickListener { v ->
                    selectHeaderDetailSource(v, HeaderDetail.DATE_YEAR)
                }

                if (header.hasLiteralDateMonth()) {
                    b.monthSource.setText(R.string.template_details_source_literal)
                    val dateMonth = header.getDateMonth()
                    b.templateDetailsDateMonth.setText(dateMonth?.toString())
                    b.monthLayout.visibility = View.VISIBLE
                } else {
                    b.monthLayout.visibility = View.GONE
                    b.monthSource.text = String.format(
                        Locale.US, groupNoText,
                        header.getDateMonthMatchGroup(),
                        getMatchGroupText(header.getDateMonthMatchGroup())
                    )
                }
                b.monthSourceLabel.setOnClickListener { v ->
                    selectHeaderDetailSource(v, HeaderDetail.DATE_MONTH)
                }
                b.monthSource.setOnClickListener { v ->
                    selectHeaderDetailSource(v, HeaderDetail.DATE_MONTH)
                }

                if (header.hasLiteralDateDay()) {
                    b.daySource.setText(R.string.template_details_source_literal)
                    val dateDay = header.getDateDay()
                    b.templateDetailsDateDay.setText(dateDay?.toString())
                    b.dayLayout.visibility = View.VISIBLE
                } else {
                    b.dayLayout.visibility = View.GONE
                    b.daySource.text = String.format(
                        Locale.US, groupNoText,
                        header.getDateDayMatchGroup(),
                        getMatchGroupText(header.getDateDayMatchGroup())
                    )
                }
                b.daySourceLabel.setOnClickListener { v ->
                    selectHeaderDetailSource(v, HeaderDetail.DATE_DAY)
                }
                b.daySource.setOnClickListener { v ->
                    selectHeaderDetailSource(v, HeaderDetail.DATE_DAY)
                }

                if (header.hasLiteralTransactionDescription()) {
                    b.templateTransactionDescriptionSource.setText(R.string.template_details_source_literal)
                    b.transactionDescription.setText(header.getTransactionDescription())
                    b.transactionDescriptionLayout.visibility = View.VISIBLE
                } else {
                    b.transactionDescriptionLayout.visibility = View.GONE
                    b.templateTransactionDescriptionSource.text = String.format(
                        Locale.US, groupNoText,
                        header.getTransactionDescriptionMatchGroup(),
                        getMatchGroupText(header.getTransactionDescriptionMatchGroup())
                    )
                }
                b.templateTransactionDescriptionSourceLabel.setOnClickListener { v ->
                    selectHeaderDetailSource(v, HeaderDetail.DESCRIPTION)
                }
                b.templateTransactionDescriptionSource.setOnClickListener { v ->
                    selectHeaderDetailSource(v, HeaderDetail.DESCRIPTION)
                }

                if (header.hasLiteralTransactionComment()) {
                    b.templateTransactionCommentSource.setText(R.string.template_details_source_literal)
                    b.transactionComment.setText(header.getTransactionComment())
                    b.transactionCommentLayout.visibility = View.VISIBLE
                } else {
                    b.transactionCommentLayout.visibility = View.GONE
                    b.templateTransactionCommentSource.text = String.format(
                        Locale.US, groupNoText,
                        header.getTransactionCommentMatchGroup(),
                        getMatchGroupText(header.getTransactionCommentMatchGroup())
                    )
                }
                b.templateTransactionCommentSourceLabel.setOnClickListener { v ->
                    selectHeaderDetailSource(v, HeaderDetail.COMMENT)
                }
                b.templateTransactionCommentSource.setOnClickListener { v ->
                    selectHeaderDetailSource(v, HeaderDetail.COMMENT)
                }

                b.templateDetailsHeadScanQrButton.setOnClickListener { v -> scanTestQR(v) }

                b.templateIsFallbackSwitch.isChecked = header.isFallback
                b.templateIsFallbackText.setText(
                    if (header.isFallback) R.string.template_is_fallback_yes
                    else R.string.template_is_fallback_no
                )

                checkPatternError(header)
            } finally {
                enableUpdatePropagation()
            }
        }

        private fun checkPatternError(item: TemplateDetailsItem.Header) {
            if (item.patternError != null) {
                b.patternLayout.error = item.patternError
                b.patternHintTitle.visibility = View.GONE
                b.patternHintText.visibility = View.GONE
            } else {
                b.patternLayout.error = null
                if (item.testMatch() != null) {
                    b.patternHintText.text = item.testMatch()
                    b.patternHintTitle.visibility = View.VISIBLE
                    b.patternHintText.visibility = View.VISIBLE
                } else {
                    b.patternLayout.error = null
                    b.patternHintTitle.visibility = View.GONE
                    b.patternHintText.visibility = View.GONE
                }
            }
        }

        private fun scanTestQR(view: View) {
            val ctx = view.context
            if (ctx is QR.QRScanTrigger) {
                ctx.triggerQRScan()
            }
        }
    }

    inner class AccountRow(
        private val b: TemplateDetailsAccountBinding
    ) : BaseItem(b.root) {

        init {
            val accountNameWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    if (updatePropagationDisabled) return

                    val accRow = getItem()
                    Logger.debug(
                        D_TEMPLATE_UI,
                        "Storing changed account name $s; accRow=$accRow"
                    )
                    accRow.setAccountName(s.toString())
                    mModel.applyList(null)
                }
            }
            b.templateDetailsAccountName.addTextChangedListener(accountNameWatcher)
            b.templateDetailsAccountName.setAdapter(AccountAutocompleteAdapter(b.root.context))
            b.templateDetailsAccountName.setOnItemClickListener { _, view, _, _ ->
                b.templateDetailsAccountName.setText((view as TextView).text)
            }

            val accountCommentWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    if (updatePropagationDisabled) return

                    val accRow = getItem()
                    Logger.debug(
                        D_TEMPLATE_UI,
                        "Storing changed account comment $s; accRow=$accRow"
                    )
                    accRow.setAccountComment(s.toString())
                    mModel.applyList(null)
                }
            }
            b.templateDetailsAccountComment.addTextChangedListener(accountCommentWatcher)

            b.templateDetailsAccountAmount.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    if (updatePropagationDisabled) return

                    val accRow = getItem()
                    val str = s.toString()
                    if (Misc.emptyIsNull(str) == null) {
                        accRow.setAmount(null)
                    } else {
                        try {
                            val amount = Data.parseNumber(str)
                            accRow.setAmount(amount)
                            b.templateDetailsAccountAmountLayout.error = null

                            Logger.debug(
                                D_TEMPLATE_UI,
                                String.format(
                                    Locale.US,
                                    "Storing changed account amount %s [%4.2f]; accRow=%s",
                                    s, amount, accRow
                                )
                            )
                        } catch (e: NumberFormatException) {
                            b.templateDetailsAccountAmountLayout.error = "!"
                        } catch (e: ParseException) {
                            b.templateDetailsAccountAmountLayout.error = "!"
                        }
                    }
                    mModel.applyList(null)
                }
            })

            b.templateDetailsAccountAmount.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) return@setOnFocusChangeListener

                val accRow = getItem()
                if (!accRow.hasLiteralAmount()) return@setOnFocusChangeListener

                val amt = accRow.getAmount() ?: return@setOnFocusChangeListener
                b.templateDetailsAccountAmount.setText(Data.formatNumber(amt))
            }

            b.negateAmountSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (updatePropagationDisabled) return@setOnCheckedChangeListener

                getItem().isNegateAmount = isChecked
                b.templateDetailsNegateAmountText.setText(
                    if (isChecked) R.string.template_account_change_amount_sign
                    else R.string.template_account_keep_amount_sign
                )
            }

            val negLabelClickListener = View.OnClickListener {
                b.negateAmountSwitch.toggle()
            }
            b.templateDetailsNegateAmountLabel.setOnClickListener(negLabelClickListener)
            b.templateDetailsNegateAmountText.setOnClickListener(negLabelClickListener)

            manageAccountLabelDrag()
        }

        @SuppressLint("ClickableViewAccessibility")
        fun manageAccountLabelDrag() {
            b.patternAccountLabel.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper.startDrag(this)
                }
                false
            }
        }

        override fun bind(item: TemplateDetailsItem) {
            disableUpdatePropagation()
            try {
                val resources = b.root.resources
                val groupNoText = resources.getString(R.string.template_item_match_group_source)

                Logger.debug(
                    "drag",
                    String.format(
                        Locale.US,
                        "Binding account id %d, pos %d at %d",
                        item.id, item.position, bindingAdapterPosition
                    )
                )

                val accRow = item.asAccountRowItem()
                b.patternAccountLabel.text = String.format(
                    Locale.US,
                    resources.getString(R.string.template_details_account_row_label),
                    accRow.position
                )

                if (accRow.hasLiteralAccountName()) {
                    b.templateDetailsAccountNameLayout.visibility = View.VISIBLE
                    b.templateDetailsAccountName.setText(accRow.getAccountName())
                    b.templateDetailsAccountNameSource.setText(R.string.template_details_source_literal)
                } else {
                    b.templateDetailsAccountNameLayout.visibility = View.GONE
                    b.templateDetailsAccountNameSource.text = String.format(
                        Locale.US, groupNoText,
                        accRow.getAccountNameMatchGroup(),
                        getMatchGroupText(accRow.getAccountNameMatchGroup())
                    )
                }

                if (accRow.hasLiteralAccountComment()) {
                    b.templateDetailsAccountCommentLayout.visibility = View.VISIBLE
                    b.templateDetailsAccountComment.setText(accRow.getAccountComment())
                    b.templateDetailsAccountCommentSource.setText(R.string.template_details_source_literal)
                } else {
                    b.templateDetailsAccountCommentLayout.visibility = View.GONE
                    b.templateDetailsAccountCommentSource.text = String.format(
                        Locale.US, groupNoText,
                        accRow.getAccountCommentMatchGroup(),
                        getMatchGroupText(accRow.getAccountCommentMatchGroup())
                    )
                }

                if (accRow.hasLiteralAmount()) {
                    b.templateDetailsAccountAmountSource.setText(R.string.template_details_source_literal)
                    b.templateDetailsAccountAmount.visibility = View.VISIBLE
                    val amt = accRow.getAmount()
                    b.templateDetailsAccountAmount.setText(
                        if (amt == null) null
                        else String.format(Data.locale.value, "%,4.2f", accRow.getAmount())
                    )
                    b.negateAmountSwitch.visibility = View.GONE
                    b.templateDetailsNegateAmountLabel.visibility = View.GONE
                    b.templateDetailsNegateAmountText.visibility = View.GONE
                } else {
                    b.templateDetailsAccountAmountSource.text = String.format(
                        Locale.US, groupNoText,
                        accRow.getAmountMatchGroup(),
                        getMatchGroupText(accRow.getAmountMatchGroup())
                    )
                    b.templateDetailsAccountAmountLayout.visibility = View.GONE
                    b.negateAmountSwitch.visibility = View.VISIBLE
                    b.negateAmountSwitch.isChecked = accRow.isNegateAmount
                    b.templateDetailsNegateAmountText.setText(
                        if (accRow.isNegateAmount) R.string.template_account_change_amount_sign
                        else R.string.template_account_keep_amount_sign
                    )
                    b.templateDetailsNegateAmountLabel.visibility = View.VISIBLE
                    b.templateDetailsNegateAmountText.visibility = View.VISIBLE
                }

                if (accRow.hasLiteralCurrency()) {
                    b.templateDetailsAccountCurrencySource.setText(R.string.template_details_source_literal)
                    val c = accRow.getCurrency()
                    if (c == null) {
                        b.templateDetailsAccountCurrency.setText(R.string.btn_no_currency)
                    } else {
                        b.templateDetailsAccountCurrency.text = c.name
                    }
                    b.templateDetailsAccountCurrency.visibility = View.VISIBLE
                } else {
                    b.templateDetailsAccountCurrencySource.text = String.format(
                        Locale.US, groupNoText,
                        accRow.getCurrencyMatchGroup(),
                        getMatchGroupText(accRow.getCurrencyMatchGroup())
                    )
                    b.templateDetailsAccountCurrency.visibility = View.GONE
                }

                b.templateAccountNameSourceLabel.setOnClickListener { v ->
                    selectAccountRowDetailSource(v, AccDetail.ACCOUNT)
                }
                b.templateDetailsAccountNameSource.setOnClickListener { v ->
                    selectAccountRowDetailSource(v, AccDetail.ACCOUNT)
                }
                b.templateAccountCommentSourceLabel.setOnClickListener { v ->
                    selectAccountRowDetailSource(v, AccDetail.COMMENT)
                }
                b.templateDetailsAccountCommentSource.setOnClickListener { v ->
                    selectAccountRowDetailSource(v, AccDetail.COMMENT)
                }
                b.templateAccountAmountSourceLabel.setOnClickListener { v ->
                    selectAccountRowDetailSource(v, AccDetail.AMOUNT)
                }
                b.templateDetailsAccountAmountSource.setOnClickListener { v ->
                    selectAccountRowDetailSource(v, AccDetail.AMOUNT)
                }
                b.templateDetailsAccountCurrencySource.setOnClickListener { v ->
                    selectAccountRowDetailSource(v, AccDetail.CURRENCY)
                }
                b.templateAccountCurrencySourceLabel.setOnClickListener { v ->
                    selectAccountRowDetailSource(v, AccDetail.CURRENCY)
                }

                if (accRow.hasLiteralCurrency()) {
                    b.templateDetailsAccountCurrency.setOnClickListener {
                        val cpf = CurrencySelectorFragment.newInstance(
                            CurrencySelectorFragment.DEFAULT_COLUMN_COUNT, false
                        )
                        cpf.setOnCurrencySelectedListener { text ->
                            if (text == null) {
                                b.templateDetailsAccountCurrency.setText(R.string.btn_no_currency)
                                accRow.setCurrency(null)
                            } else {
                                b.templateDetailsAccountCurrency.text = text
                                DB.get()
                                    .getCurrencyDAO()
                                    .getByName(text)
                                    .observe(b.root.context as LifecycleOwner) { currency ->
                                        accRow.setCurrency(currency)
                                    }
                            }
                        }
                        cpf.show(
                            (b.templateDetailsAccountCurrency.context as TemplatesActivity).supportFragmentManager,
                            "currency-selector"
                        )
                    }
                }
            } finally {
                enableUpdatePropagation()
            }
        }

        private fun getItem(): TemplateDetailsItem.AccountRow {
            return differ.currentList[bindingAdapterPosition].asAccountRowItem()
        }

        private fun selectAccountRowDetailSource(v: View, detail: AccDetail) {
            val accRow = getItem()
            val header = getHeader()
            Logger.debug(D_TEMPLATE_UI, "header is $header")

            val sel = TemplateDetailSourceSelectorFragment.newInstance(
                1, header.pattern, header.testText
            )
            sel.setOnSourceSelectedListener { literal, group ->
                if (literal) {
                    when (detail) {
                        AccDetail.ACCOUNT -> accRow.switchToLiteralAccountName()
                        AccDetail.COMMENT -> accRow.switchToLiteralAccountComment()
                        AccDetail.AMOUNT -> accRow.switchToLiteralAmount()
                        AccDetail.CURRENCY -> accRow.switchToLiteralCurrency()
                    }
                } else {
                    when (detail) {
                        AccDetail.ACCOUNT -> accRow.setAccountNameMatchGroup(group.toInt())
                        AccDetail.COMMENT -> accRow.setAccountCommentMatchGroup(group.toInt())
                        AccDetail.AMOUNT -> accRow.setAmountMatchGroup(group.toInt())
                        AccDetail.CURRENCY -> accRow.setCurrencyMatchGroup(group.toInt())
                    }
                }
                notifyItemChanged(bindingAdapterPosition)
            }

            val activity = v.context as AppCompatActivity
            sel.show(activity.supportFragmentManager, "template-details-source-selector")
        }
    }

    companion object {
        private const val D_TEMPLATE_UI = "template-ui"
    }
}
