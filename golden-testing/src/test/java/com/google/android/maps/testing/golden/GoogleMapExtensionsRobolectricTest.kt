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

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.maps.robolectric.annotation.EnableMapsShadows
import com.google.android.maps.robolectric.shadows.ShadowGoogleMap
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.atomic.AtomicReference

// [START maps_golden_extensions_test]
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@EnableMapsShadows
public class GoogleMapExtensionsRobolectricTest {

  private lateinit var activity: Activity
  private lateinit var mapView: MapView
  private lateinit var googleMap: GoogleMap
  private lateinit var shadowGoogleMap: ShadowGoogleMap

  @Before
  public fun setUp() {
    activity = Robolectric.buildActivity(Activity::class.java).create().get()
    mapView = MapView(activity)
    mapView.measure(
      View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
    )
    mapView.layout(0, 0, 100, 100)
    mapView.onCreate(Bundle())

    val mapRef = AtomicReference<GoogleMap>()
    mapView.getMapAsync { mapRef.set(it) }
    ShadowLooper.idleMainLooper()

    googleMap = checkNotNull(mapRef.get())
    shadowGoogleMap = Shadow.extract(googleMap) as ShadowGoogleMap
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
  public fun awaitMapLoaded_suspendsUntilNotified() = runTest {
    val loadDeferred = async {
      googleMap.awaitMapLoaded()
    }

    testScheduler.advanceUntilIdle()
    assertThat(loadDeferred.isCompleted).isFalse()

    shadowGoogleMap.notifyMapLoaded()
    testScheduler.advanceUntilIdle()

    assertThat(loadDeferred.isCompleted).isTrue()
  }

  @Test
  public fun awaitMapLoaded_autoDispatchesOnMainLooperIdle() = runTest {
    // [START golden_await_map_loaded]
    val loadDeferred = async {
      googleMap.awaitMapLoaded()
    }
    testScheduler.advanceUntilIdle()

    // Idling the Android main looper automatically dispatches OnMapLoadedCallback
    // without requiring manual ShadowGoogleMap extraction
    ShadowLooper.idleMainLooper()
    testScheduler.advanceUntilIdle()

    assertThat(loadDeferred.isCompleted).isTrue()
    // [END golden_await_map_loaded]
  }

  @Test
  public fun captureSnapshot_returnsBitmap() = runTest {
    val snapshotDeferred = async {
      googleMap.captureSnapshot()
    }

    testScheduler.advanceUntilIdle()
    ShadowLooper.idleMainLooper()
    testScheduler.advanceUntilIdle()

    val bitmap = snapshotDeferred.await()
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

    val snapshotDeferred = async {
      googleMap.captureSnapshot()
    }
    testScheduler.advanceUntilIdle()
    ShadowLooper.idleMainLooper()
    testScheduler.advanceUntilIdle()

    val snapshot = snapshotDeferred.await()
    // Verify that ShadowMapView rendered the TileOverlay's tiles onto the snapshot canvas
    assertThat(snapshot.getPixel(50, 50)).isEqualTo(Color.CYAN)
    // [END golden_tile_rendering_jvm]
  }
}
// [END maps_golden_extensions_test]
