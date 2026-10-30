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

import android.graphics.Color
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.GroundOverlayOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.maps.robolectric.annotation.EnableMapsShadows
import com.google.android.maps.robolectric.ktx.activeInfoWindowMarker
import com.google.android.maps.robolectric.ktx.cameraBearing
import com.google.android.maps.robolectric.ktx.cameraTarget
import com.google.android.maps.robolectric.ktx.cameraTilt
import com.google.android.maps.robolectric.ktx.cameraZoom
import com.google.android.maps.robolectric.ktx.circles
import com.google.android.maps.robolectric.ktx.click
import com.google.android.maps.robolectric.ktx.clickInfoWindow
import com.google.android.maps.robolectric.ktx.clickMap
import com.google.android.maps.robolectric.ktx.clickMarker
import com.google.android.maps.robolectric.ktx.createTestMapView
import com.google.android.maps.robolectric.ktx.dragTo
import com.google.android.maps.robolectric.ktx.getMapBlocking
import com.google.android.maps.robolectric.ktx.groundOverlays
import com.google.android.maps.robolectric.ktx.hexColor
import com.google.android.maps.robolectric.ktx.longClickInfoWindow
import com.google.android.maps.robolectric.ktx.longClickMap
import com.google.android.maps.robolectric.ktx.mapView
import com.google.android.maps.robolectric.ktx.markerWithTag
import com.google.android.maps.robolectric.ktx.markerWithTitle
import com.google.android.maps.robolectric.ktx.markers
import com.google.android.maps.robolectric.ktx.pixelColorAt
import com.google.android.maps.robolectric.ktx.pixelHexAt
import com.google.android.maps.robolectric.ktx.polygons
import com.google.android.maps.robolectric.ktx.polylines
import com.google.android.maps.robolectric.ktx.renderToBitmap
import com.google.android.maps.robolectric.ktx.requireMarker
import com.google.android.maps.robolectric.ktx.shadow
import com.google.android.maps.robolectric.ktx.tileOverlays
import com.google.android.maps.robolectric.ktx.visibleBounds
import com.google.android.maps.robolectric.ktx.withTestMap
import com.google.android.maps.robolectric.powerassert.assertGeo
import com.google.android.maps.robolectric.powerassert.assertMap
import com.google.android.maps.robolectric.powerassert.assertMarker
import com.google.android.maps.robolectric.powerassert.connects
import com.google.android.maps.robolectric.powerassert.contains
import com.google.android.maps.robolectric.powerassert.hasMarker
import com.google.android.maps.robolectric.powerassert.hasOverlayPixelAt
import com.google.android.maps.robolectric.powerassert.hasVertexNear
import com.google.android.maps.robolectric.powerassert.isCenteredOn
import com.google.android.maps.robolectric.powerassert.isInfoWindowShownFor
import com.google.android.maps.robolectric.powerassert.isShowing
import com.google.android.maps.robolectric.powerassert.matches
import com.google.android.maps.robolectric.truth.assertThat
import com.google.android.maps.robolectric.truth.distanceTo
import com.google.android.maps.robolectric.truth.feet
import com.google.android.maps.robolectric.truth.foot
import com.google.android.maps.robolectric.truth.isNear
import com.google.android.maps.robolectric.truth.isNotWithin
import com.google.android.maps.robolectric.truth.isWithin
import com.google.android.maps.robolectric.truth.kilometer
import com.google.android.maps.robolectric.truth.kilometers
import com.google.android.maps.robolectric.truth.meter
import com.google.android.maps.robolectric.truth.meters
import com.google.android.maps.robolectric.truth.mile
import com.google.android.maps.robolectric.truth.miles
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.google.common.truth.Truth.assertThat as truthAssertThat

/**
 * Parallel test suite comparing **Google Truth** domain subjects against **Kotlin Power-Assert**
 * expressions for the same Google Maps testing scenarios.
 */
// [START maps_robolectric_parallel_truth_vs_power_assert]
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@EnableMapsShadows
class LazyAssertionsComparisonTest {

  private val boulder = LatLng(40.0150, -105.2705)
  private val pearlStreet = LatLng(40.0176, -105.2797) // ~835m from boulder
  private val denver = LatLng(39.7392, -104.9903) // ~39km from boulder

