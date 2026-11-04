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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.graphics.ColorUtils
import kotlin.math.abs

// [START maps_golden_tile_strategy]
/**
 * Strategy contract for generating deterministic synthetic map tiles for golden image testing.
 */
public sealed interface GoldenTileStrategy {

  /**
   * Generates a deterministic [Bitmap] tile for the given Web Mercator tile coordinate.
   *
   * @param x The tile X coordinate (0 <= x < 2^zoom).
   * @param y The tile Y coordinate (0 <= y < 2^zoom).
   * @param zoom The map zoom level.
   * @param tileSize The tile width and height in pixels (default 256).
   * @return A non-null [Bitmap] representing the tile surface.
   */
  public fun generateTile(x: Int, y: Int, zoom: Int, tileSize: Int = 256): Bitmap

  /**
   * Solid color strategy mapping `(x, y, zoom)` deterministically to an HSL color palette.
   * Guarantees consistent background coloring across runs without network dependency.
   */
  public data class SolidColorHash(
    public val saturation: Float = 0.55f,
    public val lightness: Float = 0.72f,
  ) : GoldenTileStrategy {
    override fun generateTile(x: Int, y: Int, zoom: Int, tileSize: Int): Bitmap {
      val bitmap = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888)
      val color = calculateDeterministicColor(x, y, zoom, saturation, lightness)
      bitmap.eraseColor(color)
      return bitmap
    }
  }

  /**
   * Coordinate grid strategy. Draws a deterministic pastel background with tile coordinate
   * text (`z/x/y`) and a subtle boundary border.
   * Ideal for visual debugging of tile boundaries and projection alignment.
   */
  public data class CoordinateGrid(
    public val saturation: Float = 0.45f,
    public val lightness: Float = 0.80f,
    public val textColor: Int = Color.DKGRAY,
    public val borderColor: Int = 0x44000000,
    public val borderWidth: Float = 2f,
    public val textSizeSp: Float = 16f,
  ) : GoldenTileStrategy {
    override fun generateTile(x: Int, y: Int, zoom: Int, tileSize: Int): Bitmap {
      val bitmap = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)

      // Background
      val bgColor = calculateDeterministicColor(x, y, zoom, saturation, lightness)
      canvas.drawColor(bgColor)

      // Border
      if (borderWidth > 0f) {
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = borderColor
          style = Paint.Style.STROKE
          strokeWidth = borderWidth
        }
        canvas.drawRect(0f, 0f, tileSize.toFloat(), tileSize.toFloat(), borderPaint)
      }

      // Coordinate Text
      val label = "z$zoom\nx:$x\ny:$y"
      val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = tileSize * (textSizeSp / 256f)
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
      }

      val lines = label.split("\n")
      val lineHeight = textPaint.descent() - textPaint.ascent()
      val totalHeight = lines.size * lineHeight
      var startY = (tileSize - totalHeight) / 2f - textPaint.ascent()

      for (line in lines) {
        canvas.drawText(line, tileSize / 2f, startY, textPaint)
        startY += lineHeight
      }

      return bitmap
    }
  }

  /**
   * Alternating checkerboard pattern based on `(x + y) % 2`.
   * Provides high visual contrast across adjacent tiles.
   */
  public data class Checkerboard(
    public val colorEven: Int = 0xFFECEFF1.toInt(),
    public val colorOdd: Int = 0xFFCFD8DC.toInt(),
  ) : GoldenTileStrategy {
    override fun generateTile(x: Int, y: Int, zoom: Int, tileSize: Int): Bitmap {
      val bitmap = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888)
      val isEven = (x + y) % 2 == 0
      bitmap.eraseColor(if (isEven) colorEven else colorOdd)
      return bitmap
    }
  }

  /**
   * Custom strategy delegating to a user-supplied lambda.
   */
  public class Custom(
    private val block: (x: Int, y: Int, zoom: Int, tileSize: Int) -> Bitmap,
  ) : GoldenTileStrategy {
    override fun generateTile(x: Int, y: Int, zoom: Int, tileSize: Int): Bitmap = block(x, y, zoom, tileSize)
  }

  public companion object {
    /**
     * Computes a deterministic ARGB color integer from `(x, y, zoom)`.
     */
    public fun calculateDeterministicColor(
      x: Int,
      y: Int,
      zoom: Int,
      saturation: Float = 0.55f,
      lightness: Float = 0.72f,
    ): Int {
      // Prime-based hash to scatter hues evenly across adjacent tiles
      val rawHash = (x * 73856093) xor (y * 19349663) xor (zoom * 83492791)
      val hue = (abs(rawHash) % 360).toFloat()
      return ColorUtils.HSLToColor(floatArrayOf(hue, saturation, lightness))
    }
  }
}
// [END maps_golden_tile_strategy]
