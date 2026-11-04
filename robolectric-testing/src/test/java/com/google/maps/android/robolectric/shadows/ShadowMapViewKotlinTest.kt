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

package com.google.maps.android.robolectric.shadows

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapColorScheme
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.robolectric.annotation.EnableMapsShadows
import com.google.maps.android.robolectric.ktx.cameraTargetBounds
import com.google.maps.android.robolectric.ktx.contentDescription
import com.google.maps.android.robolectric.ktx.getMapBlocking
import com.google.maps.android.robolectric.ktx.mapStyle
import com.google.maps.android.robolectric.ktx.requireMapView
import com.google.maps.android.robolectric.ktx.simulateUserCameraMove
import com.google.maps.android.robolectric.truth.GoogleMapSubject
import com.google.maps.android.robolectric.truth.LatLngSubject
import com.google.maps.android.robolectric.truth.isWithin
import com.google.maps.android.robolectric.truth.kilometer
import com.google.maps.android.robolectric.truth.meter
import com.google.maps.android.robolectric.truth.meters
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

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@EnableMapsShadows
class ShadowMapViewKotlinTest {

  private lateinit var activity: Activity
  private lateinit var mapView: MapView

  @Before
  fun setUp() {
    activity = Robolectric.buildActivity(Activity::class.java).create().get()
    mapView = MapView(activity)
  }

  @Test
  fun testMapViewLifecycle() {
    val shadowMapView = Shadow.extract(mapView) as ShadowMapView
    mapView.onCreate(Bundle())
    assertThat(shadowMapView.isOnCreateCalled).isTrue()
  }

