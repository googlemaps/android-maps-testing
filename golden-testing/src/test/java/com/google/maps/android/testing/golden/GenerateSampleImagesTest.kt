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
import android.graphics.Path
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
public class GenerateSampleImagesTest {

  private val artifactDir: File = File("build/outputs/sample_images").apply { mkdirs() }

  @Test
  public fun generateAllSampleArtifacts() {
    if (!artifactDir.exists()) {
      artifactDir.mkdirs()
    }

    // 1. Generate 3x3 Grid of CoordinateGrid Tiles (simulating a viewport)
    val gridStrategy = GoldenTileStrategy.CoordinateGrid(borderWidth = 2f)
    val gridComposite = renderTileGrid(gridStrategy, startX = 2624, startY = 6331, zoom = 14, cols = 3, rows = 3, tileSize = 200)
    saveBitmap(gridComposite, "sample_coordinate_grid.png")

    // 2. Generate 3x3 Grid of SolidColorHash Tiles
    val solidStrategy = GoldenTileStrategy.SolidColorHash(saturation = 0.60f, lightness = 0.70f)
    val solidComposite = renderTileGrid(solidStrategy, startX = 10, startY = 20, zoom = 5, cols = 3, rows = 3, tileSize = 200)
    saveBitmap(solidComposite, "sample_solid_color_hash.png")

    // 3. Generate 3x3 Grid of Checkerboard Tiles
    val checkerStrategy = GoldenTileStrategy.Checkerboard()
    val checkerComposite = renderTileGrid(checkerStrategy, startX = 0, startY = 0, zoom = 3, cols = 3, rows = 3, tileSize = 200)
    saveBitmap(checkerComposite, "sample_checkerboard.png")

    // 4. Baseline Screenshot Simulation (Synthetic Grid + Overlays: Circle + Marker + Polygon)
    val baseline = renderMapWithOverlays(gridComposite, circleColor = Color.argb(180, 234, 67, 53), polygonColor = Color.argb(140, 66, 133, 244))
    saveBitmap(baseline, "sample_golden_baseline.png")

    // 5. Modified Screenshot Simulation (Color regression: Circle changed to orange, polygon changed to green)
    val modified = renderMapWithOverlays(gridComposite, circleColor = Color.argb(180, 251, 188, 5), polygonColor = Color.argb(140, 52, 168, 83))
    saveBitmap(modified, "sample_golden_modified.png")

    // 6. Visual Diff Image from GoldenImageDiff
    val diffResult = GoldenImageDiff.compare(baseline, modified, maxDiffPercentage = 0.0)
    saveBitmap(diffResult.diffBitmap!!, "sample_golden_diff.png")
  }

  private fun renderTileGrid(
    strategy: GoldenTileStrategy,
    startX: Int,
    startY: Int,
    zoom: Int,
    cols: Int,
    rows: Int,
    tileSize: Int,
  ): Bitmap {
    val composite = Bitmap.createBitmap(cols * tileSize, rows * tileSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(composite)

    for (c in 0 until cols) {
      for (r in 0 until rows) {
        val tile = strategy.generateTile(startX + c, startY + r, zoom, tileSize)
        canvas.drawBitmap(tile, (c * tileSize).toFloat(), (r * tileSize).toFloat(), null)
        tile.recycle()
      }
    }
    return composite
  }

  private fun renderMapWithOverlays(base: Bitmap, circleColor: Int, polygonColor: Int): Bitmap {
    val bitmap = base.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(bitmap)

    // Draw Polygon
    val polyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = polygonColor
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

    // Draw Circle
    val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = circleColor
      style = Paint.Style.FILL
    }
    canvas.drawCircle(440f, 360f, 90f, circlePaint)

    val circleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.BLACK
      style = Paint.Style.STROKE
      strokeWidth = 3f
    }
    canvas.drawCircle(440f, 360f, 90f, circleStrokePaint)

    // Draw Marker Pin
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

  private fun saveBitmap(bitmap: Bitmap, filename: String) {
    val file = File(artifactDir, filename)
    FileOutputStream(file).use { out ->
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
  }
}
