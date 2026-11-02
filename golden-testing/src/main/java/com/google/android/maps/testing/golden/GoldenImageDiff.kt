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
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

// [START maps_golden_image_diff]
/**
 * Result of comparing an actual rendered bitmap against a golden baseline bitmap.
 *
 * @property matches True if the comparison passed within configured tolerance thresholds.
 * @property diffPixelCount Total number of pixels that differed beyond the configured tolerance threshold.
 * @property totalPixelCount Total number of pixels evaluated (`width * height`).
 * @property diffPercentage Ratio of differing pixels to total pixels (0.0 to 1.0).
 * @property diffBitmap Visual representation of differences, highlighting mismatches in red.
 */
public data class DiffResult(
  public val matches: Boolean,
  public val diffPixelCount: Int,
  public val totalPixelCount: Int,
  public val diffPercentage: Double,
  public val diffBitmap: Bitmap?,
)

/**
 * Image comparison and assertion engine for golden image testing.
 */
public object GoldenImageDiff {

  /**
   * Compares [actual] against [expected] bitmap pixel-by-pixel.
   *
   * @param expected The baseline golden bitmap.
   * @param actual The newly rendered bitmap.
   * @param maxDiffPercentage Maximum acceptable ratio of mismatched pixels (0.0 = exact match). Defaults to 0.001 (0.1%).
   * @param pixelColorTolerance Per-channel RGB tolerance (0..255). Differences within this threshold are ignored. Defaults to 0.
   * @param generateDiffBitmap Whether to generate a visual diff [Bitmap]. Defaults to true.
   * @return A [DiffResult] summarizing differences.
   * @throws IllegalArgumentException If [expected] and [actual] dimensions do not match.
   */
  public fun compare(
    expected: Bitmap,
    actual: Bitmap,
    maxDiffPercentage: Double = 0.001,
    pixelColorTolerance: Int = 0,
    generateDiffBitmap: Boolean = true,
  ): DiffResult {
    require(expected.width == actual.width && expected.height == actual.height) {
      "Bitmap dimensions do not match: expected (${expected.width}x${expected.height}) vs actual (${actual.width}x${actual.height})"
    }

    val width = expected.width
    val height = expected.height
    val totalPixels = width * height

    val (safeExpected, recycledExpected) = expected.toSoftwareBitmap()
    val (safeActual, recycledActual) = actual.toSoftwareBitmap()

    val expectedPixels = IntArray(totalPixels)
    val actualPixels = IntArray(totalPixels)
    try {
      safeExpected.getPixels(expectedPixels, 0, width, 0, 0, width, height)
      safeActual.getPixels(actualPixels, 0, width, 0, 0, width, height)
    } finally {
      if (recycledExpected) safeExpected.recycle()
      if (recycledActual) safeActual.recycle()
    }

    var diffPixels = 0
    val diffPixelsOutput = if (generateDiffBitmap) IntArray(totalPixels) else null

    for (i in 0 until totalPixels) {
      val exp = expectedPixels[i]
      val act = actualPixels[i]

      if (colorsDiffer(exp, act, pixelColorTolerance)) {
        diffPixels++
        diffPixelsOutput?.set(i, Color.RED)
      } else {
        // Dim matching pixels to a semi-transparent gray in diff image
        diffPixelsOutput?.let {
          val gray = (Color.red(act) * 0.299 + Color.green(act) * 0.587 + Color.blue(act) * 0.114).toInt()
          it[i] = Color.argb(64, gray, gray, gray)
        }
      }
    }

    val diffRatio = diffPixels.toDouble() / totalPixels.toDouble()
    val matches = diffRatio <= maxDiffPercentage

    val diffBitmap = diffPixelsOutput?.let {
      val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      bitmap.setPixels(it, 0, width, 0, 0, width, height)
      bitmap
    }

    return DiffResult(
      matches = matches,
      diffPixelCount = diffPixels,
      totalPixelCount = totalPixels,
      diffPercentage = diffRatio,
      diffBitmap = diffBitmap,
    )
  }

