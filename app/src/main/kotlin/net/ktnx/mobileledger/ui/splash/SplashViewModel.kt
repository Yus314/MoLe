/*
 * Copyright © 2026 Damyan Ivanov.
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

package net.ktnx.mobileledger.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import net.ktnx.mobileledger.domain.usecase.DatabaseInitializer

/**
 * SplashScreen のための ViewModel
 *
 * DatabaseInitializer を使用してデータベース初期化を行う。
 * 最小表示時間と初期化完了の両方が満たされた時点で
 * メイン画面への遷移を通知する。
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val databaseInitializer: DatabaseInitializer
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SplashEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        startInitialization()
        startMinDisplayTimer()
    }

    private fun startInitialization() {
        viewModelScope.launch {
            logcat { "Starting database initialization" }

            databaseInitializer.initialize()
                .onSuccess { hasProfiles ->
                    logcat { "Database initialized. hasProfiles=$hasProfiles" }
                    _uiState.update { it.copy(isInitialized = true) }
                    checkNavigation()
                }
                .onFailure { error ->
                    // 初期化失敗でも続行（エラーは通常発生しない）
                    logcat(LogPriority.WARN) { "Database initialization failed: ${error.asLog()}" }
                    _uiState.update { it.copy(isInitialized = true) }
                    checkNavigation()
                }
        }
    }

    private fun startMinDisplayTimer() {
        viewModelScope.launch {
            delay(KEEP_ACTIVE_FOR_MS)
            _uiState.update { it.copy(minDisplayTimeElapsed = true) }
            checkNavigation()
        }
    }

    private fun checkNavigation() {
        val state = _uiState.value
        if (state.canNavigate) {
            viewModelScope.launch {
                logcat { "Ready to navigate to main activity" }
                _effects.send(SplashEffect.NavigateToMain)
            }
        }
    }

    companion object {
        private const val KEEP_ACTIVE_FOR_MS = 400L
    }
}