  @Test
  fun testCircleRendering() {
    mapView.measure(
      View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
    )
    mapView.layout(0, 0, 100, 100)

    mapView.onCreate(Bundle())
    val mapRef = AtomicReference<GoogleMap>()
    mapView.getMapAsync { mapRef.set(it) }
    ShadowLooper.idleMainLooper()
    val map = mapRef.get()

    val center = LatLng(37.7749, -122.4194)
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 10f))

    map.addCircle(
      CircleOptions()
        .center(center)
        .radius(1000.0)
        .fillColor(Color.RED)
        .strokeWidth(0f),
    )

    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    mapView.draw(canvas)

    assertThat(bitmap.getPixel(50, 50)).isEqualTo(Color.RED)
    assertThat(bitmap.getPixel(5, 5)).isEqualTo(Color.LTGRAY)
  }

  @Test
  fun testMapClickListeners() {
    mapView.onCreate(Bundle())
    val mapRef = AtomicReference<GoogleMap>()
    mapView.getMapAsync { mapRef.set(it) }
    ShadowLooper.idleMainLooper()
    val map = mapRef.get()
    val shadowMap = Shadow.extract(map) as ShadowGoogleMap

    val clickedLatLng = AtomicReference<LatLng>()
    map.setOnMapClickListener { clickedLatLng.set(it) }

    val longClickedLatLng = AtomicReference<LatLng>()
    map.setOnMapLongClickListener { longClickedLatLng.set(it) }

    val point = LatLng(10.0, 20.0)
    shadowMap.clickMap(point)
    assertThat(clickedLatLng.get()).isEqualTo(point)

    shadowMap.longClickMap(point)
    assertThat(longClickedLatLng.get()).isEqualTo(point)
  }

  @Test
  fun testSnapshot() {
    mapView.measure(
      View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
    )
    mapView.layout(0, 0, 100, 100)

    mapView.onCreate(Bundle())
    val mapRef = AtomicReference<GoogleMap>()
    mapView.getMapAsync { mapRef.set(it) }
    ShadowLooper.idleMainLooper()
    val map = mapRef.get()

    map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(0.0, 0.0), 0f))
    map.addPolygon(
      PolygonOptions()
        .add(LatLng(-10.0, -10.0), LatLng(-10.0, 10.0), LatLng(10.0, 10.0), LatLng(10.0, -10.0))
        .fillColor(Color.GREEN)
        .strokeWidth(0f),
    )

    val snapshotRef = AtomicReference<Bitmap>()
    map.snapshot { snapshotRef.set(it) }

    ShadowLooper.idleMainLooper()

    val snapshot = snapshotRef.get()
    assertThat(snapshot).isNotNull()
    assertThat(snapshot.width).isEqualTo(100)
    assertThat(snapshot.height).isEqualTo(100)

    val pVal = map.projection.toScreenLocation(LatLng(5.0, 0.0))
    assertThat(snapshot.getPixel(pVal.x, pVal.y)).isEqualTo(Color.GREEN)
  }

  @Test
  fun testAddMarker() {
    mapView.onCreate(Bundle())
    val mapRef = AtomicReference<GoogleMap>()
    mapView.getMapAsync { mapRef.set(it) }
    ShadowLooper.idleMainLooper()
    val map = mapRef.get()
    val shadowMap = Shadow.extract(map) as ShadowGoogleMap

    val position = LatLng(37.7749, -122.4194)
    val marker = requireNotNull(map.addMarker(MarkerOptions().position(position).title("San Francisco")))

    assertThat(marker.position).isEqualTo(position)
    assertThat(marker.title).isEqualTo("San Francisco")

    assertThat(shadowMap.markers).hasSize(1)
    assertThat(shadowMap.markers[0]).isEqualTo(marker)
  }

  @Test
  fun testCameraUpdateFactoryMethods() {
    mapView.onCreate(Bundle())
    val mapRef = AtomicReference<GoogleMap>()
    mapView.getMapAsync { mapRef.set(it) }
    ShadowLooper.idleMainLooper()
    val map = mapRef.get()

    val sf = LatLng(37.7749, -122.4194)
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(sf, 10f))
    assertThat(map.cameraPosition.zoom).isEqualTo(10f)

    // zoomIn
    map.moveCamera(CameraUpdateFactory.zoomIn())
    assertThat(map.cameraPosition.zoom).isEqualTo(11f)

    // zoomOut
    map.moveCamera(CameraUpdateFactory.zoomOut())
    assertThat(map.cameraPosition.zoom).isEqualTo(10f)

    // zoomTo
    map.moveCamera(CameraUpdateFactory.zoomTo(15f))
    assertThat(map.cameraPosition.zoom).isEqualTo(15f)

    // zoomBy
    map.moveCamera(CameraUpdateFactory.zoomBy(2.5f))
    assertThat(map.cameraPosition.zoom).isEqualTo(17.5f)

    // newLatLngBounds computes realistic Mercator zoom to fit the bounds inside the viewport
    val bounds = LatLngBounds(
      LatLng(37.0, -123.0),
      LatLng(38.0, -122.0),
    )
    map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50))
    assertThat(map.cameraPosition.target.latitude).isEqualTo(37.5)
    assertThat(map.cameraPosition.target.longitude).isEqualTo(-122.5)
    assertThat(map.cameraPosition.zoom).isGreaterThan(8f)
    assertThat(map.cameraPosition.zoom).isLessThan(12f)
    assertThat(map.projection.visibleRegion.latLngBounds.contains(bounds.southwest)).isTrue()
    assertThat(map.projection.visibleRegion.latLngBounds.contains(bounds.northeast)).isTrue()
  }

  @Test
  fun testCameraBoundsAndScrollBy() {
    mapView.onCreate(Bundle())
    val mapRef = AtomicReference<GoogleMap>()
    mapView.getMapAsync { mapRef.set(it) }
    ShadowLooper.idleMainLooper()
    val map = mapRef.get()

    // [START robolectric_camera_bounds_and_scroll]
    // 1. Fit a bounding box of markers on screen with 64px padding
    val sfBayBounds = LatLngBounds(
      LatLng(37.70, -122.52), // Southwest (Daly City)
      LatLng(37.83, -122.36), // Northeast (Treasure Island)
    )
    map.moveCamera(CameraUpdateFactory.newLatLngBounds(sfBayBounds, 64))

    // Verify camera zoomed in to fit the bay and both corners are visible on screen
    assertThat(map.cameraPosition.zoom).isGreaterThan(10f)
    val visibleBounds = map.projection.visibleRegion.latLngBounds
    assertThat(visibleBounds.contains(sfBayBounds.southwest)).isTrue()
    assertThat(visibleBounds.contains(sfBayBounds.northeast)).isTrue()

    // 2. Scroll the camera 200px East (+X) and 150px South (+Y)
    val centerBeforeScroll = map.cameraPosition.target
    val zoomBeforeScroll = map.cameraPosition.zoom
    map.moveCamera(CameraUpdateFactory.scrollBy(200f, 150f))

    assertThat(map.cameraPosition.zoom).isEqualTo(zoomBeforeScroll)
    assertThat(map.cameraPosition.target.longitude).isGreaterThan(centerBeforeScroll.longitude)
    assertThat(map.cameraPosition.target.latitude).isLessThan(centerBeforeScroll.latitude)
    // [END robolectric_camera_bounds_and_scroll]

    // 3. Explicit viewport dimensions overload works even before MapView layout
    val tighterZoomUpdate = CameraUpdateFactory.newLatLngBounds(sfBayBounds, 400, 400, 32)
    map.moveCamera(tighterZoomUpdate)
    assertThat(map.cameraPosition.zoom).isLessThan(zoomBeforeScroll)
  }

  @Test
  fun testMarkerClickDefaultAndConsumedBehavior() {
    mapView.onCreate(Bundle())
    val mapRef = AtomicReference<GoogleMap>()
    mapView.getMapAsync { mapRef.set(it) }
    ShadowLooper.idleMainLooper()
    val map = mapRef.get()
    val shadowMap = Shadow.extract(map) as ShadowGoogleMap

    // [START robolectric_marker_click_behavior]
    val ferryBuilding = LatLng(37.7955, -122.3937)
    val coitTower = LatLng(37.8024, -122.4058)

    val markerA = requireNotNull(
      map.addMarker(MarkerOptions().position(ferryBuilding).title("Ferry Building")),
    )
    val markerB = requireNotNull(
      map.addMarker(MarkerOptions().position(coitTower).title("Coit Tower")),
    )

    // 1. Default SDK behavior (no listener or listener returns false):
    //    Clicking a marker opens its InfoWindow and centers the camera on the marker.
    shadowMap.clickMarker(markerA)
    assertThat(markerA.isInfoWindowShown).isTrue()
    assertThat(map.cameraPosition.target).isEqualTo(ferryBuilding)

    // 2. Clicking a second marker automatically closes the first marker's InfoWindow
    map.setOnMarkerClickListener { false }
    shadowMap.clickMarker(markerB)
    assertThat(markerA.isInfoWindowShown).isFalse()
    assertThat(markerB.isInfoWindowShown).isTrue()
    assertThat(map.cameraPosition.target).isEqualTo(coitTower)

    // 3. Consuming the click (returning true) suppresses default InfoWindow & camera move
    markerB.hideInfoWindow()
    map.setOnMarkerClickListener { true }
    shadowMap.clickMarker(markerA)
    assertThat(markerA.isInfoWindowShown).isFalse()
    assertThat(map.cameraPosition.target).isEqualTo(coitTower)
    // [END robolectric_marker_click_behavior]
  }

  @Test
  fun testUserGestureCameraSimulationAndMapStyling() {
    mapView.onCreate(Bundle())
    val mapRef = AtomicReference<GoogleMap>()
    mapView.getMapAsync { mapRef.set(it) }
    ShadowLooper.idleMainLooper()
    val map = mapRef.get()

    // [START robolectric_user_gesture_and_styling]
    // 1. Distinguish user pan gestures (REASON_GESTURE) from programmatic animations
    var isFollowingUserLocation = true
    var showSearchThisAreaButton = false

    map.setOnCameraMoveStartedListener { reason ->
      if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
        isFollowingUserLocation = false
      }
    }
    map.setOnCameraIdleListener {
      if (!isFollowingUserLocation) {
        showSearchThisAreaButton = true
      }
    }

    // Programmatic move keeps "Follow my location" active
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(37.7749, -122.4194), 14f))
    assertThat(isFollowingUserLocation).isTrue()
    assertThat(showSearchThisAreaButton).isFalse()

    // Simulated user pan disengages follow mode and reveals "Search this area" on idle
    map.simulateUserCameraMove(LatLng(37.7850, -122.4050))
    assertThat(isFollowingUserLocation).isFalse()
    assertThat(showSearchThisAreaButton).isTrue()

    // 2. Clamp camera target to a service area & verify dark mode / styling / accessibility
    val sfServiceArea = LatLngBounds(LatLng(37.70, -122.52), LatLng(37.82, -122.36))
    val customStyle = MapStyleOptions("[{\"featureType\":\"poi\",\"stylers\":[{\"visibility\":\"off\"}]}]")

    map.setLatLngBoundsForCameraTarget(sfServiceArea)
    map.mapColorScheme = MapColorScheme.DARK
    map.setMapStyle(customStyle)
    map.setContentDescription("San Francisco Service Area Map")

    // Attempting to pan north of the service area clamps the camera target to the boundary
    map.simulateUserCameraMove(LatLng(38.50, -122.40))
    assertThat(map.cameraPosition.target.latitude).isEqualTo(37.82)

    GoogleMapSubject.assertThat(map)
      .hasMapColorScheme(MapColorScheme.DARK)
      .hasMapStyle(customStyle)
      .hasCameraTargetBounds(sfServiceArea)
      .hasContentDescription("San Francisco Service Area Map")
    // [END robolectric_user_gesture_and_styling]

    assertThat(map.mapStyle).isEqualTo(customStyle)
    assertThat(map.cameraTargetBounds).isEqualTo(sfServiceArea)
    assertThat(map.contentDescription).isEqualTo("San Francisco Service Area Map")
  }

  @Test
  fun testLatLngSubjectKotlin() {
    val sf = LatLng(37.7749, -122.4194)
    val nearby = LatLng(37.7750, -122.4193) // ~14 meters away
    val far = LatLng(37.7800, -122.4100) // ~1km away

    // Kotlin DSL syntax using import
    LatLngSubject.assertThat(sf)
      .isWithin(20.meter)
      .of(nearby)

    LatLngSubject.assertThat(sf)
      .isWithin(0.02.kilometer)
      .of(nearby)

    // Should fail
    try {
      LatLngSubject.assertThat(sf)
        .isWithin(5.meters)
        .of(nearby)
      throw AssertionError("Expected failure")
    } catch (expected: AssertionError) {
      // Expected
    }

    try {
      LatLngSubject.assertThat(sf)
        .isWithin(500.meters)
        .of(far)
      throw AssertionError("Expected failure")
    } catch (expected: AssertionError) {
      // Expected
    }
  }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@EnableMapsShadows
