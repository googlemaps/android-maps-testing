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

package com.example.androidmapsrobolectric

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

// [START maps_robolectric_demo_test]
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@EnableMapsShadows
class MapDemoTest {

  private lateinit var mapView: MapView

  @Before
  fun setUp() {
    val activity = Robolectric.buildActivity(Activity::class.java).create().get()
    mapView = MapView(activity).apply {
      measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
      )
      layout(0, 0, 100, 100)
      onCreate(Bundle())
    }
  }

  @Test
  fun testMapOverlaysAndAssertions() {
    // 1. Obtain the GoogleMap instance asynchronously
    val mapRef = AtomicReference<GoogleMap>()
    mapView.getMapAsync { mapRef.set(it) }
    ShadowLooper.idleMainLooper()
    val map = mapRef.get()
    assertThat(map).isNotNull()

    // 2. Center the camera on Boulder, Colorado
    val boulder = LatLng(40.0150, -105.2705)
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(boulder, 14f))

    // 3. Assert geodesic camera coordinates using the Truth LatLngSubject
    LatLngSubject.assertThat(map.cameraPosition.target)
      .isWithin(5.meters)
      .of(boulder)

    // 4. Add a Marker with a tinted icon
    val marker = map.addMarker(
      MarkerOptions()
        .position(boulder)
        .title("Boulder, CO")
        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)),
    )
    assertThat(marker?.title).isEqualTo("Boulder, CO")

    // 5. Draw a Polygon around the area
    map.addPolygon(
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

    // 6. Render the MapView directly onto an Android Canvas without an emulator
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    mapView.draw(canvas)

    // 7. Verify pixel color: the center pixel (50, 50) corresponds to Boulder inside the polygon
    assertThat(bitmap.getPixel(50, 50)).isEqualTo(Color.GREEN)

    // 8. Simulate user marker interaction via the shadow
    val shadowMap = Shadow.extract(map) as ShadowGoogleMap
    var clickedMarkerTitle: String? = null
    map.setOnMarkerClickListener {
      clickedMarkerTitle = it.title
      true
    }
    shadowMap.clickMarker(marker!!)
    assertThat(clickedMarkerTitle).isEqualTo("Boulder, CO")
  }
}
// [END maps_robolectric_demo_test]
