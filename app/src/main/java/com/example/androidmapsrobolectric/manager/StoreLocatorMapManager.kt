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

import android.graphics.Color
import com.example.androidmapsrobolectric.model.Store
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

// [START maps_robolectric_manager]
/**
 * Production map manager that coordinates store locator map overlays, camera positioning,
 * and user tap interactions on a GoogleMap instance.
 */
class StoreLocatorMapManager(
  private val onStoreSelected: ((Store) -> Unit)? = null,
) {
  private var googleMap: GoogleMap? = null
  private val storeMarkers = mutableMapOf<String, Marker>()
  private var deliveryZoneCircle: Circle? = null

  /**
   * Attaches and configures the [GoogleMap] instance with UI controls and click listeners.
   */
  fun attachMap(map: GoogleMap) {
    this.googleMap = map
    map.uiSettings.isZoomControlsEnabled = true
    map.uiSettings.isCompassEnabled = true

    map.setOnMarkerClickListener { marker ->
      val store = marker.tag as? Store
      if (store != null) {
        marker.showInfoWindow()
        onStoreSelected?.invoke(store)
        true
      } else {
        false
      }
    }
  }

  /**
   * Renders a collection of [Store] locations as markers on the map, color-coded by open status.
   */
  fun displayStores(stores: List<Store>) {
    val map = googleMap ?: return
    clearStores()

    for (store in stores) {
      val hue = if (store.isOpenNow) {
        BitmapDescriptorFactory.HUE_AZURE
      } else {
        BitmapDescriptorFactory.HUE_RED
      }

      val markerOptions = MarkerOptions()
        .position(store.location)
        .title(store.name)
        .snippet(if (store.isOpenNow) "Open now" else "Closed")
        .icon(BitmapDescriptorFactory.defaultMarker(hue))

      val marker = map.addMarker(markerOptions)
      if (marker != null) {
        marker.tag = store
        storeMarkers[store.id] = marker
      }
    }
  }

  /**
   * Selects a store by ID, centering the map camera on its location and displaying its info window.
   */
  fun selectStore(storeId: String): Boolean {
    val marker = storeMarkers[storeId] ?: return false
    val map = googleMap ?: return false

    map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 15f))
    marker.showInfoWindow()
    val store = marker.tag as? Store
    if (store != null) {
      onStoreSelected?.invoke(store)
    }
    return true
  }

  /**
   * Draws a delivery service perimeter circle around a store coordinate.
   */
  fun setDeliveryPerimeter(center: LatLng, radiusMeters: Double) {
    val map = googleMap ?: return
    deliveryZoneCircle?.remove()

    deliveryZoneCircle = map.addCircle(
      CircleOptions()
        .center(center)
        .radius(radiusMeters)
        .fillColor(Color.argb(70, 0, 150, 255))
        .strokeColor(Color.BLUE)
        .strokeWidth(2f),
    )
  }

  /**
   * Clears all store markers from the map.
   */
  fun clearStores() {
    for (marker in storeMarkers.values) {
      marker.remove()
    }
    storeMarkers.clear()
  }

  fun getMarkerForStore(storeId: String): Marker? = storeMarkers[storeId]
}
// [END maps_robolectric_manager]
