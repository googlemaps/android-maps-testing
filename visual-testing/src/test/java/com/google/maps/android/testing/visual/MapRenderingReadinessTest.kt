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

package com.google.maps.android.testing.visual

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MapRenderingReadinessTest {

  @Test
  fun hasRenderedMapContent_blankSurface_returnsFalse() {
    val blank = createSolidBitmap(Color.rgb(239, 236, 230))
    assertThat(MapRenderingReadiness.hasRenderedMapContent(blank)).isFalse()
    blank.recycle()
  }

  @Test
  fun hasRenderedMapContent_watermarkOnlyInCorner_returnsFalse() {
    val blankWithWatermark = createSolidBitmap(Color.rgb(239, 236, 230))
    // Draw multicolored Google logo / controls only inside the bottom-left 10% corner
    val colors = listOf(Color.BLUE, Color.RED, Color.YELLOW, Color.GREEN, Color.BLACK, Color.CYAN)
    colors.forEachIndexed { idx, color ->
      fillRect(blankWithWatermark, idx * 2, 185, idx * 2 + 2, 198, color)
    }

    assertThat(MapRenderingReadiness.hasRenderedMapContent(blankWithWatermark)).isFalse()
    blankWithWatermark.recycle()
  }

  @Test
  fun hasRenderedMapContent_faintTwoColorLoadingGrid_returnsFalse() {
    val grid = createSolidBitmap(Color.rgb(239, 236, 230))
    val gridColor = Color.rgb(220, 217, 211)
    for (pos in 0 until 200 step 32) {
      fillRect(grid, pos, 0, pos + 1, 200, gridColor)
      fillRect(grid, 0, pos, 200, pos + 1, gridColor)
    }

    assertThat(MapRenderingReadiness.hasRenderedMapContent(grid)).isFalse()
    grid.recycle()
  }

  @Test
  fun hasRenderedMapContent_renderedMapWithTilesAndMarkers_returnsTrue() {
    val rendered = createRenderedMapBitmap(offset = 0)
    assertThat(MapRenderingReadiness.hasRenderedMapContent(rendered)).isTrue()
    rendered.recycle()
  }

  @Test
  fun hasRenderedMapContent_respectsExplicitMapBounds() {
    // Full screen has colorful header at top (y: 0..60), but map bounds (y: 60..200) are blank
    val screen = createSolidBitmap(Color.rgb(239, 236, 230))
    val headerColors = listOf(Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.MAGENTA, Color.CYAN)
    headerColors.forEachIndexed { idx, color ->
      fillRect(screen, idx * 30, 0, (idx + 1) * 30, 50, color)
    }

    val mapBounds = Rect(0, 60, 200, 200)
    assertThat(MapRenderingReadiness.hasRenderedMapContent(screen, mapBounds)).isFalse()
    screen.recycle()
  }

  @Test
  fun areFramesSettled_detectsChangingVsStableFrames() {
    val frameA = createRenderedMapBitmap(offset = 0)
    val frameB = createRenderedMapBitmap(offset = 25)
    val frameC = createRenderedMapBitmap(offset = 25)

    assertThat(MapRenderingReadiness.areFramesSettled(frameA, frameB)).isFalse()
    assertThat(MapRenderingReadiness.areFramesSettled(frameB, frameC)).isTrue()

    frameA.recycle()
    frameB.recycle()
    frameC.recycle()
  }

  @Test
  fun waitUntilSettled_waitsPastFiveSecondsUntilMapRendersAndSettles() {
    var clock = 0L
    // 14 blank frames (7 seconds at 500ms/poll), then 1 animating frame, then 2 settled frames
    val frames = ArrayDeque<Bitmap>().apply {
      repeat(14) { add(createSolidBitmap(Color.rgb(239, 236, 230))) }
      add(createRenderedMapBitmap(offset = 0))
      add(createRenderedMapBitmap(offset = 20))
      add(createRenderedMapBitmap(offset = 20))
    }

    val settled = MapRenderingReadiness.waitUntilSettled(
      timeoutMillis = 30_000L,
      pollIntervalMillis = 500L,
      nowMillis = { clock },
      sleepMillis = { clock += it },
      boundsProvider = { Rect(0, 0, 200, 200) },
      captureFrame = { frames.removeFirstOrNull() },
    )

    assertThat(settled).isTrue()
    // Proves it waited beyond the old 5s cap and returned as soon as frame 16 matched frame 15
    assertThat(clock).isEqualTo(8_000L)
  }

  @Test
  fun waitUntilSettled_returnsFalseWhenBlankUntilTimeout() {
    var clock = 0L

    val settled = MapRenderingReadiness.waitUntilSettled(
      timeoutMillis = 10_000L,
      pollIntervalMillis = 500L,
      nowMillis = { clock },
      sleepMillis = { clock += it },
      boundsProvider = { null },
      captureFrame = { createSolidBitmap(Color.rgb(239, 236, 230)) },
    )

    assertThat(settled).isFalse()
    assertThat(clock).isEqualTo(10_000L)
  }

  @Test
  fun waitUntilSettled_nonPositiveTimeout_returnsFalseImmediately() {
    var captures = 0
    val settled = MapRenderingReadiness.waitUntilSettled(
      timeoutMillis = 0L,
      nowMillis = { 0L },
      sleepMillis = {},
      boundsProvider = { null },
      captureFrame = {
        captures++
        createRenderedMapBitmap(offset = 0)
      },
    )

    assertThat(settled).isFalse()
    assertThat(captures).isEqualTo(0)
  }

  private fun createSolidBitmap(color: Int, size: Int = 200): Bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
    eraseColor(color)
  }

  private fun createRenderedMapBitmap(offset: Int, size: Int = 200): Bitmap {
    val bitmap = createSolidBitmap(Color.rgb(239, 236, 230), size)
    val bands = listOf(
      Color.rgb(170, 218, 255), // water
      Color.rgb(203, 230, 163), // park
      Color.rgb(255, 255, 255), // arterial road
      Color.rgb(254, 224, 139), // highway
      Color.rgb(234, 67, 53), // red marker pin
      Color.rgb(66, 133, 244), // blue route/water label
      Color.rgb(95, 99, 104), // street label
    )
    bands.forEachIndexed { idx, color ->
      val top = (idx * 24 + offset) % size
      fillRect(bitmap, 0, top, size, (top + 20).coerceAtMost(size), color)
    }
    return bitmap
  }

  private fun fillRect(bitmap: Bitmap, left: Int, top: Int, right: Int, bottom: Int, color: Int) {
    val clampedLeft = left.coerceIn(0, bitmap.width)
    val clampedRight = right.coerceIn(clampedLeft, bitmap.width)
    val clampedTop = top.coerceIn(0, bitmap.height)
    val clampedBottom = bottom.coerceIn(clampedTop, bitmap.height)
    for (y in clampedTop until clampedBottom) {
      for (x in clampedLeft until clampedRight) {
        bitmap.setPixel(x, y, color)
      }
    }
  }
}
