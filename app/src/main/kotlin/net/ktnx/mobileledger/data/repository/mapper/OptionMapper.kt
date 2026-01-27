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

import net.ktnx.mobileledger.core.database.entity.Option as DbOption
import net.ktnx.mobileledger.core.domain.model.AppOption

/**
 * AppOption ドメインモデルとデータベースエンティティ間の変換を担当
 */
object OptionMapper {

    /**
     * データベースエンティティからドメインモデルへ変換
     *
     * @receiver Option (db.Option Room Entity)
     * @return AppOption (domain.model.AppOption)
     */
    fun DbOption.toDomain(): AppOption = AppOption(
        profileId = profileId,
        name = name,
        value = value
    )

    /**
     * ドメインモデルからデータベースエンティティへ変換
     *
     * @receiver AppOption (domain.model.AppOption)
     * @return Option (db.Option Room Entity)
     */
    fun AppOption.toEntity(): DbOption = DbOption(
        profileId = profileId,
        name = name,
        value = value
    )
}
