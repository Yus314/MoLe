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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.databinding.TemplateDetailsFragmentBinding
import net.ktnx.mobileledger.ui.FabManager
import net.ktnx.mobileledger.utils.Logger

@Suppress("DEPRECATION")
class TemplateDetailsFragment : Fragment() {
    private var binding: TemplateDetailsFragmentBinding? = null
    private var viewModel: TemplateDetailsViewModel? = null
    private var columnCount = 1
    private var patternId: Long? = null
    private var interactionListener: InteractionListener? = null

    interface InteractionListener {
        fun onDeleteTemplate(templateId: Long)
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.template_details_menu, menu)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
            R.id.delete_template -> {
                signalDeleteTemplateInteraction()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }

    private fun signalDeleteTemplateInteraction() {
        patternId?.let { id ->
            interactionListener?.onDeleteTemplate(id)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let { args ->
            columnCount = args.getInt(ARG_COLUMN_COUNT, 1)
            val id = args.getLong(ARG_TEMPLATE_ID, -1)
            patternId = if (id == -1L) null else id
        }

        setHasOptionsMenu(patternId != null)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val activity = requireActivity()
        if (activity !is InteractionListener) {
            throw IllegalStateException(
                "Containing activity must implement TemplateDetailsFragment.InteractionListener"
            )
        }
        interactionListener = activity

        val controller = (activity as TemplatesActivity).getNavController()
        val viewModelStoreOwner = controller.getViewModelStoreOwner(R.id.template_list_navigation)
        viewModel = ViewModelProvider(viewModelStoreOwner)[TemplateDetailsViewModel::class.java].also {
            it.defaultTemplateName = getString(R.string.unnamed_template)
        }
        Logger.debug("flow", "PatternDetailsFragment.onCreateView(): model=$viewModel")

        val b = TemplateDetailsFragmentBinding.inflate(inflater)
        binding = b

        val context = b.patternDetailsRecyclerView.context
        b.patternDetailsRecyclerView.layoutManager = when {
            columnCount <= 1 -> LinearLayoutManager(context)
            else -> GridLayoutManager(context, columnCount)
        }

        val vm = viewModel
        if (vm == null) {
            Logger.debug("flow", "PatternDetailsFragment.onCreateView(): viewModel is null")
            return b.root
        }
        val adapter = TemplateDetailsAdapter(vm)
        b.patternDetailsRecyclerView.adapter = adapter
        vm.getItems(patternId).observe(viewLifecycleOwner) { items ->
            adapter.setItems(items)
        }

        if (activity is FabManager.FabHandler) {
            FabManager.handle(activity, b.patternDetailsRecyclerView)
        }

        return b.root
    }

    companion object {
        const val ARG_TEMPLATE_ID = "pattern-id"
        private const val ARG_COLUMN_COUNT = "column-count"

        @JvmStatic
        fun newInstance(columnCount: Int, patternId: Int): TemplateDetailsFragment = TemplateDetailsFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                    if (patternId > 0) {
                        putInt(ARG_TEMPLATE_ID, patternId)
                    }
                }
            }
    }
}
