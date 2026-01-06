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
import android.content.Intent
import android.database.AbstractCursor
import android.os.Bundle
import android.os.ParcelFormatException
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.MenuCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.ktnx.mobileledger.BuildConfig
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.async.DescriptionSelectedCallback
import net.ktnx.mobileledger.async.GeneralBackgroundTasks
import net.ktnx.mobileledger.async.SendTransactionTask
import net.ktnx.mobileledger.async.TaskCallback
import net.ktnx.mobileledger.dao.BaseDAO
import net.ktnx.mobileledger.databinding.ActivityNewTransactionBinding
import net.ktnx.mobileledger.db.DB
import net.ktnx.mobileledger.db.TemplateHeader
import net.ktnx.mobileledger.model.Data
import net.ktnx.mobileledger.model.LedgerTransaction
import net.ktnx.mobileledger.model.MatchedTemplate
import net.ktnx.mobileledger.ui.FabManager
import net.ktnx.mobileledger.ui.QR
import net.ktnx.mobileledger.ui.activity.ProfileThemedActivity
import net.ktnx.mobileledger.ui.activity.SplashActivity
import net.ktnx.mobileledger.ui.templates.TemplatesActivity
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.Misc
import java.util.regex.Pattern

class NewTransactionActivity : ProfileThemedActivity(),
    TaskCallback,
    NewTransactionFragment.OnNewTransactionFragmentInteractionListener,
    QR.QRScanTrigger,
    QR.QRScanResultReceiver,
    DescriptionSelectedCallback,
    FabManager.FabHandler {

    internal val TAG = "new-t-a"
    private lateinit var navController: NavController
    private lateinit var model: NewTransactionModel
    private lateinit var qrScanLauncher: ActivityResultLauncher<Void?>
    private lateinit var b: ActivityNewTransactionBinding
    private lateinit var fabManager: FabManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityNewTransactionBinding.inflate(layoutInflater, null, false)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)
        Data.observeProfile(this) { profile ->
            if (profile == null) {
                Logger.debug("new-t-act", "no active profile. Redirecting to SplashActivity")
                val intent = Intent(this, SplashActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_TASK_ON_HOME or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            } else {
                b.toolbar.subtitle = profile.name
            }
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.new_transaction_nav) as NavHostFragment
        navController = navHostFragment.navController

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        model = ViewModelProvider(this)[NewTransactionModel::class.java]

        qrScanLauncher = QR.registerLauncher(this, this)

        fabManager = FabManager(b.fabAdd)

        model.isSubmittable()
            .observe(this) { isSubmittable ->
                if (isSubmittable) {
                    fabManager.showFab()
                } else {
                    fabManager.hideFab()
                }
            }

        b.fabAdd.setOnClickListener { onFabPressed() }
    }

    override fun initProfile() {
        val profileId = intent.getLongExtra(PARAM_PROFILE_ID, 0)
        val profileHue = intent.getIntExtra(PARAM_THEME, -1)

        if (profileHue < 0) {
            Logger.debug(TAG, "Started with invalid/missing theme; quitting")
            finish()
            return
        }

        if (profileId <= 0) {
            Logger.debug(TAG, "Started with invalid/missing profile_id; quitting")
            finish()
            return
        }

        setupProfileColors(profileHue)
        initProfile(profileId)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.dummy, R.anim.slide_out_down)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onTransactionSave(tr: LedgerTransaction) {
        navController.navigate(R.id.action_newTransactionFragment_to_newTransactionSavingFragment)
        try {
            val profile = mProfile ?: return
            val saver = SendTransactionTask(this, profile, tr, model.simulateSaveFlag)
            saver.start()
        } catch (e: Exception) {
            Logger.debug("new-transaction", "Unknown error: $e")

            val bundle = Bundle()
            bundle.putString("error", "unknown error")
            navController.navigate(R.id.newTransactionFragment, bundle)
        }
    }

    internal fun onSimulateCrashMenuItemClicked(item: MenuItem): Boolean {
        Logger.debug("crash", "Will crash intentionally")
        GeneralBackgroundTasks.run { throw RuntimeException("Simulated crash") }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        if (!BuildConfig.DEBUG) {
            return true
        }

        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.new_transaction, menu)

        MenuCompat.setGroupDividerEnabled(menu, true)

        menu.findItem(R.id.action_simulate_save)
            .setOnMenuItemClickListener { item -> onToggleSimulateSaveMenuItemClicked(item) }
        menu.findItem(R.id.action_simulate_crash)
            .setOnMenuItemClickListener { item -> onSimulateCrashMenuItemClicked(item) }

        model.getSimulateSave()
            .observe(this) { state ->
                menu.findItem(R.id.action_simulate_save).isChecked = state
                b.simulationLabel.visibility = if (state) View.VISIBLE else View.GONE
            }

        return true
    }

    fun dp2px(dp: Float): Int {
        return Math.round(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                resources.displayMetrics
            )
        )
    }

    override fun onTransactionSaveDone(error: String?, arg: Any?) {
        val bundle = Bundle()
        if (error != null) {
            bundle.putString("error", error)
            navController.navigate(R.id.action_newTransactionSavingFragment_Failure, bundle)
        } else {
            navController.navigate(R.id.action_newTransactionSavingFragment_Success, bundle)
            BaseDAO.runAsync { commitToDb(arg as LedgerTransaction) }
        }
    }

    fun commitToDb(tr: LedgerTransaction) {
        val dbTransaction = tr.toDBO()
        DB.get()
            .getTransactionDAO()
            .appendSync(dbTransaction)
    }

    internal fun onToggleSimulateSaveMenuItemClicked(item: MenuItem): Boolean {
        model.toggleSimulateSave()
        return true
    }

    override fun triggerQRScan() {
        qrScanLauncher.launch(null)
    }

    private fun startNewPatternActivity(scanned: String) {
        val intent = Intent(this, TemplatesActivity::class.java)
        val args = Bundle()
        args.putString(TemplatesActivity.ARG_ADD_TEMPLATE, scanned)
        startActivity(intent, args)
    }

    private fun alertNoTemplateMatch(scanned: String) {
        MaterialAlertDialogBuilder(this)
            .setCancelable(true)
            .setMessage(R.string.no_template_matches)
            .setPositiveButton(R.string.add_button) { _, _ -> startNewPatternActivity(scanned) }
            .create()
            .show()
    }

    override fun onQRScanResult(text: String?) {
        Logger.debug("qr", String.format("Got QR scan result [%s]", text))

        if (Misc.emptyIsNull(text) == null) {
            return
        }

        val allTemplates = DB.get()
            .getTemplateDAO()
            .getTemplates()
        allTemplates.observe(this) { templateHeaders: List<TemplateHeader> ->
            val matchingFallbackTemplates = ArrayList<MatchedTemplate>()
            val matchingTemplates = ArrayList<MatchedTemplate>()

            for (ph: TemplateHeader in templateHeaders) {
                val patternSource = ph.regularExpression
                if (Misc.emptyIsNull(patternSource) == null) {
                    continue
                }
                try {
                    val pattern = Pattern.compile(patternSource)
                    val matcherText = text ?: continue
                    val matcher = pattern.matcher(matcherText)
                    if (!matcher.matches()) {
                        continue
                    }

                    Logger.debug(
                        "pattern",
                        String.format(
                            "Pattern '%s' [%s] matches '%s'", ph.name,
                            patternSource, text
                        )
                    )
                    if (ph.isFallback) {
                        matchingFallbackTemplates.add(
                            MatchedTemplate(ph, matcher.toMatchResult())
                        )
                    } else {
                        matchingTemplates.add(MatchedTemplate(ph, matcher.toMatchResult()))
                    }
                } catch (e: ParcelFormatException) {
                    // ignored
                    Logger.debug(
                        "pattern",
                        String.format("Error compiling regular expression '%s'", patternSource),
                        e
                    )
                }
            }

            val templatesToUse = if (matchingTemplates.isEmpty()) {
                matchingFallbackTemplates
            } else {
                matchingTemplates
            }

            val textToMatch = text ?: return@observe
            when {
                templatesToUse.isEmpty() -> alertNoTemplateMatch(textToMatch)
                templatesToUse.size == 1 -> model.applyTemplate(templatesToUse[0], textToMatch)
                else -> chooseTemplate(templatesToUse, textToMatch)
            }
        }
    }

    private fun chooseTemplate(matchingTemplates: ArrayList<MatchedTemplate>, matchedText: String) {
        val templateNameColumn = "name"
        val cursor = object : AbstractCursor() {
            override fun getCount(): Int = matchingTemplates.size

            override fun getColumnNames(): Array<String> = arrayOf("_id", templateNameColumn)

            override fun getString(column: Int): String {
                return if (column == 0) {
                    position.toString()
                } else {
                    matchingTemplates[position].templateHead.name ?: ""
                }
            }

            override fun getShort(column: Int): Short {
                return if (column == 0) {
                    position.toShort()
                } else {
                    -1
                }
            }

            override fun getInt(column: Int): Int = getShort(column).toInt()

            override fun getLong(column: Int): Long = getShort(column).toLong()

            override fun getFloat(column: Int): Float = getShort(column).toFloat()

            override fun getDouble(column: Int): Double = getShort(column).toDouble()

            override fun isNull(column: Int): Boolean = false

            override fun getColumnCount(): Int = 2
        }

        MaterialAlertDialogBuilder(this)
            .setCancelable(true)
            .setTitle(R.string.choose_template_to_apply)
            .setIcon(R.drawable.ic_baseline_auto_graph_24)
            .setSingleChoiceItems(cursor, -1, templateNameColumn) { dialog, which ->
                model.applyTemplate(matchingTemplates[which], matchedText)
                dialog.dismiss()
            }
            .create()
            .show()
    }

    override fun onDescriptionSelected(description: String) {
        Logger.debug("description selected", description)
        if (!model.accountListIsEmpty()) {
            return
        }

        BaseDAO.runAsync {
            val accFilter = mProfile?.preferredAccountsFilter

            val trDao = DB.get()
                .getTransactionDAO()

            val filterValue = Misc.emptyIsNull(accFilter)
            var tr = if (filterValue != null) {
                trDao.getFirstByDescriptionHavingAccountSync(description, filterValue)
            } else {
                null
            }

            if (tr == null) {
                tr = trDao.getFirstByDescriptionSync(description)
            }

            val transactionToLoad = tr
            if (transactionToLoad != null) {
                model.loadTransactionIntoModel(transactionToLoad)
            }
        }
    }

    private fun onFabPressed() {
        fabManager.hideFab()
        Misc.hideSoftKeyboard(this)

        val tr = model.constructLedgerTransaction()

        onTransactionSave(tr)
    }

    override fun getContext(): Context {
        return this
    }

    override fun showManagedFab() {
        if (model.isSubmittable().value == true) {
            fabManager.showFab()
        }
    }

    override fun hideManagedFab() {
        fabManager.hideFab()
    }
}