  /**
   * Asserts that [actual] matches [expected] within the given tolerance.
   *
   * When [outputDir] is provided and the comparison fails, `<artifactName>_actual.png` and
   * `<artifactName>_diff.png` are automatically written to [outputDir] and their file paths are
   * included in the thrown [AssertionError].
   *
   * @throws AssertionError If dimensions differ or difference percentage exceeds [maxDiffPercentage].
   */
  public fun assertMatches(
    expected: Bitmap,
    actual: Bitmap,
    maxDiffPercentage: Double = 0.001,
    pixelColorTolerance: Int = 0,
    message: String? = null,
    artifactName: String = "golden",
    outputDir: File? = null,
  ) {
    val result = compare(
      expected = expected,
      actual = actual,
      maxDiffPercentage = maxDiffPercentage,
      pixelColorTolerance = pixelColorTolerance,
      generateDiffBitmap = outputDir != null,
    )

    if (!result.matches) {
      val percentFormatted = String.format("%.3f%%", result.diffPercentage * 100)
      val thresholdFormatted = String.format("%.3f%%", maxDiffPercentage * 100)
      val prefix = if (message != null) "$message: " else ""
      val artifactDiagnostic = if (outputDir != null) {
        outputDir.mkdirs()
        val actualFile = File(outputDir, "${artifactName}_actual.png")
        val diffFile = File(outputDir, "${artifactName}_diff.png")
        actual.writePngTo(actualFile)
        result.diffBitmap?.writePngTo(diffFile)
        " Failure artifacts written to: actual=${actualFile.path}, diff=${diffFile.path}"
      } else {
        ""
      }
      throw AssertionError(
        "${prefix}Golden image mismatch: $percentFormatted differing pixels (${result.diffPixelCount}/${result.totalPixelCount}) " +
          "exceeded maximum allowed threshold of $thresholdFormatted.$artifactDiagnostic",
      )
    }
  }

  /**
   * Compares [actual] against a PNG baseline stored at [goldenFile], or records [actual] as the new
   * baseline when [recordBaseline] is `true` (or `-Dmaps.golden.record=true` is set).
   *
   * On mismatch, `<name>_actual.png` and `<name>_diff.png` are written to [outputDir].
   */
  public fun assertMatches(
    goldenFile: File,
    actual: Bitmap,
    maxDiffPercentage: Double = 0.001,
    pixelColorTolerance: Int = 0,
    message: String? = null,
    outputDir: File? = goldenFile.parentFile,
    recordBaseline: Boolean = System.getProperty("maps.golden.record").toBoolean(),
  ) {
    if (recordBaseline) {
      goldenFile.parentFile?.mkdirs()
      actual.writePngTo(goldenFile)
      return
    }

    if (!goldenFile.exists()) {
      throw AssertionError(
        "Golden baseline file does not exist: ${goldenFile.path}. " +
          "Run with -Dmaps.golden.record=true (or recordBaseline = true) to record the initial baseline.",
      )
    }

    val expected = checkNotNull(BitmapFactory.decodeFile(goldenFile.path)) {
      "Failed to decode golden baseline PNG at ${goldenFile.path}"
    }
    assertMatches(
      expected = expected,
      actual = actual,
      maxDiffPercentage = maxDiffPercentage,
      pixelColorTolerance = pixelColorTolerance,
      message = message,
      artifactName = goldenFile.nameWithoutExtension,
      outputDir = outputDir,
    )
  }

  private fun Bitmap.writePngTo(file: File) {
    val (softwareBitmap, shouldRecycle) = toSoftwareBitmap()
    try {
      FileOutputStream(file).use { out ->
        softwareBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
      }
    } finally {
      if (shouldRecycle) {
        softwareBitmap.recycle()
      }
    }
  }

  private fun colorsDiffer(c1: Int, c2: Int, tolerance: Int): Boolean {
    if (c1 == c2) return false
    if (tolerance <= 0) return true

    val dr = abs(Color.red(c1) - Color.red(c2))
    val dg = abs(Color.green(c1) - Color.green(c2))
    val db = abs(Color.blue(c1) - Color.blue(c2))
    val da = abs(Color.alpha(c1) - Color.alpha(c2))

    return dr > tolerance || dg > tolerance || db > tolerance || da > tolerance
  }

  private fun Bitmap.toSoftwareBitmap(): Pair<Bitmap, Boolean> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && config == Bitmap.Config.HARDWARE) {
    copy(Bitmap.Config.ARGB_8888, false) to true
  } else {
    this to false
  }
}
// [END maps_golden_image_diff]
