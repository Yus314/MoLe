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
import android.view.MenuItem
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.databinding.ActivityTemplatesBinding
import net.ktnx.mobileledger.db.DB
import net.ktnx.mobileledger.db.TemplateWithAccounts
import net.ktnx.mobileledger.ui.FabManager
import net.ktnx.mobileledger.ui.QR
import net.ktnx.mobileledger.ui.activity.CrashReportingActivity
import net.ktnx.mobileledger.utils.Logger

class TemplatesActivity :
    CrashReportingActivity(),
    TemplateListFragment.OnTemplateListFragmentInteractionListener,
    TemplateDetailsFragment.InteractionListener,
    QR.QRScanResultReceiver,
    QR.QRScanTrigger,
    FabManager.FabHandler {

    private lateinit var b: ActivityTemplatesBinding
    private lateinit var navController: NavController
    private lateinit var qrScanLauncher: ActivityResultLauncher<Void?>
    private lateinit var fabManager: FabManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTemplatesBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)

        // Show the Up button in the action bar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.templateListFragment -> {
                    b.toolbar.title = getString(R.string.title_activity_templates)
                    b.fab.setImageResource(R.drawable.ic_add_white_24dp)
                }

                else -> {
                    b.fab.setImageResource(R.drawable.ic_save_white_24dp)
                }
            }
        }

        b.toolbar.title = getString(R.string.title_activity_templates)

        b.fab.setOnClickListener {
            val currentDestination = navController.currentDestination
            if (currentDestination?.id == R.id.templateListFragment) {
                onEditTemplate(null)
            } else {
                onSaveTemplate()
            }
        }

        qrScanLauncher = QR.registerLauncher(this, this)

        fabManager = FabManager(b.fab)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            val currentDestination = navController.currentDestination
            if (currentDestination != null &&
                currentDestination.id == R.id.templateDetailsFragment
            ) {
                navController.popBackStack()
            } else {
                finish()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDuplicateTemplate(id: Long) {
        DB.get()
            .getTemplateDAO()
            .duplicateTemplateWithAccounts(id, null)
    }

    override fun onEditTemplate(id: Long?) {
        if (id == null) {
            navController.navigate(R.id.action_templateListFragment_to_templateDetailsFragment)
            b.toolbar.title = getString(R.string.title_new_template)
        } else {
            val bundle = Bundle().apply {
                putLong(TemplateDetailsFragment.ARG_TEMPLATE_ID, id)
            }
            navController.navigate(
                R.id.action_templateListFragment_to_templateDetailsFragment,
                bundle
            )
            b.toolbar.title = getString(R.string.title_edit_template)
        }
    }

    override fun onSaveTemplate() {
        val viewModelStoreOwner = navController.getViewModelStoreOwner(R.id.template_list_navigation)
        val model = ViewModelProvider(viewModelStoreOwner)[TemplateDetailsViewModel::class.java]
        Logger.debug("flow", "TemplatesActivity.onSavePattern(): model=$model")
        model.onSaveTemplate()
        navController.navigateUp()
    }

    fun getNavController(): NavController = navController

    override fun onDeleteTemplate(templateId: Long) {
        val dao = DB.get().getTemplateDAO()

        dao.getTemplateWithAccountsAsync(templateId) { template ->
            val copy = TemplateWithAccounts.from(template)
            dao.deleteAsync(template.header) {
                navController.popBackStack(R.id.templateListFragment, false)

                Snackbar.make(
                    b.root,
                    String.format(
                        getString(R.string.template_xxx_deleted),
                        template.header.name
                    ),
                    BaseTransientBottomBar.LENGTH_LONG
                )
                    .setAction(R.string.action_undo) { dao.insertAsync(copy, null) }
                    .show()
            }
        }
    }

    override fun onQRScanResult(scanned: String?) {
        Logger.debug("PatDet_fr", String.format("Got scanned text '%s'", scanned))
        val model = ViewModelProvider(
            navController.getViewModelStoreOwner(R.id.template_list_navigation)
        )[TemplateDetailsViewModel::class.java]
        model.setTestText(scanned ?: "")
    }

    override fun triggerQRScan() {
        qrScanLauncher.launch(null)
    }

    override fun getContext(): Context = this

    override fun showManagedFab() {
        fabManager.showFab()
    }

    override fun hideManagedFab() {
        fabManager.hideFab()
    }

    companion object {
        const val ARG_ADD_TEMPLATE = "add-template"
    }
}