class EnableMapsShadowsAnnotationTest {

  @Test
  fun testMapsShadowsAutoConfigured() {
    val activity = Robolectric.buildActivity(Activity::class.java).create().get()
    val mapView = MapView(activity)
    mapView.onCreate(Bundle())

    val shadow = Shadow.extract(mapView) as? ShadowMapView
    assertThat(shadow).isNotNull()
    assertThat(shadow?.isOnCreateCalled).isTrue()
  }

  @Test
  fun testBitmapDescriptorFactoryAndMapsInitializerInKotlin() {
    val activity = Robolectric.buildActivity(Activity::class.java).create().get()
    val initResult = MapsInitializer.initialize(activity)
    assertThat(initResult).isEqualTo(0)

    val defaultMarker = BitmapDescriptorFactory.defaultMarker()
    assertThat(defaultMarker).isNotNull()

    val coloredMarker = BitmapDescriptorFactory.defaultMarker(
      BitmapDescriptorFactory.HUE_GREEN,
    )
    assertThat(coloredMarker).isNotNull()

    val bmp = Bitmap.createBitmap(12, 12, Bitmap.Config.ARGB_8888)
    val bmpDescriptor = BitmapDescriptorFactory.fromBitmap(bmp)
    assertThat(bmpDescriptor).isNotNull()
  }

