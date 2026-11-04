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
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// [START maps_golden_tile_strategy_test]
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
public class GoldenTileStrategyTest {

  @Test
  public fun solidColorHash_generatesConsistentColorForSameCoordinates() {
    val strategy = GoldenTileStrategy.SolidColorHash()
    val tile1 = strategy.generateTile(x = 10, y = 20, zoom = 5, tileSize = 64)
    val tile2 = strategy.generateTile(x = 10, y = 20, zoom = 5, tileSize = 64)

    assertThat(tile1.width).isEqualTo(64)
    assertThat(tile1.height).isEqualTo(64)
    assertThat(tile1.getPixel(0, 0)).isEqualTo(tile2.getPixel(0, 0))
    assertThat(tile1.getPixel(32, 32)).isEqualTo(tile1.getPixel(0, 0)) // solid color
  }

  @Test
  public fun solidColorHash_adjacentTilesProduceDistinctColors() {
    val strategy = GoldenTileStrategy.SolidColorHash()
    val tileA = strategy.generateTile(x = 10, y = 20, zoom = 5, tileSize = 64)
    val tileB = strategy.generateTile(x = 11, y = 20, zoom = 5, tileSize = 64)
    val tileC = strategy.generateTile(x = 10, y = 21, zoom = 5, tileSize = 64)

    val colorA = tileA.getPixel(0, 0)
    val colorB = tileB.getPixel(0, 0)
    val colorC = tileC.getPixel(0, 0)

    assertThat(colorA).isNotEqualTo(colorB)
    assertThat(colorA).isNotEqualTo(colorC)
  }

  @Test
  public fun checkerboard_alternatesColorsByParity() {
    val colorEven = 0xFF112233.toInt()
    val colorOdd = 0xFF445566.toInt()
    val strategy = GoldenTileStrategy.Checkerboard(colorEven = colorEven, colorOdd = colorOdd)

    val evenTile = strategy.generateTile(x = 2, y = 4, zoom = 3, tileSize = 32)
    val oddTile = strategy.generateTile(x = 2, y = 5, zoom = 3, tileSize = 32)

    assertThat(evenTile.getPixel(0, 0)).isEqualTo(colorEven)
    assertThat(oddTile.getPixel(0, 0)).isEqualTo(colorOdd)
  }

  @Test
  public fun coordinateGrid_generatesTileWithDimensions() {
    val strategy = GoldenTileStrategy.CoordinateGrid(borderWidth = 1f)
    val tile = strategy.generateTile(x = 5, y = 8, zoom = 4, tileSize = 128)

    assertThat(tile.width).isEqualTo(128)
    assertThat(tile.height).isEqualTo(128)
  }

  @Test
  public fun customStrategy_delegatesToLambda() {
    var calledX = -1
    var calledY = -1
    var calledZoom = -1
    val strategy = GoldenTileStrategy.Custom { x, y, zoom, tileSize ->
      calledX = x
      calledY = y
      calledZoom = zoom
      val bmp = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888)
      bmp.eraseColor(Color.BLUE)
      bmp
    }

    val tile = strategy.generateTile(x = 42, y = 99, zoom = 12, tileSize = 16)
    assertThat(calledX).isEqualTo(42)
    assertThat(calledY).isEqualTo(99)
    assertThat(calledZoom).isEqualTo(12)
    assertThat(tile.getPixel(0, 0)).isEqualTo(Color.BLUE)
  }

  @Test
  public fun calculateDeterministicColor_producesDeterministicHslColors() {
    val color1 = GoldenTileStrategy.calculateDeterministicColor(x = 1, y = 2, zoom = 3)
    val color2 = GoldenTileStrategy.calculateDeterministicColor(x = 1, y = 2, zoom = 3)
    val color3 = GoldenTileStrategy.calculateDeterministicColor(x = 2, y = 2, zoom = 3)

    assertThat(color1).isEqualTo(color2)
    assertThat(color1).isNotEqualTo(color3)
  }
}
// [END maps_golden_tile_strategy_test]
