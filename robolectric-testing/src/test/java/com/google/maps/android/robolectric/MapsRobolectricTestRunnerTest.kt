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

package com.google.maps.android.robolectric

import android.app.Activity
import android.os.Build
import android.os.Bundle
import com.google.android.gms.maps.MapView
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.robolectric.shadows.ShadowMapView
import org.junit.Test
import org.junit.runner.JUnitCore
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import java.io.File
import java.net.JarURLConnection
import java.util.jar.JarFile

class MapsRobolectricTestRunnerTest {

  @Test
  fun globalConfigRegistersEveryMapsShadow() {
    val config = MapsGlobalConfigProvider.get()

    assertThat(config.shadows.map { it.java })
      .containsExactlyElementsIn(MapsRobolectricTestRunner.MAPS_SHADOWS)
  }

  @Test
  fun mapsShadowsListsEveryShadowInTheLibrary() {
    val shadowClasses = classesInPackage(SHADOWS_PACKAGE)
      .filter { it.isAnnotationPresent(Implements::class.java) }

    assertThat(shadowClasses).isNotEmpty()
    assertThat(MapsRobolectricTestRunner.MAPS_SHADOWS).containsExactlyElementsIn(shadowClasses)
  }

  @Test
  fun classLevelConfigStillAppliesOnTopOfMapsShadows() {
    val result = JUnitCore.runClasses(RunnerFixture::class.java)

    assertThat(result.failures.map { it.trace }).isEmpty()
    assertThat(result.runCount).isEqualTo(1)
  }

  private fun classesInPackage(packageName: String): List<Class<*>> {
    val path = packageName.replace('.', '/')
    val loader = checkNotNull(javaClass.classLoader)
    // The package is split between the library's classes and this module's tests, so scan every location.
    val classFiles = loader.getResources(path).toList().flatMap { url ->
      when (url.protocol) {
        "jar" -> JarFile((url.openConnection() as JarURLConnection).jarFileURL.path).use { jar ->
          jar.entries().toList().map { it.name }
            .filter { it.startsWith("$path/") && it.endsWith(".class") && it.indexOf('/', path.length + 1) == -1 }
            .map { it.substringAfterLast('/') }
        }

        else -> File(url.toURI()).listFiles().orEmpty().map { it.name }.filter { it.endsWith(".class") }
      }
    }
    return classFiles
      .filterNot { it.contains('$') || it.endsWith("Test.class") }
      .distinct()
      .map { Class.forName("$packageName.${it.removeSuffix(".class")}", false, loader) }
  }

  private companion object {
    const val SHADOWS_PACKAGE = "com.google.maps.android.robolectric.shadows"
  }
}

/** Run through [JUnitCore] by [MapsRobolectricTestRunnerTest]; not meant to run on its own. */
@RunWith(MapsRobolectricTestRunner::class)
@Config(sdk = [33])
class RunnerFixture {

  @Test
  fun mapsShadowsAndClassConfigBothApply() {
    assertThat(Build.VERSION.SDK_INT).isEqualTo(33)

    val activity = Robolectric.buildActivity(Activity::class.java).create().get()
    val mapView = MapView(activity)
    mapView.onCreate(Bundle())

    assertThat(Shadow.extract<Any>(mapView)).isInstanceOf(ShadowMapView::class.java)
  }
}
