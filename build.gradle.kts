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

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.kotlin.power.assert) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.kover) apply false
  alias(libs.plugins.spotless)
  alias(libs.plugins.dokka) apply false
}

allprojects {
  // {x-release-please-start-version}
  version = "1.0.0-rc01"
  // {x-release-please-end}
}

subprojects {
  apply(plugin = "com.diffplug.spotless")
  configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
      target("**/*.kt")
      targetExclude("**/build/**")
      ktlint("1.8.0").editorConfigOverride(
        mapOf(
          "indent_size" to "2",
          "max_line_length" to "off",
          "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
          "ktlint_standard_function-naming" to "disabled",
          "ktlint_standard_filename" to "disabled",
          "ktlint_standard_property-naming" to "disabled",
          "ktlint_standard_backing-property-naming" to "disabled"
        )
      )
      trimTrailingWhitespace()
      endWithNewline()
    }
    java {
      target("**/*.java")
      targetExclude("**/build/**")
      googleJavaFormat("1.30.0")
      trimTrailingWhitespace()
      endWithNewline()
    }
  }
}