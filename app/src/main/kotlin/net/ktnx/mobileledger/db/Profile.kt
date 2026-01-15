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

package net.ktnx.mobileledger.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Transaction
import java.util.UUID
import net.ktnx.mobileledger.utils.Misc

@Entity(
    tableName = "profiles",
    indices = [Index(name = "profiles_uuid_idx", unique = true, value = ["uuid"])]
)
class Profile {
    @ColumnInfo
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo
    var name: String = ""

    @ColumnInfo
    var uuid: String = UUID.randomUUID().toString()

    @ColumnInfo
    var url: String = ""

    @ColumnInfo(name = "use_authentication")
    var useAuthentication: Boolean = false

    @ColumnInfo(name = "auth_user")
    var authUser: String? = null

    @ColumnInfo(name = "auth_password")
    var authPassword: String? = null

    @ColumnInfo(name = "order_no")
    var orderNo: Int = 0

    @ColumnInfo(name = "permit_posting")
    var permitPosting: Boolean = false

    @ColumnInfo(defaultValue = "-1")
    var theme: Int = -1

    @ColumnInfo(name = "preferred_accounts_filter")
    var preferredAccountsFilter: String? = null

    @ColumnInfo(name = "future_dates")
    var futureDates: Int = 0

    @ColumnInfo(name = "api_version")
    var apiVersion: Int = 0

    @ColumnInfo(name = "show_commodity_by_default")
    var showCommodityByDefault: Boolean = false

    @ColumnInfo(name = "default_commodity")
    var defaultCommodity: String? = null
        private set

    fun getDefaultCommodityOrEmpty(): String = defaultCommodity ?: ""

    fun setDefaultCommodity(value: String?) {
        defaultCommodity = Misc.nullIsEmpty(value)
    }

    @ColumnInfo(name = "show_comments_by_default", defaultValue = "1")
    var showCommentsByDefault: Boolean = true

    @ColumnInfo(name = "detected_version_pre_1_19")
    var detectedVersionPre_1_19: Boolean = false

    @ColumnInfo(name = "detected_version_major")
    var detectedVersionMajor: Int = 0

    @ColumnInfo(name = "detected_version_minor")
    var detectedVersionMinor: Int = 0

    fun isAuthEnabled(): Boolean = useAuthentication

    fun canPost(): Boolean = permitPosting

    fun isVersionPre_1_19(): Boolean = detectedVersionPre_1_19

    override fun toString(): String = name

    override fun equals(other: Any?): Boolean {
        if (other !is Profile) return false
        return id == other.id &&
            Misc.equalStrings(name, other.name) &&
            Misc.equalStrings(uuid, other.uuid) &&
            Misc.equalStrings(url, other.url) &&
            useAuthentication == other.useAuthentication &&
            Misc.equalStrings(authUser, other.authUser) &&
            Misc.equalStrings(authPassword, other.authPassword) &&
            orderNo == other.orderNo &&
            permitPosting == other.permitPosting &&
            theme == other.theme &&
            Misc.equalStrings(preferredAccountsFilter, other.preferredAccountsFilter) &&
            futureDates == other.futureDates &&
            apiVersion == other.apiVersion &&
            showCommentsByDefault == other.showCommentsByDefault &&
            Misc.equalStrings(defaultCommodity, other.defaultCommodity) &&
            showCommentsByDefault == other.showCommentsByDefault &&
            detectedVersionPre_1_19 == other.detectedVersionPre_1_19 &&
            detectedVersionMajor == other.detectedVersionMajor &&
            detectedVersionMinor == other.detectedVersionMinor
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + uuid.hashCode()
        return result
    }

    /**
     * @deprecated Use ProfileRepository.deleteProfileData() instead.
     * This method violates the Repository pattern by accessing the database directly.
     */
    @Deprecated("Use ProfileRepository.deleteProfileData() instead", ReplaceWith(""))
    @Transaction
    fun wipeAllDataSync() {
        val optDao = DB.get().getOptionDAO()
        optDao.deleteSync(optDao.allForProfileSync(id))

        val accDao = DB.get().getAccountDAO()
        accDao.deleteSync(accDao.allForProfileSync(id))

        val trnDao = DB.get().getTransactionDAO()
        trnDao.deleteSync(trnDao.getAllForProfileUnorderedSync(id))
    }

    companion object {
        const val NO_PROFILE_ID: Long = 0
    }
}
