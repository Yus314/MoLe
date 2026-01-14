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

package net.ktnx.mobileledger.ui.backups

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.domain.usecase.ConfigBackup

@HiltViewModel
class BackupsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val configBackup: ConfigBackup
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupsUiState())
    val uiState: StateFlow<BackupsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<BackupsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        // Initial backup enabled state based on current profile
        _uiState.update { it.copy(backupEnabled = profileRepository.currentProfile.value != null) }
    }

    fun onEvent(event: BackupsEvent) {
        when (event) {
            BackupsEvent.BackupClicked -> handleBackupClicked()
            BackupsEvent.RestoreClicked -> handleRestoreClicked()
            is BackupsEvent.BackupUriSelected -> { /* Handled by performBackup */ }
            is BackupsEvent.RestoreUriSelected -> { /* Handled by performRestore */ }
            BackupsEvent.MessageShown -> { /* Message cleared by UI */ }
        }
    }

    private fun handleBackupClicked() {
        val now = Date()
        val df = SimpleDateFormat("y-MM-dd HH:mm", Locale.getDefault())
        val suggestedFileName = String.format(Locale.ROOT, "MoLe-%s.json", df.format(now))

        viewModelScope.launch {
            _effects.send(BackupsEffect.LaunchBackupFilePicker(suggestedFileName))
        }
    }

    private fun handleRestoreClicked() {
        viewModelScope.launch {
            _effects.send(BackupsEffect.LaunchRestoreFilePicker)
        }
    }

    /**
     * Performs the backup operation using ConfigBackup interface.
     * @param uri Target URI for the backup file
     */
    fun performBackup(uri: Uri) {
        _uiState.update { it.copy(isBackingUp = true) }

        viewModelScope.launch {
            configBackup.backup(uri)
                .onSuccess {
                    _uiState.update { it.copy(isBackingUp = false) }
                    _effects.send(
                        BackupsEffect.ShowSnackbar(
                            BackupsMessage.Success(R.string.config_saved)
                        )
                    )
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isBackingUp = false) }
                    _effects.send(
                        BackupsEffect.ShowSnackbar(
                            BackupsMessage.Error(e.toString())
                        )
                    )
                }
        }
    }

    /**
     * Performs the restore operation using ConfigBackup interface.
     * @param uri Source URI of the backup file
     */
    fun performRestore(uri: Uri) {
        _uiState.update { it.copy(isRestoring = true) }

        viewModelScope.launch {
            configBackup.restore(uri)
                .onSuccess {
                    _uiState.update { it.copy(isRestoring = false) }
                    _effects.send(
                        BackupsEffect.ShowSnackbar(
                            BackupsMessage.Success(R.string.config_restored)
                        )
                    )
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isRestoring = false) }
                    _effects.send(
                        BackupsEffect.ShowSnackbar(
                            BackupsMessage.Error(e.toString())
                        )
                    )
                }
        }
    }

    /**
     * Update backup enabled state based on profile availability
     */
    fun updateBackupEnabled(hasProfile: Boolean) {
        _uiState.update { it.copy(backupEnabled = hasProfile) }
    }
}
