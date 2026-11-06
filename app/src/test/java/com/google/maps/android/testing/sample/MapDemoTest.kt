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

package com.google.maps.android.testing.sample

import android.graphics.Color
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.google.maps.android.robolectric.annotation.EnableMapsShadows
import com.google.maps.android.robolectric.ktx.clickMarker
import com.google.maps.android.robolectric.ktx.withTestMap
import com.google.maps.android.robolectric.truth.assertThat
import com.google.maps.android.robolectric.truth.meters
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// [START maps_robolectric_demo_test]
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@EnableMapsShadows
class MapDemoTest {

  @Test
  fun testMapOverlaysAndAssertions() = withTestMap(width = 200, height = 200) {
    // 1. Center the camera on Boulder, Colorado
    val boulder = LatLng(40.0150, -105.2705)
    moveCamera(CameraUpdateFactory.newLatLngZoom(boulder, 14f))

    // 2. Add a Marker with a tinted icon
    addMarker(
      MarkerOptions()
        .position(boulder)
        .title("Boulder, CO")
        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)),
    )

    // 3. Draw a Polygon around the area
    val northBoulderPark = LatLng(40.0185, -105.2705)
    addPolygon(
      PolygonOptions()
        .add(
          LatLng(40.0100, -105.2800),
          LatLng(40.0100, -105.2600),
          LatLng(40.0200, -105.2600),
          LatLng(40.0200, -105.2800),
        )
        .fillColor(Color.GREEN)
        .strokeColor(Color.DKGRAY)
        .strokeWidth(2f),
    )

    // 4. Simulate a user tapping the marker (opens InfoWindow by default)
    clickMarker("Boulder, CO")

    // 5. Assert geodesic camera position, active InfoWindow, and rendered canvas pixel
    assertThat(this)
      .isCenteredOn(boulder, tolerance = 5.meters)
      .hasZoom(14f)
      .hasActiveInfoWindowWithTitle("Boulder, CO")
      .hasPixelColorAt(northBoulderPark, Color.GREEN)
  }
}
// [END maps_robolectric_demo_test]
