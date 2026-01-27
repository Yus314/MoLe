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

import net.ktnx.mobileledger.core.domain.model.API
import net.ktnx.mobileledger.core.domain.model.ServerVersion

/**
 * Extension function to get suitable API version for ServerVersion.
 * This keeps the core domain model free of JSON/API dependencies.
 *
 * サポート対象: v1_32 以降のみ
 *
 * @return 適切なAPIバージョン、または null（v1_32 未満の場合）
 */
fun ServerVersion.getSuitableApiVersion(): API? {
    if (isPre_1_20_1) return null

    return when {
        atLeast(1, 50) -> API.v1_50
        atLeast(1, 40) -> API.v1_40
        atLeast(1, 32) -> API.v1_32
        else -> null // Server version < 1.32 is not supported
    }
}