  // [START maps_robolectric_truth_domain_subjects]
  @Test
  fun cameraAndMarkers_googleTruthStyle() = withTestMap {
    moveCamera(CameraUpdateFactory.newLatLngZoom(boulder, 14f))

    addMarker(
      MarkerOptions()
        .position(boulder)
        .title("Boulder HQ")
        .snippet("Open")
        .draggable(true)
        .alpha(0.9f),
    )?.apply {
      tag = "hq_1"
      showInfoWindow()
    }

    assertThat(this)
      .isCenteredOn(boulder, tolerance = 5.meters)
      .hasZoom(14f)
      .showsOnScreen(boulder)
      .doesNotShowOnScreen(denver)
      .hasMarkerCount(1)
      .hasActiveInfoWindowWithTitle("Boulder HQ")

    assertThat(this)
      .hasMarkerWithTitle("Boulder HQ")
      .isAt(boulder, tolerance = 1.meters)
      .hasSnippet("Open")
      .hasTag("hq_1")
      .isDraggable()
      .isVisible()
      .isInfoWindowShown()
      .hasAlpha(0.9f)
  }
  // [END maps_robolectric_truth_domain_subjects]

  // [START maps_robolectric_power_assert]
  @Test
  fun cameraAndMarkers_kotlinPowerAssertStyle() = withTestMap {
    moveCamera(CameraUpdateFactory.newLatLngZoom(boulder, 14f))

    val marker = addMarker(
      MarkerOptions()
        .position(boulder)
        .title("Boulder HQ")
        .snippet("Open")
        .draggable(true)
        .alpha(0.9f),
    )!!.apply {
      tag = "hq_1"
      showInfoWindow()
    }

    assertMap(
      (cameraTarget distanceTo boulder) <= 5.meters &&
        cameraZoom == 14f &&
        boulder in visibleBounds &&
        denver !in visibleBounds &&
        markers.size == 1 &&
        isInfoWindowShownFor("Boulder HQ"),
    )

    marker.assertMarker(
      (marker.position distanceTo boulder) <= 1.meters &&
        marker.title == "Boulder HQ" &&
        marker.snippet == "Open" &&
        marker.tag == "hq_1" &&
        marker.isDraggable &&
        marker.isVisible &&
        marker.isInfoWindowShown,
    )
  }
  // [END maps_robolectric_power_assert]

  @Test
  fun shapesAndPixelRendering_googleTruthStyle() = withTestMap(width = 100, height = 100) {
    moveCamera(CameraUpdateFactory.newLatLngZoom(boulder, 13f))

    addCircle(
      CircleOptions()
        .center(boulder)
        .radius(1500.0)
        .fillColor(Color.GREEN)
        .strokeColor(Color.BLUE)
        .strokeWidth(3f),
    ).tag = "zone_boulder"

    addPolygon(
      PolygonOptions()
        .add(
          LatLng(40.010, -105.280),
          LatLng(40.010, -105.260),
          LatLng(40.020, -105.260),
          LatLng(40.020, -105.280),
        )
        .fillColor(Color.RED)
        .strokeColor(Color.BLACK)
        .strokeWidth(2f),
    ).tag = "poly_boulder"

    addPolyline(
      PolylineOptions()
        .add(boulder, pearlStreet)
        .color(Color.MAGENTA)
        .width(4f),
    ).tag = "route_1"

    assertThat(this)
      .hasCircleCount(1)
      .hasPolygonCount(1)
      .hasPolylineCount(1)
      .hasNonBackgroundPixelAt(boulder)

    assertThat(this).firstCircle()
      .isCenteredOn(boulder)
      .hasRadius(1.5.kilometers)
      .contains(pearlStreet)
      .doesNotContain(denver)
      .hasFillColor(Color.GREEN)
      .hasStrokeColor(Color.BLUE)
      .hasStrokeWidth(3f)
      .hasTag("zone_boulder")
      .isVisible()

    assertThat(this).firstPolygon()
      .hasPointCount(4)
      .hasHoleCount(0)
      .hasVertexNear(LatLng(40.010, -105.280))
      .hasFillColor(Color.RED)
      .hasStrokeColor(Color.BLACK)
      .hasStrokeWidth(2f)
      .hasTag("poly_boulder")
      .isVisible()

    assertThat(this).firstPolyline()
      .hasPointCount(2)
      .startsAt(boulder)
      .endsAt(pearlStreet)
      .hasColor(Color.MAGENTA)
      .hasWidth(4f)
      .hasTag("route_1")
      .isVisible()
  }

