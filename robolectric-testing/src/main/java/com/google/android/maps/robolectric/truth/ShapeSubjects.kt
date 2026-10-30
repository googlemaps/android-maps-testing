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

import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.Polyline
import com.google.android.maps.robolectric.ktx.hexColor
import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout

// [START maps_robolectric_shape_subjects]
/**
 * Google Truth [Subject] for fluent assertions on a [Circle] overlay.
 */
class CircleSubject private constructor(
  metadata: FailureMetadata,
  private val actual: Circle?,
) : Subject(metadata, actual) {

  /** Returns a [LatLngSubject] for the circle's geographic center. */
  fun center(): LatLngSubject {
    isNotNull()
    return check("center").about(LatLngSubject.latLngs()).that(actual!!.center)
  }

  /** Asserts that the circle's center is within [tolerance] of [expectedCenter]. */
  @JvmOverloads
  fun isCenteredOn(expectedCenter: LatLng, tolerance: Distance = 1.meters): CircleSubject {
    center().isWithin(tolerance).of(expectedCenter)
    return this
  }

  /** Asserts that the circle's radius equals [expectedMeters] within [tolerance]. */
  @JvmOverloads
  fun hasRadius(expectedMeters: Double, tolerance: Double = 0.01): CircleSubject {
    isNotNull()
    check("radius").that(actual!!.radius).isWithin(tolerance).of(expectedMeters)
    return this
  }

  /** Asserts that the circle's radius equals [expectedDistance] within [tolerance]. */
  @JvmOverloads
  fun hasRadius(expectedDistance: Distance, tolerance: Distance = 0.01.meters): CircleSubject = hasRadius(expectedDistance.meters, tolerance.meters)

  /** Asserts that [point] lies inside or on the boundary of this geodesic circle. */
  fun contains(point: LatLng): CircleSubject {
    isNotNull()
    val distance = actual!!.center distanceTo point
    if (distance.meters > actual.radius) {
      failWithoutActual(
        Fact.fact("expected circle to contain", point),
        Fact.fact("circle center", actual.center),
        Fact.fact("circle radius (meters)", actual.radius),
        Fact.fact("actual distance from center", distance),
      )
    }
    return this
  }

  /** Asserts that [point] lies strictly outside this geodesic circle. */
  fun doesNotContain(point: LatLng): CircleSubject {
    isNotNull()
    val distance = actual!!.center distanceTo point
    if (distance.meters <= actual.radius) {
      failWithoutActual(
        Fact.fact("expected circle not to contain", point),
        Fact.fact("circle center", actual.center),
        Fact.fact("circle radius (meters)", actual.radius),
        Fact.fact("actual distance from center", distance),
      )
    }
    return this
  }

  /** Asserts that the circle's fill color equals [expectedColor]. */
  fun hasFillColor(expectedColor: Int): CircleSubject {
    isNotNull()
    if (actual!!.fillColor != expectedColor) {
      failWithoutActual(
        Fact.fact("expected fillColor", expectedColor.hexColor),
        Fact.fact("but was", actual.fillColor.hexColor),
      )
    }
    return this
  }

  /** Asserts that the circle's stroke color equals [expectedColor]. */
  fun hasStrokeColor(expectedColor: Int): CircleSubject {
    isNotNull()
    if (actual!!.strokeColor != expectedColor) {
      failWithoutActual(
        Fact.fact("expected strokeColor", expectedColor.hexColor),
        Fact.fact("but was", actual.strokeColor.hexColor),
      )
    }
    return this
  }

  /** Asserts that the circle's stroke width equals [expectedWidth]. */
  @JvmOverloads
  fun hasStrokeWidth(expectedWidth: Float, tolerance: Float = 0.01f): CircleSubject = apply {
    isNotNull()
    check("strokeWidth").that(actual!!.strokeWidth).isWithin(tolerance).of(expectedWidth)
  }

  /** Asserts that the circle's tag equals [expectedTag]. */
  fun hasTag(expectedTag: Any?): CircleSubject = apply {
    isNotNull()
    check("tag").that(actual!!.tag).isEqualTo(expectedTag)
  }

  /** Asserts that the circle is visible. */
  fun isVisible(): CircleSubject = apply {
    isNotNull()
    check("isVisible").that(actual!!.isVisible).isTrue()
  }

  companion object {
    @JvmStatic
    fun circles(): Factory<CircleSubject, Circle> = Factory { metadata, actual -> CircleSubject(metadata, actual) }

    @JvmStatic
    fun assertThat(actual: Circle?): CircleSubject = assertAbout(circles()).that(actual)
  }
}

/**
 * Google Truth [Subject] for fluent assertions on a [Polygon] overlay.
 */
