/*
 * Copyright © 2024 Damyan Ivanov.
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

@file:Suppress("UNUSED", "MatchingDeclarationName", "ktlint:standard:no-wildcard-imports")

package net.ktnx.mobileledger.ui.theme

// Re-export all colors from core:ui for backward compatibility
// New code should import from net.ktnx.mobileledger.core.ui.theme directly
import net.ktnx.mobileledger.core.ui.theme.MoLeBackground as CoreMoLeBackground
import net.ktnx.mobileledger.core.ui.theme.MoLeBackgroundDark as CoreMoLeBackgroundDark
import net.ktnx.mobileledger.core.ui.theme.MoLeError as CoreMoLeError
import net.ktnx.mobileledger.core.ui.theme.MoLeErrorContainer as CoreMoLeErrorContainer
import net.ktnx.mobileledger.core.ui.theme.MoLeErrorContainerDark as CoreMoLeErrorContainerDark
import net.ktnx.mobileledger.core.ui.theme.MoLeErrorDark as CoreMoLeErrorDark
import net.ktnx.mobileledger.core.ui.theme.MoLeOnBackground as CoreMoLeOnBackground
import net.ktnx.mobileledger.core.ui.theme.MoLeOnBackgroundDark as CoreMoLeOnBackgroundDark
import net.ktnx.mobileledger.core.ui.theme.MoLeOnError as CoreMoLeOnError
import net.ktnx.mobileledger.core.ui.theme.MoLeOnErrorContainer as CoreMoLeOnErrorContainer
import net.ktnx.mobileledger.core.ui.theme.MoLeOnErrorContainerDark as CoreMoLeOnErrorContainerDark
import net.ktnx.mobileledger.core.ui.theme.MoLeOnErrorDark as CoreMoLeOnErrorDark
import net.ktnx.mobileledger.core.ui.theme.MoLeOnPrimary as CoreMoLeOnPrimary
import net.ktnx.mobileledger.core.ui.theme.MoLeOnPrimaryContainerDark as CoreMoLeOnPrimaryContainerDark
import net.ktnx.mobileledger.core.ui.theme.MoLeOnPrimaryDark as CoreMoLeOnPrimaryDark
import net.ktnx.mobileledger.core.ui.theme.MoLeOnSecondary as CoreMoLeOnSecondary
import net.ktnx.mobileledger.core.ui.theme.MoLeOnSecondaryContainerDark as CoreMoLeOnSecondaryContainerDark
import net.ktnx.mobileledger.core.ui.theme.MoLeOnSecondaryDark as CoreMoLeOnSecondaryDark
import net.ktnx.mobileledger.core.ui.theme.MoLeOnSurface as CoreMoLeOnSurface
import net.ktnx.mobileledger.core.ui.theme.MoLeOnSurfaceDark as CoreMoLeOnSurfaceDark
import net.ktnx.mobileledger.core.ui.theme.MoLeOnSurfaceVariant as CoreMoLeOnSurfaceVariant
import net.ktnx.mobileledger.core.ui.theme.MoLeOnSurfaceVariantDark as CoreMoLeOnSurfaceVariantDark
import net.ktnx.mobileledger.core.ui.theme.MoLeOutline as CoreMoLeOutline
import net.ktnx.mobileledger.core.ui.theme.MoLeOutlineDark as CoreMoLeOutlineDark
import net.ktnx.mobileledger.core.ui.theme.MoLeOutlineVariant as CoreMoLeOutlineVariant
import net.ktnx.mobileledger.core.ui.theme.MoLeOutlineVariantDark as CoreMoLeOutlineVariantDark
import net.ktnx.mobileledger.core.ui.theme.MoLePrimary as CoreMoLePrimary
import net.ktnx.mobileledger.core.ui.theme.MoLePrimaryContainerDark as CoreMoLePrimaryContainerDark
import net.ktnx.mobileledger.core.ui.theme.MoLePrimaryDark as CoreMoLePrimaryDark
import net.ktnx.mobileledger.core.ui.theme.MoLePrimaryVariant as CoreMoLePrimaryVariant
import net.ktnx.mobileledger.core.ui.theme.MoLeSecondary as CoreMoLeSecondary
import net.ktnx.mobileledger.core.ui.theme.MoLeSecondaryContainerDark as CoreMoLeSecondaryContainerDark
import net.ktnx.mobileledger.core.ui.theme.MoLeSecondaryDark as CoreMoLeSecondaryDark
import net.ktnx.mobileledger.core.ui.theme.MoLeSecondaryVariant as CoreMoLeSecondaryVariant
import net.ktnx.mobileledger.core.ui.theme.MoLeSurface as CoreMoLeSurface
import net.ktnx.mobileledger.core.ui.theme.MoLeSurfaceDark as CoreMoLeSurfaceDark
import net.ktnx.mobileledger.core.ui.theme.MoLeSurfaceVariant as CoreMoLeSurfaceVariant
import net.ktnx.mobileledger.core.ui.theme.MoLeSurfaceVariantDark as CoreMoLeSurfaceVariantDark
import net.ktnx.mobileledger.core.ui.theme.NegativeAmount as CoreNegativeAmount
import net.ktnx.mobileledger.core.ui.theme.NegativeAmountDark as CoreNegativeAmountDark
import net.ktnx.mobileledger.core.ui.theme.PositiveAmount as CorePositiveAmount
import net.ktnx.mobileledger.core.ui.theme.PositiveAmountDark as CorePositiveAmountDark

val MoLePrimary = CoreMoLePrimary
val MoLePrimaryVariant = CoreMoLePrimaryVariant
val MoLeOnPrimary = CoreMoLeOnPrimary
val MoLeSecondary = CoreMoLeSecondary
val MoLeSecondaryVariant = CoreMoLeSecondaryVariant
val MoLeOnSecondary = CoreMoLeOnSecondary
val MoLeBackground = CoreMoLeBackground
val MoLeOnBackground = CoreMoLeOnBackground
val MoLeSurface = CoreMoLeSurface
val MoLeOnSurface = CoreMoLeOnSurface
val MoLeSurfaceVariant = CoreMoLeSurfaceVariant
val MoLeOnSurfaceVariant = CoreMoLeOnSurfaceVariant
val MoLeError = CoreMoLeError
val MoLeOnError = CoreMoLeOnError
val MoLeErrorContainer = CoreMoLeErrorContainer
val MoLeOnErrorContainer = CoreMoLeOnErrorContainer
val MoLeOutline = CoreMoLeOutline
val MoLeOutlineVariant = CoreMoLeOutlineVariant
val MoLePrimaryDark = CoreMoLePrimaryDark
val MoLePrimaryContainerDark = CoreMoLePrimaryContainerDark
val MoLeOnPrimaryDark = CoreMoLeOnPrimaryDark
val MoLeOnPrimaryContainerDark = CoreMoLeOnPrimaryContainerDark
val MoLeSecondaryDark = CoreMoLeSecondaryDark
val MoLeSecondaryContainerDark = CoreMoLeSecondaryContainerDark
val MoLeOnSecondaryDark = CoreMoLeOnSecondaryDark
val MoLeOnSecondaryContainerDark = CoreMoLeOnSecondaryContainerDark
val MoLeBackgroundDark = CoreMoLeBackgroundDark
val MoLeOnBackgroundDark = CoreMoLeOnBackgroundDark
val MoLeSurfaceDark = CoreMoLeSurfaceDark
val MoLeOnSurfaceDark = CoreMoLeOnSurfaceDark
val MoLeSurfaceVariantDark = CoreMoLeSurfaceVariantDark
val MoLeOnSurfaceVariantDark = CoreMoLeOnSurfaceVariantDark
val MoLeErrorDark = CoreMoLeErrorDark
val MoLeOnErrorDark = CoreMoLeOnErrorDark
val MoLeErrorContainerDark = CoreMoLeErrorContainerDark
val MoLeOnErrorContainerDark = CoreMoLeOnErrorContainerDark
val MoLeOutlineDark = CoreMoLeOutlineDark
val MoLeOutlineVariantDark = CoreMoLeOutlineVariantDark
val PositiveAmount = CorePositiveAmount
val NegativeAmount = CoreNegativeAmount
val PositiveAmountDark = CorePositiveAmountDark
val NegativeAmountDark = CoreNegativeAmountDark
