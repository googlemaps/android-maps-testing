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

package com.google.maps.android.testing.sample.manager

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.robolectric.annotation.EnableMapsShadows
import com.google.maps.android.robolectric.ktx.cameraTarget
import com.google.maps.android.robolectric.ktx.cameraZoom
import com.google.maps.android.robolectric.ktx.circles
import com.google.maps.android.robolectric.ktx.clickMarker
import com.google.maps.android.robolectric.ktx.createTestMap
import com.google.maps.android.robolectric.ktx.markers
import com.google.maps.android.robolectric.ktx.requireMarker
import com.google.maps.android.robolectric.ktx.simulateUserCameraMove
import com.google.maps.android.robolectric.ktx.visibleBounds
import com.google.maps.android.robolectric.powerassert.assertMap
import com.google.maps.android.robolectric.powerassert.contains
import com.google.maps.android.robolectric.powerassert.hasMarker
import com.google.maps.android.robolectric.powerassert.hasOverlayPixelAt
import com.google.maps.android.robolectric.powerassert.isCenteredOn
import com.google.maps.android.robolectric.powerassert.isInfoWindowShownFor
import com.google.maps.android.robolectric.truth.assertThat
import com.google.maps.android.robolectric.truth.distanceTo
import com.google.maps.android.robolectric.truth.kilometers
import com.google.maps.android.robolectric.truth.meters
import com.google.maps.android.testing.sample.model.Store
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.google.common.truth.Truth.assertThat as truthAssertThat

/**
 * Side-by-side "Lazy Programmer" comparison demonstrating how [StoreLocatorMapManager] can be
 * tested with minimal ceremony using either:
 * 1. **Google Truth** domain subjects (`com.google.maps.android.robolectric.truth.assertThat`)
 * 2. **Kotlin Power-Assert** (`com.google.maps.android.robolectric.powerassert.assertMap`)
 *
 * Compare this file with [StoreLocatorMapManagerTest] to see how the lazy KTX harness and domain
 * assertions eliminate boilerplate.
 */
// [START maps_robolectric_lazy_store_locator_comparison]
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@EnableMapsShadows
class StoreLocatorLazyAssertComparisonTest {

