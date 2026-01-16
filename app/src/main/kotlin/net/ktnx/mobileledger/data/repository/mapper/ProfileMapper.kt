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

package net.ktnx.mobileledger.data.repository.mapper

import net.ktnx.mobileledger.db.Profile as DbProfile
import net.ktnx.mobileledger.domain.model.FutureDates
import net.ktnx.mobileledger.domain.model.Profile as DomainProfile
import net.ktnx.mobileledger.domain.model.ProfileAuthentication
import net.ktnx.mobileledger.domain.model.ServerVersion

/**
 * Profile ドメインモデルとデータベースエンティティ間の変換を担当
 */
object ProfileMapper {

    /**
     * データベースエンティティからドメインモデルへ変換
     *
     * @receiver Profile (db.Profile Room Entity)
     * @return Profile (domain.model.Profile)
     */
    fun DbProfile.toDomain(): DomainProfile = DomainProfile(
        id = id,
        name = name,
        uuid = uuid,
        url = url,
        authentication = if (useAuthentication && authUser != null && authPassword != null) {
            ProfileAuthentication(user = authUser!!, password = authPassword!!)
        } else {
            null
        },
        orderNo = orderNo,
        permitPosting = permitPosting,
        theme = theme,
        preferredAccountsFilter = preferredAccountsFilter,
        futureDates = FutureDates.fromInt(futureDates),
        apiVersion = apiVersion,
        showCommodityByDefault = showCommodityByDefault,
        defaultCommodity = getDefaultCommodityOrEmpty().ifEmpty { null },
        showCommentsByDefault = showCommentsByDefault,
        serverVersion = ServerVersion(
            major = detectedVersionMajor,
            minor = detectedVersionMinor,
            isPre_1_19 = detectedVersionPre_1_19
        )
    )

    /**
     * ドメインモデルからデータベースエンティティへ変換
     *
     * @receiver Profile (domain.model.Profile)
     * @return Profile (db.Profile Room Entity)
     */
    fun DomainProfile.toEntity(): DbProfile = DbProfile().apply {
        id = this@toEntity.id ?: 0
        name = this@toEntity.name
        uuid = this@toEntity.uuid
        url = this@toEntity.url
        useAuthentication = this@toEntity.authentication != null
        authUser = this@toEntity.authentication?.user
        authPassword = this@toEntity.authentication?.password
        orderNo = this@toEntity.orderNo
        permitPosting = this@toEntity.permitPosting
        theme = this@toEntity.theme
        preferredAccountsFilter = this@toEntity.preferredAccountsFilter
        futureDates = this@toEntity.futureDates.toInt()
        apiVersion = this@toEntity.apiVersion
        showCommodityByDefault = this@toEntity.showCommodityByDefault
        setDefaultCommodity(this@toEntity.defaultCommodity)
        showCommentsByDefault = this@toEntity.showCommentsByDefault
        detectedVersionPre_1_19 = this@toEntity.serverVersion?.isPre_1_19 ?: false
        detectedVersionMajor = this@toEntity.serverVersion?.major ?: 0
        detectedVersionMinor = this@toEntity.serverVersion?.minor ?: 0
    }
}
