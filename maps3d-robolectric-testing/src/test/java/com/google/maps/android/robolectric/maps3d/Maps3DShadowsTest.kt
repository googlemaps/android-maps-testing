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

package com.google.maps.android.robolectric.maps3d

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import com.google.android.gms.maps3d.GoogleMap3D
import com.google.android.gms.maps3d.OnMap3DViewReadyCallback
import com.google.android.gms.maps3d.model.AltitudeMode
import com.google.android.gms.maps3d.model.Camera
import com.google.android.gms.maps3d.model.FlyAroundOptions
import com.google.android.gms.maps3d.model.FlyToOptions
import com.google.android.gms.maps3d.model.LatLngAltitude
import com.google.android.gms.maps3d.model.Map3DMode
import com.google.android.gms.maps3d.model.MarkerOptions
import com.google.android.gms.maps3d.model.ModelOptions
import com.google.android.gms.maps3d.model.Orientation
import com.google.android.gms.maps3d.model.PolygonOptions
import com.google.android.gms.maps3d.model.PolylineOptions
import com.google.android.gms.maps3d.model.PopoverOptions
import com.google.android.gms.maps3d.model.Vector3D
import com.google.android.gms.maps3d.model.ZoomControl
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.robolectric.maps3d.ktx.createTestMap3DView
import com.google.maps.android.robolectric.maps3d.ktx.findMap3DView
import com.google.maps.android.robolectric.maps3d.ktx.getMap3DBlocking
import com.google.maps.android.robolectric.maps3d.ktx.requireMap3DView
import com.google.maps.android.robolectric.maps3d.ktx.shadow
import com.google.maps.android.robolectric.maps3d.ktx.withTestMap3D
import com.google.maps.android.robolectric.maps3d.truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowLooper

@RunWith(Maps3DRobolectricTestRunner::class)
class Maps3DShadowsTest {

  @Test
  fun map3DView_lifecycleAndReadinessCallbacks_areTracked() {
    val activity = Robolectric.buildActivity(Activity::class.java).create().get()
    val container = FrameLayout(activity)
    val mapView = createTestMap3DView(mapMode = Map3DMode.SATELLITE, activity = activity)
    container.addView(mapView)
    activity.setContentView(container)

    assertThat(activity.requireMap3DView()).isSameInstanceAs(mapView)
    assertThat(container.findMap3DView()).isSameInstanceAs(mapView)

    mapView.onStart()
    mapView.onResume()
    mapView.onPause()
    mapView.onStop()
    mapView.onSaveInstanceState(Bundle())
    mapView.onLowMemory()
    mapView.onDestroy()

    val shadowView = mapView.shadow
    assertThat(shadowView.isOnCreateCalled).isTrue()
    assertThat(shadowView.isOnStartCalled).isTrue()
    assertThat(shadowView.isOnResumeCalled).isTrue()
    assertThat(shadowView.isOnPauseCalled).isTrue()
    assertThat(shadowView.isOnStopCalled).isTrue()
    assertThat(shadowView.isOnSaveInstanceStateCalled).isTrue()
    assertThat(shadowView.isOnLowMemoryCalled).isTrue()
    assertThat(shadowView.isOnDestroyCalled).isTrue()

    var readyReadiness = -1.0
    var steadyState = false
    val map = mapView.getMap3DBlocking()
    assertThat(map).hasMapMode(Map3DMode.SATELLITE)

    map.setOnMapReadyListener { readiness -> readyReadiness = readiness }
    map.setOnMapSteadyListener { steady -> steadyState = steady }

    shadowView.triggerMapReady(1.0)
    shadowView.triggerMapSteady(true)

    assertThat(readyReadiness).isEqualTo(1.0)
    assertThat(steadyState).isTrue()
  }

  @Test
  fun map3DView_triggerError_invokesOnErrorCallback() {
    val mapView = createTestMap3DView()
    var captured: Exception? = null
    mapView.getMap3DViewAsync(
      object : OnMap3DViewReadyCallback {
        override fun onMap3DViewReady(googleMap3D: GoogleMap3D) = Unit

        override fun onError(error: Exception) {
          captured = error
        }
      },
    )
    ShadowLooper.idleMainLooper()

    val expected = IllegalStateException("offline")
    mapView.shadow.triggerError(expected)
    assertThat(captured).isSameInstanceAs(expected)
  }

