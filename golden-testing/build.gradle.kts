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

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka)
    `maven-publish`
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

android {
    namespace = "com.google.android.maps.testing.golden"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
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
    api(libs.play.services.maps)
    api(project(":robolectric-testing"))
    compileOnly(libs.robolectric)
    compileOnly(libs.robolectric.shadows.framework)
    compileOnly(libs.truth)
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.robolectric.shadows.framework)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(project(":robolectric-testing"))

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.google.android.maps.testing"
                artifactId = "golden"
                version = rootProject.version.toString()

                pom {
                    name.set("Android Maps Golden Testing")
                    description.set("Deterministic golden image and screenshot testing library for Google Maps SDK on Android.")
                    url.set("https://github.com/googlemaps/android-maps-testing")
                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            name.set("Google Maps Platform Developer Relations")
                        }
                    }
                    scm {
                        connection.set("scm:git:github.com/googlemaps/android-maps-testing.git")
                        developerConnection.set("scm:git:ssh://github.com/googlemaps/android-maps-testing.git")
                        url.set("https://github.com/googlemaps/android-maps-testing/tree/main")
                    }
                }
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    testLogging {
        showStandardStreams = true
    }
}
