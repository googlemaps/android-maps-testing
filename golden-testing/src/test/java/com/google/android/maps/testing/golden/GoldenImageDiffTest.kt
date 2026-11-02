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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

// [START maps_golden_image_diff_test]
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
public class GoldenImageDiffTest {

  @get:Rule
  public val tempFolder: TemporaryFolder = TemporaryFolder()

  @Test
  public fun compare_exactMatchReturnsTrueWithZeroDiffs() {
    val bmp1 = createSolidBitmap(32, 32, Color.BLUE)
    val bmp2 = createSolidBitmap(32, 32, Color.BLUE)

    val result = GoldenImageDiff.compare(bmp1, bmp2)
    assertThat(result.matches).isTrue()
    assertThat(result.diffPixelCount).isEqualTo(0)
    assertThat(result.diffPercentage).isEqualTo(0.0)
  }

  @Test
  public fun compare_differentPixelsDetectedAndHighlightedInDiffBitmap() {
    val baseline = createSolidBitmap(10, 10, Color.WHITE)
    val actual = createSolidBitmap(10, 10, Color.WHITE)
    // Alter 1 pixel out of 100
    actual.setPixel(5, 5, Color.BLACK)

    val result = GoldenImageDiff.compare(baseline, actual, maxDiffPercentage = 0.0)
    assertThat(result.matches).isFalse()
    assertThat(result.diffPixelCount).isEqualTo(1)
    assertThat(result.totalPixelCount).isEqualTo(100)
    assertThat(result.diffPercentage).isEqualTo(0.01)

    assertThat(result.diffBitmap).isNotNull()
    assertThat(result.diffBitmap!!.getPixel(5, 5)).isEqualTo(Color.RED)
  }

  @Test
  public fun compare_respectsMaxDiffPercentageTolerance() {
    val baseline = createSolidBitmap(10, 10, Color.WHITE)
    val actual = createSolidBitmap(10, 10, Color.WHITE)
    actual.setPixel(0, 0, Color.BLACK) // 1% mismatch

    // Tolerance of 2% passes
    val resultPass = GoldenImageDiff.compare(baseline, actual, maxDiffPercentage = 0.02)
    assertThat(resultPass.matches).isTrue()

    // Tolerance of 0.5% fails
    val resultFail = GoldenImageDiff.compare(baseline, actual, maxDiffPercentage = 0.005)
    assertThat(resultFail.matches).isFalse()
  }

  @Test
  public fun compare_dimensionMismatchThrowsIllegalArgumentException() {
    val bmp1 = createSolidBitmap(10, 10, Color.BLUE)
    val bmp2 = createSolidBitmap(20, 20, Color.BLUE)

    assertThrows(IllegalArgumentException::class.java) {
      GoldenImageDiff.compare(bmp1, bmp2)
    }
  }

  @Test
  public fun assertMatches_throwsAssertionErrorOnMismatch() {
    val bmp1 = createSolidBitmap(10, 10, Color.BLUE)
    val bmp2 = createSolidBitmap(10, 10, Color.GREEN)

    val error = assertThrows(AssertionError::class.java) {
      GoldenImageDiff.assertMatches(bmp1, bmp2, maxDiffPercentage = 0.0)
    }
    assertThat(error.message).contains("Golden image mismatch")
  }

  @Test
  public fun assertMatches_recordsBaselineAndWritesFailureArtifacts() {
    val outDir = tempFolder.newFolder("goldens")
    val goldenFile = File(outDir, "route_overlay.png")

    // [START golden_diff_artifacts_and_record]
    val baseline = createSolidBitmap(16, 16, Color.BLUE)
    val matchingActual = createSolidBitmap(16, 16, Color.BLUE)
    val regressedActual = createSolidBitmap(16, 16, Color.BLUE).apply {
      setPixel(8, 8, Color.YELLOW)
    }

    // 1. Record the baseline PNG (or run Gradle with -Dmaps.golden.record=true)
    GoldenImageDiff.assertMatches(
      goldenFile = goldenFile,
      actual = baseline,
      recordBaseline = true,
    )
    assertThat(goldenFile.exists()).isTrue()

    // 2. Compare matching render against the recorded golden PNG
    GoldenImageDiff.assertMatches(
      goldenFile = goldenFile,
      actual = matchingActual,
      maxDiffPercentage = 0.0,
    )

    // 3. When a regression occurs, <name>_actual.png and <name>_diff.png are automatically written
    val failure = assertThrows(AssertionError::class.java) {
      GoldenImageDiff.assertMatches(
        goldenFile = goldenFile,
        actual = regressedActual,
        maxDiffPercentage = 0.0,
        outputDir = outDir,
      )
    }

    val actualArtifact = File(outDir, "route_overlay_actual.png")
    val diffArtifact = File(outDir, "route_overlay_diff.png")
    assertThat(actualArtifact.exists()).isTrue()
    assertThat(diffArtifact.exists()).isTrue()
    assertThat(failure.message).contains(diffArtifact.path)
    // [END golden_diff_artifacts_and_record]

    val decodedDiff = BitmapFactory.decodeFile(diffArtifact.path)
    assertThat(decodedDiff.getPixel(8, 8)).isEqualTo(Color.RED)
  }

  private fun createSolidBitmap(width: Int, height: Int, color: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(color)
    return bitmap
  }
}
// [END maps_golden_image_diff_test]
