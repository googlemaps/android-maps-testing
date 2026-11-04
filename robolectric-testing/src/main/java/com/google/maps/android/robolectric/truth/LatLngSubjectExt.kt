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

import com.google.android.gms.maps.model.LatLng
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// [START maps_robolectric_distance_dsl]
/**
 * Represents a physical geodesic distance on the Earth's surface.
 *
 * Implements [Comparable] and provides a concise human-readable [toString] (e.g., `"14.2m"` or
 * `"1.50km"`) so that both **Google Truth** subjects and **Kotlin Power-Assert** expression diagrams
 * render clear physical distances without manual formatting.
 */
data class Distance(val meters: Double) : Comparable<Distance> {
  val kilometers: Double get() = meters / 1000.0
  val miles: Double get() = meters / 1609.344
  val feet: Double get() = meters / 0.3048

  override fun compareTo(other: Distance): Int = meters.compareTo(other.meters)

  operator fun plus(other: Distance): Distance = Distance(meters + other.meters)
  operator fun minus(other: Distance): Distance = Distance(meters - other.meters)
  operator fun times(scale: Number): Distance = Distance(meters * scale.toDouble())
  operator fun div(scale: Number): Distance = Distance(meters / scale.toDouble())

  override fun toString(): String = if (abs(meters) >= 1000.0) {
    String.format(Locale.US, "%.2fkm (%.1fm)", kilometers, meters)
  } else {
    String.format(Locale.US, "%.1fm", meters)
  }
}

// Number extensions to construct Distance fluently (e.g., 5.meters, 2.kilometers)
val Number.meter: Distance get() = Distance(this.toDouble())
val Number.meters: Distance get() = Distance(this.toDouble())
val Number.kilometer: Distance get() = Distance(this.toDouble() * 1000.0)
val Number.kilometers: Distance get() = Distance(this.toDouble() * 1000.0)
val Number.mile: Distance get() = Distance(this.toDouble() * 1609.344)
val Number.miles: Distance get() = Distance(this.toDouble() * 1609.344)
val Number.foot: Distance get() = Distance(this.toDouble() * 0.3048)
val Number.feet: Distance get() = Distance(this.toDouble() * 0.3048)

/**
 * Computes the spherical Haversine distance in meters between two [LatLng] coordinates without
 * loading any Google Truth classes, so Kotlin Power-Assert and KTX consumers do not require
 * `com.google.truth:truth` on their test classpath.
 */
internal fun haversineDistanceMeters(p1: LatLng, p2: LatLng): Double {
  val earthRadius = 6371000.0 // meters
  val dLat = Math.toRadians(p2.latitude - p1.latitude)
  val dLng = Math.toRadians(p2.longitude - p1.longitude)
  val a = sin(dLat / 2) * sin(dLat / 2) +
    cos(Math.toRadians(p1.latitude)) *
    cos(Math.toRadians(p2.latitude)) *
    sin(dLng / 2) *
    sin(dLng / 2)
  val c = 2 * atan2(sqrt(a), sqrt(1 - a))
  return earthRadius * c
}

/**
 * Computes the spherical Haversine [Distance] between this [LatLng] and [other].
 *
 * Works seamlessly with **Kotlin Power-Assert** (without requiring Google Truth on the classpath):
 * `assertGeo(map.cameraTarget.distanceTo(boulder) <= 5.meters)`
 * or infix:
 * `assertGeo((map.cameraTarget distanceTo boulder) <= 5.meters)`
 */
infix fun LatLng.distanceTo(other: LatLng): Distance = Distance(haversineDistanceMeters(this, other))

/**
 * Returns `true` if this [LatLng] is within [tolerance] of [other].
 */
fun LatLng.isNear(other: LatLng, tolerance: Distance = 10.meters): Boolean = distanceTo(other) <= tolerance

/**
 * Asserts that the [LatLng] is within a given [Distance] of another [LatLng].
 */
fun LatLngSubject.isWithin(distance: Distance): LatLngSubject.LatLngTolerance = this.isWithin(distance.meters)

/**
 * Asserts that the [LatLng] is strictly farther than [distance] from another [LatLng].
 */
fun LatLngSubject.isNotWithin(distance: Distance): LatLngSubject.LatLngTolerance = this.isNotWithin(distance.meters)
// [END maps_robolectric_distance_dsl]
