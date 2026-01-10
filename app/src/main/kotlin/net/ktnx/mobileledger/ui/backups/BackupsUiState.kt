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

/**
 * UI state for the backups screen
 */
data class BackupsUiState(
    /** Backup operation in progress flag */
    val isBackingUp: Boolean = false,
    /** Restore operation in progress flag */
    val isRestoring: Boolean = false,
    /** Backup button enabled flag (enabled only when a profile is selected) */
    val backupEnabled: Boolean = false
)

/**
 * Message types for the backups screen (for Snackbar display)
 */
sealed class BackupsMessage {
    data class Success(val messageResId: Int) : BackupsMessage()
    data class Error(val message: String) : BackupsMessage()
}

/**
 * Events from UI to ViewModel
 */
sealed class BackupsEvent {
    /** Backup button clicked */
    data object BackupClicked : BackupsEvent()

    /** Restore button clicked */
    data object RestoreClicked : BackupsEvent()

    /** User selected a URI for backup file */
    data class BackupUriSelected(val uri: Uri) : BackupsEvent()

    /** User selected a URI for restore file */
    data class RestoreUriSelected(val uri: Uri) : BackupsEvent()

    /** Snackbar message was shown and should be cleared */
    data object MessageShown : BackupsEvent()
}

/**
 * One-shot effects from ViewModel to UI
 */
sealed class BackupsEffect {
    /** Launch the file picker for creating a backup file */
    data class LaunchBackupFilePicker(val suggestedFileName: String) : BackupsEffect()

    /** Launch the file picker for selecting a restore file */
    data object LaunchRestoreFilePicker : BackupsEffect()

    /** Show a snackbar message */
    data class ShowSnackbar(val message: BackupsMessage) : BackupsEffect()
}
