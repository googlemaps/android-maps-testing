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

package com.google.maps.android.robolectric.ktx

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowLooper
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

// [START maps_robolectric_lazy_harness]
/**
 * Traverses an Android [View] hierarchy recursively to locate the first hosted [MapView].
 *
 * Useful in Activity, Fragment, and Jetpack Compose UI tests when extracting the underlying
 * [MapView] from a layout or `AndroidView` container hosting Google Maps.
 */
fun View.findMapView(): MapView? {
  if (this is MapView) return this
  if (this is ViewGroup) {
    for (i in 0 until childCount) {
      val found = getChildAt(i).findMapView()
      if (found != null) return found
    }
  }
  return null
}

/**
 * Traverses an Android [View] hierarchy recursively and returns the first hosted [MapView],
 * or throws an [AssertionError] if no [MapView] is present.
 */
fun View.requireMapView(): MapView = findMapView()
  ?: throw AssertionError("Expected to find a MapView inside view hierarchy rooted at $this, but none was found.")

/**
 * Creates, measures, lays out, and initializes a [MapView] for headless Robolectric unit tests.
 *
 * Eliminates the 15-line ceremony of building a host [Activity], computing [View.MeasureSpec.EXACTLY]
 * specs, calling [View.layout], and dispatching [MapView.onCreate].
 *
 * @param width Viewport width in pixels. Defaults to 200.
 * @param height Viewport height in pixels. Defaults to 200.
 * @param activity Backing [Activity] context. Defaults to a fresh Robolectric [Activity].
 * @return A measured, laid-out, and created [MapView].
 */
fun createTestMapView(
  width: Int = 200,
  height: Int = 200,
  activity: Activity = Robolectric.buildActivity(Activity::class.java).create().get(),
): MapView = MapView(activity).apply {
  measure(
    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
  )
  layout(0, 0, width, height)
  onCreate(Bundle())
}

/**
 * Synchronously retrieves the [GoogleMap] from this [MapView] by requesting [MapView.getMapAsync]
 * and idling the Robolectric main looper.
 */
fun MapView.getMapBlocking(): GoogleMap {
  val mapRef = AtomicReference<GoogleMap>()
  getMapAsync { mapRef.set(it) }
  ShadowLooper.idleMainLooper()
  return checkNotNull(mapRef.get()) {
    "Failed to obtain GoogleMap from MapView. Does the test run with @RunWith(MapsRobolectricTestRunner::class)?"
  }.also { map ->
    if (width > 0 && height > 0) {
      map.shadow.setViewportSize(width, height)
    }
  }
}

/**
 * Synchronously retrieves the [GoogleMap] from this [SupportMapFragment] by requesting
 * [SupportMapFragment.getMapAsync] and idling the Robolectric main looper.
 */
fun SupportMapFragment.getMapBlocking(): GoogleMap {
  val mapRef = AtomicReference<GoogleMap>()
  getMapAsync { mapRef.set(it) }
  ShadowLooper.idleMainLooper()
  return checkNotNull(mapRef.get()) {
    "Failed to obtain GoogleMap from SupportMapFragment. Does the test run with @RunWith(MapsRobolectricTestRunner::class)?"
  }
}

/**
 * One-liner factory that provisions a ready-to-use [GoogleMap] backed by a measured and laid-out
 * [MapView] of dimensions [width] x [height].
 *
 * The backing [MapView] remains accessible via [GoogleMap.mapView] for canvas rendering and
 * lifecycle assertions.
 */
fun createTestMap(width: Int = 200, height: Int = 200): GoogleMap = createTestMapView(width = width, height = height).getMapBlocking()

/**
 * Executes [block] with a freshly initialized [GoogleMap] as receiver.
 *
 * Returns [Unit] so that JUnit 4 `@Test` methods can use single-expression syntax
 * (`@Test fun myTest() = withTestMap { ... }`).
 */
inline fun withTestMap(
  width: Int = 200,
  height: Int = 200,
  block: GoogleMap.() -> Unit,
) {
  createTestMap(width = width, height = height).block()
}

/**
 * Renders this [MapView] onto an in-memory [Bitmap] without requiring an emulator.
 */
fun MapView.renderToBitmap(
  width: Int = this.width.takeIf { it > 0 } ?: 200,
  height: Int = this.height.takeIf { it > 0 } ?: 200,
): Bitmap {
  val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)
  draw(canvas)
  return bitmap
}

/**
 * Renders the [MapView] backing this [GoogleMap] onto an in-memory [Bitmap].
 */
fun GoogleMap.renderToBitmap(
  width: Int = mapView?.width?.takeIf { it > 0 } ?: shadow.viewportWidth.coerceAtMost(400),
  height: Int = mapView?.height?.takeIf { it > 0 } ?: shadow.viewportHeight.coerceAtMost(400),
): Bitmap {
  val view = mapView
  if (view != null) {
    return view.renderToBitmap(width = width, height = height)
  }
  val snapshotRef = AtomicReference<Bitmap>()
  val preallocated = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  snapshot({ snapshotRef.set(it) }, preallocated)
  ShadowLooper.idleMainLooper()
  return snapshotRef.get() ?: preallocated
}

/**
 * Renders this [GoogleMap] and returns the ARGB pixel color at screen coordinates ([x], [y]).
 */
fun GoogleMap.pixelColorAt(x: Int, y: Int): Int = renderToBitmap().getPixel(x, y)

/**
 * Projects [latLng] into screen space, renders this [GoogleMap], and returns the ARGB pixel color
 * at that geographic coordinate.
 */
fun GoogleMap.pixelColorAt(latLng: LatLng): Int {
  val bitmap = renderToBitmap()
  val point = projection.toScreenLocation(latLng)
  require(point.x in 0 until bitmap.width && point.y in 0 until bitmap.height) {
    "Projected screen point (${point.x}, ${point.y}) for $latLng is outside viewport (${bitmap.width}x${bitmap.height})"
  }
  return bitmap.getPixel(point.x, point.y)
}

/**
 * Formats an ARGB color integer as `#AARRGGBB` for readable assertions and Power-Assert diagrams.
 */
val Int.hexColor: String
  get() = String.format(Locale.US, "#%08X", this)

/**
 * Projects [latLng] into screen space, renders this [GoogleMap], and returns the `#AARRGGBB` hex
 * color string at that geographic coordinate.
 */
fun GoogleMap.pixelHexAt(latLng: LatLng): String = pixelColorAt(latLng).hexColor
// [END maps_robolectric_lazy_harness]
