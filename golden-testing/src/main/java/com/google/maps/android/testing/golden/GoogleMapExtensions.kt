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

package com.google.maps.android.testing.golden

import android.graphics.Bitmap
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.maps.android.robolectric.ktx.renderToBitmap
import kotlinx.coroutines.suspendCancellableCoroutine
import org.robolectric.shadows.ShadowLooper
import java.io.File
import kotlin.coroutines.resume

// [START maps_golden_map_extensions]
/**
 * Enables deterministic golden screenshot testing on this [GoogleMap] instance.
 *
 * Sets [GoogleMap.getMapType] to [GoogleMap.MAP_TYPE_NONE] to prevent remote Google tile fetching,
 * and attaches a [TileOverlay] powered by [GoldenTileProvider] rendering deterministic synthetic tiles.
 *
 * @param strategy The tile rasterization strategy to use. Defaults to [GoldenTileStrategy.CoordinateGrid].
 * @param tileSize Tile width and height in pixels. Defaults to 256.
 * @param fadeIn Whether tiles should fade in upon loading. Defaults to `false` for instant, deterministic snapshots.
 * @param zIndex Z-index of the synthetic tile layer. Defaults to `-1000f` so user overlays (polylines, polygons, markers) render on top.
 * @return The attached [TileOverlay].
 */
public fun GoogleMap.enableGoldenTesting(
  strategy: GoldenTileStrategy = GoldenTileStrategy.CoordinateGrid(),
  tileSize: Int = 256,
  fadeIn: Boolean = false,
  zIndex: Float = -1000f,
): TileOverlay {
  this.mapType = GoogleMap.MAP_TYPE_NONE
  val provider = GoldenTileProvider(strategy = strategy, tileSize = tileSize)
  val options = TileOverlayOptions()
    .tileProvider(provider)
    .fadeIn(fadeIn)
    .zIndex(zIndex)
  return checkNotNull(this.addTileOverlay(options)) {
    "Failed to attach golden tile overlay to GoogleMap"
  }
}

/**
 * Suspends until the [GoogleMap] has completely finished rendering its tiles, overlays, and animations.
 *
 * When running in a local Robolectric JVM unit test, the main looper is automatically idled so
 * callers do not need to manually invoke `ShadowLooper.idleMainLooper()`.
 */
public suspend fun GoogleMap.awaitMapLoaded(): Unit = suspendCancellableCoroutine { continuation ->
  setOnMapLoadedCallback {
    if (continuation.isActive) {
      continuation.resume(Unit)
    }
  }
  continuation.invokeOnCancellation {
    setOnMapLoadedCallback(null)
  }
  idleMainLooperIfRobolectric()
}

/**
 * Captures a snapshot [Bitmap] of the [GoogleMap] asynchronously.
 *
 * When running in a local Robolectric JVM unit test, the main looper is automatically idled so
 * callers can write `val bitmap = googleMap.captureSnapshot()` directly inside `runTest` without
 * launching an `async` block or manually idling `ShadowLooper`.
 *
 * @param preallocatedBitmap Optional pre-allocated bitmap to draw into. If null, a new bitmap matching map dimensions is allocated.
 * @return The captured [Bitmap].
 */
public suspend fun GoogleMap.captureSnapshot(preallocatedBitmap: Bitmap? = null): Bitmap = suspendCancellableCoroutine { continuation ->
  val callback = GoogleMap.SnapshotReadyCallback { bitmap ->
    if (continuation.isActive) {
      if (bitmap != null) {
        continuation.resume(bitmap)
      } else {
        continuation.cancel(IllegalStateException("GoogleMap.snapshot returned null bitmap"))
      }
    }
  }
  if (preallocatedBitmap != null) {
    snapshot(callback, preallocatedBitmap)
  } else {
    snapshot(callback)
  }
  idleMainLooperIfRobolectric()
}

/**
 * Awaits map load, captures a snapshot of this [GoogleMap], and asserts that it matches the PNG
 * baseline at [goldenFile] (or records the baseline when [recordBaseline] or `-Dmaps.golden.record=true` is enabled).
 */
public suspend fun GoogleMap.assertMatchesGolden(
  goldenFile: File,
  maxDiffPercentage: Double = 0.001,
  pixelColorTolerance: Int = 0,
  message: String? = null,
  outputDir: File? = goldenFile.parentFile,
  recordBaseline: Boolean = System.getProperty("maps.golden.record").toBoolean(),
) {
  awaitMapLoaded()
  val actual = captureSnapshot()
  GoldenImageDiff.assertMatches(
    goldenFile = goldenFile,
    actual = actual,
    maxDiffPercentage = maxDiffPercentage,
    pixelColorTolerance = pixelColorTolerance,
    message = message,
    outputDir = outputDir,
    recordBaseline = recordBaseline,
  )
}

/**
 * Synchronously renders this [GoogleMap] to a [Bitmap] in Robolectric JVM tests and asserts that it
 * matches the PNG baseline at [goldenFile] (or records the baseline when [recordBaseline] or
 * `-Dmaps.golden.record=true` is enabled).
 */
public fun GoogleMap.assertMatchesGoldenBlocking(
  goldenFile: File,
  maxDiffPercentage: Double = 0.001,
  pixelColorTolerance: Int = 0,
  message: String? = null,
  outputDir: File? = goldenFile.parentFile,
  recordBaseline: Boolean = System.getProperty("maps.golden.record").toBoolean(),
) {
  val actual = renderToBitmap()
  GoldenImageDiff.assertMatches(
    goldenFile = goldenFile,
    actual = actual,
    maxDiffPercentage = maxDiffPercentage,
    pixelColorTolerance = pixelColorTolerance,
    message = message,
    outputDir = outputDir,
    recordBaseline = recordBaseline,
  )
}

private fun idleMainLooperIfRobolectric() {
  try {
    ShadowLooper.idleMainLooper()
  } catch (_: Throwable) {
    // Running on an Android device/emulator where Robolectric is not on the runtime classpath.
  }
}
// [END maps_golden_map_extensions]
