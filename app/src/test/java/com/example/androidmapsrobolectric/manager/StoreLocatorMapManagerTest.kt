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

import com.example.androidmapsrobolectric.model.Store
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.maps.robolectric.annotation.EnableMapsShadows
import com.google.android.maps.robolectric.ktx.clickMarker
import com.google.android.maps.robolectric.ktx.createTestMap
import com.google.android.maps.robolectric.ktx.markers
import com.google.android.maps.robolectric.truth.assertThat
import com.google.android.maps.robolectric.truth.meters
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.google.common.truth.Truth.assertThat as truthAssertThat

// [START maps_robolectric_manager_test]
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@EnableMapsShadows
class StoreLocatorMapManagerTest {

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
    googleMap = createTestMap(width = 200, height = 200)
    manager = StoreLocatorMapManager(onStoreSelected = { selectedStores.add(it) })
    manager.attachMap(googleMap)
  }

  @Test
  fun testDisplayStores_createsMarkersWithCorrectMetadataAndTags() {
    manager.displayStores(listOf(boulderStore, denverStore))

    assertThat(googleMap).hasMarkerCount(2)
    assertThat(googleMap)
      .hasMarkerWithTitle("Boulder Flagship Store")
      .isAt(boulderStore.location, tolerance = 1.meters)
      .hasSnippet("Open now")
      .hasTag(boulderStore)

    assertThat(googleMap)
      .hasMarkerWithTitle("Denver Downtown Store")
      .hasSnippet("Closed")
      .hasTag(denverStore)
  }

  @Test
  fun testMarkerClick_selectsStoreAndShowsInfoWindow() {
    manager.displayStores(listOf(boulderStore, denverStore))

    // Simulate a user tapping the marker by title
    googleMap.clickMarker("Boulder Flagship Store")

    // Verify the manager notified the application listener and opened the InfoWindow
    truthAssertThat(selectedStores).containsExactly(boulderStore)
    assertThat(googleMap).hasActiveInfoWindowWithTitle("Boulder Flagship Store")
  }

  @Test
  fun testSelectStore_centersCameraAndOpensInfoWindow() {
    manager.displayStores(listOf(boulderStore, denverStore))

    val success = manager.selectStore("store_boulder")
    truthAssertThat(success).isTrue()

    assertThat(googleMap)
      .isCenteredOn(boulderStore.location, tolerance = 5.meters)
      .hasZoom(15f)
      .hasActiveInfoWindowWithTitle("Boulder Flagship Store")
  }

  @Test
  fun testClearStores_removesAllMarkersFromMap() {
    manager.displayStores(listOf(boulderStore, denverStore))
    assertThat(googleMap).hasMarkerCount(2)

    manager.clearStores()
    truthAssertThat(googleMap.markers).isEmpty()
    truthAssertThat(manager.getMarkerForStore("store_boulder")).isNull()
  }

  @Test
  fun testDeliveryPerimeter_rendersVisuallyOnCanvas() {
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(boulderStore.location, 14f))
    manager.setDeliveryPerimeter(boulderStore.location, 2000.0)

    assertThat(googleMap).hasNonBackgroundPixelAt(boulderStore.location)
  }
}
// [END maps_robolectric_manager_test]
