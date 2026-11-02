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

package com.google.android.maps.testing.golden

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.maps.robolectric.annotation.EnableMapsShadows
import com.google.android.maps.robolectric.truth.meters
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@EnableMapsShadows
class ComposeMapTestExtensionsTest {

  private lateinit var activity: Activity
  private lateinit var mapView: MapView
  private lateinit var container: FrameLayout

  @Before
  fun setUp() {
    activity = Robolectric.buildActivity(Activity::class.java).create().get()
    mapView = MapView(activity).apply {
      onCreate(Bundle())
    }

    // Embed MapView deeply inside nested view groups to test tree traversal
    val innerLayout = LinearLayout(activity).apply {
      addView(mapView)
    }
    container = FrameLayout(activity).apply {
      addView(innerLayout)
    }
  }

  @Test
  fun findMapView_findsDeeplyNestedMapViewInHierarchy() {
    val found = container.findMapView()
    assertThat(found).isSameInstanceAs(mapView)
  }

  @Test
  fun findMapView_returnsNullWhenNoMapViewExists() {
    val emptyLayout = LinearLayout(activity)
    assertThat(emptyLayout.findMapView()).isNull()
  }

  @Test
  fun awaitMap_retrievesInitializedGoogleMap() = runTest {
    ShadowLooper.idleMainLooper()
    val googleMap = mapView.awaitMap()
    assertThat(googleMap).isNotNull()
  }

  @Test
  fun mapExtensions_findMarkerAndClick() = runTest {
    ShadowLooper.idleMainLooper()
    val googleMap = mapView.awaitMap()

    val seattle = LatLng(47.6062, -122.3321)
    googleMap.addMarker(MarkerOptions().position(seattle).title("Pike Place Market"))

    // Verify findMarkerByTitle
    val marker = googleMap.findMarkerByTitle("Pike Place Market")
    assertThat(marker).isNotNull()
    assertThat(marker?.title).isEqualTo("Pike Place Market")

    // Verify clickMarker
    var clicked = false
    googleMap.setOnMarkerClickListener {
      clicked = true
      true
    }

    val clickResult = googleMap.clickMarker("Pike Place Market")
    assertThat(clickResult).isTrue()
    assertThat(clicked).isTrue()

    // Non-existent marker click returns false
    val missingClick = googleMap.clickMarker("NonExistent")
    assertThat(missingClick).isFalse()
  }

  @Test
  fun mapExtensions_assertCameraTargetWithGeodesicTolerance() = runTest {
    ShadowLooper.idleMainLooper()
    val googleMap = mapView.awaitMap()

    val tokyo = LatLng(35.6762, 139.6503)
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tokyo, 15f))

    // Assert camera target within 5 meters
    googleMap.assertCameraTarget(tokyo, tolerance = 5.meters)
  }

  @Test
  fun mapExtensions_awaitMapLoadedCompletesPromptly() = runTest {
    ShadowLooper.idleMainLooper()
    val googleMap = mapView.awaitMap()

    val loadDeferred = async {
      googleMap.awaitMapLoaded()
    }
    testScheduler.advanceUntilIdle()
    assertThat(loadDeferred.isCompleted).isTrue()
  }
}
