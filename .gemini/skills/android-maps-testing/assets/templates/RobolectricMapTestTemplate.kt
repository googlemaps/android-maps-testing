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

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.maps.robolectric.annotation.EnableMapsShadows
import com.google.android.maps.robolectric.shadows.ShadowGoogleMap
import com.google.android.maps.robolectric.truth.LatLngSubject
import com.google.android.maps.robolectric.truth.isWithin
import com.google.android.maps.robolectric.truth.meters
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.atomic.AtomicReference

/**
 * Standard template for headless JVM Google Maps unit testing with Robolectric shadows.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@EnableMapsShadows
class RobolectricMapTestTemplate {

  private lateinit var mapView: MapView
  private lateinit var googleMap: GoogleMap
  private lateinit var shadowMap: ShadowGoogleMap

  @Before
  fun setUp() {
    val activity = Robolectric.buildActivity(Activity::class.java).create().get()
    mapView = MapView(activity).apply {
      measure(
        View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
      )
      layout(0, 0, 200, 200)
      onCreate(Bundle())
    }

    val mapRef = AtomicReference<GoogleMap>()
    mapView.getMapAsync { mapRef.set(it) }
    ShadowLooper.idleMainLooper()
    googleMap = checkNotNull(mapRef.get())
    shadowMap = Shadow.extract(googleMap) as ShadowGoogleMap
  }

  @Test
  fun testCameraMovementAndDistanceAssertion() {
    val targetLocation = LatLng(37.7749, -122.4194)
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLocation, 14f))

    // Assert geodesic distance within 5 meters tolerance
    LatLngSubject.assertThat(googleMap.cameraPosition.target)
      .isWithin(5.meters)
      .of(targetLocation)
  }

  @Test
  fun testMarkerInteraction() {
    val pinLocation = LatLng(37.7749, -122.4194)
    val marker = googleMap.addMarker(
      MarkerOptions()
        .position(pinLocation)
        .title("San Francisco")
        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)),
    )
    assertThat(marker?.title).isEqualTo("San Francisco")

    var clickedMarkerTitle: String? = null
    googleMap.setOnMarkerClickListener {
      clickedMarkerTitle = it.title
      true
    }

    // Simulate user tapping the marker via the shadow
    shadowMap.clickMarker(marker!!)
    assertThat(clickedMarkerTitle).isEqualTo("San Francisco")
  }

  @Test
  fun testCanvasDrawing() {
    val center = LatLng(0.0, 0.0)
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 0f))
    googleMap.addPolygon(
      PolygonOptions()
        .add(LatLng(-10.0, -10.0), LatLng(-10.0, 10.0), LatLng(10.0, 10.0), LatLng(10.0, -10.0))
        .fillColor(Color.GREEN)
        .strokeWidth(0f),
    )

    val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
    mapView.draw(Canvas(bitmap))

    // The center pixel corresponds to LatLng(0,0) inside the green polygon
    assertThat(bitmap.getPixel(100, 100)).isEqualTo(Color.GREEN)
  }
}
