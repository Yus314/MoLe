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

package net.ktnx.mobileledger.domain.model

/**
 * 取引送信のUI状態
 */
sealed class SendState {
    /**
     * アイドル状態
     */
    data object Idle : SendState()

    /**
     * 送信中
     */
    data class Sending(
        val message: String = "送信中..."
    ) : SendState()

    /**
     * 送信完了
     */
    data object Completed : SendState()

    /**
     * 送信失敗
     */
    data class Failed(
        val error: SyncError
    ) : SendState()
}
