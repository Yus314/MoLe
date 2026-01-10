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
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import net.ktnx.mobileledger.ui.activity.CrashReportingActivity
import net.ktnx.mobileledger.ui.components.CrashReportDialog
import net.ktnx.mobileledger.ui.theme.MoLeTheme

/**
 * Activity for managing templates using Jetpack Compose UI.
 * Hosts the TemplatesNavHost which provides navigation between list and detail screens.
 */
@AndroidEntryPoint
class TemplatesActivity : CrashReportingActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MoLeTheme {
                TemplatesNavHost(
                    onNavigateBack = { finish() }
                )

                crashReportText?.let { text ->
                    CrashReportDialog(
                        crashReportText = text,
                        onDismiss = { dismissCrashReport() }
                    )
                }
            }
        }
    }

    companion object {
        const val ARG_ADD_TEMPLATE = "add-template"
    }
}
