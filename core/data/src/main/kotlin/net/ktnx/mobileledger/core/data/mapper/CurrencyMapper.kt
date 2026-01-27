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

package net.ktnx.mobileledger.core.data.mapper

import net.ktnx.mobileledger.core.database.entity.Currency as DbCurrency
import net.ktnx.mobileledger.core.domain.model.Currency as DomainCurrency
import net.ktnx.mobileledger.core.domain.model.CurrencyPosition

/**
 * Currency ドメインモデルとデータベースエンティティ間の変換を担当
 */
object CurrencyMapper {

    /**
     * データベースエンティティからドメインモデルへ変換
     *
     * @receiver Currency (db.Currency Room Entity)
     * @return Currency (domain.model.Currency)
     */
    fun DbCurrency.toDomain(): DomainCurrency = DomainCurrency(
        id = id,
        name = name,
        position = CurrencyPosition.fromString(position),
        hasGap = hasGap
    )

    /**
     * ドメインモデルからデータベースエンティティへ変換
     *
     * @receiver Currency (domain.model.Currency)
     * @return Currency (db.Currency Room Entity)
     */
    fun DomainCurrency.toEntity(): DbCurrency = DbCurrency(
        id = id ?: 0L,
        name = name,
        position = position.toDbString(),
        hasGap = hasGap
    )
}
