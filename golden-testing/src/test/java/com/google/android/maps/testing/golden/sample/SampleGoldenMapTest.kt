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

package com.google.android.maps.testing.golden.sample

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.view.View
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.maps.robolectric.annotation.EnableMapsShadows
import com.google.android.maps.testing.golden.GoldenImageDiff
import com.google.android.maps.testing.golden.GoldenTileStrategy
import com.google.android.maps.testing.golden.captureSnapshot
import com.google.android.maps.testing.golden.enableGoldenTesting
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.atomic.AtomicReference

// [START maps_sample_golden_map_test]
/**
 * Sample test suite demonstrating deterministic golden screenshot testing with Google Maps.
 *
 * In this pattern:
 * 1. The map is set to `MAP_TYPE_NONE` and backed by a deterministic synthetic tile provider.
 * 2. Overlays (circles, polygons, markers) are added to the map.
 * 3. The map snapshot is captured via [GoogleMap.captureSnapshot].
 * 4. The rendered snapshot is verified against a baseline using [GoldenImageDiff.assertMatches].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@EnableMapsShadows
public class SampleGoldenMapTest {

  private lateinit var activity: Activity
  private lateinit var mapView: MapView
  private lateinit var googleMap: GoogleMap

  @Before
  public fun setUp() {
    activity = Robolectric.buildActivity(Activity::class.java).create().get()
    mapView = MapView(activity)
    mapView.measure(
      View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
    )
    mapView.layout(0, 0, 200, 200)
    mapView.onCreate(Bundle())

    val mapRef = AtomicReference<GoogleMap>()
    mapView.getMapAsync { mapRef.set(it) }
    ShadowLooper.idleMainLooper()
    googleMap = checkNotNull(mapRef.get())

    // Enable deterministic golden tiles
    googleMap.enableGoldenTesting(strategy = GoldenTileStrategy.SolidColorHash())
  }

  @Test
  public fun sample_mapRenderingIsDeterministicAcrossRepeatedCaptures() = runTest {
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(37.7749, -122.4194), 10f))
    googleMap.addCircle(
      CircleOptions()
        .center(LatLng(37.7749, -122.4194))
        .radius(5000.0)
        .fillColor(Color.RED)
        .strokeWidth(0f),
    )

    // First capture (acts as baseline)
    val baselineSnapshot = captureSnapshotWithShadowLooper()

    // Second capture under identical state
    val secondSnapshot = captureSnapshotWithShadowLooper()

    // Should match exactly (0% pixel diff)
    GoldenImageDiff.assertMatches(
      expected = baselineSnapshot,
      actual = secondSnapshot,
      maxDiffPercentage = 0.0,
      message = "Repeated map captures must be 100% pixel identical",
    )
  }

  @Test
  public fun sample_visualRegressionIsDetectedWhenOverlayColorChanges() = runTest {
    val center = LatLng(0.0, 0.0)
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 5f))

    // Baseline render with BLUE polygon
    val polygon = checkNotNull(
      googleMap.addPolygon(
        PolygonOptions()
          .add(LatLng(-5.0, -5.0), LatLng(-5.0, 5.0), LatLng(5.0, 5.0), LatLng(5.0, -5.0))
          .fillColor(Color.BLUE)
          .strokeWidth(0f),
      ),
    )

    val baselineSnapshot = captureSnapshotWithShadowLooper()

    // Introduce visual regression: change fill color to GREEN
    polygon.fillColor = Color.GREEN
    val modifiedSnapshot = captureSnapshotWithShadowLooper()

    // Verify that GoldenImageDiff flags the visual regression
    val diff = GoldenImageDiff.compare(baselineSnapshot, modifiedSnapshot)
    assertThat(diff.matches).isFalse()
    assertThat(diff.diffPixelCount).isGreaterThan(0)

    assertThrows(AssertionError::class.java) {
      GoldenImageDiff.assertMatches(baselineSnapshot, modifiedSnapshot, maxDiffPercentage = 0.0)
    }
  }

  @Test
  public fun sample_verifiesAgainstCommittedGoldenBaseline() {
    val stream = javaClass.classLoader!!.getResourceAsStream("goldens/sample_map_baseline.png")
    assertThat(stream).isNotNull()
    val committedGolden = BitmapFactory.decodeStream(stream)
    assertThat(committedGolden).isNotNull()

    // Re-generate using the exact same deterministic pipeline
    val gridStrategy = GoldenTileStrategy.CoordinateGrid(borderWidth = 2f)
    val composite = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(composite)
    for (c in 0 until 3) {
      for (r in 0 until 3) {
        val tile = gridStrategy.generateTile(2624 + c, 6331 + r, 14, 200)
        canvas.drawBitmap(tile, (c * 200).toFloat(), (r * 200).toFloat(), null)
        tile.recycle()
      }
    }
    val currentRender = renderSampleOverlays(composite)

    // Compare against committed golden reference
    GoldenImageDiff.assertMatches(
      expected = committedGolden,
      actual = currentRender,
      maxDiffPercentage = 0.0,
      message = "Current render must match committed golden baseline image",
    )
  }

  private fun renderSampleOverlays(base: Bitmap): Bitmap {
    val bitmap = base.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(bitmap)

    val polyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.argb(140, 66, 133, 244)
      style = Paint.Style.FILL
    }
    val path = Path().apply {
      moveTo(80f, 80f)
      lineTo(320f, 120f)
      lineTo(260f, 380f)
      lineTo(100f, 300f)
      close()
    }
    canvas.drawPath(path, polyPaint)

    val polyStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.DKGRAY
      style = Paint.Style.STROKE
      strokeWidth = 3f
    }
    canvas.drawPath(path, polyStrokePaint)

    val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.argb(180, 234, 67, 53)
      style = Paint.Style.FILL
    }
    canvas.drawCircle(440f, 360f, 90f, circlePaint)

    val circleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.BLACK
      style = Paint.Style.STROKE
      strokeWidth = 3f
    }
    canvas.drawCircle(440f, 360f, 90f, circleStrokePaint)

    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.RED
      style = Paint.Style.FILL
    }
    canvas.drawCircle(300f, 250f, 18f, pinPaint)
    val pinShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.BLACK
      style = Paint.Style.STROKE
      strokeWidth = 4f
    }
    canvas.drawCircle(300f, 250f, 18f, pinShadow)

    return bitmap
  }

  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  private fun TestScope.captureSnapshotWithShadowLooper(): Bitmap {
    val deferred = async { googleMap.captureSnapshot() }
    testScheduler.advanceUntilIdle()
    ShadowLooper.idleMainLooper()
    testScheduler.advanceUntilIdle()
    return deferred.getCompleted()
  }
}
// [END maps_sample_golden_map_test]
