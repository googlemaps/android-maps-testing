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
import android.graphics.Rect

/**
 * Pure pixel-sampling heuristics for detecting when a Google Map surface has finished
 * rendering tiles/overlays and settled between consecutive frames.
 */
internal object MapRenderingReadiness {
  private const val GRID_STEPS = 16
  private const val EDGE_INSET_FRACTION = 0.15f
  private const val MIN_INSET_DIMENSION_PX = 20
  private const val CHANNEL_QUANTIZATION_MASK = 0xF8
  private const val MIN_DISTINCT_COLOR_BUCKETS = 6
  private const val MIN_NON_DOMINANT_RATIO = 0.05
  private const val MIN_STABLE_SAMPLE_RATIO = 0.98

  /**
   * Returns `true` when the interior of [bounds] (or the full [bitmap] when [bounds] is null/empty)
   * contains enough color diversity and non-background coverage to indicate rendered map tiles
   * rather than a blank or grid-only loading surface.
   */
  fun hasRenderedMapContent(bitmap: Bitmap, bounds: Rect? = null): Boolean {
    val samples = sampleInteriorColors(bitmap, bounds)
    if (samples.isEmpty()) return false

    val counts = HashMap<Int, Int>()
    var maxCount = 0
    for (color in samples) {
      val updated = (counts[color] ?: 0) + 1
      counts[color] = updated
      if (updated > maxCount) {
        maxCount = updated
      }
    }

    val nonDominantRatio = (samples.size - maxCount).toDouble() / samples.size.toDouble()
    return counts.size >= MIN_DISTINCT_COLOR_BUCKETS && nonDominantRatio >= MIN_NON_DOMINANT_RATIO
  }

  /**
   * Returns `true` when [previous] and [current] agree on at least 98% of sampled interior pixels,
   * indicating that tile fade-ins and camera animations have settled.
   */
  fun areFramesSettled(previous: Bitmap, current: Bitmap, bounds: Rect? = null): Boolean {
    if (previous.width != current.width || previous.height != current.height) return false
    val prevSamples = sampleInteriorColors(previous, bounds)
    val currSamples = sampleInteriorColors(current, bounds)
    if (prevSamples.isEmpty() || prevSamples.size != currSamples.size) return false

    var matching = 0
    for (i in prevSamples.indices) {
      if (prevSamples[i] == currSamples[i]) {
        matching++
      }
    }
    return matching.toDouble() / prevSamples.size.toDouble() >= MIN_STABLE_SAMPLE_RATIO
  }

  /**
   * Polls [captureFrame] until [hasRenderedMapContent] is true and two consecutive rendered frames
   * satisfy [areFramesSettled], or until [timeoutMillis] elapses.
   *
   * Returns `true` if the map rendered and settled before the deadline, or `false` on timeout.
   */
  fun waitUntilSettled(
    timeoutMillis: Long,
    pollIntervalMillis: Long = 500L,
    nowMillis: () -> Long,
    sleepMillis: (Long) -> Unit,
    boundsProvider: () -> Rect?,
    captureFrame: () -> Bitmap?,
  ): Boolean {
    if (timeoutMillis <= 0L) return false

    val deadline = nowMillis() + timeoutMillis
    var previousRendered: Bitmap? = null

    try {
      while (true) {
        val current = captureFrame()
        if (current != null) {
          val bounds = boundsProvider()
          if (hasRenderedMapContent(current, bounds)) {
            val prev = previousRendered
            if (prev != null && areFramesSettled(prev, current, bounds)) {
              current.recycle()
              return true
            }
            prev?.recycle()
            previousRendered = current
          } else {
            previousRendered?.recycle()
            previousRendered = null
            current.recycle()
          }
        }

        val remaining = deadline - nowMillis()
        if (remaining <= 0L) {
          return false
        }
        sleepMillis(minOf(pollIntervalMillis, remaining))
      }
    } finally {
      previousRendered?.recycle()
    }
  }

  private fun sampleInteriorColors(bitmap: Bitmap, bounds: Rect?): IntArray {
    val region = resolveInteriorRect(bitmap.width, bitmap.height, bounds)
    val regionWidth = region.width()
    val regionHeight = region.height()
    if (regionWidth <= 0 || regionHeight <= 0) return IntArray(0)

    val stepsX = minOf(GRID_STEPS, regionWidth)
    val stepsY = minOf(GRID_STEPS, regionHeight)
    val samples = IntArray(stepsX * stepsY)
    var index = 0

    for (gy in 0 until stepsY) {
      val y = region.top + ((gy + 0.5) * regionHeight / stepsY).toInt().coerceIn(0, regionHeight - 1)
      for (gx in 0 until stepsX) {
        val x = region.left + ((gx + 0.5) * regionWidth / stepsX).toInt().coerceIn(0, regionWidth - 1)
        samples[index++] = quantizeRgb(bitmap.getPixel(x, y))
      }
    }
    return samples
  }

  private fun resolveInteriorRect(width: Int, height: Int, bounds: Rect?): Rect {
    val left = (bounds?.left ?: 0).coerceIn(0, width)
    val top = (bounds?.top ?: 0).coerceIn(0, height)
    val right = (bounds?.right ?: width).coerceIn(left, width)
    val bottom = (bounds?.bottom ?: height).coerceIn(top, height)

    val regionWidth = right - left
    val regionHeight = bottom - top
    if (regionWidth < MIN_INSET_DIMENSION_PX || regionHeight < MIN_INSET_DIMENSION_PX) {
      return Rect(left, top, right, bottom)
    }

    val insetX = (regionWidth * EDGE_INSET_FRACTION).toInt()
    val insetY = (regionHeight * EDGE_INSET_FRACTION).toInt()
    return Rect(left + insetX, top + insetY, right - insetX, bottom - insetY)
  }

  private fun quantizeRgb(argb: Int): Int {
    val r = (argb ushr 16) and CHANNEL_QUANTIZATION_MASK
    val g = (argb ushr 8) and CHANNEL_QUANTIZATION_MASK
    val b = argb and CHANNEL_QUANTIZATION_MASK
    return (r shl 16) or (g shl 8) or b
  }
}
