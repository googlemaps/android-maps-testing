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

import com.google.maps.android.robolectric.shadows.ShadowBitmapDescriptor
import com.google.maps.android.robolectric.shadows.ShadowBitmapDescriptorFactory
import com.google.maps.android.robolectric.shadows.ShadowCameraUpdate
import com.google.maps.android.robolectric.shadows.ShadowCameraUpdateFactory
import com.google.maps.android.robolectric.shadows.ShadowCircle
import com.google.maps.android.robolectric.shadows.ShadowGoogleMap
import com.google.maps.android.robolectric.shadows.ShadowGroundOverlay
import com.google.maps.android.robolectric.shadows.ShadowMapView
import com.google.maps.android.robolectric.shadows.ShadowMapsInitializer
import com.google.maps.android.robolectric.shadows.ShadowMarker
import com.google.maps.android.robolectric.shadows.ShadowPolygon
import com.google.maps.android.robolectric.shadows.ShadowPolyline
import com.google.maps.android.robolectric.shadows.ShadowProjection
import com.google.maps.android.robolectric.shadows.ShadowSupportMapFragment
import com.google.maps.android.robolectric.shadows.ShadowTileOverlay
import com.google.maps.android.robolectric.shadows.ShadowUiSettings
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.pluginapi.config.GlobalConfigProvider
import org.robolectric.util.inject.Injector

/**
 * Robolectric test runner that registers every Google Maps SDK shadow for the tests it runs.
 *
 * Other `@Config` values on the test class or method (such as `sdk`) still apply, and shadows
 * listed there are added to the Maps shadows.
 *
 * ```kotlin
 * @RunWith(MapsRobolectricTestRunner::class)
 * @Config(sdk = [34])
 * class MyMapTest {
 *     // All Maps shadows are registered
 * }
 * ```
 */
// [START maps_robolectric_test_runner]
open class MapsRobolectricTestRunner(testClass: Class<*>) : RobolectricTestRunner(testClass, INJECTOR) {

  /** Holds the Maps shadows registered by [MapsRobolectricTestRunner]. */
  companion object {
    /** Every shadow in `:robolectric-testing`, in the order they are registered. */
    @JvmField
    val MAPS_SHADOWS: List<Class<*>> = listOf(
      ShadowMapView::class.java,
      ShadowGoogleMap::class.java,
      ShadowCameraUpdate::class.java,
      ShadowCameraUpdateFactory::class.java,
      ShadowMarker::class.java,
      ShadowPolygon::class.java,
      ShadowPolyline::class.java,
      ShadowProjection::class.java,
      ShadowCircle::class.java,
      ShadowUiSettings::class.java,
      ShadowBitmapDescriptor::class.java,
      ShadowBitmapDescriptorFactory::class.java,
      ShadowMapsInitializer::class.java,
      ShadowSupportMapFragment::class.java,
      ShadowGroundOverlay::class.java,
      ShadowTileOverlay::class.java,
    )

    // One injector for every test class using this runner, so Robolectric keeps reusing its
    // sandboxes. Binding the GlobalConfigProvider here, instead of overriding the deprecated
    // buildGlobalConfig(), matters: buildGlobalConfig() is stored in a static field that only the
    // first runner created in the JVM gets to set.
    private val INJECTOR: Injector = defaultInjector()
      .bind(GlobalConfigProvider::class.java, MapsGlobalConfigProvider)
      .build()
  }
}

/** Supplies the default [Config] for [MapsRobolectricTestRunner]: Robolectric's defaults plus the Maps shadows. */
internal object MapsGlobalConfigProvider : GlobalConfigProvider {
  override fun get(): Config = Config.Builder().setShadows(*MapsRobolectricTestRunner.MAPS_SHADOWS.toTypedArray()).build()
}
// [END maps_robolectric_test_runner]
