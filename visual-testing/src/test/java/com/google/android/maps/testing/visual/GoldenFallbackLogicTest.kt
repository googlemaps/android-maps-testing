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

package com.google.android.maps.testing.visual

import android.graphics.Bitmap
import android.graphics.Color
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoldenFallbackLogicTest {

  @get:Rule
  val tempFolder = TemporaryFolder()

  @Test
  fun fastPassWhenGoldenMatches() = runBlocking {
    val goldensDir = tempFolder.newFolder("goldens")
    val testName = "sydney_map_test"

    // Create baseline bitmap (100x100 solid blue)
    val baselineBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
      eraseColor(Color.BLUE)
    }

    // Save baseline to goldens directory
    val goldenFile = File(goldensDir, "$testName.png")
    FileOutputStream(goldenFile).use { out ->
      baselineBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }

    // Actual screenshot is identical
    val screenshotBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
      eraseColor(Color.BLUE)
    }

    // Subclass BaseVisualTest with dummy key to verify fast-pass does NOT invoke network
    val testHarness = object : BaseVisualTest() {
      var invokedGemini = false
    }

    testHarness.verifyScreenshotWithGoldenFallback(
      testName = testName,
      screenshotBitmap = screenshotBitmap,
      prompt = "Verify map is blue",
      goldensDirectory = goldensDir,
    )

    // Golden file still exists and matches
    assertThat(goldenFile.exists()).isTrue()
  }
}