  @Test
  fun googleMap3D_cameraAndFlyAnimations_updateStateAndListeners() = withTestMap3D { map ->
    var lastCamera: Camera? = null
    var animationEnds = 0
    map.setCameraChangedListener { lastCamera = it }
    map.setCameraAnimationEndListener { animationEnds++ }

    val startCamera = Camera(LatLngAltitude(36.4859, -114.5329, 614.4), 10.0, 65.0, 0.0, 120.0)
    map.setCamera(startCamera)
    assertThat(map).hasCameraCenterNear(36.4859, -114.5329)
    assertThat(map).hasCameraAltitudeWithin(614.4)
    assertThat(lastCamera).isEqualTo(startCamera)

    val endCamera = Camera(LatLngAltitude(37.7749, -122.4194, 50.0), 45.0, 60.0, 0.0, 200.0)
    map.flyCameraTo(FlyToOptions(endCamera, 1_500L))
    assertThat(map).hasCameraCenterNear(37.7749, -122.4194)
    assertThat(map.shadow.lastFlyToOptions?.durationInMillis).isEqualTo(1_500L)
    assertThat(animationEnds).isEqualTo(1)

    map.flyCameraAround(FlyAroundOptions(startCamera, 2_000L, 1.0))
    assertThat(map.shadow.lastFlyAroundOptions?.rounds).isEqualTo(1.0)
    assertThat(animationEnds).isEqualTo(2)

    map.stopCameraAnimation()
    assertThat(map.shadow.stopCameraAnimationCallCount).isEqualTo(1)
    assertThat(animationEnds).isEqualTo(3)
    assertThat(map.shadow.cameraHistory).hasSize(3)
  }

  @Test
  fun googleMap3D_overlaysUpsertByIdAndRemoveCleanly() = withTestMap3D { map ->
    val p1 = LatLngAltitude(36.4859, -114.5329, 611.0)
    val p2 = LatLngAltitude(36.4926, -114.5313, 598.0)

    // Polyline upsert by same ID replaces in place
    val poly1 = map.addPolyline(
      PolylineOptions().apply {
        id = "route_traveled"
        path = listOf(p1, p2)
        strokeColor = Color.GREEN
        strokeWidth = 14.0
        altitudeMode = AltitudeMode.ABSOLUTE
      },
    )
    val poly2 = map.addPolyline(
      PolylineOptions().apply {
        id = "route_traveled"
        path = listOf(p1, p2)
        strokeColor = Color.RED
        strokeWidth = 20.0
        altitudeMode = AltitudeMode.ABSOLUTE
      },
    )
    assertThat(map).hasPolylineCount(1)
    assertThat(map).hasPolyline("route_traveled")
    assertThat(map.shadow.findPolylineById("route_traveled")).isSameInstanceAs(poly2)
    assertThat(poly2.strokeColor).isEqualTo(Color.RED)

    // Removing stale reference does not evict newer upserted overlay
    poly1.remove()
    assertThat(map).hasPolylineCount(1)
    poly2.remove()
    assertThat(map).hasPolylineCount(0)
    assertThat(map).doesNotHavePolyline("route_traveled")
    assertThat(poly2.shadow.isRemoved).isTrue()

    // Marker upsert + click simulation
    var markerClicked = false
    val marker = requireNotNull(
      map.addMarker(
        MarkerOptions().apply {
          id = "crest_1"
          position = p1
          label = "Crest +8%"
          altitudeMode = AltitudeMode.ABSOLUTE
        },
      ),
    )
    marker.setClickListener { markerClicked = true }
    assertThat(map).hasMarkerCount(1)
    assertThat(map).hasMarker("crest_1")
    marker.shadow.simulateClick()
    assertThat(markerClicked).isTrue()

    // Model upsert + position assertion
    val model = map.addModel(
      ModelOptions().apply {
        id = "vehicle"
        position = p1
        url = "https://example.com/car.glb"
        altitudeMode = AltitudeMode.ABSOLUTE
        orientation = Orientation(90.0, 270.0, 0.0)
        scale = Vector3D(1.5, 1.5, 1.5)
      },
    )
    assertThat(map).hasModelCount(1)
    assertThat(map).hasModel("vehicle")
    assertThat(map).hasModelNear("vehicle", p1.latitude, p1.longitude)

    // Polygon + Popover + UI controls
    val polygon = map.addPolygon(
      PolygonOptions().apply {
        id = "zone_1"
        path = listOf(p1, p2, p1)
        fillColor = Color.BLUE
        altitudeMode = AltitudeMode.ABSOLUTE
      },
    )
    assertThat(map).hasPolygonCount(1)
    assertThat(map).hasPolygon("zone_1")

    val activity = Robolectric.buildActivity(Activity::class.java).create().get()
    val popover = map.addPopover(
      PopoverOptions().apply {
        content = View(activity)
        positionAnchor = marker
        altitudeMode = AltitudeMode.ABSOLUTE
      },
    )
    popover.show()
    assertThat(popover.isVisible).isTrue()
    assertThat(map).hasPopoverCount(1)

    val uiSettings = requireNotNull(map.getUiControlSettings())
    uiSettings.setUiControlsPlacement(listOf(ZoomControl()), 1)
    assertThat(uiSettings.shadow.getControlsAt(1)).hasSize(1)

    // Map click simulation
    var clickedPlace: String? = null
    map.setMap3DClickListener { event ->
      clickedPlace = event.placeId
      true
    }
    assertThat(map.shadow.simulateMapClick(p1, "place_123")).isTrue()
    assertThat(clickedPlace).isEqualTo("place_123")

    // Cleanup verifies hasNoOverlays()
    marker.remove()
    model.remove()
    polygon.remove()
    popover.remove()
    assertThat(map).hasNoOverlays()
  }
}
