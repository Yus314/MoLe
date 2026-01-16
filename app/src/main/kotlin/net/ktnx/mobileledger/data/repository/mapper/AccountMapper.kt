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

import net.ktnx.mobileledger.db.Account as DbAccount
import net.ktnx.mobileledger.db.AccountValue
import net.ktnx.mobileledger.db.AccountWithAmounts
import net.ktnx.mobileledger.domain.model.Account
import net.ktnx.mobileledger.domain.model.AccountAmount

/**
 * Account ドメインモデルとデータベースエンティティ間の変換を担当
 */
object AccountMapper {

    /**
     * データベースエンティティからドメインモデルへ変換
     *
     * @receiver AccountWithAmounts (Room Relation)
     * @return Account ドメインモデル
     *
     * Mapping rules:
     * - id: account.id
     * - name: account.name
     * - level: account.level
     * - isExpanded: account.expanded
     * - isVisible: true (default, UI state)
     * - amounts: amounts.map { AccountAmount(it.currency, it.value) }
     *
     * Hidden fields:
     * - profileId
     * - amountsExpanded
     * - nameUpper
     * - generation
     */
    fun AccountWithAmounts.toDomain(): Account = Account(
        id = account.id,
        name = account.name,
        level = account.level,
        isExpanded = account.expanded,
        isVisible = true,
        amounts = amounts.map { it.toDomain() }
    )

    /**
     * AccountValue からドメインモデルへの変換
     */
    fun AccountValue.toDomain(): AccountAmount = AccountAmount(
        currency = currency,
        amount = value
    )

    /**
     * ドメインモデルからデータベースエンティティへ変換
     *
     * @receiver Account ドメインモデル
     * @param profileId 所属プロファイルID
     * @return AccountWithAmounts (Room Relation)
     */
    fun Account.toEntity(profileId: Long): AccountWithAmounts {
        val dbAccount = DbAccount().apply {
            this.id = this@toEntity.id ?: 0L
            this.profileId = profileId
            this.name = this@toEntity.name
            this.nameUpper = this@toEntity.name.uppercase()
            this.parentName = this@toEntity.parentName
            this.level = this@toEntity.level
            this.expanded = this@toEntity.isExpanded
            this.amountsExpanded = false
        }

        val dbAmounts = amounts.map { amount ->
            AccountValue().apply {
                this.currency = amount.currency
                this.value = amount.amount
            }
        }

        return AccountWithAmounts().apply {
            account = dbAccount
            amounts = dbAmounts
        }
    }

    /**
     * 既存アカウントのUI状態（展開状態等）を保持した新しいAccountを返す
     *
     * 同期処理で使用。既存のアカウントが存在する場合、そのIDと展開状態を引き継ぐ。
     *
     * @param existing 既存のアカウント。null の場合は元のAccountをそのまま返す
     * @return 既存のUI状態を反映した Account
     */
    fun Account.withStateFrom(existing: Account?): Account = if (existing != null) {
        copy(
            id = existing.id,
            isExpanded = existing.isExpanded
        )
    } else {
        this
    }
}