  @Test
  fun shapesAndPixelRendering_kotlinPowerAssertStyle() = withTestMap(width = 100, height = 100) {
    moveCamera(CameraUpdateFactory.newLatLngZoom(boulder, 13f))

    val circle = addCircle(
      CircleOptions()
        .center(boulder)
        .radius(1500.0)
        .fillColor(Color.GREEN)
        .strokeColor(Color.BLUE)
        .strokeWidth(3f),
    ).apply { tag = "zone_boulder" }

    val polygon = addPolygon(
      PolygonOptions()
        .add(
          LatLng(40.010, -105.280),
          LatLng(40.010, -105.260),
          LatLng(40.020, -105.260),
          LatLng(40.020, -105.280),
        )
        .fillColor(Color.GREEN),
    )

    val polyline = addPolyline(
      PolylineOptions()
        .add(boulder, pearlStreet)
        .color(Color.GREEN)
        .width(4f),
    )

    assertMap(
      circles.size == 1 &&
        polygons.size == 1 &&
        polylines.size == 1 &&
        pearlStreet in circle &&
        denver !in circle &&
        polygon.hasVertexNear(LatLng(40.010, -105.280)) &&
        polyline.connects(boulder, pearlStreet) &&
        hasOverlayPixelAt(boulder) &&
        pixelHexAt(boulder) == Color.GREEN.hexColor,
    )
  }

  @Test
  fun powerAssertFailure_producesRichDiagnosticDiagramWithDomainValues() = withTestMap {
    moveCamera(CameraUpdateFactory.newLatLngZoom(boulder, 12f))
    val marker = addMarker(MarkerOptions().position(pearlStreet).title("Pearl St"))!!

    val mapError = assertThrows(AssertionError::class.java) {
      assertMap((marker.position distanceTo boulder) <= 100.meters)
    }
    // Verify that the Kotlin Power-Assert compiler plugin instrumented the expression
    // and embedded the computed Distance ("m") and GoogleMap state into the message.
    truthAssertThat(mapError.message).contains("distanceTo")
    truthAssertThat(mapError.message).contains("100.0m")
    truthAssertThat(mapError.message).contains("Map state: GoogleMap(")

    val markerError = assertThrows(AssertionError::class.java) {
      marker.assertMarker(marker.title == "Wrong Title")
    }
    truthAssertThat(markerError.message).contains("Pearl St")
    truthAssertThat(markerError.message).contains("Marker state: Marker(")

    val geoError = assertThrows(AssertionError::class.java) {
      assertGeo(denver in visibleBounds)
    }
    truthAssertThat(geoError.message).contains("visibleBounds")
  }

