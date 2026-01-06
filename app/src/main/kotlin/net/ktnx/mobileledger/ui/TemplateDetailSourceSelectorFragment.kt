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

package net.ktnx.mobileledger.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.databinding.FragmentTemplateDetailSourceSelectorListBinding
import net.ktnx.mobileledger.model.TemplateDetailSource
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.Misc
import java.util.regex.Pattern

/**
 * A fragment representing a list of Items.
 *
 * Activities containing this fragment MUST implement the [OnSourceSelectedListener]
 * interface.
 */
class TemplateDetailSourceSelectorFragment : AppCompatDialogFragment(), OnSourceSelectedListener {

    private var mColumnCount = DEFAULT_COLUMN_COUNT
    private var mSources: ArrayList<TemplateDetailSource>? = null
    private var model: TemplateDetailSourceSelectorModel? = null
    private var onSourceSelectedListener: OnSourceSelectedListener? = null
    @StringRes
    private var mPatternProblem: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let { args ->
            mColumnCount = args.getInt(ARG_COLUMN_COUNT, DEFAULT_COLUMN_COUNT)
            val patternText = args.getString(ARG_PATTERN)
            val testText = args.getString(ARG_TEST_TEXT)

            if (Misc.emptyIsNull(patternText) == null) {
                mPatternProblem = R.string.missing_pattern_error
            } else {
                if (Misc.emptyIsNull(testText) == null) {
                    mPatternProblem = R.string.missing_test_text
                } else {
                    val pattern = Pattern.compile(patternText)
                    // testText is guaranteed non-null here (checked by Misc.emptyIsNull above)
                    val matcher = pattern.matcher(testText)
                    Logger.debug("templates",
                        String.format("Trying to match pattern '%s' against text '%s'",
                            patternText, testText))
                    if (matcher.find()) {
                        if (matcher.groupCount() >= 0) {
                            val list = ArrayList<TemplateDetailSource>()
                            for (g in 1..matcher.groupCount()) {
                                list.add(TemplateDetailSource(g.toShort(), matcher.group(g)))
                            }
                            mSources = list
                        } else {
                            mPatternProblem = R.string.pattern_without_groups
                        }
                    } else {
                        mPatternProblem = R.string.pattern_does_not_match
                    }
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val csd = Dialog(context)
        val b = FragmentTemplateDetailSourceSelectorListBinding.inflate(
            LayoutInflater.from(context))
        csd.setContentView(b.root)
        csd.setTitle(R.string.choose_template_detail_source_label)

        if (!mSources.isNullOrEmpty()) {
            val recyclerView = b.list

            if (mColumnCount <= 1) {
                recyclerView.layoutManager = LinearLayoutManager(context)
            } else {
                recyclerView.layoutManager = GridLayoutManager(context, mColumnCount)
            }

            model = ViewModelProvider(this)[TemplateDetailSourceSelectorModel::class.java]
            onSourceSelectedListener?.let { model?.setOnSourceSelectedListener(it) }
            mSources?.let { model?.setSourcesList(it) }

            val adapter = TemplateDetailSourceSelectorRecyclerViewAdapter()
            model?.groups?.observe(this) { adapter.submitList(it) }

            recyclerView.adapter = adapter
            adapter.setSourceSelectedListener(this)
        } else {
            b.list.visibility = View.GONE
            b.templateError.text = getString(
                if (mPatternProblem != 0) mPatternProblem else R.string.pattern_without_groups
            )
            b.templateError.visibility = View.VISIBLE
        }

        b.literalButton.setOnClickListener { onSourceSelected(true, -1) }

        return csd
    }

    fun setOnSourceSelectedListener(listener: OnSourceSelectedListener?) {
        onSourceSelectedListener = listener
        model?.setOnSourceSelectedListener(listener)
    }

    fun resetOnSourceSelectedListener() {
        model?.resetOnSourceSelectedListener()
    }

    override fun onSourceSelected(literal: Boolean, group: Short) {
        model?.triggerOnSourceSelectedListener(literal, group)
        onSourceSelectedListener?.onSourceSelected(literal, group)
        dismiss()
    }

    companion object {
        const val DEFAULT_COLUMN_COUNT = 1
        const val ARG_COLUMN_COUNT = "column-count"
        const val ARG_PATTERN = "pattern"
        const val ARG_TEST_TEXT = "test-text"

        @JvmStatic
        @Suppress("unused")
        fun newInstance(): TemplateDetailSourceSelectorFragment {
            return newInstance(DEFAULT_COLUMN_COUNT, null, null)
        }

        @JvmStatic
        fun newInstance(
            columnCount: Int,
            pattern: String?,
            testText: String?
        ): TemplateDetailSourceSelectorFragment {
            return TemplateDetailSourceSelectorFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                    pattern?.let { putString(ARG_PATTERN, it) }
                    testText?.let { putString(ARG_TEST_TEXT, it) }
                }
            }
        }
    }
}
