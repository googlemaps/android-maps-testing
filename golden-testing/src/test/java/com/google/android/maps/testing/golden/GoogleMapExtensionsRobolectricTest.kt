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

import android.graphics.Bitmap
import android.graphics.Color
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.maps.robolectric.annotation.EnableMapsShadows
import com.google.android.maps.robolectric.ktx.createTestMap
import com.google.android.maps.robolectric.ktx.shadow
import com.google.android.maps.robolectric.ktx.withTestMap
import com.google.android.maps.robolectric.shadows.ShadowGoogleMap
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

// [START maps_golden_extensions_test]
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@EnableMapsShadows
public class GoogleMapExtensionsRobolectricTest {

  @get:Rule
  public val tempFolder: TemporaryFolder = TemporaryFolder()

  private lateinit var googleMap: GoogleMap
  private lateinit var shadowGoogleMap: ShadowGoogleMap

  @Before
  public fun setUp() {
    googleMap = createTestMap(width = 100, height = 100)
    shadowGoogleMap = googleMap.shadow
  }

  @Test
  public fun enableGoldenTesting_setsMapTypeNoneAndAddsTileOverlay() {
    val overlay = googleMap.enableGoldenTesting(
      strategy = GoldenTileStrategy.Checkerboard(),
      zIndex = -500f,
    )

    assertThat(googleMap.mapType).isEqualTo(GoogleMap.MAP_TYPE_NONE)
    assertThat(shadowGoogleMap.tileOverlays).contains(overlay)
    assertThat(overlay.zIndex).isEqualTo(-500f)
  }

  @Test
  public fun awaitMapLoaded_completesDirectlyInRobolectric() = runTest {
    // [START golden_await_map_loaded]
    // awaitMapLoaded() and captureSnapshot() work identically on Android devices and
    // Robolectric JVM without requiring manual ShadowLooper or async boilerplate:
    googleMap.awaitMapLoaded()
    val snapshot = googleMap.captureSnapshot()
    assertThat(snapshot.width).isEqualTo(100)
    // [END golden_await_map_loaded]
  }

  @Test
  public fun captureSnapshot_returnsBitmap() = runTest {
    val bitmap = googleMap.captureSnapshot()
    assertThat(bitmap).isNotNull()
    assertThat(bitmap.width).isEqualTo(100)
    assertThat(bitmap.height).isEqualTo(100)
  }

  @Test
  public fun enableGoldenTesting_rendersDeterministicTileStrategyInRobolectricSnapshot() = runTest {
    // [START golden_tile_rendering_jvm]
    // Attach a deterministic custom solid-color tile strategy on Robolectric JVM
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(37.7749, -122.4194), 10f))
    googleMap.enableGoldenTesting(
      strategy = GoldenTileStrategy.Custom { _, _, _, tileSize ->
        Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888).apply {
          eraseColor(Color.CYAN)
        }
      },
    )

    val snapshot = googleMap.captureSnapshot()
    // Verify that ShadowMapView rendered the TileOverlay's tiles onto the snapshot canvas
    assertThat(snapshot.getPixel(50, 50)).isEqualTo(Color.CYAN)
    // [END golden_tile_rendering_jvm]
  }

  @Test
  public fun assertMatchesGolden_recordsAndVerifiesInOneLine() = runTest {
    val goldenFile = File(tempFolder.newFolder("goldens"), "sf_circle.png")

    withTestMap(width = 100, height = 100) {
      enableGoldenTesting(strategy = GoldenTileStrategy.SolidColorHash())
      moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(37.7749, -122.4194), 12f))
      addCircle(
        CircleOptions()
          .center(LatLng(37.7749, -122.4194))
          .radius(500.0)
          .fillColor(Color.RED),
      )

      // 1-liner synchronous golden record & verification inside withTestMap:
      assertMatchesGoldenBlocking(goldenFile, recordBaseline = true)
      assertMatchesGoldenBlocking(goldenFile, maxDiffPercentage = 0.0)
    }

    // Also works as a suspending 1-liner in coroutine tests:
    googleMap.enableGoldenTesting(strategy = GoldenTileStrategy.SolidColorHash())
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(37.7749, -122.4194), 12f))
    googleMap.addCircle(
      CircleOptions()
        .center(LatLng(37.7749, -122.4194))
        .radius(500.0)
        .fillColor(Color.RED),
    )
    googleMap.assertMatchesGolden(goldenFile, maxDiffPercentage = 0.0)
  }
}
// [END maps_golden_extensions_test]
