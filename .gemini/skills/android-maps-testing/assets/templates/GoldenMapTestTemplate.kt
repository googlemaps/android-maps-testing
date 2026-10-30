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

package com.example.maptest.templates

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.maps.robolectric.annotation.EnableMapsShadows
import com.google.android.maps.testing.golden.GoldenImageDiff
import com.google.android.maps.testing.golden.GoldenTileStrategy
import com.google.android.maps.testing.golden.enableGoldenTesting
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.atomic.AtomicReference

/**
 * Standard template for deterministic, offline golden image screenshot testing.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@EnableMapsShadows
class GoldenMapTestTemplate {

  private lateinit var mapView: MapView
  private lateinit var googleMap: GoogleMap

  @Before
  fun setUp() {
    val activity = Robolectric.buildActivity(Activity::class.java).create().get()
    mapView = MapView(activity).apply {
      measure(
        View.MeasureSpec.makeMeasureSpec(256, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(256, View.MeasureSpec.EXACTLY),
      )
      layout(0, 0, 256, 256)
      onCreate(Bundle())
    }

    val mapRef = AtomicReference<GoogleMap>()
    mapView.getMapAsync { mapRef.set(it) }
    ShadowLooper.idleMainLooper()
    googleMap = checkNotNull(mapRef.get())

    // 100% offline deterministic synthetic tiles
    googleMap.enableGoldenTesting(strategy = GoldenTileStrategy.CoordinateGrid())
  }

  @Test
  fun testDeterministicGoldenRendering() = runTest {
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(37.7749, -122.4194), 12f))
    googleMap.addCircle(
      CircleOptions()
        .center(LatLng(37.7749, -122.4194))
        .radius(2000.0)
        .fillColor(Color.argb(128, 255, 0, 0))
        .strokeColor(Color.RED)
        .strokeWidth(2f),
    )

    // Capture first snapshot
    val snapshotRef1 = AtomicReference<Bitmap>()
    googleMap.snapshot { snapshotRef1.set(it) }
    ShadowLooper.idleMainLooper()
    val baseline = snapshotRef1.get()

    // Capture second snapshot
    val snapshotRef2 = AtomicReference<Bitmap>()
    googleMap.snapshot { snapshotRef2.set(it) }
    ShadowLooper.idleMainLooper()
    val actual = snapshotRef2.get()

    // Assert exact match (0% diff)
    GoldenImageDiff.assertMatches(
      expected = baseline,
      actual = actual,
      maxDiffPercentage = 0.0,
      message = "Map rendering must be 100% deterministic",
    )
  }
}
