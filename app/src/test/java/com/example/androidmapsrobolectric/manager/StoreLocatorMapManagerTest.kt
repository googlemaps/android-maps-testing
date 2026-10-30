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

package com.example.androidmapsrobolectric.manager

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.example.androidmapsrobolectric.model.Store
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
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

// [START maps_robolectric_manager_test]
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@EnableMapsShadows
class StoreLocatorMapManagerTest {

  private lateinit var mapView: MapView
  private lateinit var googleMap: GoogleMap
  private val selectedStores = mutableListOf<Store>()
  private lateinit var manager: StoreLocatorMapManager

  private val boulderStore = Store(
    id = "store_boulder",
    name = "Boulder Flagship Store",
    location = LatLng(40.0150, -105.2705),
    isOpenNow = true,
  )

  private val denverStore = Store(
    id = "store_denver",
    name = "Denver Downtown Store",
    location = LatLng(39.7392, -104.9903),
    isOpenNow = false,
  )

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
    googleMap = mapRef.get()

    manager = StoreLocatorMapManager(onStoreSelected = { selectedStores.add(it) })
    manager.attachMap(googleMap)
  }

  @Test
  fun testDisplayStores_createsMarkersWithCorrectMetadataAndTags() {
    manager.displayStores(listOf(boulderStore, denverStore))

    val shadowMap = Shadow.extract(googleMap) as ShadowGoogleMap
    assertThat(shadowMap.markers).hasSize(2)

    val boulderMarker = manager.getMarkerForStore("store_boulder")
    assertThat(boulderMarker).isNotNull()
    assertThat(boulderMarker?.title).isEqualTo("Boulder Flagship Store")
    assertThat(boulderMarker?.snippet).isEqualTo("Open now")
    assertThat(boulderMarker?.tag).isEqualTo(boulderStore)
    LatLngSubject.assertThat(boulderMarker!!.position)
      .isWithin(1.meters)
      .of(boulderStore.location)

    val denverMarker = manager.getMarkerForStore("store_denver")
    assertThat(denverMarker?.snippet).isEqualTo("Closed")
    assertThat(denverMarker?.tag).isEqualTo(denverStore)
  }

  @Test
  fun testMarkerClick_selectsStoreAndShowsInfoWindow() {
    manager.displayStores(listOf(boulderStore, denverStore))
    val boulderMarker = manager.getMarkerForStore("store_boulder")!!

    // Simulate user clicking on the marker via the shadow
    val shadowMap = Shadow.extract(googleMap) as ShadowGoogleMap
    shadowMap.clickMarker(boulderMarker)

    // Verify the manager notified the application listener
    assertThat(selectedStores).containsExactly(boulderStore)
    // Verify info window is displayed
    assertThat(boulderMarker.isInfoWindowShown).isTrue()
  }

  @Test
  fun testSelectStore_centersCameraAndOpensInfoWindow() {
    manager.displayStores(listOf(boulderStore, denverStore))

    val success = manager.selectStore("store_boulder")
    assertThat(success).isTrue()

    // Verify camera moved to Boulder
    LatLngSubject.assertThat(googleMap.cameraPosition.target)
      .isWithin(5.meters)
      .of(boulderStore.location)
    assertThat(googleMap.cameraPosition.zoom).isEqualTo(15f)

    // Verify info window state
    val marker = manager.getMarkerForStore("store_boulder")
    assertThat(marker?.isInfoWindowShown).isTrue()
  }

  @Test
  fun testClearStores_removesAllMarkersFromMap() {
    manager.displayStores(listOf(boulderStore, denverStore))
    val shadowMap = Shadow.extract(googleMap) as ShadowGoogleMap
    assertThat(shadowMap.markers).hasSize(2)

    manager.clearStores()
    assertThat(shadowMap.markers).isEmpty()
    assertThat(manager.getMarkerForStore("store_boulder")).isNull()
  }

  @Test
  fun testDeliveryPerimeter_rendersVisuallyOnCanvas() {
    // Center the camera on Boulder
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(boulderStore.location, 14f))

    // Set a delivery perimeter circle
    manager.setDeliveryPerimeter(boulderStore.location, 2000.0)

    // Render MapView to a bitmap canvas
    val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    mapView.draw(canvas)

    // Center pixel (100, 100) corresponds to Boulder inside the perimeter circle
    val centerPixel = bitmap.getPixel(100, 100)
    assertThat(centerPixel).isNotEqualTo(Color.LTGRAY)
    assertThat(Color.alpha(centerPixel)).isGreaterThan(0)
  }
}
// [END maps_robolectric_manager_test]