  @Test
  fun testSupportMapFragmentInKotlin() {
    val fragment = SupportMapFragment.newInstance()
    val mapRef = AtomicReference<GoogleMap>()
    fragment.getMapAsync { mapRef.set(it) }
    ShadowLooper.idleMainLooper()
    assertThat(mapRef.get()).isNotNull()
  }

  @Test
  fun testOverlaysAndProjectionInKotlin() {
    val activity = Robolectric.buildActivity(Activity::class.java).create().get()
    val mapView = MapView(activity)
    mapView.measure(
      View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
    )
    mapView.layout(0, 0, 100, 100)
    mapView.onCreate(Bundle())

    val mapRef = AtomicReference<GoogleMap>()
    mapView.getMapAsync { mapRef.set(it) }
    ShadowLooper.idleMainLooper()
    val map = mapRef.get()

    val center = LatLng(37.7749, -122.4194)
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 12f))

    val point = map.projection.toScreenLocation(center)
    val roundTrip = map.projection.fromScreenLocation(point)
    LatLngSubject.assertThat(roundTrip).isWithin(1.0.meters).of(center)

    val marker = map.addMarker(
      MarkerOptions()
        .position(center)
        .title("SF")
        .snippet("City by the Bay")
        .draggable(true),
    )
    marker?.tag = "MyMarker"
    assertThat(marker?.tag).isEqualTo("MyMarker")

    val shadowMap = Shadow.extract(map) as ShadowGoogleMap
    assertThat(shadowMap.markers).contains(marker)

    marker?.remove()
    assertThat(shadowMap.markers).doesNotContain(marker)
  }

  @Test
  fun testFindMapViewInActivityOrComposeHierarchy() {
    val activity = Robolectric.buildActivity(Activity::class.java).create().get()
    val outerContainer = FrameLayout(activity)
    val innerContainer = FrameLayout(activity)
    val embeddedMapView = MapView(activity).apply {
      onCreate(Bundle())
    }
    innerContainer.addView(embeddedMapView)
    outerContainer.addView(innerContainer)
    activity.setContentView(outerContainer)

    // [START robolectric_view_hierarchy_lookup]
    // Locate the hosted MapView inside an Activity or Jetpack Compose view hierarchy
    val mapView = activity.window.decorView.requireMapView()
    val map = mapView.getMapBlocking()

    val seattle = LatLng(47.6062, -122.3321)
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(seattle, 13f))
    GoogleMapSubject.assertThat(map)
      .isCenteredOn(seattle, tolerance = 5.meters)
      .hasZoom(13f)
    // [END robolectric_view_hierarchy_lookup]
  }
}
