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

package net.ktnx.mobileledger.ui.templates

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/**
 * Navigation routes for the templates feature.
 */
object TemplatesDestinations {
    const val LIST_ROUTE = "template_list"
    const val DETAIL_ROUTE = "template_detail"
    const val DETAIL_ARG_TEMPLATE_ID = "templateId"

    fun detailRoute(templateId: Long?): String = "$DETAIL_ROUTE/${templateId ?: -1}"
}

/**
 * Navigation graph for the templates feature using Compose Navigation.
 * Implements slide animations for screen transitions.
 */
@Composable
fun TemplatesNavHost(navController: NavHostController = rememberNavController(), onNavigateBack: () -> Unit) {
    val animationDuration = 300

    NavHost(
        navController = navController,
        startDestination = TemplatesDestinations.LIST_ROUTE
    ) {
        composable(
            route = TemplatesDestinations.LIST_ROUTE,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(animationDuration)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(animationDuration)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(animationDuration)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(animationDuration)
                )
            }
        ) {
            TemplateListScreen(
                onNavigateToDetail = { templateId ->
                    navController.navigate(TemplatesDestinations.detailRoute(templateId))
                }
            )
        }

        composable(
            route = "${TemplatesDestinations.DETAIL_ROUTE}/{${TemplatesDestinations.DETAIL_ARG_TEMPLATE_ID}}",
            arguments = listOf(
                navArgument(TemplatesDestinations.DETAIL_ARG_TEMPLATE_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            ),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(animationDuration)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(animationDuration)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(animationDuration)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(animationDuration)
                )
            }
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getLong(TemplatesDestinations.DETAIL_ARG_TEMPLATE_ID)
            val effectiveTemplateId = if (templateId == -1L) null else templateId

            TemplateDetailScreen(
                templateId = effectiveTemplateId,
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        onNavigateBack()
                    }
                }
            )
        }
    }
}