  @Test
  fun truthSubjectsAndKtxHelpers_coverInteractionsAndEdgeCases() = withTestMap(width = 100, height = 100) {
    // Distance arithmetic & units
    val d1 = 500.meters + 0.5.kilometers
    assertGeo(d1 == 1.kilometers && 1.miles > 1.kilometers && 100.feet < 50.meters)
    assertThat(boulder).isNotWithin(500.meters).of(pearlStreet)
    assertThat(boulder).isWithinBounds(LatLngBounds(LatLng(39.0, -106.0), LatLng(41.0, -104.0)))

    // Camera position subject details
    moveCamera(CameraUpdateFactory.newLatLngZoom(boulder, 15f))
    assertThat(cameraPosition)
      .isAt(boulder, expectedZoom = 15f)
      .hasBearing(0f)
      .hasTilt(0f)
    assertGeo(cameraBearing == 0f && cameraTilt == 0f && isCenteredOn(boulder, expectedZoom = 15f))

    // Marker interactions via KTX
    val marker = addMarker(
      MarkerOptions()
        .position(boulder)
        .title("Store 1")
        .snippet("Open")
        .flat(true)
        .rotation(45f)
        .zIndex(2f),
    )!!.apply { tag = "tag_1" }

    assertThat(marker)
      .hasTitle("Store 1")
      .isFlat()
      .hasRotation(45f)
      .hasZIndex(2f)
      .isInfoWindowHidden()

    var markerClicked = false
    var infoWindowClicked = false
    var infoWindowLongClicked = false
    var draggedEndPos: LatLng? = null

    setOnMarkerClickListener {
      markerClicked = true
      it.showInfoWindow()
      true
    }
    setOnInfoWindowClickListener { infoWindowClicked = true }
    setOnInfoWindowLongClickListener { infoWindowLongClicked = true }
    setOnMarkerDragListener(
      object : OnMarkerDragListener {
        override fun onMarkerDragStart(m: Marker) {}
        override fun onMarkerDrag(m: Marker) {}
        override fun onMarkerDragEnd(m: Marker) {
          draggedEndPos = m.position
        }
      },
    )

    clickMarker("Store 1")
    marker.clickInfoWindow()
    marker.longClickInfoWindow()
    marker.dragTo(pearlStreet)

    assertGeo(
      markerClicked &&
        infoWindowClicked &&
        infoWindowLongClicked &&
        draggedEndPos == pearlStreet &&
        activeInfoWindowMarker == marker &&
        markerWithTitle("Store 1") == marker &&
        markerWithTag("tag_1") == marker &&
        hasMarker(title = "Store 1", tag = "tag_1", near = pearlStreet, infoWindowShown = true) &&
        marker.matches(title = "Store 1", snippet = "Open", tag = "tag_1", near = pearlStreet, infoWindowShown = true, visible = true),
    )

    // Map click, long-click, and overlay clicks
    var mapClickedAt: LatLng? = null
    var mapLongClickedAt: LatLng? = null
    setOnMapClickListener { mapClickedAt = it }
    setOnMapLongClickListener { mapLongClickedAt = it }
    clickMap(boulder)
    longClickMap(denver)
    assertGeo(mapClickedAt == boulder && mapLongClickedAt == denver && isShowing(boulder))

    var polyClicked = false
    var lineClicked = false
    var circleClicked = false
    var groundClicked = false
    setOnPolygonClickListener { polyClicked = true }
    setOnPolylineClickListener { lineClicked = true }
    setOnCircleClickListener { circleClicked = true }
    setOnGroundOverlayClickListener { groundClicked = true }

    addPolygon(PolygonOptions().add(boulder, pearlStreet, denver).clickable(true)).click()
    addPolyline(PolylineOptions().add(boulder, pearlStreet).clickable(true)).click()
    addCircle(CircleOptions().center(boulder).radius(100.0).clickable(true)).click()
    addGroundOverlay(GroundOverlayOptions().position(boulder, 50f, 50f).clickable(true))!!.click()
    addTileOverlay(TileOverlayOptions().zIndex(1f))

    assertThat(this)
      .hasGroundOverlayCount(1)
      .hasTileOverlayCount(1)
    assertGeo(polyClicked && lineClicked && circleClicked && groundClicked && groundOverlays.size == 1 && tileOverlays.size == 1)

    // SupportMapFragment blocking helper
    val fragmentMap = SupportMapFragment.newInstance().getMapBlocking()
    assertThat(fragmentMap).hasNoMarkers().hasNoActiveInfoWindow()
  }

