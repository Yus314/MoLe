/*
 * Copyright © 2022 Damyan Ivanov.
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

package net.ktnx.mobileledger

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.ui.backups.BackupsEffect
import net.ktnx.mobileledger.ui.backups.BackupsMessage
import net.ktnx.mobileledger.ui.backups.BackupsScreen
import net.ktnx.mobileledger.ui.backups.BackupsViewModel
import net.ktnx.mobileledger.ui.theme.MoLeTheme

@AndroidEntryPoint
class BackupsActivity : ComponentActivity() {
    @Inject
    lateinit var profileRepository: ProfileRepository

    private val viewModel: BackupsViewModel by viewModels()
    private lateinit var backupChooserLauncher: ActivityResultLauncher<String>
    private lateinit var restoreChooserLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register file pickers
        backupChooserLauncher = registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            uri?.let { viewModel.performBackup(baseContext, it) }
        }

        restoreChooserLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let { viewModel.performRestore(baseContext, it) }
        }

        // Observe profile changes for backup button state
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileRepository.currentProfile.collect { profile ->
                    viewModel.updateBackupEnabled(profile != null)
                }
            }
        }

        setContent {
            // Collect current profile for theme updates
            val currentProfile by profileRepository.currentProfile.collectAsState()
            val currentTheme = currentProfile?.theme ?: -1

            MoLeTheme(
                profileHue = if (currentTheme >= 0) currentTheme.toFloat() else null
            ) {
                val uiState by viewModel.uiState.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                // Handle effects
                LaunchedEffect(Unit) {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            is BackupsEffect.LaunchBackupFilePicker -> {
                                backupChooserLauncher.launch(effect.suggestedFileName)
                            }

                            is BackupsEffect.LaunchRestoreFilePicker -> {
                                restoreChooserLauncher.launch(arrayOf("application/json"))
                            }

                            is BackupsEffect.ShowSnackbar -> {
                                val message = when (val msg = effect.message) {
                                    is BackupsMessage.Success -> getString(msg.messageResId)
                                    is BackupsMessage.Error -> msg.message
                                }
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    }
                }

                BackupsScreen(
                    uiState = uiState,
                    snackbarHostState = snackbarHostState,
                    onEvent = viewModel::onEvent,
                    onNavigateBack = { finish() }
                )
            }
        }
    }

    companion object {
        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, BackupsActivity::class.java)
            context.startActivity(starter)
        }
    }
}