class PolygonSubject private constructor(
  metadata: FailureMetadata,
  private val actual: Polygon?,
) : Subject(metadata, actual) {

  /** Asserts that the polygon has [expectedCount] boundary vertices. */
  fun hasPointCount(expectedCount: Int): PolygonSubject = apply {
    isNotNull()
    check("points.size").that(actual!!.points).hasSize(expectedCount)
  }

  /** Asserts that the polygon contains at least one vertex within [tolerance] of [expectedVertex]. */
  @JvmOverloads
  fun hasVertexNear(expectedVertex: LatLng, tolerance: Distance = 1.meters): PolygonSubject = apply {
    isNotNull()
    val found = actual!!.points.any { (it distanceTo expectedVertex) <= tolerance }
    if (!found) {
      failWithoutActual(
        Fact.fact("expected polygon to contain vertex within", tolerance),
        Fact.fact("of", expectedVertex),
        Fact.fact("actual vertices", actual.points),
      )
    }
  }

  /** Asserts that the polygon has [expectedHoleCount] interior holes. */
  fun hasHoleCount(expectedHoleCount: Int): PolygonSubject = apply {
    isNotNull()
    check("holes.size").that(actual!!.holes).hasSize(expectedHoleCount)
  }

  /** Asserts that the polygon's fill color equals [expectedColor]. */
  fun hasFillColor(expectedColor: Int): PolygonSubject = apply {
    isNotNull()
    if (actual!!.fillColor != expectedColor) {
      failWithoutActual(
        Fact.fact("expected fillColor", expectedColor.hexColor),
        Fact.fact("but was", actual.fillColor.hexColor),
      )
    }
  }

  /** Asserts that the polygon's stroke color equals [expectedColor]. */
  fun hasStrokeColor(expectedColor: Int): PolygonSubject = apply {
    isNotNull()
    if (actual!!.strokeColor != expectedColor) {
      failWithoutActual(
        Fact.fact("expected strokeColor", expectedColor.hexColor),
        Fact.fact("but was", actual.strokeColor.hexColor),
      )
    }
  }

  /** Asserts that the polygon's stroke width equals [expectedWidth]. */
  @JvmOverloads
  fun hasStrokeWidth(expectedWidth: Float, tolerance: Float = 0.01f): PolygonSubject = apply {
    isNotNull()
    check("strokeWidth").that(actual!!.strokeWidth).isWithin(tolerance).of(expectedWidth)
  }

  /** Asserts that the polygon's tag equals [expectedTag]. */
  fun hasTag(expectedTag: Any?): PolygonSubject = apply {
    isNotNull()
    check("tag").that(actual!!.tag).isEqualTo(expectedTag)
  }

  /** Asserts that the polygon is visible. */
  fun isVisible(): PolygonSubject = apply {
    isNotNull()
    check("isVisible").that(actual!!.isVisible).isTrue()
  }

  companion object {
    @JvmStatic
    fun polygons(): Factory<PolygonSubject, Polygon> = Factory { metadata, actual -> PolygonSubject(metadata, actual) }

    @JvmStatic
    fun assertThat(actual: Polygon?): PolygonSubject = assertAbout(polygons()).that(actual)
  }
}

/**
 * Google Truth [Subject] for fluent assertions on a [Polyline] overlay.
 */
class PolylineSubject private constructor(
  metadata: FailureMetadata,
  private val actual: Polyline?,
) : Subject(metadata, actual) {

  /** Asserts that the polyline has [expectedCount] vertices. */
  fun hasPointCount(expectedCount: Int): PolylineSubject = apply {
    isNotNull()
    check("points.size").that(actual!!.points).hasSize(expectedCount)
  }

  /** Asserts that the polyline's first vertex is within [tolerance] of [expectedStart]. */
  @JvmOverloads
  fun startsAt(expectedStart: LatLng, tolerance: Distance = 1.meters): PolylineSubject = apply {
    isNotNull()
    val first = actual!!.points.firstOrNull()
      ?: run {
        failWithoutActual(Fact.simpleFact("expected polyline to have a start point, but points was empty"))
        return@apply
      }
    check("points.first()").about(LatLngSubject.latLngs()).that(first).isWithin(tolerance).of(expectedStart)
  }

  /** Asserts that the polyline's last vertex is within [tolerance] of [expectedEnd]. */
  @JvmOverloads
  fun endsAt(expectedEnd: LatLng, tolerance: Distance = 1.meters): PolylineSubject = apply {
    isNotNull()
    val last = actual!!.points.lastOrNull()
      ?: run {
        failWithoutActual(Fact.simpleFact("expected polyline to have an end point, but points was empty"))
        return@apply
      }
    check("points.last()").about(LatLngSubject.latLngs()).that(last).isWithin(tolerance).of(expectedEnd)
  }

  /** Asserts that the polyline's stroke color equals [expectedColor]. */
  fun hasColor(expectedColor: Int): PolylineSubject = apply {
    isNotNull()
    if (actual!!.color != expectedColor) {
      failWithoutActual(
        Fact.fact("expected color", expectedColor.hexColor),
        Fact.fact("but was", actual.color.hexColor),
      )
    }
  }

  /** Asserts that the polyline's width equals [expectedWidth]. */
  @JvmOverloads
  fun hasWidth(expectedWidth: Float, tolerance: Float = 0.01f): PolylineSubject = apply {
    isNotNull()
    check("width").that(actual!!.width).isWithin(tolerance).of(expectedWidth)
  }

  /** Asserts that the polyline's tag equals [expectedTag]. */
  fun hasTag(expectedTag: Any?): PolylineSubject = apply {
    isNotNull()
    check("tag").that(actual!!.tag).isEqualTo(expectedTag)
  }

  /** Asserts that the polyline is visible. */
  fun isVisible(): PolylineSubject = apply {
    isNotNull()
    check("isVisible").that(actual!!.isVisible).isTrue()
  }

  companion object {
    @JvmStatic
    fun polylines(): Factory<PolylineSubject, Polyline> = Factory { metadata, actual -> PolylineSubject(metadata, actual) }

    @JvmStatic
    fun assertThat(actual: Polyline?): PolylineSubject = assertAbout(polylines()).that(actual)
  }
}
// [END maps_robolectric_shape_subjects]
