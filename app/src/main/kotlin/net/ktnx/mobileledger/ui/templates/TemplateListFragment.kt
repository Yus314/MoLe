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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.databinding.FragmentTemplateListBinding
import net.ktnx.mobileledger.db.DB
import net.ktnx.mobileledger.ui.FabManager
import net.ktnx.mobileledger.ui.HelpDialog
import net.ktnx.mobileledger.utils.Logger

/**
 * A simple [Fragment] subclass.
 * Use the [TemplateListFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TemplateListFragment : Fragment() {
    private var b: FragmentTemplateListBinding? = null
    private var mListener: OnTemplateListFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        setHasOptionsMenu(true)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.template_list_menu, menu)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu_item_template_list_help -> {
            HelpDialog.show(
                requireContext(),
                R.string.template_list_help_title,
                R.array.template_list_help_text
            )
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Logger.debug("flow", "PatternListFragment.onCreateView()")
        b = FragmentTemplateListBinding.inflate(inflater)
        val activity = requireActivity()

        if (activity is FabManager.FabHandler) {
            b?.templateList?.let { FabManager.handle(activity, it) }
        }

        val modelAdapter = TemplatesRecyclerViewAdapter()

        b?.templateList?.adapter = modelAdapter
        val pDao = DB.get().getTemplateDAO()
        val templates = pDao.getTemplates()
        templates.observe(viewLifecycleOwner) { list -> modelAdapter.setTemplates(list) }
        val llm = LinearLayoutManager(context)
        llm.orientation = RecyclerView.VERTICAL
        b?.templateList?.layoutManager = llm
        val did = TemplateListDivider(activity, DividerItemDecoration.VERTICAL)
        b?.templateList?.addItemDecoration(did)

        return b?.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = if (context is OnTemplateListFragmentInteractionListener) {
            context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }

        val observer = object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event.targetState == Lifecycle.State.CREATED) {
                    // getActivity().setActionBar(b.toolbar);
                    lifecycle.removeObserver(this)
                }
            }
        }
        lifecycle.addObserver(observer)
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     * See the Android Training lesson
     * [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnTemplateListFragmentInteractionListener {
        fun onSaveTemplate()
        fun onEditTemplate(id: Long?)
        fun onDuplicateTemplate(id: Long)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment TemplateListFragment.
         */
        @JvmStatic
        fun newInstance(): TemplateListFragment {
            val fragment = TemplateListFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}
