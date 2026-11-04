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

package com.google.maps.android.robolectric.truth

import android.graphics.Color
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout
import com.google.maps.android.robolectric.ktx.activeInfoWindowMarker
import com.google.maps.android.robolectric.ktx.cameraTargetBounds
import com.google.maps.android.robolectric.ktx.circles
import com.google.maps.android.robolectric.ktx.contentDescription
import com.google.maps.android.robolectric.ktx.findMarker
import com.google.maps.android.robolectric.ktx.groundOverlays
import com.google.maps.android.robolectric.ktx.hexColor
import com.google.maps.android.robolectric.ktx.mapStyle
import com.google.maps.android.robolectric.ktx.markers
import com.google.maps.android.robolectric.ktx.pixelColorAt
import com.google.maps.android.robolectric.ktx.polygons
import com.google.maps.android.robolectric.ktx.polylines
import com.google.maps.android.robolectric.ktx.shadow
import com.google.maps.android.robolectric.ktx.tileOverlays
import com.google.maps.android.robolectric.ktx.visibleBounds

// [START maps_robolectric_google_map_subject]
/**
 * Comprehensive Google Truth [Subject] for asserting on a [GoogleMap] instance.
 *
 * Designed for maximum succinctness: a single `assertThat(map)` chain can verify camera coordinates,
 * zoom levels, marker presence/metadata, active info windows, shape overlays, and rendered canvas
 * pixel colors.
 */
