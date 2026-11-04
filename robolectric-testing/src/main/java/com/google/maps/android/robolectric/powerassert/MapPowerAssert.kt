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

package com.google.maps.android.robolectric.powerassert

import android.graphics.Color
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.Polyline
import com.google.maps.android.robolectric.ktx.activeInfoWindowMarker
import com.google.maps.android.robolectric.ktx.cameraTarget
import com.google.maps.android.robolectric.ktx.cameraZoom
import com.google.maps.android.robolectric.ktx.findMarker
import com.google.maps.android.robolectric.ktx.pixelColorAt
import com.google.maps.android.robolectric.ktx.shadow
import com.google.maps.android.robolectric.ktx.visibleBounds
import kotlin.math.abs
import com.google.maps.android.robolectric.truth.Distance as TruthDistance
import com.google.maps.android.robolectric.truth.distanceTo as truthDistanceTo
import com.google.maps.android.robolectric.truth.isNear as truthIsNear

// [START maps_robolectric_power_assert]
/**
 * Re-exports [TruthDistance] in the `powerassert` package so Kotlin Power-Assert tests do not need
 * to import from `com.google.maps.android.robolectric.truth`.
 */
typealias Distance = TruthDistance

val Number.meter: Distance get() = Distance(this.toDouble())
val Number.meters: Distance get() = Distance(this.toDouble())
val Number.kilometer: Distance get() = Distance(this.toDouble() * 1000.0)
val Number.kilometers: Distance get() = Distance(this.toDouble() * 1000.0)
val Number.mile: Distance get() = Distance(this.toDouble() * 1609.344)
val Number.miles: Distance get() = Distance(this.toDouble() * 1609.344)
val Number.foot: Distance get() = Distance(this.toDouble() * 0.3048)
val Number.feet: Distance get() = Distance(this.toDouble() * 0.3048)

/** Computes the spherical Haversine [Distance] between this [LatLng] and [other]. */
infix fun LatLng.distanceTo(other: LatLng): Distance = this.truthDistanceTo(other)

/** Returns `true` if this [LatLng] is within [tolerance] of [other]. */
fun LatLng.isNear(other: LatLng, tolerance: Distance = 10.meters): Boolean = this.truthIsNear(other, tolerance)

/**
 * Always-on domain assertion entrypoint instrumented by the **Kotlin Power-Assert** compiler plugin
 * (`org.jetbrains.kotlin.plugin.power-assert`).
 *
 * Unlike `kotlin.assert`, which is skipped if the JVM is launched without `-ea`, [assertGeo]
 * unconditionally throws an [AssertionError] when [condition] is `false`. When compiled with the
 * Kotlin Power-Assert plugin, calls to `assertGeo(expr)` are automatically rewritten to call the
 * two-argument overload with a full diagram of every sub-expression's runtime value.
 */
fun assertGeo(condition: Boolean) {
  if (!condition) {
    throw AssertionError("Geodesic / Map assertion failed")
  }
}

/**
 * Overload targeted by the Kotlin Power-Assert compiler plugin to inject the diagrammed expression
 * tree via [lazyMessage].
 */
fun assertGeo(condition: Boolean, lazyMessage: () -> Any) {
  if (!condition) {
    throw AssertionError(lazyMessage().toString())
  }
}

/**
 * Receiver-scoped Power-Assert entrypoint for [GoogleMap].
 *
 * Particularly succinct inside `withTestMap { ... }` blocks where `this` is already [GoogleMap]:
 * ```kotlin
 * withTestMap {
 *   moveCamera(CameraUpdateFactory.newLatLngZoom(boulder, 14f))
 *   assertMap(
 *     (cameraTarget distanceTo boulder) <= 5.meters &&
 *       cameraZoom == 14f
 *   )
 * }
 * ```
 * On failure, prints the full Power-Assert diagram followed by a summary of the [GoogleMap] state.
 */
fun GoogleMap.assertMap(condition: Boolean) {
  if (!condition) {
    throw AssertionError("Map assertion failed\nMap state: $shadow")
  }
}

/**
 * Overload targeted by the Kotlin Power-Assert compiler plugin for [GoogleMap.assertMap].
 */
fun GoogleMap.assertMap(condition: Boolean, lazyMessage: () -> Any) {
  if (!condition) {
    throw AssertionError("${lazyMessage()}\nMap state: $shadow")
  }
}

/**
 * Receiver-scoped Power-Assert entrypoint for [Marker].
 *
 * On failure, prints the full Power-Assert diagram followed by a summary of the [Marker] state.
 */
