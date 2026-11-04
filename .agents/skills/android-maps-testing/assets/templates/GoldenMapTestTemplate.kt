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

import android.graphics.Color
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.robolectric.annotation.EnableMapsShadows
import com.google.maps.android.robolectric.ktx.withTestMap
import com.google.maps.android.testing.golden.GoldenImageDiff
import com.google.maps.android.testing.golden.GoldenTileStrategy
import com.google.maps.android.testing.golden.assertMatchesGoldenBlocking
import com.google.maps.android.testing.golden.captureSnapshot
import com.google.maps.android.testing.golden.enableGoldenTesting
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Standard template for deterministic, offline golden image screenshot testing.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@EnableMapsShadows
class GoldenMapTestTemplate {

  @Test
  fun testDeterministicGoldenRendering() = runTest {
    withTestMap(width = 256, height = 256) {
      // 1. 100% offline deterministic synthetic tiles
      enableGoldenTesting(strategy = GoldenTileStrategy.CoordinateGrid())

      moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(37.7749, -122.4194), 12f))
      addCircle(
        CircleOptions()
          .center(LatLng(37.7749, -122.4194))
          .radius(2000.0)
          .fillColor(Color.argb(128, 255, 0, 0))
          .strokeColor(Color.RED)
          .strokeWidth(2f),
      )

      // 2. Capture snapshots directly (ShadowLooper is idled automatically on Robolectric)
      val baseline = captureSnapshot()
      val actual = captureSnapshot()

      // 3. Assert exact match (0% diff)
      GoldenImageDiff.assertMatches(
        expected = baseline,
        actual = actual,
        maxDiffPercentage = 0.0,
        message = "Map rendering must be 100% deterministic",
      )

      // Or verify/record against a committed PNG file in 1 line:
      // assertMatchesGoldenBlocking(File("src/test/resources/goldens/sf_circle.png"))
    }
  }
}
