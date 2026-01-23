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

package net.ktnx.mobileledger.backup.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Root backup model for MoLe configuration.
 *
 * Represents the complete structure of a backup file.
 */
@Serializable
data class BackupModel(
    val commodities: List<CurrencyBackupModel>? = null,
    val profiles: List<ProfileBackupModel>? = null,
    val templates: List<TemplateBackupModel>? = null,
    val currentProfile: String? = null
)

/**
 * Currency/commodity backup model.
 */
@Serializable
data class CurrencyBackupModel(
    val name: String,
    val position: String? = null,
    val hasGap: Boolean? = null
)

/**
 * Profile backup model.
 */
@Serializable
data class ProfileBackupModel(
    val uuid: String,
    val name: String,
    val url: String,
    val useAuth: Boolean = false,
    val authUser: String? = null,
    val authPass: String? = null,
    val apiVersion: Int? = null,
    val permitPosting: Boolean = true,
    val defaultCommodity: String? = null,
    val showCommodityByDefault: Boolean? = null,
    val showCommentsByDefault: Boolean? = null,
    val futureDates: Int? = null,
    val preferredAccountsFilter: String? = null,
    val colour: Int = 0
)

/**
 * Template backup model.
 */
@Serializable
data class TemplateBackupModel(
    val uuid: String,
    val name: String,
    val regex: String? = null,
    val testText: String? = null,
    val dateYear: Int? = null,
    val dateYearMatchGroup: Int? = null,
    val dateMonth: Int? = null,
    val dateMonthMatchGroup: Int? = null,
    val dateDay: Int? = null,
    val dateDayMatchGroup: Int? = null,
    @SerialName("description")
    val transactionDescription: String? = null,
    @SerialName("descriptionMatchGroup")
    val transactionDescriptionMatchGroup: Int? = null,
    val comment: String? = null,
    val commentMatchGroup: Int? = null,
    val isFallback: Boolean = false,
    val accounts: List<TemplateAccountBackupModel>? = null
)

/**
 * Template account backup model.
 */
@Serializable
data class TemplateAccountBackupModel(
    val name: String? = null,
    val nameMatchGroup: Int? = null,
    val comment: String? = null,
    val commentMatchGroup: Int? = null,
    val amount: Float? = null,
    val amountGroup: Int? = null,
    val negateAmount: Boolean? = null,
    @SerialName("commodity")
    val currency: Long? = null,
    @SerialName("commodityGroup")
    val currencyGroup: Int? = null
)
