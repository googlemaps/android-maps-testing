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

package com.google.android.maps.testing.golden

import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.maps.robolectric.ktx.markerWithTitle
import com.google.android.maps.robolectric.shadows.ShadowGoogleMap
import com.google.android.maps.robolectric.truth.Distance
import com.google.android.maps.robolectric.truth.LatLngSubject
import com.google.android.maps.robolectric.truth.isWithin
import com.google.android.maps.robolectric.truth.meters
import kotlinx.coroutines.suspendCancellableCoroutine
import org.robolectric.shadows.ShadowLooper
import kotlin.coroutines.resume
import com.google.android.maps.robolectric.ktx.findMapView as ktxFindMapView
import com.google.android.maps.robolectric.ktx.shadow as shadowProperty

// [START maps_compose_test_extensions]
/**
 * Traverses an Android [View] hierarchy recursively to locate the first hosted [MapView].
 *
 * Useful in Jetpack Compose UI tests when extracting the underlying [MapView] from an
 * `AndroidView` container hosting Google Maps.
 *
 * @receiver The root or parent [View] to search.
 * @return The first found [MapView], or `null` if none exists.
 */
public fun View.findMapView(): MapView? = ktxFindMapView()

/**
 * Suspends until the [GoogleMap] instance hosted by this [MapView] is initialized and ready.
 *
 * @receiver The [MapView] awaiting map initialization.
 * @return The initialized [GoogleMap].
 */
public suspend fun MapView.awaitMap(): GoogleMap = suspendCancellableCoroutine { continuation ->
  getMapAsync { map ->
    continuation.resume(map)
  }
  try {
    ShadowLooper.idleMainLooper()
  } catch (_: Throwable) {
  }
}

/**
 * Extracts the underlying [ShadowGoogleMap] instance from this [GoogleMap] when running
 * in a Robolectric unit testing environment.
 *
 * @receiver The [GoogleMap] to extract shadow state from.
 * @return The backing [ShadowGoogleMap].
 */
public fun GoogleMap.shadow(): ShadowGoogleMap = shadowProperty

/**
 * Locates an active [Marker] managed by this map whose [Marker.getTitle] matches [title].
 *
 * @receiver The [GoogleMap] containing markers.
 * @param title The exact marker title to search for.
 * @return The matching [Marker], or `null` if not found.
 */
public fun GoogleMap.findMarkerByTitle(title: String): Marker? = markerWithTitle(title)

/**
 * Simulates a user tapping on the marker matching [title].
 *
 * Invokes the configured [GoogleMap.OnMarkerClickListener] and shows the marker's info window
 * if not consumed by the listener.
 *
 * @receiver The [GoogleMap] managing the marker.
 * @param title The title of the marker to click.
 * @return `true` if the marker was found and clicked, `false` otherwise.
 */
public fun GoogleMap.clickMarker(title: String): Boolean {
  val marker = findMarkerByTitle(title) ?: return false
  shadowProperty.clickMarker(marker)
  return true
}

/**
 * Asserts that the camera target position is within [tolerance] of [expected].
 *
 * Uses the geodesic Truth [LatLngSubject] matcher.
 *
 * @receiver The [GoogleMap] whose camera position is being asserted.
 * @param expected The expected geographic coordinates.
 * @param tolerance Geodesic tolerance distance. Defaults to 10 meters.
 */
public fun GoogleMap.assertCameraTarget(expected: LatLng, tolerance: Distance = 10.meters) {
  LatLngSubject.assertThat(cameraPosition.target).isWithin(tolerance).of(expected)
}
// [END maps_compose_test_extensions]
