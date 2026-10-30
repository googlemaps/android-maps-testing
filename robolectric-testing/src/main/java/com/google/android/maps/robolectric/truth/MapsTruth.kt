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

@file:JvmName("MapsTruth")

package com.google.android.maps.robolectric.truth

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.Polyline

// [START maps_robolectric_unified_truth]
/**
 * Unified Google Truth entrypoint for [GoogleMap].
 *
 * Allows lazy programmers to import `com.google.android.maps.robolectric.truth.assertThat`
 * (or call `MapsTruth.assertThat(...)` from Java) for all Google Maps domain types.
 */
fun assertThat(actual: GoogleMap?): GoogleMapSubject = GoogleMapSubject.assertThat(actual)

/** Unified Google Truth entrypoint for [Marker]. */
fun assertThat(actual: Marker?): MarkerSubject = MarkerSubject.assertThat(actual)

/** Unified Google Truth entrypoint for [CameraPosition]. */
fun assertThat(actual: CameraPosition?): CameraPositionSubject = CameraPositionSubject.assertThat(actual)

/** Unified Google Truth entrypoint for [LatLng]. */
fun assertThat(actual: LatLng?): LatLngSubject = LatLngSubject.assertThat(actual)

/** Unified Google Truth entrypoint for [LatLngBounds]. */
fun assertThat(actual: LatLngBounds?): LatLngBoundsSubject = LatLngBoundsSubject.assertThat(actual)

/** Unified Google Truth entrypoint for [Circle]. */
fun assertThat(actual: Circle?): CircleSubject = CircleSubject.assertThat(actual)

/** Unified Google Truth entrypoint for [Polygon]. */
fun assertThat(actual: Polygon?): PolygonSubject = PolygonSubject.assertThat(actual)

/** Unified Google Truth entrypoint for [Polyline]. */
fun assertThat(actual: Polyline?): PolylineSubject = PolylineSubject.assertThat(actual)
// [END maps_robolectric_unified_truth]
