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

package com.google.maps.android.robolectric.maps3d.truth

import com.google.android.gms.maps3d.GoogleMap3D
import com.google.android.gms.maps3d.model.Map3DMode
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth
import com.google.maps.android.robolectric.maps3d.ktx.shadow
import kotlin.math.abs

/**
 * Entry point for Google Truth assertions on Maps 3D SDK types in Robolectric tests.
 */
object Maps3DTruth {
  /** Begins a fluent Truth assertion chain on a shadowed [GoogleMap3D]. */
  @JvmStatic
  fun assertThat(actual: GoogleMap3D?): GoogleMap3DSubject = Truth.assertAbout(GoogleMap3DSubject.googleMap3Ds()).that(actual)
}

/** Top-level Kotlin alias for [Maps3DTruth.assertThat]. */
fun assertThat(actual: GoogleMap3D?): GoogleMap3DSubject = Maps3DTruth.assertThat(actual)

/**
 * Google Truth [Subject] for asserting camera, map mode, and 3D overlay state on a shadowed
 * [GoogleMap3D].
 */
class GoogleMap3DSubject private constructor(
  metadata: FailureMetadata,
  private val actual: GoogleMap3D?,
) : Subject(metadata, actual) {

  /** Asserts that the map's current [Map3DMode] equals [expectedMode]. */
  fun hasMapMode(@Map3DMode expectedMode: Int) {
    val map = requireActual()
    check("mapMode").that(map.getMapMode()).isEqualTo(expectedMode)
  }

  /**
   * Asserts that the current camera center's latitude and longitude are within [tolerance] degrees
   * of ([expectedLatitude], [expectedLongitude]).
   */
  @JvmOverloads
  fun hasCameraCenterNear(
    expectedLatitude: Double,
    expectedLongitude: Double,
    tolerance: Double = 1e-4,
  ) {
    val camera = requireActual().getCamera()
    check("camera").that(camera).isNotNull()
    val center = checkNotNull(camera).center
    check("camera.center.latitude").that(center.latitude).isWithin(tolerance).of(expectedLatitude)
    check("camera.center.longitude").that(center.longitude).isWithin(tolerance).of(expectedLongitude)
  }

  /** Asserts that the current camera center's altitude is within [tolerance] meters of [expectedAltitude]. */
  @JvmOverloads
  fun hasCameraAltitudeWithin(expectedAltitude: Double, tolerance: Double = 0.5) {
    val camera = requireActual().getCamera()
    check("camera").that(camera).isNotNull()
    val altitude = checkNotNull(camera).center.altitude
    check("camera.center.altitude").that(altitude).isWithin(tolerance).of(expectedAltitude)
  }

  /** Asserts that the map currently has exactly [expectedCount] active markers. */
  fun hasMarkerCount(expectedCount: Int) {
    check("markers").that(requireActual().shadow.markers).hasSize(expectedCount)
  }

  /** Asserts that an active marker with the given [id] exists on the map. */
  fun hasMarker(id: String) {
    check("marker(%s)", id).that(requireActual().shadow.findMarkerById(id)).isNotNull()
  }

  /** Asserts that no active marker with the given [id] exists on the map. */
  fun doesNotHaveMarker(id: String) {
    check("marker(%s)", id).that(requireActual().shadow.findMarkerById(id)).isNull()
  }

  /** Asserts that the map currently has exactly [expectedCount] active polylines. */
  fun hasPolylineCount(expectedCount: Int) {
    check("polylines").that(requireActual().shadow.polylines).hasSize(expectedCount)
  }

  /** Asserts that an active polyline with the given [id] exists on the map. */
  fun hasPolyline(id: String) {
    check("polyline(%s)", id).that(requireActual().shadow.findPolylineById(id)).isNotNull()
  }

  /** Asserts that no active polyline with the given [id] exists on the map. */
  fun doesNotHavePolyline(id: String) {
    check("polyline(%s)", id).that(requireActual().shadow.findPolylineById(id)).isNull()
  }

  /** Asserts that the map currently has exactly [expectedCount] active polygons. */
  fun hasPolygonCount(expectedCount: Int) {
    check("polygons").that(requireActual().shadow.polygons).hasSize(expectedCount)
  }

  /** Asserts that an active polygon with the given [id] exists on the map. */
  fun hasPolygon(id: String) {
    check("polygon(%s)", id).that(requireActual().shadow.findPolygonById(id)).isNotNull()
  }

  /** Asserts that the map currently has exactly [expectedCount] active 3D models. */
  fun hasModelCount(expectedCount: Int) {
    check("models").that(requireActual().shadow.models).hasSize(expectedCount)
  }

  /** Asserts that an active 3D model with the given [id] exists on the map. */
  fun hasModel(id: String) {
    check("model(%s)", id).that(requireActual().shadow.findModelById(id)).isNotNull()
  }

  /** Asserts that no active 3D model with the given [id] exists on the map. */
  fun doesNotHaveModel(id: String) {
    check("model(%s)", id).that(requireActual().shadow.findModelById(id)).isNull()
  }

  /** Asserts that the map currently has exactly [expectedCount] active popovers. */
  fun hasPopoverCount(expectedCount: Int) {
    check("popovers").that(requireActual().shadow.popovers).hasSize(expectedCount)
  }

  /** Asserts that all overlay registries (markers, polylines, polygons, models, popovers) are empty. */
  fun hasNoOverlays() {
    val s = requireActual().shadow
    check("markers").that(s.markers).isEmpty()
    check("polylines").that(s.polylines).isEmpty()
    check("polygons").that(s.polygons).isEmpty()
    check("models").that(s.models).isEmpty()
    check("popovers").that(s.popovers).isEmpty()
  }

  /** Asserts that the active 3D model with [id] is positioned within [tolerance] degrees of ([latitude], [longitude]). */
  @JvmOverloads
  fun hasModelNear(
    id: String,
    latitude: Double,
    longitude: Double,
    tolerance: Double = 1e-4,
  ) {
    val model = requireActual().shadow.findModelById(id)
    check("model(%s)", id).that(model).isNotNull()
    val pos = requireNotNull(model).position
    check("model(%s).position.latitude", id).that(abs(pos.latitude - latitude)).isAtMost(tolerance)
    check("model(%s).position.longitude", id).that(abs(pos.longitude - longitude)).isAtMost(tolerance)
  }

  private fun requireActual(): GoogleMap3D {
    isNotNull()
    return checkNotNull(actual)
  }

  companion object {
    /** Factory for creating [GoogleMap3DSubject] instances via `Truth.assertAbout(...)`. */
    @JvmStatic
    fun googleMap3Ds(): Factory<GoogleMap3DSubject, GoogleMap3D> = Factory { metadata, actual -> GoogleMap3DSubject(metadata, actual) }
  }
}
