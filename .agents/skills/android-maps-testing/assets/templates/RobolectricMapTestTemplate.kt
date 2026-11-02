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

package com.example.maptest.templates

import android.graphics.Color
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.maps.robolectric.annotation.EnableMapsShadows
import com.google.android.maps.robolectric.ktx.clickMarker
import com.google.android.maps.robolectric.ktx.simulateUserCameraMove
import com.google.android.maps.robolectric.ktx.withTestMap
import com.google.android.maps.robolectric.truth.assertThat
import com.google.android.maps.robolectric.truth.meters
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Standard template for headless JVM Google Maps unit testing with Robolectric shadows.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@EnableMapsShadows
class RobolectricMapTestTemplate {

  @Test
  fun testCameraMovementAndDistanceAssertion() = withTestMap(width = 200, height = 200) {
    val targetLocation = LatLng(37.7749, -122.4194)
    moveCamera(CameraUpdateFactory.newLatLngZoom(targetLocation, 14f))

    assertThat(this)
      .isCenteredOn(targetLocation, tolerance = 5.meters)
      .hasZoom(14f)
  }

  @Test
  fun testMarkerInteractionAndUserGesture() = withTestMap(width = 200, height = 200) {
    val pinLocation = LatLng(37.7749, -122.4194)
    addMarker(
      MarkerOptions()
        .position(pinLocation)
        .title("San Francisco")
        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)),
    )

    // Default SDK behavior: tapping marker opens its InfoWindow and centers camera
    clickMarker("San Francisco")

    assertThat(this)
      .hasMarkerCount(1)
      .hasActiveInfoWindowWithTitle("San Francisco")

    // Simulate a user dragging the map (fires OnCameraMoveStartedListener.REASON_GESTURE)
    val unionSquare = LatLng(37.7879, -122.4074)
    simulateUserCameraMove(unionSquare)
    assertThat(this).isCenteredOn(unionSquare, tolerance = 5.meters)
  }

  @Test
  fun testCanvasDrawing() = withTestMap(width = 200, height = 200) {
    val center = LatLng(0.0, 0.0)
    moveCamera(CameraUpdateFactory.newLatLngZoom(center, 0f))
    addPolygon(
      PolygonOptions()
        .add(LatLng(-10.0, -10.0), LatLng(-10.0, 10.0), LatLng(10.0, 10.0), LatLng(10.0, -10.0))
        .fillColor(Color.GREEN)
        .strokeWidth(0f),
    )

    assertThat(this).hasPixelColorAt(center, Color.GREEN)
  }
}
