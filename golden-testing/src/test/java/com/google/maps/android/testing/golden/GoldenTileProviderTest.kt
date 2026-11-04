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

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// [START maps_golden_tile_provider_test]
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
public class GoldenTileProviderTest {

  @Test
  public fun getTile_returnsValidTileWithEncodedPngData() {
    val provider = GoldenTileProvider(
      strategy = GoldenTileStrategy.SolidColorHash(),
      tileSize = 128,
    )

    val tile = provider.getTile(x = 10, y = 20, zoom = 5)
    assertThat(tile).isNotNull()
    assertThat(tile.width).isEqualTo(128)
    assertThat(tile.height).isEqualTo(128)
    assertThat(tile.data).isNotNull()
    assertThat(tile.data?.isNotEmpty()).isTrue()

    // Verify decoded bitmap matches dimensions
    val decoded = BitmapFactory.decodeByteArray(tile.data, 0, tile.data!!.size)
    assertThat(decoded).isNotNull()
    assertThat(decoded.width).isEqualTo(128)
    assertThat(decoded.height).isEqualTo(128)
  }

  @Test
  public fun getTile_usesLruCacheOnRepeatedRequests() {
    var generateCount = 0
    val countingStrategy = GoldenTileStrategy.Custom { x, y, zoom, tileSize ->
      generateCount++
      GoldenTileStrategy.SolidColorHash().generateTile(x, y, zoom, tileSize)
    }

    val provider = GoldenTileProvider(strategy = countingStrategy, tileSize = 64)
    assertThat(provider.cachedTileCount()).isEqualTo(0)

    // First call generates and caches
    val tile1 = provider.getTile(1, 2, 3)
    assertThat(generateCount).isEqualTo(1)
    assertThat(provider.cachedTileCount()).isEqualTo(1)

    // Second call for same coordinates retrieves from cache
    val tile2 = provider.getTile(1, 2, 3)
    assertThat(generateCount).isEqualTo(1)
    assertThat(tile1.data).isSameInstanceAs(tile2.data)

    // Different tile coordinate invokes generator
    provider.getTile(2, 2, 3)
    assertThat(generateCount).isEqualTo(2)
    assertThat(provider.cachedTileCount()).isEqualTo(2)
  }

  @Test
  public fun clearCache_emptiesCache() {
    val provider = GoldenTileProvider(tileSize = 64)
    provider.getTile(1, 1, 1)
    provider.getTile(2, 2, 2)
    assertThat(provider.cachedTileCount()).isEqualTo(2)

    provider.clearCache()
    assertThat(provider.cachedTileCount()).isEqualTo(0)
  }
}
// [END maps_golden_tile_provider_test]
