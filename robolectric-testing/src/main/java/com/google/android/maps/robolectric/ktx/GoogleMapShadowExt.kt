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

package com.google.android.maps.robolectric.ktx

import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.UiSettings
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.GroundOverlay
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.maps.robolectric.shadows.ShadowBitmapDescriptor
import com.google.android.maps.robolectric.shadows.ShadowCircle
import com.google.android.maps.robolectric.shadows.ShadowGoogleMap
import com.google.android.maps.robolectric.shadows.ShadowGroundOverlay
import com.google.android.maps.robolectric.shadows.ShadowMapView
import com.google.android.maps.robolectric.shadows.ShadowMarker
import com.google.android.maps.robolectric.shadows.ShadowPolygon
import com.google.android.maps.robolectric.shadows.ShadowPolyline
import com.google.android.maps.robolectric.shadows.ShadowTileOverlay
import com.google.android.maps.robolectric.shadows.ShadowUiSettings
import com.google.android.maps.robolectric.truth.Distance
import com.google.android.maps.robolectric.truth.distanceTo
import com.google.android.maps.robolectric.truth.meters
import org.robolectric.shadow.api.Shadow

// [START maps_robolectric_shadow_ktx]
/** Typed Robolectric shadow extractor for [GoogleMap]. */
val GoogleMap.shadow: ShadowGoogleMap
  get() = Shadow.extract(this) as ShadowGoogleMap

/** Typed Robolectric shadow extractor for [MapView]. */
val MapView.shadow: ShadowMapView
  get() = Shadow.extract(this) as ShadowMapView

/** Typed Robolectric shadow extractor for [Marker]. */
val Marker.shadow: ShadowMarker
  get() = Shadow.extract(this) as ShadowMarker

/** Typed Robolectric shadow extractor for [Polygon]. */
val Polygon.shadow: ShadowPolygon
  get() = Shadow.extract(this) as ShadowPolygon

/** Typed Robolectric shadow extractor for [Polyline]. */
val Polyline.shadow: ShadowPolyline
  get() = Shadow.extract(this) as ShadowPolyline

/** Typed Robolectric shadow extractor for [Circle]. */
val Circle.shadow: ShadowCircle
  get() = Shadow.extract(this) as ShadowCircle

/** Typed Robolectric shadow extractor for [GroundOverlay]. */
val GroundOverlay.shadow: ShadowGroundOverlay
  get() = Shadow.extract(this) as ShadowGroundOverlay

/** Typed Robolectric shadow extractor for [TileOverlay]. */
val TileOverlay.shadow: ShadowTileOverlay
  get() = Shadow.extract(this) as ShadowTileOverlay

/** Typed Robolectric shadow extractor for [UiSettings]. */
val UiSettings.shadow: ShadowUiSettings
  get() = Shadow.extract(this) as ShadowUiSettings

/** Typed Robolectric shadow extractor for [BitmapDescriptor]. */
val BitmapDescriptor.shadow: ShadowBitmapDescriptor
  get() = Shadow.extract(this) as ShadowBitmapDescriptor

/** The [MapView] hosting this [GoogleMap], if attached. */
val GoogleMap.mapView: MapView?
  get() = shadow.mapView

/** All active [Marker] instances currently added to this [GoogleMap]. */
val GoogleMap.markers: List<Marker>
  get() = shadow.markers

/** All active [Polygon] instances currently added to this [GoogleMap]. */
val GoogleMap.polygons: List<Polygon>
  get() = shadow.polygons

/** All active [Polyline] instances currently added to this [GoogleMap]. */
val GoogleMap.polylines: List<Polyline>
  get() = shadow.polylines

/** All active [Circle] instances currently added to this [GoogleMap]. */
val GoogleMap.circles: List<Circle>
  get() = shadow.circles

/** All active [GroundOverlay] instances currently added to this [GoogleMap]. */
val GoogleMap.groundOverlays: List<GroundOverlay>
  get() = shadow.groundOverlays

/** All active [TileOverlay] instances currently added to this [GoogleMap]. */
val GoogleMap.tileOverlays: List<TileOverlay>
  get() = shadow.tileOverlays

/** The [Marker] whose info window is currently displayed on this [GoogleMap], or `null`. */
val GoogleMap.activeInfoWindowMarker: Marker?
  get() = shadow.activeInfoWindowMarker

/** Shorthand for `cameraPosition.target`. */
val GoogleMap.cameraTarget: LatLng
  get() = cameraPosition.target

/** Shorthand for `cameraPosition.zoom`. */
val GoogleMap.cameraZoom: Float
  get() = cameraPosition.zoom

/** Shorthand for `cameraPosition.bearing`. */
val GoogleMap.cameraBearing: Float
  get() = cameraPosition.bearing

/** Shorthand for `cameraPosition.tilt`. */
val GoogleMap.cameraTilt: Float
  get() = cameraPosition.tilt

/** Shorthand for `projection.visibleRegion.latLngBounds`. */
val GoogleMap.visibleBounds: LatLngBounds
  get() = projection.visibleRegion.latLngBounds

/** The last [MapStyleOptions] applied via `setMapStyle(...)`, or `null`. */
val GoogleMap.mapStyle: MapStyleOptions?
  get() = shadow.mapStyle

/** The [LatLngBounds] camera target constraint applied via `setLatLngBoundsForCameraTarget(...)`, or `null`. */
val GoogleMap.cameraTargetBounds: LatLngBounds?
  get() = shadow.latLngBoundsForCameraTarget

