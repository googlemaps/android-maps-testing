/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.powerassert.gradle.PowerAssertCompilationFilter

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.power.assert)
  alias(libs.plugins.kover)
  alias(libs.plugins.dokka)
  alias(libs.plugins.maven.publish)
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
powerAssert {
  functions = listOf(
    "kotlin.assert",
    "kotlin.require",
    "kotlin.check",
    "com.google.maps.android.robolectric.powerassert.assertGeo",
    "com.google.maps.android.robolectric.powerassert.assertMap",
    "com.google.maps.android.robolectric.powerassert.assertMarker",
  )
  compilationFilter = PowerAssertCompilationFilter.ALL
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_11)
  }
}

android {
  namespace = "com.google.maps.android.robolectric.shadows"
  compileSdk = 37

  defaultConfig {
    minSdk = 24
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

kover {
  reports {
    filters {
      excludes {
        androidGeneratedClasses()
      }
    }
  }
}

dependencies {
  compileOnly(libs.play.services.maps)
  compileOnly(libs.robolectric)
  compileOnly(libs.robolectric.shadows.framework)
  compileOnly(libs.truth)

  testImplementation(libs.play.services.maps)
  testImplementation(libs.robolectric)
  testImplementation(libs.junit)
  testImplementation(libs.truth)
}

tasks.withType<Test> {
  testLogging {
    showStandardStreams = true
  }
}