fun Marker.assertMarker(condition: Boolean) {
  if (!condition) {
    throw AssertionError("Marker assertion failed\nMarker state: $shadow")
  }
}

/**
 * Overload targeted by the Kotlin Power-Assert compiler plugin for [Marker.assertMarker].
 */
fun Marker.assertMarker(condition: Boolean, lazyMessage: () -> Any) {
  if (!condition) {
    throw AssertionError("${lazyMessage()}\nMarker state: $shadow")
  }
}

/**
 * Enables Kotlin `in` operator syntax for [LatLngBounds] in Power-Assert expressions:
 * `assertGeo(boulder in map.visibleBounds)`
 */
operator fun LatLngBounds.contains(point: LatLng): Boolean = this.contains(point)

/**
 * Enables Kotlin `in` operator syntax for [Circle] in Power-Assert expressions:
 * `assertGeo(boulder in deliveryCircle)`
 */
operator fun Circle.contains(point: LatLng): Boolean = (center distanceTo point).meters <= radius

/**
 * Returns `true` if this [GoogleMap]'s camera target is within [tolerance] of [expectedTarget] and
 * (if specified) its zoom level is within [zoomTolerance] of [expectedZoom].
 */
fun GoogleMap.isCenteredOn(
  expectedTarget: LatLng,
  expectedZoom: Float? = null,
  tolerance: Distance = 5.meters,
  zoomTolerance: Float = 0.01f,
): Boolean = cameraTarget.isNear(expectedTarget, tolerance) &&
  (expectedZoom == null || abs(cameraZoom - expectedZoom) <= zoomTolerance)

/**
 * Returns `true` if [point] is currently inside the visible region of this [GoogleMap].
 */
fun GoogleMap.isShowing(point: LatLng): Boolean = visibleBounds.contains(point)

/**
 * Returns `true` if this [GoogleMap] contains at least one [Marker] matching all non-null criteria.
 */
fun GoogleMap.hasMarker(
  title: String? = null,
  snippet: String? = null,
  tag: Any? = null,
  near: LatLng? = null,
  tolerance: Distance = 5.meters,
  infoWindowShown: Boolean? = null,
): Boolean = findMarker(title = title, snippet = snippet, tag = tag, near = near, tolerance = tolerance)
  ?.let { infoWindowShown == null || it.isInfoWindowShown == infoWindowShown }
  ?: false

/**
 * Returns `true` if this [Marker] matches all non-null criteria.
 */
fun Marker.matches(
  title: String? = null,
  snippet: String? = null,
  tag: Any? = null,
  near: LatLng? = null,
  tolerance: Distance = 1.meters,
  infoWindowShown: Boolean? = null,
  visible: Boolean? = null,
): Boolean = (title == null || this.title == title) &&
  (snippet == null || this.snippet == snippet) &&
  (tag == null || this.tag == tag) &&
  (near == null || this.position.isNear(near, tolerance)) &&
  (infoWindowShown == null || this.isInfoWindowShown == infoWindowShown) &&
  (visible == null || this.isVisible == visible)

/**
 * Returns `true` if the marker with [title] currently has its info window displayed on this [GoogleMap].
 */
fun GoogleMap.isInfoWindowShownFor(title: String): Boolean = activeInfoWindowMarker?.title == title

/**
 * Returns `true` if this [Polygon] has a boundary vertex within [tolerance] of [expectedVertex].
 */
fun Polygon.hasVertexNear(expectedVertex: LatLng, tolerance: Distance = 1.meters): Boolean = points.any { it.isNear(expectedVertex, tolerance) }

/**
 * Returns `true` if this [Polyline] starts within [tolerance] of [start] and ends within [tolerance] of [end].
 */
fun Polyline.connects(
  start: LatLng,
  end: LatLng,
  tolerance: Distance = 1.meters,
): Boolean = points.isNotEmpty() &&
  points.first().isNear(start, tolerance) &&
  points.last().isNear(end, tolerance)

/**
 * Returns `true` if an overlay has rendered a non-background pixel at [latLng] on this [GoogleMap].
 */
fun GoogleMap.hasOverlayPixelAt(
  latLng: LatLng,
  backgroundColor: Int = Color.LTGRAY,
): Boolean {
  val color = pixelColorAt(latLng)
  return color != backgroundColor && Color.alpha(color) > 0
}
// [END maps_robolectric_power_assert]
