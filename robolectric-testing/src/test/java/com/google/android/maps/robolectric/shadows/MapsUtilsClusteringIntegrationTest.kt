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

package com.google.android.maps.robolectric.shadows

import android.app.Activity
import android.graphics.Point
import android.os.Bundle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.maps.robolectric.annotation.EnableMapsShadows
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
import kotlin.math.hypot

/**
 * Integration test verifying that [ShadowGoogleMap] and [ShadowProjection] provide the exact
 * projection and camera lifecycle semantics required by android-maps-utils clustering algorithms.
 */
// [START maps_robolectric_clustering_test]
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@EnableMapsShadows
class MapsUtilsClusteringIntegrationTest {

  private lateinit var mapView: MapView
  private lateinit var googleMap: GoogleMap
  private lateinit var shadowMap: ShadowGoogleMap

  @Before
  fun setUp() {
    val activity = Robolectric.buildActivity(Activity::class.java).create().get()
    mapView = MapView(activity).apply {
      onCreate(Bundle())
    }

    val mapRef = AtomicReference<GoogleMap>()
    mapView.getMapAsync { mapRef.set(it) }
    ShadowLooper.idleMainLooper()
    googleMap = mapRef.get()
    shadowMap = Shadow.extract(googleMap) as ShadowGoogleMap
  }

  @Test
  fun projection_providesFallbackViewportWhenUnmeasured() {
    // Even if MapView layout hasn't run, projection provides realistic 1080x1920 viewport
    val projection = googleMap.projection
    assertThat(projection).isNotNull()

    val screenPoint = projection.toScreenLocation(LatLng(0.0, 0.0))
    assertThat(screenPoint.x).isGreaterThan(0)
    assertThat(screenPoint.y).isGreaterThan(0)

    val region = projection.visibleRegion
    assertThat(region).isNotNull()
    assertThat(region.latLngBounds).isNotNull()
    assertThat(region.latLngBounds.northeast.latitude).isGreaterThan(region.latLngBounds.southwest.latitude)
  }

  @Test
  fun clusteringSimulation_clustersNearbyItemsAndSplitsOnZoom() {
    // Configure standard 1080x1920 test viewport
    shadowMap.setViewportSize(1080, 1920)

    // Center map on San Francisco at low zoom (level 10)
    val sf = LatLng(37.7749, -122.4194)
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sf, 10f))

    // Two nearby locations in downtown San Francisco (~1 km apart)
    val marketStreet = LatLng(37.7750, -122.4190)
    val unionSquare = LatLng(37.7879, -122.4074)

    val items = listOf(marketStreet, unionSquare)

    // Define clustering function mimicking android-maps-utils DistanceBasedAlgorithm
    fun clusterItems(maxScreenDistancePx: Double): List<List<LatLng>> {
      val projection = googleMap.projection
      val clusters = mutableListOf<MutableList<LatLng>>()

      for (item in items) {
        val point = projection.toScreenLocation(item)
        var addedToCluster = false

        for (cluster in clusters) {
          val clusterCenterPoint = projection.toScreenLocation(cluster.first())
          val distance = hypot(
            (point.x - clusterCenterPoint.x).toDouble(),
            (point.y - clusterCenterPoint.y).toDouble(),
          )
          if (distance < maxScreenDistancePx) {
            cluster.add(item)
            addedToCluster = true
            break
          }
        }

        if (!addedToCluster) {
          clusters.add(mutableListOf(item))
        }
      }
      return clusters
    }

    // At zoom 10, both items are close on screen (< 80px apart) -> single cluster
    val clustersZoom10 = clusterItems(maxScreenDistancePx = 80.0)
    assertThat(clustersZoom10).hasSize(1)
    assertThat(clustersZoom10.first()).hasSize(2)

    // Wire re-clustering to onCameraIdleListener (standard android-maps-utils pattern)
    var currentClusterCount = 0
    googleMap.setOnCameraIdleListener {
      currentClusterCount = clusterItems(maxScreenDistancePx = 80.0).size
    }

    // Zoom in to level 16 -> camera move triggers onCameraIdleListener
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sf, 16f))

    // At zoom 16, distance between items expands on screen (> 300px) -> clusters split into 2
    assertThat(currentClusterCount).isEqualTo(2)
  }

  @Test
  fun markerClicks_triggerClusterSelection() {
    val pos = LatLng(37.7749, -122.4194)
    val clusterMarker = googleMap.addMarker(
      MarkerOptions()
        .position(pos)
        .title("Cluster (2 items)"),
    )

    var clickedClusterTitle: String? = null
    googleMap.setOnMarkerClickListener { marker ->
      clickedClusterTitle = marker.title
      true
    }

    shadowMap.clickMarker(clusterMarker!!)
    assertThat(clickedClusterTitle).isEqualTo("Cluster (2 items)")
  }
}
// [END maps_robolectric_clustering_test]
