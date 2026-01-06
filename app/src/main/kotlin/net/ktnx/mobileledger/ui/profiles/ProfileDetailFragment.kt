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

package net.ktnx.mobileledger.ui.profiles

import android.app.AlertDialog
import android.app.backup.BackupManager
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputLayout
import net.ktnx.mobileledger.BuildConfig
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.dao.BaseDAO
import net.ktnx.mobileledger.databinding.ProfileDetailBinding
import net.ktnx.mobileledger.db.DB
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.json.API
import net.ktnx.mobileledger.model.Data
import net.ktnx.mobileledger.model.FutureDates
import net.ktnx.mobileledger.ui.CurrencySelectorFragment
import net.ktnx.mobileledger.ui.HueRingDialog
import net.ktnx.mobileledger.utils.Colors
import net.ktnx.mobileledger.utils.Logger.debug
import net.ktnx.mobileledger.utils.Misc
import java.net.MalformedURLException
import java.net.URL

/**
 * A fragment representing a single Profile detail screen.
 * a [ProfileDetailActivity]
 * on handsets.
 */
class ProfileDetailFragment : Fragment(R.layout.profile_detail) {
    private var defaultCommoditySet = false
    private var syncingModelFromUI = false
    private var binding: ProfileDetailBinding? = null

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        debug("profiles", "[fragment] Creating profile details options menu")
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.profile_details, menu)
        val menuDeleteProfile = menu.findItem(R.id.menuDelete)
        menuDeleteProfile.setOnMenuItemClickListener { onDeleteProfile() }
        val profiles = Data.profiles.value

        val menuWipeProfileData = menu.findItem(R.id.menuWipeData)
        if (BuildConfig.DEBUG) {
            menuWipeProfileData.setOnMenuItemClickListener { onWipeDataMenuClicked() }
        }

        getModel().getProfileId().observe(viewLifecycleOwner) { id ->
            menuDeleteProfile.isVisible = id > 0
            if (BuildConfig.DEBUG) {
                menuWipeProfileData.isVisible = id > 0
            }
        }
    }

    private fun onDeleteProfile(): Boolean {
        val builder = AlertDialog.Builder(context)
        val model = getModel()
        builder.setTitle(model.getProfileName())
        builder.setMessage(R.string.remove_profile_dialog_message)
        builder.setPositiveButton(R.string.Remove) { _, _ ->
            val profileId = model.getProfileId().value ?: return@setPositiveButton
            debug("profiles", String.format("[fragment] removing profile %s", profileId))
            val dao = DB.get().getProfileDAO()
            dao.getById(profileId).observe(viewLifecycleOwner) { profile ->
                if (profile != null) {
                    BaseDAO.runAsync {
                        DB.get().runInTransaction {
                            dao.deleteSync(profile)
                            dao.updateOrderSync(dao.getAllOrderedSync())
                        }
                    }
                }
            }

            activity?.finish()
        }
        builder.show()
        return false
    }

    private fun onWipeDataMenuClicked(): Boolean {
        // this is a development option, so no confirmation
        val profileId = getModel().getProfileId().value ?: return false
        DB.get()
            .getProfileDAO()
            .getById(profileId)
            .observe(viewLifecycleOwner) { profile ->
                profile?.wipeAllData()
            }
        return true
    }

    private fun hookTextChangeSyncRoutine(view: TextView, syncRoutine: TextChangeSyncRoutine) {
        view.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                syncRoutine.onTextChanged(s?.toString() ?: "")
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = ProfileDetailBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = activity ?: return

        val viewLifecycleOwner = getViewLifecycleOwner()
        val model = getModel()

        model.observeDefaultCommodity(viewLifecycleOwner) { c ->
            if (c != null) {
                setDefaultCommodity(c)
            } else {
                resetDefaultCommodity()
            }
        }

        val fab = context.findViewById<View>(R.id.fabAdd)
        fab.setOnClickListener { onSaveFabClicked() }

        binding?.let { b ->
            hookTextChangeSyncRoutine(b.profileName) { model.setProfileName(it) }
            model.observeProfileName(viewLifecycleOwner) { pn ->
                if (!Misc.equalStrings(pn, Misc.nullIsEmpty(b.profileName.text))) {
                    b.profileName.setText(pn)
                }
            }

            hookTextChangeSyncRoutine(b.url) { model.setUrl(it) }
            model.observeUrl(viewLifecycleOwner) { u ->
                if (!Misc.equalStrings(u ?: "", Misc.nullIsEmpty(b.url.text))) {
                    b.url.setText(u ?: "")
                }
            }

            b.defaultCommodityLayout.setOnClickListener { v ->
                val cpf = CurrencySelectorFragment.newInstance(
                    CurrencySelectorFragment.DEFAULT_COLUMN_COUNT, false
                )
                cpf.setOnCurrencySelectedListener { currency -> model.setDefaultCommodity(currency) }
                val activity = v.context as AppCompatActivity
                cpf.show(activity.supportFragmentManager, "currency-selector")
            }

            b.profileShowCommodity.setOnCheckedChangeListener { _, isChecked ->
                model.setShowCommodityByDefault(isChecked)
            }
            model.observeShowCommodityByDefault(viewLifecycleOwner) { checked ->
                b.profileShowCommodity.isChecked = checked
            }

            model.observePostingPermitted(viewLifecycleOwner) { isChecked ->
                b.profilePermitPosting.isChecked = isChecked
                b.postingSubItems.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
            b.profilePermitPosting.setOnCheckedChangeListener { _, isChecked ->
                model.setPostingPermitted(isChecked)
            }

            model.observeShowCommentsByDefault(viewLifecycleOwner) { checked ->
                b.profileShowComments.isChecked = checked
            }
            b.profileShowComments.setOnCheckedChangeListener { _, isChecked ->
                model.setShowCommentsByDefault(isChecked)
            }

            b.futureDatesLayout.setOnClickListener { v ->
                val mi = MenuInflater(context)
                val menu = PopupMenu(context, v)
                menu.inflate(R.menu.future_dates)
                menu.setOnMenuItemClickListener { item ->
                    model.setFutureDates(futureDatesSettingFromMenuItemId(item.itemId))
                    true
                }
                menu.show()
            }
            model.observeFutureDates(viewLifecycleOwner) { v ->
                b.futureDatesText.text = v.getText(resources)
            }

            model.observeApiVersion(viewLifecycleOwner) { apiVer ->
                b.apiVersionText.text = apiVer.getDescription(resources)
            }
            b.apiVersionLabel.setOnClickListener { v -> chooseAPIVersion(v) }
            b.apiVersionText.setOnClickListener { v -> chooseAPIVersion(v) }

            b.serverVersionLabel.setOnClickListener { model.triggerVersionDetection() }
            model.observeDetectedVersion(viewLifecycleOwner) { ver ->
                when {
                    ver == null -> b.detectedServerVersionText.setText(
                        R.string.server_version_unknown_label
                    )
                    ver.isPre_1_20_1 -> b.detectedServerVersionText.setText(
                        R.string.detected_server_pre_1_20_1
                    )
                    else -> b.detectedServerVersionText.text = ver.toString()
                }
            }
            b.detectedServerVersionText.setOnClickListener { model.triggerVersionDetection() }
            b.serverVersionDetectButton.setOnClickListener { model.triggerVersionDetection() }
            model.observeDetectingHledgerVersion(viewLifecycleOwner) { running ->
                b.serverVersionDetectButton.visibility =
                    if (running) View.VISIBLE else View.INVISIBLE
            }

            b.enableHttpAuth.setOnCheckedChangeListener { _, isChecked ->
                val wasOn = model.getUseAuthentication()
                model.setUseAuthentication(isChecked)
                if (!wasOn && isChecked) {
                    b.authUserName.requestFocus()
                }
            }
            model.observeUseAuthentication(viewLifecycleOwner) { isChecked ->
                b.enableHttpAuth.isChecked = isChecked
                b.authParams.visibility = if (isChecked) View.VISIBLE else View.GONE
                checkInsecureSchemeWithAuth()
            }

            model.observeUserName(viewLifecycleOwner) { text ->
                if (!Misc.equalStrings(text ?: "", Misc.nullIsEmpty(b.authUserName.text))) {
                    b.authUserName.setText(text ?: "")
                }
            }
            hookTextChangeSyncRoutine(b.authUserName) { model.setAuthUserName(it) }

            model.observePassword(viewLifecycleOwner) { text ->
                if (!Misc.equalStrings(text ?: "", Misc.nullIsEmpty(b.password.text))) {
                    b.password.setText(text ?: "")
                }
            }
            hookTextChangeSyncRoutine(b.password) { model.setAuthPassword(it) }

            model.observeThemeId(viewLifecycleOwner) { themeId ->
                val hue = if (themeId == -1) Colors.DEFAULT_HUE_DEG else themeId
                val profileColor = Colors.getPrimaryColorForHue(hue)
                b.btnPickRingColor.setBackgroundColor(profileColor)
                b.btnPickRingColor.tag = hue
            }

            model.observePreferredAccountsFilter(viewLifecycleOwner) { text ->
                if (!Misc.equalStrings(text ?: "", Misc.nullIsEmpty(b.preferredAccountsFilter.text))) {
                    b.preferredAccountsFilter.setText(text ?: "")
                }
            }
            hookTextChangeSyncRoutine(b.preferredAccountsFilter) {
                model.setPreferredAccountsFilter(it)
            }

            hookClearErrorOnFocusListener(b.profileName, b.profileNameLayout)
            hookClearErrorOnFocusListener(b.url, b.urlLayout)
            hookClearErrorOnFocusListener(b.authUserName, b.authUserNameLayout)
            hookClearErrorOnFocusListener(b.password, b.passwordLayout)

            b.url.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    checkInsecureSchemeWithAuth()
                }
            })

            b.btnPickRingColor.setOnClickListener { v ->
                val d = HueRingDialog(
                    this@ProfileDetailFragment.requireContext(),
                    model.initialThemeHue,
                    v.tag as Int
                )
                d.show()
                d.setColorSelectedListener { model.setThemeId(it) }
            }

            b.profileName.requestFocus()
        }
    }

    private fun chooseAPIVersion(v: View) {
        val context = activity ?: return
        val model = getModel()
        val menu = PopupMenu(context, v)
        menu.inflate(R.menu.api_version)
        menu.setOnMenuItemClickListener { item ->
            val apiVer = when (item.itemId) {
                R.id.api_version_menu_html -> API.html
                R.id.api_version_menu_1_23 -> API.v1_23
                R.id.api_version_menu_1_19_1 -> API.v1_19_1
                R.id.api_version_menu_1_15 -> API.v1_15
                R.id.api_version_menu_1_14 -> API.v1_14
                else -> API.auto
            }
            model.setApiVersion(apiVer)
            binding?.apiVersionText?.text = apiVer.getDescription(resources)
            true
        }
        menu.show()
    }

    private fun futureDatesSettingFromMenuItemId(itemId: Int): FutureDates {
        return when (itemId) {
            R.id.menu_future_dates_7 -> FutureDates.OneWeek
            R.id.menu_future_dates_14 -> FutureDates.TwoWeeks
            R.id.menu_future_dates_30 -> FutureDates.OneMonth
            R.id.menu_future_dates_60 -> FutureDates.TwoMonths
            R.id.menu_future_dates_90 -> FutureDates.ThreeMonths
            R.id.menu_future_dates_180 -> FutureDates.SixMonths
            R.id.menu_future_dates_365 -> FutureDates.OneYear
            R.id.menu_future_dates_all -> FutureDates.All
            else -> FutureDates.None
        }
    }

    private fun getModel(): ProfileDetailModel {
        return ViewModelProvider(requireActivity())[ProfileDetailModel::class.java]
    }

    private fun onSaveFabClicked() {
        if (!checkValidity()) return

        val model = getModel()
        val dao = DB.get().getProfileDAO()

        val profile = Profile()
        model.updateProfile(profile)
        if (profile.id > 0) {
            dao.update(profile)
            debug("profiles", "profile stored in DB")
        } else {
            dao.insertLast(profile, null)
        }

        BackupManager.dataChanged(BuildConfig.APPLICATION_ID)

        activity?.finish()
    }

    private fun checkUrlValidity(): Boolean {
        var valid = true
        val model = getModel()

        val b = binding ?: return false

        val url = model.getUrl().trim()
        if (url.isEmpty()) {
            valid = false
            b.urlLayout.error = resources.getText(R.string.err_profile_url_empty)
        }
        try {
            val parsedUrl = URL(url)
            val host = parsedUrl.host
            if (host.isNullOrEmpty()) {
                throw MalformedURLException("Missing host")
            }
            val protocol = parsedUrl.protocol.uppercase()
            if (protocol != "HTTP" && protocol != "HTTPS") {
                valid = false
                b.urlLayout.error = resources.getText(R.string.err_invalid_url)
            }
        } catch (e: MalformedURLException) {
            valid = false
            b.urlLayout.error = resources.getText(R.string.err_invalid_url)
        }

        return valid
    }

    private fun checkInsecureSchemeWithAuth() {
        var showWarning = false
        val model = getModel()
        val b = binding ?: return

        if (model.getUseAuthentication()) {
            val urlText = model.getUrl()
            if (urlText.startsWith("http://") ||
                (urlText.length >= 8 && !urlText.startsWith("https://"))
            ) {
                showWarning = true
            }
        }

        b.insecureSchemeText.visibility = if (showWarning) View.VISIBLE else View.GONE
    }

    private fun hookClearErrorOnFocusListener(view: TextView, layout: TextInputLayout) {
        view.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                layout.error = null
            }
        }
        view.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                layout.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun syncModelFromUI() {
        if (syncingModelFromUI) return

        syncingModelFromUI = true
        try {
            val model = getModel()
            val b = binding ?: return

            model.setProfileName(b.profileName.text)
            model.setUrl(b.url.text)
            model.setPreferredAccountsFilter(b.preferredAccountsFilter.text)
            model.setAuthUserName(b.authUserName.text)
            model.setAuthPassword(b.password.text)
        } finally {
            syncingModelFromUI = false
        }
    }

    private fun checkValidity(): Boolean {
        var valid = true
        val b = binding ?: return false

        val nameVal = b.profileName.text?.toString() ?: ""
        if (nameVal.trim().isEmpty()) {
            valid = false
            b.profileNameLayout.error = resources.getText(R.string.err_profile_name_empty)
        }

        if (!checkUrlValidity()) {
            valid = false
        }

        if (b.enableHttpAuth.isChecked) {
            val userVal = b.authUserName.text?.toString() ?: ""
            if (userVal.trim().isEmpty()) {
                valid = false
                b.authUserNameLayout.error = resources.getText(R.string.err_profile_user_name_empty)
            }

            val passVal = b.password.text?.toString() ?: ""
            if (passVal.trim().isEmpty()) {
                valid = false
                b.passwordLayout.error = resources.getText(R.string.err_profile_password_empty)
            }
        }

        return valid
    }

    private fun resetDefaultCommodity() {
        defaultCommoditySet = false
        binding?.let { b ->
            b.defaultCommodityText.setText(R.string.btn_no_currency)
            b.defaultCommodityText.setTypeface(b.defaultCommodityText.typeface, Typeface.ITALIC)
        }
    }

    private fun setDefaultCommodity(name: String) {
        defaultCommoditySet = true
        binding?.let { b ->
            b.defaultCommodityText.text = name
            b.defaultCommodityText.typeface = Typeface.DEFAULT
        }
    }

    fun interface TextChangeSyncRoutine {
        fun onTextChanged(text: String)
    }

    companion object {
        const val ARG_ITEM_ID = "item_id"
        const val ARG_HUE = "hue"
    }
}
