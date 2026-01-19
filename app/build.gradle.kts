/*
 * Copyright © 2023, 2024 Damyan Ivanov.
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

import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kover)
}

// Load keystore properties for release signing
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    compileSdk = 34
    defaultConfig {
        applicationId = "net.ktnx.mobileledger"
        minSdk = 22
        targetSdk = 34
        vectorDrawables.useSupportLibrary = true
        versionCode = 59
        versionName = "0.22.1"
        testInstrumentationRunner = "net.ktnx.mobileledger.HiltTestRunner"
    }
    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }
    buildTypes {
        release {
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
        debug {
            versionNameSuffix = "-debug"
            applicationIdSuffix = ".debug"
        }
        create("pre") {
            applicationIdSuffix = ".pre"
            versionNameSuffix = "-pre"
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets", "src/main/assets/")
            java.srcDirs("src/main/kotlin")
        }
        getByName("test") {
            java.srcDirs("src/test/kotlin")
        }
        getByName("androidTest") {
            java.srcDirs("src/androidTest/kotlin")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + listOf("-Xjsr305=strict")
        javaParameters = true
    }
    @Suppress("UnstableApiUsage")
    productFlavors {
    }
    buildFeatures {
        viewBinding = false
        compose = true
    }
    buildToolsVersion = "34.0.0"
    namespace = "net.ktnx.mobileledger"
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}

dependencies {
    // Kotlin
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.collections.immutable)

    // AndroidX Lifecycle
    implementation(libs.bundles.lifecycle)

    // Room
    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)

    // Navigation
    implementation(libs.bundles.navigation)

    // Jackson with Kotlin module
    implementation(libs.bundles.jackson)

    // AndroidX
    implementation(fileTree(mapOf("include" to listOf("*.jar"), "dir" to "libs")))
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.google.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.appcompat)

    // Annotations
    implementation(libs.jetbrains.annotations)

    // Logging
    implementation(libs.logcat)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.compose.navigation)
    implementation(libs.compose.hilt.navigation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.reorderable)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.hilt.android.testing)
    testImplementation(libs.robolectric)
    kspTest(libs.hilt.compiler)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.bundles.compose.testing)
    kspAndroidTest(libs.hilt.compiler)
}

allprojects {
    gradle.projectsEvaluated {
        tasks.withType<JavaCompile>().configureEach {
            options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
        }
    }
}

// Kover Configuration for Test Coverage (Kotlin-optimized)
kover {
    reports {
        // Filter out generated code from coverage
        filters {
            excludes {
                classes(
                    // Android generated
                    "*R",
                    "*R\$*",
                    "*BuildConfig",
                    "*Manifest*",
                    // Hilt generated
                    "*_HiltModules*",
                    "*_Factory",
                    "*_Factory\$*",
                    "*_MembersInjector",
                    "*Module_*Factory",
                    "Hilt_*",
                    "*_HiltComponents*",
                    // Dagger generated
                    "dagger.*",
                    "*_Impl",
                    "*_Impl\$*",
                    // Room generated
                    "*Dao_Impl",
                    "*Dao_Impl\$*",
                    "*Database_Impl",
                    "*Database_Impl\$*",
                    // DI modules (configuration only)
                    "*.di.*Module",
                    "*.di.*Module\$*"
                )
                packages(
                    "hilt_aggregated_deps",
                    "dagger.hilt.internal.aggregatedroot.codegen",
                    "dagger.hilt.internal.processedrootsentinel.codegen"
                )
            }
        }

        // Verification rules
        verify {
            rule {
                minBound(30) // 30% line coverage minimum
            }
        }
    }
}
