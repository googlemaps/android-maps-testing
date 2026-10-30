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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.TileProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

// [START maps_golden_instrumented_test]
/**
 * Instrumented test verifying the synthetic tile pipeline on real Android runtime.
 */
@RunWith(AndroidJUnit4::class)
public class GoldenMapInstrumentedTest {

  @Test
  public fun goldenTileProvider_generatesValidTileOnAndroidDevice() {
    val provider = GoldenTileProvider(
      strategy = GoldenTileStrategy.CoordinateGrid(),
      tileSize = 256,
    )

    val tile = provider.getTile(x = 0, y = 0, zoom = 0)
    assertThat(tile).isNotNull()
    assertThat(tile).isNotEqualTo(TileProvider.NO_TILE)
    assertThat(tile.width).isEqualTo(256)
    assertThat(tile.height).isEqualTo(256)
    assertThat(tile.data).isNotNull()
    assertThat(tile.data!!.size).isGreaterThan(0)
  }

  @Test
  public fun goldenImageDiff_evaluatesMatchingBitmapsOnDevice() {
    val strategy = GoldenTileStrategy.SolidColorHash()
    val tile1 = strategy.generateTile(1, 1, 1, 64)
    val tile2 = strategy.generateTile(1, 1, 1, 64)

    GoldenImageDiff.assertMatches(tile1, tile2, maxDiffPercentage = 0.0)
  }
}
// [END maps_golden_instrumented_test]