  private lateinit var map: GoogleMap
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
    // 1-liner map creation (replaces Activity + MeasureSpec + layout + onCreate + AtomicReference + ShadowLooper)
    map = createTestMap(width = 200, height = 200)
    manager = StoreLocatorMapManager(onStoreSelected = { selectedStores.add(it) })
    manager.attachMap(map)
  }

  // ===========================================================================================
  // SCENARIO 1: Displaying stores & verifying marker metadata
  // ===========================================================================================

  @Test
  fun displayStores_googleTruth() {
    manager.displayStores(listOf(boulderStore, denverStore))

    assertThat(map).hasMarkerCount(2)
    assertThat(map).hasMarkerWithTitle("Boulder Flagship Store")
      .isAt(boulderStore.location)
      .hasSnippet("Open now")
      .hasTag(boulderStore)
    assertThat(map).hasMarkerWithTitle("Denver Downtown Store")
      .isAt(denverStore.location)
      .hasSnippet("Closed")
      .hasTag(denverStore)
  }

  @Test
  fun displayStores_kotlinPowerAssert() {
    manager.displayStores(listOf(boulderStore, denverStore))

    map.assertMap(
      map.markers.size == 2 &&
        map.hasMarker(title = "Boulder Flagship Store", snippet = "Open now", tag = boulderStore, near = boulderStore.location) &&
        map.hasMarker(title = "Denver Downtown Store", snippet = "Closed", tag = denverStore, near = denverStore.location),
    )
  }

  // ===========================================================================================
  // SCENARIO 2: Clicking a marker selects the store and opens its info window
  // ===========================================================================================

  @Test
  fun markerClick_googleTruth() {
    manager.displayStores(listOf(boulderStore, denverStore))

    map.clickMarker("Boulder Flagship Store")

    truthAssertThat(selectedStores).containsExactly(boulderStore)
    assertThat(map).hasActiveInfoWindowWithTitle("Boulder Flagship Store")
  }

  @Test
  fun markerClick_kotlinPowerAssert() {
    manager.displayStores(listOf(boulderStore, denverStore))

    map.clickMarker("Boulder Flagship Store")

    map.assertMap(
      selectedStores == listOf(boulderStore) &&
        map.isInfoWindowShownFor("Boulder Flagship Store"),
    )
  }

  // ===========================================================================================
  // SCENARIO 3: Selecting a store animates the camera and opens its info window
  // ===========================================================================================

  @Test
  fun selectStore_googleTruth() {
    manager.displayStores(listOf(boulderStore, denverStore))

    truthAssertThat(manager.selectStore("store_boulder")).isTrue()
    assertThat(map)
      .isCenteredOn(boulderStore.location, tolerance = 5.meters)
      .hasZoom(15f)
      .hasActiveInfoWindowWithTitle("Boulder Flagship Store")
  }

  @Test
  fun selectStore_kotlinPowerAssert() {
    manager.displayStores(listOf(boulderStore, denverStore))

    map.assertMap(
      manager.selectStore("store_boulder") &&
        (map.cameraTarget distanceTo boulderStore.location) <= 5.meters &&
        map.cameraZoom == 15f &&
        map.requireMarker(title = "Boulder Flagship Store").isInfoWindowShown,
    )
  }

  // ===========================================================================================
  // SCENARIO 4: Delivery perimeter circle geometry & canvas pixel rendering
  // ===========================================================================================

  @Test
  fun deliveryPerimeter_googleTruth() {
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(boulderStore.location, 14f))
    manager.setDeliveryPerimeter(boulderStore.location, 2000.0)

    assertThat(map)
      .hasCircleCount(1)
      .hasNonBackgroundPixelAt(boulderStore.location)
    assertThat(map).firstCircle()
      .isCenteredOn(boulderStore.location)
      .hasRadius(2.kilometers)
      .contains(boulderStore.location)
      .doesNotContain(denverStore.location)
  }

  @Test
  fun deliveryPerimeter_kotlinPowerAssert() {
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(boulderStore.location, 14f))
    manager.setDeliveryPerimeter(boulderStore.location, 2000.0)

    val circle = map.circles.single()
    map.assertMap(
      map.isCenteredOn(boulderStore.location, expectedZoom = 14f) &&
        boulderStore.location in circle &&
        denverStore.location !in circle &&
        map.hasOverlayPixelAt(boulderStore.location),
    )
  }

  // ===========================================================================================
  // SCENARIO 5: Fitting all stores in bounds & detecting user pan gestures (REASON_GESTURE)
  // ===========================================================================================

  @Test
  fun fitAllStoresAndUserGesture_googleTruth() {
    manager.displayStores(listOf(boulderStore, denverStore))

    truthAssertThat(manager.fitAllStores(paddingPx = 16)).isTrue()
    assertThat(map)
      .showsOnScreen(boulderStore.location)
      .showsOnScreen(denverStore.location)
    truthAssertThat(manager.isSearchThisAreaVisible).isFalse()

    map.simulateUserCameraMove(LatLng(39.90, -105.10))
    truthAssertThat(manager.isSearchThisAreaVisible).isTrue()
  }

  @Test
  fun fitAllStoresAndUserGesture_kotlinPowerAssert() {
    manager.displayStores(listOf(boulderStore, denverStore))

    truthAssertThat(manager.fitAllStores(paddingPx = 16)).isTrue()
    map.assertMap(
      boulderStore.location in map.visibleBounds &&
        denverStore.location in map.visibleBounds &&
        !manager.isSearchThisAreaVisible,
    )

    map.simulateUserCameraMove(LatLng(39.90, -105.10))
    map.assertMap(manager.isSearchThisAreaVisible)
  }
}
// [END maps_robolectric_lazy_store_locator_comparison]
