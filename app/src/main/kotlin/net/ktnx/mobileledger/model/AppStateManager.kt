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

package net.ktnx.mobileledger.model

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParseException
import java.text.ParsePosition
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import net.ktnx.mobileledger.async.RetrieveTransactionsTask
import net.ktnx.mobileledger.db.DB
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.utils.Logger

/**
 * Application state manager for UI and app-level state.
 *
 * This object manages:
 * - Background task state (running status, progress)
 * - Drawer open/closed state
 * - Currency formatting preferences
 * - Locale settings
 * - Update status information
 *
 * ## Migration Note (008-data-layer-repository)
 *
 * Profile-related state is being migrated to ProfileRepository:
 * - `profiles` -> ProfileRepository.getAllProfiles()
 * - `profile` -> ProfileRepository.currentProfile
 * - `getProfile()` -> ProfileRepository.currentProfile.value
 * - `setCurrentProfile()` -> ProfileRepository.setCurrentProfile()
 *
 * The profile-related members are marked @Deprecated and will be removed
 * once all ViewModels are migrated to use ProfileRepository.
 */
object AppStateManager {
    @JvmField
    val backgroundTasksRunning = MutableLiveData(false)

    @JvmField
    val backgroundTaskProgress = MutableLiveData<RetrieveTransactionsTask.Progress>()

    /**
     * @deprecated Use ProfileRepository.getAllProfiles() instead.
     * This will be removed after ViewModel migration (Phase 6).
     */
    @Deprecated(
        message = "Use ProfileRepository.getAllProfiles() instead",
        replaceWith = ReplaceWith("profileRepository.getAllProfiles()")
    )
    @JvmField
    val profiles: LiveData<List<Profile>> = DB.get().getProfileDAO().getAllOrdered()

    @JvmField
    val currencySymbolPosition = MutableLiveData<Currency.Position>()

    @JvmField
    val currencyGap = MutableLiveData(true)

    @JvmField
    val locale = MutableLiveData<Locale>()

    @JvmField
    val drawerOpen = MutableLiveData(false)

    @JvmField
    val lastUpdateDate = MutableLiveData<Date?>(null)

    @JvmField
    val lastUpdateTransactionCount = MutableLiveData(0)

    @JvmField
    val lastUpdateAccountCount = MutableLiveData(0)

    @JvmField
    val lastUpdateTotalAccountCount = MutableLiveData(0)

    @JvmField
    val lastTransactionsUpdateText = MutableLiveData<String>()

    @JvmField
    val lastAccountsUpdateText = MutableLiveData<String>()

    const val decimalDot = "."

    /**
     * @deprecated Use ProfileRepository.currentProfile instead.
     * This will be removed after ViewModel migration (Phase 6).
     */
    @Deprecated(
        message = "Use ProfileRepository.currentProfile instead",
        replaceWith = ReplaceWith("profileRepository.currentProfile")
    )
    private val profile = MutableLiveData<Profile?>()

    private val backgroundTaskCount = AtomicInteger(0)
    private var numberFormatter: NumberFormat? = null
    private var decimalSeparator = ""

    init {
        locale.value = Locale.getDefault()
    }

    @JvmStatic
    fun getDecimalSeparator(): String = decimalSeparator

    /**
     * @deprecated Use ProfileRepository.currentProfile.value instead.
     * This will be removed after ViewModel migration (Phase 6).
     */
    @Deprecated(
        message = "Use ProfileRepository.currentProfile.value instead",
        replaceWith = ReplaceWith("profileRepository.currentProfile.value")
    )
    @JvmStatic
    fun getProfile(): Profile? = profile.value

    @JvmStatic
    fun backgroundTaskStarted() {
        val cnt = backgroundTaskCount.incrementAndGet()
        Logger.debug(
            "data",
            String.format(Locale.ENGLISH, "background task count is %d after incrementing", cnt)
        )
        backgroundTasksRunning.postValue(cnt > 0)
    }

    @JvmStatic
    fun backgroundTaskFinished() {
        val cnt = backgroundTaskCount.decrementAndGet()
        Logger.debug(
            "data",
            String.format(Locale.ENGLISH, "background task count is %d after decrementing", cnt)
        )
        backgroundTasksRunning.postValue(cnt > 0)
    }