class GoogleMapSubject private constructor(
  metadata: FailureMetadata,
  private val actual: GoogleMap?,
) : Subject(metadata, actual) {

  /** Returns a [CameraPositionSubject] for the map's current camera state. */
  fun cameraPosition(): CameraPositionSubject {
    isNotNull()
    return check("cameraPosition").about(CameraPositionSubject.cameraPositions()).that(actual!!.cameraPosition)
  }

  /** Returns a [LatLngSubject] for the map's current camera target coordinate. */
  fun cameraTarget(): LatLngSubject {
    isNotNull()
    return check("cameraPosition.target").about(LatLngSubject.latLngs()).that(actual!!.cameraPosition.target)
  }

  /** Asserts that the map camera target is within [tolerance] of [expectedTarget]. */
  @JvmOverloads
  fun isCenteredOn(expectedTarget: LatLng, tolerance: Distance = 5.meters): GoogleMapSubject = apply {
    cameraTarget().isWithin(tolerance).of(expectedTarget)
  }

  /** Fluent tolerance builder for the map's camera target. */
  fun hasCameraTargetWithin(tolerance: Distance): LatLngSubject.LatLngTolerance = cameraTarget().isWithin(tolerance)

  /** Asserts that the map camera zoom level equals [expectedZoom] within [tolerance]. */
  @JvmOverloads
  fun hasZoom(expectedZoom: Float, tolerance: Float = 0.01f): GoogleMapSubject = apply {
    isNotNull()
    check("cameraPosition.zoom").that(actual!!.cameraPosition.zoom).isWithin(tolerance).of(expectedZoom)
  }

  /** Returns a [LatLngBoundsSubject] for the map's current visible viewport bounds. */
  fun visibleBounds(): LatLngBoundsSubject {
    isNotNull()
    return check("visibleBounds").about(LatLngBoundsSubject.latLngBounds()).that(actual!!.visibleBounds)
  }

  /** Asserts that [point] lies inside the map's current visible region bounds. */
  fun showsOnScreen(point: LatLng): GoogleMapSubject = apply { visibleBounds().contains(point) }

  /** Asserts that [point] lies outside the map's current visible region bounds. */
  fun doesNotShowOnScreen(point: LatLng): GoogleMapSubject = apply { visibleBounds().doesNotContain(point) }

  /** Asserts that the map currently has [expectedCount] active markers. */
  fun hasMarkerCount(expectedCount: Int): GoogleMapSubject = apply {
    isNotNull()
    check("markers.size").that(actual!!.markers).hasSize(expectedCount)
  }

  /** Asserts that the map currently has zero markers. */
  fun hasNoMarkers(): GoogleMapSubject = apply {
    isNotNull()
    check("markers").that(actual!!.markers).isEmpty()
  }

  /**
   * Asserts that the map contains a marker with [expectedTitle] and returns a [MarkerSubject]
   * scoped to that marker for further chaining.
   */
  fun hasMarkerWithTitle(expectedTitle: String): MarkerSubject {
    isNotNull()
    val marker = actual!!.findMarker(title = expectedTitle)
    if (marker == null) {
      failWithoutActual(
        Fact.fact("expected map to have marker with title", expectedTitle),
        Fact.fact("available markers", actual.markers.map { it.shadow }),
      )
    }
    return check("markerWithTitle($expectedTitle)").about(MarkerSubject.markers()).that(marker)
  }

  /**
   * Asserts that the map contains a marker with [expectedTag] and returns a [MarkerSubject]
   * scoped to that marker for further chaining.
   */
  fun hasMarkerWithTag(expectedTag: Any): MarkerSubject {
    isNotNull()
    val marker = actual!!.findMarker(tag = expectedTag)
    if (marker == null) {
      failWithoutActual(
        Fact.fact("expected map to have marker with tag", expectedTag),
        Fact.fact("available markers", actual.markers.map { it.shadow }),
      )
    }
    return check("markerWithTag($expectedTag)").about(MarkerSubject.markers()).that(marker)
  }

  /**
   * Asserts that the map contains a marker within [tolerance] of [expectedPosition] and returns
   * a [MarkerSubject] scoped to that marker.
   */
  @JvmOverloads
  fun hasMarkerNear(expectedPosition: LatLng, tolerance: Distance = 10.meters): MarkerSubject {
    isNotNull()
    val marker = actual!!.findMarker(near = expectedPosition, tolerance = tolerance)
    if (marker == null) {
      failWithoutActual(
        Fact.fact("expected map to have marker within", tolerance),
        Fact.fact("of", expectedPosition),
        Fact.fact("available markers", actual.markers.map { it.shadow }),
      )
    }
    return check("markerNear($expectedPosition)").about(MarkerSubject.markers()).that(marker)
  }

  /** Asserts that the marker with [expectedTitle] currently has its info window shown. */
  fun hasActiveInfoWindowWithTitle(expectedTitle: String): GoogleMapSubject {
    isNotNull()
    val active = actual!!.activeInfoWindowMarker
    if (active == null || active.title != expectedTitle) {
      failWithoutActual(
        Fact.fact("expected active info window with title", expectedTitle),
        Fact.fact("actual active info window marker", active),
      )
    }
    return this
  }

  /** Asserts that no marker currently has an open info window on this map. */
  fun hasNoActiveInfoWindow(): GoogleMapSubject = apply {
    isNotNull()
    check("activeInfoWindowMarker").that(actual!!.activeInfoWindowMarker).isNull()
  }

  /** Asserts that the map has [expectedCount] polygons. */
  fun hasPolygonCount(expectedCount: Int): GoogleMapSubject = apply {
    isNotNull()
    check("polygons.size").that(actual!!.polygons).hasSize(expectedCount)
  }

  /** Returns a [PolygonSubject] for the first polygon on the map. */
  fun firstPolygon(): PolygonSubject {
    isNotNull()
    val first = actual!!.polygons.firstOrNull()
    if (first == null) {
      failWithoutActual(Fact.simpleFact("expected map to have at least one polygon, but polygons was empty"))
    }
    return check("polygons.first()").about(PolygonSubject.polygons()).that(first)
  }

  /** Asserts that the map has [expectedCount] polylines. */
  fun hasPolylineCount(expectedCount: Int): GoogleMapSubject = apply {
    isNotNull()
    check("polylines.size").that(actual!!.polylines).hasSize(expectedCount)
  }

  /** Returns a [PolylineSubject] for the first polyline on the map. */
  fun firstPolyline(): PolylineSubject {
    isNotNull()
    val first = actual!!.polylines.firstOrNull()
    if (first == null) {
      failWithoutActual(Fact.simpleFact("expected map to have at least one polyline, but polylines was empty"))
    }
    return check("polylines.first()").about(PolylineSubject.polylines()).that(first)
  }

  /** Asserts that the map has [expectedCount] circles. */
  fun hasCircleCount(expectedCount: Int): GoogleMapSubject = apply {
    isNotNull()
    check("circles.size").that(actual!!.circles).hasSize(expectedCount)
  }

  /** Returns a [CircleSubject] for the first circle on the map. */
  fun firstCircle(): CircleSubject {
    isNotNull()
    val first = actual!!.circles.firstOrNull()
    if (first == null) {
      failWithoutActual(Fact.simpleFact("expected map to have at least one circle, but circles was empty"))
    }
    return check("circles.first()").about(CircleSubject.circles()).that(first)
  }

  /** Asserts that the map has [expectedCount] ground overlays. */
  fun hasGroundOverlayCount(expectedCount: Int): GoogleMapSubject = apply {
    isNotNull()
    check("groundOverlays.size").that(actual!!.groundOverlays).hasSize(expectedCount)
  }

  /** Asserts that the map has [expectedCount] tile overlays. */
  fun hasTileOverlayCount(expectedCount: Int): GoogleMapSubject = apply {
    isNotNull()
    check("tileOverlays.size").that(actual!!.tileOverlays).hasSize(expectedCount)
  }

  /** Asserts that the map's type equals [expectedMapType]. */
  fun hasMapType(expectedMapType: Int): GoogleMapSubject = apply {
    isNotNull()
    check("mapType").that(actual!!.mapType).isEqualTo(expectedMapType)
  }

  /** Asserts that the map's color scheme equals [expectedScheme] (e.g. `MapColorScheme.DARK`). */
  fun hasMapColorScheme(expectedScheme: Int): GoogleMapSubject = apply {
    isNotNull()
    check("mapColorScheme").that(actual!!.mapColorScheme).isEqualTo(expectedScheme)
  }

  /** Asserts that the map's style options equal [expectedStyle]. */
  fun hasMapStyle(expectedStyle: MapStyleOptions?): GoogleMapSubject = apply {
    isNotNull()
    check("mapStyle").that(actual!!.mapStyle).isEqualTo(expectedStyle)
  }

  /** Asserts that the map's camera target clamping bounds equal [expectedBounds]. */
  fun hasCameraTargetBounds(expectedBounds: LatLngBounds?): GoogleMapSubject = apply {
    isNotNull()
    check("cameraTargetBounds").that(actual!!.cameraTargetBounds).isEqualTo(expectedBounds)
  }

  /** Asserts that the map's accessibility content description equals [expectedDescription]. */
  fun hasContentDescription(expectedDescription: String?): GoogleMapSubject = apply {
    isNotNull()
    check("contentDescription").that(actual!!.contentDescription).isEqualTo(expectedDescription)
  }

  /**
   * Renders the map to an in-memory bitmap and asserts that the pixel at the projected screen
   * coordinates of [latLng] equals [expectedColor].
   */
  fun hasPixelColorAt(latLng: LatLng, expectedColor: Int): GoogleMapSubject {
    isNotNull()
    val actualColor = actual!!.pixelColorAt(latLng)
    if (actualColor != expectedColor) {
      val screenPoint = actual.projection.toScreenLocation(latLng)
      failWithoutActual(
        Fact.fact("expected pixel color at", latLng),
        Fact.fact("projected screen point", "(${screenPoint.x}, ${screenPoint.y})"),
        Fact.fact("expected color", expectedColor.hexColor),
        Fact.fact("but was", actualColor.hexColor),
      )
    }
    return this
  }

  /**
   * Renders the map to an in-memory bitmap and asserts that the pixel at screen coordinates
   * ([x], [y]) equals [expectedColor].
   */
  fun hasPixelColorAt(x: Int, y: Int, expectedColor: Int): GoogleMapSubject {
    isNotNull()
    val actualColor = actual!!.pixelColorAt(x, y)
    if (actualColor != expectedColor) {
      failWithoutActual(
        Fact.fact("expected pixel color at screen point", "($x, $y)"),
        Fact.fact("expected color", expectedColor.hexColor),
        Fact.fact("but was", actualColor.hexColor),
      )
    }
    return this
  }

  /**
   * Renders the map to an in-memory bitmap and asserts that an overlay has been drawn over the
   * default background color at [latLng].
   */
  @JvmOverloads
  fun hasNonBackgroundPixelAt(
    latLng: LatLng,
    backgroundColor: Int = Color.LTGRAY,
  ): GoogleMapSubject {
    isNotNull()
    val actualColor = actual!!.pixelColorAt(latLng)
    if (actualColor == backgroundColor || Color.alpha(actualColor) == 0) {
      val screenPoint = actual.projection.toScreenLocation(latLng)
      failWithoutActual(
        Fact.fact("expected non-background pixel at", latLng),
        Fact.fact("projected screen point", "(${screenPoint.x}, ${screenPoint.y})"),
        Fact.fact("background color", backgroundColor.hexColor),
        Fact.fact("actual pixel color", actualColor.hexColor),
      )
    }
    return this
  }

  companion object {
    @JvmStatic
    fun googleMaps(): Factory<GoogleMapSubject, GoogleMap> = Factory { metadata, actual -> GoogleMapSubject(metadata, actual) }

    @JvmStatic
    fun assertThat(actual: GoogleMap?): GoogleMapSubject = assertAbout(googleMaps()).that(actual)
  }
}
// [END maps_robolectric_google_map_subject]
