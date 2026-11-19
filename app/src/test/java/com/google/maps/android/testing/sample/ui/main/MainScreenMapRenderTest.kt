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

package com.google.maps.android.testing.sample.ui.main

import android.app.Activity
import android.view.View.MeasureSpec
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.robolectric.MapsRobolectricTestRunner
import com.google.maps.android.robolectric.ktx.getMapBlocking
import com.google.maps.android.robolectric.truth.assertThat
import com.google.maps.android.testing.sample.data.DefaultDataRepository
import com.google.maps.android.testing.sample.manager.StoreLocatorMapManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(MapsRobolectricTestRunner::class)
@Config(sdk = [34])
class MainScreenMapRenderTest {

  @Test
  fun renderStoresOnMap_whenLaidOut_displaysMarkersAndCentersOnBounds() = runTest {
    val stores = DefaultDataRepository().data.first()
    val expectedBounds = LatLngBounds.builder().apply {
      stores.forEach { include(it.location) }
    }.build()

    val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    val mapView = MapView(activity).apply {
      onCreate(null)
      onResume()
    }
    activity.setContentView(
      mapView,
      FrameLayout.LayoutParams(320, 640),
    )
    mapView.measure(
      MeasureSpec.makeMeasureSpec(320, MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(640, MeasureSpec.EXACTLY),
    )
    mapView.layout(0, 0, 320, 640)

    val googleMap = mapView.getMapBlocking()
    val mapManager = StoreLocatorMapManager()
    mapManager.attachMap(googleMap)

    renderStoresOnMap(mapView, mapManager, stores)

    assertThat(googleMap)
      .hasMarkerCount(stores.size)
      .isCenteredOn(expectedBounds.center)
    for (store in stores) {
      assertThat(googleMap).showsOnScreen(store.location)
    }
  }

  @Test
  fun renderStoresOnMap_beforeLayout_centersOnBoundsOnceLaidOut() = runTest {
    val stores = DefaultDataRepository().data.first()
    val expectedBounds = LatLngBounds.builder().apply {
      stores.forEach { include(it.location) }
    }.build()

    val mapView = MapView(ApplicationProvider.getApplicationContext()).apply {
      onCreate(null)
      onResume()
    }
    val googleMap = mapView.getMapBlocking()
    val mapManager = StoreLocatorMapManager()
    mapManager.attachMap(googleMap)

    renderStoresOnMap(mapView, mapManager, stores)

    assertThat(googleMap)
      .hasMarkerCount(stores.size)
      .isCenteredOn(LatLng(0.0, 0.0))

    mapView.measure(
      MeasureSpec.makeMeasureSpec(320, MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(640, MeasureSpec.EXACTLY),
    )
    mapView.layout(0, 0, 320, 640)

    assertThat(googleMap).isCenteredOn(expectedBounds.center)
    for (store in stores) {
      assertThat(googleMap).showsOnScreen(store.location)
    }
  }
}