/** The accessibility content description set via `setContentDescription(...)`, or `null`. */
val GoogleMap.contentDescription: String?
  get() = shadow.contentDescription

/**
 * Simulates a user-initiated pan or zoom gesture (`OnCameraMoveStartedListener.REASON_GESTURE`)
 * applying [update].
 */
fun GoogleMap.simulateUserCameraMove(update: CameraUpdate) = shadow.simulateGestureCameraMove(update)

/**
 * Simulates a user-initiated pan or zoom gesture (`OnCameraMoveStartedListener.REASON_GESTURE`)
 * to [target] and [zoom].
 */
fun GoogleMap.simulateUserCameraMove(
  target: LatLng,
  zoom: Float = cameraPosition.zoom,
) = shadow.simulateGestureCameraMove(CameraUpdateFactory.newLatLngZoom(target, zoom))

/**
 * Finds the first [Marker] on this [GoogleMap] matching all non-null filter criteria.
 */
fun GoogleMap.findMarker(
  title: String? = null,
  snippet: String? = null,
  tag: Any? = null,
  near: LatLng? = null,
  tolerance: Distance = 5.meters,
): Marker? = markers.firstOrNull { marker ->
  (title == null || marker.title == title) &&
    (snippet == null || marker.snippet == snippet) &&
    (tag == null || marker.tag == tag) &&
    (near == null || (marker.position distanceTo near) <= tolerance)
}

/**
 * Returns the first [Marker] matching the given criteria, or throws an [AssertionError] listing all
 * available markers on the map.
 */
fun GoogleMap.requireMarker(
  title: String? = null,
  snippet: String? = null,
  tag: Any? = null,
  near: LatLng? = null,
  tolerance: Distance = 5.meters,
): Marker = findMarker(title = title, snippet = snippet, tag = tag, near = near, tolerance = tolerance)
  ?: throw AssertionError(
    "Expected a marker matching (title=$title, snippet=$snippet, tag=$tag, near=$near) " +
      "on $shadow, but available markers were: ${markers.map { it.shadow }}",
  )

/** Finds a [Marker] by exact [title], or returns `null`. */
fun GoogleMap.markerWithTitle(title: String): Marker? = findMarker(title = title)

/** Finds a [Marker] by exact [tag], or returns `null`. */
fun GoogleMap.markerWithTag(tag: Any): Marker? = findMarker(tag = tag)

/** Simulates a user tap on this [GoogleMap] at [latLng]. */
fun GoogleMap.clickMap(latLng: LatLng) = shadow.clickMap(latLng)

/** Simulates a user long-press on this [GoogleMap] at [latLng]. */
fun GoogleMap.longClickMap(latLng: LatLng) = shadow.longClickMap(latLng)

/** Simulates a user tapping [marker] on this [GoogleMap]. */
fun GoogleMap.clickMarker(marker: Marker): Marker {
  shadow.clickMarker(marker)
  return marker
}

/** Finds the marker with [title] on this [GoogleMap], simulates a tap on it, and returns it. */
fun GoogleMap.clickMarker(title: String): Marker {
  val marker = requireMarker(title = title)
  shadow.clickMarker(marker)
  return marker
}

/** Simulates a user tapping this [Marker]. */
fun Marker.click(): Marker {
  val map = checkNotNull(shadow.googleMap) { "Marker is not attached to a GoogleMap" }
  map.shadow.clickMarker(this)
  return this
}

/** Simulates a user tapping this [Marker]'s info window. */
fun Marker.clickInfoWindow(): Marker {
  val map = checkNotNull(shadow.googleMap) { "Marker is not attached to a GoogleMap" }
  map.shadow.clickInfoWindow(this)
  return this
}

/** Simulates a user long-pressing this [Marker]'s info window. */
fun Marker.longClickInfoWindow(): Marker {
  val map = checkNotNull(shadow.googleMap) { "Marker is not attached to a GoogleMap" }
  map.shadow.longClickInfoWindow(this)
  return this
}

/**
 * Simulates a complete user drag gesture on this [Marker] from its current position to [newPosition]
 * (invoking `onMarkerDragStart`, `onMarkerDrag`, and `onMarkerDragEnd`).
 */
fun Marker.dragTo(newPosition: LatLng): Marker {
  val map = checkNotNull(shadow.googleMap) { "Marker is not attached to a GoogleMap" }
  map.shadow.dragMarkerStart(this)
  map.shadow.dragMarker(this, newPosition)
  map.shadow.dragMarkerEnd(this, newPosition)
  return this
}

/** Simulates a user clicking this [Polygon]. */
fun Polygon.click(): Polygon {
  val map = checkNotNull(shadow.googleMap) { "Polygon is not attached to a GoogleMap" }
  map.shadow.clickPolygon(this)
  return this
}

/** Simulates a user clicking this [Polyline]. */
fun Polyline.click(): Polyline {
  val map = checkNotNull(shadow.googleMap) { "Polyline is not attached to a GoogleMap" }
  map.shadow.clickPolyline(this)
  return this
}

/** Simulates a user clicking this [Circle]. */
fun Circle.click(): Circle {
  val map = checkNotNull(shadow.googleMap) { "Circle is not attached to a GoogleMap" }
  map.shadow.clickCircle(this)
  return this
}

/** Simulates a user clicking this [GroundOverlay]. */
fun GroundOverlay.click(): GroundOverlay {
  val map = checkNotNull(shadow.googleMap) { "GroundOverlay is not attached to a GoogleMap" }
  map.shadow.clickGroundOverlay(this)
  return this
}
// [END maps_robolectric_shadow_ktx]