    /**
     * @deprecated Use ProfileRepository.setCurrentProfile() instead.
     * This will be removed after ViewModel migration (Phase 6).
     */
    @Deprecated(
        message = "Use ProfileRepository.setCurrentProfile() instead",
        replaceWith = ReplaceWith("profileRepository.setCurrentProfile(newProfile)")
    )
    @JvmStatic
    fun setCurrentProfile(newProfile: Profile?) {
        profile.value = newProfile
    }

    /**
     * @deprecated Use ProfileRepository.setCurrentProfile() instead (with appropriate coroutine context).
     * This will be removed after ViewModel migration (Phase 6).
     */
    @Deprecated(
        message = "Use ProfileRepository.setCurrentProfile() instead",
        replaceWith = ReplaceWith("profileRepository.setCurrentProfile(newProfile)")
    )
    @JvmStatic
    fun postCurrentProfile(newProfile: Profile?) {
        profile.postValue(newProfile)
    }

    @JvmStatic
    fun refreshCurrencyData(locale: Locale) {
        val formatter = NumberFormat.getCurrencyInstance(locale)
        val currency = formatter.currency
        val symbol = currency?.symbol ?: ""
        Logger.debug(
            "locale",
            String.format(
                Locale.ROOT,
                "Discovering currency symbol position for locale %s (currency is %s; symbol is %s)",
                locale.toString(),
                currency?.toString() ?: "<none>",
                symbol
            )
        )
        val formatted = formatter.format(1234.56f)
        Logger.debug("locale", String.format(Locale.ROOT, "1234.56 formats as '%s'", formatted))

        when {
            formatted.startsWith(symbol) -> {
                currencySymbolPosition.value = Currency.Position.BEFORE
                // is the currency symbol directly followed by the first formatted digit?
                val canary = formatted[symbol.length]
                currencyGap.value = canary != '1'
            }

            formatted.endsWith(symbol) -> {
                currencySymbolPosition.value = Currency.Position.AFTER
                // is the currency symbol directly preceded by the last formatted digit?
                val canary = formatted[formatted.length - symbol.length - 1]
                currencyGap.value = canary != '6'
            }

            else -> currencySymbolPosition.value = Currency.Position.NONE
        }

        val newNumberFormatter = NumberFormat.getNumberInstance().apply {
            isParseIntegerOnly = false
            isGroupingUsed = true
            minimumIntegerDigits = 1
            minimumFractionDigits = 2
        }

        numberFormatter = newNumberFormatter

        decimalSeparator = DecimalFormatSymbols.getInstance(locale)
            .monetaryDecimalSeparator
            .toString()
    }

    @JvmStatic
    fun formatCurrency(number: Float): String {
        val formatter = NumberFormat.getCurrencyInstance(locale.value)
        return formatter.format(number)
    }

    @JvmStatic
    fun formatNumber(number: Float): String = numberFormatter?.format(number) ?: number.toString()

    /**
     * @deprecated Use ProfileRepository.currentProfile with Flow collection instead.
     * This will be removed after ViewModel migration (Phase 6).
     */
    @Deprecated(
        message = "Use ProfileRepository.currentProfile with Flow collection instead",
        replaceWith = ReplaceWith("profileRepository.currentProfile.collect { ... }")
    )
    @JvmStatic
    fun observeProfile(lifecycleOwner: LifecycleOwner, observer: Observer<Profile?>) {
        profile.observe(lifecycleOwner, observer)
    }

    /**
     * @deprecated Use structured concurrency with Flow instead.
     * This will be removed after ViewModel migration (Phase 6).
     */
    @Deprecated(
        message = "Use structured concurrency with Flow instead",
        replaceWith = ReplaceWith("// Use coroutine scope cancellation instead")
    )
    @JvmStatic
    fun removeProfileObservers(owner: LifecycleOwner) {
        profile.removeObservers(owner)
    }

    @JvmStatic
    @Throws(ParseException::class)
    fun parseNumber(str: String): Float {
        val pos = ParsePosition(0)
        val parsed = numberFormatter?.parse(str)
        if (parsed == null || pos.errorIndex > -1) {
            throw ParseException("Error parsing '$str'", pos.errorIndex)
        }

        return parsed.toFloat()
    }
}

/**
 * Type alias for backward compatibility during migration.
 * @deprecated Use AppStateManager directly.
 */
@Deprecated(
    message = "Use AppStateManager directly",
    replaceWith = ReplaceWith("AppStateManager")
)
typealias Data = AppStateManager
