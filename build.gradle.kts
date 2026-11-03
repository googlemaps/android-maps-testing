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

import com.diffplug.gradle.spotless.SpotlessExtension
import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SourcesJar
import kotlinx.validation.KotlinApiBuildTask
import kotlinx.validation.KotlinApiCompareTask

private val R_TYPES = listOf(
  "anim", "animator", "array", "attr", "bool", "color", "dimen", "drawable", "font", "fraction", "id",
  "integer", "interpolator", "layout", "menu", "mipmap", "plurals", "raw", "string", "style", "styleable",
  "transition", "xml",
)

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
  alias(libs.plugins.binary.compatibility.validator)
  alias(libs.plugins.maven.publish) apply false
}

// The public API of the library modules is checked against the committed api/<module>.api files.
apiValidation {
  ignoredProjects += listOf("app", "docs")
}

val apiDump = tasks.register("apiDump") {
  group = "verification"
  description = "Dumps public API declarations for all library modules."
}

val apiCheck = tasks.register("apiCheck") {
  group = "verification"
  description = "Checks that public API declarations match the committed .api files."
}

allprojects {
  // {x-release-please-start-version}
  version = "1.0.0-rc01"
  // {x-release-please-end}
}

subprojects {
  apply(plugin = "com.diffplug.spotless")
  configure<SpotlessExtension> {
    kotlin {
      target("src/**/*.kt")
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
      target("src/**/*.java")
      targetExclude("**/build/**")
      googleJavaFormat("1.30.0")
      trimTrailingWhitespace()
      endWithNewline()
    }
  }

  if (name in listOf("robolectric-testing", "golden-testing", "visual-testing")) {
    val projectName = name
    val apiFile = layout.projectDirectory.file("api/$projectName.api")
    val buildApiFile = layout.buildDirectory.file("api/$projectName.api")

    plugins.withId("com.vanniktech.maven.publish") {
      extensions.configure<MavenPublishBaseExtension> {
        configure(
          AndroidSingleVariantLibrary(
            variant = "release",
            sourcesJar = SourcesJar.Sources(),
            javadocJar = JavadocJar.Javadoc(),
          ),
        )

        publishToMavenCentral()
        if (
          findProperty("signing.keyId")?.toString()?.isNotBlank() == true ||
          findProperty("signing.secretKeyRingFile")?.toString()?.isNotBlank() == true ||
          findProperty("signingInMemoryKey")?.toString()?.isNotBlank() == true
        ) {
          signAllPublications()
        }

        val (moduleGroupId, moduleArtifactId, modulePomName, moduleDescription) = when (projectName) {
          "robolectric-testing" -> listOf(
            "com.google.android.maps.robolectric",
            "shadows",
            "Android Maps Robolectric Shadows",
            "Robolectric shadows, Kotlin Power-Assert helpers, and Google Truth testing utilities for Google Maps SDK on Android.",
          )
          "golden-testing" -> listOf(
            "com.google.android.maps.testing",
            "golden",
            "Android Maps Golden Testing",
            "Deterministic golden image and screenshot testing library for Google Maps SDK on Android.",
          )
          else -> listOf(
            "com.google.android.maps.testing",
            "visual",
            "Android Maps Visual Testing",
            "AI-powered visual verification and screenshot testing library for Google Maps SDK on Android.",
          )
        }

        coordinates(
          groupId = moduleGroupId,
          artifactId = moduleArtifactId,
          version = rootProject.version.toString(),
        )

        pom {
          name.set(modulePomName)
          description.set(moduleDescription)
          url.set("https://github.com/googlemaps/android-maps-testing")
          licenses {
            license {
              name.set("The Apache Software License, Version 2.0")
              url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
              distribution.set("repo")
            }
          }
          scm {
            connection.set("scm:git@github.com:googlemaps/android-maps-testing.git")
            developerConnection.set("scm:git@github.com:googlemaps/android-maps-testing.git")
            url.set("https://github.com/googlemaps/android-maps-testing")
          }
          developers {
            developer {
              id.set("google")
              name.set("Google LLC")
            }
          }
          organization {
            name.set("Google Inc")
            url.set("http://developers.google.com/maps")
          }
        }
      }
    }

    plugins.withId("com.android.library") {
      afterEvaluate {
        val bundleTask = tasks.findByName("bundleLibCompileToJarRelease") ?: return@afterEvaluate
        val classesJar = layout.buildDirectory.file(
          "intermediates/compile_library_classes_jar/release/bundleLibCompileToJarRelease/classes.jar",
        )

        val apiBuild = tasks.register<KotlinApiBuildTask>("apiBuild") {
          group = "verification"
          description = "Builds public API declaration for $projectName."
          inputJar.set(classesJar)
          outputApiFile.set(buildApiFile)
          val rClasses = listOf(
            "com.google.android.maps.robolectric.R",
            "com.google.android.maps.testing.golden.R",
            "com.google.android.maps.testing.visual.R",
          )
          ignoredClasses.addAll(rClasses)
          ignoredClasses.addAll(rClasses.flatMap { r -> R_TYPES.map { type -> "$r$$type" } })
          dependsOn(bundleTask)
        }

        val moduleApiDump = tasks.register<Copy>("apiDump") {
          group = "verification"
          description = "Syncs public API declarations of $projectName to the project api/ directory."
          from(apiBuild.flatMap { it.outputApiFile })
          into(apiFile.asFile.parentFile)
          dependsOn(apiBuild)
        }

        val moduleApiCheck = tasks.register<KotlinApiCompareTask>("apiCheck") {
          group = "verification"
          description = "Checks public API declarations of $projectName against the committed api/$projectName.api."
          projectApiFile.set(apiFile)
          generatedApiFile.set(apiBuild.flatMap { it.outputApiFile })
          dependsOn(apiBuild)
          mustRunAfter(moduleApiDump)
        }

        tasks.findByName("check")?.dependsOn(moduleApiCheck)

        rootProject.tasks.matching { it.name == "apiDump" }.configureEach {
          dependsOn(moduleApiDump)
        }
        rootProject.tasks.matching { it.name == "apiCheck" }.configureEach {
          dependsOn(moduleApiCheck)
        }
      }
    }
  }
}