  @Test
  fun comprehensiveSubjectsAndDiagnosticBranches() = withTestMap(width = 100, height = 100) {
    // Distance units, arithmetic, and formatting
    val distKm = (2.kilometer - 500.meter) * 2 / 2
    truthAssertThat(distKm.toString()).contains("1.50km")
    truthAssertThat(1.mile.miles).isWithin(0.01).of(1.0)
    truthAssertThat(1.foot.feet).isWithin(0.01).of(1.0)

    // Empty map failure diagnostics
    assertThrows(AssertionError::class.java) { assertThat(this).firstCircle() }
    assertThrows(AssertionError::class.java) { assertThat(this).firstPolygon() }
    assertThrows(AssertionError::class.java) { assertThat(this).firstPolyline() }
    assertThrows(AssertionError::class.java) { assertThat(this).hasMarkerWithTitle("Missing") }
    assertThrows(AssertionError::class.java) { assertThat(this).hasMarkerWithTag("Missing") }
    assertThrows(AssertionError::class.java) { assertThat(this).hasMarkerNear(boulder) }
    assertThrows(AssertionError::class.java) { assertThat(this).hasActiveInfoWindowWithTitle("Missing") }
    assertThrows(AssertionError::class.java) { requireMarker(title = "Missing") }

    // Camera, MapType, and Bounds subjects
    mapType = GoogleMap.MAP_TYPE_SATELLITE
    moveCamera(CameraUpdateFactory.newLatLngZoom(boulder, 14f))
    assertThat(this)
      .hasMapType(GoogleMap.MAP_TYPE_SATELLITE)
      .hasCameraTargetWithin(5.meters).of(boulder)
    assertThat(this).cameraPosition().hasTargetWithin(5.meters).of(boulder)

    val bounds = LatLngBounds(LatLng(39.5, -105.5), LatLng(40.5, -105.0))
    assertThat(bounds)
      .contains(boulder)
      .doesNotContain(LatLng(38.0, -105.0))
    assertThat(bounds).hasCenterWithin(10.kilometers).of(LatLng(40.0, -105.25))
    assertThat(bounds).southwest().isWithin(1.meters).of(LatLng(39.5, -105.5))
    assertThat(bounds).northeast().isWithin(1.meters).of(LatLng(40.5, -105.0))
    assertThrows(AssertionError::class.java) { assertThat(bounds).contains(LatLng(38.0, -105.0)) }
    assertThrows(AssertionError::class.java) { assertThat(bounds).doesNotContain(boulder) }
    assertThrows(AssertionError::class.java) { assertThat(boulder).isNotWithin(10.meters).of(boulder) }
    assertThrows(AssertionError::class.java) { assertThat(LatLng(38.0, -105.0)).isWithinBounds(bounds) }

    // Hidden / non-draggable marker & direct click overloads
    val hiddenMarker = addMarker(
      MarkerOptions()
        .position(boulder)
        .title("Hidden")
        .visible(false)
        .draggable(false),
    )!!.apply { tag = "hidden_tag" }

    assertThat(this).hasMarkerWithTag("hidden_tag")
      .isNotVisible()
      .isNotDraggable()
    assertThat(this).hasMarkerNear(boulder, 5.meters).hasTitle("Hidden")
    clickMarker(hiddenMarker)
    hiddenMarker.click()

    // Shape subjects via MapsTruth.assertThat(...) and failure branches
    val circle = addCircle(
      CircleOptions()
        .center(boulder)
        .radius(500.0)
        .fillColor(Color.GREEN)
        .strokeColor(Color.BLUE),
    )
    assertThat(circle).hasRadius(500.0)
    assertThrows(AssertionError::class.java) { assertThat(circle).contains(denver) }
    assertThrows(AssertionError::class.java) { assertThat(circle).doesNotContain(boulder) }
    assertThrows(AssertionError::class.java) { assertThat(circle).hasFillColor(Color.RED) }
    assertThrows(AssertionError::class.java) { assertThat(circle).hasStrokeColor(Color.RED) }

    val polygon = addPolygon(
      PolygonOptions()
        .add(boulder, pearlStreet, denver)
        .fillColor(Color.GREEN)
        .strokeColor(Color.BLUE),
    )
    assertThat(polygon).hasPointCount(3)
    assertThrows(AssertionError::class.java) { assertThat(polygon).hasVertexNear(LatLng(0.0, 0.0)) }
    assertThrows(AssertionError::class.java) { assertThat(polygon).hasFillColor(Color.RED) }
    assertThrows(AssertionError::class.java) { assertThat(polygon).hasStrokeColor(Color.RED) }

    val polyline = addPolyline(
      PolylineOptions()
        .add(boulder, pearlStreet)
        .color(Color.GREEN),
    )
    assertThat(polyline).hasPointCount(2)
    assertThrows(AssertionError::class.java) { assertThat(polyline).hasColor(Color.RED) }

    // Pixel assertions on GoogleMapSubject (both pass & fail)
    assertThat(this)
      .hasPixelColorAt(boulder, Color.GREEN)
      .hasPixelColorAt(50, 50, Color.GREEN)
    assertThrows(AssertionError::class.java) { assertThat(this).hasPixelColorAt(boulder, Color.RED) }
    assertThrows(AssertionError::class.java) { assertThat(this).hasPixelColorAt(50, 50, Color.RED) }
    assertThrows(AssertionError::class.java) {
      assertThat(this).hasNonBackgroundPixelAt(boulder, backgroundColor = Color.GREEN)
    }

    // Shadow KTX extractors & headless fragment rendering
    val customView = createTestMapView()
    truthAssertThat(customView.shadow).isNotNull()
    truthAssertThat(mapView?.renderToBitmap()).isNotNull()
    truthAssertThat(uiSettings.shadow).isNotNull()
    truthAssertThat(BitmapDescriptorFactory.defaultMarker().shadow).isNotNull()
    val tile = addTileOverlay(TileOverlayOptions().zIndex(2f))!!
    truthAssertThat(tile.shadow).isNotNull()

    val fragMap = SupportMapFragment.newInstance().getMapBlocking()
    truthAssertThat(fragMap.renderToBitmap(width = 80, height = 80).width).isEqualTo(80)
  }
}
// [END maps_robolectric_parallel_truth_vs_power_assert]
