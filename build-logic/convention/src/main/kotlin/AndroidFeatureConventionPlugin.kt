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

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for feature modules.
 *
 * Applies:
 * - mole.android.library (base library setup)
 * - mole.android.hilt (Hilt DI)
 * - Compose dependencies and build features
 *
 * Usage in feature module build.gradle.kts:
 * ```
 * plugins {
 *     alias(libs.plugins.mole.android.feature)
 * }
 *
 * android {
 *     namespace = "net.ktnx.mobileledger.feature.myfeature"
 * }
 *
 * dependencies {
 *     implementation(project(":core:domain"))
 *     // feature-specific dependencies
 * }
 * ```
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("mole.android.library")
                apply("mole.android.hilt")
                apply("org.jetbrains.kotlin.plugin.compose")
            }

            extensions.configure<LibraryExtension> {
                buildFeatures {
                    compose = true
                }
            }

            val libs = extensions.getByType(org.gradle.api.artifacts.VersionCatalogsExtension::class.java)
                .named("libs")

            dependencies {
                // Compose BOM and core dependencies
                add("implementation", platform(libs.findLibrary("compose-bom").get()))
                add("implementation", libs.findLibrary("compose-ui").get())
                add("implementation", libs.findLibrary("compose-material3").get())
                add("implementation", libs.findLibrary("compose-ui-tooling-preview").get())
                add("debugImplementation", libs.findLibrary("compose-ui-tooling").get())

                // Lifecycle ViewModel Compose integration
                add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())

                // Hilt Navigation Compose
                add("implementation", libs.findLibrary("compose-hilt-navigation").get())

                // Core modules commonly needed by features
                add("implementation", project(":core:common"))
                add("implementation", project(":core:domain"))
            }
        }
    }
}
