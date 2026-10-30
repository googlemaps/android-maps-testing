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

package com.google.android.maps.robolectric.truth

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout

// [START maps_robolectric_camera_bounds_subjects]
/**
 * Google Truth [Subject] for fluent assertions on [CameraPosition].
 */
class CameraPositionSubject private constructor(
  metadata: FailureMetadata,
  private val actual: CameraPosition?,
) : Subject(metadata, actual) {

  /** Returns a [LatLngSubject] for the camera's target coordinate. */
  fun target(): LatLngSubject {
    isNotNull()
    return check("target").about(LatLngSubject.latLngs()).that(actual!!.target)
  }

  /** Asserts that the camera target is within [tolerance] of [expected]. */
  fun isCenteredOn(expected: LatLng, tolerance: Distance = 5.meters): CameraPositionSubject {
    target().isWithin(tolerance).of(expected)
    return this
  }

  /** Asserts that the camera target is within [tolerance] of another coordinate. */
  fun hasTargetWithin(tolerance: Distance): LatLngSubject.LatLngTolerance = target().isWithin(tolerance)

  /** Asserts that the camera zoom level equals [expectedZoom] within [tolerance]. */
  @JvmOverloads
  fun hasZoom(expectedZoom: Float, tolerance: Float = 0.01f): CameraPositionSubject {
    isNotNull()
    check("zoom").that(actual!!.zoom).isWithin(tolerance).of(expectedZoom)
    return this
  }

  /** Asserts that the camera bearing equals [expectedBearing] within [tolerance]. */
  @JvmOverloads
  fun hasBearing(expectedBearing: Float, tolerance: Float = 0.01f): CameraPositionSubject {
    isNotNull()
    check("bearing").that(actual!!.bearing).isWithin(tolerance).of(expectedBearing)
    return this
  }

  /** Asserts that the camera tilt equals [expectedTilt] within [tolerance]. */
  @JvmOverloads
  fun hasTilt(expectedTilt: Float, tolerance: Float = 0.01f): CameraPositionSubject {
    isNotNull()
    check("tilt").that(actual!!.tilt).isWithin(tolerance).of(expectedTilt)
    return this
  }

  /** Single-call assertion checking both target coordinate and zoom level. */
  @JvmOverloads
  fun isAt(
    expectedTarget: LatLng,
    expectedZoom: Float,
    tolerance: Distance = 5.meters,
  ): CameraPositionSubject {
    isCenteredOn(expectedTarget, tolerance)
    hasZoom(expectedZoom)
    return this
  }

  companion object {
    @JvmStatic
    fun cameraPositions(): Factory<CameraPositionSubject, CameraPosition> = Factory { metadata, actual -> CameraPositionSubject(metadata, actual) }

    @JvmStatic
    fun assertThat(actual: CameraPosition?): CameraPositionSubject = assertAbout(cameraPositions()).that(actual)
  }
}

/**
 * Google Truth [Subject] for fluent assertions on [LatLngBounds].
 */
class LatLngBoundsSubject private constructor(
  metadata: FailureMetadata,
  private val actual: LatLngBounds?,
) : Subject(metadata, actual) {

  /** Asserts that this bounding box geographically contains [point]. */
  fun contains(point: LatLng): LatLngBoundsSubject {
    isNotNull()
    if (!actual!!.contains(point)) {
      failWithoutActual(
        Fact.fact("expected bounds to contain", point),
        Fact.fact("southwest", actual.southwest),
        Fact.fact("northeast", actual.northeast),
      )
    }
    return this
  }

  /** Asserts that this bounding box does NOT contain [point]. */
  fun doesNotContain(point: LatLng): LatLngBoundsSubject {
    isNotNull()
    if (actual!!.contains(point)) {
      failWithoutActual(
        Fact.fact("expected bounds not to contain", point),
        Fact.fact("southwest", actual.southwest),
        Fact.fact("northeast", actual.northeast),
      )
    }
    return this
  }

  /** Returns a [LatLngSubject] for the geographic center of the bounds. */
  fun center(): LatLngSubject {
    isNotNull()
    return check("center").about(LatLngSubject.latLngs()).that(actual!!.center)
  }

  /** Asserts that the center of this bounding box is within [tolerance] of [expectedCenter]. */
  fun hasCenterWithin(tolerance: Distance): LatLngSubject.LatLngTolerance = center().isWithin(tolerance)

  /** Returns a [LatLngSubject] for the southwest corner of the bounds. */
  fun southwest(): LatLngSubject {
    isNotNull()
    return check("southwest").about(LatLngSubject.latLngs()).that(actual!!.southwest)
  }

  /** Returns a [LatLngSubject] for the northeast corner of the bounds. */
  fun northeast(): LatLngSubject {
    isNotNull()
    return check("northeast").about(LatLngSubject.latLngs()).that(actual!!.northeast)
  }

  companion object {
    @JvmStatic
    fun latLngBounds(): Factory<LatLngBoundsSubject, LatLngBounds> = Factory { metadata, actual -> LatLngBoundsSubject(metadata, actual) }

    @JvmStatic
    fun assertThat(actual: LatLngBounds?): LatLngBoundsSubject = assertAbout(latLngBounds()).that(actual)
  }
}
// [END maps_robolectric_camera_bounds_subjects]
