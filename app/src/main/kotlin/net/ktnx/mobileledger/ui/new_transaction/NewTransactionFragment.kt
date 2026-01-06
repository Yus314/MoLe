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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.json.API
import net.ktnx.mobileledger.model.Data
import net.ktnx.mobileledger.model.LedgerTransaction
import net.ktnx.mobileledger.ui.FabManager
import net.ktnx.mobileledger.ui.QR
import net.ktnx.mobileledger.ui.profiles.ProfileDetailActivity
import net.ktnx.mobileledger.utils.Logger

// TODO: offer to undo account remove-on-swipe

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [OnNewTransactionFragmentInteractionListener] interface
 * to handle interaction events.
 */
@Suppress("DEPRECATION")
class NewTransactionFragment : Fragment() {
    private var listAdapter: NewTransactionItemsAdapter? = null
    private lateinit var viewModel: NewTransactionModel
    private var mListener: OnNewTransactionFragmentInteractionListener? = null
    private var mProfile: Profile? = null

    init {
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val activity = activity

        inflater.inflate(R.menu.new_transaction_fragment, menu)

        menu.findItem(R.id.scan_qr)
            .setOnMenuItemClickListener { item -> onScanQrAction(item) }

        menu.findItem(R.id.action_reset_new_transaction_activity)
            .setOnMenuItemClickListener {
                viewModel.reset()
                true
            }

        val toggleCurrencyItem = menu.findItem(R.id.toggle_currency)
        toggleCurrencyItem.setOnMenuItemClickListener {
            viewModel.toggleCurrencyVisible()
            true
        }
        activity?.let {
            viewModel.getShowCurrency()
                .observe(it) { checked -> toggleCurrencyItem.isChecked = checked }
        }

        val toggleCommentsItem = menu.findItem(R.id.toggle_comments)
        toggleCommentsItem.setOnMenuItemClickListener {
            viewModel.toggleShowComments()
            true
        }
        activity?.let {
            viewModel.getShowComments()
                .observe(it) { checked -> toggleCommentsItem.isChecked = checked }
        }
    }

    private fun onScanQrAction(item: MenuItem): Boolean {
        try {
            val ctx = requireContext()
            if (ctx is QR.QRScanTrigger) {
                ctx.triggerQRScan()
            }
        } catch (e: Exception) {
            Logger.debug("qr", "Error launching QR scanner", e)
        }

        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_new_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity
            ?: throw IllegalStateException("getActivity() returned null within onViewCreated()")

        viewModel = ViewModelProvider(activity)[NewTransactionModel::class.java]
        viewModel.observeDataProfile(this)
        mProfile = Data.getProfile()
        listAdapter = mProfile?.let { NewTransactionItemsAdapter(viewModel, it) }

        viewModel.getItems()
            .observe(viewLifecycleOwner) { newList -> listAdapter?.setItems(newList) }

        val list: RecyclerView = activity.findViewById(R.id.new_transaction_accounts)
        list.adapter = listAdapter
        list.layoutManager = LinearLayoutManager(activity)

        Data.observeProfile(viewLifecycleOwner) { profile ->
            mProfile = profile
            profile?.let { listAdapter?.setProfile(it) }
        }
        var keep = false

        val args = arguments
        if (args != null) {
            val error = args.getString("error")
            if (error != null) {
                Logger.debug("new-trans-f", String.format("Got error: %s", error))

                val context = context
                if (context != null) {
                    val builder = AlertDialog.Builder(context)
                    val resources = context.resources
                    val message = StringBuilder()
                    message.append(resources.getString(R.string.err_json_send_error_head))
                        .append("\n\n")
                        .append(error)
                        .append("\n\n")
                    if (API.valueOf(mProfile?.apiVersion ?: 0) == API.auto) {
                        message.append(
                            resources.getString(R.string.err_json_send_error_unsupported)
                        )
                    } else {
                        message.append(resources.getString(R.string.err_json_send_error_tail))
                        builder.setPositiveButton(R.string.btn_profile_options) { _, _ ->
                            Logger.debug("error", "will start profile editor")
                            mProfile?.let { ProfileDetailActivity.start(context, it) }
                        }
                    }
                    builder.setMessage(message)
                    builder.create()
                        .show()
                } else {
                    Snackbar.make(list, error, Snackbar.LENGTH_INDEFINITE)
                        .show()
                }
                keep = true
            }
        }

        var focused = 0
        var element: FocusedElement? = null
        if (savedInstanceState != null) {
            keep = keep or savedInstanceState.getBoolean("keep", true)
            focused = savedInstanceState.getInt("focused-item", 0)
            val focusedElementString = savedInstanceState.getString("focused-element")
            if (focusedElementString != null) {
                element = FocusedElement.valueOf(focusedElementString)
            }
        }

        if (!keep) {
            // we need the DB up and running
            Data.observeProfile(viewLifecycleOwner) { p ->
                if (p != null) {
                    viewModel.reset()
                }
            }
        } else {
            viewModel.noteFocusChanged(focused, element)
        }

        val p: ProgressBar = activity.findViewById(R.id.progressBar)
        viewModel.getBusyFlag()
            .observe(viewLifecycleOwner) { isBusy ->
                if (isBusy) {
                    p.visibility = View.VISIBLE
                } else {
                    p.visibility = View.INVISIBLE
                }
            }

        if (activity is FabManager.FabHandler) {
            FabManager.handle(activity, list)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("keep", true)
        val focusInfo = viewModel.getFocusInfo().value
        if (focusInfo != null) {
            val focusedItem = focusInfo.position
            if (focusedItem >= 0) {
                outState.putInt("focused-item", focusedItem)
            }
            focusInfo.element?.let {
                outState.putString("focused-element", it.toString())
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnNewTransactionFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(
                "$context must implement OnFragmentInteractionListener"
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
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
    interface OnNewTransactionFragmentInteractionListener {
        fun onTransactionSave(tr: LedgerTransaction)
    }
}
